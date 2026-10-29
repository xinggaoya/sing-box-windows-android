package cn.moncn.sing_box_windows.v2.core.arch

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * MVI 基类：单向数据流。
 *
 * - `Intent` 统一走 [submitIntent]，由内部 Channel 串行消费，
 *   避免多个 Intent 并发交错导致的竞态（例如重复提交保存）。
 * - `State` 只读暴露，变更必须通过 [updateState]。
 * - `Effect` 用于一次性事件（导航、Toast 等）。
 */
abstract class MviViewModel<I : UiIntent, S : UiState, E : UiEffect>(
    initialState: S
) : ViewModel() {
    private val _state = MutableStateFlow(initialState)
    val state: StateFlow<S> = _state.asStateFlow()

    private val _effect = MutableSharedFlow<E>(
        extraBufferCapacity = 16
    )
    val effect: SharedFlow<E> = _effect.asSharedFlow()

    private val intents = Channel<I>(Channel.UNLIMITED)

    init {
        viewModelScope.launch {
            for (intent in intents) {
                handleIntent(intent)
            }
        }
    }

    /** 提交意图（串行处理） */
    fun submitIntent(intent: I) {
        intents.trySend(intent)
    }

    protected abstract suspend fun handleIntent(intent: I)

    protected fun updateState(reducer: (S) -> S) {
        _state.update(reducer)
    }

    protected suspend fun emitEffect(effect: E) {
        _effect.emit(effect)
    }
}
