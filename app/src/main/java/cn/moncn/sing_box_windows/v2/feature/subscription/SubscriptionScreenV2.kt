package cn.moncn.sing_box_windows.v2.feature.subscription

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.RssFeed
import androidx.compose.material.icons.rounded.Sync
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import cn.moncn.sing_box_windows.ui.Format
import cn.moncn.sing_box_windows.ui.components.AppAlertDialog
import cn.moncn.sing_box_windows.ui.components.AppBackground
import cn.moncn.sing_box_windows.ui.components.AppCard
import cn.moncn.sing_box_windows.ui.components.AppInlineBanner
import cn.moncn.sing_box_windows.ui.components.AppPageArrangement
import cn.moncn.sing_box_windows.ui.components.AppSegmentedControl
import cn.moncn.sing_box_windows.ui.components.AppTextField
import cn.moncn.sing_box_windows.ui.components.AppTopBar
import cn.moncn.sing_box_windows.ui.components.DialogTone
import cn.moncn.sing_box_windows.ui.components.EmptyState
import cn.moncn.sing_box_windows.ui.components.IconActionButton
import cn.moncn.sing_box_windows.ui.components.StatusChip
import cn.moncn.sing_box_windows.ui.components.StatusTone
import cn.moncn.sing_box_windows.ui.components.TertiaryButton
import cn.moncn.sing_box_windows.ui.components.appPagePadding
import cn.moncn.sing_box_windows.ui.theme.AppSpace
import cn.moncn.sing_box_windows.ui.theme.LocalAppColors
import cn.moncn.sing_box_windows.v2.domain.subscription.SubscriptionGateway
import cn.moncn.sing_box_windows.v2.domain.subscription.SubscriptionItem

private enum class AddMode(val label: String) {
    Remote("远程订阅"),
    Local("本地导入")
}

@Composable
fun SubscriptionRouteV2(
    gateway: SubscriptionGateway
) {
    val viewModel: SubscriptionViewModel = viewModel(
        factory = SubscriptionViewModel.factory(gateway)
    )
    val state by viewModel.state.collectAsStateWithLifecycle()

    var adding by remember { mutableStateOf(false) }
    var editingItem by remember { mutableStateOf<SubscriptionItem?>(null) }
    var pendingRemoval by remember { mutableStateOf<SubscriptionItem?>(null) }

    if (!state.error.isNullOrBlank()) {
        AppAlertDialog(
            title = "操作失败",
            message = state.error,
            confirmText = "知道了",
            dismissText = null,
            onDismissRequest = { viewModel.submitIntent(SubscriptionIntent.ClearError) },
            onConfirm = { viewModel.submitIntent(SubscriptionIntent.ClearError) }
        )
    }

    if (adding) {
        AddSubscriptionDialog(
            onDismiss = { adding = false },
            onAddRemote = { name, url ->
                viewModel.submitIntent(SubscriptionIntent.AddRemote(name, url))
                adding = false
            },
            onAddLocal = { name, content ->
                viewModel.submitIntent(SubscriptionIntent.AddLocal(name, content))
                adding = false
            }
        )
    }

    editingItem?.let { item ->
        EditSubscriptionDialog(
            item = item,
            onDismiss = { editingItem = null },
            onSave = { name, url ->
                viewModel.submitIntent(SubscriptionIntent.Edit(item.id, name, url))
                editingItem = null
            }
        )
    }

    pendingRemoval?.let { item ->
        AppAlertDialog(
            title = "删除订阅",
            message = "确定删除「${item.name.ifBlank { "未命名订阅" }}」吗？该操作不可撤销。",
            confirmText = "删除",
            dismissText = "取消",
            tone = DialogTone.DANGER,
            onDismissRequest = { pendingRemoval = null },
            onConfirm = {
                pendingRemoval = null
                viewModel.submitIntent(SubscriptionIntent.Remove(item.id))
            }
        )
    }

    SubscriptionScreenV2(
        state = state,
        onAddClick = { adding = true },
        onActivate = { viewModel.submitIntent(SubscriptionIntent.Activate(it)) },
        onSync = { viewModel.submitIntent(SubscriptionIntent.Sync(it)) },
        onEdit = { editingItem = it },
        onDelete = { item -> pendingRemoval = item },
        onClearMessage = { viewModel.submitIntent(SubscriptionIntent.ClearMessage) }
    )
}

