# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## 项目概述

这是一个基于 sing-box 核心的 Android VPN 客户端应用，使用 Kotlin 和 Jetpack Compose (Material 3) 构建。应用支持订阅管理、节点选择、流量监控和 Clash API 诊断等功能。

## 构建命令

```bash
# 构建 Debug 版本
./gradlew assembleDebug

# 构建 Release 版本
./gradlew assembleRelease

# 安装到连接的设备
./gradlew installDebug

# 按架构构建特定 APK (splits.abi 配置)
./gradlew assembleRelease  # 会生成 arm64-v8a, armeabi-v7a, x86, x86_64 四个独立 APK
```

## 架构概览

### 核心架构模式

应用采用 **Clean Architecture + MVI** 模式：

1. **领域层 (`v2/domain`)**: 只定义能力接口与领域模型，**不含任何 Android / libbox 类型**
2. **数据层 (`v2/data`)**: 全部实现（内核命令层、配置仓储、订阅仓储）
3. **功能层 (`v2/feature`)**: 页面 + MVI ViewModel，仅依赖领域接口
4. **MVI 基座 (`v2/core/arch`)**: 统一 `Intent / State / Effect` 数据流，
   Intent 通过 `Channel` **串行消费**，避免并发交错导致的重复提交

依赖方向单向：`feature → domain ← data`。

### 关键组件

#### 1. VPN 服务层 (`vpn/`) — 系统边界

- `AppVpnService`: Android VpnService 实现，负责 VPN 接口建立与生命周期
- `VpnStateStore`: 全局 VPN 状态单例 (IDLE/CONNECTING/CONNECTED/ERROR)
- `VpnController`: 控制 VPN 服务的启动/停止
- `PlatformInterfaceBridge`: 实现 libbox 1.14 的 `PlatformInterface`；
  Shell / SSH / Bridge / Tailscale 等平台专属能力在 Android 上显式返回「不支持」

#### 2. 内核命令层 (`v2/data/core`) — ★ 核心

- `LibboxBootstrap`: libbox 全局引导（`SetupOptions`，含 `commandServerListenPort = 0`）
- `SingBoxEngine`: 由 `CommandServer` 托管内核实例（1.11 起 libbox 不再暴露独立 `BoxService`）
- `DefaultCoreGateway`: 基于 `CommandClient` 的 `CoreGateway` 实现，
  内含 `CommandClientHandler` 回调转接
- `CoreCommandBridge`: 进程级单例，供 DI 容器与 VPN 服务共享同一条命令通道
- `CoreRuntimeCoordinator`: 配置准备 + 命令通道生命周期编排
- `CoreInfoStore`: 内核版本（直接读 `Libbox.version()`）

#### 3. 配置层 (`v2/data/config`)

> 领域模型已下沉到 `v2/domain`：`AppSettings` → `v2/domain/settings`，
> 订阅模型 → `v2/domain/subscription`，`VpnState` → `v2/domain/runtime`，
> 更新模型 → `v2/domain/update`。

- `ConfigRepository`: sing-box JSON 配置的加载、保存和迁移
- `ConfigSettingsApplier`: 将用户设置应用到配置 JSON
- `ConfigDefaults`: 默认配置模板
- `SubscriptionRepository`: 订阅列表 CRUD，支持远程订阅与本地节点导入
- `ClashConverter`: Clash YAML 配置转换为 sing-box JSON
- `SettingsRepository/AppSettings`: 用户设置持久化

#### 4. 设计系统 (`ui/`)

- `theme/IndigoPalette`: 原始色板（与桌面端 `tokens.css` 第一层一致）
- `theme/IndigoTokens`: 语义令牌（亮/暗两套）+ `LocalIndigoTokens`
- `theme/Color / Shape / Dimens / Type`: Material3 配色、圆角、间距、字号
- `components/`: `SectionCard` / `MetricTile` / `StatusChip` / `EmptyState` 等通用组件
- `Format`: 字节 / 速率 / 时长格式化

#### 5. 导航与入口 (`v2/app`、`v2/RefactorMainActivity.kt`)

- `V2AppRoot`: Scaffold + NavHost 壳层，底部导航 `首页 / 代理 / 连接 / 订阅 / 设置`
- `V2Route`: 路由表，`日志` 与 `出站` 作为首页的二级入口
- `V2Container`: 手写依赖装配，注入 5 个 Gateway

### 数据流

```
用户操作 → ViewModel.submitIntent → MVI 串行处理 → domain Gateway
                                                       ↓
                                            data 层实现（libbox CommandClient）
                                                       ↓
                              内核 gRPC 主动推送 → MutableStateFlow → Compose UI
```

### 内核接口集成

应用通过 **libbox 自带的 `CommandServer` / `CommandClient`** 与 sing-box 交互
（底层为 sing-box 官方 daemon gRPC 服务，与桌面端 `sing-box-windows` 同源）：

| 命令 | 用途 | 取代的 Clash API 端点 |
| --- | --- | --- |
| `CommandStatus` | 流量 / 内存 / 协程数 | WS `/traffic` `/memory` |
| `CommandGroup` | 代理组与成员 | `GET /proxies` |
| `CommandOutbounds` | 出站列表 | `GET /proxies` |
| `CommandConnections` | 活跃连接 | `GET /connections` |
| `CommandClashMode` | 代理模式 | `GET /configs` / `PATCH /configs` |
| `CommandLog` | 内核日志 | 无（新增能力） |

动作接口：`selectOutbound` / `urlTest` / `setClashMode` / `setGroupExpand` /
`closeConnection(s)`。

> **配置中不再写入 `experimental.clash_api`**，因此应用不会在 `127.0.0.1:9090`
> 开启 HTTP / WebSocket 监听；代理模式改为连接建立后通过 `setClashMode` 下发。

## 重要约定

### 1. 状态管理模式

所有全局状态都使用 Store 单例模式，定义在对应模块中：

```kotlin
object SomeStore {
    var state by mutableStateOf<SomeState?>(null)
        private set

    fun update(newState: SomeState) {
        state = newState
    }
}
```

### 2. 文件存储位置

- 配置文件: `context.filesDir/singbox.json`
- 订阅数据: `context.filesDir/subscriptions.json`
- 设置数据: `context.filesDir/settings.json`

### 3. libbox AAR 依赖

核心库位于 `app/libs/libbox.aar`，提供 `io.nekohasekai.libbox` 包下的 JNI 接口。

### 4. 架构拆分

- `splits.abi` 配置会为每个 CPU 架构生成独立 APK
- 通用 APK 已禁用 (`universalApk false`)

### 5. 页面导航

页面切换通过 `AppNavigation` 中的 `Screen` sealed class 定义，添加新页面需要：
1. 在 `Screen` 中添加新路由
2. 在 `screens` 列表中添加
3. 在 NavHost 中添加 composable

### 6. 主题和样式

使用 Material 3 设计系统，主题定义在 `ui/theme/` 目录。状态颜色根据 VPN 状态动态变化（在 AppNavigation 中定义）。
