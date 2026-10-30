package cn.moncn.sing_box_windows.v2.feature.home

import android.app.Activity
import android.net.VpnService
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AltRoute
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.automirrored.rounded.CallSplit
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Lan
import androidx.compose.material.icons.rounded.Memory
import androidx.compose.material.icons.rounded.PowerSettingsNew
import androidx.compose.material.icons.rounded.Terminal
import androidx.compose.material.icons.rounded.Upload
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cn.moncn.sing_box_windows.ui.Format
import cn.moncn.sing_box_windows.ui.components.AppAlertDialog
import cn.moncn.sing_box_windows.ui.components.AppBackground
import cn.moncn.sing_box_windows.ui.components.AppPageArrangement
import cn.moncn.sing_box_windows.ui.components.AppSectionCard
import cn.moncn.sing_box_windows.ui.components.AppSegmentedControl
import cn.moncn.sing_box_windows.ui.components.AppTopBar
import cn.moncn.sing_box_windows.ui.components.GradientCard
import cn.moncn.sing_box_windows.ui.components.IconTileButton
import cn.moncn.sing_box_windows.ui.components.MetricRow
import cn.moncn.sing_box_windows.ui.components.MetricTile
import cn.moncn.sing_box_windows.ui.components.StatusChip
import cn.moncn.sing_box_windows.ui.components.StatusTone
import cn.moncn.sing_box_windows.ui.components.appPagePadding
import cn.moncn.sing_box_windows.ui.theme.AppPalette
import cn.moncn.sing_box_windows.ui.theme.AppRadius
import cn.moncn.sing_box_windows.ui.theme.AppSpace
import cn.moncn.sing_box_windows.ui.theme.LocalAppColors
import cn.moncn.sing_box_windows.v2.domain.core.CoreClashMode
import cn.moncn.sing_box_windows.v2.domain.runtime.VpnState
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
    val state by viewModel.state.collectAsStateWithLifecycle()

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
        AppAlertDialog(
            title = "连接提示",
            message = state.lastError,
            confirmText = "知道了",
            dismissText = null,
            onDismissRequest = { viewModel.submitIntent(HomeIntent.ClearError) },
            onConfirm = { viewModel.submitIntent(HomeIntent.ClearError) }
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
internal fun HomeScreenV2(
    state: HomeUiState,
    onConnect: () -> Unit,
    onDisconnect: () -> Unit,
    onSwitchMode: (CoreClashMode) -> Unit,
    onOpenLogs: () -> Unit,
    onOpenOutbounds: () -> Unit,
    onOpenConnections: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalAppColors.current
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

    AppBackground(glow = true) {
        LazyColumn(
            modifier = modifier.fillMaxSize(),
            contentPadding = appPagePadding(),
            verticalArrangement = AppPageArrangement
        ) {
            item(key = "header") {
                AppTopBar(
                    title = "SingBox",
                    subtitle = "连接状态与实时流量",
                    brand = true
                )
            }

            item(key = "hero") {
                ConnectHeroCard(
                    state = state,
                    statusText = statusText,
                    statusTone = statusTone,
                    onToggle = {
                        if (state.connected || state.connecting) onDisconnect() else onConnect()
                    }
                )
            }

            item(key = "speed") {
                MetricRow {
                    MetricTile(
                        label = "上传速度",
                        value = Format.speed(status?.uplink ?: 0L),
                        icon = Icons.Rounded.ArrowUpward,
                        accent = colors.success.content,
                        modifier = Modifier.weight(1f)
                    )
                    MetricTile(
                        label = "下载速度",
                        value = Format.speed(status?.downlink ?: 0L),
                        icon = Icons.Rounded.ArrowDownward,
                        accent = colors.info.content,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            item(key = "total") {
                MetricRow {
                    MetricTile(
                        label = "累计上传",
                        value = Format.bytes(status?.uplinkTotal ?: 0L),
                        icon = Icons.Rounded.Upload,
                        accent = colors.textSecondary,
                        modifier = Modifier.weight(1f)
                    )
                    MetricTile(
                        label = "累计下载",
                        value = Format.bytes(status?.downlinkTotal ?: 0L),
                        icon = Icons.Rounded.Download,
                        accent = colors.textSecondary,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            item(key = "mode") {
                ModeSection(
                    currentMode = state.currentMode,
                    modeSupported = state.modeSupported,
                    connected = state.connected,
                    onSwitchMode = onSwitchMode
                )
            }

            item(key = "core") {
                AppSectionCard(
                    title = "内核",
                    subtitle = "状态与资源占用",
                    leadingIcon = Icons.Rounded.Memory
                ) {
                    MetricRow {
                        MetricTile(
                            label = "常驻",
                            value = Format.memory(state.memory?.rssBytes ?: 0L),
                            compact = true,
                            modifier = Modifier.weight(1f)
                        )
                        MetricTile(
                            label = "Java 堆",
                            value = Format.memory(state.memory?.javaHeapUsedBytes ?: 0L),
                            compact = true,
                            modifier = Modifier.weight(1f)
                        )
                        MetricTile(
                            label = "native 堆",
                            value = Format.memory(state.memory?.nativeHeapBytes ?: 0L),
                            compact = true,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    MetricRow {
                        MetricTile(
                            label = "内核上报",
                            value = Format.memory(status?.memory ?: 0L),
                            compact = true,
                            modifier = Modifier.weight(1f)
                        )
                        MetricTile(
                            label = "协程",
                            value = (status?.goroutines ?: 0).toString(),
                            compact = true,
                            modifier = Modifier.weight(1f)
                        )
                        MetricTile(
                            label = "连接",
                            value = (status?.connectionsOut ?: 0).toString(),
                            compact = true,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            item(key = "tools") {
                AppSectionCard(
                    title = "工具",
                    subtitle = "连接详情 / 实时日志 / 出站管理",
                    leadingIcon = Icons.Rounded.Apps
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(AppSpace.md)) {
                        IconTileButton(
                            label = "连接",
                            icon = Icons.Rounded.Lan,
                            onClick = onOpenConnections,
                            modifier = Modifier.weight(1f)
                        )
                        IconTileButton(
                            label = "日志",
                            icon = Icons.Rounded.Terminal,
                            onClick = onOpenLogs,
                            modifier = Modifier.weight(1f)
                        )
                        IconTileButton(
                            label = "出站",
                            icon = Icons.AutoMirrored.Rounded.CallSplit,
                            onClick = onOpenOutbounds,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}

/**
 * 连接主控卡片。
 *
 * 整个应用最高频的操作被单独抽离为 Hero 区域：
 * 状态文字 → 主按钮 → 操作提示，三段式纵向排列让视线自然落到按钮上。
 */
@Composable
private fun ConnectHeroCard(
    state: HomeUiState,
    statusText: String,
    statusTone: StatusTone,
    onToggle: () -> Unit
) {
    val colors = LocalAppColors.current
    GradientCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            StatusChip(text = statusText, tone = statusTone, showDot = true)
            state.currentMode?.let { mode ->
                Text(
                    text = "当前模式 · ${mode.displayName}",
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.textSecondary
                )
            }
        }
        PowerButton(
            connected = state.connected,
            connecting = state.connecting,
            onClick = onToggle,
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .padding(vertical = AppSpace.xl)
        )
        Text(
            text = when {
                state.connected -> "点击断开连接"
                state.connecting -> "正在建立隧道，点击取消"
                else -> "点击开始连接"
            },
            style = MaterialTheme.typography.bodyMedium,
            color = colors.textSecondary,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )
    }
}

/**
 * 主操作按钮。
 *
 * - 未连接：描边 + 品牌色图标，低调但可发现；
 * - 已连接：实心成功色 + 呼吸光晕，明确表达「隧道在线」；
 * - 连接中：警告色 + 进度感光晕。
 */
@Composable
private fun PowerButton(
    connected: Boolean,
    connecting: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalAppColors.current

    val infiniteTransition = rememberInfiniteTransition(label = "power_pulse")
    val pulse by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "power_pulse_value"
    )

    val active = connected || connecting
    val accent = when {
        connected -> AppPalette.Green700
        connecting -> AppPalette.Amber700
        else -> colors.primary
    }
    val containerColor by animateColorAsState(
        targetValue = if (active) accent else colors.surface,
        label = "power_container"
    )
    val iconColor by animateColorAsState(
        targetValue = if (active) Color.White else colors.primary,
        label = "power_icon"
    )
    val glowAlpha = if (active) 0.10f + 0.16f * pulse else 0f

    Box(
        modifier = modifier.size(112.dp),
        contentAlignment = Alignment.Center
    ) {
        // 呼吸光晕
        Box(
            modifier = Modifier
                .size(112.dp)
                .clip(CircleShape)
                .background(accent.copy(alpha = glowAlpha))
        )
        Box(
            modifier = Modifier
                .size(88.dp)
                .clip(CircleShape)
                .background(
                    brush = if (active) {
                        Brush.verticalGradient(listOf(accent, accent.copy(alpha = 0.88f)))
                    } else {
                        Brush.verticalGradient(listOf(colors.surface, colors.surface))
                    }
                )
                .border(
                    width = if (active) 0.dp else 1.5.dp,
                    color = if (active) Color.Transparent else colors.primaryBorder,
                    shape = CircleShape
                )
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Rounded.PowerSettingsNew,
                contentDescription = if (active) "断开连接" else "建立连接",
                tint = iconColor,
                modifier = Modifier.size(34.dp)
            )
        }
    }
}

/**
 * 运行模式切换。
 */
@Composable
private fun ModeSection(
    currentMode: CoreClashMode?,
    modeSupported: Boolean,
    connected: Boolean,
    onSwitchMode: (CoreClashMode) -> Unit
) {
    val colors = LocalAppColors.current
    val modes = listOf(CoreClashMode.RULE, CoreClashMode.GLOBAL, CoreClashMode.DIRECT)
    AppSectionCard(
        title = "运行模式",
        subtitle = when {
            !connected -> "未连接状态可预选，连接后自动生效"
            !modeSupported -> "当前内核未返回模式能力，已保存偏好用于后续连接"
            else -> "已连接，可实时切换"
        }
    ) {
        AppSegmentedControl(
            options = modes,
            selected = currentMode ?: CoreClashMode.RULE,
            onSelect = onSwitchMode,
            label = { it.displayName }
        )
        Text(
            text = "规则模式按分流规则决定走代理还是直连；全局模式全部走代理；直连模式全部直连。",
            style = MaterialTheme.typography.bodySmall,
            color = colors.textTertiary,
            modifier = Modifier.padding(top = AppSpace.sm)
        )
    }
}
