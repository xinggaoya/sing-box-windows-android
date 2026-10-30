package cn.moncn.sing_box_windows.v2.data.config

import org.json.JSONArray
import org.json.JSONObject

/**
 * sing-box 配置格式迁移。
 *
 * 内核从 1.12.0 升级到 1.14.4 后，若干字段被移除，直接使用会报
 * `legacy inbound fields are deprecated in sing-box 1.11.0 and removed in sing-box 1.13.0`
 * 一类的解码错误。本类负责把历史配置迁移到当前内核支持的格式。
 *
 * 迁移项：
 * 1. **入站 legacy 字段**（1.11.0 弃用 / 1.13.0 移除）
 *    `sniff` / `sniff_override_destination` / `sniff_timeout` /
 *    `domain_strategy` / `udp_disable_domain_unmapping`
 *    → 移除，sniff 能力改由 `route.rules` 的 `action: sniff` 承担
 * 2. **DNS 独立缓存**（1.14.0 弃用 / 1.16.0 移除）→ 移除 `dns.independent_cache`
 * 3. **DNS 服务器 legacy 格式**（1.12.0 弃用 / 1.14.0 移除）
 *    `{"address": "tls://1.1.1.1"}` → `{"type": "tls", "server": "1.1.1.1"}`
 * 4. **Clash API 段**（已由 libbox 原生命令接口取代）→ 移除 `experimental.clash_api`
 */
object ConfigMigrator {

    /** sing-box 1.11.0 起从入站移除的字段 */
    private val LEGACY_INBOUND_FIELDS = arrayOf(
        "sniff",
        "sniff_override_destination",
        "sniff_timeout",
        "domain_strategy",
        "udp_disable_domain_unmapping"
    )

    /** sing-box 1.14.0 起废弃的 DNS 字段 */
    private val DEPRECATED_DNS_FIELDS = arrayOf("independent_cache")

    /** legacy DNS 地址 scheme → 新格式的传输类型 */
    private val DNS_SCHEME_TO_TYPE = mapOf(
        "tls" to "tls",
        "https" to "https",
        "h3" to "h3",
        "quic" to "quic",
        "udp" to "udp",
        "tcp" to "tcp",
        "local" to "local",
        "hosts" to "hosts",
        "dhcp" to "dhcp",
        "mdns" to "mdns",
        "fakeip" to "fakeip"
    )

    /** 支持 `path` 字段的 DNS 传输（legacy 地址中的路径需要单独拆出） */
    private val DNS_TYPES_WITH_PATH = setOf("https", "h3", "quic")

    /**
     * 迁移配置文本；解析失败时原样返回，避免损坏用户的配置。
     */
    fun migrate(json: String): String {
        return runCatching {
            val root = JSONObject(json)
            if (migrate(root)) root.toString(2) else json
        }.getOrElse { json }
    }

    /**
     * 就地迁移配置，返回是否发生了变更。
     */
    fun migrate(root: JSONObject): Boolean {
        var changed = false
        changed = changed or migrateInbounds(root)
        changed = changed or migrateDns(root)
        changed = changed or removeClashApi(root)
        return changed
    }

    /**
     * 移除入站 legacy 字段，并在原本依赖入站 sniff 的情况下补上 `action: sniff` 路由动作。
     */
    private fun migrateInbounds(root: JSONObject): Boolean {
        val inbounds = root.optJSONArray("inbounds") ?: return false
        var changed = false
        var hadSniff = false
        for (i in 0 until inbounds.length()) {
            val inbound = inbounds.optJSONObject(i) ?: continue
            for (field in LEGACY_INBOUND_FIELDS) {
                if (!inbound.has(field)) continue
                if (field == "sniff" && inbound.optBoolean("sniff")) {
                    hadSniff = true
                }
                inbound.remove(field)
                changed = true
            }
        }
        if (hadSniff) {
            changed = changed or ensureSniffAction(root)
        }
        return changed
    }

