package cn.moncn.sing_box_windows.v2.data.runtime

import android.content.Context
import androidx.compose.runtime.snapshotFlow
import cn.moncn.sing_box_windows.config.ConfigRepository
import cn.moncn.sing_box_windows.config.SettingsRepository
import cn.moncn.sing_box_windows.core.ClashApiClient
import cn.moncn.sing_box_windows.core.ClashModeManager
import cn.moncn.sing_box_windows.core.CoreRuntimeCoordinator
import cn.moncn.sing_box_windows.core.CoreStatus
import cn.moncn.sing_box_windows.core.CoreStatusStore
import cn.moncn.sing_box_windows.v2.domain.runtime.RuntimeGateway
import cn.moncn.sing_box_windows.vpn.VpnController
import cn.moncn.sing_box_windows.vpn.VpnState
import cn.moncn.sing_box_windows.vpn.VpnStateStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * 复用现有运行时组件，先完成 v2 的首条样板链路。
 * 后续可把这里替换为真正的数据层实现，不影响上层页面。
 */
class AndroidRuntimeGateway(
    context: Context
) : RuntimeGateway {
    private val appContext = context.applicationContext
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override val vpnStateFlow: Flow<VpnState> = snapshotFlow { VpnStateStore.state }
        .distinctUntilChanged()

    override val vpnErrorFlow: Flow<String?> = snapshotFlow { VpnStateStore.lastError }
        .distinctUntilChanged()

    override val coreStatusFlow: Flow<CoreStatus?> = snapshotFlow { CoreStatusStore.status }
        .distinctUntilChanged()

    override val modeFlow: Flow<ClashModeManager.ClashMode?> = snapshotFlow {
        ClashModeManager.currentMode
    }.distinctUntilChanged()

    override val modeSupportedFlow: Flow<Boolean> = snapshotFlow {
        ClashModeManager.isModeSupported
    }.distinctUntilChanged()

    init {
        scope.launch {
            val settings = SettingsRepository.load(appContext)
            val preferredMode = ClashModeManager.ClashMode.fromValue(settings.clashMode)
                ?: ClashModeManager.ClashMode.Rule
            ClashModeManager.updatePreferredMode(preferredMode)
        }

        scope.launch {
            vpnStateFlow.collectLatest { state ->
                val connected = state == VpnState.CONNECTED || state == VpnState.CONNECTING
                CoreRuntimeCoordinator.syncByConnectionState(
                    context = appContext,
                    connected = connected
                )

                // 连接建立后再次应用偏好模式，避免核心重载后回落到默认模式。
                if (state == VpnState.CONNECTED && ClashApiClient.isConfigured()) {
                    ClashModeManager.switchMode(ClashModeManager.preferredMode)
                }
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

    override suspend fun switchMode(mode: ClashModeManager.ClashMode): Result<Unit> {
        val persistResult = persistModePreference(mode)
        if (persistResult.isFailure) {
            return persistResult
        }

        ClashModeManager.updatePreferredMode(mode)

        // 未连接时只保存偏好，连接后会自动应用。
        if (!ClashApiClient.isConfigured()) {
            return Result.success(Unit)
        }

        return ClashModeManager.switchMode(mode)
    }

    private suspend fun persistModePreference(mode: ClashModeManager.ClashMode): Result<Unit> {
        return runCatching {
            val current = SettingsRepository.load(appContext)
            val updated = current.copy(clashMode = mode.value)
            SettingsRepository.save(appContext, updated)
            ConfigRepository.applySettings(appContext, updated)
        }
    }
}
