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
