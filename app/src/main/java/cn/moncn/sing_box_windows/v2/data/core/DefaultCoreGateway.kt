package cn.moncn.sing_box_windows.v2.data.core

import android.util.Log
import cn.moncn.sing_box_windows.v2.domain.core.CoreConnection
import cn.moncn.sing_box_windows.v2.domain.core.CoreConnectionState
import cn.moncn.sing_box_windows.v2.domain.core.CoreGateway
import cn.moncn.sing_box_windows.v2.domain.core.CoreLogEntry
import cn.moncn.sing_box_windows.v2.domain.core.CoreStatus
import cn.moncn.sing_box_windows.v2.domain.core.OutboundGroup
import cn.moncn.sing_box_windows.v2.domain.core.OutboundGroupItem
import io.nekohasekai.libbox.CommandClient
import io.nekohasekai.libbox.CommandClientHandler
import io.nekohasekai.libbox.CommandClientOptions
import io.nekohasekai.libbox.ConnectionEvent
import io.nekohasekai.libbox.ConnectionEvents
import io.nekohasekai.libbox.Libbox
import io.nekohasekai.libbox.LogIterator
import io.nekohasekai.libbox.OutboundGroupItemIterator
import io.nekohasekai.libbox.OutboundGroupIterator
import io.nekohasekai.libbox.StatusMessage
import io.nekohasekai.libbox.StringIterator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/**
 * 基于 libbox 自带 `CommandClient` 的内核网关实现。
 *
 * 取代原先基于 Clash API（HTTP `127.0.0.1:9090` + WebSocket 轮询）的实现：
 * 内核通过官方 daemon gRPC 服务主动推送状态 / 代理组 / 出站 / 连接 / 日志，
 * 应用侧无需任何轮询或 JSON 解析。
 *
 * 线程模型：libbox 回调来自 Go 协程线程，本类全部通过 `MutableStateFlow`
 * （原子更新）写入，天然线程安全；连接 / 断开操作用互斥锁串行化，
 * 避免并发调用创建出多个 `CommandClient`。
 */
class DefaultCoreGateway : CoreGateway {

    private val statusState = MutableStateFlow<CoreStatus?>(null)
    private val groupsState = MutableStateFlow<List<OutboundGroup>>(emptyList())
    private val outboundsState = MutableStateFlow<List<OutboundGroupItem>>(emptyList())
    private val connectionsState = MutableStateFlow<Map<String, CoreConnection>>(emptyMap())
    private val logsState = MutableStateFlow<List<CoreLogEntry>>(emptyList())
    private val clashModesState = MutableStateFlow<List<String>>(emptyList())
    private val currentModeState = MutableStateFlow<String?>(null)
    private val connectionStateState = MutableStateFlow(CoreConnectionState.DISCONNECTED)

    /** 串行化 connect / disconnect，防止并发创建多个 CommandClient */
    private val clientMutex = Mutex()

    @Volatile
    private var client: CommandClient? = null

    /** 主动断开标记：用于区分「用户主动断开」与「内核侧异常断开」 */
    @Volatile
    private var userDisconnecting = false

    /** 速率平滑：内核每秒推送一次，直接展示会明显跳动，这里做加权平均 */
    @Volatile
    private var lastUplink = 0L

    @Volatile
    private var lastDownlink = 0L

    // ==================== 对外状态 ====================

    override val statusFlow: Flow<CoreStatus?> = statusState.asStateFlow()
    override val groupsFlow: Flow<List<OutboundGroup>> = groupsState.asStateFlow()
    override val outboundsFlow: Flow<List<OutboundGroupItem>> = outboundsState.asStateFlow()
    override val connectionsFlow: Flow<List<CoreConnection>> =
        connectionsState.map { it.values.toList() }

    override val logsFlow: Flow<List<CoreLogEntry>> = logsState.asStateFlow()
    override val clashModesFlow: Flow<List<String>> = clashModesState.asStateFlow()
    override val currentModeFlow: Flow<String?> = currentModeState.asStateFlow()
    override val connectionStateFlow: Flow<CoreConnectionState> = connectionStateState.asStateFlow()

    // ==================== 生命周期 ====================

