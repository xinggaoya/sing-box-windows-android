package cn.moncn.sing_box_windows.v2.data.config

import org.json.JSONArray
import org.json.JSONObject
import cn.moncn.sing_box_windows.v2.domain.core.CoreClashMode
import cn.moncn.sing_box_windows.vpn.VpnDefaults
import cn.moncn.sing_box_windows.v2.domain.settings.AppSettings

object ConfigSettingsApplier {
    /** 配置中找不到 direct 出站时的兜底标签 */
    private const val DEFAULT_DIRECT_TAG = "direct"

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
            val directTag = findDirectOutboundTag(root)
            applyModeToRules(root.optJSONObject("route")?.optJSONArray("rules"), mode, directTag)
            applyModeToRules(root.optJSONObject("dns")?.optJSONArray("rules"), mode, null)
            root.toString(2)
        }.getOrElse { rawJson }
    }

    /**
     * 找到配置中 `type=direct` 的出站标签。
     *
     * 「直连」模式下必须真正走直连出站，而不是走「本地直连」选择组
     * （该组默认直连，但可被用户切换到代理节点）。
     */
    private fun findDirectOutboundTag(root: JSONObject): String {
        val outbounds = root.optJSONArray("outbounds") ?: return DEFAULT_DIRECT_TAG
        for (index in 0 until outbounds.length()) {
            val outbound = outbounds.optJSONObject(index) ?: continue
            if (outbound.optString("type") == "direct") {
                val tag = outbound.optString("tag")
                if (tag.isNotBlank()) return tag
            }
        }
        return DEFAULT_DIRECT_TAG
    }

    private fun applyModeToRules(rules: JSONArray?, mode: CoreClashMode, directTag: String?) {
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
            // 「直连」模式下强制走直连出站，避免落到可选代理的「本地直连」组
            if (mode == CoreClashMode.DIRECT && directTag != null && rule.has("outbound")) {
                rule.put("outbound", directTag)
            }
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
            // 协议栈固定为 gvisor：system 栈在 Android 上 TCP 连接全部超时
            inbound.put("stack", VpnDefaults.STACK)
            // 移除 dns_address：保留内核「地址 + 1」的派生与自动 DNS 劫持能力
            // （Android Private DNS 的 DoT/853 依赖该能力）
            inbound.remove("dns_address")
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
