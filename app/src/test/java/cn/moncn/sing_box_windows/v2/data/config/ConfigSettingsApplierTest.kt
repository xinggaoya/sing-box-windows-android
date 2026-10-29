package cn.moncn.sing_box_windows.v2.data.config

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 配置应用逻辑的单元测试。
 *
 * 关键约束：迁移到 libbox 原生命令接口后，配置中不再允许出现
 * `experimental.clash_api`（否则会重新开启 127.0.0.1 的 HTTP/WS 监听）。
 */
class ConfigSettingsApplierTest {

    private fun baseConfig(): JSONObject = JSONObject(
        """
        {
          "dns": { "servers": [] },
          "route": { "default_domain_resolver": { "server": "dns_resolver" } },
          "inbounds": [
            {
              "type": "tun",
              "tag": "tun-in",
              "mtu": 9000,
              "auto_route": true,
              "strict_route": true,
              "platform": { "http_proxy": { "enabled": true } }
            }
          ],
          "experimental": { "cache_file": { "enabled": true } }
        }
        """.trimIndent()
    )

    @Test
    fun applySettings_writesDnsStrategyAndCacheFlags() {
        val settings = AppSettings(
            dnsStrategy = "ipv4_only",
            dnsCacheEnabled = false,
            dnsIndependentCache = false
        )
        val result = JSONObject(
            ConfigSettingsApplier.applySettings(baseConfig().toString(), settings)
        )
        val dns = result.getJSONObject("dns")
        assertEquals("ipv4_only", dns.getString("strategy"))
        assertTrue(dns.getBoolean("disable_cache"))
        assertFalse(dns.getBoolean("independent_cache"))
        assertEquals(
            "ipv4_only",
            result.getJSONObject("route").getJSONObject("default_domain_resolver").getString("strategy")
        )
    }

    @Test
    fun applySettings_writesTunInboundFlags() {
        val settings = AppSettings(
            tunMtu = 1500,
            tunAutoRoute = false,
            tunStrictRoute = false,
            httpProxyEnabled = false
        )
        val result = JSONObject(
            ConfigSettingsApplier.applySettings(baseConfig().toString(), settings)
        )
        val inbound = result.getJSONArray("inbounds").getJSONObject(0)
        assertEquals(1500, inbound.getInt("mtu"))
        assertFalse(inbound.getBoolean("auto_route"))
        assertFalse(inbound.getBoolean("strict_route"))
        assertFalse(inbound.getJSONObject("platform").getJSONObject("http_proxy").getBoolean("enabled"))
    }

    @Test
    fun applySettings_neverWritesClashApiSection() {
        val result = JSONObject(
            ConfigSettingsApplier.applySettings(baseConfig().toString(), AppSettings())
        )
        val experimental = result.optJSONObject("experimental")
        assertTrue("experimental 段应保留", experimental != null)
        assertFalse(
            "不应再写入 clash_api",
            experimental!!.has("clash_api")
        )
    }

    @Test
    fun applySettings_keepsOriginalTextWhenParsingFails() {
        val broken = "{ not json"
        assertEquals(broken, ConfigSettingsApplier.applySettings(broken, AppSettings()))
    }
}
