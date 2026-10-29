package cn.moncn.sing_box_windows.v2.data.core

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import io.nekohasekai.libbox.Libbox

/**
 * 内核信息（版本号）。
 *
 * 数据直接取自 libbox，不再需要向 Clash API 的 `/version` 轮询。
 */
object CoreInfoStore {
    var version: String? by mutableStateOf(null)
        private set

    /** 读取当前内核版本 */
    fun refresh(): String? {
        val value = runCatching { Libbox.version() }.getOrNull()?.takeIf { it.isNotBlank() }
        version = value
        return value
    }

    fun reset() {
        version = null
    }
}
