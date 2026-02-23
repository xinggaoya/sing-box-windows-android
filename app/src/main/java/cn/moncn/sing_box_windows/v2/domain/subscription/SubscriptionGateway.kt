package cn.moncn.sing_box_windows.v2.domain.subscription

import cn.moncn.sing_box_windows.config.SubscriptionEditResult
import cn.moncn.sing_box_windows.config.SubscriptionState
import cn.moncn.sing_box_windows.config.SubscriptionUpdateResult

interface SubscriptionGateway {
    suspend fun load(): SubscriptionState
    suspend fun addAndActivate(name: String, url: String): SubscriptionUpdateResult
    suspend fun importLocal(name: String, content: String): SubscriptionUpdateResult
    suspend fun activate(id: String): SubscriptionUpdateResult
    suspend fun sync(id: String, selectedId: String?): SubscriptionUpdateResult
    suspend fun edit(id: String, name: String, url: String): SubscriptionEditResult
    suspend fun remove(id: String): SubscriptionState
}
