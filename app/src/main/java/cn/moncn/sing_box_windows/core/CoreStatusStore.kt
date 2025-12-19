package cn.moncn.sing_box_windows.core

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import io.nekohasekai.libbox.StatusMessage

data class CoreStatus(
    val memoryBytes: Long,
    val goroutines: Int,
    val connectionsIn: Int,
    val connectionsOut: Int,
    val trafficAvailable: Boolean,
    val uplinkBytes: Long,
    val downlinkBytes: Long,
    val uplinkTotalBytes: Long,
    val downlinkTotalBytes: Long,
    val updatedAt: Long
)

object CoreStatusStore {
    var status by mutableStateOf<CoreStatus?>(null)
        private set

    private const val SPEED_HOLD_MS = 2000L

    private var lastUplinkDisplay = 0L
    private var lastDownlinkDisplay = 0L
    private var lastUplinkNonZeroAt = 0L
    private var lastDownlinkNonZeroAt = 0L

    fun update(status: CoreStatus) {
        val previous = this.status
        val computed = if (status.trafficAvailable && previous != null) {
            // Fallback to delta-based speed when core does not provide instantaneous rates.
            val timeDelta = status.updatedAt - previous.updatedAt
            val uplinkSpeed = pickSpeed(
                raw = status.uplinkBytes,
                total = status.uplinkTotalBytes,
                prevTotal = previous.uplinkTotalBytes,
                timeDeltaMs = timeDelta
            )
            val downlinkSpeed = pickSpeed(
                raw = status.downlinkBytes,
                total = status.downlinkTotalBytes,
                prevTotal = previous.downlinkTotalBytes,
                timeDeltaMs = timeDelta
            )
            val now = status.updatedAt
            val uplinkState = stabilizeSpeed(
                speed = uplinkSpeed,
                lastDisplay = lastUplinkDisplay,
                lastNonZeroAt = lastUplinkNonZeroAt,
                now = now
            )
            val downlinkState = stabilizeSpeed(
                speed = downlinkSpeed,
                lastDisplay = lastDownlinkDisplay,
                lastNonZeroAt = lastDownlinkNonZeroAt,
                now = now
            )
            lastUplinkDisplay = uplinkState.value
            lastDownlinkDisplay = downlinkState.value
            lastUplinkNonZeroAt = uplinkState.lastNonZeroAt
            lastDownlinkNonZeroAt = downlinkState.lastNonZeroAt
            status.copy(
                uplinkBytes = lastUplinkDisplay,
                downlinkBytes = lastDownlinkDisplay
            )
        } else {
            status
        }
        this.status = computed
    }

    fun update(message: StatusMessage) {
        update(
            CoreStatus(
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
        )
    }

    fun reset() {
        status = null
        lastUplinkDisplay = 0
        lastDownlinkDisplay = 0
        lastUplinkNonZeroAt = 0
        lastDownlinkNonZeroAt = 0
    }

    private fun pickSpeed(raw: Long, total: Long, prevTotal: Long, timeDeltaMs: Long): Long {
        if (raw > 0) return raw
        if (timeDeltaMs <= 0) return 0
        val delta = (total - prevTotal).coerceAtLeast(0)
        return (delta * 1000L / timeDeltaMs).coerceAtLeast(0)
    }

    private fun stabilizeSpeed(
        speed: Long,
        lastDisplay: Long,
        lastNonZeroAt: Long,
        now: Long
    ): SpeedState {
        if (speed > 0) {
            val blended = if (lastDisplay > 0) {
                (lastDisplay * 7 + speed * 3) / 10
            } else {
                speed
            }
            return SpeedState(blended, now)
        }
        if (now - lastNonZeroAt <= SPEED_HOLD_MS) {
            return SpeedState(lastDisplay, lastNonZeroAt)
        }
        return SpeedState(0L, lastNonZeroAt)
    }

    private data class SpeedState(
        val value: Long,
        val lastNonZeroAt: Long
    )
}
