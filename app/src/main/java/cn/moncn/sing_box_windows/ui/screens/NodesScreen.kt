package cn.moncn.sing_box_windows.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import cn.moncn.sing_box_windows.core.OutboundGroupModel
import cn.moncn.sing_box_windows.core.OutboundItemModel
import cn.moncn.sing_box_windows.ui.components.AppCard
import cn.moncn.sing_box_windows.ui.components.AppCardShape
import cn.moncn.sing_box_windows.ui.components.StatusBadge

@Composable
fun NodesScreen(
    groups: List<OutboundGroupModel>,
    onSelectNode: (String, String) -> Unit,
    onTestNode: (String) -> Unit
) {
    val expandedStates = remember { mutableStateMapOf<String, Boolean>() }

    if (expandedStates.isEmpty() && groups.isNotEmpty()) {
        expandedStates[groups.first().tag] = true
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "节点分组",
                    style = MaterialTheme.typography.titleLarge
                )
                Text(
                    text = "选择更合适的出口与策略",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        if (groups.isEmpty()) {
            item {
                AppCard(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "暂无节点分组，请先添加订阅并连接。",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        items(groups) { group ->
            val isExpanded = expandedStates[group.tag] ?: false
            NodeGroupCard(
                group = group,
                isExpanded = isExpanded,
                onToggle = { expandedStates[group.tag] = !isExpanded },
                onSelectNode = { nodeTag -> onSelectNode(group.tag, nodeTag) },
                onTestNode = onTestNode
            )
        }

        item {
            Spacer(modifier = Modifier.size(8.dp))
        }
    }
}

@Composable
fun NodeGroupCard(
    group: OutboundGroupModel,
    isExpanded: Boolean,
    onToggle: () -> Unit,
    onSelectNode: (String) -> Unit,
    onTestNode: (String) -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    AppCard(modifier = Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onToggle() }
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(text = group.tag, style = MaterialTheme.typography.titleMedium)
                    Text(
                        text = "${group.type} • 已选 ${group.selected}",
                        style = MaterialTheme.typography.labelSmall,
                        color = scheme.onSurfaceVariant
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    StatusBadge(
                        text = "${group.items.size} 个节点",
                        color = scheme.secondary
                    )
                    IconButton(onClick = onToggle) {
                        Icon(
                            imageVector = if (isExpanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                            contentDescription = if (isExpanded) "折叠" else "展开"
                        )
                    }
                }
            }

            if (isExpanded) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    group.items.forEach { node ->
                        NodeItemRow(
                            node = node,
                            isSelected = group.selected == node.tag,
                            selectable = group.selectable,
                            onSelect = { onSelectNode(node.tag) },
                            onTest = { onTestNode(node.tag) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun NodeItemRow(
    node: OutboundItemModel,
    isSelected: Boolean,
    selectable: Boolean,
    onSelect: () -> Unit,
    onTest: () -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    val delayMs = node.delayMs
    val delayColor = when {
        delayMs == null || delayMs <= 0 -> scheme.outline
        delayMs < 150 -> scheme.secondary
        delayMs < 500 -> scheme.tertiary
        else -> scheme.error
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.Transparent)
            .clickable(enabled = selectable) { onSelect() },
        color = if (isSelected) scheme.secondaryContainer else scheme.surface,
        shape = AppCardShape,
        border = BorderStroke(1.dp, if (isSelected) scheme.secondary else scheme.outline.copy(alpha = 0.2f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(text = node.tag, style = MaterialTheme.typography.bodyLarge)
                if (delayMs != null && delayMs > 0) {
                    StatusBadge(text = "${delayMs}ms", color = delayColor)
                } else {
                    Text(
                        text = "延迟未知",
                        style = MaterialTheme.typography.labelSmall,
                        color = scheme.onSurfaceVariant
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onTest, enabled = !node.isTesting) {
                    Icon(
                        imageVector = Icons.Rounded.Speed,
                        contentDescription = "测试延迟"
                    )
                }
                if (selectable) {
                    IconButton(onClick = onSelect, enabled = !isSelected) {
                        Icon(
                            imageVector = Icons.Rounded.Check,
                            contentDescription = if (isSelected) "已选择" else "选择",
                            tint = if (isSelected) scheme.secondary else scheme.outline
                        )
                    }
                }
            }
        }
    }
}
