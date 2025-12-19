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
    val delayMs: Int?
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

    var groups by mutableStateOf<List<OutboundGroupModel>>(emptyList())
        private set

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
                    items.add(
                        OutboundItemModel(
                            tag = item.tag,
                            type = item.type,
                            delayMs = delay
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
            mainHandler.post { groups = groupList }
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
    }

    fun stop() {
        runCatching { client?.disconnect() }
        client = null
        mainHandler.post { groups = emptyList() }
    }

    fun select(groupTag: String, outboundTag: String) {
        runCatching { client?.selectOutbound(groupTag, outboundTag) }
    }
}
