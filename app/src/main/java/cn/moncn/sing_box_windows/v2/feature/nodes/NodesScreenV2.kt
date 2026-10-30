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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Dns
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import cn.moncn.sing_box_windows.ui.components.AppAlertDialog
import cn.moncn.sing_box_windows.ui.components.AppBackground
import cn.moncn.sing_box_windows.ui.components.AppPageArrangement
import cn.moncn.sing_box_windows.ui.components.AppSectionCard
import cn.moncn.sing_box_windows.ui.components.AppTopBar
import cn.moncn.sing_box_windows.ui.components.EmptyState
import cn.moncn.sing_box_windows.ui.components.IconActionButton
import cn.moncn.sing_box_windows.ui.components.SelectionIndicator
import cn.moncn.sing_box_windows.ui.components.StatusChip
import cn.moncn.sing_box_windows.ui.components.StatusTone
import cn.moncn.sing_box_windows.ui.components.appPagePadding
import cn.moncn.sing_box_windows.ui.theme.AppRadius
import cn.moncn.sing_box_windows.ui.theme.AppSpace
import cn.moncn.sing_box_windows.ui.theme.LocalAppColors
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
        AppAlertDialog(
            title = "节点操作失败",
            message = state.error,
            confirmText = "知道了",
            dismissText = null,
            onDismissRequest = { viewModel.submitIntent(NodesIntent.ClearError) },
            onConfirm = { viewModel.submitIntent(NodesIntent.ClearError) }
        )
    }

    NodesScreenV2(
        state = state,
        onSelect = { groupTag, nodeTag -> viewModel.submitIntent(NodesIntent.Select(groupTag, nodeTag)) },
        onTest = { tag -> viewModel.submitIntent(NodesIntent.Test(tag)) }
    )
}

@Composable
internal fun NodesScreenV2(
    state: NodesUiState,
    onSelect: (String, String) -> Unit,
    onTest: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val expandedMap = remember { mutableStateMapOf<String, Boolean>() }

    AppBackground(glow = true) {
        LazyColumn(
            modifier = modifier.fillMaxSize(),
            contentPadding = appPagePadding(),
            verticalArrangement = AppPageArrangement
        ) {
            item(key = "header") {
                AppTopBar(
                    title = "代理",
                    subtitle = if (state.groups.isEmpty()) {
                        "分组选择、节点切换与延迟测试"
                    } else {
                        "${state.groups.size} 个代理组 · ${state.groups.sumOf { it.items.size }} 个节点"
                    }
                )
            }

            if (state.groups.isEmpty()) {
                item(key = "empty") {
                    EmptyState(
                        icon = Icons.Rounded.Dns,
                        title = "暂无代理组",
                        description = "连接内核后会自动加载代理组与节点",
                        modifier = Modifier.fillParentMaxHeight(0.7f)
                    )
                }
            } else {
                items(
                    count = state.groups.size,
                    key = { index -> state.groups[index].tag }
                ) { index ->
                    val group = state.groups[index]
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
}

@Composable
private fun GroupCard(
    group: OutboundGroup,
    expanded: Boolean,
    onToggle: () -> Unit,
    onSelect: (String, String) -> Unit,
    onTest: (String) -> Unit
) {
    val colors = LocalAppColors.current
    AppSectionCard(
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
                    tint = colors.textTertiary,
                    modifier = Modifier
                        .padding(start = AppSpace.sm)
                        .size(22.dp)
                        .rotate(if (expanded) 180f else 0f)
                )
            }
        }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(AppRadius.sm))
                .background(colors.surfaceMuted)
                .clickable { onToggle() }
                .padding(horizontal = 12.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = if (expanded) "收起节点列表" else "展开节点列表",
                style = MaterialTheme.typography.labelMedium,
                color = colors.primary
            )
            Text(
                text = "${group.items.size}",
                style = MaterialTheme.typography.labelMedium,
                color = colors.textTertiary
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
    val colors = LocalAppColors.current
    val shape = RoundedCornerShape(AppRadius.md)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(if (selected) colors.primaryContainer else colors.surfaceMuted, shape)
            .clickable(enabled = selectable) { onSelect(groupTag, node.tag) }
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AppSpace.md)
    ) {
        SelectionIndicator(selected = selected)
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = node.tag,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                color = if (selected) colors.onPrimaryContainer else colors.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "${node.type} · ${formatDelay(node.urlTestDelay)}",
                style = MaterialTheme.typography.labelSmall,
                color = if (selected) colors.onPrimaryContainer else colors.textTertiary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        IconActionButton(
            icon = Icons.Rounded.Speed,
            contentDescription = "测速",
            onClick = { onTest(node.tag) },
            tint = colors.primary
        )
    }
}

private fun formatDelay(delayMs: Int): String {
    if (delayMs <= 0) return "延迟未知"
    return "延迟 ${delayMs} ms"
}
