package cn.moncn.sing_box_windows.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import cn.moncn.sing_box_windows.config.SubscriptionState
import cn.moncn.sing_box_windows.core.CoreStatus
import cn.moncn.sing_box_windows.core.OutboundGroupModel
import cn.moncn.sing_box_windows.ui.theme.Amber500
import cn.moncn.sing_box_windows.ui.theme.Moss500
import cn.moncn.sing_box_windows.ui.theme.Teal600
import cn.moncn.sing_box_windows.vpn.VpnState
import io.nekohasekai.libbox.Libbox
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun MainScreen(
    state: VpnState,
    error: String?,
    coreStatus: CoreStatus?,
    coreVersion: String?,
    subscriptions: SubscriptionState,
    nameInput: String,
    urlInput: String,
    updateMessage: String?,
    updating: Boolean,
    groups: List<OutboundGroupModel>,
    onConnect: () -> Unit,
    onDisconnect: () -> Unit,
    onNameChange: (String) -> Unit,
    onUrlChange: (String) -> Unit,
    onAddSubscription: () -> Unit,
    onSelectSubscription: (String) -> Unit,
    onRemoveSubscription: (String) -> Unit,
    onUpdateSubscription: () -> Unit,
    onSelectNode: (String, String) -> Unit
) {
    val accent = when (state) {
        VpnState.CONNECTED -> Teal600
        VpnState.CONNECTING -> Amber500
        VpnState.ERROR -> Moss500
        VpnState.IDLE -> Teal600
    }
    val statusText = when (state) {
        VpnState.CONNECTED -> "已连接"
        VpnState.CONNECTING -> "连接中"
        VpnState.ERROR -> "错误"
        VpnState.IDLE -> "未连接"
    }
    val actionText = if (state == VpnState.CONNECTED || state == VpnState.CONNECTING) {
        "断开"
    } else {
        "连接"
    }
    val selected = subscriptions.selected()
    val dateFormatter = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US)
    var currentTab by remember { mutableStateOf(MainTab.Home) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        MaterialTheme.colorScheme.background,
                        MaterialTheme.colorScheme.surfaceVariant
                    )
                )
            )
            .padding(24.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "sing-box-windows",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "基于 sing-box 内核的移动代理",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                )
            }

            TabRow(
                selectedTabIndex = currentTab.ordinal,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.onSurface
            ) {
                MainTab.entries.forEach { tab ->
                    Tab(
                        selected = currentTab == tab,
                        onClick = { currentTab = tab },
                        text = { Text(tab.title) }
                    )
                }
            }

            when (currentTab) {
                MainTab.Home -> HomeTab(
                    state = state,
                    error = error,
                    statusText = statusText,
                    actionText = actionText,
                    accent = accent,
                    coreStatus = coreStatus,
                    coreVersion = coreVersion,
                    onConnect = onConnect,
                    onDisconnect = onDisconnect
                )
                MainTab.Subscriptions -> SubscriptionTab(
                    accent = accent,
                    selectedName = selected?.name,
                    subscriptions = subscriptions,
                    nameInput = nameInput,
                    urlInput = urlInput,
                    updateMessage = updateMessage,
                    updating = updating,
                    dateFormatter = dateFormatter,
                    onNameChange = onNameChange,
                    onUrlChange = onUrlChange,
                    onAddSubscription = onAddSubscription,
                    onSelectSubscription = onSelectSubscription,
                    onRemoveSubscription = onRemoveSubscription,
                    onUpdateSubscription = onUpdateSubscription
                )
                MainTab.Nodes -> NodesTab(
                    groups = groups,
                    accent = accent,
                    onSelectNode = onSelectNode
                )
            }
        }
    }
}

