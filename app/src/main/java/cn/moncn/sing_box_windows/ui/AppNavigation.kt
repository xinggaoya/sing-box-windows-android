package cn.moncn.sing_box_windows.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.List
import androidx.compose.material.icons.rounded.Dns
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
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
import cn.moncn.sing_box_windows.core.ClashModeManager
import cn.moncn.sing_box_windows.config.SubscriptionState
import cn.moncn.sing_box_windows.vpn.VpnState
import cn.moncn.sing_box_windows.ui.screens.HomeScreen
import cn.moncn.sing_box_windows.ui.screens.NodesScreen
import cn.moncn.sing_box_windows.ui.screens.SettingsScreen
import cn.moncn.sing_box_windows.ui.screens.SubscriptionScreen
import androidx.compose.animation.animateColorAsState
import androidx.compose.ui.graphics.Color
import cn.moncn.sing_box_windows.ui.components.AppBackground

sealed class Screen(val route: String, val label: String, val icon: androidx.compose.ui.graphics.vector.ImageVector) {
    object Home : Screen("home", "首页", Icons.Rounded.Home)
    object Subscriptions : Screen("subscriptions", "订阅", Icons.AutoMirrored.Rounded.List)
    object Nodes : Screen("nodes", "节点", Icons.Rounded.Dns)
    object Settings : Screen("settings", "设置", Icons.Rounded.Settings)
}

@Composable
fun AppNavigation(
    state: VpnState,
    coreStatus: CoreStatus?,
    coreVersion: String?,
    currentMode: ClashModeManager.ClashMode?,
    isModeSupported: Boolean,
    subscriptions: SubscriptionState,
    nameInput: String,
    urlInput: String,
    updatingId: String?,
    groups: List<OutboundGroupModel>,
    // Actions
    onConnect: () -> Unit,
    onDisconnect: () -> Unit,
    onSwitchMode: (ClashModeManager.ClashMode) -> Unit,
    onAddSubscription: (String, String) -> Unit,
    onImportNodes: (String, String) -> Unit,
    onEditSubscription: (String, String, String) -> Unit,
    onRemoveSubscription: (String) -> Unit,
    onSelectSubscription: (String) -> Unit,
    onUpdateSubscription: (String) -> Unit,
    onSelectNode: (String, String) -> Unit,
    onTestNode: (String) -> Unit,
    onShowMessage: (MessageDialogState) -> Unit
) {
    val navController = rememberNavController()
    val scheme = MaterialTheme.colorScheme

    val statusColor by animateColorAsState(
        targetValue = when (state) {
            VpnState.CONNECTED -> scheme.secondary
            VpnState.CONNECTING -> scheme.tertiary
            VpnState.ERROR -> scheme.error
            VpnState.IDLE -> scheme.primary
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

    val screens = listOf(Screen.Home, Screen.Subscriptions, Screen.Nodes, Screen.Settings)

    Scaffold(
        containerColor = Color.Transparent,
        bottomBar = {
            NavigationBar(
                containerColor = scheme.surface.copy(alpha = 0.95f)
            ) {
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentDestination = navBackStackEntry?.destination
                screens.forEach { screen ->
                    NavigationBarItem(
                        icon = { Icon(screen.icon, contentDescription = screen.label) },
                        label = { Text(screen.label) },
                        selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true,
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = scheme.onSecondaryContainer,
                            selectedTextColor = scheme.onSecondaryContainer,
                            indicatorColor = scheme.secondaryContainer,
                            unselectedIconColor = scheme.onSurfaceVariant,
                            unselectedTextColor = scheme.onSurfaceVariant
                        ),
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
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            AppBackground(modifier = Modifier.fillMaxSize())
            NavHost(
                navController = navController,
                startDestination = Screen.Home.route,
                modifier = Modifier.fillMaxSize()
            ) {
                composable(Screen.Home.route) {
                    HomeScreen(
                        state = state,
                        statusText = statusText,
                        statusColor = statusColor,
                        actionText = actionText,
                        coreStatus = coreStatus,
                        coreVersion = coreVersion,
                        groups = groups,
                        currentMode = currentMode,
                        isModeSupported = isModeSupported,
                        onConnect = onConnect,
                        onDisconnect = onDisconnect,
                        onSwitchMode = onSwitchMode
                    )
                }
                composable(Screen.Subscriptions.route) {
                    SubscriptionScreen(
                        subscriptions = subscriptions,
                        updatingId = updatingId,
                        onAddSubscription = onAddSubscription,
                        onImportNodes = onImportNodes,
                        onEditSubscription = onEditSubscription,
                        onRemoveSubscription = onRemoveSubscription,
                        onSelectSubscription = onSelectSubscription,
                        onUpdateSubscription = { id ->
                            onUpdateSubscription(id)
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
                composable(Screen.Settings.route) {
                    SettingsScreen(
                        onShowMessage = onShowMessage
                    )
                }
            }
        }
    }
}
