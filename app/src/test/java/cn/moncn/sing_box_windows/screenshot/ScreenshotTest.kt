package cn.moncn.sing_box_windows.screenshot

import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.performClick
import cn.moncn.sing_box_windows.ui.theme.ThemeMode
import cn.moncn.sing_box_windows.v2.app.V2Route
import cn.moncn.sing_box_windows.v2.domain.core.CoreClashMode
import cn.moncn.sing_box_windows.v2.domain.core.CoreConnection
import cn.moncn.sing_box_windows.v2.domain.core.CoreLogEntry
import cn.moncn.sing_box_windows.v2.domain.core.CoreStatus
import cn.moncn.sing_box_windows.v2.domain.core.OutboundGroup
import cn.moncn.sing_box_windows.v2.domain.core.OutboundGroupItem
import cn.moncn.sing_box_windows.v2.domain.subscription.SubscriptionItem
import cn.moncn.sing_box_windows.v2.domain.subscription.SubscriptionState
import cn.moncn.sing_box_windows.v2.domain.update.GitHubRelease
import cn.moncn.sing_box_windows.v2.domain.update.UpdateState
import cn.moncn.sing_box_windows.v2.domain.runtime.VpnState
import cn.moncn.sing_box_windows.v2.feature.connections.ConnectionFilter
import cn.moncn.sing_box_windows.v2.feature.connections.ConnectionsScreenV2
import cn.moncn.sing_box_windows.v2.feature.connections.ConnectionsUiState
import cn.moncn.sing_box_windows.v2.feature.home.HomeScreenV2
import cn.moncn.sing_box_windows.v2.feature.home.HomeUiState
import cn.moncn.sing_box_windows.v2.feature.logs.LogsScreenV2
import cn.moncn.sing_box_windows.v2.feature.logs.LogsUiState
import cn.moncn.sing_box_windows.v2.feature.nodes.NodesScreenV2
import cn.moncn.sing_box_windows.v2.feature.nodes.NodesUiState
import cn.moncn.sing_box_windows.v2.feature.outbounds.OutboundsScreenV2
import cn.moncn.sing_box_windows.v2.feature.outbounds.OutboundsUiState
import cn.moncn.sing_box_windows.v2.feature.settings.SettingsScreenV2
import cn.moncn.sing_box_windows.v2.feature.settings.SettingsUiState
import cn.moncn.sing_box_windows.v2.feature.subscription.SubscriptionScreenV2
import cn.moncn.sing_box_windows.v2.feature.subscription.SubscriptionUiState
import org.junit.Test

/**
 * 全页面视觉回归截图。
 *
 * 每个页面在浅色 / 深色两套主题下各渲染一次，输出到 `build/screenshots/`，
 * 用于人工目检布局层级、留白、对比度与组件状态。
 */
class ScreenshotTest : ScreenshotHarness() {

    // ---------------- 首页 ----------------

    @Test
    fun home_light() {
        renderTopLevel(V2Route.Home.route, ThemeMode.LIGHT) {
            HomeScreenV2(
                state = homeState(),
                onConnect = {},
                onDisconnect = {},
                onSwitchMode = {},
                onOpenLogs = {},
                onOpenOutbounds = {},
                onOpenConnections = {}
            )
        }
        capture("01_home_light")
    }

    @Test
    fun home_dark() {
        renderTopLevel(V2Route.Home.route, ThemeMode.DARK) {
            HomeScreenV2(
                state = homeState(),
                onConnect = {},
                onDisconnect = {},
                onSwitchMode = {},
                onOpenLogs = {},
                onOpenOutbounds = {},
                onOpenConnections = {}
            )
        }
        capture("02_home_dark")
    }

    // ---------------- 代理 ----------------

    @Test
    fun nodes_light() {
        renderTopLevel(V2Route.Proxy.route, ThemeMode.LIGHT) {
            NodesScreenV2(
                state = nodesState(),
                onSelect = { _, _ -> },
                onTest = {}
            )
        }
        // 展开第一个分组，展示节点列表的展开态
        composeRule.onAllNodesWithText("展开节点列表")[0].performClick()
        composeRule.waitForIdle()
        capture("03_nodes_light")
    }

    @Test
    fun nodes_dark() {
        renderTopLevel(V2Route.Proxy.route, ThemeMode.DARK) {
            NodesScreenV2(
                state = nodesState(),
                onSelect = { _, _ -> },
                onTest = {}
            )
        }
        composeRule.onAllNodesWithText("展开节点列表")[0].performClick()
        composeRule.waitForIdle()
        capture("04_nodes_dark")
    }

