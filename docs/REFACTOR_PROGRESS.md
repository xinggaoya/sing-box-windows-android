# 重构进度（完成）

更新时间：2026-02-22

## 已完成（M0-M3）

1. v2 架构基础（Intent/State/Effect + MVI 基类）。
2. v2 依赖装配入口（`V2Container` + `V2Graph`）。
3. v2 运行时与模式控制链路（VPN 状态、核心状态、模式切换）。
4. v2 五大页面全部迁移：
   1. 首页（连接控制、速度、模式切换）；
   2. 订阅（新增/编辑/删除/启用/同步/本地导入）；
   3. 节点（分组、切换、测速）；
   4. 设置（参数保存应用、更新组件接入）；
   5. 诊断（Clash API + WebSocket 状态）。
5. `RefactorMainActivity` 已切换为默认启动入口（LAUNCHER）。
6. 自动内核链路已落地（`update-libbox.sh` + CI 工作流）。

## 验证结果

1. `:app:compileDebugKotlin` 通过（JDK17 环境）。
2. `:app:assembleDebug` 通过（JDK17 环境）。
