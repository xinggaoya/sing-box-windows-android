# Android 端 V3 架构设计（内核原生接口迁移 + 桌面端设计对齐）

> 基线：`b29becd`（v1.2.1）→ 产出：v1.3.0
> 参考：桌面端 `xinggaoya/sing-box-windows`（Tauri 2 + Vue 3 + Rust）
> 上游内核：`SagerNet/sing-box` **v1.14.4**（commit `c005f35`）

> 本文档既是设计方案，也是实现记录；文中涉及的 API 均以 `app/libs/libbox.aar`
> （1.14.4）反编译结果为准。

---

## 1. 现状诊断

### 1.1 内核与接口

| 维度 | 现状 | 问题 |
| --- | --- | --- |
| 内核版本 | `libbox.aar` = sing-box **1.12.0** | 落后上游 2 个大版本 |
| 内核控制协议 | **Clash API**（HTTP `127.0.0.1:9090` + WebSocket `/traffic` `/memory`） | 桌面端已在 1.14 彻底移除；Clash API 为兼容层，能力受限且需轮询 |
| 代理组/出站 | `OutboundGroupManager` 轮询 `GET /proxies` | 1s 级延迟，且需 JSON 解析 |
| 测速 | `GET /proxies/{name}/delay` | 仅能返回延迟，无原生事件流 |
| 模式切换 | `PATCH /configs` | 需改配置再重载 |
| 流量/内存 | `ClashApiStreamManager` WebSocket | 自行拼装 protobuf/json |
| 日志 | **无**（未接内核日志流） | 缺日志排查能力 |
| 连接列表 | **无** | 缺连接管理 |

### 1.2 架构

`v2/`（26 文件，MVI）与老 `core/` `config/` `vpn/`（38 文件）并存，**节点、订阅、设置三项能力各有两套实现**，`v2/data/*` 仅 4 个 Gateway 薄封装老层。

### 1.3 UI

4 个页面（Home / Subscription / Nodes / Settings），主题仅有 `Color.kt` + `Type.kt`，无间距/形状/组件层设计系统。与桌面端 Indigo 三层 token（primitive → semantic → size）差距明显。

### 1.4 构建链

- `gradlew` **缺少可执行位**（mode 644），全新克隆无法 `./gradlew` 构建
- AGP 8.13.2（8.x 末代）、Kotlin 2.0.21、Compose BOM 2024.09.00、Java 11
- `gradle/libs.versions.toml` 声明 `kotlin-parcelize` 插件但未 apply

---

## 2. 目标

1. 内核升级至 sing-box **v1.14.4**，迁移至 libbox **自带 `CommandServer`/`CommandClient`** 接口，**删除全部 Clash API 代码**
2. 以 `v2/` 为唯一主线，老层能力下沉合并入 `v2` 分层，消除双份实现
3. 移植桌面端 **Indigo 设计系统**，补齐 **Connections / Logs / Outbounds / Proxy** 四个页面
4. 依赖最小改动升级（Kotlin / Compose BOM / Java 17），AGP 保持 8.13.2

---

## 3. 内核接口迁移设计

### 3.1 上游能力对齐

sing-box v1.14.4 的 `experimental/libbox` 中，`CommandClient` 底层已重构为 **官方 daemon gRPC 服务**（`daemon.StartedServiceClient`），与桌面端所用 gRPC API 同源：

```
CommandClient ──gRPC(unix socket command.sock / 127.0.0.1:port)──▶ CommandServer ──▶ 内核
```

命令常量（`experimental/libbox/command.go`）：

| 常量 | 值 | 回调 | 替代的 Clash API |
| --- | --- | --- | --- |
| `CommandLog` | 0 | `writeLogs(LogIterator)` | 无（新增能力） |
| `CommandStatus` | 1 | `writeStatus(StatusMessage)` | WS `/traffic` `/memory` |
| `CommandGroup` | 2 | `writeGroups(OutboundGroupIterator)` | 轮询 `GET /proxies` |
| `CommandClashMode` | 3 | `initializeClashMode` / `updateClashMode` | 轮询 `GET /configs` |
| `CommandConnections` | 4 | `writeConnectionEvents(ConnectionEvents)` | 无（新增能力） |
| `CommandOutbounds` | 5 | `writeOutbounds(OutboundGroupItemIterator)` | `GET /proxies`（出站维度） |