    /** 确保路由规则里存在 `action: sniff`，使迁移后仍具备流量嗅探能力 */
    private fun ensureSniffAction(root: JSONObject): Boolean {
        val route = root.optJSONObject("route") ?: return false
        val rules = route.optJSONArray("rules") ?: return false
        for (i in 0 until rules.length()) {
            val rule = rules.optJSONObject(i) ?: continue
            if (rule.optString("action") == "sniff") return false
        }
        rules.put(JSONObject().put("action", "sniff"))
        // sniff 必须优先于其它路由规则
        val first = rules.optJSONObject(0) ?: return true
        if (first.optString("action") == "sniff") return true
        val reordered = JSONArray()
        reordered.put(JSONObject().put("action", "sniff"))
        for (i in 0 until rules.length()) {
            reordered.put(rules.getJSONObject(i))
        }
        route.put("rules", reordered)
        return true
    }

    /** 迁移 DNS 段：移除废弃字段并转换 legacy 服务器格式 */
    private fun migrateDns(root: JSONObject): Boolean {
        val dns = root.optJSONObject("dns") ?: return false
        var changed = false
        for (field in DEPRECATED_DNS_FIELDS) {
            if (dns.has(field)) {
                dns.remove(field)
                changed = true
            }
        }
        val servers = dns.optJSONArray("servers") ?: return changed
        for (i in 0 until servers.length()) {
            val server = servers.optJSONObject(i) ?: continue
            if (migrateLegacyDnsServer(server)) {
                changed = true
            }
        }
        return changed
    }

    /**
     * legacy DNS 服务器格式 → 新格式。
     *
     * legacy: `{"address": "tls://1.1.1.1", "address_resolver": "local"}`
     * new:    `{"type": "tls", "server": "1.1.1.1"}`
     */
    private fun migrateLegacyDnsServer(server: JSONObject): Boolean {
        if (server.has("type")) return false
        val address = server.optString("address")
        if (address.isBlank()) return false

        val separator = address.indexOf("://")
        if (separator <= 0) return false
        val scheme = address.substring(0, separator).lowercase()
        val type = DNS_SCHEME_TO_TYPE[scheme] ?: return false
        val target = address.substring(separator + 3)

        server.remove("address")
        server.remove("address_resolver")
        server.remove("address_strategy")
        server.put("type", type)

        // target 可能是 host、host:port 或 ipv6
        val host: String
        val port: Int?
        if (target.startsWith("[")) {
            val closing = target.indexOf(']')
            if (closing < 0) return true
            host = target.substring(1, closing)
            val rest = target.substring(closing + 1)
            port = rest.removePrefix(":").toIntOrNull()
        } else {
            val colon = target.lastIndexOf(':')
            val parsed = target.substring(colon + 1).toIntOrNull()
            if (colon > 0 && parsed != null) {
                host = target.substring(0, colon)
                port = parsed
            } else {
                host = target
                port = null
            }
        }
        server.put("server", extractPath(server, type, host))
        if (port != null) {
            server.put("server_port", port)
        }
        return true
    }

    /**
     * legacy 地址形如 `https://dns.google/dns-query`，
     * 新格式需把路径拆到独立的 `path` 字段。
     */
    private fun extractPath(server: JSONObject, type: String, host: String): String {
        if (type !in DNS_TYPES_WITH_PATH) return host
        val slash = host.indexOf('/')
        if (slash <= 0) return host
        val path = host.substring(slash)
        server.put("path", path)
        return host.substring(0, slash)
    }

    /**
     * 移除 `experimental.clash_api`。
     *
     * 应用已改用 libbox 自带的 `CommandServer` / `CommandClient`，
     * 不再需要在本地 9090 端口开启 Clash API 的 HTTP / WebSocket 监听。
     */
    private fun removeClashApi(root: JSONObject): Boolean {
        val experimental = root.optJSONObject("experimental") ?: return false
        if (!experimental.has("clash_api")) return false
        experimental.remove("clash_api")
        return true
    }
}
