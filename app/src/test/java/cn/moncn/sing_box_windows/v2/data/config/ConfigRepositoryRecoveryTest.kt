package cn.moncn.sing_box_windows.v2.data.config

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

/**
 * 配置文件的加载与自愈。
 *
 * 回归目标：空文件 / 损坏文件曾经会让内核报
 * `unexpected end of JSON input` 而**永远连不上**，且用户无法自行恢复。
 */
class ConfigRepositoryRecoveryTest {

    @get:Rule
    val temporaryFolder = TemporaryFolder()

    private fun configFile(): File = File(temporaryFolder.root, "singbox.json")

    @Test
    fun missingConfigFile_isCreatedAsDefault() {
        val json = ConfigRepository.loadOrCreateConfigFrom(configFile())
        val root = JSONObject(json)
        assertTrue("兜底配置必须有 dns 段", root.has("dns"))
        assertTrue("兜底配置必须有路由规则", root.getJSONObject("route").has("rules"))
        assertTrue("兜底配置被写回磁盘", configFile().readText().isNotBlank())
    }

    @Test
    fun emptyConfigFile_isRegeneratedAsDefault() {
        configFile().writeText("")
        val json = ConfigRepository.loadOrCreateConfigFrom(configFile())
        assertTrue(JSONObject(json).has("dns"))
        assertTrue("兜底配置被写回磁盘", configFile().readText().isNotBlank())
    }

    @Test
    fun corruptedConfigFile_isRegeneratedAsDefault() {
        configFile().writeText("{ this is not json ")
        val json = ConfigRepository.loadOrCreateConfigFrom(configFile())
        assertTrue(JSONObject(json).has("dns"))
    }

    @Test
    fun legacyConfig_isMigratedOnLoad() {
        configFile().writeText(
            """
            {
              "dns": {
                "servers": [
                  { "address": "https://dns.google/dns-query", "address_resolver": "local" },
                  { "type": "udp", "server": "114.114.114.114", "tag": "dns_resolver" }
                ],
                "final": "dns_resolver"
              },
              "inbounds": [
                { "type": "tun", "tag": "tun-in", "address": [ "10.0.0.2/30" ], "stack": "system", "sniff": true }
              ],
              "outbounds": [ { "type": "direct", "tag": "direct" } ],
              "route": { "rules": [ { "action": "sniff" } ], "final": "direct" }
            }
            """.trimIndent()
        )
        val root = JSONObject(ConfigRepository.loadOrCreateConfigFrom(configFile()))

        val inbound = root.getJSONArray("inbounds").getJSONObject(0)
        assertEquals("协议栈必须迁移为 gvisor", "gvisor", inbound.optString("stack"))
        assertFalse("不得保留 dns_address（会破坏自动 DNS 劫持）", inbound.has("dns_address"))
        val address = inbound.getJSONArray("address").getString(0)
        assertEquals("TUN 地址必须归一化", "172.18.0.1/30", address)

        val dnsServer = root.getJSONObject("dns").getJSONArray("servers").getJSONObject(0)
        assertEquals(
            "legacy address_resolver 必须迁移为 domain_resolver",
            "local",
            dnsServer.optString("domain_resolver")
        )
    }
}
