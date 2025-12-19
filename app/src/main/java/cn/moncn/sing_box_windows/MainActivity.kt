package cn.moncn.sing_box_windows

import android.app.Activity
import android.Manifest
import android.net.VpnService
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.content.ContextCompat
import androidx.core.app.NotificationManagerCompat
import cn.moncn.sing_box_windows.config.SubscriptionRepository
import cn.moncn.sing_box_windows.config.SubscriptionState
import cn.moncn.sing_box_windows.core.CoreStatusStore
import cn.moncn.sing_box_windows.core.LibboxManager
import cn.moncn.sing_box_windows.core.OutboundGroupManager
import cn.moncn.sing_box_windows.ui.MainScreen
import cn.moncn.sing_box_windows.ui.theme.SingboxwindowsTheme
import cn.moncn.sing_box_windows.vpn.VpnController
import cn.moncn.sing_box_windows.vpn.VpnState
import cn.moncn.sing_box_windows.vpn.VpnStateStore
import io.nekohasekai.libbox.Libbox
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        LibboxManager.initialize(this)
        enableEdgeToEdge()
        setContent {
            SingboxwindowsTheme {
                val context = LocalContext.current
                val state = VpnStateStore.state
                val error = VpnStateStore.lastError
                val scope = rememberCoroutineScope()
                val groups = OutboundGroupManager.groups
                val coreStatus = CoreStatusStore.status
                val coreVersion = remember {
                    runCatching { Libbox.version() }.getOrNull()
                }
                var subscriptions by remember { mutableStateOf(SubscriptionState.empty()) }
                var nameInput by remember { mutableStateOf("") }
                var urlInput by remember { mutableStateOf("") }
                var updateMessage by remember { mutableStateOf<String?>(null) }
                var updating by remember { mutableStateOf(false) }
                var pendingConnect by remember { mutableStateOf(false) }

                LaunchedEffect(Unit) {
                    subscriptions = withContext(Dispatchers.IO) {
                        SubscriptionRepository.load(context)
                    }
                }

                val launcher = rememberLauncherForActivityResult(
                    ActivityResultContracts.StartActivityForResult()
                ) { result ->
                    if (result.resultCode == Activity.RESULT_OK) {
                        VpnController.start(context)
                    } else {
                        VpnStateStore.update(VpnState.ERROR, "VPN 权限被拒绝")
                    }
                }

                val notificationLauncher = rememberLauncherForActivityResult(
                    ActivityResultContracts.RequestPermission()
                ) { granted ->
                    if (!granted) {
                        Toast.makeText(
                            context,
                            "通知权限被拒绝，前台通知可能无法显示",
                            Toast.LENGTH_LONG
                        ).show()
                        pendingConnect = false
                    } else if (pendingConnect) {
                        pendingConnect = false
                        val intent = VpnService.prepare(context)
                        if (intent != null) {
                            launcher.launch(intent)
                        } else {
                            VpnController.start(context)
                        }
                    }
                }

                LaunchedEffect(Unit) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        val permission = Manifest.permission.POST_NOTIFICATIONS
                        val granted = ContextCompat.checkSelfPermission(
                            context,
                            permission
                        ) == PackageManager.PERMISSION_GRANTED
                        if (!granted) {
                            notificationLauncher.launch(permission)
                        }
                    }
                }

                MainScreen(
                    state = state,
                    error = error,
                    coreStatus = coreStatus,
                    coreVersion = coreVersion,
                    subscriptions = subscriptions,
                    nameInput = nameInput,
                    urlInput = urlInput,
                    updateMessage = updateMessage,
                    updating = updating,
                    groups = groups,
                    onConnect = {
                        val notificationsEnabled =
                            NotificationManagerCompat.from(context).areNotificationsEnabled()
                        if (!notificationsEnabled) {
                            Toast.makeText(
                                context,
                                "请在系统设置中允许通知，否则无法显示常驻通知",
                                Toast.LENGTH_LONG
                            ).show()
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                val permission = Manifest.permission.POST_NOTIFICATIONS
                                val granted = ContextCompat.checkSelfPermission(
                                    context,
                                    permission
                                ) == PackageManager.PERMISSION_GRANTED
                                if (!granted) {
                                    pendingConnect = true
                                    notificationLauncher.launch(permission)
                                    return@MainScreen
                                }
                            }
                            openNotificationSettings(context)
                        }
                        val intent = VpnService.prepare(context)
                        if (intent != null) {
                            launcher.launch(intent)
                        } else {
                            VpnController.start(context)
                        }
                    },
                    onDisconnect = { VpnController.stop(context) },
                    onNameChange = { nameInput = it },
                    onUrlChange = { urlInput = it },
                    onAddSubscription = {
                        val trimmedUrl = urlInput.trim()
                        if (trimmedUrl.isBlank()) {
                            updateMessage = "请输入订阅地址"
                            return@MainScreen
                        }
                        scope.launch {
                            val newState = withContext(Dispatchers.IO) {
                                SubscriptionRepository.add(context, nameInput, trimmedUrl)
                            }
                            subscriptions = newState
                            nameInput = ""
                            urlInput = ""
                            updateMessage = "订阅已添加"
                        }
                    },
                    onSelectSubscription = { id ->
                        scope.launch {
                            subscriptions = withContext(Dispatchers.IO) {
                                SubscriptionRepository.select(context, id)
                            }
                        }
                    },
                    onRemoveSubscription = { id ->
                        scope.launch {
                            subscriptions = withContext(Dispatchers.IO) {
                                SubscriptionRepository.remove(context, id)
                            }
                            updateMessage = "订阅已删除"
                        }
                    },
                    onUpdateSubscription = {
                        if (subscriptions.selectedId == null) {
                            updateMessage = "请先选择订阅"
                            return@MainScreen
                        }
                        scope.launch {
                            updating = true
                            val result = withContext(Dispatchers.IO) {
                                SubscriptionRepository.updateSelected(context)
                            }
                            subscriptions = result.state
                            updating = false
                            updateMessage = result.message
                        }
                    },
                    onSelectNode = { groupTag, outboundTag ->
                        scope.launch {
                            OutboundGroupManager.select(groupTag, outboundTag)
                        }
                    },
                    onTestNode = { outboundTag ->
                        scope.launch {
                            OutboundGroupManager.urlTest(outboundTag)
                        }
                    })
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun MainPreview() {
    SingboxwindowsTheme {
        MainScreen(
            state = VpnState.IDLE,
            error = null,
            coreStatus = null,
            coreVersion = "1.0.0",
            subscriptions = SubscriptionState.empty(),
            nameInput = "",
            urlInput = "",
            updateMessage = null,
            updating = false,
            groups = emptyList(),
            onConnect = {},
            onDisconnect = {},
            onNameChange = {},
            onUrlChange = {},
            onAddSubscription = {},
            onSelectSubscription = {},
            onRemoveSubscription = {},
            onUpdateSubscription = {},
            onSelectNode = { _, _ -> },
            onTestNode = {})
    }
}

private fun openNotificationSettings(context: android.content.Context) {
    val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
        putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    runCatching { context.startActivity(intent) }
}
