# Sing Box Windows - Android VPN 客户端

## v1.3.0 (2026-10-29)

### 主要更新

#### 内核升级：sing-box 1.12.0 → 1.14.4
- `app/libs/libbox.aar` 重新编译自 sing-box **v1.14.4**（commit `c005f35`）
- 新增 `app/libs/libbox.version.json`，记录上游标签 / 提交 / 工具链版本，内核来源可追溯
- 同步适配 libbox 1.14 的 API 变更：
  - `PlatformInterface`：新增 Shell / SSH / Bridge / Tailscale / 邻居发现等能力，
    Android 端统一以「明确不支持」方式返回；`findConnectionOwner` 改为返回 `ConnectionOwner`
  - `TunOptions`：`dns_server_address` 改为字符串迭代器，HTTP 代理相关字段上移
  - `SetupOptions`：新增命令服务端口 / 密钥 / 日志缓冲行数等配置

#### 内核接口迁移：Clash API → libbox 原生命令接口
- **彻底移除 Clash API**（HTTP `127.0.0.1:9090` + WebSocket），与桌面端
  `sing-box-windows` 迁移到 sing-box 官方 API 的方向保持一致
- 新增 `v2/core` + `v2/data/core` 命令层：
  - `SingBoxEngine`：由 `CommandServer` 托管内核实例（1.11 起 libbox 不再暴露独立 `BoxService`）
  - `DefaultCoreGateway`：基于 `CommandClient` 订阅 `CommandStatus / CommandGroup /
    CommandClashMode / CommandConnections / CommandOutbounds / CommandLog`
  - `CoreCommandBridge`：进程级单例，供 DI 容器与 VPN 服务共享同一条命令通道
- 全部数据改为**内核主动推送**（事件驱动），不再需要定时轮询：
  - 流量 / 内存 / 协程数：Clash API WebSocket → `CommandStatus`
  - 代理组 / 出站：轮询 `GET /proxies` → `CommandGroup` / `CommandOutbounds`
  - 连接列表：新增能力 → `CommandConnections`
  - 内核日志：新增能力 → `CommandLog`
- 动作接口同步改为内核原生调用：`selectOutbound` / `urlTest` / `setClashMode` /
  `setGroupExpand` / `closeConnection(s)`
- 生成配置不再写入 `experimental.clash_api`，**关闭了本地 9090 端口的 HTTP / WS 监听**

### 新增功能

#### 桌面端能力对齐
- 新增 **连接** 页面：活跃连接列表、上/下行流量、命中规则、单条 / 全部关闭
- 新增 **日志** 页面：内核实时日志、按级别过滤、清空、等宽排版
- 新增 **出站** 页面：出站清单、单条 / 全部测速、延迟状态
- **代理** 页面（原节点面板）：代理组展开、节点选择、测速、选中态高亮

### 界面改进

#### 移植桌面端 Indigo 设计系统
- 三层令牌结构：原始色板（Primitive）→ 语义令牌（Semantic）→ 尺寸令牌（Size）
- 主色靛蓝 `#6366F1`（暗色 `#818CF8`），中性色 Slate，功能色绿 / 琥珀 / 红 / 蓝
- 新增 `ui/theme/`：`IndigoPalette` / `IndigoTokens` / `Shape` / `Dimens` / `Type`
- 新增 `ui/components/`：`SectionCard` / `MetricTile` / `StatusChip` / `EmptyState`
- 首页、代理、连接、日志、出站、订阅、设置页面统一换肤，移除硬编码色值

### 架构重构

#### v2 为主线，老层下沉合并
- 删除 `core/` 包下 15 个类（Clash API 系列、轮询管理器、状态存储等），全部职责并入 `v2/`
- `config/` 包下沉至 `v2/data/config/`，包结构收敛为
  `v2/{app,core,data,domain,feature}` + `ui/` + `vpn/` + `update/`
- `MviViewModel` 改为 Channel 串行消费 Intent，消除并发交错导致的重复提交竞态
- `V2Container` 注入 `CoreGateway`，依赖关系单向（feature → domain ← data）

### 构建与工程

