package cn.moncn.sing_box_windows.v2.feature.connections

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Lan
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import cn.moncn.sing_box_windows.ui.Format
import cn.moncn.sing_box_windows.ui.components.EmptyState
import cn.moncn.sing_box_windows.ui.components.MetricRow
import cn.moncn.sing_box_windows.ui.components.MetricTile
import cn.moncn.sing_box_windows.ui.components.SectionCardWithHeader
import cn.moncn.sing_box_windows.ui.components.StatusChip
import cn.moncn.sing_box_windows.ui.components.StatusTone
import cn.moncn.sing_box_windows.ui.theme.AppSpace
import cn.moncn.sing_box_windows.ui.theme.LocalIndigoTokens
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

    if (!state.error.isNullOrBlank()) {
        AlertDialog(
            onDismissRequest = { viewModel.submitIntent(ConnectionsIntent.ClearError) },
            title = { Text("连接操作失败") },
            text = { Text(state.error.orEmpty()) },
            confirmButton = {
                TextButton(onClick = { viewModel.submitIntent(ConnectionsIntent.ClearError) }) {
                    Text("知道了")
                }
            }
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
private fun ConnectionsScreenV2(
    state: ConnectionsUiState,
    onSetFilter: (ConnectionFilter) -> Unit,
    onCloseConnection: (String) -> Unit,
    onCloseAll: () -> Unit,
    modifier: Modifier = Modifier
) {
    val tokens = LocalIndigoTokens.current

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    listOf(tokens.bgBase, tokens.bgSurface2, tokens.bgBase)
                )
            )
            .padding(horizontal = AppSpace.xl, vertical = AppSpace.xl),
        verticalArrangement = Arrangement.spacedBy(AppSpace.lg)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(AppSpace.xs)) {
            Text(
                text = "连接",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = tokens.textPrimary
            )
            Text(
                text = "内核当前持有的连接与流量",
                style = MaterialTheme.typography.bodyMedium,
                color = tokens.textTertiary
            )
        }

        MetricRow {
            MetricTile(
                label = "活跃连接",
                value = state.activeCount.toString(),
                accent = tokens.success,
                modifier = Modifier.weight(1f)
            )
            MetricTile(
                label = "累计上传",
                value = Format.bytes(state.totalUplink),
                modifier = Modifier.weight(1f)
            )
            MetricTile(
                label = "累计下载",
                value = Format.bytes(state.totalDownlink),
                modifier = Modifier.weight(1f)
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(AppSpace.sm),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ConnectionFilter.entries.forEach { filter ->
                FilterChip(
                    selected = state.filter == filter,
                    onClick = { onSetFilter(filter) },
                    label = { Text(filter.label) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = tokens.primarySoft,
                        selectedLabelColor = tokens.primary
                    )
                )
            }
            if (state.connections.isNotEmpty()) {
                IconButton(onClick = onCloseAll) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = "关闭全部连接",
                        tint = tokens.error
                    )
                }
            }
        }

        if (state.visible.isEmpty()) {
            EmptyState(
                icon = Icons.Rounded.Lan,
                title = "暂无连接",
                description = "内核未报告活动连接",
                modifier = Modifier.weight(1f)
            )
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(AppSpace.sm)
            ) {
                items(state.visible, key = { it.id }) { connection ->
                    ConnectionCard(
                        connection = connection,
                        onClose = { onCloseConnection(connection.id) }
                    )
                }
            }
        }
    }
}

@Composable
private fun ConnectionCard(
    connection: CoreConnection,
    onClose: () -> Unit
) {
    val tokens = LocalIndigoTokens.current
    SectionCardWithHeader(
        title = connection.displayTarget.ifBlank { "未知目标" },
        subtitle = connection.outbound.ifBlank { connection.fromOutbound },
        trailing = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                StatusChip(
                    text = if (connection.closed) "已关闭" else connection.network.uppercase(),
                    tone = if (connection.closed) StatusTone.NEUTRAL else StatusTone.SUCCESS
                )
                IconButton(onClick = onClose) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = "关闭连接",
                        tint = tokens.textTertiary
                    )
                }
            }
        }
    ) {
        Text(
            text = "${Format.bytes(connection.uplinkTotal)} ↑  ${Format.bytes(connection.downlinkTotal)} ↓",
            style = MaterialTheme.typography.bodySmall,
            color = tokens.textSecondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        if (connection.rule.isNotBlank()) {
            Text(
                text = "规则：${connection.rule}",
                style = MaterialTheme.typography.labelSmall,
                color = tokens.textTertiary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
