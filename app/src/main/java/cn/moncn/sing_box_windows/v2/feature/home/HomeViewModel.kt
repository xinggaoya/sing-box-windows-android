package cn.moncn.sing_box_windows.v2.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import cn.moncn.sing_box_windows.v2.core.arch.MviViewModel
import cn.moncn.sing_box_windows.v2.data.core.ProcessMemory
import cn.moncn.sing_box_windows.v2.domain.core.MemorySnapshot
import cn.moncn.sing_box_windows.v2.domain.runtime.RuntimeGateway
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class HomeViewModel(
    private val runtimeGateway: RuntimeGateway
) : MviViewModel<HomeIntent, HomeUiState, HomeEffect>(HomeUiState()) {

    init {
        observeRuntime()
    }

    override suspend fun handleIntent(intent: HomeIntent) {
        when (intent) {
            HomeIntent.ConnectClicked -> {
                if (state.value.connecting || state.value.connected) return
                emitEffect(HomeEffect.RequestVpnPermission)
            }

            HomeIntent.DisconnectClicked -> stopVpn()
            is HomeIntent.SwitchMode -> {
                runtimeGateway.switchMode(intent.mode)
                    .onFailure { error ->
                        updateState { current ->
                            current.copy(
                                lastError = error.message ?: "模式切换失败"
                            )
                        }
                    }
            }
            is HomeIntent.VpnPermissionResult -> {
                if (!intent.granted) {
                    updateState { current ->
                        current.copy(
                            lastError = "VPN 权限被拒绝"
                        )
                    }
                    return
                }
                startVpn()
            }

            HomeIntent.ClearError -> {
                updateState { current ->
                    current.copy(lastError = null)
                }
            }
        }
    }

    private fun observeRuntime() {
        viewModelScope.launch {
            runtimeGateway.vpnStateFlow.collectLatest { vpnState ->
                updateState { current ->
                    current.copy(
                        vpnState = vpnState
                    )
                }
            }
        }

        viewModelScope.launch {
            runtimeGateway.coreStatusFlow.collectLatest { status ->
                updateState { current ->
                    current.copy(
                        coreStatus = status,
                        // 与状态推送同频（1 次/秒）采样，避免高频读 /proc
                        memory = ProcessMemory.snapshot()
                    )
                }
            }
        }

        viewModelScope.launch {
            runtimeGateway.modeFlow.collectLatest { mode ->
                updateState { current ->
                    current.copy(
                        currentMode = mode
                    )
                }
            }
        }

        viewModelScope.launch {
            runtimeGateway.modeSupportedFlow.collectLatest { supported ->
                updateState { current ->
                    current.copy(
                        modeSupported = supported
                    )
                }
            }
        }

        viewModelScope.launch {
            runtimeGateway.vpnErrorFlow.collectLatest { error ->
                if (error.isNullOrBlank()) return@collectLatest
                updateState { current ->
                    current.copy(
                        lastError = error
                    )
                }
            }
        }
    }

    private suspend fun startVpn() {
        runCatching { runtimeGateway.startVpn() }
            .onFailure { error ->
                updateState { current ->
                    current.copy(
                        lastError = error.message ?: "启动失败"
                    )
                }
            }
    }

    private suspend fun stopVpn() {
        runCatching { runtimeGateway.stopVpn() }
            .onFailure { error ->
                updateState { current ->
                    current.copy(
                        lastError = error.message ?: "停止失败"
                    )
                }
            }
    }


    /**
     * Intent 处理出现未预期异常时的兜底：写入错误状态，
     * 避免异常只停留在日志里、用户却看不到任何提示。
     */
    override fun onIntentError(intent: HomeIntent, error: Throwable) {
        updateState { current -> current.copy(lastError = error.message ?: "操作失败") }
    }

    companion object {
        fun factory(runtimeGateway: RuntimeGateway): ViewModelProvider.Factory {
            return object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return HomeViewModel(runtimeGateway) as T
                }
            }
        }
    }
}