@Composable
private fun HomeTab(
    state: VpnState,
    error: String?,
    statusText: String,
    actionText: String,
    accent: androidx.compose.ui.graphics.Color,
    coreStatus: CoreStatus?,
    coreVersion: String?,
    onConnect: () -> Unit,
    onDisconnect: () -> Unit
) {
    val timeFormatter = remember { SimpleDateFormat("HH:mm:ss", Locale.US) }
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    StatusRow(color = accent, label = statusText)
                    Text(
                        text = "模式：VPN",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (!error.isNullOrBlank()) {
                        Text(
                            text = error,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                }
            }
        }
        item {
            val status = coreStatus
            val updatedAt = status?.updatedAt?.let { timeFormatter.format(Date(it)) } ?: "—"
            val trafficAvailable = status?.trafficAvailable == true
            val coreVersionText = coreVersion?.takeIf { it.isNotBlank() } ?: "—"
            val memoryText = status?.let { formatMemory(it.memoryBytes) } ?: "—"
            val goroutinesText = status?.goroutines?.toString() ?: "—"
            val connectionsText = status?.let { "${it.connectionsIn}/${it.connectionsOut}" } ?: "—"
            val uplinkText = if (trafficAvailable && status != null) {
                formatSpeed(status.uplinkBytes)
            } else {
                "—"
            }
            val downlinkText = if (trafficAvailable && status != null) {
                formatSpeed(status.downlinkBytes)
            } else {
                "—"
            }
            val uplinkTotalText = if (trafficAvailable && status != null) {
                formatBytes(status.uplinkTotalBytes)
            } else {
                "—"
            }
            val downlinkTotalText = if (trafficAvailable && status != null) {
                formatBytes(status.downlinkTotalBytes)
            } else {
                "—"
            }
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = "核心与流量",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "最近更新：$updatedAt",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        }
                        Text(
                            text = if (state == VpnState.CONNECTED) "运行中" else "未连接",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        StatItem(
                            label = "内核版本",
                            value = coreVersionText,
                            modifier = Modifier.weight(1f)
                        )
                        StatItem(
                            label = "内存",
                            value = memoryText,
                            modifier = Modifier.weight(1f)
                        )
                        StatItem(
                            label = "协程",
                            value = goroutinesText,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        StatItem(
                            label = "连接(入/出)",
                            value = connectionsText,
                            modifier = Modifier.weight(1f)
                        )
                        StatItem(
                            label = "流量统计",
                            value = if (trafficAvailable) "已开启" else "未开启",
                            modifier = Modifier.weight(1f)
                        )
                        StatItem(
                            label = "状态",
                            value = statusText,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        StatItem(
                            label = "上行速率",
                            value = uplinkText,
                            modifier = Modifier.weight(1f)
                        )
                        StatItem(
                            label = "下行速率",
                            value = downlinkText,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        StatItem(
                            label = "上行总量",
                            value = uplinkTotalText,
                            modifier = Modifier.weight(1f)
                        )
                        StatItem(
                            label = "下行总量",
                            value = downlinkTotalText,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    if (!trafficAvailable) {
                        Text(
                            text = "流量统计未开启，可检查 sing-box 配置中的 stats/实验选项。",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                }
            }
        }
        item {
            Button(
                onClick = {
                    if (state == VpnState.CONNECTED || state == VpnState.CONNECTING) {
                        onDisconnect()
                    } else {
                        onConnect()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = accent),
                enabled = state != VpnState.CONNECTING
            ) {
                Text(text = actionText, style = MaterialTheme.typography.labelLarge)
            }
        }
        item { Spacer(modifier = Modifier.height(4.dp)) }
    }
}

@Composable
private fun SubscriptionTab(
    accent: androidx.compose.ui.graphics.Color,
    selectedName: String?,
    subscriptions: SubscriptionState,
    nameInput: String,
    urlInput: String,
    updateMessage: String?,
    updating: Boolean,
    dateFormatter: SimpleDateFormat,
    onNameChange: (String) -> Unit,
    onUrlChange: (String) -> Unit,
    onAddSubscription: () -> Unit,
    onSelectSubscription: (String) -> Unit,
    onRemoveSubscription: (String) -> Unit,
    onUpdateSubscription: () -> Unit
) {
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = "订阅管理",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = selectedName ?: "未选择订阅",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Button(
                            onClick = onUpdateSubscription,
                            enabled = !updating && selectedName != null,
                            colors = ButtonDefaults.buttonColors(containerColor = accent)
                        ) {
                            Text(text = if (updating) "更新中..." else "更新订阅")
                        }
                    }

                    if (!updateMessage.isNullOrBlank()) {
                        Text(
                            text = updateMessage,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }

                    OutlinedTextField(
                        value = nameInput,
                        onValueChange = onNameChange,
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("名称（可选）") },
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = urlInput,
                        onValueChange = onUrlChange,
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("订阅地址") },
                        singleLine = true
                    )
                    Button(
                        onClick = onAddSubscription,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = accent)
                    ) {
                        Text(text = "添加订阅")
                    }
                }
            }
        }

        if (subscriptions.items.isEmpty()) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text(
                            text = "暂无订阅",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                    }
                }
            }
        } else {
            items(subscriptions.items, key = { it.id }) { item ->
                val selectedItem = subscriptions.selectedId == item.id
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = if (selectedItem) {
                            accent.copy(alpha = 0.12f)
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant
                        }
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = item.name,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = item.url,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        val updateText = item.lastUpdatedAt?.let {
                            "更新时间：${dateFormatter.format(Date(it))}"
                        } ?: "更新时间：-"
                        Text(
                            text = updateText,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                        if (!item.lastError.isNullOrBlank()) {
                            Text(
                                text = "错误：${item.lastError}",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Button(
                                onClick = { onSelectSubscription(item.id) },
                                enabled = !selectedItem,
                                colors = ButtonDefaults.buttonColors(containerColor = accent)
                            ) {
                                Text(text = if (selectedItem) "已选中" else "选择")
                            }
                            OutlinedButton(onClick = { onRemoveSubscription(item.id) }) {
                                Text(text = "删除")
                            }
                        }
                    }
                }
            }
        }
        item { Spacer(modifier = Modifier.height(4.dp)) }
    }
}

