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
 * ### 内存要点
 *
 * libbox 的 gomobile 绑定中，每��� `next()` 返回的对象都是一个 **Java 代理**，
 * 它持有对应的 Go 对象引用；只有当 **ART GC 回收该代理**（PhantomReference 入队）后，
 * Go 侧的引用才会被释放。由于 ART 的 GC 只在 Java 堆接近上限时才触发，
 * 高频流持续产生的代理会不断堆积，导致 **进程 RSS 单调增长且不会回落**。
 *
 * 因此本实现：
 * 1. 把高频流（连接 / 日志）拆成**独立客户端**，按页面可见性**按需订阅**，
 *    不在视野内就不产生任何代理对象；
 * 2. 常驻流只保留状态 / 代理组 / 出站 / 代理模式，均为低频事件驱动；
 * 3. 连接事件在**原��� Map 上就地更新**，不再为每个事件批次复制整张表。
 */
class DefaultCoreGateway : CoreGateway {

    private val statusState = MutableStateFlow<CoreStatus?>(null)
    private val groupsState = MutableStateFlow<List<OutboundGroup>>(emptyList())
    private val outboundsState = MutableStateFlow<List<OutboundGroupItem>>(emptyList())
    private val connectionsState = MutableStateFlow<List<CoreConnection>>(emptyList())
    private val logsState = MutableStateFlow<List<CoreLogEntry>>(emptyList())
    private val clashModesState = MutableStateFlow<List<String>>(emptyList())
    private val currentModeState = MutableStateFlow<String?>(null)
    private val connectionStateState = MutableStateFlow(CoreConnectionState.DISCONNECTED)

    /** 串行化客户端的创建 / 销毁，避免并发创建多个 CommandClient */
    private val clientMutex = Mutex()

    /** 常驻客户端：状态 / 代理组 / 出站 / 代理模式 */
    @Volatile
    private var coreClient: CommandClient? = null

    /** 按需客户端：连接事件 */
    @Volatile
    private var connectionsClient: CommandClient? = null

    /** 按需客户端：内核日志 */
    @Volatile
    private var logsClient: CommandClient? = null

    /** 主动断开标记：用于区分「用户主动断开」与「内核侧异常断开」 */
    @Volatile
    private var userDisconnecting = false

    /** 连接表：就地更新 + 淘汰已关闭连接 + 快照发布节流 */
    private val connectionTable = ConnectionTable()

    /** 速率平滑：内核每秒推送一次，直接展示会明显跳动，这里做加权平均 */
    @Volatile
    private var lastUplink = 0L

    @Volatile
    private var lastDownlink = 0L

    // ==================== 对外状态 ====================

    override val statusFlow: Flow<CoreStatus?> = statusState.asStateFlow()
    override val groupsFlow: Flow<List<OutboundGroup>> = groupsState.asStateFlow()
    override val outboundsFlow: Flow<List<OutboundGroupItem>> = outboundsState.asStateFlow()
    override val connectionsFlow: Flow<List<CoreConnection>> = connectionsState.asStateFlow()
    override val logsFlow: Flow<List<CoreLogEntry>> = logsState.asStateFlow()
    override val clashModesFlow: Flow<List<String>> = clashModesState.asStateFlow()
    override val currentModeFlow: Flow<String?> = currentModeState.asStateFlow()
    override val connectionStateFlow: Flow<CoreConnectionState> = connectionStateState.asStateFlow()

    // ==================== 生命周期 ====================

    override suspend fun connect() {
        clientMutex.withLock {
            if (coreClient != null) return
            connectionStateState.value = CoreConnectionState.CONNECTING
            createClient(
                commands = intArrayOf(
                    Libbox.CommandStatus,
                    Libbox.CommandGroup,
                    Libbox.CommandClashMode,
                    Libbox.CommandOutbounds
                ),
                onCreated = { coreClient = it }
            )
        }
    }

    override suspend fun disconnect() {
        val clients = clientMutex.withLock {
            val list = listOfNotNull(coreClient, connectionsClient, logsClient)
            coreClient = null
            connectionsClient = null
            logsClient = null
            list
        }
        if (clients.isEmpty()) return

        userDisconnecting = true
        try {
            withContext(Dispatchers.IO) {
                clients.forEach { runCatching { it.disconnect() } }
            }
        } finally {
            userDisconnecting = false
        }
        connectionStateState.value = CoreConnectionState.DISCONNECTED
        statusState.value = null
        groupsState.value = emptyList()
        outboundsState.value = emptyList()
        clashModesState.value = emptyList()
        currentModeState.value = null
        connectionTable.clear()
        connectionsState.value = emptyList()
    }

    // ==================== 按需订阅 ====================

