package cn.moncn.sing_box_windows.core

import android.os.Handler
import android.os.Looper
import io.nekohasekai.libbox.CommandClient
import io.nekohasekai.libbox.CommandClientHandler
import io.nekohasekai.libbox.CommandClientOptions
import io.nekohasekai.libbox.Connections
import io.nekohasekai.libbox.Libbox
import io.nekohasekai.libbox.OutboundGroupIterator
import io.nekohasekai.libbox.StatusMessage
import io.nekohasekai.libbox.StringIterator
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

data class OutboundItemModel(
    val tag: String,
    val type: String,
    val delayMs: Int?,
    val lastTestAt: Long?,
    val isTesting: Boolean = false
)

data class OutboundGroupModel(
    val tag: String,
    val type: String,
    val selectable: Boolean,
    val selected: String,
    val items: List<OutboundItemModel>
)

object OutboundGroupManager {
    private val mainHandler = Handler(Looper.getMainLooper())
    private var client: CommandClient? = null
    private var urlTestClient: CommandClient? = null
    private var lastTestSnapshot: Map<String, TestSnapshot> = emptyMap()
    private var testingTags by mutableStateOf<Map<String, TestingState>>(emptyMap())

    var groups by mutableStateOf<List<OutboundGroupModel>>(emptyList())
        private set

    data class TestSnapshot(
        val delayMs: Int?,
        val lastTestAt: Long?
    )

    data class TestingState(
        val baseline: TestSnapshot?,
        val startedAt: Long
    )

    private val handler = object : CommandClientHandler {
        override fun connected() = Unit

        override fun disconnected(message: String) {
            mainHandler.post { groups = emptyList() }
        }

        override fun clearLogs() = Unit

        override fun writeLogs(messageList: StringIterator) = Unit

        override fun writeStatus(message: StatusMessage) = Unit

        override fun writeConnections(message: Connections) = Unit

        override fun initializeClashMode(modeList: StringIterator, currentMode: String) = Unit

        override fun updateClashMode(newMode: String) = Unit

        override fun writeGroups(message: OutboundGroupIterator) {
            val groupList = mutableListOf<OutboundGroupModel>()
            while (message.hasNext()) {
                val group = message.next()
                val items = mutableListOf<OutboundItemModel>()
                val itemIterator = group.items
                while (itemIterator.hasNext()) {
                    val item = itemIterator.next()
                    val delay = if (item.urlTestDelay > 0) item.urlTestDelay else null
                    val testTime = if (item.urlTestTime > 0) item.urlTestTime else null
                    items.add(
                        OutboundItemModel(
                            tag = item.tag,
                            type = item.type,
                            delayMs = delay,
                            lastTestAt = testTime
                        )
                    )
                }
                groupList.add(
                    OutboundGroupModel(
                        tag = group.tag,
                        type = group.type,
                        selectable = group.selectable,
                        selected = group.selected,
                        items = items
                    )
                )
            }
            val snapshot = buildTestSnapshot(groupList)
            val updatedTesting = testingTags.filterNot { (tag, state) ->
                val current = snapshot[tag]
                current != null && current != state.baseline
            }
            val normalizedGroups = normalizeTestResults(groupList, snapshot)
            val testingSet = updatedTesting.keys
            val displayedGroups = normalizedGroups.map { group ->
                val updatedItems = group.items.map { item ->
                    item.copy(isTesting = testingSet.contains(item.tag))
                }
                group.copy(items = updatedItems)
            }
            mainHandler.post {
                lastTestSnapshot = snapshot
                testingTags = updatedTesting
                groups = displayedGroups
            }
        }
    }

    fun start() {
        if (client != null) return
        val options = CommandClientOptions().apply {
            command = Libbox.CommandGroup
            statusInterval = 3000
        }
        client = Libbox.newCommandClient(handler, options)
        runCatching { client?.connect() }
        ensureUrlTestClient()
    }

    fun stop() {
        runCatching { client?.disconnect() }
        client = null
        runCatching { urlTestClient?.disconnect() }
        urlTestClient = null
        mainHandler.post { groups = emptyList() }
    }

