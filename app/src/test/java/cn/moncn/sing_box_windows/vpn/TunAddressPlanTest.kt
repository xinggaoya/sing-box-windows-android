package cn.moncn.sing_box_windows.vpn

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * TUN 地址规划单元测试。
 *
 * 回归目标：内核按「TUN 地址 + 1」派生下发给系统的 DNS 服务器地址，
 * 对 /30 前缀该值恰好是子网广播地址，会被 Linux 内核丢弃，
 * 导致系统 DNS 完全不可用（WiFi 感叹号 / 全应用无网络 / 无法分流）。
 */
class TunAddressPlanTest {

    @Test
    fun slash30_withHostTwo_rejectsBroadcastAddress() {
        // 10.0.0.2/30 的广播地址是 10.0.0.3，内核派生值正是它 —— 必须被拒绝
        val plan = TunAddressPlan.resolveDnsAddress("10.0.0.2", 30, "10.0.0.3")
        assertEquals("10.0.0.1", plan.address)
        assertFalse(plan.hostRouteRequired)
    }

    @Test
    fun slash30_withHostOne_keepsSuggestedAddress() {
        val plan = TunAddressPlan.resolveDnsAddress("10.0.0.1", 30, "10.0.0.2")
        assertEquals("10.0.0.2", plan.address)
        assertFalse(plan.hostRouteRequired)
    }

    @Test
    fun slash24_neverReturnsBroadcastAddress() {
        val plan = TunAddressPlan.resolveDnsAddress("192.168.7.5", 24, "192.168.7.255")
        assertEquals("192.168.7.1", plan.address)
    }

    @Test
    fun slash30_networkAddressIsRejected() {
        // 网络地址 10.0.0.0/30 本身不可用作主机地址
        val plan = TunAddressPlan.resolveDnsAddress("10.0.0.0", 30, "10.0.0.0")
        assertEquals("10.0.0.1", plan.address)
    }

    @Test
    fun slash31_requiresHostRoute() {
        val plan = TunAddressPlan.resolveDnsAddress("10.0.0.2", 31, "10.0.0.3")
        assertTrue("前缀 >= 31 需要补主机路由", plan.hostRouteRequired)
    }

    @Test
    fun unparsableAddress_fallsBackToSuggestion() {
        val plan = TunAddressPlan.resolveDnsAddress("not-an-ip", 30, "10.0.0.2")
        assertEquals("10.0.0.2", plan.address)
        assertTrue(plan.hostRouteRequired)
    }

    @Test
    fun parseAndFormatIpv4_roundTrips() {
        listOf("0.0.0.0", "10.0.0.1", "192.168.1.255", "255.255.255.255").forEach { value ->
            val parsed = TunAddressPlan.parseIpv4(value)
            assertTrue("应能解析 $value", parsed != null)
            assertEquals(value, TunAddressPlan.formatIpv4(parsed!!))
        }
        assertEquals(null, TunAddressPlan.parseIpv4("10.0.0"))
        assertEquals(null, TunAddressPlan.parseIpv4("10.0.0.256"))
        assertEquals(null, TunAddressPlan.parseIpv4("dns.alidns.com"))
    }

    @Test
    fun defaultTunAddress_yieldsUsableDnsAddress() {
        val plan = TunAddressPlan.resolveDnsAddress(
            VpnDefaults.ADDRESS,
            VpnDefaults.PREFIX,
            TunAddressPlan.formatIpv4(
                TunAddressPlan.parseIpv4(VpnDefaults.ADDRESS)!! + 1
            )
        )
        assertEquals(VpnDefaults.DNS, plan.address)
        val broadcast = broadcastOf(VpnDefaults.ADDRESS, VpnDefaults.PREFIX)
        assertFalse("默认 DNS 地址不得是子网广播地址", plan.address == broadcast)
        assertEquals(VpnDefaults.DNS, plan.address)
    }

    private fun broadcastOf(host: String, prefix: Int): String {
        val ip = TunAddressPlan.parseIpv4(host)!!
        val mask = if (prefix == 0) 0 else (-1 shl (32 - prefix))
        return TunAddressPlan.formatIpv4(ip and mask or mask.inv())
    }
}
