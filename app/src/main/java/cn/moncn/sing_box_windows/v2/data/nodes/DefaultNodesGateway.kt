package cn.moncn.sing_box_windows.v2.data.nodes

import androidx.compose.runtime.snapshotFlow
import cn.moncn.sing_box_windows.core.OutboundGroupManager
import cn.moncn.sing_box_windows.core.OutboundGroupModel
import cn.moncn.sing_box_windows.v2.domain.nodes.NodesGateway
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged

class DefaultNodesGateway : NodesGateway {
    override val groupsFlow: Flow<List<OutboundGroupModel>> = snapshotFlow { OutboundGroupManager.groups }
        .distinctUntilChanged()

    override suspend fun select(groupTag: String, outboundTag: String) {
        OutboundGroupManager.select(groupTag, outboundTag)
    }

    override suspend fun test(outboundTag: String): Result<Unit> {
        return OutboundGroupManager.urlTest(outboundTag)
    }
}
