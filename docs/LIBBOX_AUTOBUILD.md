# libbox 自动拉取与构建

## 目标

通过 `scripts/update-libbox.sh` 自动完成以下流程：

1. 拉取 `SagerNet/sing-box` 最新 Release（或指定版本）。
2. 自动准备构建工具链（优先复用系统 Java/Go，不满足时下载临时版本）。
3. 调用 sing-box 官方构建入口生成 `libbox.aar`。
4. 写回本仓库 `app/libs/libbox.aar`，并生成构建元数据 `app/libs/libbox.version.json`。
5. 默认清理临时目录，避免长期污染本机环境。

## 本地使用

```bash
# 1) 构建最新 Release
./scripts/update-libbox.sh

# 2) 构建指定版本
./scripts/update-libbox.sh --version v1.12.0

# 3) 同时输出 legacy AAR
./scripts/update-libbox.sh --include-legacy

# 4) 仅查看解析结果（不实际构建）
./scripts/update-libbox.sh --dry-run
```

## 参数说明

```text
--version <tag>         指定 tag（如 v1.12.0 或 1.12.0）
--ref <git-ref>         指定分支/标签/提交，优先级高于 --version
--output <path>         主 AAR 输出路径（默认 app/libs/libbox.aar）
--metadata <path>       元数据 JSON 路径（默认 app/libs/libbox.version.json）
--include-legacy        同时输出 libbox-legacy.aar
--legacy-output <path>  legacy AAR 输出路径
--android-sdk <path>    指定 Android SDK 路径
--ndk-version <ver>     指定 NDK 版本（默认 28.0.13004108）
--workdir <path>        指定工作目录（默认保留该目录）
--keep-workdir          保留自动创建的临时目录
--dry-run               仅打印计划
```

## 环境前提

脚本会自动下载临时 JDK/Go，但仍需要 Android SDK（可被脚本自动检测）。  
若未安装 NDK 且本机有 `sdkmanager`，脚本会自动尝试安装指定 NDK 版本。

## CI 自动化

仓库新增工作流 `.github/workflows/update-libbox.yml`：

1. 支持手动触发 `workflow_dispatch`（可指定版本）。
2. 每周定时触发（北京时间周一上午，对应 UTC 周一 02:30）。
3. 构建完成后上传 AAR Artifact。
4. 可自动创建更新 PR（包含 `libbox.aar` 和元数据）。

## 版本追踪

每次构建会更新 `app/libs/libbox.version.json`，记录：

1. 上游仓库/标签/提交；
2. 构建时间；
3. Go/Java/gomobile/NDK 版本。

建议在评审时同时检查该文件，确保二进制来源可追溯。
