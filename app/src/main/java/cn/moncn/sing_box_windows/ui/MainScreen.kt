package cn.moncn.sing_box_windows.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.view.OnApplyWindowInsetsListener
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import cn.moncn.sing_box_windows.config.SubscriptionItem
import cn.moncn.sing_box_windows.config.SubscriptionState
import cn.moncn.sing_box_windows.core.CoreStatus
import cn.moncn.sing_box_windows.core.OutboundGroupModel
import cn.moncn.sing_box_windows.ui.theme.Amber500
import cn.moncn.sing_box_windows.ui.theme.Mint500
import cn.moncn.sing_box_windows.ui.theme.Rose500
import cn.moncn.sing_box_windows.vpn.VpnState
import io.nekohasekai.libbox.Libbox
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val CardShape = RoundedCornerShape(24.dp)
private val PillShape = RoundedCornerShape(999.dp)

enum class MessageTone {
    Info,
    Success,
    Warning,
    Error
}

data class MessageDialogState(
    val title: String,
    val message: String,
    val tone: MessageTone = MessageTone.Info,
    val confirmText: String = "确定",
    val dismissText: String? = null,
    val onConfirm: (() -> Unit)? = null
)

@Composable
fun MainScreen(
    state: VpnState,
    coreStatus: CoreStatus?,
    coreVersion: String?,
    subscriptions: SubscriptionState,
    nameInput: String,
    urlInput: String,
    updatingId: String?,
    groups: List<OutboundGroupModel>,
    dialogMessage: MessageDialogState?,
    onDismissDialog: () -> Unit,
    onShowMessage: (MessageDialogState) -> Unit,
    onConnect: () -> Unit,
    onDisconnect: () -> Unit,
    onNameChange: (String) -> Unit,
    onUrlChange: (String) -> Unit,
    onAddSubscription: () -> Unit,
    onEditSubscription: (String, String, String) -> Unit,
    onSelectSubscription: (String) -> Unit,
    onRemoveSubscription: (String) -> Unit,
    onUpdateSubscription: () -> Unit,
    onSelectNode: (String, String) -> Unit,
    onTestNode: (String) -> Unit
) {
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
        VpnState.ERROR -> "连接异常"
        VpnState.IDLE -> "未连接"
    }
    val actionText = if (state == VpnState.CONNECTED || state == VpnState.CONNECTING) {
        "断开连接"
    } else {
        "开始连接"
    }
    val selected = subscriptions.selected()
    val dateFormatter = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US)
    var currentTab by remember { mutableStateOf(MainTab.Home) }
    val statusBarPadding = rememberStatusBarPadding()

    if (dialogMessage != null) {
        MessageDialog(
            title = dialogMessage.title,
            message = dialogMessage.message,
            tone = dialogMessage.tone,
            confirmText = dialogMessage.confirmText,
            dismissText = dialogMessage.dismissText,
            onConfirm = {
                dialogMessage.onConfirm?.invoke()
                onDismissDialog()
            },
            onDismiss = onDismissDialog
        )
    }

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
    ) {
        // 背景氛围层，提升整体层次感。
        BackgroundOrnaments()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    start = 20.dp,
                    end = 20.dp,
                    top = statusBarPadding + 8.dp,
                    bottom = 12.dp
                ),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            AppHeader(statusText = statusText, statusColor = statusColor)

            TabSwitcher(
                currentTab = currentTab,
                highlightColor = MaterialTheme.colorScheme.primary,
                onTabSelected = { currentTab = it }
            )

            when (currentTab) {
                MainTab.Home -> HomeTab(
                    modifier = Modifier.weight(1f),
                    state = state,
                    statusText = statusText,
                    actionText = actionText,
                    statusColor = statusColor,
                    coreStatus = coreStatus,
                    coreVersion = coreVersion,
                    onConnect = onConnect,
                    onDisconnect = onDisconnect
                )
                MainTab.Subscriptions -> SubscriptionTab(
                    modifier = Modifier.weight(1f),
                    selectedName = selected?.name,
                    subscriptions = subscriptions,
                    nameInput = nameInput,
                    urlInput = urlInput,
                    updatingId = updatingId,
                    dateFormatter = dateFormatter,
                    onNameChange = onNameChange,
                    onUrlChange = onUrlChange,
                    onShowMessage = onShowMessage,
                    onAddSubscription = onAddSubscription,
                    onEditSubscription = onEditSubscription,
                    onSelectSubscription = onSelectSubscription,
                    onRemoveSubscription = onRemoveSubscription,
                    onUpdateSubscription = onUpdateSubscription
                )
                MainTab.Nodes -> NodesTab(
                    modifier = Modifier.weight(1f),
                    groups = groups,
                    accent = MaterialTheme.colorScheme.primary,
                    onSelectNode = onSelectNode,
                    onTestNode = onTestNode
                )
            }
        }
    }
}

