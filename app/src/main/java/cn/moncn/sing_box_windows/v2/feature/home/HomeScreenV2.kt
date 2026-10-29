package cn.moncn.sing_box_windows.v2.feature.home

import android.app.Activity
import android.net.VpnService
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.CallSplit
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Lan
import androidx.compose.material.icons.rounded.PowerSettingsNew
import androidx.compose.material.icons.rounded.Terminal
import androidx.compose.material.icons.rounded.Upload
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import cn.moncn.sing_box_windows.ui.Format
import cn.moncn.sing_box_windows.ui.components.MetricRow
import cn.moncn.sing_box_windows.ui.components.MetricTile
import cn.moncn.sing_box_windows.ui.components.SectionCardWithHeader
import cn.moncn.sing_box_windows.ui.components.StatusChip
import cn.moncn.sing_box_windows.ui.components.StatusTone
import cn.moncn.sing_box_windows.ui.theme.AppRadius
import cn.moncn.sing_box_windows.ui.theme.AppSpace
import cn.moncn.sing_box_windows.ui.theme.LocalIndigoTokens
import cn.moncn.sing_box_windows.v2.domain.core.CoreClashMode
import cn.moncn.sing_box_windows.vpn.VpnState
import kotlinx.coroutines.flow.collect

@Composable
fun HomeRouteV2(
    viewModel: HomeViewModel,
    modifier: Modifier = Modifier,
    onOpenLogs: () -> Unit = {},
    onOpenOutbounds: () -> Unit = {},
    onOpenConnections: () -> Unit = {}
) {
    val context = LocalContext.current
    val state by viewModel.state.collectAsState()

    val vpnPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        viewModel.submitIntent(
            HomeIntent.VpnPermissionResult(
                granted = result.resultCode == Activity.RESULT_OK
            )
        )
    }

    LaunchedEffect(viewModel) {
        viewModel.effect.collect { effect ->
            when (effect) {
                HomeEffect.RequestVpnPermission -> {
                    val intent = VpnService.prepare(context)
                    if (intent == null) {
                        viewModel.submitIntent(HomeIntent.VpnPermissionResult(granted = true))
                    } else {
                        vpnPermissionLauncher.launch(intent)
                    }
                }
            }
        }
    }

    if (!state.lastError.isNullOrBlank()) {
        AlertDialog(
            onDismissRequest = { viewModel.submitIntent(HomeIntent.ClearError) },
            title = { Text("连接提示") },
            text = { Text(state.lastError.orEmpty()) },
            confirmButton = {
                Button(onClick = { viewModel.submitIntent(HomeIntent.ClearError) }) {
                    Text("知道了")
                }
            }
        )
    }

    HomeScreenV2(
        state = state,
        onConnect = { viewModel.submitIntent(HomeIntent.ConnectClicked) },
        onDisconnect = { viewModel.submitIntent(HomeIntent.DisconnectClicked) },
        onSwitchMode = { mode -> viewModel.submitIntent(HomeIntent.SwitchMode(mode)) },
        onOpenLogs = onOpenLogs,
        onOpenOutbounds = onOpenOutbounds,
        onOpenConnections = onOpenConnections,
        modifier = modifier
    )
}

