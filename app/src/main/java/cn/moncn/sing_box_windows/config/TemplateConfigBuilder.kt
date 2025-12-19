package cn.moncn.sing_box_windows.config

import cn.moncn.sing_box_windows.vpn.VpnDefaults
import org.json.JSONArray
import org.json.JSONObject
import org.yaml.snakeyaml.Yaml

data class TemplateBuildResult(
    val ok: Boolean,
    val configJson: String? = null,
    val warnings: List<String> = emptyList(),
    val error: String? = null
)

data class NodeParseResult(
    val nodes: List<JSONObject>,
    val warnings: List<String>
)

object TemplateConfigBuilder {
    private const val TAG_PROXY = "Proxy"
    private const val TAG_AUTO = "Auto"
    private const val TAG_DIRECT = "DIRECT"
    private const val TAG_BLOCK = "BLOCK"
    private const val TAG_DNS_LOCAL = "dns-local"
    private const val TAG_DNS_REMOTE = "dns-remote"

    fun buildFromSubscription(content: String): TemplateBuildResult {
        val trimmed = content.trim()
        val jsonNodes = parseSingBoxNodes(trimmed)
        val parseResult = jsonNodes ?: parseClashNodes(trimmed)
            ?: return TemplateBuildResult(
                ok = false,
                error = "订阅内容无法识别为 sing-box JSON 或 Clash 配置"
            )
        if (parseResult.nodes.isEmpty()) {
            return TemplateBuildResult(ok = false, error = "订阅中未找到可用节点")
        }
        val config = buildTemplate(parseResult.nodes)
        return TemplateBuildResult(ok = true, configJson = config, warnings = parseResult.warnings)
    }

    private fun parseSingBoxNodes(json: String): NodeParseResult? {
        if (!isLikelyJson(json)) return null
        return runCatching {
            val root = JSONObject(json)
            val outbounds = root.optJSONArray("outbounds") ?: return null
            val warnings = mutableListOf<String>()
            val nodes = mutableListOf<JSONObject>()
            for (i in 0 until outbounds.length()) {
                val outbound = outbounds.optJSONObject(i) ?: continue
                val type = outbound.optString("type")
                if (isGroupOrSystemOutbound(type)) {
                    continue
                }
                nodes.add(JSONObject(outbound.toString()))
            }
            NodeParseResult(nodes, warnings)
        }.getOrNull()
    }

    private fun parseClashNodes(yamlContent: String): NodeParseResult? {
        val root = runCatching { Yaml().load<Any>(yamlContent) as? Map<*, *> }
            .getOrNull() ?: return null
        val warnings = mutableListOf<String>()
        val proxies = readMapList(root["proxies"])
        val nodes = mutableListOf<JSONObject>()
        proxies.forEach { proxy ->
            mapProxy(proxy, warnings)?.let { nodes.add(it) }
        }
        return NodeParseResult(nodes, warnings)
    }