    fun select(groupTag: String, outboundTag: String) {
        runCatching { client?.selectOutbound(groupTag, outboundTag) }
        mainHandler.post {
            groups = groups.map { group ->
                if (group.tag == groupTag) {
                    group.copy(selected = outboundTag)
                } else {
                    group
                }
            }
        }
        // 主动断开旧连接，让节点切换立即生效，避免需重启才能观察到变化。
        runCatching { client?.closeConnections() }
    }

    fun urlTest(outboundTag: String): Result<Unit> {
        if (client == null) {
            return Result.failure(IllegalStateException("核心未运行"))
        }
        val group = findUrlTestGroup(outboundTag)
            ?: return Result.failure(IllegalStateException("当前配置没有可测速的分组"))
        ensureUrlTestClient()
        val result = runCatching {
            urlTestClient?.urlTest(group.tag) ?: error("测速通道不可用")
        }.recoverCatching {
            client?.urlTest(group.tag) ?: error("测速通道不可用")
        }
        if (result.isSuccess) {
            markTesting(outboundTag)
        }
        return result
    }

    private fun ensureUrlTestClient() {
        if (urlTestClient != null) return
        val options = CommandClientOptions().apply {
            command = Libbox.CommandURLTest
            statusInterval = 1000
        }
        val emptyHandler = object : CommandClientHandler {
            override fun connected() = Unit
            override fun disconnected(message: String) = Unit
            override fun clearLogs() = Unit
            override fun writeLogs(messageList: StringIterator) = Unit
            override fun writeStatus(message: StatusMessage) = Unit
            override fun writeConnections(message: Connections) = Unit
            override fun initializeClashMode(modeList: StringIterator, currentMode: String) = Unit
            override fun updateClashMode(newMode: String) = Unit
            override fun writeGroups(message: OutboundGroupIterator) = Unit
        }
        urlTestClient = Libbox.newCommandClient(emptyHandler, options)
        runCatching { urlTestClient?.connect() }
    }

    private fun findUrlTestGroup(outboundTag: String): OutboundGroupModel? {
        return groups.firstOrNull { group ->
            group.type == "urltest" && group.items.any { it.tag == outboundTag }
        } ?: groups.firstOrNull { it.type == "urltest" }
    }

    private fun buildTestSnapshot(
        groupList: List<OutboundGroupModel>
    ): Map<String, TestSnapshot> {
        val snapshot = mutableMapOf<String, TestSnapshot>()
        groupList.forEach { group ->
            group.items.forEach { item ->
                if (item.delayMs == null && item.lastTestAt == null) return@forEach
                val current = snapshot[item.tag]
                if (current == null || (item.lastTestAt ?: 0L) > (current.lastTestAt ?: 0L)) {
                    snapshot[item.tag] = TestSnapshot(item.delayMs, item.lastTestAt)
                }
            }
        }
        return snapshot
    }

    private fun normalizeTestResults(
        groupList: List<OutboundGroupModel>,
        snapshot: Map<String, TestSnapshot>
    ): List<OutboundGroupModel> {
        return groupList.map { group ->
            val items = group.items.map { item ->
                val resolved = snapshot[item.tag]
                val delayMs = item.delayMs ?: resolved?.delayMs
                val lastTestAt = item.lastTestAt ?: resolved?.lastTestAt
                item.copy(delayMs = delayMs, lastTestAt = lastTestAt)
            }
            group.copy(items = items)
        }
    }

    private fun markTesting(tag: String) {
        val baseline = lastTestSnapshot[tag]
        val startedAt = System.currentTimeMillis()
        testingTags = testingTags + (tag to TestingState(baseline, startedAt))
        refreshTestingState()
        mainHandler.postDelayed({
            val current = testingTags[tag] ?: return@postDelayed
            if (current.startedAt == startedAt) {
                testingTags = testingTags - tag
                refreshTestingState()
            }
        }, 15_000)
    }

    private fun refreshTestingState() {
        val testingSet = testingTags.keys
        mainHandler.post {
            groups = groups.map { group ->
                val items = group.items.map { item ->
                    item.copy(isTesting = testingSet.contains(item.tag))
                }
                group.copy(items = items)
            }
        }
    }
}