@Composable
private fun HomeScreenV2(
    state: HomeUiState,
    onConnect: () -> Unit,
    onDisconnect: () -> Unit,
    onSwitchMode: (CoreClashMode) -> Unit,
    onOpenLogs: () -> Unit,
    onOpenOutbounds: () -> Unit,
    onOpenConnections: () -> Unit,
    modifier: Modifier = Modifier
) {
    val tokens = LocalIndigoTokens.current
    val statusTone = when (state.vpnState) {
        VpnState.CONNECTED -> StatusTone.SUCCESS
        VpnState.CONNECTING -> StatusTone.WARNING
        VpnState.ERROR -> StatusTone.ERROR
        VpnState.IDLE -> StatusTone.NEUTRAL
    }
    val statusText = when (state.vpnState) {
        VpnState.CONNECTED -> "已连接"
        VpnState.CONNECTING -> "连接中"
        VpnState.ERROR -> "连接异常"
        VpnState.IDLE -> "未连接"
    }
    val status = state.coreStatus

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    listOf(tokens.bgBase, tokens.bgSurface2, tokens.bgBase)
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = AppSpace.xl, vertical = AppSpace.xl),
            verticalArrangement = Arrangement.spacedBy(AppSpace.lg)
        ) {
            // ---------- 页头 ----------
            Column(verticalArrangement = Arrangement.spacedBy(AppSpace.xs)) {
                Text(
                    text = "SingBox",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = tokens.textPrimary
                )
                Text(
                    text = "连接状态与实时流量",
                    style = MaterialTheme.typography.bodyMedium,
                    color = tokens.textTertiary
                )
            }

            // ---------- 连接卡片 ----------
            SectionCardWithHeader(
                title = "VPN 状态",
                subtitle = statusText,
                trailing = { StatusChip(text = statusText, tone = statusTone) }
            ) {
                FilledIconButton(
                    onClick = {
                        if (state.connected || state.connecting) onDisconnect() else onConnect()
                    },
                    modifier = Modifier
                        .size(84.dp)
                        .align(Alignment.CenterHorizontally),
                    shape = CircleShape
                ) {
                    Icon(
                        imageVector = Icons.Rounded.PowerSettingsNew,
                        contentDescription = "连接按钮",
                        tint = androidx.compose.ui.graphics.Color.White
                    )
                }
                Text(
                    text = if (state.connected || state.connecting) "点击断开连接" else "点击开始连接",
                    style = MaterialTheme.typography.bodySmall,
                    color = tokens.textTertiary,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
            }

            // ---------- 实时速率 ----------
            MetricRow {
                MetricTile(
                    label = "上传速度",
                    value = Format.speed(status?.uplink ?: 0L),
                    icon = Icons.Rounded.ArrowUpward,
                    accent = tokens.success,
                    modifier = Modifier.weight(1f)
                )
                MetricTile(
                    label = "下载速度",
                    value = Format.speed(status?.downlink ?: 0L),
                    icon = Icons.Rounded.ArrowDownward,
                    accent = tokens.info,
                    modifier = Modifier.weight(1f)
                )
            }

            // ---------- 累计流量 ----------
            MetricRow {
                MetricTile(
                    label = "累计上传",
                    value = Format.bytes(status?.uplinkTotal ?: 0L),
                    icon = Icons.Rounded.Upload,
                    accent = tokens.textTertiary,
                    modifier = Modifier.weight(1f)
                )
                MetricTile(
                    label = "累计下载",
                    value = Format.bytes(status?.downlinkTotal ?: 0L),
                    icon = Icons.Rounded.Download,
                    accent = tokens.textTertiary,
                    modifier = Modifier.weight(1f)
                )
            }

            // ---------- 运行模式 ----------
            ModeSection(
                currentMode = state.currentMode,
                modeSupported = state.modeSupported,
                connected = state.connected,
                onSwitchMode = onSwitchMode
            )

            // ---------- 内核信息 ----------
            SectionCardWithHeader(
                title = "内核",
                subtitle = "状态与内存占用"
            ) {
                MetricRow {
                    MetricTile(
                        label = "内存",
                        value = Format.memory(status?.memory ?: 0L),
                        modifier = Modifier.weight(1f)
                    )
                    MetricTile(
                        label = "协程",
                        value = (status?.goroutines ?: 0).toString(),
                        modifier = Modifier.weight(1f)
                    )
                    MetricTile(
                        label = "连接",
                        value = (status?.connectionsOut ?: 0).toString(),
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // ---------- 快捷入口 ----------
            SectionCardWithHeader(
                title = "工具",
                subtitle = "连接 / 日志 / 出站"
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(AppSpace.sm)) {
                    QuickEntry(
                        label = "连接",
                        icon = Icons.Rounded.Lan,
                        modifier = Modifier.weight(1f),
                        onClick = onOpenConnections
                    )
                    QuickEntry(
                        label = "日志",
                        icon = Icons.Rounded.Terminal,
                        modifier = Modifier.weight(1f),
                        onClick = onOpenLogs
                    )
                    QuickEntry(
                        label = "出站",
                        icon = Icons.Rounded.CallSplit,
                        modifier = Modifier.weight(1f),
                        onClick = onOpenOutbounds
                    )
                }
            }
        }
    }
}

@Composable
private fun QuickEntry(
    label: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val tokens = LocalIndigoTokens.current
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(AppRadius.md))
            .background(tokens.bgSurface2)
            .clickable(onClick = onClick)
            .padding(vertical = AppSpace.md),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(AppSpace.xs)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = tokens.primary,
            modifier = Modifier.size(22.dp)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = tokens.textSecondary
        )
    }
}

@Composable
private fun ModeSection(
    currentMode: CoreClashMode?,
    modeSupported: Boolean,
    connected: Boolean,
    onSwitchMode: (CoreClashMode) -> Unit
) {
    val tokens = LocalIndigoTokens.current
    val modes = listOf(CoreClashMode.RULE, CoreClashMode.GLOBAL, CoreClashMode.DIRECT)
    SectionCardWithHeader(
        title = "运行模式",
        subtitle = when {
            !connected -> "未连接状态可预选，连接后自动生效"
            !modeSupported -> "当前内核未返回模式能力，已保存偏好用于后续连接"
            else -> "已连接，可实时切换"
        }
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(AppSpace.sm)) {
            modes.forEach { mode ->
                val selected = currentMode == mode
                androidx.compose.material3.FilterChip(
                    selected = selected,
                    onClick = { onSwitchMode(mode) },
                    label = { Text(mode.displayName) },
                    colors = androidx.compose.material3.FilterChipDefaults.filterChipColors(
                        selectedContainerColor = tokens.primarySoft,
                        selectedLabelColor = tokens.primary
                    )
                )
            }
        }
    }
}
