# Aurora Glass 设计系统（Android 端）

> 适用于 `cn.moncn.sing_box_windows` 的移动端界面规范。
> 品牌延续桌面端 `sing-box-windows` 的 Indigo 主色，交互与版式按移动端重新设计。

---

## 1. 设计理念

| 原则 | 落地方式 |
| --- | --- |
| **单一事实来源** | `AppColors` 定义全部语义色，Material3 `colorScheme` 由其派生，组件不再各自取色 |
| **纯色分层，不用脏渐变** | 页面底色为纯色 + 顶部品牌光晕；旧版 `bgBase→bgSurface2→bgBase` 纵向渐变已移除 |
| **层级靠描边而非投影** | 亮色用 1px 描边区分层级，暗色下描边承担全部层级表达，投影只在底栏使用 |
| **中英文分离的字体策略** | 标题/正文使用系统字体（保证中文字重与行高一致），品牌字体仅用于纯拉丁/数字场景 |
| **互斥选项用分段控件** | 运行模式、连接筛选、日志级别、主题模式统一使用 `AppSegmentedControl`，不再散落 FilterChip |
| **状态不只靠颜色** | 持续状态（已连接/测速中）额外附加圆点，兼顾色觉障碍用户 |

---

## 2. 令牌体系

### 2.1 色彩（`ui/theme/AppColors.kt`）

三层结构，与桌面端 `tokens.css` 对齐：

```
AppPalette  →  AppColors  →  MaterialTheme.colorScheme
(原始色阶)     (语义令牌)      (M3 原生组件)
```

| 语义组 | 字段 | 亮色 | 暗色 |
| --- | --- | --- | --- |
| 品牌 | `primary` | `#4F46E5` | `#818CF8` |
| | `onPrimary` | `#FFFFFF` | `#14162B` |
| | `primaryContainer` | `#EEF2FF` | `#312E81` |
| 背景 | `background` | `#F4F5FA` | `#0A0E1A` |
| | `surface` | `#FFFFFF` | `#121828` |
| | `surfaceMuted` | `#F1F3F9` | `#1A2234` |
| | `surfaceSunken` | `#EDEFF6` | `#070B14` |
| | `surfaceElevated` | `#FFFFFF` | `#1A2236` |
| 文本 | `textPrimary` | `#0F172A` | `#F1F5F9` |
| | `textSecondary` | `#475569` | `#CBD5E1` |
| | `textTertiary` | `#64748B` | `#94A3B8` |
| 描边 | `border` / `borderStrong` / `borderSubtle` | `#E2E8F0` / `#CBD5E1` / `#EFF2F7` | `#232C3D` / `#33415A` / `#1A2130` |

状态色统一封装为 `StatusPalette(content, container, onContainer, border)`，
一次定义即可同时得到「强调色 / 浅底 / 底上文字 / 边框」，杜绝底色与文字色不匹配。

**对比度守卫**：`AppColorsTest` 会在每次构建时校验

- 正文三级文字在卡片上的对比度 ≥ 4.5:1（WCAG AA）
- 品牌色与状态色在卡片上 ≥ 3:1（图形 AA）
- 状态色在自身浅底上 ≥ 4.5:1
- 明暗两套令牌字段完全一致（防止只改了一半）

### 2.2 排版（`ui/theme/AppTypography.kt`）

| 层级 | 字号 | 字重 | 用途 |
| --- | --- | --- | --- |
| display | 30 / 24 / 20 | SemiBold | 品牌字、Hero 数值 |
| headline | 20 / 17 / 15 | SemiBold / Medium | 页面大标题 |
| title | 17 / 15 / 14 | SemiBold / Medium | 卡片标题、列表主文案 |
| body | 15 / 14 / 12 | Regular | 说明文、正文、辅助文案 |
| label | 14 / 12 / 11 | Medium | 按钮、标签、微标签 |

- `BrandFont`（Space Grotesk）仅用于品牌字样与指标数值，并开启 `tnum` 等宽数字特性，数值刷新时不抖动。
- `MonoFont` 用于日志与配置片段。

