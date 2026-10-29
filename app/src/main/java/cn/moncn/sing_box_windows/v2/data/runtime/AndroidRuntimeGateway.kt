package cn.moncn.sing_box_windows.v2.data.runtime

import android.content.Context
import androidx.compose.runtime.snapshotFlow
import cn.moncn.sing_box_windows.v2.data.config.ConfigRepository
import cn.moncn.sing_box_windows.v2.data.config.SettingsRepository
import cn.moncn.sing_box_windows.v2.data.core.CoreCommandBridge
import cn.moncn.sing_box_windows.v2.data.core.CoreRuntimeCoordinator
import cn.moncn.sing_box_windows.v2.domain.core.CoreClashMode
import cn.moncn.sing_box_windows.v2.domain.core.CoreStatus
import cn.moncn.sing_box_windows.v2.domain.runtime.RuntimeGateway
import cn.moncn.sing_box_windows.vpn.VpnController
import cn.moncn.sing_box_windows.vpn.VpnState
import cn.moncn.sing_box_windows.vpn.VpnStateStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * 运行时网关实现。
 *
 * VPN 生命周期沿用系统 `VpnService`；
 * 内核状态、代理模式等全部来自内核命令通道（事件推送），不再轮询 Clash API。
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
                CoreRuntimeCoordinator.syncByConnectionState(
                    context = appContext,
                    connected = connected
                )
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
        // 未连接时内核命令通道不可用，仅保存偏好；连接建立后会自动下发。
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
}
