package cn.moncn.sing_box_windows.v2.feature.outbounds

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
import androidx.compose.material.icons.rounded.Dns
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import cn.moncn.sing_box_windows.ui.components.EmptyState
import cn.moncn.sing_box_windows.ui.components.MetricRow
import cn.moncn.sing_box_windows.ui.components.MetricTile
import cn.moncn.sing_box_windows.ui.components.SectionCardWithHeader
import cn.moncn.sing_box_windows.ui.components.StatusChip
import cn.moncn.sing_box_windows.ui.components.StatusTone
import cn.moncn.sing_box_windows.ui.theme.AppSpace
import cn.moncn.sing_box_windows.ui.theme.LocalIndigoTokens
import cn.moncn.sing_box_windows.v2.domain.core.CoreGateway
import cn.moncn.sing_box_windows.v2.domain.core.OutboundGroupItem

@Composable
fun OutboundsRouteV2(
    coreGateway: CoreGateway,
    modifier: Modifier = Modifier
) {
    val viewModel: OutboundsViewModel = viewModel(factory = OutboundsViewModel.factory(coreGateway))
    val state by viewModel.state.collectAsState()

    OutboundsScreenV2(
        state = state,
        onTest = { viewModel.submitIntent(OutboundsIntent.Test(it)) },
        onTestAll = { viewModel.submitIntent(OutboundsIntent.TestAll) },
        modifier = modifier
    )
}

@Composable
private fun OutboundsScreenV2(
    state: OutboundsUiState,
    onTest: (String) -> Unit,
    onTestAll: () -> Unit,
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
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(AppSpace.xs)) {
                Text(
                    text = "出站",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = tokens.textPrimary
                )
                Text(
                    text = "内核已加载的出站列表",
                    style = MaterialTheme.typography.bodyMedium,
                    color = tokens.textTertiary
                )
            }
            TextButton(onClick = onTestAll, enabled = state.outbounds.isNotEmpty()) {
                Text("全部测速")
            }
        }

        MetricRow {
            MetricTile(
                label = "出站总数",
                value = state.outbounds.size.toString(),
                modifier = Modifier.weight(1f)
            )
            MetricTile(
                label = "已测速",
                value = state.testedCount.toString(),
                accent = tokens.success,
                modifier = Modifier.weight(1f)
            )
        }

        if (state.outbounds.isEmpty()) {
            EmptyState(
                icon = Icons.Rounded.Dns,
                title = "暂无出站",
                description = "内核未报告出站信息，请先连接",
                modifier = Modifier.weight(1f)
            )
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(AppSpace.sm)
            ) {
                items(state.outbounds, key = { it.tag }) { outbound ->
                    OutboundCard(
                        outbound = outbound,
                        testing = state.testing.contains(outbound.tag),
                        onTest = { onTest(outbound.tag) }
                    )
                }
            }
        }
    }
}

@Composable
private fun OutboundCard(
    outbound: OutboundGroupItem,
    testing: Boolean,
    onTest: () -> Unit
) {
    val tokens = LocalIndigoTokens.current
    SectionCardWithHeader(
        title = outbound.tag,
        subtitle = outbound.type,
        trailing = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                StatusChip(
                    text = when {
                        testing -> "测速中"
                        outbound.urlTestDelay > 0 -> "${outbound.urlTestDelay} ms"
                        outbound.tested -> "超时"
                        else -> "未测速"
                    },
                    tone = when {
                        testing -> StatusTone.INFO
                        outbound.urlTestDelay > 0 -> StatusTone.SUCCESS
                        outbound.tested -> StatusTone.WARNING
                        else -> StatusTone.NEUTRAL
                    }
                )
                IconButton(onClick = onTest, enabled = !testing) {
                    Icon(
                        imageVector = Icons.Rounded.Speed,
                        contentDescription = "测速",
                        tint = tokens.primary
                    )
                }
            }
        }
    ) {
        Text(
            text = if (outbound.tested) "最近测速时间戳 ${outbound.urlTestTime}" else "尚未测速",
            style = MaterialTheme.typography.labelSmall,
            color = tokens.textTertiary
        )
    }
}
