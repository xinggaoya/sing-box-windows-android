package cn.moncn.sing_box_windows.v2.feature.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Dns
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Route
import androidx.compose.material.icons.rounded.SystemUpdate
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import cn.moncn.sing_box_windows.ui.components.AppAlertDialog
import cn.moncn.sing_box_windows.ui.components.AppBackground
import cn.moncn.sing_box_windows.ui.components.AppDropdownField
import cn.moncn.sing_box_windows.ui.components.AppInlineBanner
import cn.moncn.sing_box_windows.ui.components.AppPageArrangement
import cn.moncn.sing_box_windows.ui.components.AppProgressBar
import cn.moncn.sing_box_windows.ui.components.AppSectionCard
import cn.moncn.sing_box_windows.ui.components.AppSegmentedControl
import cn.moncn.sing_box_windows.ui.components.AppTextField
import cn.moncn.sing_box_windows.ui.components.AppTopBar
import cn.moncn.sing_box_windows.ui.components.ButtonRow
import cn.moncn.sing_box_windows.ui.components.PrimaryButton
import cn.moncn.sing_box_windows.ui.components.RowDivider
import cn.moncn.sing_box_windows.ui.components.SecondaryButton
import cn.moncn.sing_box_windows.ui.components.StatusChip
import cn.moncn.sing_box_windows.ui.components.StatusTone
import cn.moncn.sing_box_windows.ui.components.SwitchRow
import cn.moncn.sing_box_windows.ui.components.appPagePadding
import cn.moncn.sing_box_windows.ui.theme.AppSpace
import cn.moncn.sing_box_windows.ui.theme.LocalAppColors
import cn.moncn.sing_box_windows.ui.theme.ThemeMode
import cn.moncn.sing_box_windows.v2.domain.settings.SettingsGateway
import cn.moncn.sing_box_windows.v2.domain.update.UpdateState

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
    val state by viewModel.state.collectAsStateWithLifecycle()

    if (!state.error.isNullOrBlank()) {
        AppAlertDialog(
            title = "设置失败",
            message = state.error,
            confirmText = "知道了",
            dismissText = null,
            onDismissRequest = { viewModel.submitIntent(SettingsIntent.ClearError) },
            onConfirm = { viewModel.submitIntent(SettingsIntent.ClearError) }
        )
    }

    SettingsScreenV2(
        state = state,
        onIntent = viewModel::submitIntent
    )
}

