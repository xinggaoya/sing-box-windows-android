package cn.moncn.sing_box_windows.v2.domain.core

import kotlinx.coroutines.flow.Flow

/**
 * 内核命令网关。
 *
 * 抽象 libbox 自带的 `CommandServer` / `CommandClient`：
 * 取代原先基于 Clash API（HTTP + WebSocket 轮询）的实现，
 * 与桌面端 `sing-box-windows` 迁移到 sing-box 官方 API 的方向保持一致。
 *
 * 所有数据均为内核主动推送（事件驱动），无需轮询。
 */
interface CoreGateway {

    /** 内核状态（流量 / 内存 / 连接数），未连接时为 null */
    val statusFlow: Flow<CoreStatus?>

    /** 代理组列表 */
    val groupsFlow: Flow<List<OutboundGroup>>

    /** 出站列表 */
    val outboundsFlow: Flow<List<OutboundGroupItem>>

    /** 活跃连接列表 */
    val connectionsFlow: Flow<List<CoreConnection>>

    /** 内核日志 */
    val logsFlow: Flow<List<CoreLogEntry>>

    /** 可用的代理模式列表 */
    val clashModesFlow: Flow<List<String>>

    /** 当前代理模式 */
    val currentModeFlow: Flow<String?>

    /** 命令通道连接状态 */
    val connectionStateFlow: Flow<CoreConnectionState>

    /** 建立命令通道（幂等） */
    suspend fun connect()

    /** 断开命令通道（幂等） */
    suspend fun disconnect()

    /** 选择代理组中的出站 */
    suspend fun selectOutbound(groupTag: String, outboundTag: String): Result<Unit>

    /** 对指定出站发起测速 */
    suspend fun urlTest(outboundTag: String): Result<Unit>

    /** 切换代理模式 */
    suspend fun setClashMode(mode: String): Result<Unit>

    /** 展开 / 收起代理组 */
    suspend fun setGroupExpanded(groupTag: String, expanded: Boolean): Result<Unit>

    /** 关闭单条连接 */
    suspend fun closeConnection(connectionId: String): Result<Unit>

    /** 关闭全部连接 */
    suspend fun closeConnections(): Result<Unit>

    /** 清空内核日志缓冲 */
    suspend fun clearLogs(): Result<Unit>

    /** 重载内核配置 */
    suspend fun reload(): Result<Unit>
}
