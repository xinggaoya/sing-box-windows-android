package cn.moncn.sing_box_windows.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cn.moncn.sing_box_windows.config.AppSettingsDefaults
import cn.moncn.sing_box_windows.config.ClashApiDefaults
import cn.moncn.sing_box_windows.config.ConfigRepository
import cn.moncn.sing_box_windows.config.ConfigSettingsApplier
import cn.moncn.sing_box_windows.config.SettingsRepository
import cn.moncn.sing_box_windows.core.ClashApiClient
import cn.moncn.sing_box_windows.core.ClashApiDiagnostics
import cn.moncn.sing_box_windows.core.ClashApiDiagnosticsManager
import cn.moncn.sing_box_windows.core.EndpointStatus
import cn.moncn.sing_box_windows.ui.components.AppCard
import cn.moncn.sing_box_windows.ui.components.AppSection
import cn.moncn.sing_box_windows.ui.components.StatusBadge
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date

@Composable
fun DiagnosticsScreen(
    diagnostics: ClashApiDiagnostics?
) {
    val context = LocalContext.current
    var configInfo by remember { mutableStateOf(ConfigInfo()) }
    DisposableEffect(Unit) {
        ClashApiDiagnosticsManager.start()
        onDispose { ClashApiDiagnosticsManager.stop() }
    }
    LaunchedEffect(Unit) {
        // 诊断页打开时主动读取配置，确保 UI 进程已初始化 Clash API。
        val settings = withContext(Dispatchers.IO) { SettingsRepository.load(context) }
        val rawConfig = withContext(Dispatchers.IO) { ConfigRepository.loadOrCreateConfig(context) }
        val appliedConfig = ConfigSettingsApplier.applySettings(rawConfig, settings)
        configInfo = parseConfigInfo(settings, appliedConfig)
        ClashApiClient.configureFromConfig(appliedConfig)
        if (!ClashApiClient.isConfigured() && settings.clashApiEnabled) {
            val address = settings.clashApiAddress.trim().ifBlank {
                AppSettingsDefaults.CLASH_API_ADDRESS
            }
            ClashApiClient.configure(address, ClashApiDefaults.SECRET)
        }
    }
    val scheme = MaterialTheme.colorScheme
    val display = diagnostics

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "API 诊断",
                    style = MaterialTheme.typography.titleLarge
                )
                Text(
                    text = "查看接口连接状态与返回概览",
                    style = MaterialTheme.typography.bodySmall,
                    color = scheme.onSurfaceVariant
                )
            }
        }

        item {
            AppSection(title = { Text("连接信息", style = MaterialTheme.typography.titleMedium) }) {
                AppCard(modifier = Modifier.fillMaxWidth()) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        InfoRow(label = "控制地址", value = display?.baseUrl ?: "未配置")
                        InfoRow(label = "鉴权 Token", value = if (display?.hasSecret == true) "已设置" else "未设置")
                        InfoRow(label = "设置开关", value = if (configInfo.settingsEnabled) "开启" else "关闭")
                        InfoRow(label = "设置地址", value = configInfo.settingsAddress.ifBlank { "---" })
                        InfoRow(label = "配置包含 API", value = if (configInfo.configEnabled) "是" else "否")
                        InfoRow(label = "配置地址", value = configInfo.configAddress.ifBlank { "---" })
                        InfoRow(label = "配置 Secret", value = if (configInfo.configHasSecret) "已设置" else "未设置")
                        InfoRow(
                            label = "更新时间",
                            value = formatTime(display?.updatedAt)
                        )
                    }
                }
            }
        }

        item {
            AppSection(title = { Text("WebSocket", style = MaterialTheme.typography.titleMedium) }) {
                AppCard(modifier = Modifier.fillMaxWidth()) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        val traffic = display?.trafficSocket
                        SocketRow(
                            label = "流量通道",
                            connected = traffic?.connected == true,
                            lastMessageAt = traffic?.lastMessageAt,
                            error = traffic?.lastError
                        )
                        val memory = display?.memorySocket
                        SocketRow(
                            label = "内存通道",
                            connected = memory?.connected == true,
                            lastMessageAt = memory?.lastMessageAt,
                            error = memory?.lastError
                        )
                    }
                }
            }
        }

        item {
            AppSection(title = { Text("HTTP 接口", style = MaterialTheme.typography.titleMedium) }) {}
        }

        if (display == null) {
            item {
                AppCard(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "诊断数据暂不可用，请稍后重试。",
                        color = scheme.onSurfaceVariant
                    )
                }
            }
        } else {
            items(display.endpoints, key = { it.path }) { endpoint ->
                EndpointCard(endpoint = endpoint)
            }
        }

        item {
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun EndpointCard(endpoint: EndpointStatus) {
    val scheme = MaterialTheme.colorScheme
    val badgeColor = if (endpoint.ok) scheme.secondary else scheme.error
    AppCard(modifier = Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(text = endpoint.name, style = MaterialTheme.typography.titleMedium)
                    Text(
                        text = endpoint.path,
                        style = MaterialTheme.typography.labelSmall,
                        color = scheme.onSurfaceVariant
                    )
                }
                StatusBadge(
                    text = endpoint.message ?: if (endpoint.ok) "OK" else "失败",
                    color = badgeColor
                )
            }

            if (!endpoint.sample.isNullOrBlank()) {
                Text(
                    text = endpoint.sample!!,
                    style = MaterialTheme.typography.bodySmall,
                    color = scheme.onSurfaceVariant
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                InfoLabel(
                    label = "HTTP",
                    value = endpoint.code?.toString() ?: "-"
                )
                InfoLabel(
                    label = "时间",
                    value = formatTime(endpoint.updatedAt)
                )
            }
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    val scheme = MaterialTheme.colorScheme
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, style = MaterialTheme.typography.labelMedium, color = scheme.onSurfaceVariant)
        Text(text = value, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun InfoLabel(label: String, value: String) {
    val scheme = MaterialTheme.colorScheme
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = scheme.onSurfaceVariant)
        Text(text = value, style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
private fun SocketRow(
    label: String,
    connected: Boolean,
    lastMessageAt: Long?,
    error: String?
) {
    val scheme = MaterialTheme.colorScheme
    val badgeColor = if (connected) scheme.secondary else scheme.error
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = label, style = MaterialTheme.typography.bodyMedium)
            StatusBadge(text = if (connected) "已连接" else "未连接", color = badgeColor)
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "最后消息",
                style = MaterialTheme.typography.labelSmall,
                color = scheme.onSurfaceVariant
            )
            Text(
                text = formatTime(lastMessageAt),
                style = MaterialTheme.typography.labelSmall
            )
        }
        if (!error.isNullOrBlank()) {
            Text(
                text = error,
                style = MaterialTheme.typography.labelSmall,
                color = scheme.error
            )
        }
    }
}

private fun formatTime(timestamp: Long?): String {
    if (timestamp == null || timestamp <= 0L) return "---"
    val formatter = SimpleDateFormat.getTimeInstance()
    return formatter.format(Date(timestamp))
}

private data class ConfigInfo(
    val settingsEnabled: Boolean = false,
    val settingsAddress: String = "",
    val configEnabled: Boolean = false,
    val configAddress: String = "",
    val configHasSecret: Boolean = false
)

private fun parseConfigInfo(
    settings: cn.moncn.sing_box_windows.config.AppSettings,
    configJson: String
): ConfigInfo {
    val root = runCatching { JSONObject(configJson) }.getOrNull()
    val clashApi = root?.optJSONObject("experimental")?.optJSONObject("clash_api")
    val address = clashApi?.optString("external_controller")
        ?: clashApi?.optString("external-controller")
        ?: ""
    val hasSecret = !clashApi?.optString("secret").isNullOrBlank()
    return ConfigInfo(
        settingsEnabled = settings.clashApiEnabled,
        settingsAddress = settings.clashApiAddress.trim(),
        configEnabled = clashApi != null,
        configAddress = address.trim(),
        configHasSecret = hasSecret
    )
}
