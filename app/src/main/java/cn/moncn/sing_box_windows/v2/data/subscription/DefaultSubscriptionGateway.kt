package cn.moncn.sing_box_windows.v2.data.subscription

import android.content.Context
import android.util.Log
import cn.moncn.sing_box_windows.v2.data.config.SubscriptionRepository
import cn.moncn.sing_box_windows.v2.data.core.CoreRuntimeCoordinator
import cn.moncn.sing_box_windows.v2.domain.runtime.VpnState
import cn.moncn.sing_box_windows.v2.domain.subscription.SubscriptionEditResult
import cn.moncn.sing_box_windows.v2.domain.subscription.SubscriptionGateway
import cn.moncn.sing_box_windows.v2.domain.subscription.SubscriptionState
import cn.moncn.sing_box_windows.v2.domain.subscription.SubscriptionUpdateResult
import cn.moncn.sing_box_windows.vpn.VpnStateStore

class DefaultSubscriptionGateway(
    context: Context
) : SubscriptionGateway {
    private val appContext = context.applicationContext

    override suspend fun load(): SubscriptionState {
        return SubscriptionRepository.load(appContext)
    }

    override suspend fun addAndActivate(name: String, url: String): SubscriptionUpdateResult {
        val addResult = SubscriptionRepository.add(appContext, name, url)
        val result = SubscriptionRepository.activate(appContext, addResult.item.id)
        if (result.ok) reloadCoreIfRunning()
        return result
    }

    override suspend fun importLocal(name: String, content: String): SubscriptionUpdateResult {
        val result = SubscriptionRepository.importLocal(appContext, name, content)
        if (result.ok) reloadCoreIfRunning()
        return result
    }

    override suspend fun activate(id: String): SubscriptionUpdateResult {
        val result = SubscriptionRepository.activate(appContext, id)
        if (result.ok) reloadCoreIfRunning()
        return result
    }

    override suspend fun sync(id: String, selectedId: String?): SubscriptionUpdateResult {
        val result = if (id == selectedId) {
            SubscriptionRepository.updateSelected(appContext)
        } else {
            SubscriptionRepository.activate(appContext, id)
        }
        if (result.ok) reloadCoreIfRunning()
        return result
    }

    override suspend fun edit(id: String, name: String, url: String): SubscriptionEditResult {
        return SubscriptionRepository.edit(appContext, id, name, url)
    }

    override suspend fun remove(id: String): SubscriptionState {
        return SubscriptionRepository.remove(appContext, id)
    }

    /**
     * 订阅变更后会重新生成 `singbox.json`，需要重载内核才能立即生效，
     * 否则要等到下一次连接才会应用新订阅。
     *
     * 未连接时内核未运行，无需重载（下次连接会直接使用新配置）。
     */
    private suspend fun reloadCoreIfRunning() {
        val state = VpnStateStore.state
        if (state != VpnState.CONNECTED && state != VpnState.CONNECTING) return
        runCatching { CoreRuntimeCoordinator.reloadCore(appContext) }
            .onFailure { error ->
                Log.w(TAG, "reload core after subscription change failed", error)
            }
    }

    private companion object {
        const val TAG = "SubscriptionGateway"
    }
}
