package cn.moncn.sing_box_windows.v2.core.arch

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * MVI 基类：单向数据流。
 *
 * - `Intent` 统一走 [submitIntent]，由内部 Channel 串行消费，
 *   避免多个 Intent 并发交错导致的竞态（例如重复提交保存）。
 * - `State` 只读暴露，变更必须通过 [updateState]。
 * - `Effect` 用于一次性事件（导航、Toast 等），走独立的有缓冲 Channel，
 *   不会因为暂无订阅者而阻塞 Intent 消费循环。
 */
abstract class MviViewModel<I : UiIntent, S : UiState, E : UiEffect>(
    initialState: S
) : ViewModel() {
    private val _state = MutableStateFlow(initialState)
    val state: StateFlow<S> = _state.asStateFlow()

    private val effectChannel = Channel<E>(Channel.BUFFERED)
    val effect: Flow<E> = effectChannel.receiveAsFlow()

    private val intents = Channel<I>(Channel.UNLIMITED)

    init {
        viewModelScope.launch {
            for (intent in intents) {
                try {
                    handleIntent(intent)
                } catch (cancellation: CancellationException) {
                    throw cancellation
                } catch (error: Throwable) {
                    // 兜底：消费协程一旦异常退出，后续 Intent 会被静默吞掉，
                    // 因此这里必须捕获并交给子类处理，而不是让协程终止。
                    Log.e(TAG, "intent handling failed: $intent", error)
                    onIntentError(intent, error)
                }
            }
        }
    }

    /** 提交意图（串行处理） */
    fun submitIntent(intent: I) {
        intents.trySend(intent)
    }

    protected abstract suspend fun handleIntent(intent: I)

    /**
     * Intent 处理异常回调。
     * 默认仅记录日志；需要把错误呈现给用户的页面可覆写并写入 State。
     */
    protected open fun onIntentError(intent: I, error: Throwable) = Unit

    protected fun updateState(reducer: (S) -> S) {
        _state.update(reducer)
    }

    protected suspend fun emitEffect(effect: E) {
        effectChannel.send(effect)
    }

    override fun onCleared() {
        intents.close()
        effectChannel.close()
        super.onCleared()
    }

    private companion object {
        const val TAG = "MviViewModel"
    }
}