- 修复 `gradlew` 缺少可执行位（全新克隆无法构建）
- 依赖升级（保持 AGP 8.13.2 不变）：Kotlin `2.0.21 → 2.2.21`、
  Compose BOM `2024.09.00 → 2026.06.01`（兼容 AGP 8.x 的最新版本）、Java `11 → 17`
- Release 构建启用 R8 + 资源裁剪：APK 体积从 136.7 MB 降至 **78.5 MB**（arm64）
- `scripts/update-libbox.sh` 修复三处健壮性问题：
  - `GOBIN` 未绑定到 `$GOPATH/bin` 导致 `missing gomobile installation`
  - mise / asdf 的 `go` shim 覆盖 `JAVA_HOME` 导致 OpenJDK 17 校验失败
  - Go 临时编译目录落在容量受限的 `/tmp` 导致磁盘耗尽
  - 源码目录复用逻辑，支持脚本幂等重跑
- 新增 `.github/workflows/ci.yml`：推主干 / 提 PR 时执行 lint + 单元测试 + Debug 构建
- `release.yml` 增加 Go 模块缓存、内核版本入参、单元测试步骤与内核版本摘要
- 新增 21 个单元测试，覆盖格式化工具、领域模型、配置应用与模板生成

### 代码审查修复

经两轮独立代码审查（对照上游 sing-box v1.14.4 源码与 AAR 字节码逐项校验），
修复以下阻塞级与重要级问题：

- **MVI 消费协程异常兜底**：`handleIntent` 抛异常会导致消费协程终止，
  后续 Intent 被静默吞掉且可能直接崩溃 —— 增加异常边界与 `onIntentError` 钩子，
  各 ViewModel 将异常写入状态并在页面提示
- **内核命令通道建立失败不再卡死**：失败时状态回落为 `DISCONNECTED_ERROR`，
  并由 `AndroidRuntimeGateway` 按指数退避自动重连
- **内核关闭移出主线程**：`closeService` 内含 `FreeOSMemory`，放在主线程有 ANR 风险；
  改用独立于 `onDestroy` 的 shutdown 作用域，确保关闭流程不被取消
- **内核启动失败不再泄漏 gRPC Server**：启动异常时关闭已拉起的命令服务
- **内核重载不再阻塞 Go 线程**：改为返回缓存配置 + 后台异步刷新
- **依赖方向彻底收敛**：领域模型下沉到 `v2/domain`（设置 / 订阅 / VPN 状态 / 更新），
  `v2/domain` 与 `v2/feature` 均 0 处越界依赖；应用更新模块归入 `v2/data/update`
- **暗色模式文本可读性**：次要 / 禁用文本色档位对齐桌面端 tokens.css，
  修复深色下仪表盘标签近乎不可见
- **移除无消费方的 `updatedAt`**：该字段破坏 StateFlow 相等性合并，导致每秒全量重组
- 页面状态改用 `collectAsStateWithLifecycle`；列表补稳定 key；
  三个新页面补齐错误提示；设置页展示内核版本

### 修复：内核升级后配置格式未同步迁移（启动即失败）

升级到 sing-box 1.14.4 后，旧配置使用了 1.11.0 弃用 / 1.13.0 移除的字段，
内核直接拒绝解码：

```
start or reload service: decode config: inbounds[1]: legacy inbound fields are
deprecated in sing-box 1.11.0 and removed in sing-box 1.13.0
```

处理：

- **模板与兜底配置**移除入站 legacy 字段（`sniff` / `sniff_override_destination` /
  `sniff_timeout` / `domain_strategy` / `udp_disable_domain_unmapping`），
  嗅探能力改由 `route.rules` 的 `action: sniff` 承担
- **新增 `ConfigMigrator`**：集中迁移历史配置——
  入站 legacy 字段、DNS `independent_cache`（1.14 弃用）、
  legacy DNS 服务器格式（`address` → `type` + `server`）、`experimental.clash_api`；
  已持久化在设备上的旧配置会在加载时自动升级，无需用户手动重置
- **Clash 格式订阅转换**补齐 sniff / hijack-dns 路由动作
- **启动前用内核自身校验配置**（`Libbox.checkConfig`），
  配置有问题时直接把精确原因显示给用户
