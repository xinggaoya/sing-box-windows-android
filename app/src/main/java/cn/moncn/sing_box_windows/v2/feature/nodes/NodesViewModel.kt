package cn.moncn.sing_box_windows.v2.feature.nodes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import cn.moncn.sing_box_windows.v2.core.arch.MviViewModel
import cn.moncn.sing_box_windows.v2.domain.nodes.NodesGateway
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class NodesViewModel(
    private val gateway: NodesGateway
) : MviViewModel<NodesIntent, NodesUiState, Nothing>(NodesUiState()) {

    init {
        viewModelScope.launch {
            gateway.groupsFlow.collectLatest { groups ->
                updateState { current ->
                    current.copy(
                        groups = groups,
                        updatedAt = now()
                    )
                }
            }
        }
    }

    override suspend fun handleIntent(intent: NodesIntent) {
        when (intent) {
            is NodesIntent.Select -> {
                runCatching { gateway.select(intent.groupTag, intent.nodeTag) }
                    .onFailure { error ->
                        updateState { current ->
                            current.copy(
                                error = error.message ?: "节点切换失败",
                                updatedAt = now()
                            )
                        }
                    }
            }
            is NodesIntent.Test -> {
                val result = gateway.test(intent.nodeTag)
                result.onFailure { error ->
                    updateState { current ->
                        current.copy(
                            error = error.message ?: "测速失败",
                            updatedAt = now()
                        )
                    }
                }
            }
            NodesIntent.ClearError -> {
                updateState { current ->
                    current.copy(
                        error = null,
                        updatedAt = now()
                    )
                }
            }
        }
    }

    private fun now(): Long = System.currentTimeMillis()

    companion object {
        fun factory(gateway: NodesGateway): ViewModelProvider.Factory {
            return object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return NodesViewModel(gateway) as T
                }
            }
        }
    }
}