    override suspend fun observeConnections(enabled: Boolean) {
        if (!enabled) {
            val client = clientMutex.withLock {
                val existing = connectionsClient
                connectionsClient = null
                existing
            }
            withContext(Dispatchers.IO) { runCatching { client?.disconnect() } }
            return
        }
        clientMutex.withLock {
            if (connectionsClient != null) return
            createClient(
                commands = intArrayOf(Libbox.CommandConnections),
                onCreated = { connectionsClient = it }
            )
        }
    }

    override suspend fun observeLogs(enabled: Boolean) {
        if (!enabled) {
            val client = clientMutex.withLock {
                val existing = logsClient
                logsClient = null
                existing
            }
            withContext(Dispatchers.IO) { runCatching { client?.disconnect() } }
            logsState.value = emptyList()
            return
        }
        clientMutex.withLock {
            if (logsClient != null) return
            createClient(
                commands = intArrayOf(Libbox.CommandLog),
                onCreated = { logsClient = it }
            )
        }
    }

    /**
     * 创建并连接一个命令客户端。
     *
     * 建立失败不能让状态卡在 CONNECTING，否则上层永远等不到重连信号。
     */
    private suspend fun createClient(commands: IntArray, onCreated: (CommandClient) -> Unit) {
        val result = withContext(Dispatchers.IO) {
            runCatching {
                val options = CommandClientOptions().apply {
                    commands.forEach { addCommand(it) }
                    statusInterval = STATUS_INTERVAL_MS
                }
                Libbox.newCommandClient(handler, options).also { it.connect() }
            }
        }
        result.onSuccess { created ->
            onCreated(created)
            if (coreClient != null) {
                connectionStateState.value = CoreConnectionState.CONNECTED
            }
        }.onFailure { error ->
            Log.w(TAG, "connect command client failed (commands=${commands.toList()})", error)
            if (coreClient == null) {
                connectionStateState.value = CoreConnectionState.DISCONNECTED_ERROR
            }
        }
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
            val current = coreClient
            if (current == null) {
                Result.failure(IllegalStateException("内核命令通道未连接"))
            } else {
                runCatching { block(current) }
            }
        }

    // ==================== libbox 回调 ====================

    private val handler = object : CommandClientHandler {
        override fun connected() {
            if (coreClient != null) {
                connectionStateState.value = CoreConnectionState.CONNECTED
            }
        }

        override fun disconnected(message: String?) {
            // 主动断开时不视为异常
            if (userDisconnecting) return
            Log.w(TAG, "command channel disconnected: ${message.orEmpty()}")
            if (coreClient != null) {
                connectionStateState.value = CoreConnectionState.DISCONNECTED_ERROR
            }
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
            // 内容未变化时不更新状态，避免无谓的重组与下游计算
            if (groupsState.value != result) {
                groupsState.value = result
            }
        }

        override fun writeOutbounds(message: OutboundGroupItemIterator?) {
            if (message == null) return
            val result = ArrayList<OutboundGroupItem>()
            while (message.hasNext()) {
                val item = message.next() ?: continue
                result.add(item.toDomain())
            }
            if (outboundsState.value != result) {
                outboundsState.value = result
            }
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
            if (events.reset) {
                connectionTable.clear()
            }
            var structuralChange = false
            while (iterator.hasNext()) {
                if (applyConnectionEvent(iterator.next())) {
                    structuralChange = true
                }
            }
            if (connectionTable.evictExpired(System.currentTimeMillis())) {
                structuralChange = true
            }
            // 连接新增 / 关闭立即生效；仅流量增量按秒节流，避免整表拷贝频率过高
            val snapshot = connectionTable.publishIfNeeded(
                force = structuralChange,
                now = System.currentTimeMillis(),
                minIntervalMs = CONNECTION_PUBLISH_INTERVAL_MS
            )
            if (snapshot != null && connectionsState.value != snapshot) {
                connectionsState.value = snapshot
            }
        }
    }

    /**
     * 应用单个连接事件。
     *
     * @return 是否发生了结构性变化（新增 / 关闭），结构性变化需要立即刷新界面
     */
    private fun applyConnectionEvent(event: ConnectionEvent?): Boolean {
        if (event == null) return false
        val id = event.getID()
        when (event.type) {
            CONNECTION_EVENT_NEW -> {
                val connection = event.connection ?: return false
                connectionTable.onNew(connection.toDomain())
                return true
            }

            CONNECTION_EVENT_UPDATE -> {
                return connectionTable.onTraffic(id, event.uplinkDelta, event.downlinkDelta)
            }

            CONNECTION_EVENT_CLOSED -> {
                return connectionTable.onClosed(id, event.closedAt)
            }
        }
        return false
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

        /** 连接快照的最小发布间隔（毫秒） */
        private const val CONNECTION_PUBLISH_INTERVAL_MS = 1000L

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
