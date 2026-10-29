package cn.moncn.sing_box_windows.v2.feature.connections

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import cn.moncn.sing_box_windows.v2.core.arch.MviViewModel
import cn.moncn.sing_box_windows.v2.domain.core.CoreGateway
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * 连接列表 ViewModel。
 *
 * 数据来自内核命令通道的事件推送（`CommandConnections`），无需轮询。
 */
class ConnectionsViewModel(
    private val coreGateway: CoreGateway
) : MviViewModel<ConnectionsIntent, ConnectionsUiState, ConnectionsEffect>(ConnectionsUiState()) {

    init {
        viewModelScope.launch {
            coreGateway.connectionsFlow.collectLatest { connections ->
                updateState { current -> current.copy(connections = connections) }
            }
        }
    }

    override suspend fun handleIntent(intent: ConnectionsIntent) {
        when (intent) {
            is ConnectionsIntent.SetFilter -> {
                updateState { current -> current.copy(filter = intent.filter) }
            }

            is ConnectionsIntent.CloseConnection -> {
                coreGateway.closeConnection(intent.id)
                    .onFailure { error -> reportError(error.message ?: "关闭连接失败") }
            }

            ConnectionsIntent.CloseAll -> {
                coreGateway.closeConnections()
                    .onFailure { error -> reportError(error.message ?: "关闭全部连接失败") }
            }

            ConnectionsIntent.ClearError -> {
                updateState { current -> current.copy(error = null) }
            }
        }
    }

    private fun reportError(message: String) {
        updateState { current -> current.copy(error = message) }
    }


    /**
     * Intent 处理出现未预期异常时的兜底：写入错误状态，
     * 避免异常只停留在日志里、用户却看不到任何提示。
     */
    override fun onIntentError(intent: ConnectionsIntent, error: Throwable) {
        updateState { current -> current.copy(error = error.message ?: "操作失败") }
    }

    companion object {
        fun factory(coreGateway: CoreGateway): ViewModelProvider.Factory {
            return object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return ConnectionsViewModel(coreGateway) as T
                }
            }
        }
    }
}
