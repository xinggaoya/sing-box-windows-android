# 应用内自动更新说明

## 目标

在应用内实现完整的更新闭环：

1. 检查 GitHub Release 最新版本。
2. 按设备 ABI 下载对应 APK。
3. 下载过程中显示进度。
4. 下载完成后发送系统通知，用户点击通知触发安装。

## 当前行为（v2）

入口在设置页“应用更新”卡片。

1. 点击“检查更新”后进入检查状态。
2. 发现新版本后显示版本信息与“下载更新包”按钮。
3. 下载中显示进度（页面 + 系统通知）。
4. 下载完成后进入“下载完成”状态，并发送“点击安装”通知。
5. 用户可点击系统通知安装；若通知不可见，可在设置页点击“安装已下载包”。

## 关键设计

1. 不再自动拉起安装器，避免打断用户操作。
2. 安装动作由用户触发（通知点击或设置页按钮）。
3. 通过 `FileProvider` 安全分享 APK 给系统安装器。
4. Android 13+ 若未授予通知权限，仍可通过设置页“安装已下载包”兜底安装。

## 状态机

`UpdateState` 主要状态：

1. `Idle`：空闲。
2. `Checking`：检查中。
3. `UpToDate`：已是最新版本。
4. `UpdateAvailable`：发现可更新版本。
5. `Downloading`：下载中（带进度）。
6. `ReadyToInstall`：下载完成，等待用户安装。
7. `Failed`：流程失败。

## 主要代码位置

1. 更新流程管理：`app/src/main/java/cn/moncn/sing_box_windows/update/UpdateManager.kt`
2. 更新通知：`app/src/main/java/cn/moncn/sing_box_windows/update/UpdateNotificationHelper.kt`
3. 安装器：`app/src/main/java/cn/moncn/sing_box_windows/update/AppUpdateInstaller.kt`
4. 设置页状态与交互：`app/src/main/java/cn/moncn/sing_box_windows/v2/feature/settings/SettingsViewModel.kt`
5. 设置页 UI：`app/src/main/java/cn/moncn/sing_box_windows/v2/feature/settings/SettingsScreenV2.kt`

## 验证建议

1. 检查更新后，确认能显示 `UpdateAvailable` 状态。
2. 下载时，确认页面与通知进度同步更新。
3. 下载完成后，确认收到“点击安装”通知。
4. 点击通知后，确认系统安装界面可打开。
5. 关闭通知权限后，确认“安装已下载包”仍可触发安装流程。
