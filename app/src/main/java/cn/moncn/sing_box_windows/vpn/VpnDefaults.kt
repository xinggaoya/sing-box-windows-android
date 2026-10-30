package cn.moncn.sing_box_windows.vpn

object VpnDefaults {
    const val SESSION_NAME = "SingBox"

    /**
     * TUN 网卡地址。
     *
     * sing-box 1.14 起，内核按「`address` 列表中的第一个 IPv4 地址 + 1」派生
     * 下发给系统的 DNS 服务器地址（未显式配置 `dns_address` 时）。
     * 因此本地址必须是 `/30` 子网内的**可用主机地址**：
     *
     * - `10.0.0.2/30` → 派生地址 `10.0.0.3` 恰为该 /30 的**广播地址**，
     *   Linux 内核会直接丢弃发往广播地址的报文，系统 DNS 与 TCP 全部不可用；
     * - `172.18.0.1/30` → 派生地址 `172.18.0.2`，是合法主机地址。
     *
     * 取值与官方文档示例一致（172.18.0.1/30）。
     */
    const val ADDRESS = "172.18.0.1"
    const val PREFIX = 30

    /** 下发给系统的 DNS 服务器地址（与 TUN 同子网的可用主机位） */
    const val DNS = "172.18.0.2"

    /**
     * TUN 的 TCP/IP 栈。
     *
     * 必须使用 `gvisor`（sing-tun 自带的用户态协议栈）：
     * `system` 栈依赖内核 TCP 在 socketpair 上完成握手，实测在 Android 上
     * TCP 连接全部超时（UDP/ICMP 正常），表现为「能解析域名但任何应用都连不上」。
     * sing-box 1.15 起也将废弃 `stack` 选项、默认改用 sing-tun 自己的协议栈。
     */
    const val STACK = "gvisor"

    const val MTU = 1500
}
