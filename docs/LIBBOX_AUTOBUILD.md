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

## 健壮性说明（v1.3.0 修复）

脚本在真实环境中暴露并修复了以下问题，均已加入回归保护：

| 问题 | 现象 | 修复 |
| --- | --- | --- |
| `GOBIN` 未绑定 | 宿主机设置了 `GOBIN`（如 mise/asdf 的 Go 安装）时，`go install` 装到 `GOBIN` 而非 `$GOPATH/bin`，上游报 `missing gomobile installation` | `setup_go` 中显式 `export GOBIN="${GOPATH}/bin"` |
| `go` shim 覆盖 `JAVA_HOME` | mise/asdf 的 `go` 是 shim，会重写子进程环境，导致上游 `checkJavaVersion` 判定非 OpenJDK 17 而中止 | 解析真实 `GOROOT` 并前置到 `PATH`，绕过 shim |
| 临时目录空间不足 | Go 默认把中间产物放在 `$TMPDIR`（部分环境是容量很小的 tmpfs），多 ABI 编译会耗尽磁盘 | 新增 `GOTMPDIR="${WORKDIR}/gotmp"`，统一收敛到工作目录 |
| 源码目录不可复用 | 指定 `--workdir` 指向已有检出时 `git clone` 失败，脚本无法重跑 | `clone_sing_box` 优先复用已有工作区（fetch + checkout），保证幂等 |

此外：`setup_java` 在复用系统 `JAVA_HOME` 时显式 `export`，避免依赖调用方环境。

## 环境前提

脚本会自动下载临时 JDK/Go，但仍需要 Android SDK（可被脚本自动检测）。  
若未安装 NDK 且本机有 `sdkmanager`，脚本会自动尝试安装指定 NDK 版本。

## CI 自动化

仓库仅保留发布工作流 `.github/workflows/release.yml`：

1. 通过推送版本标签（`v*`）触发发布构建。
2. 构建 APK 前会自动执行 `scripts/update-libbox.sh` 拉取并编译最新内核。
3. 自动产出已签名 Release APK 并创建 GitHub Release。

## 版本追踪

每次构建会更新 `app/libs/libbox.version.json`，记录：

1. 上游仓库/标签/提交；
2. 构建时间；
3. Go/Java/gomobile/NDK 版本。

建议在评审时同时检查该文件，确保二进制来源可追溯。
