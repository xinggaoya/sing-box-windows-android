package cn.moncn.sing_box_windows.v2.data.nodes

import cn.moncn.sing_box_windows.v2.data.core.CoreCommandBridge
import cn.moncn.sing_box_windows.v2.domain.core.OutboundGroup
import cn.moncn.sing_box_windows.v2.domain.nodes.NodesGateway
import kotlinx.coroutines.flow.Flow

/**
 * 节点网关实现：委托内核命令通道。
 */
class DefaultNodesGateway : NodesGateway {

    override val groupsFlow: Flow<List<OutboundGroup>> = CoreCommandBridge.gateway.groupsFlow

    override suspend fun select(groupTag: String, outboundTag: String): Result<Unit> =
        CoreCommandBridge.gateway.selectOutbound(groupTag, outboundTag)

    override suspend fun test(outboundTag: String): Result<Unit> =
        CoreCommandBridge.gateway.urlTest(outboundTag)
}
