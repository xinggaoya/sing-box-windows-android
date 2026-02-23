package cn.moncn.sing_box_windows.v2.feature.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.text.KeyboardOptions
import androidx.lifecycle.viewmodel.compose.viewModel
import cn.moncn.sing_box_windows.update.UpdateState
import cn.moncn.sing_box_windows.v2.domain.settings.SettingsGateway

private val DNS_STRATEGIES = listOf("prefer_ipv4", "prefer_ipv6", "ipv4_only", "ipv6_only")
private val DNS_LABELS = mapOf(
    "prefer_ipv4" to "优先 IPv4",
    "prefer_ipv6" to "优先 IPv6",
    "ipv4_only" to "仅 IPv4",
    "ipv6_only" to "仅 IPv6"
)

@Composable
fun SettingsRouteV2(
    gateway: SettingsGateway
) {
    val viewModel: SettingsViewModel = viewModel(factory = SettingsViewModel.factory(gateway))
    val state by viewModel.state.collectAsState()

    if (!state.error.isNullOrBlank()) {
        AlertDialog(
            onDismissRequest = { viewModel.submitIntent(SettingsIntent.ClearError) },
            title = { Text("设置失败") },
            text = { Text(state.error.orEmpty()) },
            confirmButton = {
                Button(onClick = { viewModel.submitIntent(SettingsIntent.ClearError) }) {
                    Text("知道了")
                }
            }
        )
    }

    if (!state.message.isNullOrBlank()) {
        AlertDialog(
            onDismissRequest = { viewModel.submitIntent(SettingsIntent.ClearMessage) },
            title = { Text("设置结果") },
            text = { Text(state.message.orEmpty()) },
            confirmButton = {
                Button(onClick = { viewModel.submitIntent(SettingsIntent.ClearMessage) }) {
                    Text("确定")
                }
            }
        )
    }

    SettingsScreenV2(
        state = state,
        onIntent = viewModel::submitIntent
    )
}

@Composable
private fun SettingsScreenV2(
    state: SettingsUiState,
    onIntent: (SettingsIntent) -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    val updateState = state.updateState
    val isCheckingUpdate = updateState is UpdateState.Checking
    val isDownloadingUpdate = updateState is UpdateState.Downloading
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.linearGradient(
                    listOf(Color(0xFFF6FAFF), Color(0xFFEBF2FA), Color(0xFFF8FCFF))
                )
            )
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "设置中心",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "网络参数与更新管理",
                        style = MaterialTheme.typography.bodyMedium,
                        color = scheme.onSurfaceVariant
                    )
                }
            }

            item {
                Card(
                    shape = RoundedCard,
                    colors = CardDefaults.cardColors(containerColor = scheme.surface.copy(alpha = 0.94f))
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        DnsSelector(
                            selected = state.dnsStrategy,
                            onSelected = { onIntent(SettingsIntent.ChangeDnsStrategy(it)) }
                        )
                        SettingsSwitchRow(
                            title = "启用 DNS 缓存",
                            checked = state.dnsCacheEnabled,
                            onCheckedChange = { onIntent(SettingsIntent.ChangeDnsCacheEnabled(it)) }
                        )
                        SettingsSwitchRow(
                            title = "独立 DNS 缓存",
                            checked = state.dnsIndependentCache,
                            onCheckedChange = { onIntent(SettingsIntent.ChangeDnsIndependentCache(it)) }
                        )
                    }
                }
            }

            item {
                Card(
                    shape = RoundedCard,
                    colors = CardDefaults.cardColors(containerColor = scheme.surface.copy(alpha = 0.94f))
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedTextField(
                            value = state.tunMtuInput,
                            onValueChange = { onIntent(SettingsIntent.ChangeTunMtuInput(it)) },
                            label = { Text("TUN MTU") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth()
                        )
                        SettingsSwitchRow(
                            title = "自动路由",
                            checked = state.tunAutoRoute,
                            onCheckedChange = { onIntent(SettingsIntent.ChangeTunAutoRoute(it)) }
                        )
                        SettingsSwitchRow(
                            title = "严格路由",
                            checked = state.tunStrictRoute,
                            onCheckedChange = { onIntent(SettingsIntent.ChangeTunStrictRoute(it)) }
                        )
                        SettingsSwitchRow(
                            title = "HTTP 代理",
                            checked = state.httpProxyEnabled,
                            onCheckedChange = { onIntent(SettingsIntent.ChangeHttpProxyEnabled(it)) }
                        )
                    }
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = { onIntent(SettingsIntent.Save) },
                        modifier = Modifier.weight(1f),
                        enabled = !state.isSaving && !state.isLoading
                    ) {
                        Text(if (state.isSaving) "保存中..." else "保存并应用")
                    }
                    Button(
                        onClick = { onIntent(SettingsIntent.CheckUpdate) },
                        modifier = Modifier.weight(1f),
                        enabled = !state.isSaving && !isCheckingUpdate && !isDownloadingUpdate
                    ) {
                        Text(
                            when {
                                isCheckingUpdate -> "检查中..."
                                isDownloadingUpdate -> "下载中..."
                                else -> "检查更新"
                            }
                        )
                    }
                }
            }

            item {
                UpdateCard(
                    updateState = state.updateState,
                    onIntent = onIntent
                )
            }

            item { Spacer(modifier = Modifier.height(20.dp)) }
        }
    }
}