@Composable
internal fun SubscriptionScreenV2(
    state: SubscriptionUiState,
    onAddClick: () -> Unit,
    onActivate: (String) -> Unit,
    onSync: (String) -> Unit,
    onEdit: (SubscriptionItem) -> Unit,
    onDelete: (SubscriptionItem) -> Unit,
    onClearMessage: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalAppColors.current
    val items = state.subscriptions.items

    AppBackground(glow = true) {
        LazyColumn(
            modifier = modifier.fillMaxSize(),
            contentPadding = appPagePadding(),
            verticalArrangement = AppPageArrangement
        ) {
            item(key = "header") {
                AppTopBar(
                    title = "订阅",
                    subtitle = if (items.isEmpty()) {
                        "统一管理订阅、同步与启用"
                    } else {
                        "${items.size} 个订阅 · 当前使用 ${activeSubscriptionName(state, items)}"
                    },
                    actions = {
                        IconActionButton(
                            icon = Icons.Rounded.Add,
                            contentDescription = "添加订阅",
                            onClick = onAddClick,
                            tint = colors.primary
                        )
                    }
                )
            }

            if (!state.message.isNullOrBlank()) {
                item(key = "message") {
                    AppInlineBanner(
                        message = state.message.orEmpty(),
                        // 结果文案可能同时包含成功与提示性信息，统一使用中性信息色避免误判
                        tone = StatusTone.INFO,
                        onDismiss = onClearMessage
                    )
                }
            }

            if (items.isEmpty() && state.isLoading) {
                item(key = "loading") {
                    Box(
                        modifier = Modifier.fillParentMaxHeight(0.6f),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(28.dp),
                            strokeWidth = 2.dp,
                            color = colors.primary
                        )
                    }
                }
            } else if (items.isEmpty()) {
                item(key = "empty") {
                    EmptyState(
                        icon = Icons.Rounded.RssFeed,
                        title = "暂无订阅",
                        description = "点击右上角的加号添加远程订阅，或导入本地节点列表",
                        modifier = Modifier.fillParentMaxHeight(0.7f)
                    )
                }
            } else {
                items(
                    count = items.size,
                    key = { index -> items[index].id }
                ) { index ->
                    val item = items[index]
                    SubscriptionItemCard(
                        item = item,
                        selected = state.subscriptions.selectedId == item.id,
                        updating = state.updatingId == item.id,
                        onActivate = onActivate,
                        onSync = onSync,
                        onEdit = onEdit,
                        onDelete = onDelete
                    )
                }
            }
        }
    }
}

/** 当前生效订阅的名称，未选中时显示「无」。 */
private fun activeSubscriptionName(
    state: SubscriptionUiState,
    items: List<SubscriptionItem>
): String {
    val selectedId = state.subscriptions.selectedId ?: return "无"
    val item = items.firstOrNull { it.id == selectedId } ?: return "无"
    return item.name.ifBlank { "未命名订阅" }
}

