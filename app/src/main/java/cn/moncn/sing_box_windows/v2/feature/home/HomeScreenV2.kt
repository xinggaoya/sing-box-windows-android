package cn.moncn.sing_box_windows.v2.feature.home

import android.app.Activity
import android.net.VpnService
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.PowerSettingsNew
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import cn.moncn.sing_box_windows.core.ClashModeManager
import cn.moncn.sing_box_windows.vpn.VpnState
import kotlinx.coroutines.flow.collect
import kotlin.math.ln
import kotlin.math.pow

@Composable
fun HomeRouteV2(
    viewModel: HomeViewModel,
    modifier: Modifier = Modifier
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
        modifier = modifier
    )
}

@Composable
private fun HomeScreenV2(
    state: HomeUiState,
    onConnect: () -> Unit,
    onDisconnect: () -> Unit,
    onSwitchMode: (ClashModeManager.ClashMode) -> Unit,
    modifier: Modifier = Modifier
) {
    val scheme = MaterialTheme.colorScheme
    val statusColor = when (state.vpnState) {
        VpnState.CONNECTED -> Color(0xFF2E7D32)
        VpnState.CONNECTING -> Color(0xFFEF6C00)
        VpnState.ERROR -> Color(0xFFC62828)
        VpnState.IDLE -> Color(0xFF546E7A)
    }
    val statusText = when (state.vpnState) {
        VpnState.CONNECTED -> "已连接"
        VpnState.CONNECTING -> "连接中"
        VpnState.ERROR -> "连接异常"
        VpnState.IDLE -> "未连接"
    }

    Scaffold(
        containerColor = Color.Transparent,
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Color(0xFFF3F7FF),
                            Color(0xFFE6EFFA),
                            Color(0xFFF8FBFF)
                        )
                    )
                )
                .padding(innerPadding)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp, vertical = 24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "SingBox",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = scheme.onBackground
                )
                Text(
                    text = "连接状态与实时流量",
                    style = MaterialTheme.typography.bodyMedium,
                    color = scheme.onSurfaceVariant
                )

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = scheme.surface.copy(alpha = 0.92f)
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(18.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "VPN 状态",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = scheme.onSurfaceVariant
                                )
                                Text(
                                    text = statusText,
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            Surface(
                                color = statusColor.copy(alpha = 0.16f),
                                shape = CircleShape
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(18.dp)
                                        .padding(3.dp)
                                        .clip(CircleShape)
                                        .background(statusColor)
                                )
                            }
                        }

                        FilledIconButton(
                            onClick = {
                                if (state.connected || state.connecting) {
                                    onDisconnect()
                                } else {
                                    onConnect()
                                }
                            },
                            modifier = Modifier
                                .size(88.dp)
                                .align(Alignment.CenterHorizontally),
                            shape = CircleShape
                        ) {
                            androidx.compose.material3.Icon(
                                imageVector = Icons.Rounded.PowerSettingsNew,
                                contentDescription = "连接按钮",
                                tint = Color.White
                            )
                        }

                        Text(
                            text = if (state.connected || state.connecting) "点击断开连接" else "点击开始连接",
                            style = MaterialTheme.typography.bodyMedium,
                            color = scheme.onSurfaceVariant,
                            modifier = Modifier.align(Alignment.CenterHorizontally)
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    MetricCard(
                        title = "上传速度",
                        value = formatSpeed(state.coreStatus?.uplinkBytes ?: 0L),
                        modifier = Modifier.weight(1f)
                    )
                    MetricCard(
                        title = "下载速度",
                        value = formatSpeed(state.coreStatus?.downlinkBytes ?: 0L),
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    MetricCard(
                        title = "累计上传",
                        value = formatBytes(state.coreStatus?.uplinkTotalBytes ?: 0L),
                        modifier = Modifier.weight(1f)
                    )
                    MetricCard(
                        title = "累计下载",
                        value = formatBytes(state.coreStatus?.downlinkTotalBytes ?: 0L),
                        modifier = Modifier.weight(1f)
                    )
                }

                ModeSection(
                    currentMode = state.currentMode,
                    modeSupported = state.modeSupported,
                    connected = state.connected,
                    onSwitchMode = onSwitchMode
                )
            }
        }
    }
}

@Composable
private fun ModeSection(
    currentMode: ClashModeManager.ClashMode?,
    modeSupported: Boolean,
    connected: Boolean,
    onSwitchMode: (ClashModeManager.ClashMode) -> Unit
) {
    val modes = listOf(
        ClashModeManager.ClashMode.Rule,
        ClashModeManager.ClashMode.Global,
        ClashModeManager.ClashMode.Direct
    )
    val scheme = MaterialTheme.colorScheme
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = scheme.surface.copy(alpha = 0.9f)
        )
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "运行模式",
                style = MaterialTheme.typography.labelMedium,
                color = scheme.onSurfaceVariant
            )
            Text(
                text = when {
                    !connected -> "未连接状态可预选，连接后自动生效"
                    !modeSupported -> "当前核心未返回模式能力，已保存偏好用于后续连接"
                    else -> "已连接，可实时切换"
                },
                style = MaterialTheme.typography.bodySmall,
                color = scheme.onSurfaceVariant
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                modes.forEach { mode ->
                    AssistChip(
                        onClick = { onSwitchMode(mode) },
                        label = { Text(mode.displayName) },
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = if (currentMode == mode) {
                                scheme.primaryContainer
                            } else {
                                scheme.surface
                            }
                        )
                    )
                }
            }
        }
    }
}

@Composable
private fun MetricCard(
    title: String,
    value: String,
    modifier: Modifier = Modifier
) {
    val scheme = MaterialTheme.colorScheme
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = scheme.surface.copy(alpha = 0.9f)
        )
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                color = scheme.onSurfaceVariant
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

private fun formatSpeed(bytes: Long): String {
    return "${formatBytes(bytes)}/s"
}

private fun formatBytes(bytes: Long): String {
    if (bytes <= 0) return "0 B"
    val units = arrayOf("B", "KB", "MB", "GB", "TB")
    val digitGroup = (ln(bytes.toDouble()) / ln(1024.0)).toInt().coerceAtMost(units.lastIndex)
    val value = bytes / 1024.0.pow(digitGroup.toDouble())
    return String.format("%.2f %s", value, units[digitGroup])
}