@Composable
private fun HomeTab(
    modifier: Modifier,
    state: VpnState,
    statusText: String,
    actionText: String,
    statusColor: Color,
    coreStatus: CoreStatus?,
    coreVersion: String?,
    onConnect: () -> Unit,
    onDisconnect: () -> Unit
) {
    val timeFormatter = remember { SimpleDateFormat("HH:mm:ss", Locale.US) }
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

    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(bottom = 12.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        item {
            ConnectionHero(
                statusText = statusText,
                state = state,
                statusColor = statusColor,
                actionText = actionText,
                onConnect = onConnect,
                onDisconnect = onDisconnect
            )
        }
        item {
            SectionCard {
                SectionTitle(
                    title = "核心状态",
                    subtitle = "最近更新：$updatedAt"
                )
                // 统计信息可能为空，统一使用占位符避免布局跳动。
                MetricRow {
                    MetricTile(
                        label = "内核版本",
                        value = coreVersionText,
                        modifier = Modifier.weight(1f)
                    )
                    MetricTile(
                        label = "内存",
                        value = memoryText,
                        modifier = Modifier.weight(1f)
                    )
                }
                MetricRow {
                    MetricTile(
                        label = "协程",
                        value = goroutinesText,
                        modifier = Modifier.weight(1f)
                    )
                    MetricTile(
                        label = "连接(入/出)",
                        value = connectionsText,
                        modifier = Modifier.weight(1f)
                    )
                }
                MetricRow {
                    MetricTile(
                        label = "运行状态",
                        value = statusText,
                        modifier = Modifier.weight(1f),
                        highlight = state == VpnState.CONNECTED
                    )
                    MetricTile(
                        label = "统计开关",
                        value = if (trafficAvailable) "已开启" else "未开启",
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
        item {
            SectionCard {
                SectionTitle(
                    title = "流量概览",
                    subtitle = if (trafficAvailable) "实时统计中" else "统计未开启"
                )
                MetricRow {
                    MetricTile(
                        label = "上行速率",
                        value = uplinkText,
                        modifier = Modifier.weight(1f)
                    )
                    MetricTile(
                        label = "下行速率",
                        value = downlinkText,
                        modifier = Modifier.weight(1f)
                    )
                }
                MetricRow {
                    MetricTile(
                        label = "上行总量",
                        value = uplinkTotalText,
                        modifier = Modifier.weight(1f)
                    )
                    MetricTile(
                        label = "下行总量",
                        value = downlinkTotalText,
                        modifier = Modifier.weight(1f)
                    )
                }
                if (!trafficAvailable) {
                    Text(
                        text = "流量统计未开启，可在 sing-box 配置中启用 stats/实验选项。",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun SubscriptionTab(
    modifier: Modifier,
    selectedName: String?,
    subscriptions: SubscriptionState,
    nameInput: String,
    urlInput: String,
    updatingId: String?,
    dateFormatter: SimpleDateFormat,
    onNameChange: (String) -> Unit,
    onUrlChange: (String) -> Unit,
    onShowMessage: (MessageDialogState) -> Unit,
    onAddSubscription: () -> Unit,
    onEditSubscription: (String, String, String) -> Unit,
    onSelectSubscription: (String) -> Unit,
    onRemoveSubscription: (String) -> Unit,
    onUpdateSubscription: () -> Unit
) {
    var showAddDialog by remember { mutableStateOf(false) }
    var editingItem by remember { mutableStateOf<SubscriptionItem?>(null) }
    var editNameInput by remember { mutableStateOf("") }
    var editUrlInput by remember { mutableStateOf("") }
    val isUpdating = updatingId != null
    val accent = MaterialTheme.colorScheme.primary

    if (showAddDialog) {
        // 弹窗内复用输入状态，避免新增与页面状态不同步。
        AddSubscriptionDialog(
            nameInput = nameInput,
            urlInput = urlInput,
            onNameChange = onNameChange,
            onUrlChange = onUrlChange,
            onDismiss = {
                showAddDialog = false
            },
            onConfirm = {
                val trimmedUrl = urlInput.trim()
                if (trimmedUrl.isBlank()) {
                    onShowMessage(
                        MessageDialogState(
                            title = "提示",
                            message = "请输入订阅地址",
                            tone = MessageTone.Warning
                        )
                    )
                } else {
                    onAddSubscription()
                    showAddDialog = false
                }
            }
        )
    }

    if (editingItem != null) {
        EditSubscriptionDialog(
            nameInput = editNameInput,
            urlInput = editUrlInput,
            onNameChange = { editNameInput = it },
            onUrlChange = { editUrlInput = it },
            onDismiss = { editingItem = null },
            onConfirm = {
                val trimmedUrl = editUrlInput.trim()
                if (trimmedUrl.isBlank()) {
                    onShowMessage(
                        MessageDialogState(
                            title = "提示",
                            message = "请输入订阅地址",
                            tone = MessageTone.Warning
                        )
                    )
                } else {
                    onEditSubscription(editingItem!!.id, editNameInput, editUrlInput)
                    editingItem = null
                }
            }
        )
    }

    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(bottom = 12.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        item {
            SectionCard {
                SectionTitle(
                    title = "订阅管理",
                    subtitle = selectedName ?: "未启用订阅",
                    trailing = {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedButton(
                                onClick = {
                                    showAddDialog = true
                                },
                                enabled = !isUpdating
                            ) {
                                Text(text = "添加订阅")
                            }
                            Button(
                                onClick = onUpdateSubscription,
                                enabled = !isUpdating && selectedName != null,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = accent
                                )
                            ) {
                                Text(text = if (isUpdating) "同步中..." else "刷新当前")
                            }
                        }
                    }
                )
                Text(
                    text = "选择订阅后会自动下载并启用。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        if (subscriptions.items.isEmpty()) {
            item {
                SectionCard {
                    Text(
                        text = "暂无订阅，请点击上方“添加订阅”进行新增。",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            items(subscriptions.items, key = { it.id }) { item ->
                val selectedItem = subscriptions.selectedId == item.id
                val isItemUpdating = updatingId == item.id
                val highlightColor = accent
                val backgroundModifier = if (selectedItem) {
                    Modifier.background(
                        Brush.horizontalGradient(
                            listOf(
                                highlightColor.copy(alpha = 0.18f),
                                MaterialTheme.colorScheme.surface
                            )
                        )
                    )
                } else {
                    Modifier
                }
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(CardShape)
                        .border(
                            width = 1.dp,
                            color = if (selectedItem) {
                                highlightColor.copy(alpha = 0.45f)
                            } else {
                                MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                            },
                            shape = CardShape
                        ),
                    shape = CardShape,
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = if (selectedItem) 4.dp else 2.dp,
                    shadowElevation = if (selectedItem) 10.dp else 6.dp
                ) {
                    Column(
                        modifier = backgroundModifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = item.name,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = item.url,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            if (isItemUpdating) {
                                TagChip(text = "同步中", color = MaterialTheme.colorScheme.secondary)
                            } else if (selectedItem) {
                                TagChip(text = "使用中", color = highlightColor)
                            }
                        }
                        val updateText = item.lastUpdatedAt?.let {
                            "最近同步：${dateFormatter.format(Date(it))}"
                        } ?: "尚未同步"
                        Text(
                            text = updateText,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (!item.lastError.isNullOrBlank()) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                TagChip(
                                    text = "同步失败",
                                    color = MaterialTheme.colorScheme.error
                                )
                                TextButton(
                                    onClick = {
                                        onShowMessage(
                                            MessageDialogState(
                                                title = "订阅同步失败",
                                                message = item.lastError,
                                                tone = MessageTone.Error
                                            )
                                        )
                                    },
                                    enabled = !isUpdating,
                                    colors = ButtonDefaults.textButtonColors(
                                        contentColor = MaterialTheme.colorScheme.error
                                    )
                                ) {
                                    Text(text = "查看错误")
                                }
                            }
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            val actionText = if (selectedItem) "刷新订阅" else "启用并下载"
                            Button(
                                onClick = {
                                    if (selectedItem) {
                                        onUpdateSubscription()
                                    } else {
                                        onSelectSubscription(item.id)
                                    }
                                },
                                modifier = Modifier.weight(1f),
                                enabled = !isUpdating,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = highlightColor
                                )
                            ) {
                                Text(text = if (isItemUpdating) "同步中..." else actionText)
                            }
                            OutlinedButton(
                                onClick = {
                                    editingItem = item
                                    editNameInput = item.name
                                    editUrlInput = item.url
                                },
                                enabled = !isUpdating
                            ) {
                                Text(text = "编辑")
                            }
                            TextButton(
                                onClick = { onRemoveSubscription(item.id) },
                                enabled = !isUpdating,
                                colors = ButtonDefaults.textButtonColors(
                                    contentColor = MaterialTheme.colorScheme.error
                                )
                            ) {
                                Text(text = "删除")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun NodesTab(
    modifier: Modifier,
    groups: List<OutboundGroupModel>,
    accent: Color,
    onSelectNode: (String, String) -> Unit,
    onTestNode: (String) -> Unit
) {
    val timeFormatter = remember { SimpleDateFormat("HH:mm:ss", Locale.US) }
    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(bottom = 12.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        item {
            SectionCard {
                SectionTitle(title = "节点选择", subtitle = "连接后可切换分组节点")
                Text(
                    text = "支持手动选择的分组会显示“可选择”标记。",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        if (groups.isEmpty()) {
            item {
                SectionCard {
                    Text(
                        text = "暂无节点信息，请先启用订阅并建立连接。",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            items(groups, key = { it.tag }) { group ->
                Surface(
                    shape = CardShape,
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 2.dp,
                    shadowElevation = 6.dp
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = group.tag,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "当前：${group.selected}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            TagChip(
                                text = if (group.selectable) "可选择" else "自动策略",
                                color = if (group.selectable) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.secondary
                                }
                            )
                        }
                        if (!group.selectable) {
                            Text(
                                text = "该分组为自动策略，暂不支持手动选择。",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            group.items.forEach { item ->
                                val isSelected = group.selected == item.tag
                                NodeRow(
                                    tag = item.tag,
                                    delayMs = item.delayMs,
                                    lastTestAt = item.lastTestAt,
                                    isSelected = isSelected,
                                    selectable = group.selectable,
                                    accent = accent,
                                    timeFormatter = timeFormatter,
                                    onTestNode = { onTestNode(item.tag) },
                                    onSelectNode = { onSelectNode(group.tag, item.tag) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AppHeader(statusText: String, statusColor: Color) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "SingBox Windows",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = "基于 sing-box 内核的移动代理",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
            )
        }
        StatusPill(
            label = statusText,
            backgroundColor = statusColor.copy(alpha = 0.16f),
            contentColor = statusColor
        )
    }
}

@Composable
private fun TabSwitcher(
    currentTab: MainTab,
    highlightColor: Color,
    onTabSelected: (MainTab) -> Unit
) {
    // 自定义分段控件，避免默认 TabRow 的拘谨感。
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.92f))
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        MainTab.entries.forEach { tab ->
            val selected = currentTab == tab
            val textColor = if (selected) {
                MaterialTheme.colorScheme.onPrimary
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(16.dp))
                    .background(if (selected) highlightColor else Color.Transparent)
                    .clickable { onTabSelected(tab) }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = tab.title,
                    style = MaterialTheme.typography.labelLarge,
                    color = textColor
                )
            }
        }
    }
}

@Composable
private fun ConnectionHero(
    statusText: String,
    state: VpnState,
    statusColor: Color,
    actionText: String,
    onConnect: () -> Unit,
    onDisconnect: () -> Unit
) {
    val onPrimary = MaterialTheme.colorScheme.onPrimary
    val pillText = when (state) {
        VpnState.CONNECTED -> "在线"
        VpnState.CONNECTING -> "连接中"
        VpnState.ERROR -> "异常"
        VpnState.IDLE -> "离线"
    }
    Surface(
        shape = RoundedCornerShape(28.dp),
        color = Color.Transparent,
        shadowElevation = 12.dp
    ) {
        Column(
            modifier = Modifier
                .background(
                    Brush.linearGradient(
                        listOf(
                            statusColor.copy(alpha = 0.9f),
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.95f)
                        )
                    )
                )
                .padding(22.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "连接状态",
                        style = MaterialTheme.typography.labelLarge,
                        color = onPrimary.copy(alpha = 0.7f)
                    )
                    Text(
                        text = statusText,
                        style = MaterialTheme.typography.titleLarge,
                        color = onPrimary
                    )
                }
                StatusPill(
                    label = pillText,
                    backgroundColor = onPrimary.copy(alpha = 0.18f),
                    contentColor = onPrimary
                )
            }
            Text(
                text = "模式：VPN",
                style = MaterialTheme.typography.bodyMedium,
                color = onPrimary.copy(alpha = 0.85f)
            )
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
                    .height(52.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = onPrimary,
                    contentColor = statusColor
                ),
                enabled = state != VpnState.CONNECTING
            ) {
                Text(text = actionText, style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}

@Composable
private fun SectionCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = CardShape,
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
        tonalElevation = 2.dp,
        shadowElevation = 6.dp
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            content = content
        )
    }
}

@Composable
private fun SectionTitle(
    title: String,
    subtitle: String? = null,
    trailing: @Composable (() -> Unit)? = null
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (!subtitle.isNullOrBlank()) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        if (trailing != null) {
            Spacer(modifier = Modifier.size(12.dp))
            trailing()
        }
    }
}

@Composable
private fun MetricRow(content: @Composable RowScope.() -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        content = content
    )
}

@Composable
private fun MetricTile(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    highlight: Boolean = false
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        color = if (highlight) {
            MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
        } else {
            MaterialTheme.colorScheme.surfaceVariant
        }
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun StatusPill(
    label: String,
    backgroundColor: Color,
    contentColor: Color
) {
    Surface(shape = PillShape, color = backgroundColor) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(contentColor)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = contentColor
            )
        }
    }
}

@Composable
private fun TagChip(text: String, color: Color) {
    Surface(shape = PillShape, color = color.copy(alpha = 0.14f)) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelMedium,
            color = color
        )
    }
}

@Composable
private fun AddSubscriptionDialog(
    nameInput: String,
    urlInput: String,
    onNameChange: (String) -> Unit,
    onUrlChange: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(22.dp),
        title = {
            Text(text = "添加订阅", style = MaterialTheme.typography.titleMedium)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "支持 Clash/通用订阅地址。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = nameInput,
                    onValueChange = onNameChange,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("名称（可选）") },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp)
                )
                OutlinedTextField(
                    value = urlInput,
                    onValueChange = onUrlChange,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("订阅地址") },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm
            ) {
                Text(text = "添加")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = "取消")
            }
        }
    )
}

@Composable
private fun EditSubscriptionDialog(
    nameInput: String,
    urlInput: String,
    onNameChange: (String) -> Unit,
    onUrlChange: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(22.dp),
        title = {
            Text(text = "编辑订阅", style = MaterialTheme.typography.titleMedium)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "修改名称或订阅地址。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = nameInput,
                    onValueChange = onNameChange,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("名称（可选）") },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp)
                )
                OutlinedTextField(
                    value = urlInput,
                    onValueChange = onUrlChange,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("订阅地址") },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp)
                )
            }
        },
        confirmButton = {
            Button(onClick = onConfirm) {
                Text(text = "保存")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = "取消")
            }
        }
    )
}

