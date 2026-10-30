package cn.moncn.sing_box_windows.v2.data.config

import cn.moncn.sing_box_windows.v2.domain.core.CoreClashMode
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 代理模式配置的回归测试。
 *
 * 背景：本项目使用 sing-box 1.14 原生的 `route.rules[].clash_mode`，
 * 未开启 `experimental.clash_api`，因此内核命令通道的 `SetClashMode` 必然失败，
 * 模式只能在配置层面固化。
 */
class ConfigSettingsApplierTest {

    private fun config(): JSONObject {
        val rules = JSONArray()
            .put(JSONObject().put("action", "sniff"))
            .put(JSONObject().put("ip_is_private", true).put("outbound", "direct"))
            .put(JSONObject().put("clash_mode", "global").put("outbound", "proxy"))
            .put(JSONObject().put("clash_mode", "direct").put("outbound", "direct"))
            .put(JSONObject().put("rule_set", "geosite-cn").put("outbound", "direct"))
        return JSONObject().put("route", JSONObject().put("rules", rules))
    }

    private fun rulesOf(root: JSONObject): JSONArray =
        root.getJSONObject("route").getJSONArray("rules")

    @Test
    fun applyClashMode_ruleMode_removesAllClashModeRules() {
        val result = JSONObject(
            ConfigSettingsApplier.applyClashMode(config().toString(), CoreClashMode.RULE)
        )
        val rules = rulesOf(result)

        assertEquals(3, rules.length())
        assertEquals("sniff", rules.getJSONObject(0).getString("action"))
        assertTrue(rules.toString().contains("\"clash_mode\"") == false)
    }

    @Test
    fun applyClashMode_globalMode_keepsGlobalRuleAndDropsDirectRule() {
        val result = JSONObject(
            ConfigSettingsApplier.applyClashMode(config().toString(), CoreClashMode.GLOBAL)
        )
        val rules = rulesOf(result)

        // sniff / ip_is_private / clash_mode=global / geosite-cn
        assertEquals(4, rules.length())
        assertTrue(rules.toString().contains("\"clash_mode\"") == false)
        assertEquals("proxy", rules.getJSONObject(2).getString("outbound"))
    }

    @Test
    fun applyClashMode_directMode_keepsDirectRuleAndDropsGlobalRule() {
        val result = JSONObject(
            ConfigSettingsApplier.applyClashMode(config().toString(), CoreClashMode.DIRECT)
        )
        val rules = rulesOf(result)

        assertEquals(4, rules.length())
        assertTrue(rules.toString().contains("\"clash_mode\"") == false)
        assertEquals("direct", rules.getJSONObject(2).getString("outbound"))
    }

    @Test
    fun applyClashMode_keepsNonClashModeRulesUntouched() {
        val result = JSONObject(
            ConfigSettingsApplier.applyClashMode(config().toString(), CoreClashMode.GLOBAL)
        )
        val rules = rulesOf(result)

        assertEquals("sniff", rules.getJSONObject(0).getString("action"))
        assertEquals("geosite-cn", rules.getJSONObject(3).getString("rule_set"))
    }

    @Test
    fun applyClashMode_dnsRulesAreAlsoProcessed() {
        val root = config()
        root.put(
            "dns",
            JSONObject().put(
                "rules",
                JSONArray()
                    .put(JSONObject().put("clash_mode", "global").put("server", "proxy"))
                    .put(JSONObject().put("clash_mode", "direct").put("server", "direct"))
                    .put(JSONObject().put("rule_set", "geosite-cn").put("server", "direct"))
            )
        )

        val result = JSONObject(
            ConfigSettingsApplier.applyClashMode(root.toString(), CoreClashMode.DIRECT)
        )
        val dnsRules = result.getJSONObject("dns").getJSONArray("rules")

        assertEquals(2, dnsRules.length())
        assertEquals("direct", dnsRules.getJSONObject(0).getString("server"))
        assertEquals("geosite-cn", dnsRules.getJSONObject(1).getString("rule_set"))
    }

    @Test
    fun applyClashMode_invalidJson_returnsOriginal() {
        val broken = "{ not json"
        assertEquals(broken, ConfigSettingsApplier.applyClashMode(broken, CoreClashMode.GLOBAL))
    }
}
