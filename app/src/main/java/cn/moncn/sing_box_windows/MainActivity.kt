package cn.moncn.sing_box_windows

import android.app.Activity
import android.net.VpnService
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
            onSelectNode = { _, _ -> })
    }
}