### 2.3 间距 / 圆角 / 动效（`ui/theme/AppDimens.kt`、`AppMotion.kt`）

- 间距阶梯 `AppSpace`：4 / 8 / 12 / 16 / 20 / 24 / 32 / 40 / 48
- 页面常量 `AppLayout`：`gutter = 20dp`（左右边距）、`pagePadding = 20dp`、`sectionGap = 16dp`、`cardPadding = 16dp`、`bottomBarHeight = 64dp`
- 圆角 `AppRadius`：6 / 10 / 14 / 18 / 22 / 28 / pill
- 动效 `AppMotion`：微交互 120ms、常规 220ms、页面转场 300ms、退出 160ms（退出更快更利落）

---

## 3. 组件清单（`ui/components/`）

| 组件 | 职责 |
| --- | --- |
| `AppBackground` | 页面背景：纯色底 + 顶部品牌光晕 |
| `AppPage` / `appPagePadding()` | 统一页面留白（状态栏 / 底栏 / 手势条 / 左右边距） |
| `AppTopBar` | 大标题页头，支持返回按钮与右侧操作区 |
| `AppCard` / `AppSectionCard` / `GradientCard` | 常规卡片 / 带标题分组卡片 / 品牌渐变卡片 |
| `AppListItem` / `SelectionIndicator` | 列表行与单选指示器 |
| `AppMetricTile` / `MetricRow` | 指标块与指标行 |
| `AppStatusChip` / `StatusTone` | 语义状态标签（支持状态圆点） |
| `AppSegmentedControl` | 互斥选项分段控件 |
| `PrimaryButton` / `SecondaryButton` / `TertiaryButton` / `IconActionButton` / `IconTileButton` | 按钮体系 |
| `SettingsRow` / `SwitchRow` / `RowDivider` | 设置项行组件 |
| `AppTextField` / `AppDropdownField` / `AppProgressBar` | 表单与进度 |
| `AppAlertDialog` / `AppInlineBanner` | 对话框（错误 / 表单）与非阻断提示条（成功反馈） |
| `AppBottomBar` | 底部导航（含手势条适配与选中态语义） |
| `EmptyState` | 空状态 |

> 组件参数顺序约定：可组合插槽（`trailing` / `content`）统一放在最后，
> 便于以尾随 lambda 形式书写。

---

## 4. 页面结构规范

每个页面遵循同一骨架：

```
AppBackground
└── LazyColumn(contentPadding = appPagePadding(), verticalArrangement = AppPageArrangement)
    ├── item { AppTopBar(标题, 副标题, 操作) }   ← 大标题
    ├── item { 分区卡片 }                        ← AppSectionCard
    ├── item { 指标行 }                          ← MetricRow
    └── items { 列表项 }                        ← 稳定 key
```

- 页面左右边距、顶部起始、底部收尾全部来自 `appPagePadding()`，页面之间不允许自行写 padding。
- 区块间距固定 `AppPageArrangement`（16dp）。
- 空状态使用 `EmptyState` + `fillParentMaxHeight(0.7f)`，占据可视区主要位置。

### 反馈层级

- **错误** → 模态对话框（`AppAlertDialog`），阻断性强，必须被明确看到。
- **成功 / 结果** → 顶部非阻断提示条（`AppInlineBanner`），可一键关闭，不打断当前操作。
- **加载** → 订阅页使用居中进度指示器；按钮内联进度由 `PrimaryButton(loading = true)` 承担。

### 响应式

- 所有页面内容受 `AppLayout.contentMaxWidth = 560dp` 约束并水平居中：
  手机竖屏不生效（保持窄屏密度），平板 / 横屏 / 分屏下限制行长，避免一行文字横跨整屏。
- 底部导航始终横跨全宽，保证在宽屏下的可达性。

### 各页面要点