    private fun buildTemplate(nodes: List<JSONObject>): String {
        val outbounds = JSONArray()
        val usedTags = linkedSetOf(TAG_DIRECT, TAG_BLOCK, TAG_PROXY, TAG_AUTO)

        outbounds.put(JSONObject().put("type", "direct").put("tag", TAG_DIRECT))
        outbounds.put(JSONObject().put("type", "block").put("tag", TAG_BLOCK))

        val normalizedNodes = nodes.mapIndexed { index, node ->
            val copied = JSONObject(node.toString())
            val originalTag = copied.optString("tag").ifBlank { "Node-${index + 1}" }
            val safeTag = uniqueTag(originalTag, usedTags)
            if (safeTag != originalTag) {
                copied.put("tag", safeTag)
            }
            usedTags.add(safeTag)
            copied
        }

        normalizedNodes.forEach { outbounds.put(it) }

        val nodeTags = normalizedNodes.map { it.optString("tag") }

        val autoGroup = JSONObject()
            .put("type", "urltest")
            .put("tag", TAG_AUTO)
            .put("outbounds", JSONArray().apply { nodeTags.forEach { put(it) } })
            .put("url", "http://www.gstatic.com/generate_204")
            .put("interval", "5m")
        outbounds.put(autoGroup)

        val proxyTags = JSONArray().apply {
            put(TAG_AUTO)
            nodeTags.forEach { put(it) }
            put(TAG_DIRECT)
        }
        outbounds.put(
            JSONObject()
                .put("type", "selector")
                .put("tag", TAG_PROXY)
                .put("outbounds", proxyTags)
                .put("default", TAG_AUTO)
        )

        val dns = JSONObject()
            .put(
                "servers",
                JSONArray()
                    .put(
                        JSONObject()
                            .put("tag", TAG_DNS_REMOTE)
                            .put("type", "tls")
                            .put("server", "8.8.8.8")
                    )
                    .put(
                        JSONObject()
                            .put("tag", TAG_DNS_LOCAL)
                            .put("type", "udp")
                            .put("server", "223.5.5.5")
                    )
            )
            .put(
                "rules",
                JSONArray()
                    .put(JSONObject().put("rule_set", "geosite-geolocation-cn").put("server", TAG_DNS_LOCAL))
                    .put(JSONObject().put("rule_set", "geoip-cn").put("server", TAG_DNS_LOCAL))
            )
            .put("strategy", "ipv4_only")

        val routeRules = JSONArray()
            .put(JSONObject().put("action", "sniff"))
            .put(
                JSONObject()
                    .put("type", "logical")
                    .put("mode", "or")
                    .put(
                        "rules",
                        JSONArray()
                            .put(JSONObject().put("protocol", "dns"))
                            .put(JSONObject().put("port", 53))
                    )
                    .put("action", "hijack-dns")
            )
            .put(JSONObject().put("ip_is_private", true).put("outbound", TAG_DIRECT))
            .put(JSONObject().put("rule_set", "geosite-geolocation-cn").put("outbound", TAG_DIRECT))
            .put(JSONObject().put("rule_set", "geoip-cn").put("outbound", TAG_DIRECT))

        val route = JSONObject()
            .put("rules", routeRules)
            .put("final", TAG_PROXY)
            .put("auto_detect_interface", true)
            .put("default_domain_resolver", TAG_DNS_LOCAL)
            .put(
                "rule_set",
                JSONArray()
                    .put(
                        JSONObject()
                            .put("type", "remote")
                            .put("tag", "geosite-geolocation-cn")
                            .put("format", "binary")
                            .put(
                                "url",
                                "https://raw.githubusercontent.com/SagerNet/sing-geosite/rule-set/geosite-geolocation-cn.srs"
                            )
                    )
                    .put(
                        JSONObject()
                            .put("type", "remote")
                            .put("tag", "geoip-cn")
                            .put("format", "binary")
                            .put(
                                "url",
                                "https://raw.githubusercontent.com/SagerNet/sing-geoip/rule-set/geoip-cn.srs"
                            )
                    )
            )

        val inbound = JSONObject()
            .put("type", "tun")
            .put("tag", "tun-in")
            .put("mtu", VpnDefaults.MTU)
            .put("address", JSONArray().put("${VpnDefaults.ADDRESS}/${VpnDefaults.PREFIX}"))
            .put("auto_route", true)
            .put("strict_route", true)
            .put("stack", "system")
            .put("sniff", true)
            .put("sniff_override_destination", true)

        val config = JSONObject()
            .put("log", JSONObject().put("level", "info"))
            .put("dns", dns)
            .put("inbounds", JSONArray().put(inbound))
            .put("outbounds", outbounds)
            .put("route", route)
            .put(
                "experimental",
                JSONObject().put(
                    "cache_file",
                    JSONObject()
                        .put("enabled", true)
                        .put("store_rdrc", true)
                )
            )

        return config.toString(2)
    }

    private fun readMapList(value: Any?): List<Map<*, *>> {
        return (value as? List<*>)?.mapNotNull { it as? Map<*, *> } ?: emptyList()
    }

    private fun mapProxy(proxy: Map<*, *>, warnings: MutableList<String>): JSONObject? {
        val name = proxy["name"]?.toString() ?: return null
        val type = proxy["type"]?.toString()?.lowercase() ?: return null
        return when (type) {
            "ss", "shadowsocks" -> mapShadowsocks(proxy, name, warnings)
            "trojan" -> mapTrojan(proxy, name)
            "vmess" -> mapVmess(proxy, name)
            "vless" -> mapVless(proxy, name)
            "socks5", "socks" -> mapSocks(proxy, name)
            "http" -> mapHttp(proxy, name)
            else -> {
                warnings.add("unsupported proxy type: $type ($name)")
                null
            }
        }
    }

    private fun mapShadowsocks(proxy: Map<*, *>, name: String, warnings: MutableList<String>): JSONObject? {
        val server = proxy["server"]?.toString() ?: return null
        val port = proxy["port"]?.toString()?.toIntOrNull() ?: return null
        val method = proxy["cipher"]?.toString() ?: proxy["method"]?.toString()
        val password = proxy["password"]?.toString()
        if (method.isNullOrBlank() || password.isNullOrBlank()) {
            warnings.add("shadowsocks missing method/password: $name")
            return null
        }
        val outbound = JSONObject()
            .put("type", "shadowsocks")
            .put("tag", name)
            .put("server", server)
            .put("server_port", port)
            .put("method", method)
            .put("password", password)
        val plugin = proxy["plugin"]?.toString()
        if (!plugin.isNullOrBlank()) {
            outbound.put("plugin", plugin)
            proxy["plugin-opts"]?.toString()?.let { outbound.put("plugin_opts", it) }
        }
        return outbound
    }

    private fun mapTrojan(proxy: Map<*, *>, name: String): JSONObject? {
        val server = proxy["server"]?.toString() ?: return null
        val port = proxy["port"]?.toString()?.toIntOrNull() ?: return null
        val password = proxy["password"]?.toString() ?: return null
        val outbound = JSONObject()
            .put("type", "trojan")
            .put("tag", name)
            .put("server", server)
            .put("server_port", port)
            .put("password", password)
        buildTls(proxy)?.let { outbound.put("tls", it) }
        buildTransport(proxy)?.let { outbound.put("transport", it) }
        return outbound
    }

