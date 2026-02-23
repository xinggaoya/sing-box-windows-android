package cn.moncn.sing_box_windows.v2.core.di

import android.content.Context
import cn.moncn.sing_box_windows.v2.data.nodes.DefaultNodesGateway
import cn.moncn.sing_box_windows.v2.data.runtime.AndroidRuntimeGateway
import cn.moncn.sing_box_windows.v2.data.settings.DefaultSettingsGateway
import cn.moncn.sing_box_windows.v2.data.subscription.DefaultSubscriptionGateway
import cn.moncn.sing_box_windows.v2.domain.nodes.NodesGateway
import cn.moncn.sing_box_windows.v2.domain.runtime.RuntimeGateway
import cn.moncn.sing_box_windows.v2.domain.settings.SettingsGateway
import cn.moncn.sing_box_windows.v2.domain.subscription.SubscriptionGateway

/**
 * v2 最小依赖装配入口。
 * 当前先采用手工装配，后续再迁移到正式 DI 框架。
 */
object V2Container {
    data class V2Graph(
        val runtimeGateway: RuntimeGateway,
        val subscriptionGateway: SubscriptionGateway,
        val nodesGateway: NodesGateway,
        val settingsGateway: SettingsGateway
    )

    fun provideGraph(context: Context): V2Graph {
        val appContext = context.applicationContext
        return V2Graph(
            runtimeGateway = AndroidRuntimeGateway(appContext),
            subscriptionGateway = DefaultSubscriptionGateway(appContext),
            nodesGateway = DefaultNodesGateway(),
            settingsGateway = DefaultSettingsGateway(appContext)
        )
    }
}
