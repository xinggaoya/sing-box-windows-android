package cn.moncn.sing_box_windows.v2.data.config

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * sing-box 配置格式迁移的单元测试。
 *
 * 背景：内核升级到 sing-box 1.14.4 后，1.11.0 弃用 / 1.13.0 移除的入站 legacy 字段
 * 会导致解码直接失败（`legacy inbound fields are deprecated ...`），
 * 设备上已持久化的历史配置必须能自动迁移。
 */
class ConfigMigratorTest {

    @Test
    fun migrate_removesLegacyInboundFields() {
        val legacy = """
            {
              "inbounds": [
                { "type": "tun", "tag": "tun-in", "sniff": true, "sniff_override_destination": true },
                { "type": "mixed", "tag": "mixed-in", "sniff": true, "sniff_timeout": "300ms",
                  "domain_strategy": "prefer_ipv4", "udp_disable_domain_unmapping": true }
              ],
              "route": { "rules": [ { "action": "sniff" } ] }
            }
        """.trimIndent()

        val result = JSONObject(ConfigMigrator.migrate(legacy))
        val inbounds = result.getJSONArray("inbounds")
        for (i in 0 until inbounds.length()) {
            val inbound = inbounds.getJSONObject(i)
            for (field in listOf(
                "sniff", "sniff_override_destination", "sniff_timeout",
                "domain_strategy", "udp_disable_domain_unmapping"
            )) {
                assertFalse("inbounds[$i] 不应再包含 $field", inbound.has(field))
            }
        }
    }

    @Test
    fun migrate_addsSniffActionWhenInboundSniffRemoved() {
        val legacy = """
            {
              "inbounds": [ { "type": "mixed", "tag": "mixed-in", "sniff": true } ],
              "route": { "rules": [ { "ip_is_private": true, "outbound": "direct" } ] }
            }
        """.trimIndent()

        val result = JSONObject(ConfigMigrator.migrate(legacy))
        val rules = result.getJSONObject("route").getJSONArray("rules")
        assertEquals("sniff", rules.getJSONObject(0).getString("action"))
    }

    @Test
    fun migrate_keepsExistingSniffActionWithoutDuplicating() {
        val legacy = """
            {
              "inbounds": [ { "type": "mixed", "tag": "mixed-in", "sniff": true } ],
              "route": { "rules": [ { "action": "sniff" }, { "ip_is_private": true, "outbound": "direct" } ] }
            }
        """.trimIndent()

        val result = JSONObject(ConfigMigrator.migrate(legacy))
        val rules = result.getJSONObject("route").getJSONArray("rules")
        var sniffCount = 0
        for (i in 0 until rules.length()) {
            if (rules.getJSONObject(i).optString("action") == "sniff") sniffCount++
        }
        assertEquals(1, sniffCount)
    }

    @Test
    fun migrate_removesDeprecatedIndependentCache() {
        val legacy = """
            { "dns": { "servers": [], "independent_cache": true, "strategy": "prefer_ipv4" } }
        """.trimIndent()

        val result = JSONObject(ConfigMigrator.migrate(legacy))
        val dns = result.getJSONObject("dns")
        assertFalse(dns.has("independent_cache"))
        assertEquals("prefer_ipv4", dns.getString("strategy"))
    }

    @Test
    fun migrate_convertsLegacyDnsServerFormat() {
        val legacy = """
            {
              "dns": {
                "servers": [
                  { "address": "tls://1.1.1.1", "address_resolver": "local", "address_strategy": "prefer_ipv4" },
                  { "address": "https://dns.google/dns-query", "tag": "g" },
                  { "address": "8.8.8.8" }
                ]
              }
            }
        """.trimIndent()

        val result = JSONObject(ConfigMigrator.migrate(legacy))
        val servers = result.getJSONObject("dns").getJSONArray("servers")

        val first = servers.getJSONObject(0)
        assertEquals("tls", first.getString("type"))
        assertEquals("1.1.1.1", first.getString("server"))
        assertFalse(first.has("address"))
        assertFalse(first.has("address_resolver"))
        assertFalse(first.has("address_strategy"))

        val second = servers.getJSONObject(1)
        assertEquals("https", second.getString("type"))
        assertEquals("dns.google", second.getString("server"))
        assertEquals("/dns-query", second.getString("path"))

        // 不带 scheme 的地址不是 legacy 格式，保持原样
        val third = servers.getJSONObject(2)
        assertFalse(third.has("type"))
    }

    @Test
    fun migrate_removesClashApiSection() {
        val legacy = """
            {
              "experimental": {
                "cache_file": { "enabled": true },
                "clash_api": { "external_controller": "127.0.0.1:9090" }
              }
            }
        """.trimIndent()

        val result = JSONObject(ConfigMigrator.migrate(legacy))
        val experimental = result.getJSONObject("experimental")
        assertFalse(experimental.has("clash_api"))
        assertTrue(experimental.has("cache_file"))
    }

    @Test
    fun migrate_isIdempotent() {
        val legacy = """
            {
              "inbounds": [ { "type": "mixed", "tag": "mixed-in", "sniff": true } ],
              "route": { "rules": [ { "action": "sniff" } ] },
              "dns": { "servers": [], "independent_cache": true }
            }
        """.trimIndent()

        val once = ConfigMigrator.migrate(legacy)
        val twice = ConfigMigrator.migrate(once)
        assertEquals("二次迁移不应再产生变更", once, twice)
    }

    @Test
    fun migrate_keepsInvalidJsonUntouched() {
        val broken = "{ not json"
        assertEquals(broken, ConfigMigrator.migrate(broken))
    }
}