客户端动作方法（直接替代写操作）：

| 方法 | 替代 |
| --- | --- |
| `selectOutbound(group, outbound)` | `PUT /proxies/{group}` |
| `urlTest(outbound)` | `GET /proxies/{name}/delay` |
| `setClashMode(mode)` | `PATCH /configs` |
| `closeConnection(id)` / `closeConnections()` | `DELETE /connections/{id}` |
| `setGroupExpand(group, expanded)` | — |
| `serviceReload()` | 重启核心 |

### 3.2 1.12 → 1.14.4 API 破坏性变更（迁移注意）

- `CommandClientHandler` 新增 `SetDefaultLogLevel(int)`、`WriteOutbounds(...)`、`WriteConnectionEvents(...)`；`WriteConnections` → `WriteConnectionEvents`
- `CommandClientOptions.command: int` → `AddCommand(int)`（多命令订阅）
- `CommandServerHandler` 新增 `ServiceStop()`、`TriggerNativeCrash()`、`WriteDebugMessage(String)`、`ConnectSSHAgent()`
- `NewCommandServer(handler, platformInterface)` 签名变化（不再是 `(handler, port)`）
- 日志条目带 `Level` 字段

### 3.3 模块设计（已实现）

实现位于 `v2/domain/core`（领域）与 `v2/data/core`（实现）：

| 文件 | 职责 |
| --- | --- |
| `v2/domain/core/CoreModels.kt` | 领域模型：`CoreStatus` / `OutboundGroup` / `OutboundGroupItem` / `CoreConnection` / `CoreLogEntry` / `CoreClashMode` / `CoreConnectionState` |
| `v2/domain/core/CoreGateway.kt` | 领域接口：状态流 + 动作方法（不依赖任何 Android / libbox 类型） |
| `v2/data/core/DefaultCoreGateway.kt` | 基于 `CommandClient` 的实现，内含 `CommandClientHandler` 回调转接 |
| `v2/data/core/SingBoxEngine.kt` | 由 `CommandServer` 托管内核实例，提供 `start/stop/reload` |
| `v2/data/core/CoreCommandBridge.kt` | 进程级单例，供 DI 容器与 VPN 服务共享同一条命令通道 |
| `v2/data/core/CoreRuntimeCoordinator.kt` | 配置准备 + 命令通道生命周期编排 |
| `v2/data/core/LibboxBootstrap.kt` | libbox 全局引导（`SetupOptions`） |
| `v2/data/core/CoreInfoStore.kt` | 内核版本（直接读 `Libbox.version()`） |

数据流：

```
内核 gRPC ──▶ CommandClientHandler 回调 ──▶ DefaultCoreGateway 内的 MutableStateFlow
                                                      │
                                    ┌─────────────────┴──────────────────┐
                                    ▼                                    ▼
                          HomeViewModel / NodesViewModel      Connections/Logs/Outbounds ViewModel
```

**线程模型**：Go 回调线程直接写入 `MutableStateFlow`（原子更新，线程安全）；
不再使用原先 `CoreStatusManager` / `CoreInfoManager` 的 `Handler.post` 跨线程投递模式。

**连接方式**：`SetupOptions.commandServerListenPort = 0` → 内核命令服务监听
`basePath/command.sock`（unix socket），不额外占用 TCP 端口。

### 3.4 配置变更

- `TemplateConfigBuilder.buildExperimentalConfig()` 移除 `clash_api` 段
- `ConfigSettingsApplier` 的 `clash_api.default_mode` 改为记录到 `AppSettings`，由 `setClashMode()` 主动下发
- `ClashApiDefaults.kt` 删除

### 3.5 删除清单（已执行）

已删除 16 个类：

