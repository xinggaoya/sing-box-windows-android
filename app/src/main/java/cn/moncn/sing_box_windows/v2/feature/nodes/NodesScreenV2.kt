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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Dns
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import cn.moncn.sing_box_windows.ui.components.EmptyState
import cn.moncn.sing_box_windows.ui.components.SectionCardWithHeader
import cn.moncn.sing_box_windows.ui.components.StatusChip
import cn.moncn.sing_box_windows.ui.components.StatusTone
import cn.moncn.sing_box_windows.ui.theme.AppRadius
import cn.moncn.sing_box_windows.ui.theme.AppSpace
import cn.moncn.sing_box_windows.ui.theme.LocalIndigoTokens
import cn.moncn.sing_box_windows.v2.domain.core.OutboundGroup
import cn.moncn.sing_box_windows.v2.domain.core.OutboundGroupItem
import cn.moncn.sing_box_windows.v2.domain.nodes.NodesGateway

@Composable
fun NodesRouteV2(
    gateway: NodesGateway
) {
    val viewModel: NodesViewModel = viewModel(factory = NodesViewModel.factory(gateway))
    val state by viewModel.state.collectAsStateWithLifecycle()

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
    val tokens = LocalIndigoTokens.current
    val expandedMap = remember { mutableStateMapOf<String, Boolean>() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    listOf(tokens.bgBase, tokens.bgSurface2, tokens.bgBase)
                )
            )
    ) {
        if (state.groups.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = AppSpace.xl, vertical = AppSpace.xl)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(AppSpace.xs)) {
                    Text(
                        text = "代理",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = tokens.textPrimary
                    )
                    Text(
                        text = "分组选择、节点切换与延迟测试",
                        style = MaterialTheme.typography.bodyMedium,
                        color = tokens.textTertiary
                    )
                }
                EmptyState(
                    icon = Icons.Rounded.Dns,
                    title = "暂无代理组",
                    description = "连接内核后会自动加载代理组与节点",
                    modifier = Modifier.weight(1f)
                )
            }
            return
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                horizontal = AppSpace.xl,
                vertical = AppSpace.xl
            ),
            verticalArrangement = Arrangement.spacedBy(AppSpace.md)
        ) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(AppSpace.xs)) {
                    Text(
                        text = "代理",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = tokens.textPrimary
                    )
                    Text(
                        text = "${state.groups.size} 个代理组 · ${state.groups.sumOf { it.items.size }} 个节点",
                        style = MaterialTheme.typography.bodyMedium,
                        color = tokens.textTertiary
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
        }
    }
}

@Composable
private fun GroupCard(
    group: OutboundGroup,
    expanded: Boolean,
    onToggle: () -> Unit,
    onSelect: (String, String) -> Unit,
    onTest: (String) -> Unit
) {
    val tokens = LocalIndigoTokens.current
    SectionCardWithHeader(
        title = group.tag,
        subtitle = "${group.type} · ${group.items.size} 节点",
        trailing = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                StatusChip(
                    text = group.selected.ifBlank { "未选择" },
                    tone = if (group.selected.isNotBlank()) StatusTone.PRIMARY else StatusTone.NEUTRAL
                )
                Icon(
                    imageVector = Icons.Rounded.KeyboardArrowDown,
                    contentDescription = if (expanded) "收起" else "展开",
                    tint = tokens.textTertiary,
                    modifier = Modifier
                        .padding(start = AppSpace.sm)
                        .rotate(if (expanded) 180f else 0f)
                )
            }
        }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onToggle() },
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (expanded) "收起节点列表" else "展开节点列表",
                style = MaterialTheme.typography.labelMedium,
                color = tokens.primary
            )
        }

        AnimatedVisibility(
            visible = expanded,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(AppSpace.sm)) {
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

@Composable
private fun NodeRow(
    groupTag: String,
    node: OutboundGroupItem,
    selected: Boolean,
    selectable: Boolean,
    onSelect: (String, String) -> Unit,
    onTest: (String) -> Unit
) {
    val tokens = LocalIndigoTokens.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(AppRadius.md))
            .background(if (selected) tokens.primarySoft else tokens.bgSurface2)
            .clickable(enabled = selectable && !selected) { onSelect(groupTag, node.tag) }
            .padding(horizontal = AppSpace.md, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = node.tag,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                color = if (selected) tokens.primary else tokens.textPrimary
            )
            Text(
                text = "${node.type} · ${formatDelay(node.urlTestDelay)}",
                style = MaterialTheme.typography.labelSmall,
                color = tokens.textTertiary
            )
        }
        IconButton(onClick = { onTest(node.tag) }) {
            Icon(
                imageVector = Icons.Rounded.Speed,
                contentDescription = "测速",
                tint = tokens.primary
            )
        }
    }
}

private fun formatDelay(delayMs: Int): String {
    if (delayMs <= 0) return "延迟未知"
    return "延迟 ${delayMs} ms"
}
