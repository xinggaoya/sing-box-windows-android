package cn.moncn.sing_box_windows.v2.feature.connections

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.Lan
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.withContext
import androidx.lifecycle.viewmodel.compose.viewModel
import cn.moncn.sing_box_windows.ui.Format
import cn.moncn.sing_box_windows.ui.components.AppAlertDialog
import cn.moncn.sing_box_windows.ui.components.AppBackground
import cn.moncn.sing_box_windows.ui.components.AppListItem
import cn.moncn.sing_box_windows.ui.components.DialogTone
import cn.moncn.sing_box_windows.ui.components.AppPageArrangement
import cn.moncn.sing_box_windows.ui.components.AppSegmentedControl
import cn.moncn.sing_box_windows.ui.components.AppTopBar
import cn.moncn.sing_box_windows.ui.components.EmptyState
import cn.moncn.sing_box_windows.ui.components.IconActionButton
import cn.moncn.sing_box_windows.ui.components.MetricRow
import cn.moncn.sing_box_windows.ui.components.MetricTile
import cn.moncn.sing_box_windows.ui.components.StatusChip
import cn.moncn.sing_box_windows.ui.components.StatusTone
import cn.moncn.sing_box_windows.ui.components.appPagePadding
import cn.moncn.sing_box_windows.ui.theme.AppSpace
import cn.moncn.sing_box_windows.ui.theme.LocalAppColors
import cn.moncn.sing_box_windows.v2.domain.core.CoreConnection

@Composable
fun ConnectionsRouteV2(
    coreGateway: cn.moncn.sing_box_windows.v2.domain.core.CoreGateway,
    modifier: Modifier = Modifier
) {
    val viewModel: ConnectionsViewModel = viewModel(
        factory = ConnectionsViewModel.factory(coreGateway)
    )
    val state by viewModel.state.collectAsStateWithLifecycle()

    // 连接事件是高频流：仅在页面可见时订阅，避免无谓的代理对象堆积
    LaunchedEffect(coreGateway) {
        coreGateway.observeConnections(true)
        try {
            // 挂起直到页面离开
            awaitCancellation()
        } finally {
            // 协程被取消后无法再执行挂起调用，需切到 NonCancellable 完成退订
            withContext(NonCancellable) { coreGateway.observeConnections(false) }
        }
    }

    if (!state.error.isNullOrBlank()) {
        AppAlertDialog(
            title = "连接操作失败",
            message = state.error,
            confirmText = "知道了",
            dismissText = null,
            onDismissRequest = { viewModel.submitIntent(ConnectionsIntent.ClearError) },
            onConfirm = { viewModel.submitIntent(ConnectionsIntent.ClearError) }
        )
    }

    ConnectionsScreenV2(
        state = state,
        onSetFilter = { viewModel.submitIntent(ConnectionsIntent.SetFilter(it)) },
        onCloseConnection = { viewModel.submitIntent(ConnectionsIntent.CloseConnection(it)) },
        onCloseAll = { viewModel.submitIntent(ConnectionsIntent.CloseAll) },
        modifier = modifier
    )
}

@Composable
internal fun ConnectionsScreenV2(
    state: ConnectionsUiState,
    onSetFilter: (ConnectionFilter) -> Unit,
    onCloseConnection: (String) -> Unit,
    onCloseAll: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalAppColors.current
    var confirmCloseAll by remember { mutableStateOf(false) }

    AppBackground(glow = true) {
        LazyColumn(
            modifier = modifier.fillMaxSize(),
            contentPadding = appPagePadding(),
            verticalArrangement = AppPageArrangement
        ) {
            item(key = "header") {
                AppTopBar(
                    title = "连接",
                    subtitle = "${state.activeCount} 个活跃连接 · 共 ${state.connections.size} 条",
                    actions = {
                        IconActionButton(
                            icon = Icons.Rounded.DeleteSweep,
                            contentDescription = "关闭全部连接",
                            onClick = { confirmCloseAll = true },
                            tint = colors.error.content,
                            enabled = state.activeCount > 0
                        )
                    }
                )
            }

            item(key = "metrics") {
                MetricRow {
                    MetricTile(
                        label = "活跃",
                        value = state.activeCount.toString(),
                        icon = Icons.Rounded.Bolt,
                        accent = colors.success.content,
                        modifier = Modifier.weight(1f)
                    )
                    MetricTile(
                        label = "总上传",
                        value = Format.bytes(state.totalUplink),
                        icon = Icons.Rounded.ArrowUpward,
                        accent = colors.info.content,
                        modifier = Modifier.weight(1f)
                    )
                    MetricTile(
                        label = "总下载",
                        value = Format.bytes(state.totalDownlink),
                        icon = Icons.Rounded.ArrowDownward,
                        accent = colors.primary,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            item(key = "filter") {
                AppSegmentedControl(
                    options = ConnectionFilter.entries.toList(),
                    selected = state.filter,
                    onSelect = onSetFilter,
                    label = { it.label }
                )
            }

            if (state.visible.isEmpty()) {
                item(key = "empty") {
                    EmptyState(
                        icon = Icons.Rounded.Lan,
                        title = when (state.filter) {
                            ConnectionFilter.ALL -> "暂无连接"
                            ConnectionFilter.ACTIVE -> "没有活跃连接"
                            ConnectionFilter.CLOSED -> "没有已关闭连接"
                        },
                        description = "连接内核后，实时连接会自动显示在这里",
                        modifier = Modifier.fillParentMaxHeight(0.6f)
                    )
                }
            } else {
                items(
                    count = state.visible.size,
                    key = { index -> state.visible[index].id }
                ) { index ->
                    val connection = state.visible[index]
                    ConnectionRow(
                        connection = connection,
                        onClose = { onCloseConnection(connection.id) }
                    )
                }
            }
        }
    }

    if (confirmCloseAll) {
        AppAlertDialog(
            title = "关闭全部连接",
            message = "将断开当前全部 ${state.activeCount} 个活跃连接，确定继续吗？",
            confirmText = "全部断开",
            dismissText = "取消",
            tone = DialogTone.DANGER,
            onDismissRequest = { confirmCloseAll = false },
            onConfirm = {
                confirmCloseAll = false
                onCloseAll()
            }
        )
    }
}

@Composable
private fun ConnectionRow(
    connection: CoreConnection,
    onClose: () -> Unit
) {
    val colors = LocalAppColors.current
    AppListItem(
        title = connection.displayTarget.ifBlank { "未知目标" },
        subtitle = buildString {
            append(connection.network.ifBlank { "TCP" })
            connection.outbound.takeIf { it.isNotBlank() }?.let { append(" · $it") }
            connection.rule.takeIf { it.isNotBlank() }?.let { append(" · $it") }
        },
        leadingIcon = Icons.Rounded.Lan,
        leadingTint = if (connection.closed) colors.textTertiary else colors.primary,
        showDivider = false,
        trailing = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(AppSpace.sm)
            ) {
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "${Format.bytes(connection.uplinkTotal)} ↑  ${Format.bytes(connection.downlinkTotal)} ↓",
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.textSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    StatusChip(
                        text = if (connection.closed) "已关闭" else "活跃",
                        tone = if (connection.closed) StatusTone.NEUTRAL else StatusTone.SUCCESS
                    )
                }
                IconActionButton(
                    icon = Icons.Rounded.Close,
                    contentDescription = "关闭连接",
                    onClick = onClose,
                    tint = colors.error.content,
                    enabled = !connection.closed
                )
            }
        }
    )
}
