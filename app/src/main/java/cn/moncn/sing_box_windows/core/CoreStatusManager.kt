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

object CoreStatusManager {
    private val mainHandler = Handler(Looper.getMainLooper())
    private var client: CommandClient? = null

    private val handler = object : CommandClientHandler {
        override fun connected() = Unit

        override fun disconnected(message: String) {
            mainHandler.post { CoreStatusStore.reset() }
        }

        override fun clearLogs() = Unit

        override fun writeLogs(messageList: StringIterator) = Unit

        override fun writeStatus(message: StatusMessage) {
            val snapshot = CoreStatus(
                memoryBytes = message.memory,
                goroutines = message.goroutines,
                connectionsIn = message.connectionsIn,
                connectionsOut = message.connectionsOut,
                trafficAvailable = message.trafficAvailable,
                uplinkBytes = message.uplink,
                downlinkBytes = message.downlink,
                uplinkTotalBytes = message.uplinkTotal,
                downlinkTotalBytes = message.downlinkTotal,
                updatedAt = System.currentTimeMillis()
            )
            mainHandler.post { CoreStatusStore.update(snapshot) }
        }

        override fun writeConnections(message: Connections) = Unit

        override fun initializeClashMode(modeList: StringIterator, currentMode: String) = Unit

        override fun updateClashMode(newMode: String) = Unit

        override fun writeGroups(message: OutboundGroupIterator) = Unit
    }

    fun start() {
        if (client != null) return
        val options = CommandClientOptions().apply {
            command = Libbox.CommandStatus
            statusInterval = 1000
        }
        client = Libbox.newCommandClient(handler, options)
        runCatching { client?.connect() }
    }

    fun stop() {
        runCatching { client?.disconnect() }
        client = null
        mainHandler.post { CoreStatusStore.reset() }
    }
}