`core/ClashApiClient.kt`、`core/ClashApiStreamManager.kt`、`core/ClashApiDiagnosticsManager.kt`、
`core/OutboundGroupManager.kt`、`core/ClashModeManager.kt`、`core/CoreStatusManager.kt`、
`core/CoreInfoManager.kt`、`core/CoreStatusStore.kt`、`core/CoreInfoStore.kt`、
`core/CommandServerHolder.kt`、`core/LibboxServiceHolder.kt`、`core/LibboxManager.kt`、
`core/SingBoxEngine.kt`、`core/CoreRuntimeCoordinator.kt`、`core/CoreStartResult.kt`、
`config/ClashApiDefaults.kt`

`core/` 包已整体清空；`config/` 包下沉至 `v2/data/config/`。
`config/ClashConverter.kt` 保留（负责 Clash 格式订阅解析，与内核接口无关）。

---

## 3.6 配置生成与格式迁移（补充）

内核二进制升级后，**配置格式必须同步迁移**，否则内核会直接拒绝解码
（`legacy inbound fields are deprecated in sing-box 1.11.0 and removed in sing-box 1.13.0`）。

### 生成层（`v2/data/config/`）

| 文件 | 职责 |
| --- | --- |
| `TemplateConfigBuilder` | 订阅 → sing-box 配置模板（1.14 格式） |
| `ConfigDefaults` | 兜底默认配置（1.14 格式） |
| `ClashConverter` | Clash YAML → sing-box 配置（1.14 格式） |
| `ConfigSettingsApplier` | 把用户设置写入配置 |
| **`ConfigMigrator`** | **历史配置 → 当前内核格式** |
| `ConfigRepository` | 配置持久化 + 归一化（加载时自动套用迁移） |

### 迁移项

| 来源 | 目标 | 说明 |
| --- | --- | --- |
| 入站 `sniff` / `sniff_override_destination` / `sniff_timeout` / `domain_strategy` / `udp_disable_domain_unmapping` | 移除 | 1.11 弃用 / 1.13 移除；嗅探改由 `route.rules` 的 `action: sniff` 承担 |
| DNS `independent_cache` | 移除 | 1.14 弃用 / 1.16 移除，缓存行为已由内核固定 |
| DNS 服务器 `{"address": "tls://1.1.1.1"}` | `{"type": "tls", "server": "1.1.1.1"}` | 1.12 弃用 / 1.14 移除（含 path / port 拆分） |
| `experimental.clash_api` | 移除 | 已由 libbox 原生命令接口取代，不再开启本地 9090 监听 |

### 双保险

1. **加载即迁移**：`ConfigRepository.loadOrCreateConfig` 对设备上已持久化的
   历史配置自动迁移并回写，用户无需手动重置订阅
2. **启动即校验**：`SingBoxEngine.start` 先调用内核自身的 `Libbox.checkConfig`，
   配置非法时把内核给出的**精确原因**直接呈现，而不是笼统的「启动失败」

### 回归防护

`SingBoxConfigOracleTest` 用**真实 sing-box CLI** 对四类配置执行 `check`：

- 订阅生成的模板配置
- 默认兜底配置
- 迁移后的历史配置
- Clash 格式转换结果

```bash
# 构建与 AAR 同 revision 的 sing-box CLI 作为校验 oracle
go build -tags "with_quic,with_wireguard,with_utls,with_clash_api,with_gvisor" \
  -o /tmp/sing-box-cli ./cmd/sing-box
SING_BOX_CLI=/tmp/sing-box-cli ./gradlew :app:testDebugUnitTest
```

未提供 `SING_BOX_CLI` 时该组用例会自动跳过，不影响常规 CI。

---

## 4. 架构收敛（v2 为主线，已执行）

包结构（实现后）：

