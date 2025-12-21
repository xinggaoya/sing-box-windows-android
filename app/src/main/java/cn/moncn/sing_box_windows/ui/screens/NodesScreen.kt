package cn.moncn.sing_box_windows.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cn.moncn.sing_box_windows.core.OutboundGroupModel
import cn.moncn.sing_box_windows.core.OutboundItemModel
import cn.moncn.sing_box_windows.ui.components.AppCard
import cn.moncn.sing_box_windows.ui.components.StatusBadge

@Composable
fun NodesScreen(
    groups: List<OutboundGroupModel>,
    onSelectNode: (String, String) -> Unit,
    onTestNode: (String) -> Unit
) {
    // Keep track of expanded groups locally
    val expandedStates = remember { mutableStateMapOf<String, Boolean>() }

    // Initialize first group as expanded if map is empty and groups exist
    if (expandedStates.isEmpty() && groups.isNotEmpty()) {
        expandedStates[groups.first().tag] = true
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
         item {
            Text(
                text = "代理分组",
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }

        if (groups.isEmpty()) {
            item {
                AppCard(modifier = Modifier.fillMaxWidth()) {
                    Text("暂无节点分组，请先添加订阅并连接。", color = MaterialTheme.colorScheme.secondary)
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
    AppCard(modifier = Modifier.fillMaxWidth()) {
        Column {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onToggle() }
                    .padding(vertical = 4.dp), // Check padding vs Card internal padding
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(text = group.tag, style = MaterialTheme.typography.titleMedium)
                    Text(
                        text = "${group.type} • ${group.selected}", 
                        style = MaterialTheme.typography.bodySmall, 
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = onToggle) {
                    Icon(
                        imageVector = if (isExpanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                        contentDescription = if (isExpanded) "折叠" else "展开"
                    )
                }
            }

            if (isExpanded) {
                androidx.compose.foundation.layout.Spacer(modifier = Modifier.padding(top = 12.dp))
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
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
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = if (isSelected) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(8.dp)
            )
            .padding(8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = node.tag, style = MaterialTheme.typography.bodyMedium)
            if (node.delayMs != null && node.delayMs > 0) {
                 Text(
                    text = "${node.delayMs}ms", 
                    style = MaterialTheme.typography.labelSmall, 
                    color = if (node.delayMs < 500) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                )
            }
        }

        Row {
             IconButton(onClick = onTest, enabled = !node.isTesting) {
                Icon(Icons.Rounded.Speed, contentDescription = "测试延迟", modifier = Modifier.padding(4.dp))
            }
            if (selectable) {
                IconButton(onClick = onSelect, enabled = !isSelected) {
                    if (isSelected) {
                        Icon(Icons.Rounded.Check, contentDescription = "已选择", tint = MaterialTheme.colorScheme.primary)
                    } else {
                        // Empty ring or check logic? Just leave it as select button
                        // Or maybe a radio button style logic?
                        // Let's use a simple radio icon or check
                         Icon(Icons.Rounded.Check, contentDescription = "选择", tint = MaterialTheme.colorScheme.outline.copy(alpha=0.3f))
                    }
                }
            }
        }
    }
}
