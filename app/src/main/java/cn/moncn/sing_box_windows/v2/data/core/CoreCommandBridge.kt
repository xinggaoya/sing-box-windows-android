package cn.moncn.sing_box_windows.v2.data.core

import cn.moncn.sing_box_windows.v2.domain.core.CoreGateway

/**
 * 内核命令通道桥接。
 *
 * 持有进程内唯一的 [DefaultCoreGateway]，供 DI 容器与 VPN 服务共享。
 * 内核（CommandServer）与命令客户端（CommandClient）运行在同一进程，
 * 通过 `basePath/command.sock` 的 unix socket 通信。
 */
object CoreCommandBridge {
    val gateway: DefaultCoreGateway by lazy { DefaultCoreGateway() }

    /** 暴露为领域接口 */
    val coreGateway: CoreGateway get() = gateway

    suspend fun connect() = gateway.connect()

    suspend fun disconnect() = gateway.disconnect()
}
