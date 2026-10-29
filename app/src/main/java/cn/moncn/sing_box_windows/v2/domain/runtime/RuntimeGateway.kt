package cn.moncn.sing_box_windows.v2.domain.runtime

import cn.moncn.sing_box_windows.v2.domain.core.CoreClashMode
import cn.moncn.sing_box_windows.v2.domain.core.CoreStatus
import cn.moncn.sing_box_windows.v2.domain.runtime.VpnState
import kotlinx.coroutines.flow.Flow

/**
 * 运行时能力抽象，隔离平台实现细节，供 v2 用例与 ViewModel 使用。
 */
interface RuntimeGateway {
    val vpnStateFlow: Flow<VpnState>
    val vpnErrorFlow: Flow<String?>
    val coreStatusFlow: Flow<CoreStatus?>
    val modeFlow: Flow<CoreClashMode?>
    val modeSupportedFlow: Flow<Boolean>

    suspend fun startVpn()
    suspend fun stopVpn()
    suspend fun switchMode(mode: CoreClashMode): Result<Unit>
}