- **移除已失效的「独立 DNS 缓存」设置项**（内核已移除该区分）
- 新增 `SingBoxConfigOracleTest`：用真实 sing-box CLI 执行 `check`，
  对模板配置、默认配置、迁移后的历史配置、Clash 转换结果做内核级校验
  （设置 `SING_BOX_CLI` 指向可执行文件后启用，未提供时自动跳过）

### 修复：进程内存持续增长（连接表无界增长 + 高频流常驻订阅）

**现象**：连接后首页「内核 · 内存」持续上涨（最高到 600MB 后回落到 500MB 再涨），
停止并重新连接后会从上次的高水位继续增长。

**根因**（经上游源码与本地复现实验定位）：

1. **连接表无界增长**：内核只推送「连接关闭」事件，不推送「从列表删除」事件。
   App 侧连接表从不淘汰已关闭连接 → 表随会话持续增长；
   同时**每个事件批次都整表拷贝**生成快照，分配量随连接数线性增长，
   于是垃圾产生速率不断加快 —— 正好对应「越涨越快」的锯齿曲线。
2. **高频流常驻订阅**：`CommandConnections` / `CommandLog` 始终订阅。
   libbox 的 gomobile 绑定中，每个 `next()` 返回的都是持有 Go 引用的 **Java 代理**，
   只有 ART GC 回收代理后 Go 侧引用才释放；ART GC 不频繁运行会让代理持续堆积，
   进一步推高进程 RSS。
3. **指标本身有误导性**：内核上报的 `memory` 在 Linux 上取自 `/proc/self/statm` 的
   **RSS（进程常驻内存）**，包含 ART 堆、Go 堆与 native 代码页，
   并不是「内核占了多少内存」。

**修复**：

- **★ 根因：`statusInterval` 单位错误（毫秒 vs 纳秒）**
  `daemon.SubscribeStatusRequest.Interval` 是 `time.Duration`（**纳秒**），
  原先传 `1000L` 被解释为 **1 微秒**，内核遂以 **每秒约 29,500 次**的频率
  推送状态与连接快照 —— 实测对比：

  | `statusInterval` | 3 秒内推送条数 | 速率 |
  | --- | --- | --- |
  | `1000` ns（原值） | 88,550 | ~29,500 次/秒 |
  | `1_000_000_000` ns（修正值） | 3 | 1 次/秒 |

  这同时解释了两个现象：每秒 2.95 万个 protobuf + gomobile 代理对象把 Go 堆与
  ART 堆双双撑大（RSS 592MB 且锯齿上涨）；以及首页每秒重组 2.95 万次导致**卡顿**。
  修正为 `1_000_000_000L`（1 秒）。
- **★ 修复内存诊断自身引入的卡顿**
  `/proc/self/smaps_rollup` 需要遍历整个进程页表，592MB 的进程下是数十~上百毫秒
  的阻塞；原先在主线程每秒读一次，等于在 2.95 万次/秒的重组中反复读页表。
  现改为：IO 线程采样、间隔放宽到 5 秒、PSS 单独按需采集。
- 新增 `ConnectionTable`：已关闭连接保留 60 秒后淘汰；快照发布按秒节流；
  结构性变化（新增 / 关闭 / 淘汰）立即生效
- `CommandConnections` / `CommandLog` 拆为**独立客户端按需订阅**：
  仅在对应页面可见时订阅，离开页面立即退订（退订在 `NonCancellable` 中完成）
- 首页「内核」卡片改为展示**内存分解**：常驻(RSS) / Java 堆 / native 堆 /
  内核上报值 / 协程 / 连接，便于定位增长发生在哪一侧
- 代理组与出站在内容未变化时不更新状态，避免无谓的重组

**排查手段**（保留在仓库中）：

- 用上游同 revision 的 sing-box CLI 复现：全量订阅 6 路流并持续压测，
  确认 **Go 侧 RSS 稳定在 48MB、协程数恒定**，从而排除内核泄漏
- `SingBoxConfigOracleTest` 继续作为配置格式的内核级校验

