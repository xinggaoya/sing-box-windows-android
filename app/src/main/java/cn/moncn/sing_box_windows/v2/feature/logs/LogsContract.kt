package cn.moncn.sing_box_windows.v2.feature.logs

import cn.moncn.sing_box_windows.v2.core.arch.UiEffect
import cn.moncn.sing_box_windows.v2.core.arch.UiIntent
import cn.moncn.sing_box_windows.v2.core.arch.UiState
import cn.moncn.sing_box_windows.v2.domain.core.CoreLogEntry

/** 日志级别过滤 */
enum class LogLevelFilter(val label: String, val minLevel: Int) {
    ALL("全部", CoreLogEntry.LEVEL_TRACE),
    INFO("信息", CoreLogEntry.LEVEL_INFO),
    WARN("警告", CoreLogEntry.LEVEL_WARN),
    ERROR("错误", CoreLogEntry.LEVEL_ERROR);

    fun accepts(level: Int): Boolean = level >= minLevel
}

data class LogsUiState(
    val logs: List<CoreLogEntry> = emptyList(),
    val filter: LogLevelFilter = LogLevelFilter.ALL,
    val autoScroll: Boolean = true,
    val error: String? = null
) : UiState {
    val visible: List<CoreLogEntry>
        get() = logs.filter { filter.accepts(it.level) }
}

sealed interface LogsIntent : UiIntent {
    data class SetFilter(val filter: LogLevelFilter) : LogsIntent
    data class SetAutoScroll(val enabled: Boolean) : LogsIntent
    data object Clear : LogsIntent
    data object ClearError : LogsIntent
}

sealed interface LogsEffect : UiEffect
