package cn.moncn.sing_box_windows.v2.domain.nodes

import cn.moncn.sing_box_windows.v2.domain.core.OutboundGroup
import kotlinx.coroutines.flow.Flow

/**
 * 节点（代理组）能力抽象。
 *
 * 数据来源为内核命令通道推送的代理组快照，无需轮询。
 */
interface NodesGateway {
    val groupsFlow: Flow<List<OutboundGroup>>

    /** 选择代理组中的出站 */
    suspend fun select(groupTag: String, outboundTag: String): Result<Unit>

    /** 对指定出站发起测速 */
    suspend fun test(outboundTag: String): Result<Unit>
}
