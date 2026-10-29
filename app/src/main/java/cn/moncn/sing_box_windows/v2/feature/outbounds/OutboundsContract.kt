package cn.moncn.sing_box_windows.v2.feature.outbounds

import cn.moncn.sing_box_windows.v2.core.arch.UiEffect
import cn.moncn.sing_box_windows.v2.core.arch.UiIntent
import cn.moncn.sing_box_windows.v2.core.arch.UiState
import cn.moncn.sing_box_windows.v2.domain.core.OutboundGroupItem

data class OutboundsUiState(
    val outbounds: List<OutboundGroupItem> = emptyList(),
    val testing: Set<String> = emptySet(),
    val error: String? = null
) : UiState {
    val testedCount: Int get() = outbounds.count { it.tested }
}

sealed interface OutboundsIntent : UiIntent {
    data class Test(val tag: String) : OutboundsIntent
    data object TestAll : OutboundsIntent
    data object ClearError : OutboundsIntent
}

sealed interface OutboundsEffect : UiEffect
