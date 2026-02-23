package cn.moncn.sing_box_windows.v2.data.subscription

import android.content.Context
import cn.moncn.sing_box_windows.config.SubscriptionEditResult
import cn.moncn.sing_box_windows.config.SubscriptionRepository
import cn.moncn.sing_box_windows.config.SubscriptionState
import cn.moncn.sing_box_windows.config.SubscriptionUpdateResult
import cn.moncn.sing_box_windows.v2.domain.subscription.SubscriptionGateway

class DefaultSubscriptionGateway(
    context: Context
) : SubscriptionGateway {
    private val appContext = context.applicationContext

    override suspend fun load(): SubscriptionState {
        return SubscriptionRepository.load(appContext)
    }

    override suspend fun addAndActivate(name: String, url: String): SubscriptionUpdateResult {
        val addResult = SubscriptionRepository.add(appContext, name, url)
        return SubscriptionRepository.activate(appContext, addResult.item.id)
    }

    override suspend fun importLocal(name: String, content: String): SubscriptionUpdateResult {
        return SubscriptionRepository.importLocal(appContext, name, content)
    }

    override suspend fun activate(id: String): SubscriptionUpdateResult {
        return SubscriptionRepository.activate(appContext, id)
    }

    override suspend fun sync(id: String, selectedId: String?): SubscriptionUpdateResult {
        return if (id == selectedId) {
            SubscriptionRepository.updateSelected(appContext)
        } else {
            SubscriptionRepository.activate(appContext, id)
        }
    }

    override suspend fun edit(id: String, name: String, url: String): SubscriptionEditResult {
        return SubscriptionRepository.edit(appContext, id, name, url)
    }

    override suspend fun remove(id: String): SubscriptionState {
        return SubscriptionRepository.remove(appContext, id)
    }
}