@Composable
private fun MessageDialog(
    title: String,
    message: String,
    tone: MessageTone,
    confirmText: String,
    dismissText: String?,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    val accent = when (tone) {
        MessageTone.Info -> MaterialTheme.colorScheme.primary
        MessageTone.Success -> Mint500
        MessageTone.Warning -> Amber500
        MessageTone.Error -> Rose500
    }
    val accentSurface = accent.copy(alpha = 0.12f)
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(22.dp),
        icon = {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(accentSurface),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .clip(CircleShape)
                        .background(accent)
                )
            }
        },
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = accent
            )
        },
        text = {
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = accent)
            ) {
                Text(text = confirmText)
            }
        },
        dismissButton = {
            if (!dismissText.isNullOrBlank()) {
                TextButton(
                    onClick = onDismiss,
                    colors = ButtonDefaults.textButtonColors(contentColor = accent)
                ) {
                    Text(text = dismissText)
                }
            }
        }
    )
}
@Composable
private fun NodeRow(
    tag: String,
    delayMs: Int?,
    lastTestAt: Long?,
    isSelected: Boolean,
    selectable: Boolean,
    accent: Color,
    timeFormatter: SimpleDateFormat,
    onTestNode: () -> Unit,
    onSelectNode: () -> Unit
) {
    val delayText = delayMs?.let { "${it}ms" } ?: "未测速"
    val testTime = lastTestAt?.let { timeFormatter.format(Date(it)) } ?: "—"
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = if (isSelected) accent.copy(alpha = 0.14f) else MaterialTheme.colorScheme.surfaceVariant
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = tag,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "延迟：$delayText",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "测速时间：$testTime",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(onClick = onTestNode) {
                    Text(text = "测速")
                }
                Button(
                    onClick = onSelectNode,
                    enabled = selectable && !isSelected,
                    colors = ButtonDefaults.buttonColors(containerColor = accent)
                ) {
                    Text(text = if (isSelected) "已选" else "选择")
                }
            }
        }
    }
}