    // ---------------- 连接 ----------------

    @Test
    fun connections_light() {
        renderTopLevel(V2Route.Connections.route, ThemeMode.LIGHT) {
            ConnectionsScreenV2(
                state = connectionsState(),
                onSetFilter = {},
                onCloseConnection = {},
                onCloseAll = {}
            )
        }
        capture("05_connections_light")
    }

    @Test
    fun connections_dark() {
        renderTopLevel(V2Route.Connections.route, ThemeMode.DARK) {
            ConnectionsScreenV2(
                state = connectionsState(),
                onSetFilter = {},
                onCloseConnection = {},
                onCloseAll = {}
            )
        }
        capture("06_connections_dark")
    }

    // ---------------- 订阅 ----------------

    @Test
    fun subscription_light() {
        renderTopLevel(V2Route.Subscription.route, ThemeMode.LIGHT) {
            SubscriptionScreenV2(
                state = subscriptionState(),
                onAddClick = {},
                onActivate = {},
                onSync = {},
                onEdit = {},
                onDelete = {},
                onClearMessage = {}
            )
        }
        capture("07_subscription_light")
    }

    @Test
    fun subscription_dark() {
        renderTopLevel(V2Route.Subscription.route, ThemeMode.DARK) {
            SubscriptionScreenV2(
                state = subscriptionState(),
                onAddClick = {},
                onActivate = {},
                onSync = {},
                onEdit = {},
                onDelete = {},
                onClearMessage = {}
            )
        }
        capture("08_subscription_dark")
    }

    // ---------------- 设置 ----------------

    @Test
    fun settings_light() {
        renderTopLevel(V2Route.Settings.route, ThemeMode.LIGHT) {
            SettingsScreenV2(
                state = settingsState(),
                onIntent = {}
            )
        }
        capture("09_settings_light")
    }

    @Test
    fun settings_dark() {
        renderTopLevel(V2Route.Settings.route, ThemeMode.DARK) {
            SettingsScreenV2(
                state = settingsState(),
                onIntent = {}
            )
        }
        capture("10_settings_dark")
    }

    // ---------------- 日志 ----------------

    @Test
    fun logs_light() {
        renderSecondary(ThemeMode.LIGHT) {
            LogsScreenV2(
                state = logsState(),
                onSetFilter = {},
                onSetAutoScroll = {},
                onClear = {},
                onBack = {}
            )
        }
        capture("11_logs_light")
    }

    @Test
    fun logs_dark() {
        renderSecondary(ThemeMode.DARK) {
            LogsScreenV2(
                state = logsState(),
                onSetFilter = {},
                onSetAutoScroll = {},
                onClear = {},
                onBack = {}
            )
        }
        capture("12_logs_dark")
    }

    // ---------------- 出站 ----------------

    @Test
    fun outbounds_light() {
        renderSecondary(ThemeMode.LIGHT) {
            OutboundsScreenV2(
                state = outboundsState(),
                onTest = {},
                onTestAll = {},
                onBack = {}
            )
        }
        capture("13_outbounds_light")
    }

    @Test
    fun outbounds_dark() {
        renderSecondary(ThemeMode.DARK) {
            OutboundsScreenV2(
                state = outboundsState(),
                onTest = {},
                onTestAll = {},
                onBack = {}
            )
        }
        capture("14_outbounds_dark")
    }

    // ---------------- 样本数据 ----------------

    private fun homeState() = HomeUiState(
        vpnState = VpnState.CONNECTED,
        coreStatus = CoreStatus(
            trafficAvailable = true,
            uplink = 128_400L,
            downlink = 1_258_291L,
            uplinkTotal = 52_428_800L,
            downlinkTotal = 1_073_741_824L,
            memory = 48_234_496L,
            goroutines = 42,
            connectionsIn = 3,
            connectionsOut = 7
        ),
        currentMode = CoreClashMode.RULE,
        modeSupported = true
    )

    private fun nodesState() = NodesUiState(
        groups = listOf(
            OutboundGroup(
                tag = "节点选择",
                type = "Selector",
                selectable = true,
                selected = "香港 01",
                items = listOf(
                    OutboundGroupItem("香港 01", "shadowsocks", 1_730_000_000_000L, 86),
                    OutboundGroupItem("日本 02", "vmess", 1_730_000_000_000L, 142),
                    OutboundGroupItem("新加坡 03", "trojan", 0L, -1)
                )
            ),
            OutboundGroup(
                tag = "自动选择",
                type = "URLTest",
                selectable = false,
                selected = "",
                items = listOf(
                    OutboundGroupItem("香港 01", "shadowsocks", 1_730_000_000_000L, 86),
                    OutboundGroupItem("日本 02", "vmess", 1_730_000_000_000L, 142)
                )
            )
        )
    )

