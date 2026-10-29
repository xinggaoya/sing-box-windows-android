package cn.moncn.sing_box_windows.v2.feature.logs

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.Terminal
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import cn.moncn.sing_box_windows.ui.components.EmptyState
import cn.moncn.sing_box_windows.ui.theme.AppSpace
import cn.moncn.sing_box_windows.ui.theme.LocalIndigoTokens
import cn.moncn.sing_box_windows.ui.theme.MonoTextStyle
import cn.moncn.sing_box_windows.v2.domain.core.CoreGateway
import cn.moncn.sing_box_windows.v2.domain.core.CoreLogEntry

@Composable
fun LogsRouteV2(
    coreGateway: CoreGateway,
    modifier: Modifier = Modifier
) {
    val viewModel: LogsViewModel = viewModel(factory = LogsViewModel.factory(coreGateway))
    val state by viewModel.state.collectAsState()

    LogsScreenV2(
        state = state,
        onSetFilter = { viewModel.submitIntent(LogsIntent.SetFilter(it)) },
        onClear = { viewModel.submitIntent(LogsIntent.Clear) },
        modifier = modifier
    )
}

@Composable
private fun LogsScreenV2(
    state: LogsUiState,
    onSetFilter: (LogLevelFilter) -> Unit,
    onClear: () -> Unit,
    modifier: Modifier = Modifier
) {
    val tokens = LocalIndigoTokens.current
    val listState = rememberLazyListState()

    LaunchedEffect(state.visible.size, state.autoScroll) {
        if (state.autoScroll && state.visible.isNotEmpty()) {
            listState.scrollToItem(state.visible.lastIndex)
        }
    }

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
                    text = "日志",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = tokens.textPrimary
                )
                Text(
                    text = "内核实时输出（${state.visible.size} 条）",
                    style = MaterialTheme.typography.bodyMedium,
                    color = tokens.textTertiary
                )
            }
            IconButton(onClick = onClear) {
                Icon(
                    imageVector = Icons.Rounded.DeleteSweep,
                    contentDescription = "清空日志",
                    tint = tokens.textSecondary
                )
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(AppSpace.sm)) {
            LogLevelFilter.entries.forEach { filter ->
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
        }

        if (state.visible.isEmpty()) {
            EmptyState(
                icon = Icons.Rounded.Terminal,
                title = "暂无日志",
                description = "内核未输出日志内容",
                modifier = Modifier.weight(1f)
            )
        } else {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .background(tokens.bgSurface, MaterialTheme.shapes.medium)
                    .padding(AppSpace.sm)
            ) {
                LazyColumn(state = listState) {
                    items(state.visible) { entry ->
                        LogRow(entry = entry)
                    }
                }
            }
        }
    }
}

@Composable
private fun LogRow(entry: CoreLogEntry) {
    val tokens = LocalIndigoTokens.current
    val levelColor = when {
        entry.level >= CoreLogEntry.LEVEL_ERROR -> tokens.error
        entry.level == CoreLogEntry.LEVEL_WARN -> tokens.warning
        entry.level == CoreLogEntry.LEVEL_DEBUG -> tokens.textTertiary
        else -> tokens.textSecondary
    }
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(AppSpace.sm)
    ) {
        Text(
            text = CoreLogEntry.levelName(entry.level),
            style = MonoTextStyle,
            color = levelColor,
            modifier = Modifier.padding(top = 1.dp)
        )
        Text(
            text = entry.message,
            style = MonoTextStyle,
            color = tokens.textPrimary,
            modifier = Modifier.weight(1f)
        )
    }
}
