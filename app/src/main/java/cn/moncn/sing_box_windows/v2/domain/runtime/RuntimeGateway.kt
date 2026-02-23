package cn.moncn.sing_box_windows.v2.domain.runtime

import cn.moncn.sing_box_windows.core.CoreStatus
import cn.moncn.sing_box_windows.core.ClashModeManager
import cn.moncn.sing_box_windows.vpn.VpnState
import kotlinx.coroutines.flow.Flow

/**
 * 运行时能力抽象，隔离平台实现细节，供 v2 用例与 ViewModel 使用。
 */
interface RuntimeGateway {
    val vpnStateFlow: Flow<VpnState>
    val vpnErrorFlow: Flow<String?>
    val coreStatusFlow: Flow<CoreStatus?>
    val modeFlow: Flow<ClashModeManager.ClashMode?>
    val modeSupportedFlow: Flow<Boolean>

    suspend fun startVpn()
    suspend fun stopVpn()
    suspend fun switchMode(mode: ClashModeManager.ClashMode): Result<Unit>
}
