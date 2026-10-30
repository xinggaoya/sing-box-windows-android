package cn.moncn.sing_box_windows.v2.data.runtime

import android.content.Context
import android.util.Log
import androidx.compose.runtime.snapshotFlow
import cn.moncn.sing_box_windows.v2.data.config.ConfigRepository
import cn.moncn.sing_box_windows.v2.data.config.SettingsRepository
import cn.moncn.sing_box_windows.v2.data.core.CoreCommandBridge
import cn.moncn.sing_box_windows.v2.data.core.CoreRuntimeCoordinator
import cn.moncn.sing_box_windows.v2.domain.core.CoreClashMode
import cn.moncn.sing_box_windows.v2.domain.core.CoreConnectionState
import cn.moncn.sing_box_windows.v2.domain.core.CoreStatus
import cn.moncn.sing_box_windows.v2.domain.runtime.RuntimeGateway
import cn.moncn.sing_box_windows.vpn.VpnController
import cn.moncn.sing_box_windows.v2.domain.runtime.VpnState
import cn.moncn.sing_box_windows.vpn.VpnStateStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * 运行时网关实现。
 *
 * VPN 生命周期沿用系统 `VpnService`；
 * 内核状态、代理模式等全部来自内核命令通道（事件推送），不再轮询 Clash API。
 *
 * 实例为进程级单例（由 [cn.moncn.sing_box_windows.v2.core.di.V2Container] 缓存），
 * 避免 Activity 重建时重复创建采集协程。
 */
class AndroidRuntimeGateway(
    context: Context
) : RuntimeGateway {
    private val appContext = context.applicationContext
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val coreGateway = CoreCommandBridge.gateway

    override val vpnStateFlow: Flow<VpnState> = snapshotFlow { VpnStateStore.state }
        .distinctUntilChanged()

    override val vpnErrorFlow: Flow<String?> = snapshotFlow { VpnStateStore.lastError }
        .distinctUntilChanged()

    override val coreStatusFlow: Flow<CoreStatus?> = coreGateway.statusFlow

    /** 当前代理模式（可写，用于切换后立即刷新 UI） */
    private val modeState = MutableStateFlow<CoreClashMode?>(null)

    /**
     * 当前代理模式。
     *
     * 代理模式由配置（`route.rules[].clash_mode`）表达，**不再**由内核命令通道
     * 下发（该接口依赖 `experimental.clash_api`，本项目未开启），
     * 因此模式取自本地设置。
     *
     * 注意：不能直接用 `snapshotFlow { SettingsRepository.load(...) }`——
     * SharedPreferences 不是 Compose 快照状态，`snapshotFlow` 不会因它变化而重新发射，
     * 会导致分段控件点击后选中态不刷新。
     */
    override val modeFlow: Flow<CoreClashMode?> = modeState.asStateFlow()

    /** 模式由配置表达，三种模式始终可用 */
    override val modeSupportedFlow: Flow<Boolean> = flowOf(true)

    init {
        scope.launch {
            modeState.value = CoreClashMode.fromRaw(SettingsRepository.load(appContext).clashMode)
        }
        scope.launch {
            vpnStateFlow.collectLatest { state ->
                val connected = state == VpnState.CONNECTED || state == VpnState.CONNECTING
                // 内核命令通道建立失败不应影响 VPN 状态采集，更不能因未捕获异常
                // 终止整个协程（否则后续 VPN 状态变化将永远不再被处理）。
                runCatching {
                    CoreRuntimeCoordinator.syncByConnectionState(appContext, connected)
                }.onFailure { error ->
                    Log.w(TAG, "sync core command channel failed (state=$state)", error)
                }
            }
        }

        // 命令通道异常断开时按指数退避重连，避免 VPN 仍在运行但数据永久空白。
        scope.launch {
            var retryDelay = INITIAL_RETRY_DELAY_MS
            coreGateway.connectionStateFlow.collectLatest { state ->
                if (state != CoreConnectionState.DISCONNECTED_ERROR) {
                    retryDelay = INITIAL_RETRY_DELAY_MS
                    return@collectLatest
                }
                val connected = VpnStateStore.state == VpnState.CONNECTED ||
                    VpnStateStore.state == VpnState.CONNECTING
                if (!connected) return@collectLatest
                delay(retryDelay)
                runCatching { coreGateway.connect() }
                    .onFailure { Log.w(TAG, "reconnect command channel failed", it) }
                retryDelay = (retryDelay * 2).coerceAtMost(MAX_RETRY_DELAY_MS)
            }
        }
    }

    override suspend fun startVpn() {
        withContext(Dispatchers.Main.immediate) {
            VpnController.start(appContext)
        }
    }

    override suspend fun stopVpn() {
        withContext(Dispatchers.Main.immediate) {
            VpnController.stop(appContext)
        }
    }

    override suspend fun switchMode(mode: CoreClashMode): Result<Unit> {
        val persistResult = persistModePreference(mode)
        if (persistResult.isFailure) {
            return persistResult
        }
        // 立即刷新 UI 选中态（设置写入成功即生效）
        modeState.value = mode
        // 未连接时内核未运行，仅保存偏好（连接建立时会用新配置启动内核）。
        val running = VpnStateStore.state == VpnState.CONNECTED ||
            VpnStateStore.state == VpnState.CONNECTING
        if (!running) {
            return Result.success(Unit)
        }
        // 已连接：改配置后重载内核，使新模式立即生效。
        // （代理模式由 route.rules[].clash_mode 表达，内核命令通道的
        //   SetClashMode 依赖 experimental.clash_api，本项目未开启，不可用。）
        return CoreRuntimeCoordinator.reloadCore(appContext)
    }

    private suspend fun persistModePreference(mode: CoreClashMode): Result<Unit> {
        return runCatching {
            val current = SettingsRepository.load(appContext)
            val updated = current.copy(clashMode = mode.raw)
            SettingsRepository.save(appContext, updated)
            ConfigRepository.applySettings(appContext, updated)
        }
    }

    private companion object {
        const val TAG = "AndroidRuntimeGateway"
        const val INITIAL_RETRY_DELAY_MS = 1_000L
        const val MAX_RETRY_DELAY_MS = 30_000L
    }
}
