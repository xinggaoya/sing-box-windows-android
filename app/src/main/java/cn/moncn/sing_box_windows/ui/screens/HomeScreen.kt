package cn.moncn.sing_box_windows.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.PowerSettingsNew
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import cn.moncn.sing_box_windows.core.CoreStatus
import cn.moncn.sing_box_windows.ui.components.AppCard
import cn.moncn.sing_box_windows.ui.components.AppSection
import cn.moncn.sing_box_windows.ui.components.MetricTile
import cn.moncn.sing_box_windows.ui.components.StatusBadge
import cn.moncn.sing_box_windows.vpn.VpnState

@Composable
fun HomeScreen(
    state: VpnState,
    statusText: String,
    statusColor: Color,
    actionText: String,
    coreStatus: CoreStatus?,
    coreVersion: String?,
    onConnect: () -> Unit,
    onDisconnect: () -> Unit
) {
    val scrollState = rememberScrollState()
    val trafficAvailable = coreStatus?.trafficAvailable == true
    val scheme = MaterialTheme.colorScheme
    val glowAlpha by animateFloatAsState(
        targetValue = if (state == VpnState.CONNECTED) 0.55f else 0.35f,
        label = "glowAlpha"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Spacer(modifier = Modifier.height(8.dp))

        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = "连接中心",
                style = MaterialTheme.typography.titleLarge
            )
            Text(
                text = "实时状态与流量概览",
                style = MaterialTheme.typography.bodySmall,
                color = scheme.onSurfaceVariant
            )
        }

        ConnectionCard(
            state = state,
            statusText = statusText,
            statusColor = statusColor,
            actionText = actionText,
            glowAlpha = glowAlpha,
            onConnect = onConnect,
            onDisconnect = onDisconnect
        )

        AppSection(title = { Text("流量统计", style = MaterialTheme.typography.titleMedium) }) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                MetricTile(
                    label = "上传速度",
                    value = if (trafficAvailable) formatSpeed(coreStatus?.uplinkBytes ?: 0) else "---",
                    modifier = Modifier.weight(1f),
                    highlight = true
                )
                MetricTile(
                    label = "下载速度",
                    value = if (trafficAvailable) formatSpeed(coreStatus?.downlinkBytes ?: 0) else "---",
                    modifier = Modifier.weight(1f),
                    highlight = true
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                MetricTile(
                    label = "总上传",
                    value = if (trafficAvailable) formatBytes(coreStatus?.uplinkTotalBytes ?: 0) else "---",
                    modifier = Modifier.weight(1f)
                )
                MetricTile(
                    label = "总下载",
                    value = if (trafficAvailable) formatBytes(coreStatus?.downlinkTotalBytes ?: 0) else "---",
                    modifier = Modifier.weight(1f)
                )
            }
        }

        AppSection(title = { Text("系统信息", style = MaterialTheme.typography.titleMedium) }) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                MetricTile(
                    label = "内核版本",
                    value = coreVersion ?: "未知",
                    modifier = Modifier.weight(1f)
                )
                MetricTile(
                    label = "内存占用",
                    value = formatMemory(coreStatus?.memoryBytes ?: 0),
                    modifier = Modifier.weight(1f)
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun ConnectionCard(
    state: VpnState,
    statusText: String,
    statusColor: Color,
    actionText: String,
    glowAlpha: Float,
    onConnect: () -> Unit,
    onDisconnect: () -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    val badgeText = when (state) {
        VpnState.CONNECTED -> "VPN 已连接"
        VpnState.CONNECTING -> "连接建立中"
        VpnState.ERROR -> "连接异常"
        VpnState.IDLE -> "VPN 未连接"
    }
    val contentColor = when (state) {
        VpnState.CONNECTED -> scheme.onSecondary
        VpnState.CONNECTING -> scheme.onTertiary
        VpnState.ERROR -> scheme.onError
        VpnState.IDLE -> scheme.onPrimary
    }

    AppCard(
        modifier = Modifier.fillMaxWidth(),
        containerColor = scheme.surface
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "连接状态",
                        style = MaterialTheme.typography.labelMedium,
                        color = scheme.onSurfaceVariant
                    )
                    Text(
                        text = statusText,
                        style = MaterialTheme.typography.titleLarge
                    )
                }
                StatusBadge(text = badgeText, color = statusColor)
            }

            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                val glowBrush = Brush.radialGradient(
                    colors = listOf(statusColor.copy(alpha = glowAlpha), Color.Transparent)
                )
                Box(
                    modifier = Modifier
                        .size(200.dp)
                        .clip(CircleShape)
                        .background(glowBrush)
                )
                Button(
                    onClick = {
                        if (state == VpnState.CONNECTED || state == VpnState.CONNECTING) {
                            onDisconnect()
                        } else {
                            onConnect()
                        }
                    },
                    modifier = Modifier
                        .size(120.dp)
                        .shadow(12.dp, CircleShape),
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = statusColor,
                        contentColor = contentColor
                    )
                ) {
                    Icon(
                        imageVector = Icons.Rounded.PowerSettingsNew,
                        contentDescription = actionText,
                        modifier = Modifier.size(44.dp)
                    )
                }
            }

            Text(
                text = actionText,
                style = MaterialTheme.typography.labelLarge,
                color = scheme.onSurfaceVariant
            )
        }
    }
}

fun formatSpeed(bytes: Long): String {
    if (bytes < 1024) return "$bytes B/s"
    if (bytes < 1024 * 1024) return String.format("%.1f KB/s", bytes / 1024f)
    return String.format("%.1f MB/s", bytes / (1024f * 1024f))
}

fun formatBytes(bytes: Long): String {
    if (bytes < 1024) return "$bytes B"
    if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024f)
    if (bytes < 1024 * 1024 * 1024) return String.format("%.1f MB", bytes / (1024f * 1024f))
    return String.format("%.1f GB", bytes / (1024f * 1024f * 1024f))
}

fun formatMemory(bytes: Long): String {
    if (bytes < 1024) return "$bytes B"
    if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024f)
    return String.format("%.1f MB", bytes / (1024f * 1024f))
}