    override suspend fun connect() {
        clientMutex.withLock {
            if (client != null) return
            connectionStateState.value = CoreConnectionState.CONNECTING
            val result = withContext(Dispatchers.IO) {
                runCatching {
                    val options = CommandClientOptions().apply {
                        addCommand(Libbox.CommandStatus)
                        addCommand(Libbox.CommandGroup)
                        addCommand(Libbox.CommandClashMode)
                        addCommand(Libbox.CommandConnections)
                        addCommand(Libbox.CommandOutbounds)
                        addCommand(Libbox.CommandLog)
                        statusInterval = STATUS_INTERVAL_MS
                    }
                    Libbox.newCommandClient(handler, options).also { it.connect() }
                }
            }
            result.onSuccess { created ->
                client = created
                connectionStateState.value = CoreConnectionState.CONNECTED
            }.onFailure { error ->
                // 建立失败不能让状态卡在 CONNECTING，否则上层永远等不到重连信号
                Log.w(TAG, "connect command client failed", error)
                client = null
                connectionStateState.value = CoreConnectionState.DISCONNECTED_ERROR
            }
        }
    }

    override suspend fun disconnect() {
        val current = clientMutex.withLock {
            val existing = client
            client = null
            existing
        } ?: return

        userDisconnecting = true
        try {
            withContext(Dispatchers.IO) {
                runCatching { current.disconnect() }
            }
        } finally {
            userDisconnecting = false
        }
        connectionStateState.value = CoreConnectionState.DISCONNECTED
        statusState.value = null
        groupsState.value = emptyList()
        outboundsState.value = emptyList()
        connectionsState.value = emptyMap()
        clashModesState.value = emptyList()
        currentModeState.value = null
    }

    // ==================== 动作 ====================

    override suspend fun selectOutbound(groupTag: String, outboundTag: String): Result<Unit> =
        call { it.selectOutbound(groupTag, outboundTag) }

    override suspend fun urlTest(outboundTag: String): Result<Unit> =
        call { it.urlTest(outboundTag) }

    override suspend fun setClashMode(mode: String): Result<Unit> =
        call { it.setClashMode(mode) }

    override suspend fun setGroupExpanded(groupTag: String, expanded: Boolean): Result<Unit> =
        call { it.setGroupExpand(groupTag, expanded) }

    override suspend fun closeConnection(connectionId: String): Result<Unit> =
        call { it.closeConnection(connectionId) }

    override suspend fun closeConnections(): Result<Unit> =
        call { it.closeConnections() }

    override suspend fun clearLogs(): Result<Unit> {
        // 内核侧清空缓冲，同时清空本地镜像
        logsState.value = emptyList()
        return call { it.clearLogs() }
    }

    override suspend fun reload(): Result<Unit> =
        call { it.serviceReload() }

    private suspend inline fun call(crossinline block: (CommandClient) -> Unit): Result<Unit> =
        withContext(Dispatchers.IO) {
            val current = client
            if (current == null) {
                Result.failure(IllegalStateException("内核命令通道未连接"))
            } else {
                runCatching { block(current) }
            }
        }

    // ==================== libbox 回调 ====================

