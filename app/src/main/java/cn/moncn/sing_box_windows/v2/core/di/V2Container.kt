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
 * 内核命令网关为进程级单例（与 VPN 服务共享同一条命令通道）。
 */
object V2Container {
    data class V2Graph(
        val runtimeGateway: RuntimeGateway,
        val subscriptionGateway: SubscriptionGateway,
        val nodesGateway: NodesGateway,
        val settingsGateway: SettingsGateway,
        val coreGateway: CoreGateway
    )

    fun provideGraph(context: Context): V2Graph {
        val appContext = context.applicationContext
        return V2Graph(
            runtimeGateway = AndroidRuntimeGateway(appContext),
            subscriptionGateway = DefaultSubscriptionGateway(appContext),
            nodesGateway = DefaultNodesGateway(),
            settingsGateway = DefaultSettingsGateway(appContext),
            coreGateway = CoreCommandBridge.coreGateway
        )
    }
}
