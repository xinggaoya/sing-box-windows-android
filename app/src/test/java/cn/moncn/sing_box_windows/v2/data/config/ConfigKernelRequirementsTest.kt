package cn.moncn.sing_box_windows.v2.data.config

import cn.moncn.sing_box_windows.vpn.TunAddressPlan
import cn.moncn.sing_box_windows.vpn.VpnDefaults
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 配置的**内核约束**单元测试。
 *
 * 这些约束来自 sing-box 1.14 的官方行为，`sing-box check`（只做解码）**查不出来**，
 * 但会在内核初始化阶段直接失败，必须由应用侧自行保证：
 *
 * 1. DNS 服务器地址为**域名**时，该服务器必须自带 `domain_resolver`
 *    （`route.default_domain_resolver` 不能替代），否则内核报
 *    `initialize DNS server[N]: missing domain resolver for domain server address`；
 * 2. 路由规则里出现 `hijack-dns` 动作时，配置必须存在 `dns` 段；
 * 3. 规则引用的 `rule_set` / `outbound` 必须真实存在；
 * 4. TUN 地址必须落在子网的可用主机位上，否则内核派生的 DNS 地址会落到广播地址，
 *    系统 DNS 全部被内核丢弃。
 */
class ConfigKernelRequirementsTest {

    private val sampleSubscription = """
        {
          "outbounds": [
            { "type": "shadowsocks", "tag": "node-a", "server": "1.2.3.4", "server_port": 8388, "method": "aes-256-gcm", "password": "secret" }
          ]
        }
    """.trimIndent()

    private fun templateConfig(): JSONObject {
        val result = TemplateConfigBuilder.buildFromSubscription(sampleSubscription)
        assertTrue(result.ok)
        return JSONObject(result.configJson!!)
    }

    @Test
    fun templateConfig_dnsServersWithDomainAddressMustDeclareDomainResolver() {
        val servers = templateConfig().getJSONObject("dns").getJSONArray("servers")
        for (index in 0 until servers.length()) {
            val server = servers.getJSONObject(index)
            val address = server.optString("server")
            if (address.isBlank() || isIpAddress(address)) continue
            assertTrue(
                "DNS 服务器 $address (index=$index) 是域名地址，必须声明 domain_resolver",
                server.optString("domain_resolver").isNotBlank()
            )
        }
    }

    @Test
    fun templateConfig_defaultDomainResolverMustExist() {
        val route = templateConfig().getJSONObject("route")
        val resolver = route.optJSONObject("default_domain_resolver")
        assertTrue("必须声明 route.default_domain_resolver", resolver != null)
        val tag = resolver!!.optString("server")
        assertTrue("default_domain_resolver 必须指向已定义的 DNS 服务器", dnsServerTags().contains(tag))
    }

    @Test
    fun templateConfig_hijackDnsRequiresDnsSection() {
        val config = templateConfig()
        val hasHijack = allRules().any { it.optString("action") == "hijack-dns" }
        if (hasHijack) {
            assertTrue("存在 hijack-dns 动作时必须有 dns 段", config.has("dns"))
        }
    }

    @Test
    fun templateConfig_referencedRuleSetsAndOutboundsMustExist() {
        val config = templateConfig()
        val ruleSetTags = config.getJSONObject("route").optJSONArray("rule_set")
            ?.let { array -> (0 until array.length()).map { array.getJSONObject(it).optString("tag") } }
            ?.toSet()
            ?: emptySet()
        val outboundTags = config.getJSONArray("outbounds")
            .let { array -> (0 until array.length()).map { array.getJSONObject(it).optString("tag") } }
            .toSet()

        allRules().forEach { rule ->
            rule.optJSONArray("rule_set")?.let { array ->
                for (index in 0 until array.length()) {
                    val tag = array.getString(index)
                    assertTrue("规则引用了未定义的 rule_set: $tag", tag in ruleSetTags)
                }
            }
            val outbound = rule.optString("outbound")
            if (outbound.isNotBlank()) {
                assertTrue("规则引用了未定义的 outbound: $outbound", outbound in outboundTags)
            }
        }
    }

    @Test
    fun templateConfig_tunAddressMustYieldUsableDnsAddress() {
        val inbound = templateConfig().getJSONArray("inbounds")
            .let { array -> (0 until array.length()).map { array.getJSONObject(it) } }
            .first { it.optString("type") == "tun" }
        val address = inbound.getJSONArray("address").getString(0)
        val prefix = address.substringAfter('/').toInt()
        val host = address.substringBefore('/')
        val plan = TunAddressPlan.resolveDnsAddress(host, prefix, null)
        val broadcast = broadcastOf(host, prefix)
        assertFalse(
            "下发给系统的 DNS 地址不得为子网广播地址（$broadcast）",
            plan.address == broadcast
        )
    }

    @Test
    fun templateConfig_tunMustUseGvisorStack() {
        val inbound = tunInboundOf(templateConfig())
        assertEquals(
            "system 栈在 Android 上 TCP 全部超时，必须使用 gvisor 栈",
            VpnDefaults.STACK,
            inbound.optString("stack")
        )
    }

    @Test
    fun templateConfig_tunMustNotDeclareDnsAddressExplicitly() {
        val inbound = tunInboundOf(templateConfig())
        assertFalse(
            "显式声明 dns_address 会让内核跳过自动 DNS 劫持（DoT/853 泄漏）",
            inbound.has("dns_address")
        )
    }