```
cn/moncn/sing_box_windows/
├── ui/                     设计系统：theme/ + components/ + Format 工具
├── vpn/                    VpnService + PlatformInterface 桥接（系统边界，保留）
└── v2/
    ├── RefactorMainActivity.kt
    ├── app/                V2AppRoot（导航）、V2Route（路由表）
    ├── core/
    │   ├── arch/           MviViewModel（Channel 串行消费 Intent）、UiContract
    │   └── di/             V2Container
    ├── domain/             core / nodes / subscription / settings / runtime / update
    ├── data/               config / core / nodes / runtime / settings / subscription / update
    └── feature/            connections / home / logs / nodes / outbounds / settings / subscription
```

**收敛原则**

1. `v2/domain/*` 只定义接口与模型，**不含任何 Android / libbox / 实现层类型**
   （已校验：domain 层 0 处越界 import，feature 层 0 处直接依赖 data/libbox）
2. `v2/data/*` 持有全部实现，`v2/feature/*` 只依赖 `domain` 接口
3. 老 `core/` 全部能力下沉；`vpn/` 属系统边界保留；应用内更新模块归入 `v2/data/update`
4. `MviViewModel` 改为 `Channel<I>` 串行消费 Intent，消除 `submitIntent` 并发交错
   （原 `SettingsViewModel.save()` 的 `isSaving` 检查存在竞态）

---

## 5. 设计系统移植

### 5.1 令牌

三层结构，与桌面端 `src/assets/tokens.css` 一一对应：

**Primitive（固定）**
- Indigo：`#eef2ff #e0e7ff #c7d2fe #a5b4fc #818cf8 #6366f1 #4f46e5 #4338ca #3730a3 #312e81`
- Slate：`#f8fafc #f1f5f9 #e2e8f0 #cbd5e1 #94a3b8 #64748b #475569 #334155 #1e293b #0f172a #020617`
- 功能色：green `#34d399/#10b981/#059669`、amber `#fbbf24/#f59e0b/#d97706`、red `#f87171/#ef4444/#dc2626`、blue `#60a5fa/#3b82f6/#2563eb`、orange `#f97316`

**Semantic**
- 亮：primary `#6366f1`、text slate-900/600/400/300、bg slate-50/#fff/slate-100
- 暗：primary `#818cf8`、text `#f8fafc`/`#cbd5e1`/`#64748b`/`#475569`、bg slate-900/slate-800/slate-700

**Size**
- radius：xs 6 / sm 8 / md 12 / lg 16 / xl 20 / 2xl 24 / pill 9999
- spacing：4 网格（4/8/12/16/20/24/28/32/40/48）
- shadow：xs→xl 五级 + focus ring

### 5.2 Compose 落地（已实现）

- `ui/theme/IndigoPalette.kt`：原始色板常量（与桌面端 `tokens.css` 第一层一致）
- `ui/theme/IndigoTokens.kt`：语义令牌（亮/暗两套）+ `LocalIndigoTokens`
- `ui/theme/Color.kt`：`lightColorScheme` / `darkColorScheme` 映射
- `ui/theme/Shape.kt`：radius 阶梯；`ui/theme/Dimens.kt`：spacing 阶梯
- `ui/theme/Type.kt`：字号阶梯（xs 12 → 3xl 30）+ 等宽 `MonoTextStyle`
- `ui/components/`：`SectionCard` / `SectionCardWithHeader` / `MetricTile` / `MetricRow` /
  `StatusChip` / `EmptyState`
- `ui/Format.kt`：字节 / 速率 / 时长格式化（配套单元测试）

### 5.3 新页面（已实现）

| 页面 | 路由 | 数据源 | 核心交互 |
| --- | --- | --- | --- |
| **连接** | `connections` | `CommandConnections` → `writeConnectionEvents` | 活跃 / 已关闭筛选、单条关闭、全部关闭、上下行流量 |
| **日志** | `logs` | `CommandLog` → `writeLogs` | 级别过滤、清空、等宽排版、自动滚动 |
| **出站** | `outbounds` | `CommandOutbounds` → `writeOutbounds` | 出站清单、单条 / 全部测速、延迟状态 |
| **代理** | `proxy` | `CommandGroup` → `writeGroups` | 代理组展开、节点选择（`selectOutbound`）、测速（`urlTest`） |

