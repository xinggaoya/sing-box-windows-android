package cn.moncn.sing_box_windows.config

import org.json.JSONArray
import org.json.JSONObject

object ConfigSettingsApplier {
    fun applySettings(rawJson: String, settings: AppSettings): String {
        return runCatching {
            val root = JSONObject(rawJson)
            applyLog(root, settings)
            applyDns(root, settings)
            applyTunInbound(root, settings)
            applyLocalInbounds(root, settings)
            applyExperimental(root, settings)
            root.toString(2)
        }.getOrElse { rawJson }
    }

    private fun applyLog(root: JSONObject, settings: AppSettings) {
        val log = root.optJSONObject("log") ?: JSONObject().also { root.put("log", it) }
        log.put("level", settings.logLevel)
        log.put("timestamp", settings.logTimestamp)
    }

    private fun applyDns(root: JSONObject, settings: AppSettings) {
        val dns = root.optJSONObject("dns") ?: return
        dns.put("strategy", settings.dnsStrategy)
        dns.put("independent_cache", settings.dnsIndependentCache)
        dns.put("disable_cache", !settings.dnsCacheEnabled)
        val route = root.optJSONObject("route") ?: return
        val resolver = route.optJSONObject("default_domain_resolver") ?: return
        resolver.put("strategy", settings.dnsStrategy)
    }

    private fun applyTunInbound(root: JSONObject, settings: AppSettings) {
        val inbounds = root.optJSONArray("inbounds") ?: return
        forEachInbound(inbounds) { inbound ->
            if (inbound.optString("type") != "tun") return@forEachInbound
            inbound.put("mtu", settings.tunMtu)
            inbound.put("auto_route", settings.tunAutoRoute)
            inbound.put("strict_route", settings.tunStrictRoute)
            val platform = inbound.optJSONObject("platform") ?: JSONObject().also {
                inbound.put("platform", it)
            }
            val httpProxy = platform.optJSONObject("http_proxy") ?: JSONObject().also {
                platform.put("http_proxy", it)
            }
            httpProxy.put("enabled", settings.httpProxyEnabled)
            if (settings.httpProxyEnabled) {
                // 让系统代理与 mixed 端口保持一致，避免端口不一致导致代理失效。
                httpProxy.put("server", "127.0.0.1")
                httpProxy.put("server_port", settings.mixedInboundPort)
            }
        }
    }

    private fun applyLocalInbounds(root: JSONObject, settings: AppSettings) {
        val inbounds = root.optJSONArray("inbounds") ?: return
        forEachInbound(inbounds) { inbound ->
            when (inbound.optString("type")) {
                "mixed" -> inbound.put("listen_port", settings.mixedInboundPort)
                "socks" -> inbound.put("listen_port", settings.socksInboundPort)
            }
        }
    }

    private fun applyExperimental(root: JSONObject, settings: AppSettings) {
        val experimental = root.optJSONObject("experimental") ?: JSONObject().also {
            root.put("experimental", it)
        }
        val cacheFile = experimental.optJSONObject("cache_file") ?: JSONObject().also {
            experimental.put("cache_file", it)
        }
        cacheFile.put("enabled", settings.cacheFileEnabled)
        if (settings.clashApiEnabled) {
            val clashApi = experimental.optJSONObject("clash_api") ?: JSONObject().also {
                experimental.put("clash_api", it)
            }
            if (!clashApi.has("default_mode")) {
                clashApi.put("default_mode", "rule")
            }
            clashApi.put("external_controller", settings.clashApiAddress)
            clashApi.put("secret", ClashApiDefaults.SECRET)
        } else {
            experimental.remove("clash_api")
        }
    }

    private fun forEachInbound(inbounds: JSONArray, action: (JSONObject) -> Unit) {
        for (index in 0 until inbounds.length()) {
            val inbound = inbounds.optJSONObject(index) ?: continue
            action(inbound)
        }
    }
}
