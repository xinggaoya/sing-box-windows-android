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
        AddSubscriptionDialog(
            name = name,
            url = url,
            onNameChange = { name = it },
            onUrlChange = { url = it },
            onDismiss = { showAddDialog = false },
            onConfirm = {
                if (url.isNotBlank()) {
                    onAddSubscription(name, url)
                    showAddDialog = false
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
                        text = item.url,
                        style = MaterialTheme.typography.bodySmall,
                        color = scheme.onSurfaceVariant,
                        maxLines = 1
                    )
                }
                if (isSelected) {
                    StatusBadge(text = "使用中", color = scheme.secondary)
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
                            StatusBadge(text = "同步中", color = scheme.tertiary)
                        }
                        item.lastError != null -> {
                            Text(
                                text = "同步失败: ${item.lastError}",
                                color = scheme.error,
                                style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier.clickable { onShowError(item.lastError!!) }
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
                    IconButton(onClick = onUpdate, enabled = !isUpdating) {
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
    onNameChange: (String) -> Unit,
    onUrlChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("添加订阅") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = onNameChange,
                    label = { Text("名称 (可选)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = url,
                    onValueChange = onUrlChange,
                    label = { Text("订阅地址") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(onClick = onConfirm) {
                Text("添加")
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
                    label = { Text("订阅地址") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
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