@Composable
private fun UpdateCard(
    updateState: UpdateState,
    onIntent: (SettingsIntent) -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    Card(
        shape = RoundedCard,
        colors = CardDefaults.cardColors(containerColor = scheme.surface.copy(alpha = 0.94f))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text("应用更新", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            when (updateState) {
                UpdateState.Idle -> {
                    Text("点击“检查更新”以获取最新版本。", color = scheme.onSurfaceVariant)
                }

                UpdateState.Checking -> {
                    Text("正在检查新版本...", color = scheme.onSurfaceVariant)
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                }

                UpdateState.UpToDate -> {
                    Text("当前已是最新版本。", color = scheme.primary)
                }

                is UpdateState.UpdateAvailable -> {
                    val release = updateState.releaseInfo
                    Text("发现新版本：${release.tagName}", fontWeight = FontWeight.Medium)
                    if (release.name.isNotBlank()) {
                        Text(release.name, color = scheme.onSurfaceVariant)
                    }
                    Text(
                        "发布时间：${release.publishedAt}",
                        style = MaterialTheme.typography.bodySmall,
                        color = scheme.onSurfaceVariant
                    )
                    Button(onClick = { onIntent(SettingsIntent.DownloadUpdate) }) {
                        Text("下载更新包")
                    }
                }

                is UpdateState.Downloading -> {
                    val progress = updateState.progress
                    Text("正在下载：${updateState.releaseInfo.tagName}", fontWeight = FontWeight.Medium)
                    LinearProgressIndicator(
                        progress = { (progress.percentage / 100f).coerceIn(0f, 1f) },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text(
                        "${progress.getPercentageString()}  ${progress.getDownloadedSizeReadable()} / ${progress.getTotalSizeReadable()}",
                        style = MaterialTheme.typography.bodySmall,
                        color = scheme.onSurfaceVariant
                    )
                }

                is UpdateState.ReadyToInstall -> {
                    Text("下载完成：${updateState.releaseInfo.tagName}", fontWeight = FontWeight.Medium)
                    Text(
                        "下载包已就绪。可点击系统通知安装；若通知不可见，可用下方按钮安装。",
                        style = MaterialTheme.typography.bodySmall,
                        color = scheme.onSurfaceVariant
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = { onIntent(SettingsIntent.InstallDownloadedUpdate) }) {
                            Text("安装已下载包")
                        }
                        TextButton(onClick = { onIntent(SettingsIntent.CheckUpdate) }) {
                            Text("重新检查")
                        }
                    }
                }

                is UpdateState.Failed -> {
                    Text("更新失败：${updateState.error}", color = scheme.error)
                }
            }
        }
    }
}

@Composable
private fun DnsSelector(
    selected: String,
    onSelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text("DNS 策略", style = MaterialTheme.typography.labelLarge)
        TextButton(onClick = { expanded = true }) {
            Text(DNS_LABELS[selected] ?: selected)
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DNS_STRATEGIES.forEach { item ->
                DropdownMenuItem(
                    text = { Text(DNS_LABELS[item] ?: item) },
                    onClick = {
                        onSelected(item)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
private fun SettingsSwitchRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, style = MaterialTheme.typography.bodyLarge)
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

private val RoundedCard = androidx.compose.foundation.shape.RoundedCornerShape(20.dp)
