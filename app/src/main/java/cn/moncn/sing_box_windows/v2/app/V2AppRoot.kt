package cn.moncn.sing_box_windows.v2.app

import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.List
import androidx.compose.material.icons.automirrored.rounded.CallSplit
import androidx.compose.material.icons.rounded.Dns
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Lan
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Terminal
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import cn.moncn.sing_box_windows.ui.components.AppBottomBar
import cn.moncn.sing_box_windows.ui.components.AppNavItem
import cn.moncn.sing_box_windows.ui.components.LocalBottomBarPadding
import cn.moncn.sing_box_windows.ui.theme.AppLayout
import cn.moncn.sing_box_windows.ui.theme.AppMotion
import cn.moncn.sing_box_windows.ui.theme.LocalAppColors
import cn.moncn.sing_box_windows.v2.core.di.V2Container.V2Graph
import cn.moncn.sing_box_windows.v2.feature.connections.ConnectionsRouteV2
import cn.moncn.sing_box_windows.v2.feature.home.HomeRouteV2
import cn.moncn.sing_box_windows.v2.feature.home.HomeViewModel
import cn.moncn.sing_box_windows.v2.feature.logs.LogsRouteV2
import cn.moncn.sing_box_windows.v2.feature.nodes.NodesRouteV2
import cn.moncn.sing_box_windows.v2.feature.outbounds.OutboundsRouteV2
import cn.moncn.sing_box_windows.v2.feature.settings.SettingsRouteV2
import cn.moncn.sing_box_windows.v2.feature.subscription.SubscriptionRouteV2

/** 应用路由表 */
sealed class V2Route(
    val route: String,
    val title: String,
    val icon: ImageVector
) {
    /** 底部导航项（顶级页面） */
    data object Home : V2Route("home", "首页", Icons.Rounded.Home)
    data object Proxy : V2Route("proxy", "代理", Icons.Rounded.Dns)
    data object Connections : V2Route("connections", "连接", Icons.Rounded.Lan)
    data object Subscription : V2Route("subscription", "订阅", Icons.AutoMirrored.Rounded.List)
    data object Settings : V2Route("settings", "设置", Icons.Rounded.Settings)

    /** 二级页面 */
    data object Logs : V2Route("logs", "日志", Icons.Rounded.Terminal)
    data object Outbounds : V2Route("outbounds", "出站", Icons.AutoMirrored.Rounded.CallSplit)
}

/** 顶级路由集合（底部导航固定展示这些页面） */
private val topLevelRoutes = listOf(
    V2Route.Home,
    V2Route.Proxy,
    V2Route.Connections,
    V2Route.Subscription,
    V2Route.Settings
)

@Composable
fun V2AppRoot(
    graph: V2Graph
) {
    val navController = rememberNavController()
    val colors = LocalAppColors.current
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    val currentRoute = currentDestination?.route

    val navItems = remember {
        topLevelRoutes.map { AppNavItem(route = it.route, label = it.title, icon = it.icon) }
    }

    val isTopLevel = remember(currentRoute) {
        topLevelRoutes.any { it.route == currentRoute }
    }

    // 底部导航栏自身已包含手势条 inset，这里只记录「导航栏本体高度」，
    // 手势条留白由 appPagePadding() 统一叠加，避免重复计算。
    val bottomInset = if (isTopLevel) AppLayout.bottomBarHeight else 0.dp

    CompositionLocalProvider(LocalBottomBarPadding provides bottomInset) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(colors.background)
        ) {
            NavHost(
                navController = navController,
                startDestination = V2Route.Home.route,
                modifier = Modifier.fillMaxSize(),
                enterTransition = { fadeIn(AppMotion.enterTween()) },
                exitTransition = { fadeOut(AppMotion.exitTween()) },
                popEnterTransition = { fadeIn(AppMotion.enterTween()) },
                popExitTransition = { fadeOut(AppMotion.exitTween()) }
            ) {
                composable(V2Route.Home.route) {
                    val viewModel: HomeViewModel = viewModel(
                        factory = HomeViewModel.factory(graph.runtimeGateway)
                    )
                    HomeRouteV2(
                        viewModel = viewModel,
                        onOpenLogs = { navController.navigateSingleTop(V2Route.Logs.route) },
                        onOpenOutbounds = { navController.navigateSingleTop(V2Route.Outbounds.route) },
                        onOpenConnections = { navigateTopLevel(navController, V2Route.Connections.route) }
                    )
                }
                composable(V2Route.Proxy.route) {
                    NodesRouteV2(gateway = graph.nodesGateway)
                }
                composable(V2Route.Connections.route) {
                    ConnectionsRouteV2(coreGateway = graph.coreGateway)
                }
                composable(V2Route.Subscription.route) {
                    SubscriptionRouteV2(gateway = graph.subscriptionGateway)
                }
                composable(V2Route.Settings.route) {
                    SettingsRouteV2(gateway = graph.settingsGateway)
                }
                composable(V2Route.Logs.route) {
                    LogsRouteV2(
                        coreGateway = graph.coreGateway,
                        onBack = { navController.popBackStack() }
                    )
                }
                composable(V2Route.Outbounds.route) {
                    OutboundsRouteV2(
                        coreGateway = graph.coreGateway,
                        onBack = { navController.popBackStack() }
                    )
                }
            }

            // 底部导航悬浮于内容之上，页面通过 LocalBottomBarPadding 预留空间。
            if (isTopLevel) {
                AppBottomBar(
                    items = navItems,
                    currentRoute = currentRoute,
                    onNavigate = { item ->
                        navigateTopLevel(navController, item.route)
                    },
                    modifier = Modifier.align(Alignment.BottomCenter)
                )
            }
        }
    }
}

/** 二级页面导航：避免重复压入同一路由 */
private fun NavHostController.navigateSingleTop(route: String) {
    navigate(route) {
        launchSingleTop = true
    }
}

/**
 * 顶级页面导航：与底部导航栏保持一致的单栈行为。
 *
 * 底部导航与首页快捷入口共用该逻辑，避免出现「同一页面在返回栈里出现两次」。
 */
private fun navigateTopLevel(navController: NavHostController, route: String) {
    navController.navigate(route) {
        popUpTo(navController.graph.findStartDestination().id) {
            saveState = true
        }
        launchSingleTop = true
        restoreState = true
    }
}

/** 供页面判断自身是否为顶级路由（用于决定是否展示底部导航）。 */
fun isTopLevelRoute(route: String?): Boolean = topLevelRoutes.any { it.route == route }