@Composable
internal fun SettingsScreenV2(
    state: SettingsUiState,
    onIntent: (SettingsIntent) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalAppColors.current
    val updateState = state.updateState
    val isCheckingUpdate = updateState is UpdateState.Checking
    val isDownloadingUpdate = updateState is UpdateState.Downloading

    AppBackground(glow = true) {
        LazyColumn(
            modifier = modifier.fillMaxSize(),
            contentPadding = appPagePadding(),
            verticalArrangement = AppPageArrangement
        ) {
            item(key = "header") {
                AppTopBar(
                    title = "设置",
                    subtitle = "网络参数、外观与更新管理"
                )
            }

            if (!state.message.isNullOrBlank()) {
                item(key = "message") {
                    AppInlineBanner(
                        message = state.message.orEmpty(),
                        // 结果文案可能同时包含成功与提示性信息，统一使用中性信息色避免误判
                        tone = StatusTone.INFO,
                        onDismiss = { onIntent(SettingsIntent.ClearMessage) }
                    )
                }
            }

            item(key = "appearance") {
                AppSectionCard(
                    title = "外观",
                    subtitle = "主题模式立即生效，无需重启",
                    leadingIcon = Icons.Rounded.Palette
                ) {
                    AppSegmentedControl(
                        options = ThemeMode.entries.toList(),
                        selected = state.themeMode,
                        onSelect = { onIntent(SettingsIntent.ChangeThemeMode(it)) },
                        label = { it.label }
                    )
                }
            }

            item(key = "dns") {
                AppSectionCard(
                    title = "DNS",
                    subtitle = "解析策略与缓存行为",
                    leadingIcon = Icons.Rounded.Dns
                ) {
                    DnsSelector(
                        selected = state.dnsStrategy,
                        onSelected = { onIntent(SettingsIntent.ChangeDnsStrategy(it)) }
                    )
                    RowDivider()
                    SwitchRow(
                        title = "启用 DNS 缓存",
                        checked = state.dnsCacheEnabled,
                        onCheckedChange = { onIntent(SettingsIntent.ChangeDnsCacheEnabled(it)) }
                    )
                }
            }

            item(key = "tun") {
                AppSectionCard(
                    title = "TUN 隧道",
                    subtitle = "MTU 与路由策略",
                    leadingIcon = Icons.Rounded.Route
                ) {
                    AppTextField(
                        value = state.tunMtuInput,
                        onValueChange = { onIntent(SettingsIntent.ChangeTunMtuInput(it)) },
                        label = "TUN MTU",
                        keyboardType = KeyboardType.Number
                    )
                    RowDivider()
                    SwitchRow(
                        title = "自动路由",
                        checked = state.tunAutoRoute,
                        onCheckedChange = { onIntent(SettingsIntent.ChangeTunAutoRoute(it)) }
                    )
                    RowDivider()
                    SwitchRow(
                        title = "严格路由",
                        subtitle = "严格模式下不允许绕过隧道",
                        checked = state.tunStrictRoute,
                        onCheckedChange = { onIntent(SettingsIntent.ChangeTunStrictRoute(it)) }
                    )
                    RowDivider()
                    SwitchRow(
                        title = "HTTP 代理",
                        subtitle = "在内核中开启本地 HTTP 代理",
                        checked = state.httpProxyEnabled,
                        onCheckedChange = { onIntent(SettingsIntent.ChangeHttpProxyEnabled(it)) }
                    )
                }
            }

            item(key = "actions") {
                ButtonRow {
                    PrimaryButton(
                        text = if (state.isSaving) "保存中" else "保存并应用",
                        onClick = { onIntent(SettingsIntent.Save) },
                        enabled = !state.isSaving && !state.isLoading,
                        loading = state.isSaving,
                        modifier = Modifier.weight(1f)
                    )
                    SecondaryButton(
                        text = when {
                            isCheckingUpdate -> "检查中"
                            isDownloadingUpdate -> "下载中"
                            else -> "检查更新"
                        },
                        onClick = { onIntent(SettingsIntent.CheckUpdate) },
                        enabled = !state.isSaving && !isCheckingUpdate && !isDownloadingUpdate,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            item(key = "update") {
                UpdateCard(
                    updateState = updateState,
                    kernelVersion = state.kernelVersion,
                    onIntent = onIntent
                )
            }
        }
    }
}

/**
 * 应用更新卡片。
 *
 * 根据 [UpdateState] 呈现不同状态，统一使用品牌色进度条与语义状态标签，
 * 避免不同状态下的按钮样式跳变。
 */
@Composable
private fun UpdateCard(
    updateState: UpdateState,
    kernelVersion: String?,
    onIntent: (SettingsIntent) -> Unit
) {
    val colors = LocalAppColors.current
    AppSectionCard(
        title = "应用更新",
        subtitle = kernelVersion?.takeIf { it.isNotBlank() }?.let { "内核 sing-box $it" },
        leadingIcon = Icons.Rounded.SystemUpdate
    ) {
        when (updateState) {
            UpdateState.Idle -> {
                Text(
                    text = "点击「检查更新」获取最新版本，下载完成后可通过系统通知安装。",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.textTertiary
                )
            }

            UpdateState.Checking -> {
                Text(
                    text = "正在检查新版本…",
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.textSecondary
                )
                AppProgressBar(progress = 0f)
            }

            UpdateState.UpToDate -> {
                StatusChip(text = "当前已是最新版本", tone = StatusTone.SUCCESS)
            }

            is UpdateState.UpdateAvailable -> {
                val release = updateState.releaseInfo
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "发现新版本 ${release.tagName}",
                            style = MaterialTheme.typography.titleSmall,
                            color = colors.textPrimary
                        )
                        if (release.name.isNotBlank()) {
                            Text(
                                text = release.name,
                                style = MaterialTheme.typography.bodySmall,
                                color = colors.textTertiary
                            )
                        }
                    }
                    StatusChip(text = "可更新", tone = StatusTone.INFO)
                }
                Text(
                    text = "发布时间：${release.publishedAt}",
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.textTertiary
                )
                ButtonRow {
                    PrimaryButton(
                        text = "下载更新包",
                        onClick = { onIntent(SettingsIntent.DownloadUpdate) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            is UpdateState.Downloading -> {
                val progress = updateState.progress
                Text(
                    text = "正在下载 ${updateState.releaseInfo.tagName}",
                    style = MaterialTheme.typography.titleSmall,
                    color = colors.textPrimary
                )
                AppProgressBar(progress = (progress.percentage / 100f).coerceIn(0f, 1f))
                Text(
                    text = "${progress.getPercentageString()}  ${progress.getDownloadedSizeReadable()} / ${progress.getTotalSizeReadable()}",
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.textTertiary
                )
            }

            is UpdateState.ReadyToInstall -> {
                Text(
                    text = "下载完成：${updateState.releaseInfo.tagName}",
                    style = MaterialTheme.typography.titleSmall,
                    color = colors.textPrimary
                )
                Text(
                    text = "安装包已就绪，可点击系统通知安装；若通知不可见，可用下方按钮安装。",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.textTertiary
                )
                ButtonRow {
                    PrimaryButton(
                        text = "安装已下载包",
                        onClick = { onIntent(SettingsIntent.InstallDownloadedUpdate) },
                        modifier = Modifier.weight(1f)
                    )
                    SecondaryButton(
                        text = "重新检查",
                        onClick = { onIntent(SettingsIntent.CheckUpdate) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            is UpdateState.Failed -> {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(AppSpace.sm),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    StatusChip(text = "更新失败", tone = StatusTone.ERROR)
                    Text(
                        text = updateState.error,
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.textSecondary
                    )
                }
                SecondaryButton(
                    text = "重新检查",
                    onClick = { onIntent(SettingsIntent.CheckUpdate) }
                )
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
    AppDropdownField(
        label = "DNS 策略",
        selectedLabel = DNS_LABELS[selected] ?: selected,
        expanded = expanded,
        onExpandedChange = { expanded = it },
        options = DNS_STRATEGIES,
        optionLabels = DNS_LABELS,
        onSelected = onSelected
    )
}