    @Test
    fun templateConfig_derivedDnsAddressMustNotBeBroadcast() {
        val inbound = tunInboundOf(templateConfig())
        val address = inbound.getJSONArray("address").getString(0)
        val prefix = address.substringAfter('/').toInt()
        val host = address.substringBefore('/')
        val derived = TunAddressPlan.formatIpv4(TunAddressPlan.parseIpv4(host)!! + 1)
        assertFalse(
            "内核派生的 DNS 地址 $derived 不得为子网广播地址",
            derived == broadcastOf(host, prefix)
        )
    }

    @Test
    fun defaultConfig_hasDnsSectionAndUsableTunAddress() {
        val config = JSONObject(ConfigDefaults.defaultConfigJson())
        assertTrue("兜底配置必须有 dns 段", config.has("dns"))
        val inbound = config.getJSONArray("inbounds").getJSONObject(0)
        val address = inbound.getJSONArray("address").getString(0)
        val prefix = address.substringAfter('/').toInt()
        val host = address.substringBefore('/')
        val plan = TunAddressPlan.resolveDnsAddress(host, prefix, null)
        assertFalse(
            "兜底配置的 DNS 地址不得为子网广播地址",
            plan.address == broadcastOf(host, prefix)
        )
    }

    @Test
    fun migratedLegacyConfig_keepsDomainResolverForDomainAddress() {
        val legacy = """
            {
              "dns": {
                "servers": [
                  { "address": "https://dns.google/dns-query", "address_resolver": "local" },
                  { "type": "udp", "server": "114.114.114.114", "tag": "dns_resolver" }
                ],
                "rules": [ { "action": "route", "server": "dns_resolver" } ],
                "final": "dns_resolver"
              },
              "inbounds": [
                { "type": "tun", "tag": "tun-in", "address": [ "10.0.0.2/30" ], "sniff": true }
              ],
              "outbounds": [ { "type": "direct", "tag": "direct" } ],
              "route": { "rules": [ { "action": "sniff" } ], "final": "direct" }
            }
        """.trimIndent()

        val migrated = JSONObject(ConfigMigrator.migrate(legacy))
        val servers = migrated.getJSONObject("dns").getJSONArray("servers")
        val first = servers.getJSONObject(0)
        assertEquals("https", first.optString("type"))
        assertEquals("dns.google", first.optString("server"))
        assertEquals(
            "legacy address_resolver 必须迁移为 domain_resolver",
            "local",
            first.optString("domain_resolver")
        )
        assertTrue(
            "必须补齐 route.default_domain_resolver",
            migrated.getJSONObject("route").has("default_domain_resolver")
        )
    }

    @Test
    fun migratedLegacyConfig_normalisesTunAddress() {
        val legacy = """
            {
              "inbounds": [
                { "type": "tun", "tag": "tun-in", "address": [ "10.0.0.2/32" ], "stack": "system" }
              ],
              "outbounds": [ { "type": "direct", "tag": "direct" } ],
              "route": { "final": "direct" }
            }
        """.trimIndent()

        val migrated = JSONObject(ConfigMigrator.migrate(legacy))
        val address = migrated.getJSONArray("inbounds").getJSONObject(0)
            .getJSONArray("address").getString(0)
        val host = address.substringBefore('/')
        val prefix = address.substringAfter('/').toInt()
        val plan = TunAddressPlan.resolveDnsAddress(host, prefix, null)
        assertFalse(
            "迁移后 TUN 地址不得让 DNS 落到广播地址",
            plan.address == broadcastOf(host, prefix)
        )
    }

    private fun tunInboundOf(config: JSONObject): JSONObject {
        return config.getJSONArray("inbounds")
            .let { array -> (0 until array.length()).map { array.getJSONObject(it) } }
            .first { it.optString("type") == "tun" }
    }

    private fun allRules(): List<JSONObject> {
        val result = mutableListOf<JSONObject>()
        val config = templateConfig()
        config.optJSONObject("route")?.optJSONArray("rules")?.let { collect(result, it) }
        config.optJSONObject("dns")?.optJSONArray("rules")?.let { collect(result, it) }
        return result
    }

    private fun collect(target: MutableList<JSONObject>, array: JSONArray) {
        for (index in 0 until array.length()) {
            val rule = array.optJSONObject(index) ?: continue
            target.add(rule)
            rule.optJSONArray("rules")?.let { collect(target, it) }
        }
    }

    private fun dnsServerTags(): Set<String> {
        val servers = templateConfig().getJSONObject("dns").getJSONArray("servers")
        return (0 until servers.length()).map { servers.getJSONObject(it).optString("tag") }.toSet()
    }

    private fun isIpAddress(value: String): Boolean {
        val parts = value.split('.')
        if (parts.size != 4) return false
        return parts.all { part ->
            part.isNotEmpty() && part.length <= 3 && part.toIntOrNull()?.let { it in 0..255 } == true
        }
    }

    private fun broadcastOf(host: String, prefix: Int): String {
        val ip = TunAddressPlan.parseIpv4(host)!!
        val mask = if (prefix == 0) 0 else (-1 shl (32 - prefix))
        return TunAddressPlan.formatIpv4(ip and mask or mask.inv())
    }
}
