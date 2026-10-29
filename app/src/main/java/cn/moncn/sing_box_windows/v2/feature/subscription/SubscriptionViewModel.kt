package cn.moncn.sing_box_windows.v2.feature.subscription

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import cn.moncn.sing_box_windows.v2.core.arch.MviViewModel
import cn.moncn.sing_box_windows.v2.domain.subscription.SubscriptionGateway

class SubscriptionViewModel(
    private val gateway: SubscriptionGateway
) : MviViewModel<SubscriptionIntent, SubscriptionUiState, Nothing>(SubscriptionUiState()) {

    init {
        submitIntent(SubscriptionIntent.Load)
    }

    override suspend fun handleIntent(intent: SubscriptionIntent) {
        when (intent) {
            SubscriptionIntent.Load -> loadSubscriptions()
            is SubscriptionIntent.AddRemote -> addRemote(intent.name, intent.url)
            is SubscriptionIntent.AddLocal -> addLocal(intent.name, intent.content)
            is SubscriptionIntent.Activate -> activate(intent.id)
            is SubscriptionIntent.Sync -> sync(intent.id)
            is SubscriptionIntent.Remove -> remove(intent.id)
            is SubscriptionIntent.Edit -> edit(intent.id, intent.name, intent.url)
            SubscriptionIntent.ClearError -> {
                updateState { current -> current.copy(error = null) }
            }
            SubscriptionIntent.ClearMessage -> {
                updateState { current -> current.copy(message = null) }
            }
        }
    }

    private suspend fun loadSubscriptions() {
        updateState { current -> current.copy(isLoading = true, error = null, message = null) }
        runCatching { gateway.load() }
            .onSuccess { state ->
                updateState { current ->
                    current.copy(
                        subscriptions = state,
                        isLoading = false
                    )
                }
            }
            .onFailure { error ->
                updateState { current ->
                    current.copy(
                        isLoading = false,
                        error = error.message ?: "加载订阅失败"
                    )
                }
            }
    }

    private suspend fun addRemote(name: String, url: String) {
        updateState { current -> current.copy(message = null, error = null) }
        runCatching { gateway.addAndActivate(name, url) }
            .onSuccess { result ->
                updateState { current ->
                    current.copy(
                        subscriptions = result.state,
                        message = result.message,
                        error = if (result.ok) null else result.message
                    )
                }
            }
            .onFailure { error ->
                updateState { current ->
                    current.copy(
                        error = error.message ?: "添加订阅失败"
                    )
                }
            }
    }

    private suspend fun addLocal(name: String, content: String) {
        updateState { current -> current.copy(message = null, error = null) }
        runCatching { gateway.importLocal(name, content) }
            .onSuccess { result ->
                updateState { current ->
                    current.copy(
                        subscriptions = result.state,
                        message = result.message,
                        error = if (result.ok) null else result.message
                    )
                }
            }
            .onFailure { error ->
                updateState { current ->
                    current.copy(
                        error = error.message ?: "导入本地节点失败"
                    )
                }
            }
    }

    private suspend fun activate(id: String) {
        updateState { current -> current.copy(updatingId = id, message = null, error = null) }
        runCatching { gateway.activate(id) }
            .onSuccess { result ->
                updateState { current ->
                    current.copy(
                        updatingId = null,
                        subscriptions = result.state,
                        message = result.message,
                        error = if (result.ok) null else result.message
                    )
                }
            }
            .onFailure { error ->
                updateState { current ->
                    current.copy(
                        updatingId = null,
                        error = error.message ?: "启用失败"
                    )
                }
            }
    }

    private suspend fun sync(id: String) {
        val selectedId = state.value.subscriptions.selectedId
        updateState { current -> current.copy(updatingId = id, message = null, error = null) }
        runCatching { gateway.sync(id, selectedId) }
            .onSuccess { result ->
                updateState { current ->
                    current.copy(
                        updatingId = null,
                        subscriptions = result.state,
                        message = result.message,
                        error = if (result.ok) null else result.message
                    )
                }
            }
            .onFailure { error ->
                updateState { current ->
                    current.copy(
                        updatingId = null,
                        error = error.message ?: "同步失败"
                    )
                }
            }
    }

    private suspend fun remove(id: String) {
        updateState { current -> current.copy(message = null, error = null) }
        runCatching { gateway.remove(id) }
            .onSuccess { newState ->
                updateState { current ->
                    current.copy(
                        subscriptions = newState,
                        message = "订阅已删除"
                    )
                }
            }
            .onFailure { error ->
                updateState { current ->
                    current.copy(
                        error = error.message ?: "删除失败"
                    )
                }
            }
    }

    private suspend fun edit(id: String, name: String, url: String) {
        updateState { current -> current.copy(message = null, error = null) }
        runCatching { gateway.edit(id, name, url) }
            .onSuccess { result ->
                if (!result.ok) {
                    updateState { current ->
                        current.copy(
                            error = result.message
                        )
                    }
                    return
                }
                var message = result.message
                val nextState = result.state
                if (result.urlChanged && nextState.selectedId == id) {
                    val syncResult = runCatching { gateway.sync(id, nextState.selectedId) }.getOrNull()
                    if (syncResult != null) {
                        message = syncResult.message
                        updateState { current ->
                            current.copy(
                                subscriptions = syncResult.state,
                                message = message,
                                error = if (syncResult.ok) null else syncResult.message
                            )
                        }
                        return
                    }
                }
                updateState { current ->
                    current.copy(
                        subscriptions = nextState,
                        message = message
                    )
                }
            }
            .onFailure { error ->
                updateState { current ->
                    current.copy(
                        error = error.message ?: "编辑失败"
                    )
                }
            }
    }

    private fun now(): Long = System.currentTimeMillis()


    /**
     * Intent 处理出现未预期异常时的兜底：写入错误状态，
     * 避免异常只停留在日志里、用户却看不到任何提示。
     */
    override fun onIntentError(intent: SubscriptionIntent, error: Throwable) {
        updateState { current -> current.copy(error = error.message ?: "操作失败") }
    }

    companion object {
        fun factory(gateway: SubscriptionGateway): ViewModelProvider.Factory {
            return object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return SubscriptionViewModel(gateway) as T
                }
            }
        }
    }
}