导航：底部导航 `首页 / 代理 / 连接 / 订阅 / 设置`；
`日志` 与 `出站` 作为首页的「工具」快捷入口（与桌面端侧边栏层级对应）。

---

## 6. 依赖升级（最小改动）

| 组件 | 现状 | 目标（已执行） |
| --- | --- | --- |
| AGP | 8.13.2 | **保持 8.13.2** |
| Gradle | 8.13 | 保持 8.13 |
| Kotlin | 2.0.21 | **2.2.21** |
| Compose BOM | 2024.09.00 | **2026.06.01** |
| Java | 11 | 17 |
| core-ktx / lifecycle / activity / navigation | 1.10.1 / 2.6.1 / 1.8.0 / 2.8.5 | 保持或随 Compose BOM 同步的最小必要升级 |
| okhttp / serialization | 4.12.0 / 1.7.3 | 保持（超出最小改动范围） |

> **为什么不是 Compose BOM 2026.09.00**：该版本对应 `compose ui 1.12.1`，其
> `aar-metadata.properties` 声明 `minAndroidGradlePluginVersion=9.1.0`、
> `minCompileSdk=37`，与「保持 AGP 8.13.2」冲突。
> 2026.06.01（`compose ui 1.11.4`）声明 `minAGP=8.6.0` / `minCompileSdk=35`，可用。

附带修复：`gradlew` 可执行位；`libs.versions.toml` 移除未使用的 `kotlin-parcelize`；
Release 启用 R8 + `shrinkResources`（arm64 APK 136.7 MB → 78.5 MB）。

---

## 7. 实施阶段与完成情况

| 阶段 | 内容 | 状态 |
| --- | --- | --- |
| P0 | 环境（SDK/NDK）、`local.properties`、`gradlew` 可执行位 | ✅ |
| P1 | 编译 sing-box v1.14.4 `libbox.aar`，产出 `libbox.version.json` | ✅ commit `c005f35` |
| P2 | 依赖最小升级（Kotlin 2.2.21 / Compose BOM 2026.06.01 / Java 17） | ✅ |
| P3 | `v2/domain/core` + `v2/data/core` 命令层，删除 Clash API 层 | ✅ 删除 16 个类 |
| P4 | 架构收敛：老 `core/` 清空、`config/` 下沉 `v2/data/config/` | ✅ |
| P5 | Indigo 设计系统（令牌 + 组件库 + 主题） | ✅ |
| P6 | 连接 / 日志 / 出站 / 代理 四页 + 导航重构 | ✅ |
| P7 | CI、文档、CHANGELOG、单元测试 | ✅ 21 个用例通过 |

**验证结果**

| 项 | 结果 |
| --- | --- |
| `./gradlew assembleDebug` | ✅ BUILD SUCCESSFUL（4 个 ABI APK） |
| `./gradlew assembleRelease` | ✅ BUILD SUCCESSFUL（R8 + 资源裁剪） |
| `./gradlew testDebugUnitTest` | ✅ 21 个用例 / 0 失败 |
| Release APK 体积（arm64） | 136.7 MB → **78.5 MB** |
| 内核版本校验 | `strings libbox.so` 含 `1.14.4`；`libbox.version.json` 记录 commit |

## 8. 风险

| 风险 | 缓解 |
| --- | --- |
| libbox 1.14.4 API 变更导致既有调用不兼容 | P1 后立即用 `javap` 导出新 API 清单，据此改写 `core/` |
| CommandServer 端口/套接字在 Android 后台进程被杀后失效 | 连接状态变化时统一 `disconnect/connect`，并加指数退避重连 |
| Go 回调线程与 Compose 状态竞争 | 全部经 `MutableStateFlow`（原子更新），不直接改 Compose 状态 |
| Compose BOM 2026.09 与 AGP 8.13.2 不兼容 | 若编译失败，退至 2025.x BOM（仍属最小改动范畴） |
| 桌面端 gRPC 协议细节不可直接照搬 | Android 走 libbox 封装的 `CommandClient`，无需自行实现 gRPC 编解码 |
