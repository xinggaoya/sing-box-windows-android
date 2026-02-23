package cn.moncn.sing_box_windows.v2.feature.subscription

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Done
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Sync
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import cn.moncn.sing_box_windows.config.SubscriptionItem
import cn.moncn.sing_box_windows.v2.domain.subscription.SubscriptionGateway
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private enum class AddMode {
    Remote, Local
}

@Composable
fun SubscriptionRouteV2(
    gateway: SubscriptionGateway
) {
    val viewModel: SubscriptionViewModel = viewModel(
        factory = SubscriptionViewModel.factory(gateway)
    )
    val state by viewModel.state.collectAsState()

    var adding by remember { mutableStateOf(false) }
    var editingItem by remember { mutableStateOf<SubscriptionItem?>(null) }

    if (!state.error.isNullOrBlank()) {
        AlertDialog(
            onDismissRequest = { viewModel.submitIntent(SubscriptionIntent.ClearError) },
            title = { Text("操作失败") },
            text = { Text(state.error.orEmpty()) },
            confirmButton = {
                Button(onClick = { viewModel.submitIntent(SubscriptionIntent.ClearError) }) {
                    Text("知道了")
                }
            }
        )
    }

    if (!state.message.isNullOrBlank()) {
        AlertDialog(
            onDismissRequest = { viewModel.submitIntent(SubscriptionIntent.ClearMessage) },
            title = { Text("结果") },
            text = { Text(state.message.orEmpty()) },
            confirmButton = {
                Button(onClick = { viewModel.submitIntent(SubscriptionIntent.ClearMessage) }) {
                    Text("确定")
                }
            }
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

    SubscriptionScreenV2(
        state = state,
        onAddClick = { adding = true },
        onActivate = { viewModel.submitIntent(SubscriptionIntent.Activate(it)) },
        onSync = { viewModel.submitIntent(SubscriptionIntent.Sync(it)) },
        onEdit = { editingItem = it },
        onDelete = { viewModel.submitIntent(SubscriptionIntent.Remove(it)) }
    )
}

@Composable
private fun SubscriptionScreenV2(
    state: SubscriptionUiState,
    onAddClick: () -> Unit,
    onActivate: (String) -> Unit,
    onSync: (String) -> Unit,
    onEdit: (SubscriptionItem) -> Unit,
    onDelete: (String) -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = onAddClick) {
                Icon(Icons.Rounded.Add, contentDescription = "添加订阅")
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.linearGradient(
                        listOf(Color(0xFFF6FAFF), Color(0xFFEAF2FA), Color(0xFFF8FCFF))
                    )
                )
                .padding(innerPadding)
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 24.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "订阅中心",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "统一管理订阅、同步与启用",
                            style = MaterialTheme.typography.bodyMedium,
                            color = scheme.onSurfaceVariant
                        )
                    }
                }

                if (state.subscriptions.items.isEmpty() && !state.isLoading) {
                    item {
                        Text(
                            text = "暂无订阅，点击右下角按钮添加。",
                            style = MaterialTheme.typography.bodyMedium,
                            color = scheme.onSurfaceVariant
                        )
                    }
                }

                items(state.subscriptions.items, key = { it.id }) { item ->
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

                item { Spacer(modifier = Modifier.height(84.dp)) }
            }
        }
    }
}

@Composable
private fun SubscriptionItemCard(
    item: SubscriptionItem,
    selected: Boolean,
    updating: Boolean,
    onActivate: (String) -> Unit,
    onSync: (String) -> Unit,
    onEdit: (SubscriptionItem) -> Unit,
    onDelete: (String) -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = !selected) { onActivate(item.id) },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (selected) {
                scheme.primaryContainer.copy(alpha = 0.45f)
            } else {
                scheme.surface.copy(alpha = 0.94f)
            }
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.name.ifBlank { "未命名订阅" },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = if (item.isLocal) "本地节点列表" else item.url,
                        style = MaterialTheme.typography.bodySmall,
                        color = scheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                if (selected) {
                    Icon(
                        imageVector = Icons.Rounded.Done,
                        contentDescription = "已选中",
                        tint = scheme.primary
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val syncText = item.lastUpdatedAt?.let {
                    "上次更新: ${SimpleDateFormat("MM-dd HH:mm", Locale.getDefault()).format(Date(it))}"
                } ?: "尚未同步"
                Text(
                    text = syncText,
                    style = MaterialTheme.typography.labelSmall,
                    color = scheme.onSurfaceVariant
                )
                if (!item.lastError.isNullOrBlank()) {
                    Text(
                        text = "上次失败",
                        style = MaterialTheme.typography.labelSmall,
                        color = scheme.error
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row {
                    IconButton(
                        onClick = { onSync(item.id) },
                        enabled = !updating
                    ) {
                        Icon(Icons.Rounded.Sync, contentDescription = "同步")
                    }
                    IconButton(
                        onClick = { onEdit(item) },
                        enabled = !updating
                    ) {
                        Icon(Icons.Rounded.Edit, contentDescription = "编辑")
                    }
                    IconButton(
                        onClick = { onDelete(item.id) },
                        enabled = !updating
                    ) {
                        Icon(Icons.Rounded.Delete, contentDescription = "删除", tint = scheme.error)
                    }
                }
                if (!selected) {
                    TextButton(
                        onClick = { onActivate(item.id) },
                        enabled = !updating
                    ) {
                        Text("启用")
                    }
                }
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

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("添加订阅") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = { mode = AddMode.Remote }) {
                        Text(if (mode == AddMode.Remote) "远程订阅 ✓" else "远程订阅")
                    }
                    TextButton(onClick = { mode = AddMode.Local }) {
                        Text(if (mode == AddMode.Local) "本地导入 ✓" else "本地导入")
                    }
                }
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("名称（可选）") },
                    modifier = Modifier.fillMaxWidth()
                )
                if (mode == AddMode.Remote) {
                    OutlinedTextField(
                        value = remoteUrl,
                        onValueChange = { remoteUrl = it },
                        label = { Text("订阅 URL") },
                        modifier = Modifier.fillMaxWidth()
                    )
                } else {
                    OutlinedTextField(
                        value = localContent,
                        onValueChange = { localContent = it },
                        label = { Text("节点内容") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(160.dp)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (mode == AddMode.Remote) {
                        onAddRemote(name, remoteUrl)
                    } else {
                        onAddLocal(name, localContent)
                    }
                },
                enabled = if (mode == AddMode.Remote) remoteUrl.isNotBlank() else localContent.isNotBlank()
            ) {
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
private fun EditSubscriptionDialog(
    item: SubscriptionItem,
    onDismiss: () -> Unit,
    onSave: (String, String) -> Unit
) {
    var name by remember(item.id) { mutableStateOf(item.name) }
    var url by remember(item.id) { mutableStateOf(item.url) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("编辑订阅") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("名称") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = url,
                    onValueChange = { url = it },
                    label = { Text("地址") },
                    enabled = !item.isLocal,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(onClick = { onSave(name, url) }) {
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
