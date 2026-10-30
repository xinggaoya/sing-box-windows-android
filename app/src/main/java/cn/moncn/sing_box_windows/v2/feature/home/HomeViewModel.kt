package cn.moncn.sing_box_windows.v2.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import cn.moncn.sing_box_windows.v2.core.arch.MviViewModel
import cn.moncn.sing_box_windows.v2.data.core.ProcessMemory
import cn.moncn.sing_box_windows.v2.domain.core.MemorySnapshot
import cn.moncn.sing_box_windows.v2.domain.runtime.RuntimeGateway
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.isActive
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
        observeProcessMemory()

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
                updateState { current -> current.copy(coreStatus = status) }
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

    /**
     * 采样进程内存分解。
     *
     * 读取 `/proc/self/statm` 很廉价；`/proc/self/smaps_rollup` 需要遍历整个进程的页表，
     * 进程越大越慢，因此：
     * - 廉价指标每次采样都刷新（保证数字跟手）；
     * - 页表类指标（PSS / 私有内存 / 匿名内存）降频采集，期间沿用上一次的结果。
     *
     * 采样统一放在 IO 线程，避免拖垮主线程造成卡顿。
     */
    private fun observeProcessMemory() {
        viewModelScope.launch(Dispatchers.IO) {
            var tick = 0
            while (isActive) {
                val withRollup = tick % ROLLUP_SAMPLE_EVERY_TICKS == 0
                val fresh = ProcessMemory.snapshot(includePageTable = withRollup)
                updateState { current ->
                    current.copy(
                        memory = ProcessMemory.merge(
                            previous = current.memory,
                            fresh = fresh,
                            withPageTable = withRollup
                        )
                    )
                }
                tick++
                delay(MEMORY_SAMPLE_INTERVAL_MS)
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
        /** 内存指标采样间隔（毫秒）：读 /proc 较慢，5 秒足够观察趋势 */
        private const val MEMORY_SAMPLE_INTERVAL_MS = 5_000L

        /**
         * 页表类指标（/proc/self/smaps）的采样降频倍数。
         * 每 3 个 tick（15 秒）读一次，避免频繁遍历进程页表造成卡顿。
         */
        private const val ROLLUP_SAMPLE_EVERY_TICKS = 3

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