@Composable
private fun BackgroundOrnaments() {
    Box(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .size(260.dp)
                .offset(x = 160.dp, y = (-80).dp)
                .background(
                    Brush.radialGradient(
                        listOf(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.28f),
                            Color.Transparent
                        )
                    )
                )
        )
        Box(
            modifier = Modifier
                .size(320.dp)
                .offset(x = (-140).dp, y = 360.dp)
                .background(
                    Brush.radialGradient(
                        listOf(
                            MaterialTheme.colorScheme.tertiary.copy(alpha = 0.22f),
                            Color.Transparent
                        )
                    )
                )
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

@Composable
private fun rememberStatusBarPadding(): androidx.compose.ui.unit.Dp {
    val view = LocalView.current
    val density = LocalDensity.current
    var topInset by remember { mutableStateOf(0) }

    DisposableEffect(view) {
        // 使用系统 WindowInsets 获取状态栏高度，适配不同设备。
        val listener = OnApplyWindowInsetsListener { _, insets ->
            topInset = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top
            insets
        }
        ViewCompat.setOnApplyWindowInsetsListener(view, listener)
        ViewCompat.requestApplyInsets(view)
        onDispose { ViewCompat.setOnApplyWindowInsetsListener(view, null) }
    }

    return with(density) { topInset.toDp() }
}

private enum class MainTab(val title: String) {
    Home("主页"),
    Subscriptions("订阅"),
    Nodes("节点")
}
