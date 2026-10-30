package cn.moncn.sing_box_windows.v2.feature.logs

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.Terminal
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.withContext
import androidx.lifecycle.viewmodel.compose.viewModel
import cn.moncn.sing_box_windows.ui.components.AppAlertDialog
import cn.moncn.sing_box_windows.ui.components.AppPageColumn
import cn.moncn.sing_box_windows.ui.components.AppSegmentedControl
import cn.moncn.sing_box_windows.ui.components.AppTopBar
import cn.moncn.sing_box_windows.ui.components.EmptyState
import cn.moncn.sing_box_windows.ui.components.IconActionButton
import cn.moncn.sing_box_windows.ui.components.SwitchRow
import cn.moncn.sing_box_windows.ui.theme.AppRadius
import cn.moncn.sing_box_windows.ui.theme.AppSpace
import cn.moncn.sing_box_windows.ui.theme.LocalAppColors
import cn.moncn.sing_box_windows.ui.theme.MonoTextStyle
import cn.moncn.sing_box_windows.v2.domain.core.CoreGateway
import cn.moncn.sing_box_windows.v2.domain.core.CoreLogEntry

@Composable
fun LogsRouteV2(
    coreGateway: CoreGateway,
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null
) {
    val viewModel: LogsViewModel = viewModel(factory = LogsViewModel.factory(coreGateway))
    val state by viewModel.state.collectAsStateWithLifecycle()

    // 日志同样是高频流：仅在页面可见时订阅
    LaunchedEffect(coreGateway) {
        coreGateway.observeLogs(true)
        try {
            awaitCancellation()
        } finally {
            // 协程被取消后无法再执行挂起调用，需切到 NonCancellable 完成退订
            withContext(NonCancellable) { coreGateway.observeLogs(false) }
        }
    }

    if (!state.error.isNullOrBlank()) {
        AppAlertDialog(
            title = "日志操作失败",
            message = state.error,
            confirmText = "知道了",
            dismissText = null,
            onDismissRequest = { viewModel.submitIntent(LogsIntent.ClearError) },
            onConfirm = { viewModel.submitIntent(LogsIntent.ClearError) }
        )
    }

    LogsScreenV2(
        state = state,
        onSetFilter = { viewModel.submitIntent(LogsIntent.SetFilter(it)) },
        onSetAutoScroll = { viewModel.submitIntent(LogsIntent.SetAutoScroll(it)) },
        onClear = { viewModel.submitIntent(LogsIntent.Clear) },
        onBack = onBack,
        modifier = modifier
    )
}

@Composable
internal fun LogsScreenV2(
    state: LogsUiState,
    onSetFilter: (LogLevelFilter) -> Unit,
    onSetAutoScroll: (Boolean) -> Unit,
    onClear: () -> Unit,
    onBack: (() -> Unit)?,
    modifier: Modifier = Modifier
) {
    val colors = LocalAppColors.current
    val listState = rememberLazyListState()

    LaunchedEffect(state.visible.size, state.autoScroll) {
        if (state.autoScroll && state.visible.isNotEmpty()) {
            listState.scrollToItem(state.visible.lastIndex)
        }
    }

    AppPageColumn(modifier = modifier, glow = false) {
        AppTopBar(
            title = "日志",
            subtitle = "内核实时输出（${state.visible.size} 条）",
            onBack = onBack,
            actions = {
                IconActionButton(
                    icon = Icons.Rounded.DeleteSweep,
                    contentDescription = "清空日志",
                    onClick = onClear,
                    tint = colors.textSecondary,
                    enabled = state.logs.isNotEmpty()
                )
            }
        )

        AppSegmentedControl(
            options = LogLevelFilter.entries.toList(),
            selected = state.filter,
            onSelect = onSetFilter,
            label = { it.label }
        )

        if (state.visible.isEmpty()) {
            Box(modifier = Modifier.weight(1f)) {
                EmptyState(
                    icon = Icons.Rounded.Terminal,
                    title = "暂无日志",
                    description = "内核未输出日志内容"
                )
            }
        } else {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .background(colors.surfaceSunken, RoundedCornerShape(AppRadius.lg))
                    .border(1.dp, colors.border, RoundedCornerShape(AppRadius.lg))
                    .padding(AppSpace.md)
            ) {
                LazyColumn(state = listState) {
                    items(
                        count = state.visible.size,
                        key = { index -> "${state.visible[index].level}-${state.visible[index].message.hashCode()}-$index" }
                    ) { index ->
                        LogRow(entry = state.visible[index])
                    }
                }
            }

            SwitchRow(
                title = "自动滚动",
                subtitle = "新日志到达时自动滚动到底部",
                checked = state.autoScroll,
                onCheckedChange = onSetAutoScroll
            )
        }
    }
}

@Composable
private fun LogRow(entry: CoreLogEntry) {
    val colors = LocalAppColors.current
    val levelColor = when {
        entry.level >= CoreLogEntry.LEVEL_ERROR -> colors.error.content
        entry.level == CoreLogEntry.LEVEL_WARN -> colors.warning.content
        entry.level == CoreLogEntry.LEVEL_DEBUG -> colors.textTertiary
        else -> colors.info.content
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(AppSpace.sm)
    ) {
        Text(
            text = CoreLogEntry.levelName(entry.level),
            style = MonoTextStyle,
            fontWeight = FontWeight.Bold,
            color = levelColor,
            modifier = Modifier.width(52.dp)
        )
        Text(
            text = entry.message,
            style = MonoTextStyle,
            color = colors.textPrimary,
            modifier = Modifier.weight(1f)
        )
    }
}
