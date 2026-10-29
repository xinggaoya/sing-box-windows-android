package cn.moncn.sing_box_windows.v2.core.di

import android.content.Context
import cn.moncn.sing_box_windows.v2.data.core.CoreCommandBridge
import cn.moncn.sing_box_windows.v2.data.nodes.DefaultNodesGateway
import cn.moncn.sing_box_windows.v2.data.runtime.AndroidRuntimeGateway
import cn.moncn.sing_box_windows.v2.data.settings.DefaultSettingsGateway
import cn.moncn.sing_box_windows.v2.data.subscription.DefaultSubscriptionGateway
import cn.moncn.sing_box_windows.v2.domain.core.CoreGateway
import cn.moncn.sing_box_windows.v2.domain.nodes.NodesGateway
import cn.moncn.sing_box_windows.v2.domain.runtime.RuntimeGateway
import cn.moncn.sing_box_windows.v2.domain.settings.SettingsGateway
import cn.moncn.sing_box_windows.v2.domain.subscription.SubscriptionGateway

/**
 * v2 依赖装配入口。
 *
 * 依赖图在进程内缓存：各 Gateway 内部持有协程作用域，
 * 若每次 Activity 重建都新建实例，会残留重复的采集协程。
 */
object V2Container {
    data class V2Graph(
        val runtimeGateway: RuntimeGateway,
        val subscriptionGateway: SubscriptionGateway,
        val nodesGateway: NodesGateway,
        val settingsGateway: SettingsGateway,
        val coreGateway: CoreGateway
    )

    @Volatile
    private var cached: V2Graph? = null

    fun provideGraph(context: Context): V2Graph {
        cached?.let { return it }
        return synchronized(this) {
            cached ?: create(context.applicationContext).also { cached = it }
        }
    }

    private fun create(appContext: Context): V2Graph = V2Graph(
        runtimeGateway = AndroidRuntimeGateway(appContext),
        subscriptionGateway = DefaultSubscriptionGateway(appContext),
        nodesGateway = DefaultNodesGateway(),
        settingsGateway = DefaultSettingsGateway(appContext),
        coreGateway = CoreCommandBridge.coreGateway
    )
}
