package cn.moncn.sing_box_windows.v2.data.config

import cn.moncn.sing_box_windows.vpn.VpnDefaults
import org.json.JSONArray
import org.json.JSONObject

/**
 * 内核配置的兜底默认值。
 *
 * sing-box 1.11 起入站不再支持 `sniff` / `sniff_override_destination` 等 legacy 字段
 * （1.13.0 已彻底移除），改由 `route.rules` 的 `sniff` / `hijack-dns` 动作承担。
 */
object ConfigDefaults {
    const val TAG_DIRECT = "DIRECT"
    const val TAG_BLOCK = "BLOCK"
    const val TAG_DNS_DIRECT = "dns_direct"
    const val TAG_DNS_RESOLVER = "dns_resolver"

    /**
     * TUN 入站。
     *
     * 注意：**不要**显式声明 `dns_address`。
     * sing-box 1.14 只有在未声明该字段时，才会把「发往派生 DNS 地址的连接」
     * 自动按 DNS 处理（含 Android Private DNS 的 DoT/853）；
     * 显式声明反而会让 DoT 连接按普通流量放行、泄漏到真实网络并超时。
     * DNS 地址由内核按「第一个 IPv4 地址 + 1」派生，
     * 因此 TUN 地址必须是 `/30` 子网内的可用主机地址（见 [VpnDefaults]）。
     */
    fun tunInbound(): JSONObject {
        return JSONObject()
            .put("type", "tun")
            .put("tag", "tun-in")
            .put("mtu", VpnDefaults.MTU)
            .put("address", JSONArray().put("${VpnDefaults.ADDRESS}/${VpnDefaults.PREFIX}"))
            .put("auto_route", false)
            .put("strict_route", false)
            .put("stack", VpnDefaults.STACK)
    }

    /** 与模板配置保持一致的 sniff / hijack-dns 路由动作 */
    private fun defaultRouteRules(): JSONArray {
        return JSONArray()
            .put(JSONObject().put("action", "sniff"))
            .put(JSONObject().put("protocol", "dns").put("action", "hijack-dns"))
    }

    /**
     * 兜底 DNS 段。
     *
     * 必须存在：`hijack-dns` 动作依赖内核的 DNS 模块，若缺少 `dns` 段，
     * 系统 DNS 查询既不会被接管也不会被放行，设备将完全无法联网。
     *
     * 服务器地址一律使用 IP，避免触发 sing-box 1.14 对
     * 「域名地址必须自带 domain_resolver」的强制要求。
     */
    private fun defaultDns(): JSONObject {
        return JSONObject()
            .put(
                "servers",
                JSONArray()
                    .put(
                        JSONObject()
                            .put("type", "udp")
                            .put("server", "223.5.5.5")
                            .put("tag", TAG_DNS_DIRECT)
                    )
                    .put(
                        JSONObject()
                            .put("type", "udp")
                            .put("server", "114.114.114.114")
                            .put("tag", TAG_DNS_RESOLVER)
                    )
            )
            .put("rules", JSONArray())
            .put("strategy", "prefer_ipv4")
            .put("final", TAG_DNS_DIRECT)
    }

    fun defaultConfigJson(): String {
        val outbounds = JSONArray()
            .put(JSONObject().put("type", "direct").put("tag", TAG_DIRECT))
            .put(JSONObject().put("type", "block").put("tag", TAG_BLOCK))

        val route = JSONObject()
            .put("rules", defaultRouteRules())
            .put(
                "default_domain_resolver",
                JSONObject().put("server", TAG_DNS_RESOLVER).put("strategy", "prefer_ipv4")
            )
            .put("final", TAG_DIRECT)

        val config = JSONObject()
            .put("log", JSONObject().put("level", "info"))
            .put("dns", defaultDns())
            .put("inbounds", JSONArray().put(tunInbound()))
            .put("outbounds", outbounds)
            .put("route", route)

        return config.toString(2)
    }
}
