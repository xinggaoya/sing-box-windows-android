package cn.moncn.sing_box_windows.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cn.moncn.sing_box_windows.config.SubscriptionItem
import cn.moncn.sing_box_windows.config.SubscriptionState
import cn.moncn.sing_box_windows.ui.MessageDialogState
import cn.moncn.sing_box_windows.ui.MessageTone
import cn.moncn.sing_box_windows.ui.components.AppCard
import cn.moncn.sing_box_windows.ui.components.StatusBadge
import java.text.SimpleDateFormat
import java.util.Date

@Composable
fun SubscriptionScreen(
    subscriptions: SubscriptionState,
    updatingId: String?,
    onAddSubscription: (String, String) -> Unit,
    onImportNodes: (String, String) -> Unit,
    onEditSubscription: (String, String, String) -> Unit,
    onRemoveSubscription: (String) -> Unit,
    onSelectSubscription: (String) -> Unit,
    onUpdateSubscription: (String) -> Unit,
    onShowMessage: (MessageDialogState) -> Unit
) {
    var showAddDialog by remember { mutableStateOf(false) }
    var editingItem by remember { mutableStateOf<SubscriptionItem?>(null) }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "订阅管理",
                        style = MaterialTheme.typography.titleLarge
                    )
                    Text(
                        text = "同步订阅，快速更新节点列表",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (subscriptions.items.isEmpty()) {
                item {
                    AppCard(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "暂无订阅，请点击右下角按钮添加。",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                items(subscriptions.items, key = { it.id }) { item ->
                    SubscriptionItemRow(
                        item = item,
                        isSelected = subscriptions.selectedId == item.id,
                        isUpdating = updatingId == item.id,
                        onSelect = { onSelectSubscription(item.id) },
                        onEdit = { editingItem = item },
                        onDelete = { onRemoveSubscription(item.id) },
                        onUpdate = { onUpdateSubscription(item.id) },
                        onShowError = { msg ->
                            onShowMessage(
                                MessageDialogState(
                                    "错误详情",
                                    msg,
                                    MessageTone.Error
                                )
                            )
                        }
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.padding(bottom = 96.dp))
            }
        }

        ExtendedFloatingActionButton(
            onClick = { showAddDialog = true },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(24.dp),
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            icon = { Icon(Icons.Rounded.Add, contentDescription = "添加订阅") },
            text = { Text("添加订阅") }
        )
    }

    if (showAddDialog) {
        var name by remember { mutableStateOf("") }
        var url by remember { mutableStateOf("") }
        var content by remember { mutableStateOf("") }
        var inputMode by remember { mutableStateOf(AddSubscriptionInputMode.Url) }
        AddSubscriptionDialog(
            name = name,
            url = url,
            content = content,
            inputMode = inputMode,
            onNameChange = { name = it },
            onUrlChange = { url = it },
            onContentChange = { content = it },
            onModeChange = { inputMode = it },
            onDismiss = { showAddDialog = false },
            onConfirm = {
                when (inputMode) {
                    AddSubscriptionInputMode.Url -> {
                        if (url.isNotBlank()) {
                            onAddSubscription(name, url)
                            showAddDialog = false
                        }
                    }
                    AddSubscriptionInputMode.NodeList -> {
                        if (content.isNotBlank()) {
                            onImportNodes(name, content)
                            showAddDialog = false
                        }
                    }
                }
            }
        )
    }

    editingItem?.let { item ->
        var name by remember { mutableStateOf(item.name) }
        var url by remember { mutableStateOf(item.url) }
        EditSubscriptionDialog(
            name = name,
            url = url,
            isLocal = item.isLocal,
            onNameChange = { name = it },
            onUrlChange = { url = it },
            onDismiss = { editingItem = null },
            onConfirm = {
                if (url.isNotBlank()) {
                    onEditSubscription(item.id, name, url)
                    editingItem = null
                }
            }
        )
    }
}

@Composable
fun SubscriptionItemRow(
    item: SubscriptionItem,
    isSelected: Boolean,
    isUpdating: Boolean,
    onSelect: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onUpdate: () -> Unit,
    onShowError: (String) -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    AppCard(
        modifier = Modifier.fillMaxWidth(),
        containerColor = if (isSelected) scheme.secondaryContainer else scheme.surface
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = if (item.name.isNotBlank()) item.name else "未命名订阅",
                        style = MaterialTheme.typography.titleMedium,
                        color = scheme.onSurface
                    )
                    Text(
                        text = if (item.isLocal) "本地节点列表" else item.url,
                        style = MaterialTheme.typography.bodySmall,
                        color = scheme.onSurfaceVariant,
                        maxLines = 1
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    if (item.isLocal) {
                        StatusBadge(text = "本地", color = scheme.tertiary)
                    }
                    if (isSelected) {
                        StatusBadge(text = "使用中", color = scheme.secondary)
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    when {
                        isUpdating -> {
                            StatusBadge(
                                text = if (item.isLocal) "应用中" else "同步中",
                                color = scheme.tertiary
                            )
                        }
                        item.lastError != null -> {
                            Text(
                                text = if (item.isLocal) "应用失败: ${item.lastError}" else "同步失败: ${item.lastError}",
                                color = scheme.error,
                                style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier.clickable { onShowError(item.lastError!!) }
                            )
                        }
                        item.isLocal -> {
                            Text(
                                text = "本地订阅，不支持同步更新",
                                style = MaterialTheme.typography.labelSmall,
                                color = scheme.outline
                            )
                        }
                        else -> {
                            val date = item.lastUpdatedAt?.let { Date(it) }
                            val dateStr = if (date != null) {
                                SimpleDateFormat.getDateTimeInstance().format(date)
                            } else {
                                "从未"
                            }
                            Text(
                                text = "更新于: $dateStr",
                                style = MaterialTheme.typography.labelSmall,
                                color = scheme.outline
                            )
                        }
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onUpdate, enabled = !isUpdating && !item.isLocal) {
                        Icon(Icons.Rounded.Refresh, contentDescription = "更新")
                    }
                    IconButton(onClick = onEdit) {
                        Icon(Icons.Rounded.Edit, contentDescription = "编辑")
                    }
                    IconButton(onClick = onDelete) {
                        Icon(
                            Icons.Rounded.Delete,
                            contentDescription = "删除",
                            tint = scheme.error
                        )
                    }
                    if (!isSelected) {
                        FilledTonalButton(
                            onClick = onSelect,
                            enabled = !isUpdating,
                            modifier = Modifier.padding(start = 8.dp)
                        ) {
                            Text("启用")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AddSubscriptionDialog(
    name: String,
    url: String,
    content: String,
    inputMode: AddSubscriptionInputMode,
    onNameChange: (String) -> Unit,
    onUrlChange: (String) -> Unit,
    onContentChange: (String) -> Unit,
    onModeChange: (AddSubscriptionInputMode) -> Unit,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("添加订阅") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (inputMode == AddSubscriptionInputMode.Url) {
                        FilledTonalButton(onClick = { onModeChange(AddSubscriptionInputMode.Url) }) {
                            Text("订阅地址")
                        }
                        TextButton(onClick = { onModeChange(AddSubscriptionInputMode.NodeList) }) {
                            Text("节点列表")
                        }
                    } else {
                        TextButton(onClick = { onModeChange(AddSubscriptionInputMode.Url) }) {
                            Text("订阅地址")
                        }
                        FilledTonalButton(onClick = { onModeChange(AddSubscriptionInputMode.NodeList) }) {
                            Text("节点列表")
                        }
                    }
                }
                OutlinedTextField(
                    value = name,
                    onValueChange = onNameChange,
                    label = { Text("名称 (可选)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                if (inputMode == AddSubscriptionInputMode.Url) {
                    OutlinedTextField(
                        value = url,
                        onValueChange = onUrlChange,
                        label = { Text("订阅地址") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                } else {
                    OutlinedTextField(
                        value = content,
                        onValueChange = onContentChange,
                        label = { Text("节点列表 (每行一条)") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 4
                    )
                    Text(
                        text = "支持 vless://、vmess://、ss://、ssr://、trojan://、hysteria2://、tuic://",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "节点列表会生成配置并保存为本地订阅，无法在线更新。",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        confirmButton = {
            val canSubmit = when (inputMode) {
                AddSubscriptionInputMode.Url -> url.isNotBlank()
                AddSubscriptionInputMode.NodeList -> content.isNotBlank()
            }
            val buttonText = if (inputMode == AddSubscriptionInputMode.Url) "添加" else "导入并保存"
            Button(onClick = onConfirm, enabled = canSubmit) {
                Text(buttonText)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}

@Composable
fun EditSubscriptionDialog(
    name: String,
    url: String,
    isLocal: Boolean,
    onNameChange: (String) -> Unit,
    onUrlChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("编辑订阅") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = onNameChange,
                    label = { Text("名称") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = url,
                    onValueChange = onUrlChange,
                    label = { Text(if (isLocal) "节点来源" else "订阅地址") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isLocal
                )
                if (isLocal) {
                    Text(
                        text = "本地订阅无法更新内容，如需修改请重新导入。",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        confirmButton = {
            Button(onClick = onConfirm) {
                Text("保存")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}

enum class AddSubscriptionInputMode {
    Url,
    NodeList
}
