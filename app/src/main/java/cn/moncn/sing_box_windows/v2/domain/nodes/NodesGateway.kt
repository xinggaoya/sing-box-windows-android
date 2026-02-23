package cn.moncn.sing_box_windows.v2.domain.nodes

import cn.moncn.sing_box_windows.core.OutboundGroupModel
import kotlinx.coroutines.flow.Flow

interface NodesGateway {
    val groupsFlow: Flow<List<OutboundGroupModel>>

    suspend fun select(groupTag: String, outboundTag: String)
    suspend fun test(outboundTag: String): Result<Unit>
}
