package cn.moncn.sing_box_windows.v2.data.core

/**
 * 内核命令服务端回调。
 *
 * libbox 的 `CommandServerHandler` 由内核在 gRPC 侧触发，
 * 需要回到应用层执行「停止 VPN」「重载配置」等动作。
 */
interface CoreServerCallbacks {
    /** 内核请求停止服务 */
    fun onStopRequested()

    /**
     * 内核请求重载配置。
     * 返回新配置内容；返回 null 表示不重载。
     */
    fun onReloadRequested(): String?
}
