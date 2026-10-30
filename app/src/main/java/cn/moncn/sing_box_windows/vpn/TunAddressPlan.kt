package cn.moncn.sing_box_windows.vpn

/**
 * TUN 地址规划。
 *
 * ## 为什么需要它
 *
 * sing-box / libbox 会把「TUN 网卡地址 + 1」作为下发给系统的 DNS 服务器地址。
 * 对 `/30` 这类前缀，`+1` 恰好落在**子网广播地址**上（例如 `10.0.0.2/30` 的广播地址是
 * `10.0.0.3`），而 Linux 内核会直接丢弃发往子网广播地址的报文：
 *
 * ```
 * local 10.0.0.2 dev tun0 table local proto kernel scope host src 10.0.0.2
 * broadcast 10.0.0.3 dev tun0 table local proto kernel scope link src 10.0.0.2
 * ```
 *
 * 结果是系统的 DNS 查询根本没有进入 TUN，内核里的 DNS 模块从未被触发，
 * 表现为：WiFi 显示「无网络」感叹号、所有应用都提示未知主机、
 * 基于域名的分流规则全部失效。
 *
 * 这里基于**实际下发给内核的 TUN 地址与前缀**重新推导一个可用的主机地址，
 * 保证 DNS 查询一定能被路由到 TUN 并被内核接管。
 */
object TunAddressPlan {

    /**
     * DNS 服务器地址规划结果。
     *
     * @param address 下发给系统的 DNS 服务器地址
     * @param hostRouteRequired 是否需要额外为该地址补一条 /32 主机路由
     *        （前缀 >= 31 时子网内没有可用主机位，只能依赖主机路由）
     */
    data class DnsPlan(
        val address: String,
        val hostRouteRequired: Boolean
    )

    /**
     * 计算应当下发给系统的 DNS 服务器地址。
     *
     * @param tunAddress 内核实际使用的 TUN 地址（不含前缀）
     * @param tunPrefix TUN 地址的前缀长度
     * @param suggested 内核按「地址 + 1」派生出来的建议值
     */
    fun resolveDnsAddress(
        tunAddress: String?,
        tunPrefix: Int,
        suggested: String?
    ): DnsPlan {
        val tunIp = tunAddress?.trim()?.let { parseIpv4(it) }
        if (tunIp == null || tunPrefix !in 0..32) {
            // 无法解析时只能沿用内核建议值（并由调用方补主机路由兜底）
            val fallback = suggested?.trim().orEmpty()
            return if (fallback.isEmpty()) DnsPlan("", false) else DnsPlan(fallback, true)
        }

        // 前缀 >= 31 时子网内没有第二个可用主机位，只能靠主机路由把 DNS 地址指到 TUN
        if (tunPrefix >= 31) {
            val fallback = suggested?.trim().orEmpty().ifEmpty { formatIpv4(tunIp + 1) }
            return DnsPlan(fallback, true)
        }

        val mask = prefixToMask(tunPrefix)
        val network = tunIp and mask
        val broadcast = network or mask.inv()
        val tunHost = tunIp and mask.inv()

        // 子网内除 TUN 自身之外的所有可用主机位
        val usableHosts = (network + 1 until broadcast).filter { it != tunHost }

        // 优先采用内核建议值，前提是它确实是一个可用主机地址
        val suggestedIp = suggested?.trim()?.let { parseIpv4(it) }
        if (suggestedIp != null && suggestedIp in usableHosts) {
            return DnsPlan(formatIpv4(suggestedIp), false)
        }

        val chosen = usableHosts.firstOrNull() ?: (tunIp + 1)
        return DnsPlan(formatIpv4(chosen), false)
    }

    /** 解析点分十进制 IPv4，失败返回 null */
    fun parseIpv4(value: String): Int? {
        val parts = value.split('.')
        if (parts.size != 4) return null
        var result = 0
        for (part in parts) {
            if (part.isEmpty() || part.length > 3) return null
            val octet = part.toIntOrNull() ?: return null
            if (octet !in 0..255) return null
            result = (result shl 8) or octet
        }
        return result
    }

    /** 整数形式转点分十进制 IPv4 */
    fun formatIpv4(value: Int): String {
        return listOf(
            (value ushr 24) and 0xFF,
            (value ushr 16) and 0xFF,
            (value ushr 8) and 0xFF,
            value and 0xFF
        ).joinToString(".")
    }

    private fun prefixToMask(prefix: Int): Int {
        if (prefix == 0) return 0
        return (-1 shl (32 - prefix))
    }
}