| 页面 | 结构 |
| --- | --- |
| 首页 | 页头（品牌字样）→ **Hero 连接卡**（状态 + 主按钮 + 呼吸光晕）→ 实时速率 → 累计流量 → 运行模式（分段控件）→ 内核指标 → 工具入口 |
| 代理 | 页头（分组/节点统计）→ 分组卡片（当前选择胶囊 + 展开箭头旋转）→ 节点行（单选点 + 延迟 + 测速） |
| 连接 | 页头 → 活跃/上行/下行指标 → 筛选分段控件 → 连接行（目标、协议、规则、流量、关闭） |
| 订阅 | 页头（右上「+」）→ 订阅卡片（名称、URL、启用状态、同步时间、同步/编辑/删除/启用） |
| 设置 | 页头 → **外观**（主题分段控件）→ DNS → TUN 隧道 → 保存/检查更新 → 更新卡片 |
| 日志 | 页头（返回 + 清空）→ 级别分段控件 → 终端面板（等宽字体、级别着色）→ 自动滚动开关 |
| 出站 | 页头（返回 + 全部测速）→ 指标 → 出站卡片（延迟状态 + 单项测速） |

---

## 5. 导航与交互

- **底部导航**：5 个顶级页面（首页 / 代理 / 连接 / 订阅 / 设置），图标在上文字在下；
  选中态为品牌色 + 指示器底色，并通过 `semantics { selected = true }` 暴露给无障碍服务。
- **二级页面**（日志 / 出站）不显示底部导航，页头提供返回按钮，返回栈行为与系统返回键一致。
- **页面转场**：`fadeIn(220ms)` / `fadeOut(160ms)`，避免生硬的整屏替换。
- **底部导航悬浮**于内容之上，页面通过 `LocalBottomBarPadding` 预留空间；
  `navigationBarsPadding()` 处理全面屏手势条，不会再出现底栏被遮挡的问题。

---

## 6. 无障碍

- 所有正文文字对比度 ≥ 4.5:1，图形/图标 ≥ 3:1（由 `AppColorsTest` 自动校验）。
- 触控区域不小于 48dp；图标按钮视觉尺寸 40dp，触控区域由 `IconButton` 保证。
- 状态不单独依赖颜色：持续状态附加圆点，图标按钮均提供 `contentDescription`。
- 分段控件使用 `Role.RadioButton` 并暴露 `selected` 语义；底部导航项使用 `Role.Tab`。

---

## 7. 主题模式

- `ThemeMode`：`SYSTEM`（跟随系统）/ `LIGHT` / `DARK`。
- 偏好保存在 `ThemeStore`（独立于内核配置，不会写进 sing-box 配置文件）。
- 切换后由 `RefactorMainActivity` 收集 `StateFlow` 立即重绘，**无需重启**。
- 外观偏好属于 UI 层，不参与设置页「保存并应用」，也不触发「重新连接后生效」提示。

---

## 8. 视觉回归（无需真机）

项目内置了基于 **Robolectric 原生图形模式**的界面截图能力：在 JVM 上真实渲染 Compose 界面并导出 PNG，
用于逐页目检布局、留白、对比度与组件状态，不必依赖真机或模拟器。

```bash
# 生成全部页面在浅色 / 深色主题下的截图
./gradlew :app:testDebugUnitTest --tests "*ScreenshotTest*"

# 产物目录
app/build/screenshots/01_home_light.png ... 14_outbounds_dark.png
```

| 文件 | 内容 |
| --- | --- |
| `ScreenshotHarness` | 渲染基类：主题切换、底部导航装配、PNG 导出 |
| `ScreenshotTest` | 7 个页面 × 2 套主题 = 14 张截图，内置各页面样本数据 |
| `AppColorsTest` | 对比度 / 令牌结构守卫（纯单元测试） |
| `AppComponentsTest` | 分段控件、底部导航、状态标签、禁用按钮的交互断言 |
| `ThemeStoreTest` | 主题偏好持久化（重启后仍保持） |

> 实现要点：`captureToImage()` 依赖 `PixelCopy`，Robolectric 不支持，
> 因此改用「`decorView.draw(Canvas(bitmap))`」在原生图形模式下取真实像素。
>
> 全部测试均在 JVM 上运行（`./gradlew :app:testDebugUnitTest`），无需真机 / 模拟器。

