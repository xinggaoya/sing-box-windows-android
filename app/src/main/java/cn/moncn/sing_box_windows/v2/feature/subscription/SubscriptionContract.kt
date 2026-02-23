package cn.moncn.sing_box_windows.v2.feature.subscription

import cn.moncn.sing_box_windows.config.SubscriptionState
import cn.moncn.sing_box_windows.v2.core.arch.UiIntent
import cn.moncn.sing_box_windows.v2.core.arch.UiState

data class SubscriptionUiState(
    val subscriptions: SubscriptionState = SubscriptionState.empty(),
    val updatingId: String? = null,
    val isLoading: Boolean = true,
    val message: String? = null,
    val error: String? = null,
    val updatedAt: Long = 0L
) : UiState

sealed interface SubscriptionIntent : UiIntent {
    data object Load : SubscriptionIntent
    data class AddRemote(val name: String, val url: String) : SubscriptionIntent
    data class AddLocal(val name: String, val content: String) : SubscriptionIntent
    data class Activate(val id: String) : SubscriptionIntent
    data class Sync(val id: String) : SubscriptionIntent
    data class Remove(val id: String) : SubscriptionIntent
    data class Edit(val id: String, val name: String, val url: String) : SubscriptionIntent
    data object ClearMessage : SubscriptionIntent
    data object ClearError : SubscriptionIntent
}
