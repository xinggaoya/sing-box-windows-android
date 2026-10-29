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
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
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

    override val modeFlow: Flow<CoreClashMode?> = coreGateway.currentModeFlow
        .map { raw -> CoreClashMode.fromRaw(raw) }
        .distinctUntilChanged()

    override val modeSupportedFlow: Flow<Boolean> = coreGateway.clashModesFlow
        .map { modes -> modes.isNotEmpty() }
        .distinctUntilChanged()

    init {
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
        // 未连接时内核命令通道不可用，仅保存偏好（连接建立后会自动下发），
        // 此时返回成功，避免给用户弹出与页面文案相矛盾的失败提示。
        val channelReady = coreGateway.connectionStateFlow.first() == CoreConnectionState.CONNECTED
        if (!channelReady) {
            return Result.success(Unit)
        }
        return coreGateway.setClashMode(mode.raw)
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
