package cn.moncn.sing_box_windows.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        Spacer(modifier = Modifier.height(32.dp))

        // Large Connect Button
        Box(contentAlignment = Alignment.Center) {
            // Pulse effect or outer ring (simplified for now)
            Box(
                modifier = Modifier
                    .size(200.dp)
                    .clip(CircleShape)
                    .background(statusColor.copy(alpha = 0.1f))
            )
            Box(
                modifier = Modifier
                    .size(160.dp)
                    .clip(CircleShape)
                    .background(statusColor.copy(alpha = 0.2f))
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
                    .shadow(elevation = 12.dp, shape = CircleShape, spotColor = statusColor),
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(
                    containerColor = statusColor,
                    contentColor = MaterialTheme.colorScheme.surface
                )
            ) {
                Icon(
                    imageVector = Icons.Rounded.PowerSettingsNew,
                    contentDescription = actionText,
                    modifier = Modifier.size(48.dp)
                )
            }
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = statusText,
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(8.dp))
            StatusBadge(
                text = if (state == VpnState.CONNECTED) "VPN 已连接" else "已断开",
                color = statusColor
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        AppSection(title = { Text("流量统计", style = MaterialTheme.typography.titleMedium) }) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
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
            Spacer(modifier = Modifier.height(16.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
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
            AppCard(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("内核版本", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.outline)
                        Text(coreVersion ?: "未知", style = MaterialTheme.typography.bodyLarge)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("内存占用", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.outline)
                        Text(formatMemory(coreStatus?.memoryBytes ?: 0), style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}

// Helper functions (copied from original likely, or reimplemented)
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
