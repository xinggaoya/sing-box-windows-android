package cn.moncn.sing_box_windows.v2.app

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.List
import androidx.compose.material.icons.rounded.Dns
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.CallSplit
import androidx.compose.material.icons.rounded.Lan
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Terminal
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import cn.moncn.sing_box_windows.ui.theme.AppLayout
import cn.moncn.sing_box_windows.ui.theme.LocalIndigoTokens
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
    /** 底部导航项 */
    data object Home : V2Route("home", "首页", Icons.Rounded.Home)
    data object Proxy : V2Route("proxy", "代理", Icons.Rounded.Dns)
    data object Connections : V2Route("connections", "连接", Icons.Rounded.Lan)
    data object Subscription : V2Route("subscription", "订阅", Icons.AutoMirrored.Rounded.List)
    data object Settings : V2Route("settings", "设置", Icons.Rounded.Settings)

    /** 二级页面 */
    data object Logs : V2Route("logs", "日志", Icons.Rounded.Terminal)
    data object Outbounds : V2Route("outbounds", "出站", Icons.Rounded.CallSplit)
}

@Composable
fun V2AppRoot(
    graph: V2Graph
) {
    val navController = rememberNavController()
    val tokens = LocalIndigoTokens.current
    val screens = remember {
        listOf(
            V2Route.Home,
            V2Route.Proxy,
            V2Route.Connections,
            V2Route.Subscription,
            V2Route.Settings
        )
    }

    Scaffold(
        containerColor = Color.Transparent,
        bottomBar = {
            val navBackStackEntry by navController.currentBackStackEntryAsState()
            val currentDestination = navBackStackEntry?.destination
            V2BottomBar(
                screens = screens,
                currentDestination = currentDestination,
                onNavigate = { screen ->
                    navController.navigate(screen.route) {
                        popUpTo(navController.graph.findStartDestination().id) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            )
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = V2Route.Home.route,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            composable(V2Route.Home.route) {
                val viewModel: HomeViewModel = viewModel(
                    factory = HomeViewModel.factory(graph.runtimeGateway)
                )
                HomeRouteV2(
                    viewModel = viewModel,
                    onOpenLogs = { navController.navigateSingleTop(V2Route.Logs.route) },
                    onOpenOutbounds = { navController.navigateSingleTop(V2Route.Outbounds.route) },
                    onOpenConnections = { navController.navigateSingleTop(V2Route.Connections.route) }
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
                LogsRouteV2(coreGateway = graph.coreGateway)
            }
            composable(V2Route.Outbounds.route) {
                OutboundsRouteV2(coreGateway = graph.coreGateway)
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

@Composable
private fun V2BottomBar(
    screens: List<V2Route>,
    currentDestination: NavDestination?,
    onNavigate: (V2Route) -> Unit
) {
    val tokens = LocalIndigoTokens.current
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                horizontal = AppLayout.bottomBarPadding,
                vertical = AppLayout.bottomBarVerticalPadding
            )
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(28.dp),
            color = tokens.bgSurface,
            shadowElevation = 14.dp,
            tonalElevation = 2.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                screens.forEach { screen ->
                    val selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true
                    val scale by animateFloatAsState(
                        targetValue = if (selected) 1f else 0.97f,
                        label = "v2_bar_item_scale"
                    )
                    val labelColor = if (selected) tokens.primary else tokens.textTertiary
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .graphicsLayer {
                                scaleX = scale
                                scaleY = scale
                            }
                            .clip(RoundedCornerShape(20.dp))
                            .background(
                                brush = if (selected) {
                                    Brush.horizontalGradient(
                                        listOf(tokens.primarySoft, tokens.primarySoftStrong)
                                    )
                                } else {
                                    Brush.horizontalGradient(
                                        listOf(Color.Transparent, Color.Transparent)
                                    )
                                }
                            )
                            .clickable { onNavigate(screen) }
                            .padding(horizontal = 6.dp, vertical = 11.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = screen.icon,
                                contentDescription = screen.title,
                                tint = labelColor
                            )
                            Text(
                                text = screen.title,
                                color = labelColor,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }
    }
}
