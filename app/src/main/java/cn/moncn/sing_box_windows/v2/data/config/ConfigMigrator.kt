package cn.moncn.sing_box_windows.v2.data.config

import cn.moncn.sing_box_windows.vpn.VpnDefaults
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
        changed = changed or migrateTunAddress(root)
        changed = changed or migrateInbounds(root)
        changed = changed or migrateDns(root)
        changed = changed or ensureDnsDomainResolvers(root)
        changed = changed or ensureDefaultDomainResolver(root)
        changed = changed or removeClashApi(root)
        return changed
    }

    /**
     * 归一化 TUN 入站（地址 / DNS 地址 / 协议栈）。
     *
     * 1. **地址**：内核按「第一个 IPv4 地址 + 1」派生下发给系统的 DNS 服务器地址。
     *    历史配置用的是 `10.0.0.2/30`，派生出的 `10.0.0.3` 恰好是该 /30 的**广播地址**，
     *    Linux 内核会丢弃发往广播地址的报文 —— 系统 DNS 完全不可用，
     *    表现为 WiFi「无网络」感叹号、所有应用无法联网、域名分流完全失效。
     *    这里统一归一化为官方示例地址 `172.18.0.1/30`（派生 DNS 为 `172.18.0.2`）。
     * 2. **DNS 地址**：移除历史配置中可能存在的 `dns_address`。
     *    sing-box 1.14 只有在**未**声明该字段时，才会把「发往派生 DNS 地址的连接」
     *    自动按 DNS 处理（含 Android Private DNS 的 DoT/853）；
     *    一旦显式声明，DoT 连接会按普通流量放行并泄漏到真实网络。
     * 3. **协议栈**：`system` 栈依赖内核 TCP 在 socketpair 上完成握手，
     *    实测在 Android 上 TCP 连接全部超时（UDP/ICMP 正常），
     *    因此统一改为 sing-tun 自带的 `gvisor` 用户态协议栈。
     */
    private fun migrateTunAddress(root: JSONObject): Boolean {
        val inbounds = root.optJSONArray("inbounds") ?: return false
        var changed = false
        val canonical = "${VpnDefaults.ADDRESS}/${VpnDefaults.PREFIX}"
        for (i in 0 until inbounds.length()) {
            val inbound = inbounds.optJSONObject(i) ?: continue
            if (inbound.optString("type") != "tun") continue

            // 协议栈：system → gvisor
            val stack = inbound.optString("stack", "system")
            if (stack != VpnDefaults.STACK) {
                inbound.put("stack", VpnDefaults.STACK)
                changed = true
            }

            // 地址：统一为官方示例地址
            val addresses = inbound.optJSONArray("address")
            if (addresses != null) {
                val updated = JSONArray()
                var addressChanged = false
                for (j in 0 until addresses.length()) {
                    val address = addresses.optString(j)
                    val replacement = if (isIpv4Cidr(address)) canonical else address
                    if (replacement != address) {
                        addressChanged = true
                    }
                    updated.put(replacement)
                }
                if (addressChanged) {
                    inbound.put("address", updated)
                    changed = true
                }
            }

            // 移除 dns_address：sing-box 1.14 只有在未声明该字段时，
            // 才会把「发往派生 DNS 地址的连接」自动按 DNS 处理（含 DoT/853），
            // 显式声明会让 Android Private DNS 的 DoT 连接泄漏到真实网络并超时。
            if (inbound.has("dns_address")) {
                inbound.remove("dns_address")
                changed = true
            }
        }
        return changed
    }

    private fun isIpv4Cidr(value: String): Boolean {
        val host = value.substringBefore('/')
        return isIpAddress(host)
    }

    private fun isIpv4(value: String): Boolean = isIpAddress(value)

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
     * 为「域名地址」的 DNS 服务器补齐 `domain_resolver`。
     *
     * sing-box 1.14 的硬性要求：DNS 服务器地址为域名时，**必须**由该服务器自身携带
     * `domain_resolver`，`route.default_domain_resolver` 不能作为兜底。缺失时内核初始化直接失败：
     *
     * ```
     * initialize DNS server[1]: missing domain resolver for domain server address
     * ```
     *
     * 迁移历史配置时若直接丢弃 legacy 的 `address_resolver`，就会命中该错误，
     * 导致内核根本起不来（表现为连接失败而不是「无法分流」）。
     */
    private fun ensureDnsDomainResolvers(root: JSONObject): Boolean {
        val dns = root.optJSONObject("dns") ?: return false
        val servers = dns.optJSONArray("servers") ?: return false
        var changed = false

        // 优先使用 route.default_domain_resolver 指向的解析器
        val defaultResolver = root.optJSONObject("route")
            ?.optJSONObject("default_domain_resolver")
            ?.optString("server")
            ?.takeIf { it.isNotBlank() }

        // 其次找一台地址为 IP 的 DNS 服务器作为引导解析器
        val ipResolverTag = (0 until servers.length())
            .mapNotNull { servers.optJSONObject(it) }
            .firstOrNull { server ->
                val address = server.optString("server")
                address.isNotBlank() && isIpAddress(address)
            }
            ?.optString("tag")
            ?.takeIf { it.isNotBlank() }

        val fallback = defaultResolver ?: ipResolverTag ?: return changed

        for (i in 0 until servers.length()) {
            val server = servers.optJSONObject(i) ?: continue
            val address = server.optString("server")
            if (address.isBlank() || isIpAddress(address)) continue
            if (server.optString("domain_resolver").isNotBlank()) continue
            server.put("domain_resolver", fallback)
            changed = true
        }
        return changed
    }

    /**
     * 确保 `route.default_domain_resolver` 存在。
     *
     * sing-box 1.14 起，出站/服务端使用域名时必须指定解析器；
     * 缺失会导致出站无法解析节点域名（表现为「节点全部超时」）。
     */
    private fun ensureDefaultDomainResolver(root: JSONObject): Boolean {
        val route = root.optJSONObject("route") ?: return false
        if (route.optJSONObject("default_domain_resolver") != null) return false
        val servers = root.optJSONObject("dns")?.optJSONArray("servers") ?: return false
        val resolverTag = (0 until servers.length())
            .mapNotNull { servers.optJSONObject(it) }
            .firstOrNull { server ->
                val address = server.optString("server")
                address.isNotBlank() && isIpAddress(address)
            }
            ?.optString("tag")
            ?.takeIf { it.isNotBlank() }
            ?: return false
        route.put(
            "default_domain_resolver",
            JSONObject().put("server", resolverTag).put("strategy", "prefer_ipv4")
        )
        return true
    }

    private fun isIpAddress(value: String): Boolean {
        val parts = value.split('.')
        if (parts.size != 4) return false
        return parts.all { part ->
            part.isNotEmpty() && part.length <= 3 && part.toIntOrNull()?.let { it in 0..255 } == true
        }
    }

    /**
     * legacy DNS 服务器格式 → 新格式。
     *
     * legacy: `{"address": "tls://1.1.1.1", "address_resolver": "local"}`
     * new:    `{"type": "tls", "server": "1.1.1.1", "domain_resolver": "local"}`
     *
     * 注意：`address_resolver` 必须映射为 `domain_resolver`。
     * legacy 格式下解析器写在 `address_resolver`，新格式改名为 `domain_resolver`；
     * 迁移时丢弃该字段会让「域名地址」的 DNS 服务器缺少解析器，内核启动即失败。
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

        val legacyResolver = server.optString("address_resolver")
        server.remove("address")
        server.remove("address_resolver")
        server.remove("address_strategy")
        server.put("type", type)
        if (legacyResolver.isNotBlank() && !server.has("domain_resolver")) {
            server.put("domain_resolver", legacyResolver)
        }

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
