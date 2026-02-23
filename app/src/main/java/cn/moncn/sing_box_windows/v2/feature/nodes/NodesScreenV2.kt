package cn.moncn.sing_box_windows.v2.feature.nodes

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import cn.moncn.sing_box_windows.core.OutboundGroupModel
import cn.moncn.sing_box_windows.core.OutboundItemModel
import cn.moncn.sing_box_windows.v2.domain.nodes.NodesGateway

@Composable
fun NodesRouteV2(
    gateway: NodesGateway
) {
    val viewModel: NodesViewModel = viewModel(factory = NodesViewModel.factory(gateway))
    val state by viewModel.state.collectAsState()

    if (!state.error.isNullOrBlank()) {
        AlertDialog(
            onDismissRequest = { viewModel.submitIntent(NodesIntent.ClearError) },
            title = { Text("节点操作失败") },
            text = { Text(state.error.orEmpty()) },
            confirmButton = {
                TextButton(onClick = { viewModel.submitIntent(NodesIntent.ClearError) }) {
                    Text("知道了")
                }
            }
        )
    }

    NodesScreenV2(
        state = state,
        onSelect = { groupTag, nodeTag -> viewModel.submitIntent(NodesIntent.Select(groupTag, nodeTag)) },
        onTest = { tag -> viewModel.submitIntent(NodesIntent.Test(tag)) }
    )
}

@Composable
private fun NodesScreenV2(
    state: NodesUiState,
    onSelect: (String, String) -> Unit,
    onTest: (String) -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    val expandedMap = remember { mutableStateMapOf<String, Boolean>() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(Color(0xFFF6FAFF), Color(0xFFEBF2FA), Color(0xFFF9FCFF))
                )
            )
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "节点面板",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "分组选择、节点切换与延迟测试",
                        style = MaterialTheme.typography.bodyMedium,
                        color = scheme.onSurfaceVariant
                    )
                }
            }

            if (state.groups.isEmpty()) {
                item {
                    Text(
                        text = "暂无节点数据，连接后会自动加载。",
                        style = MaterialTheme.typography.bodyMedium,
                        color = scheme.onSurfaceVariant
                    )
                }
            }

            items(state.groups, key = { it.tag }) { group ->
                val expanded = expandedMap[group.tag] ?: false
                GroupCard(
                    group = group,
                    expanded = expanded,
                    onToggle = { expandedMap[group.tag] = !expanded },
                    onSelect = onSelect,
                    onTest = onTest
                )
            }

            item { Spacer(modifier = Modifier.height(20.dp)) }
        }
    }
}

@Composable
private fun GroupCard(
    group: OutboundGroupModel,
    expanded: Boolean,
    onToggle: () -> Unit,
    onSelect: (String, String) -> Unit,
    onTest: (String) -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = scheme.surface.copy(alpha = 0.94f))
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onToggle() },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(group.tag, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Text(
                        "已选: ${group.selected} · ${group.items.size} 节点",
                        style = MaterialTheme.typography.bodySmall,
                        color = scheme.onSurfaceVariant
                    )
                }
                Icon(
                    imageVector = Icons.Rounded.KeyboardArrowDown,
                    contentDescription = if (expanded) "收起" else "展开",
                    modifier = Modifier.rotate(if (expanded) 180f else 0f)
                )
            }

            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    group.items.forEach { node ->
                        NodeRow(
                            groupTag = group.tag,
                            node = node,
                            selected = group.selected == node.tag,
                            selectable = group.selectable,
                            onSelect = onSelect,
                            onTest = onTest
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun NodeRow(
    groupTag: String,
    node: OutboundItemModel,
    selected: Boolean,
    selectable: Boolean,
    onSelect: (String, String) -> Unit,
    onTest: (String) -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = selectable && !selected) { onSelect(groupTag, node.tag) },
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (selected) {
                scheme.primaryContainer.copy(alpha = 0.45f)
            } else {
                scheme.surface
            }
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = node.tag,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
                )
                Text(
                    text = formatDelay(node.delayMs),
                    style = MaterialTheme.typography.labelSmall,
                    color = scheme.onSurfaceVariant
                )
            }
            IconButton(onClick = { onTest(node.tag) }) {
                Icon(Icons.Rounded.Speed, contentDescription = "测速")
            }
        }
    }
}

private fun formatDelay(delayMs: Int?): String {
    if (delayMs == null || delayMs <= 0) return "延迟：未知"
    return "延迟：${delayMs} ms"
}