    private val handler = object : CommandClientHandler {
        override fun connected() {
            connectionStateState.value = CoreConnectionState.CONNECTED
        }

        override fun disconnected(message: String?) {
            // 主动断开时不视为异常
            if (userDisconnecting) return
            Log.w(TAG, "command channel disconnected: ${message.orEmpty()}")
            connectionStateState.value = CoreConnectionState.DISCONNECTED_ERROR
        }

        override fun setDefaultLogLevel(level: Int) = Unit

        override fun clearLogs() {
            logsState.value = emptyList()
        }

        override fun writeLogs(messageList: LogIterator?) {
            if (messageList == null) return
            val batch = ArrayList<CoreLogEntry>(messageList.len())
            while (messageList.hasNext()) {
                val entry = messageList.next() ?: continue
                batch.add(CoreLogEntry(entry.level, entry.message))
            }
            if (batch.isEmpty()) return
            logsState.update { (it + batch).takeLast(MAX_LOG_ENTRIES) }
        }

        override fun writeStatus(message: StatusMessage?) {
            if (message == null) return
            val uplink = smoothSpeed(message.uplink, lastUplink)
            val downlink = smoothSpeed(message.downlink, lastDownlink)
            lastUplink = uplink
            lastDownlink = downlink
            statusState.value = CoreStatus(
                trafficAvailable = message.trafficAvailable,
                uplink = uplink,
                downlink = downlink,
                uplinkTotal = message.uplinkTotal,
                downlinkTotal = message.downlinkTotal,
                memory = message.memory,
                goroutines = message.goroutines,
                connectionsIn = message.connectionsIn,
                connectionsOut = message.connectionsOut
            )
        }

        override fun writeGroups(message: OutboundGroupIterator?) {
            if (message == null) return
            val result = ArrayList<OutboundGroup>()
            while (message.hasNext()) {
                val group = message.next() ?: continue
                val items = ArrayList<OutboundGroupItem>()
                group.items?.let { iterator ->
                    while (iterator.hasNext()) {
                        val item = iterator.next() ?: continue
                        items.add(item.toDomain())
                    }
                }
                result.add(
                    OutboundGroup(
                        tag = group.tag,
                        type = group.type,
                        selectable = group.selectable,
                        selected = group.selected,
                        expanded = group.isExpand,
                        items = items
                    )
                )
            }
            groupsState.value = result
        }

        override fun writeOutbounds(message: OutboundGroupItemIterator?) {
            if (message == null) return
            val result = ArrayList<OutboundGroupItem>()
            while (message.hasNext()) {
                val item = message.next() ?: continue
                result.add(item.toDomain())
            }
            outboundsState.value = result
        }

        override fun initializeClashMode(modeList: StringIterator?, currentMode: String?) {
            val modes = ArrayList<String>()
            modeList?.let {
                while (it.hasNext()) {
                    modes.add(it.next() ?: continue)
                }
            }
            clashModesState.value = modes
            currentModeState.value = currentMode
        }

        override fun updateClashMode(newMode: String?) {
            currentModeState.value = newMode
        }

        override fun writeConnectionEvents(events: ConnectionEvents?) {
            if (events == null) return
            val iterator = events.iterator() ?: return
            connectionsState.update { current ->
                val snapshot = if (events.reset) {
                    LinkedHashMap<String, CoreConnection>()
                } else {
                    LinkedHashMap(current)
                }
                while (iterator.hasNext()) {
                    applyConnectionEvent(iterator.next(), snapshot)
                }
                snapshot
            }
        }
    }

    private fun applyConnectionEvent(
        event: ConnectionEvent?,
        snapshot: MutableMap<String, CoreConnection>
    ) {
        if (event == null) return
        val id = event.getID()
        when (event.type) {
            CONNECTION_EVENT_NEW -> {
                val connection = event.connection ?: return
                snapshot[id] = connection.toDomain()
            }

            CONNECTION_EVENT_UPDATE -> {
                val existing = snapshot[id] ?: return
                snapshot[id] = existing.copy(
                    uplink = event.uplinkDelta,
                    downlink = event.downlinkDelta,
                    uplinkTotal = existing.uplinkTotal + event.uplinkDelta,
                    downlinkTotal = existing.downlinkTotal + event.downlinkDelta
                )
            }

            CONNECTION_EVENT_CLOSED -> {
                val existing = snapshot[id] ?: return
                snapshot[id] = existing.copy(
                    closedAt = event.closedAt,
                    uplink = 0L,
                    downlink = 0L
                )
            }
        }
    }

    private fun io.nekohasekai.libbox.OutboundGroupItem.toDomain() = OutboundGroupItem(
        tag = tag,
        type = type,
        urlTestTime = getURLTestTime(),
        urlTestDelay = getURLTestDelay()
    )

    private fun io.nekohasekai.libbox.Connection.toDomain() = CoreConnection(
        id = getID(),
        inbound = inbound,
        inboundType = inboundType,
        network = network,
        source = source,
        destination = destination,
        domain = domain,
        protocol = protocol,
        user = user,
        fromOutbound = fromOutbound,
        outbound = outbound,
        outboundType = outboundType,
        rule = rule,
        createdAt = createdAt,
        closedAt = closedAt,
        uplink = uplink,
        downlink = downlink,
        uplinkTotal = uplinkTotal,
        downlinkTotal = downlinkTotal
    )

    companion object {
        private const val TAG = "DefaultCoreGateway"

        /** 状态推送间隔（毫秒） */
        private const val STATUS_INTERVAL_MS = 1000L

        /** 本地保留的最大日志条数 */
        private const val MAX_LOG_ENTRIES = 500

        private const val CONNECTION_EVENT_NEW = 0
        private const val CONNECTION_EVENT_UPDATE = 1
        private const val CONNECTION_EVENT_CLOSED = 2
    }
}

/** 指数平滑：新值占 30%，历史值占 70% */
private fun smoothSpeed(current: Long, previous: Long): Long {
    if (current <= 0L) return 0L
    if (previous <= 0L) return current
    return (previous * 7 + current * 3) / 10
}
