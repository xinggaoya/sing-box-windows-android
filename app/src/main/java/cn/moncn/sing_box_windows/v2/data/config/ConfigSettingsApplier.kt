package cn.moncn.sing_box_windows.v2.data.config

import org.json.JSONArray
import org.json.JSONObject
import cn.moncn.sing_box_windows.v2.domain.core.CoreClashMode
import cn.moncn.sing_box_windows.v2.domain.settings.AppSettings

object ConfigSettingsApplier {
    fun applySettings(rawJson: String, settings: AppSettings): String {
        return runCatching {
            val root = JSONObject(rawJson)
            applyDns(root, settings)
            applyTunInbound(root, settings)
            root.toString(2)
        }.getOrElse { rawJson }
    }

    /**
     * 应用代理模式。
     *
     * 本项目使用 sing-box 1.14 原生的 `route.rules[].clash_mode`（未开启
     * `experimental.clash_api`），因此**无法**通过内核命令通道的 `SetClashMode`
     * 切换模式（该接口依赖 clash_api，调用必然失败）。
     *
     * 改为在配置层面把 `clash_mode` 规则“固化”：
     *
     * - 规则（rule）：删除全部 `clash_mode` 规则，回归分流规则；
     * - 全局（global）：保留 `clash_mode=global` 规则并去掉该字段（变成无条件命中），
     *   删除 `clash_mode=direct` 规则；
     * - 直连（direct）：保留 `clash_mode=direct` 规则并去掉该字段，删除 `clash_mode=global` 规则。
     *
     * 规则表中的顺序保证了：
     * `ip_is_private → direct` 在前（VPN 自身流量仍然直连），
     * 其余流量在 global/direct 模式下会被紧随其后的规则全部接管。
     */
    fun applyClashMode(rawJson: String, mode: CoreClashMode): String {
        return runCatching {
            val root = JSONObject(rawJson)
            applyModeToRules(root.optJSONObject("route")?.optJSONArray("rules"), mode)
            applyModeToRules(root.optJSONObject("dns")?.optJSONArray("rules"), mode)
            root.toString(2)
        }.getOrElse { rawJson }
    }

    private fun applyModeToRules(rules: JSONArray?, mode: CoreClashMode) {
        if (rules == null) return
        val kept = JSONArray()
        for (index in 0 until rules.length()) {
            val rule = rules.optJSONObject(index) ?: continue
            val clashMode = rule.optString("clash_mode")
            if (clashMode.isEmpty()) {
                kept.put(rule)
                continue
            }
            val keep = when (mode) {
                CoreClashMode.RULE -> false
                CoreClashMode.GLOBAL -> clashMode == CoreClashMode.GLOBAL.raw
                CoreClashMode.DIRECT -> clashMode == CoreClashMode.DIRECT.raw
            }
            if (!keep) continue
            // 去掉 clash_mode 字段 → 该规则变成无条件命中
            rule.remove("clash_mode")
            kept.put(rule)
        }
        // JSONArray 无法重新赋值，需要原地重建
        while (rules.length() > 0) rules.remove(0)
        for (index in 0 until kept.length()) rules.put(kept.optJSONObject(index))
    }

    private fun applyDns(root: JSONObject, settings: AppSettings) {
        val dns = root.optJSONObject("dns") ?: return
        dns.put("strategy", settings.dnsStrategy)
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
        }
    }

    private fun forEachInbound(inbounds: JSONArray, action: (JSONObject) -> Unit) {
        for (index in 0 until inbounds.length()) {
            val inbound = inbounds.optJSONObject(index) ?: continue
            action(inbound)
        }
    }
}