@Composable
private fun NodesTab(
    groups: List<OutboundGroupModel>,
    accent: androidx.compose.ui.graphics.Color,
    onSelectNode: (String, String) -> Unit
) {
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "节点选择",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "请在连接后选择分组节点。",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                }
            }
        }

        if (groups.isEmpty()) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text(
                            text = "暂无节点信息，请先更新订阅并连接。",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                    }
                }
            }
        } else {
            items(groups, key = { it.tag }) { group ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = group.tag,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "当前：${group.selected}",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (!group.selectable) {
                            Text(
                                text = "该组为自动策略，暂不支持手动选择。",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        }
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            group.items.forEach { item ->
                                val isSelected = group.selected == item.tag
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = item.tag,
                                            style = MaterialTheme.typography.bodyLarge,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        val delayText = item.delayMs?.let { "${it}ms" } ?: "未测速"
                                        Text(
                                            text = "延迟：$delayText",
                                            style = MaterialTheme.typography.bodyLarge,
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                        )
                                    }
                                    Button(
                                        onClick = { onSelectNode(group.tag, item.tag) },
                                        enabled = group.selectable && !isSelected,
                                        colors = ButtonDefaults.buttonColors(containerColor = accent)
                                    ) {
                                        Text(text = if (isSelected) "已选" else "选择")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        item { Spacer(modifier = Modifier.height(4.dp)) }
    }
}

@Composable
private fun StatusRow(color: androidx.compose.ui.graphics.Color, label: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(12.dp)
                .clip(CircleShape)
                .background(color)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun StatItem(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

private fun formatBytes(value: Long): String {
    if (value <= 0) return "0B"
    return runCatching { Libbox.formatBytes(value) }.getOrElse { "${value}B" }
}

private fun formatMemory(value: Long): String {
    if (value <= 0) return "0B"
    return runCatching { Libbox.formatMemoryBytes(value) }.getOrElse { formatBytes(value) }
}

private fun formatSpeed(value: Long): String {
    if (value <= 0) return "0B/s"
    return "${formatBytes(value)}/s"
}

private enum class MainTab(val title: String) {
    Home("主页"),
    Subscriptions("订阅"),
    Nodes("节点")
}
