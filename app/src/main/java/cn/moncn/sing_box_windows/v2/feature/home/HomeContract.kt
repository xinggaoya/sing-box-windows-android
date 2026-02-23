package cn.moncn.sing_box_windows.v2.feature.home

import cn.moncn.sing_box_windows.core.CoreStatus
import cn.moncn.sing_box_windows.core.ClashModeManager
import cn.moncn.sing_box_windows.v2.core.arch.UiEffect
import cn.moncn.sing_box_windows.v2.core.arch.UiIntent
import cn.moncn.sing_box_windows.v2.core.arch.UiState
import cn.moncn.sing_box_windows.vpn.VpnState

data class HomeUiState(
    val vpnState: VpnState = VpnState.IDLE,
    val coreStatus: CoreStatus? = null,
    val currentMode: ClashModeManager.ClashMode? = null,
    val modeSupported: Boolean = false,
    val lastError: String? = null,
    val updatedAt: Long = 0L
) : UiState {
    val connected: Boolean = vpnState == VpnState.CONNECTED
    val connecting: Boolean = vpnState == VpnState.CONNECTING
}

sealed interface HomeIntent : UiIntent {
    data object ConnectClicked : HomeIntent
    data object DisconnectClicked : HomeIntent
    data class SwitchMode(val mode: ClashModeManager.ClashMode) : HomeIntent
    data class VpnPermissionResult(val granted: Boolean) : HomeIntent
    data object ClearError : HomeIntent
}

sealed interface HomeEffect : UiEffect {
    data object RequestVpnPermission : HomeEffect
}
