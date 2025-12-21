package cn.moncn.sing_box_windows.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Dns
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.List
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import cn.moncn.sing_box_windows.core.CoreStatus
import cn.moncn.sing_box_windows.core.OutboundGroupModel
import cn.moncn.sing_box_windows.config.SubscriptionState
import cn.moncn.sing_box_windows.vpn.VpnState
import cn.moncn.sing_box_windows.ui.screens.HomeScreen
import cn.moncn.sing_box_windows.ui.screens.NodesScreen
import cn.moncn.sing_box_windows.ui.screens.SubscriptionScreen
import androidx.compose.ui.graphics.Color
import cn.moncn.sing_box_windows.ui.theme.Mint500
import cn.moncn.sing_box_windows.ui.theme.Amber500
import cn.moncn.sing_box_windows.ui.theme.Rose500
import androidx.compose.animation.animateColorAsState

sealed class Screen(val route: String, val label: String, val icon: androidx.compose.ui.graphics.vector.ImageVector) {
    object Home : Screen("home", "首页", Icons.Rounded.Home)
    object Subscriptions : Screen("subscriptions", "订阅", Icons.Rounded.List)
    object Nodes : Screen("nodes", "节点", Icons.Rounded.Dns)
}

@Composable
fun AppNavigation(
    state: VpnState,
    coreStatus: CoreStatus?,
    coreVersion: String?,
    subscriptions: SubscriptionState,
    nameInput: String, // Kept for state continuity if needed, or refs
    urlInput: String,
    updatingId: String?,
    groups: List<OutboundGroupModel>,
    // Actions
    onConnect: () -> Unit,
    onDisconnect: () -> Unit,
    onAddSubscription: (String, String) -> Unit,
    onEditSubscription: (String, String, String) -> Unit,
    onRemoveSubscription: (String) -> Unit,
    onSelectSubscription: (String) -> Unit,
    onUpdateSubscription: (String) -> Unit,
    onSelectNode: (String, String) -> Unit,
    onTestNode: (String) -> Unit,
    onShowMessage: (MessageDialogState) -> Unit
) {
    val navController = rememberNavController()

    val statusColor by animateColorAsState(
        targetValue = when (state) {
            VpnState.CONNECTED -> Mint500
            VpnState.CONNECTING -> Amber500
            VpnState.ERROR -> Rose500
            VpnState.IDLE -> MaterialTheme.colorScheme.primary
        },
        label = "statusColor"
    )
    val statusText = when (state) {
        VpnState.CONNECTED -> "已连接"
        VpnState.CONNECTING -> "连接中"
        VpnState.ERROR -> "错误"
        VpnState.IDLE -> "未连接"
    }
     val actionText = if (state == VpnState.CONNECTED || state == VpnState.CONNECTING) {
        "断开连接"
    } else {
        "点击连接"
    }

    val screens = listOf(Screen.Home, Screen.Subscriptions, Screen.Nodes)

    Scaffold(
        bottomBar = {
            NavigationBar {
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentDestination = navBackStackEntry?.destination
                screens.forEach { screen ->
                    NavigationBarItem(
                        icon = { Icon(screen.icon, contentDescription = screen.label) },
                        label = { Text(screen.label) },
                        selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true,
                        onClick = {
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
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Home.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Home.route) {
                HomeScreen(
                    state = state,
                    statusText = statusText,
                    statusColor = statusColor,
                    actionText = actionText,
                    coreStatus = coreStatus,
                    coreVersion = coreVersion,
                    onConnect = onConnect,
                    onDisconnect = onDisconnect
                )
            }
            composable(Screen.Subscriptions.route) {
                SubscriptionScreen(
                    subscriptions = subscriptions,
                    updatingId = updatingId,
                    onAddSubscription = onAddSubscription,
                    onEditSubscription = onEditSubscription,
                    onRemoveSubscription = onRemoveSubscription,
                    onSelectSubscription = onSelectSubscription,
                    onUpdateSubscription = { id -> 
                        // Logic adapter: MainScreen used null for "Update Selected", here we pass ID specifically or handle logic
                        // If id matches selected, we can call updateSelected. 
                        // For now assuming caller handles generic update logic or we pass simple lambda
                         onUpdateSubscription(id) // Adapter in MainActivity will handle this
                    },
                    onShowMessage = onShowMessage
                )
            }
            composable(Screen.Nodes.route) {
                NodesScreen(
                    groups = groups,
                    onSelectNode = onSelectNode,
                    onTestNode = onTestNode
                )
            }
        }
    }
}