@Composable
private fun SubscriptionItemCard(
    item: SubscriptionItem,
    selected: Boolean,
    updating: Boolean,
    onActivate: (String) -> Unit,
    onSync: (String) -> Unit,
    onEdit: (SubscriptionItem) -> Unit,
    onDelete: (SubscriptionItem) -> Unit
) {
    val colors = LocalAppColors.current
    AppCard(selected = selected) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(AppSpace.md),
            verticalAlignment = Alignment.CenterVertically
        ) {
            androidx.compose.material3.Icon(
                imageVector = if (item.isLocal) Icons.Rounded.FolderOpen else Icons.Rounded.RssFeed,
                contentDescription = null,
                tint = if (selected) colors.primary else colors.textSecondary,
                modifier = Modifier.size(20.dp)
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.name.ifBlank { "未命名订阅" },
                    style = MaterialTheme.typography.titleMedium,
                    color = colors.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = if (item.isLocal) "本地节点列表" else item.url,
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.textTertiary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            StatusChip(
                text = if (selected) "使用中" else "未启用",
                // 选中卡片本身就是品牌色底，若胶囊也用品牌色会与卡片糊在一起，
                // 这里改用「生效中」的语义色，既贴合含义又保证可读。
                tone = if (selected) StatusTone.SUCCESS else StatusTone.NEUTRAL
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            val syncText = item.lastUpdatedAt?.let {
                "上次更新 ${Format.timestamp(it)}"
            } ?: "尚未同步"
            Text(
                text = syncText,
                style = MaterialTheme.typography.labelSmall,
                color = colors.textTertiary
            )
            if (!item.lastError.isNullOrBlank()) {
                StatusChip(text = "上次同步失败", tone = StatusTone.ERROR)
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(AppSpace.xs),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TertiaryButton(
                text = "同步",
                icon = Icons.Rounded.Sync,
                onClick = { onSync(item.id) },
                enabled = !updating
            )
            TertiaryButton(
                text = "编辑",
                icon = Icons.Rounded.Edit,
                onClick = { onEdit(item) },
                enabled = !updating
            )
            TertiaryButton(
                text = "删除",
                icon = Icons.Rounded.Delete,
                onClick = { onDelete(item) },
                enabled = !updating,
                tint = colors.error.content
            )
            if (!selected) {
                TertiaryButton(
                    text = "启用",
                    onClick = { onActivate(item.id) },
                    enabled = !updating
                )
            }
        }
    }
}

@Composable
private fun AddSubscriptionDialog(
    onDismiss: () -> Unit,
    onAddRemote: (String, String) -> Unit,
    onAddLocal: (String, String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var remoteUrl by remember { mutableStateOf("") }
    var localContent by remember { mutableStateOf("") }
    var mode by remember { mutableStateOf(AddMode.Remote) }

    AppAlertDialog(
        title = "添加订阅",
        onDismissRequest = onDismiss,
        onConfirm = {
            if (mode == AddMode.Remote) {
                onAddRemote(name, remoteUrl)
            } else {
                onAddLocal(name, localContent)
            }
        },
        confirmText = "添加",
        dismissText = "取消",
        confirmEnabled = if (mode == AddMode.Remote) remoteUrl.isNotBlank() else localContent.isNotBlank(),
        content = {
            Column(verticalArrangement = Arrangement.spacedBy(AppSpace.md)) {
                AppSegmentedControl(
                    options = AddMode.entries.toList(),
                    selected = mode,
                    onSelect = { mode = it },
                    label = { it.label }
                )
                AppTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = "名称（可选）"
                )
                if (mode == AddMode.Remote) {
                    AppTextField(
                        value = remoteUrl,
                        onValueChange = { remoteUrl = it },
                        label = "订阅 URL",
                        placeholder = "https://example.com/sub",
                        // URL 大小写敏感：禁止输入法自动大写与纠错
                        keyboardType = KeyboardType.Uri,
                        keyboardCapitalization = KeyboardCapitalization.None,
                        autoCorrect = false
                    )
                } else {
                    AppTextField(
                        value = localContent,
                        onValueChange = { localContent = it },
                        label = "节点内容",
                        placeholder = "粘贴 Clash / sing-box 节点内容",
                        singleLine = false
                    )
                    Text(
                        text = "粘贴 Clash 或 sing-box 格式的节点内容",
                        style = MaterialTheme.typography.bodySmall,
                        color = LocalAppColors.current.textTertiary
                    )
                }
            }
        }
    )
}

@Composable
private fun EditSubscriptionDialog(
    item: SubscriptionItem,
    onDismiss: () -> Unit,
    onSave: (String, String) -> Unit
) {
    var name by remember(item.id) { mutableStateOf(item.name) }
    var url by remember(item.id) { mutableStateOf(item.url) }

    AppAlertDialog(
        title = "编辑订阅",
        onDismissRequest = onDismiss,
        onConfirm = { onSave(name, url) },
        confirmText = "保存",
        dismissText = "取消",
        content = {
            Column(verticalArrangement = Arrangement.spacedBy(AppSpace.md)) {
                AppTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = "名称"
                )
                AppTextField(
                    value = url,
                    onValueChange = { url = it },
                    label = "地址",
                    enabled = !item.isLocal,
                    // URL 大小写敏感：禁止输入法自动大写与纠错
                    keyboardType = KeyboardType.Uri,
                    keyboardCapitalization = KeyboardCapitalization.None,
                    autoCorrect = false
                )
            }
        }
    )
}