### 修复：内存指标口径错误（首页「内核」卡片显示 300MB 实为进程常驻）

**现象**：连接内核后首页「内核」卡片显示 300MB~600MB，看似内核内存暴涨。

**实测结论**（小米 14 Ultra / Android 16，正式包）：

| 指标 | 内核未启动 | 内核已启动 |
| --- | --- | --- |
| 卡片显示的「内核上报」= 进程 RSS | 240 MB | 271 MB |
| TOTAL RSS | 322 MB | 352 MB |
| TOTAL PSS（真实分摊占用） | 155 MB | 184 MB |
| Private Dirty（应用独占） | ~28 MB | ~30 MB |
| Unknown（≈Go 运行时堆） | 7.2 MB | 18.9 MB |

352MB RSS 中，**约 220MB 与内核无关**：`libbox.so` 代码页 ~50MB、APK/dex/资源映射 ~104MB、
EGL 图形缓冲 74MB。内核真正占用只有 ~18MB。

**根因**：

1. 上游 `daemon.StartedService.readStatus()` 用 `memory.Total()` 上报内存，
   而 `common/memory/memory_linux.go` 的实现是读 `/proc/self/statm` 的 **RSS**，
   即**整个 App 进程的常驻内存**（含 ART 堆、Go 堆、libbox.so 代码页、EGL 缓冲）。
   App 侧把它标注成「内核上报」，与旁边的「常驻」读同一份 `/proc/self/statm`，
   因此**两列永远相等**，极易被误解为内核泄漏。
2. RSS 会把**与其他进程共享的文件页**计入，无法反映真实占用。

**修复**：

- 首页「内核」卡片改为展示 **内核内存 / 协程 / 连接** 三项：
  - **内核内存**：`/proc/self/smaps` 中 `[anon: Go: ...]` 映射（Go 运行时堆、
    immortal metadata 等）的 RSS 合计——Android 上 Go 运行时映射由
    `prctl(PR_SET_VMA_ANON_NAME)` 命名，可精确归给内核，实测与 `dumpsys` 的
    `Unknown` 分类一致
  - **协程 / 连接**：取自内核 `Status`；未连接时显示 `-`
- 卡片副标题补充进程级口径：`进程 RSS xxx · PSS xxx`
- `smaps_rollup` 改为解析 `smaps`（一次读取同时得到 PSS / Private Dirty /
  Go 堆），并降频到 15 秒一次，避免频繁遍历进程页表造成卡顿

### 修复：命令通道被内核判死（首页协程 / 连接 / 流量恒为 0）

**现象**：连接成功、VPN 可用，但首页「协程 / 连接」始终为 0，流量也不刷新。

**根因**：App 订阅了 `Libbox.CommandClashMode`，但本项目配置使用 sing-box 1.14
原生的 `route.rules[].clash_mode`，**并未开启 `experimental.clash_api`**，
内核的 `GetClashModeStatus` 恒返回 `invalid argument`；而上游客户端只容忍
`NotFound`，其余错误一律回调 `Disconnected()`，导致**整条命令通道被判死**。

**修复**：

- 移除 `CommandClashMode` 订阅（命令通道只保留 Status / Group / Outbounds）
- `connect()` 在通道处于 `DISCONNECTED_ERROR` 时**重建客户端**，
  修复了原先「客户端非空即提前返回」导致重连逻辑空转、无法自愈的问题
- 代理模式改为**改配置 + 重载内核**实现（见下条）

### 修复：代理模式切换无效 / 点击无反应

- **切换无效**：原先通过内核命令通道的 `SetClashMode` 下发模式，但该接口同样依赖
  `experimental.clash_api`，必然失败。新增 `ConfigSettingsApplier.applyClashMode`：
  在配置层把 `clash_mode` 规则固化（规则模式删除两条 `clash_mode` 规则；
  全局 / 直连模式保留对应规则并去掉 `clash_mode` 字段），再重载内核生效。
  磁盘上始终保存**模板态**配置（保留 `clash_mode` 规则），
  只有启动内核时才按当前模式固化，否则规则一旦固化就无法再切回。
