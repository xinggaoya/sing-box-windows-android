package cn.moncn.sing_box_windows.v2.feature.connections

import cn.moncn.sing_box_windows.v2.core.arch.UiEffect
import cn.moncn.sing_box_windows.v2.core.arch.UiIntent
import cn.moncn.sing_box_windows.v2.core.arch.UiState
import cn.moncn.sing_box_windows.v2.domain.core.CoreConnection

/** 连接列表筛选 */
enum class ConnectionFilter(val label: String) {
    ALL("全部"),
    ACTIVE("活跃"),
    CLOSED("已关闭")
}

data class ConnectionsUiState(
    val connections: List<CoreConnection> = emptyList(),
    val filter: ConnectionFilter = ConnectionFilter.ALL,
    val error: String? = null
) : UiState {
    /** 按筛选条件过滤后的连接 */
    val visible: List<CoreConnection>
        get() = when (filter) {
            ConnectionFilter.ALL -> connections
            ConnectionFilter.ACTIVE -> connections.filter { !it.closed }
            ConnectionFilter.CLOSED -> connections.filter { it.closed }
        }

    val activeCount: Int get() = connections.count { !it.closed }

    val totalUplink: Long get() = connections.sumOf { it.uplinkTotal }

    val totalDownlink: Long get() = connections.sumOf { it.downlinkTotal }
}

sealed interface ConnectionsIntent : UiIntent {
    data class SetFilter(val filter: ConnectionFilter) : ConnectionsIntent
    data class CloseConnection(val id: String) : ConnectionsIntent
    data object CloseAll : ConnectionsIntent
    data object ClearError : ConnectionsIntent
}

sealed interface ConnectionsEffect : UiEffect
