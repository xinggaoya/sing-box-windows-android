package cn.moncn.sing_box_windows.v2.feature.outbounds

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Dns
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import cn.moncn.sing_box_windows.ui.Format
import cn.moncn.sing_box_windows.ui.components.AppAlertDialog
import cn.moncn.sing_box_windows.ui.components.AppBackground
import cn.moncn.sing_box_windows.ui.components.AppPageArrangement
import cn.moncn.sing_box_windows.ui.components.AppSectionCard
import cn.moncn.sing_box_windows.ui.components.AppTopBar
import cn.moncn.sing_box_windows.ui.components.EmptyState
import cn.moncn.sing_box_windows.ui.components.IconActionButton
import cn.moncn.sing_box_windows.ui.components.MetricRow
import cn.moncn.sing_box_windows.ui.components.MetricTile
import cn.moncn.sing_box_windows.ui.components.SecondaryButton
import cn.moncn.sing_box_windows.ui.components.StatusChip
import cn.moncn.sing_box_windows.ui.components.StatusTone
import cn.moncn.sing_box_windows.ui.components.appPagePadding
import cn.moncn.sing_box_windows.ui.theme.AppSpace
import cn.moncn.sing_box_windows.ui.theme.LocalAppColors
import cn.moncn.sing_box_windows.v2.domain.core.CoreGateway
import cn.moncn.sing_box_windows.v2.domain.core.OutboundGroupItem

@Composable
fun OutboundsRouteV2(
    coreGateway: CoreGateway,
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null
) {
    val viewModel: OutboundsViewModel = viewModel(factory = OutboundsViewModel.factory(coreGateway))
    val state by viewModel.state.collectAsStateWithLifecycle()

    if (!state.error.isNullOrBlank()) {
        AppAlertDialog(
            title = "出站操作失败",
            message = state.error,
            confirmText = "知道了",
            dismissText = null,
            onDismissRequest = { viewModel.submitIntent(OutboundsIntent.ClearError) },
            onConfirm = { viewModel.submitIntent(OutboundsIntent.ClearError) }
        )
    }

    OutboundsScreenV2(
        state = state,
        onTest = { viewModel.submitIntent(OutboundsIntent.Test(it)) },
        onTestAll = { viewModel.submitIntent(OutboundsIntent.TestAll) },
        onBack = onBack,
        modifier = modifier
    )
}

@Composable
internal fun OutboundsScreenV2(
    state: OutboundsUiState,
    onTest: (String) -> Unit,
    onTestAll: () -> Unit,
    onBack: (() -> Unit)?,
    modifier: Modifier = Modifier
) {
    val colors = LocalAppColors.current

    AppBackground(glow = true) {
        LazyColumn(
            modifier = modifier.fillMaxSize(),
            contentPadding = appPagePadding(),
            verticalArrangement = AppPageArrangement
        ) {
            item(key = "header") {
                AppTopBar(
                    title = "出站",
                    subtitle = "内核已加载的出站列表",
                    onBack = onBack,
                    actions = {
                        SecondaryButton(
                            text = "全部测速",
                            onClick = onTestAll,
                            enabled = state.outbounds.isNotEmpty()
                        )
                    }
                )
            }

            item(key = "metrics") {
                MetricRow {
                    MetricTile(
                        label = "出站总数",
                        value = state.outbounds.size.toString(),
                        icon = Icons.Rounded.Dns,
                        modifier = Modifier.weight(1f)
                    )
                    MetricTile(
                        label = "已测速",
                        value = state.testedCount.toString(),
                        icon = Icons.Rounded.Speed,
                        accent = colors.success.content,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            if (state.outbounds.isEmpty()) {
                item(key = "empty") {
                    EmptyState(
                        icon = Icons.Rounded.Dns,
                        title = "暂无出站",
                        description = "内核未报告出站信息，请先连接",
                        modifier = Modifier.fillParentMaxHeight(0.7f)
                    )
                }
            } else {
                items(
                    count = state.outbounds.size,
                    key = { index -> state.outbounds[index].tag }
                ) { index ->
                    val outbound = state.outbounds[index]
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
    val colors = LocalAppColors.current
    AppSectionCard(
        title = outbound.tag,
        // tag 与 type 相同时（如 direct / block）不再重复展示副标题
        subtitle = outbound.type.takeIf { it != outbound.tag },
        trailing = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(AppSpace.xs)
            ) {
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
                IconActionButton(
                    icon = Icons.Rounded.Speed,
                    contentDescription = "测速",
                    onClick = onTest,
                    tint = colors.primary,
                    enabled = !testing
                )
            }
        }
    ) {
        Text(
            text = if (outbound.tested) {
                "最近测速 ${Format.timestamp(outbound.urlTestTime)}"
            } else {
                "尚未测速"
            },
            style = MaterialTheme.typography.labelSmall,
            color = colors.textTertiary
        )
    }
}
