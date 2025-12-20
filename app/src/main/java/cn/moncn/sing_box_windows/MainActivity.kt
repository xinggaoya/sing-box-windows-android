package cn.moncn.sing_box_windows

import android.app.Activity
import android.Manifest
import android.net.VpnService
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
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
import cn.moncn.sing_box_windows.config.SubscriptionUpdateResult
import cn.moncn.sing_box_windows.core.CoreStatusStore
import cn.moncn.sing_box_windows.core.LibboxManager
import cn.moncn.sing_box_windows.core.OutboundGroupManager
import cn.moncn.sing_box_windows.ui.MessageDialogState
import cn.moncn.sing_box_windows.ui.MessageTone
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
                var updatingId by remember { mutableStateOf<String?>(null) }
                var dialogMessage by remember { mutableStateOf<MessageDialogState?>(null) }
                var lastVpnError by remember { mutableStateOf<String?>(null) }
                var pendingConnect by remember { mutableStateOf(false) }

                LaunchedEffect(Unit) {
                    subscriptions = withContext(Dispatchers.IO) {
                        SubscriptionRepository.load(context)
                    }
                }

                LaunchedEffect(error) {
                    if (!error.isNullOrBlank() && error != lastVpnError) {
                        lastVpnError = error
                        dialogMessage = MessageDialogState(
                            title = "连接错误",
                            message = error,
                            tone = MessageTone.Error
                        )
                    }
                }

                fun showMessage(
                    title: String,
                    message: String,
                    tone: MessageTone = MessageTone.Info,
                    confirmText: String = "确定",
                    dismissText: String? = null,
                    onConfirm: (() -> Unit)? = null
                ) {
                    dialogMessage = MessageDialogState(
                        title = title,
                        message = message,
                        tone = tone,
                        confirmText = confirmText,
                        dismissText = dismissText,
                        onConfirm = onConfirm
                    )
                }

                fun showSyncResult(result: SubscriptionUpdateResult) {
                    val tone = when {
                        !result.ok -> MessageTone.Error
                        result.warnings.isNotEmpty() -> MessageTone.Warning
                        else -> MessageTone.Success
                    }
                    val title = if (result.ok) "订阅同步完成" else "订阅同步失败"
                    showMessage(title, result.message, tone)
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
                        showMessage(
                            title = "通知权限",
                            message = "通知权限被拒绝，前台通知可能无法显示。",
                            tone = MessageTone.Warning
                        )
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
                    coreStatus = coreStatus,
                    coreVersion = coreVersion,
                    subscriptions = subscriptions,
                    nameInput = nameInput,
                    urlInput = urlInput,
                    updatingId = updatingId,
                    groups = groups,
                    dialogMessage = dialogMessage,
                    onDismissDialog = { dialogMessage = null },
                    onShowMessage = { dialogMessage = it },
                    onConnect = {
                        val notificationsEnabled =
                            NotificationManagerCompat.from(context).areNotificationsEnabled()
                        if (!notificationsEnabled) {
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
                            showMessage(
                                title = "通知设置",
                                message = "请在系统设置中允许通知，否则无法显示常驻通知。",
                                tone = MessageTone.Warning,
                                confirmText = "去设置",
                                dismissText = "稍后",
                                onConfirm = { openNotificationSettings(context) }
                            )
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
                            showMessage(
                                title = "提示",
                                message = "请输入订阅地址",
                                tone = MessageTone.Warning
                            )
                            return@MainScreen
                        }
                        scope.launch {
                            val addResult = withContext(Dispatchers.IO) {
                                SubscriptionRepository.add(context, nameInput, trimmedUrl)
                            }
                            subscriptions = addResult.state
                            nameInput = ""
                            urlInput = ""
                            updatingId = addResult.item.id
                            val result = withContext(Dispatchers.IO) {
                                SubscriptionRepository.activate(context, addResult.item.id)
                            }
                            subscriptions = result.state
                            updatingId = null
                            showSyncResult(result)
                        }
                    },
                    onEditSubscription = { id, name, url ->
                        scope.launch {
                            val result = withContext(Dispatchers.IO) {
                                SubscriptionRepository.edit(context, id, name, url)
                            }
                            if (!result.ok) {
                                showMessage("编辑失败", result.message, MessageTone.Error)
                                return@launch
                            }
                            subscriptions = result.state
                            if (result.urlChanged && result.state.selectedId == id) {
                                showMessage(
                                    title = "订阅已更新",
                                    message = "当前订阅地址已变更，是否立即同步？",
                                    tone = MessageTone.Warning,
                                    confirmText = "立即同步",
                                    dismissText = "稍后",
                                    onConfirm = {
                                        scope.launch {
                                            val currentId = subscriptions.selectedId
                                            updatingId = currentId
                                            val syncResult = withContext(Dispatchers.IO) {
                                                SubscriptionRepository.updateSelected(context)
                                            }
                                            subscriptions = syncResult.state
                                            updatingId = null
                                            showSyncResult(syncResult)
                                        }
                                    }
                                )
                            } else {
                                val message = if (result.urlChanged) {
                                    "订阅地址已更新，下次启用将使用新地址。"
                                } else {
                                    "订阅名称已更新。"
                                }
                                showMessage("订阅已保存", message, MessageTone.Success)
                            }
                        }
                    },
                    onSelectSubscription = { id ->
                        scope.launch {
                            updatingId = id
                            val result = withContext(Dispatchers.IO) {
                                SubscriptionRepository.activate(context, id)
                            }
                            subscriptions = result.state
                            updatingId = null
                            showSyncResult(result)
                        }
                    },
                    onRemoveSubscription = { id ->
                        scope.launch {
                            subscriptions = withContext(Dispatchers.IO) {
                                SubscriptionRepository.remove(context, id)
                            }
                            showMessage("订阅已删除", "订阅已从列表中移除。", MessageTone.Success)
                        }
                    },
                    onUpdateSubscription = {
                        if (subscriptions.selectedId == null) {
                            showMessage(
                                title = "提示",
                                message = "请先启用订阅",
                                tone = MessageTone.Warning
                            )
                            return@MainScreen
                        }
                        scope.launch {
                            val currentId = subscriptions.selectedId
                            updatingId = currentId
                            val result = withContext(Dispatchers.IO) {
                                SubscriptionRepository.updateSelected(context)
                            }
                            subscriptions = result.state
                            updatingId = null
                            showSyncResult(result)
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
            coreStatus = null,
            coreVersion = "1.0.0",
            subscriptions = SubscriptionState.empty(),
            nameInput = "",
            urlInput = "",
            updatingId = null,
            groups = emptyList(),
            dialogMessage = null,
            onDismissDialog = {},
            onShowMessage = {},
            onConnect = {},
            onDisconnect = {},
            onNameChange = {},
            onUrlChange = {},
            onAddSubscription = {},
            onEditSubscription = { _, _, _ -> },
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
