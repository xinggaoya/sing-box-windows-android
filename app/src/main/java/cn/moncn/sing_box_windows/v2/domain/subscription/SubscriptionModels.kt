package cn.moncn.sing_box_windows.v2.domain.subscription

/**
 * 订阅领域模型。
 *
 * 与实现层分离，供 domain 接口与 feature 层直接使用。
 */
data class SubscriptionItem(
    val id: String,
    val name: String,
    val url: String,
    val lastUpdatedAt: Long? = null,
    val lastError: String? = null,
    val isLocal: Boolean = false,
    val content: String? = null
)

data class SubscriptionState(
    val items: List<SubscriptionItem>,
    val selectedId: String?
) {
    fun selected(): SubscriptionItem? = items.firstOrNull { it.id == selectedId }

    companion object {
        fun empty(): SubscriptionState = SubscriptionState(emptyList(), null)
    }
}

data class SubscriptionUpdateResult(
    val ok: Boolean,
    val message: String,
    val warnings: List<String>,
    val state: SubscriptionState
)

data class SubscriptionAddResult(
    val state: SubscriptionState,
    val item: SubscriptionItem
)

data class SubscriptionEditResult(
    val ok: Boolean,
    val message: String,
    val state: SubscriptionState,
    val item: SubscriptionItem? = null,
    val urlChanged: Boolean = false,
    val nameChanged: Boolean = false
)
