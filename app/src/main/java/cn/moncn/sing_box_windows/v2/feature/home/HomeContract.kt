package cn.moncn.sing_box_windows.v2.feature.home

import cn.moncn.sing_box_windows.v2.core.arch.UiEffect
import cn.moncn.sing_box_windows.v2.core.arch.UiIntent
import cn.moncn.sing_box_windows.v2.core.arch.UiState
import cn.moncn.sing_box_windows.v2.domain.core.CoreClashMode
import cn.moncn.sing_box_windows.v2.domain.core.CoreStatus
import cn.moncn.sing_box_windows.v2.domain.core.MemorySnapshot
import cn.moncn.sing_box_windows.v2.domain.runtime.VpnState

data class HomeUiState(
    val vpnState: VpnState = VpnState.IDLE,
    val coreStatus: CoreStatus? = null,
    /** 进程内存分解（用于区分 Java 堆 / native 堆增长） */
    val memory: MemorySnapshot? = null,
    val currentMode: CoreClashMode? = null,
    val modeSupported: Boolean = false,
    val lastError: String? = null,
) : UiState {
    val connected: Boolean = vpnState == VpnState.CONNECTED
    val connecting: Boolean = vpnState == VpnState.CONNECTING
}

sealed interface HomeIntent : UiIntent {
    data object ConnectClicked : HomeIntent
    data object DisconnectClicked : HomeIntent
    data class SwitchMode(val mode: CoreClashMode) : HomeIntent
    data class VpnPermissionResult(val granted: Boolean) : HomeIntent
    data object ClearError : HomeIntent
}

sealed interface HomeEffect : UiEffect {
    data object RequestVpnPermission : HomeEffect
}