- **点击无反应**：模式流原先用 `snapshotFlow { SettingsRepository.load(...) }`，
  而 SharedPreferences 不是 Compose 快照状态，`snapshotFlow` 不会重新发射，
  选中态永远不刷新。改为 `MutableStateFlow`，切换后立即更新 UI。

### 修复：多订阅切换不生效

订阅启用 / 同步后会重新生成 `singbox.json`，但**没有重载正在运行的内核**，
必须断开重连才会生效。现在订阅变更成功后会自动重载内核（未连接时跳过，
下次连接直接使用新配置）。

### 修复：订阅 URL 被输入法自动大写导致地址失效

输入法默认把 `/` 后的首字母大写（`https://host/api/...` → `https://host/API/...`），
订阅地址大小写敏感会直接失效。`AppTextField` 新增 `keyboardCapitalization` /
`autoCorrect` 参数，URL 输入框改为 `KeyboardCapitalization.None` + 关闭自动纠错。

### 修复：内核实例反复重建导致 Go 堆单调增长

**实测**（模拟器，连续 8 次「连接 → 断开」）：

```
cycle 1: Go 堆 13.5MB → cycle 8: Go 堆 27.0MB   （每次净增 2~3MB，停止后不回落）
```

**修复**：`SingBoxEngine` 原先每次断开都会 `closeService()` + `close()`，
把整个 `CommandServer`（gRPC daemon）销毁，下次连接再重建，daemon 侧的全局状态
反复重建、旧实例引用无法回收。现改为：

- `stop()`：只关闭 box service，**保留命令服务**以便下次连接复用
- `release()`：彻底释放（服务销毁时调用）
- `start()`：命令服务已存在时直接 `startOrReloadService` 重载内核

### 新增：诊断构建变体

新增 `diagnostic` 构建类型（debuggable + 沿用发布签名，可直接覆盖安装并保留用户数据），
内置 `DiagnosticReceiver`：通过广播驱动连接 / 断开 / 内存采样，便于在真机上
自动化复现与取证；同时支持 `-PabiIncludes=arm64-v8a` 缩短本地构建时间。

### 文档

- 新增 `docs/ARCHITECTURE_V3.md`，说明内核接口迁移、架构收敛与设计系统方案

---

## v1.2.1 (2026-02-23)

### 主要修复

#### 应用内更新流程补全（下载通知安装）
- 修复设置页“检查更新”仅触发检查、未打通下载与安装链路的问题
- 新增设置页“应用更新”状态卡片，展示检查/发现更新/下载中/下载完成/失败状态
- 新增“下载更新包”与“安装已下载包”入口，形成完整用户闭环
- 下载完成后不再自动弹安装，改为发送系统通知，用户点击通知安装
- 增加下载进度通知与下载失败通知

### 文档

- 新增 `docs/IN_APP_UPDATE.md`，说明当前更新流程、状态机与关键代码位置

---

## v1.2.0 (2025-12-24)

### 主要更新

#### 应用自动更新功能
- **完整的应用内更新系统**：支持检查更新、下载和安装新版本
- **GitHub Release 集成**：自动从 GitHub Releases 获取最新版本信息
- **智能架构匹配**：根据设备 CPU 架构自动选择对应的 APK 文件
- **断点续传支持**：下载支持进度显示和断点续传
- **Wi-Fi 下载限制**：可设置仅在 Wi-Fi 环境下下载更新
- **安装权限管理**：自动检测并引导用户开启安装权限

#### 设置页面重构
- **代码简化**：移除冗余的状态变量，统一使用 `AppSettings` 数据类
- **新增更新卡片**：在设置页面集成应用更新功能入口
- **UI 优化**：使用 `UpdateSection` 组件展示版本信息和更新状态

### 技术改进

#### 更新模块架构 (`update/`)
- **UpdateManager**: 统一更新流程管理器（检查、下载、安装）
- **GitHubReleaseChecker**: GitHub API 版本检查器，支持加速代理
- **ApkDownloader**: APK 文件下载器，支持进度回调
- **AppUpdateInstaller**: APK 安装器，适配不同 Android 版本
- **UpdateStore**: 更新状态管理，使用 StateFlow 响应式更新
- **UpdateSection.kt**: 更新 UI 组件，包含版本显示、更新对话框等