    private fun mapVmess(proxy: Map<*, *>, name: String): JSONObject? {
        val server = proxy["server"]?.toString() ?: return null
        val port = proxy["port"]?.toString()?.toIntOrNull() ?: return null
        val uuid = proxy["uuid"]?.toString() ?: return null
        val outbound = JSONObject()
            .put("type", "vmess")
            .put("tag", name)
            .put("server", server)
            .put("server_port", port)
            .put("uuid", uuid)
        proxy["cipher"]?.toString()?.let { outbound.put("security", it) }
        proxy["alterId"]?.toString()?.toIntOrNull()?.let { outbound.put("alter_id", it) }
        buildTls(proxy)?.let { outbound.put("tls", it) }
        buildTransport(proxy)?.let { outbound.put("transport", it) }
        return outbound
    }

    private fun mapVless(proxy: Map<*, *>, name: String): JSONObject? {
        val server = proxy["server"]?.toString() ?: return null
        val port = proxy["port"]?.toString()?.toIntOrNull() ?: return null
        val uuid = proxy["uuid"]?.toString() ?: return null
        val outbound = JSONObject()
            .put("type", "vless")
            .put("tag", name)
            .put("server", server)
            .put("server_port", port)
            .put("uuid", uuid)
        proxy["flow"]?.toString()?.let { outbound.put("flow", it) }
        buildTls(proxy)?.let { outbound.put("tls", it) }
        buildTransport(proxy)?.let { outbound.put("transport", it) }
        return outbound
    }

    private fun mapSocks(proxy: Map<*, *>, name: String): JSONObject? {
        val server = proxy["server"]?.toString() ?: return null
        val port = proxy["port"]?.toString()?.toIntOrNull() ?: return null
        val outbound = JSONObject()
            .put("type", "socks")
            .put("tag", name)
            .put("server", server)
            .put("server_port", port)
        proxy["username"]?.toString()?.let { outbound.put("username", it) }
        proxy["password"]?.toString()?.let { outbound.put("password", it) }
        return outbound
    }

    private fun mapHttp(proxy: Map<*, *>, name: String): JSONObject? {
        val server = proxy["server"]?.toString() ?: return null
        val port = proxy["port"]?.toString()?.toIntOrNull() ?: return null
        val outbound = JSONObject()
            .put("type", "http")
            .put("tag", name)
            .put("server", server)
            .put("server_port", port)
        proxy["username"]?.toString()?.let { outbound.put("username", it) }
        proxy["password"]?.toString()?.let { outbound.put("password", it) }
        buildTls(proxy)?.let { outbound.put("tls", it) }
        return outbound
    }

    private fun buildTls(proxy: Map<*, *>): JSONObject? {
        val tlsEnabled = proxy["tls"] as? Boolean ?: false
        if (!tlsEnabled) {
            return null
        }
        val tls = JSONObject().put("enabled", true)
        val serverName = proxy["servername"]?.toString() ?: proxy["sni"]?.toString()
        if (!serverName.isNullOrBlank()) {
            tls.put("server_name", serverName)
        }
        (proxy["skip-cert-verify"] as? Boolean)?.let { tls.put("insecure", it) }
        val alpn = proxy["alpn"]
        if (alpn is List<*>) {
            val alpnArray = JSONArray()
            alpn.filterNotNull().forEach { alpnArray.put(it.toString()) }
            tls.put("alpn", alpnArray)
        }
        return tls
    }

    private fun buildTransport(proxy: Map<*, *>): JSONObject? {
        val network = proxy["network"]?.toString()?.lowercase() ?: return null
        if (network != "ws") {
            return null
        }
        val opts = proxy["ws-opts"] as? Map<*, *>
        val path = opts?.get("path")?.toString() ?: "/"
        val headers = opts?.get("headers") as? Map<*, *>
        val transport = JSONObject()
            .put("type", "ws")
            .put("path", path)
        if (headers != null) {
            val headerJson = JSONObject()
            headers.forEach { (key, value) ->
                if (key != null && value != null) {
                    headerJson.put(key.toString(), value.toString())
                }
            }
            transport.put("headers", headerJson)
        }
        return transport
    }

    private fun isGroupOrSystemOutbound(type: String): Boolean {
        return type in setOf("direct", "block", "dns", "selector", "urltest", "fallback")
    }

    private fun uniqueTag(base: String, existing: MutableSet<String>): String {
        if (!existing.contains(base)) {
            return base
        }
        var index = 2
        while (true) {
            val candidate = "$base-$index"
            if (!existing.contains(candidate)) {
                return candidate
            }
            index++
        }
    }

    private fun isLikelyJson(text: String): Boolean {
        val sample = text.trimStart()
        if (!(sample.startsWith("{") || sample.startsWith("["))) {
            return false
        }
        return sample.contains("\"outbounds\"") || sample.contains("\"inbounds\"")
    }
}
