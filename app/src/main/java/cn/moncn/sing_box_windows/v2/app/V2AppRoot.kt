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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.List
import androidx.compose.material.icons.rounded.Dns
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import cn.moncn.sing_box_windows.v2.core.di.V2Container.V2Graph
import cn.moncn.sing_box_windows.v2.feature.home.HomeRouteV2
import cn.moncn.sing_box_windows.v2.feature.home.HomeViewModel
import cn.moncn.sing_box_windows.v2.feature.nodes.NodesRouteV2
import cn.moncn.sing_box_windows.v2.feature.settings.SettingsRouteV2
import cn.moncn.sing_box_windows.v2.feature.subscription.SubscriptionRouteV2

private sealed class V2Screen(
    val route: String,
    val title: String,
    val icon: ImageVector
) {
    data object Home : V2Screen("home", "首页", Icons.Rounded.Home)
    data object Subscription : V2Screen("subscription", "订阅", Icons.AutoMirrored.Rounded.List)
    data object Nodes : V2Screen("nodes", "节点", Icons.Rounded.Dns)
    data object Settings : V2Screen("settings", "设置", Icons.Rounded.Settings)
}

@Composable
fun V2AppRoot(
    graph: V2Graph
) {
    val navController = rememberNavController()
    val screens = listOf(
        V2Screen.Home,
        V2Screen.Subscription,
        V2Screen.Nodes,
        V2Screen.Settings
    )

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
            startDestination = V2Screen.Home.route,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            composable(V2Screen.Home.route) {
                val viewModel: HomeViewModel = viewModel(
                    factory = HomeViewModel.factory(graph.runtimeGateway)
                )
                HomeRouteV2(viewModel = viewModel)
            }
            composable(V2Screen.Subscription.route) {
                SubscriptionRouteV2(gateway = graph.subscriptionGateway)
            }
            composable(V2Screen.Nodes.route) {
                NodesRouteV2(gateway = graph.nodesGateway)
            }
            composable(V2Screen.Settings.route) {
                SettingsRouteV2(gateway = graph.settingsGateway)
            }
        }
    }
}

@Composable
private fun V2BottomBar(
    screens: List<V2Screen>,
    currentDestination: NavDestination?,
    onNavigate: (V2Screen) -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp, vertical = 12.dp)
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = androidx.compose.foundation.shape.RoundedCornerShape(28.dp),
            color = Color(0xFFF2F7FF),
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
                    val selectedBrush = Brush.horizontalGradient(
                        colors = listOf(Color(0xFF5A84D9), Color(0xFF78A5EB))
                    )
                    val labelColor = if (selected) Color.White else scheme.onSurfaceVariant
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .graphicsLayer {
                                scaleX = scale
                                scaleY = scale
                            }
                            .clip(androidx.compose.foundation.shape.RoundedCornerShape(20.dp))
                            .background(
                                brush = if (selected) selectedBrush else Brush.horizontalGradient(
                                    listOf(Color.Transparent, Color.Transparent)
                                )
                            )
                            .clickable { onNavigate(screen) }
                            .padding(horizontal = 8.dp, vertical = 11.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = screen.icon,
                                contentDescription = screen.title,
                                tint = labelColor
                            )
                            Text(
                                text = screen.title,
                                color = labelColor,
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }
    }
}
