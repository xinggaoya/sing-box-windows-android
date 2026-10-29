package cn.moncn.sing_box_windows.v2.feature.logs

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import cn.moncn.sing_box_windows.v2.core.arch.MviViewModel
import cn.moncn.sing_box_windows.v2.domain.core.CoreGateway
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * 内核日志 ViewModel。
 *
 * 日志由内核命令通道通过 `CommandLog` 主动推送。
 */
class LogsViewModel(
    private val coreGateway: CoreGateway
) : MviViewModel<LogsIntent, LogsUiState, LogsEffect>(LogsUiState()) {

    init {
        viewModelScope.launch {
            coreGateway.logsFlow.collectLatest { logs ->
                updateState { current -> current.copy(logs = logs) }
            }
        }
    }

    override suspend fun handleIntent(intent: LogsIntent) {
        when (intent) {
            is LogsIntent.SetFilter -> {
                updateState { current -> current.copy(filter = intent.filter) }
            }

            is LogsIntent.SetAutoScroll -> {
                updateState { current -> current.copy(autoScroll = intent.enabled) }
            }

            LogsIntent.Clear -> {
                coreGateway.clearLogs()
                    .onFailure { error ->
                        updateState { current ->
                            current.copy(error = error.message ?: "清空日志失败")
                        }
                    }
            }

            LogsIntent.ClearError -> {
                updateState { current -> current.copy(error = null) }
            }
        }
    }

    companion object {
        fun factory(coreGateway: CoreGateway): ViewModelProvider.Factory {
            return object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return LogsViewModel(coreGateway) as T
                }
            }
        }
    }
}