    private fun connectionsState() = ConnectionsUiState(
        connections = listOf(
            CoreConnection(
                id = "c1",
                inbound = "mixed-in",
                inboundType = "mixed",
                network = "tcp",
                source = "192.168.1.24:52344",
                destination = "142.250.72.206",
                domain = "www.google.com",
                protocol = "tls",
                outbound = "香港 01",
                outboundType = "shadowsocks",
                rule = "geosite-cn",
                createdAt = 1_730_000_000_000L,
                uplinkTotal = 1_048_576L,
                downlinkTotal = 8_388_608L
            ),
            CoreConnection(
                id = "c2",
                inbound = "tun-in",
                inboundType = "tun",
                network = "udp",
                source = "10.0.0.2:33000",
                destination = "104.16.132.229",
                domain = "api.github.com",
                protocol = "quic",
                outbound = "日本 02",
                outboundType = "vmess",
                rule = "geosite-ads",
                createdAt = 1_730_000_000_000L,
                uplinkTotal = 262_144L,
                downlinkTotal = 524_288L
            ),
            CoreConnection(
                id = "c3",
                inbound = "mixed-in",
                inboundType = "mixed",
                network = "tcp",
                source = "192.168.1.31:44102",
                destination = "223.5.5.5",
                domain = "dns.alidns.com",
                protocol = "dns",
                outbound = "直连",
                outboundType = "direct",
                rule = "direct",
                createdAt = 1_729_999_000_000L,
                closedAt = 1_729_999_500_000L,
                uplinkTotal = 4_096L,
                downlinkTotal = 8_192L
            )
        ),
        filter = ConnectionFilter.ALL
    )

    private fun subscriptionState() = SubscriptionUiState(
        subscriptions = SubscriptionState(
            items = listOf(
                SubscriptionItem(
                    id = "s1",
                    name = "自建节点",
                    url = "https://example.com/sub",
                    lastUpdatedAt = 1_730_000_000_000L
                ),
                SubscriptionItem(
                    id = "s2",
                    name = "机场订阅",
                    url = "https://sub.example.org/clash",
                    lastUpdatedAt = 1_729_990_000_000L,
                    lastError = "订阅内容为空"
                )
            ),
            selectedId = "s1"
        ),
        isLoading = false
    )

    private fun settingsState() = SettingsUiState(
        dnsStrategy = "prefer_ipv4",
        dnsCacheEnabled = true,
        tunMtuInput = "9000",
        tunAutoRoute = true,
        tunStrictRoute = true,
        httpProxyEnabled = false,
        clashMode = "rule",
        themeMode = ThemeMode.LIGHT,
        isLoading = false,
        updateState = UpdateState.UpdateAvailable(
            GitHubRelease(
                tagName = "v1.4.0",
                name = "Aurora Glass 视觉重构",
                body = "全新视觉体系",
                publishedAt = "2026-10-28",
                assets = emptyList()
            )
        ),
        kernelVersion = "1.14.4"
    )

    private fun logsState() = LogsUiState(
        logs = listOf(
            CoreLogEntry(CoreLogEntry.LEVEL_INFO, "sing-box 1.14.4 started with 7 outbounds"),
            CoreLogEntry(CoreLogEntry.LEVEL_INFO, "tun inbound listening on 172.19.0.1:2080"),
            CoreLogEntry(CoreLogEntry.LEVEL_DEBUG, "selector[节点选择] -> 香港 01"),
            CoreLogEntry(CoreLogEntry.LEVEL_WARN, "outbound 香港 01 handshake timeout, retrying"),
            CoreLogEntry(CoreLogEntry.LEVEL_ERROR, "dial tcp 104.16.132.229:443: i/o timeout")
        )
    )

    private fun outboundsState() = OutboundsUiState(
        outbounds = listOf(
            OutboundGroupItem("direct", "direct", 1_730_000_000_000L, 12),
            OutboundGroupItem("香港 01", "shadowsocks", 1_730_000_000_000L, 86),
            OutboundGroupItem("日本 02", "vmess", 1_730_000_000_000L, 142),
            OutboundGroupItem("block", "block", 0L, -1)
        )
    )
}
