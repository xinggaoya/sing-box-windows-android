package cn.moncn.sing_box_windows.v2.feature.nodes

import cn.moncn.sing_box_windows.core.OutboundGroupModel
import cn.moncn.sing_box_windows.v2.core.arch.UiIntent
import cn.moncn.sing_box_windows.v2.core.arch.UiState

data class NodesUiState(
    val groups: List<OutboundGroupModel> = emptyList(),
    val error: String? = null,
    val updatedAt: Long = 0L
) : UiState

sealed interface NodesIntent : UiIntent {
    data class Select(val groupTag: String, val nodeTag: String) : NodesIntent
    data class Test(val nodeTag: String) : NodesIntent
    data object ClearError : NodesIntent
}
