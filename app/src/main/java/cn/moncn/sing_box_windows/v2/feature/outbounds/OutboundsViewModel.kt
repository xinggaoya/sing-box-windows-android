package cn.moncn.sing_box_windows.v2.feature.outbounds

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import cn.moncn.sing_box_windows.v2.core.arch.MviViewModel
import cn.moncn.sing_box_windows.v2.domain.core.CoreGateway
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * 出站列表 ViewModel。
 *
 * 出站快照由内核命令通道推送（`CommandOutbounds`），
 * 测速走内核原生 `urlTest`，延迟结果由内核维护。
 */
class OutboundsViewModel(
    private val coreGateway: CoreGateway
) : MviViewModel<OutboundsIntent, OutboundsUiState, OutboundsEffect>(OutboundsUiState()) {

    init {
        viewModelScope.launch {
            coreGateway.outboundsFlow.collectLatest { outbounds ->
                updateState { current -> current.copy(outbounds = outbounds) }
            }
        }
    }

    override suspend fun handleIntent(intent: OutboundsIntent) {
        when (intent) {
            is OutboundsIntent.Test -> {
                updateState { current -> current.copy(testing = current.testing + intent.tag) }
                coreGateway.urlTest(intent.tag)
                    .onFailure { error ->
                        updateState { current ->
                            current.copy(error = error.message ?: "测速失败")
                        }
                    }
                updateState { current -> current.copy(testing = current.testing - intent.tag) }
            }

            OutboundsIntent.TestAll -> {
                val tags = state.value.outbounds.map { it.tag }
                updateState { current -> current.copy(testing = tags.toSet()) }
                tags.forEach { tag ->
                    coreGateway.urlTest(tag)
                }
                updateState { current -> current.copy(testing = emptySet()) }
            }

            OutboundsIntent.ClearError -> {
                updateState { current -> current.copy(error = null) }
            }
        }
    }

    companion object {
        fun factory(coreGateway: CoreGateway): ViewModelProvider.Factory {
            return object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return OutboundsViewModel(coreGateway) as T
                }
            }
        }
    }
}