#### 依赖配置
- 新增 `kotlinx-serialization-json` 用于 JSON 解析
- 配置 `kotlin-parcelize` 插件支持 Parcelable
- 添加 `REQUEST_INSTALL_PACKAGES` 权限用于 APK 安装
- 配置 FileProvider 用于安全分享 APK 文件

#### 其他改进
- 优化 GitHub Actions 构建流程配置
- 修复签名步骤的 Base64 解码问题

---

## v1.1.0 (2025-12-23)

### 主要更新

#### Clash API 深度集成
- 使用 Clash REST API 替代原有 CommandClient，实现更稳定的核心通信
- 新增实时流量统计功能，动态展示上传/下载速度
- 新增连接诊断页面，支持查看实时连接详情

#### 本地节点支持
- 支持导入本地节点列表文件
- 本地订阅与远程订阅统一管理
- 支持手动编辑和维护自定义节点

#### 界面体验优化
- 重构应用导航架构，采用底部导航栏设计
- 完善设计系统组件，统一 Material 3 风格
- 优化配色方案和排版，提升视觉效果

#### 设置功能增强
- 新增设置持久化功能，配置自动保存
- 完善设置页面，支持更多自定义选项

### 技术改进
- 重构核心状态管理机制
- 优化订阅管理数据流
- 改进错误处理和用户反馈

---

## 首次发布 (v1.0.0)

Sing Box Windows 是一个基于 sing-box 核心的 Android VPN 客户端应用，提供现代化的网络代理管理功能。

### 核心功能

- 高性能 VPN 服务：基于 sing-box 核心，支持多种代理协议
- 现代化界面：采用 Jetpack Compose 构建的 Material 3 设计风格界面
- 订阅管理：支持添加、编辑和管理多个订阅链接
- 节点测速：自动测试节点延迟，帮助选择最优节点
- 流量统计：实时显示网络连接和流量使用情况
- 智能分组：自动组织和管理节点分组

### 技术特性

- **多架构支持**：支持 ARM64 和 ARMv7 架构设备
- **协议支持**：兼容 VLESS、Shadowsocks 等主流代理协议
- **Base64 解码**：自动处理 Base64 编码的订阅内容
- **GZIP 解压**：支持压缩格式的订阅文件
- **通知管理**：完善的前台通知和权限处理
- **状态管理**：稳定的 VPN 连接状态监控

### v1.0.0 功能亮点

1. **VPN 服务优化**
   - 优化 VPN 启动逻辑
   - 增强状态管理和错误处理
   - 改进权限请求流程

2. **用户界面改进**
   - 实现节点测速功能界面
   - 优化颜色主题和排版风格
   - 添加应用图标资源

3. **订阅管理重构**
   - 重构订阅管理与 UI 交互逻辑
   - 支持编辑现有订阅
   - 改进订阅同步错误处理

4. **核心功能增强**
   - 添加核心状态管理
   - 实现流量统计展示
   - 支持 VLESS 和 Shadowsocks 协议

5. **架构优化**
   - 实现 ABI 拆分配置
   - 生成独立 APK 文件
   - 优化构建配置

### 安装说明

本项目支持多种架构的 APK 文件：

- `app-arm64-v8a-release.apk`：适用于 64 位 ARM 架构设备（推荐）
- `app-armeabi-v7a-release.apk`：适用于 32 位 ARM 架构设备

### 使用要求

- Android 5.0 (API Level 29) 或更高版本
- 需要 VPN 权限和网络访问权限

### 开发信息

- 开发语言：Kotlin + Jetpack Compose
- 核心库：libbox (sing-box)
- 构建工具：Gradle with Kotlin DSL
- UI 框架：Material Design 3

### 已知问题

- 部分旧设备可能需要手动授予通知权限
- 首次连接时需要用户授权 VPN 权限

### 贡献指南

请参考项目中的 [AGENTS.md](AGENTS.md) 文件了解项目结构、编码规范和贡献流程。

---

**注意**：本应用仅供学习和研究使用，请遵守当地法律法规。
