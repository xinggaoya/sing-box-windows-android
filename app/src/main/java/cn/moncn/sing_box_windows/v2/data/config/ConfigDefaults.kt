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
    fun tunInbound(): JSONObject {
        return JSONObject()
            .put("type", "tun")
            .put("tag", "tun-in")
            .put("mtu", VpnDefaults.MTU)
            .put("address", JSONArray().put("${VpnDefaults.ADDRESS}/${VpnDefaults.PREFIX}"))
            .put("auto_route", false)
            .put("strict_route", false)
            .put("stack", "system")
    }

    /** 与模板配置保持一致的 sniff / hijack-dns 路由动作 */
    private fun defaultRouteRules(): JSONArray {
        return JSONArray()
            .put(JSONObject().put("action", "sniff"))
            .put(JSONObject().put("protocol", "dns").put("action", "hijack-dns"))
    }

    fun defaultConfigJson(): String {
        val outbounds = JSONArray()
            .put(JSONObject().put("type", "direct").put("tag", "DIRECT"))
            .put(JSONObject().put("type", "block").put("tag", "BLOCK"))

        val route = JSONObject()
            .put("rules", defaultRouteRules())
            .put("final", "DIRECT")

        val config = JSONObject()
            .put("log", JSONObject().put("level", "info"))
            .put("inbounds", JSONArray().put(tunInbound()))
            .put("outbounds", outbounds)
            .put("route", route)

        return config.toString(2)
    }
}
