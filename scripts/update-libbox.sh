#!/usr/bin/env bash
set -Eeuo pipefail

# 自动拉取 sing-box 最新（或指定）版本并编译 libbox.aar。
# 设计目标：
# 1) 尽量不依赖宿主机工具链（自动下载临时 JDK17/Go）；
# 2) 构建完成后默认清理临时目录；
# 3) 将产物写回 app/libs 供 Android 项目直接引用。

SCRIPT_NAME="$(basename "$0")"
SCRIPT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd -- "${SCRIPT_DIR}/.." && pwd)"

SING_BOX_REPO="https://github.com/SagerNet/sing-box.git"
DEFAULT_OUTPUT_AAR="${REPO_ROOT}/app/libs/libbox.aar"
DEFAULT_LEGACY_AAR="${REPO_ROOT}/app/libs/libbox-legacy.aar"
DEFAULT_METADATA_FILE="${REPO_ROOT}/app/libs/libbox.version.json"
DEFAULT_NDK_VERSION="28.0.13004108"
DEFAULT_GOMOBILE_VERSION="v0.1.11"

OUTPUT_AAR="$DEFAULT_OUTPUT_AAR"
OUTPUT_LEGACY_AAR="$DEFAULT_LEGACY_AAR"
METADATA_FILE="$DEFAULT_METADATA_FILE"
TARGET_VERSION=""
TARGET_REF=""
ANDROID_SDK_OVERRIDE=""
ANDROID_NDK_VERSION="$DEFAULT_NDK_VERSION"
WORKDIR=""
KEEP_WORKDIR=0
INCLUDE_LEGACY=0
DRY_RUN=0

SING_BOX_DIR=""
GOMOBILE_VERSION="$DEFAULT_GOMOBILE_VERSION"
RESOLVED_TAG=""
RESOLVED_REF=""
SING_BOX_COMMIT=""
SING_BOX_TAG=""
REQUIRED_GO_VERSION=""
JAVA_VERSION_LINE=""
GO_VERSION_LINE=""
ANDROID_SDK_PATH=""
ANDROID_NDK_PATH=""

log() {
    echo "[libbox-build] $*"
}

warn() {
    echo "[libbox-build][WARN] $*" >&2
}

die() {
    echo "[libbox-build][ERROR] $*" >&2
    exit 1
}

usage() {
    cat <<EOF
用法:
  ${SCRIPT_NAME} [选项]

选项:
  --version <tag>         指定 sing-box 标签，例如 v1.12.0 或 1.12.0（默认自动取最新 Release）
  --ref <git-ref>         指定 git ref（分支/标签/提交），优先级高于 --version
  --output <path>         主产物 libbox.aar 输出路径（默认: ${DEFAULT_OUTPUT_AAR}）
  --metadata <path>       元数据 JSON 输出路径（默认: ${DEFAULT_METADATA_FILE}）
  --include-legacy        同时导出 libbox-legacy.aar
  --legacy-output <path>  legacy AAR 输出路径（默认: ${DEFAULT_LEGACY_AAR}）
  --android-sdk <path>    指定 Android SDK 路径（默认自动探测）
  --ndk-version <ver>     期望的 NDK 版本（默认: ${DEFAULT_NDK_VERSION}）
  --workdir <path>        指定工作目录（指定后默认保留目录）
  --keep-workdir          保留自动创建的临时目录，便于排查
  --dry-run               仅打印解析结果，不执行下载与构建
  -h, --help              显示帮助

示例:
  ${SCRIPT_NAME}
  ${SCRIPT_NAME} --version v1.12.0 --include-legacy
  ${SCRIPT_NAME} --ref dev --keep-workdir
EOF
}

ensure_cmd() {
    command -v "$1" >/dev/null 2>&1 || die "缺少命令: $1"
}

normalize_tag() {
    local value="$1"
    if [[ -z "$value" ]]; then
        echo ""
        return
    fi
    if [[ "$value" =~ ^v ]]; then
        echo "$value"
    else
        echo "v${value}"
    fi
}

resolve_latest_release_tag() {
    local api="https://api.github.com/repos/SagerNet/sing-box/releases/latest"
    local tag
    tag="$(
        curl -fsSL \
            -H "Accept: application/vnd.github+json" \
            "$api" | sed -n 's/^[[:space:]]*"tag_name":[[:space:]]*"\([^"]*\)".*/\1/p' | head -n1
    )"
    [[ -n "$tag" ]] || die "无法从 GitHub API 获取最新 tag"
    echo "$tag"
}

detect_host_os() {
    case "$(uname -s)" in
        Linux) echo "linux" ;;
        Darwin) echo "darwin" ;;
        *) die "当前系统不支持自动下载工具链: $(uname -s)" ;;
    esac
}

detect_host_arch_go() {
    case "$(uname -m)" in
        x86_64 | amd64) echo "amd64" ;;
        arm64 | aarch64) echo "arm64" ;;
        *) die "当前架构不支持自动下载 Go: $(uname -m)" ;;
    esac
}

detect_host_arch_java() {
    case "$(uname -m)" in
        x86_64 | amd64) echo "x64" ;;
        arm64 | aarch64) echo "aarch64" ;;
        *) die "当前架构不支持自动下载 JDK: $(uname -m)" ;;
    esac
}

java_is_openjdk17() {
    local java_bin="$1"
    local version_line
    version_line="$("$java_bin" --version 2>/dev/null | head -n1 || true)"
    [[ -n "$version_line" ]] || return 1
    JAVA_VERSION_LINE="$version_line"
    [[ "$version_line" == *"openjdk 17"* ]] || [[ "$version_line" == *"17."* ]]
}

version_gte() {
    local left="$1"
    local right="$2"
    [[ "$(printf '%s\n%s\n' "$right" "$left" | sort -V | head -n1)" == "$right" ]]
}

extract_go_version() {
    local go_bin="$1"
    "$go_bin" version | awk '{print $3}' | sed 's/^go//'
}

setup_workdir() {
    if [[ -n "$WORKDIR" ]]; then
        mkdir -p "$WORKDIR"
        WORKDIR="$(cd "$WORKDIR" && pwd)"
        KEEP_WORKDIR=1
    else
        WORKDIR="$(mktemp -d -t libbox-build-XXXXXX)"
    fi
    mkdir -p "$WORKDIR"
    log "工作目录: $WORKDIR"
}

cleanup() {
    local code=$?
    if [[ $code -ne 0 ]]; then
        warn "构建失败，工作目录保留: $WORKDIR"
        exit "$code"
    fi
    if [[ "$KEEP_WORKDIR" -eq 1 ]]; then
        log "已按设置保留工作目录: $WORKDIR"
        return
    fi
    if [[ -n "$WORKDIR" && -d "$WORKDIR" ]]; then
        # 部分构建步骤（如容器内生成）可能产生当前用户不可删除文件，清理失败不应导致整体构建失败。
        chmod -R u+rwX "$WORKDIR" >/dev/null 2>&1 || true
        if rm -rf "$WORKDIR" >/dev/null 2>&1; then
            log "已清理临时目录"
        else
            warn "临时目录清理失败，已保留: $WORKDIR"
        fi
    fi
}

clone_sing_box() {
    SING_BOX_DIR="${WORKDIR}/sing-box"
    if git clone --depth 1 --branch "$RESOLVED_REF" "$SING_BOX_REPO" "$SING_BOX_DIR" >/dev/null 2>&1; then
        :
    else
        git clone --depth 1 "$SING_BOX_REPO" "$SING_BOX_DIR" >/dev/null 2>&1
        git -C "$SING_BOX_DIR" fetch --depth 1 origin "$RESOLVED_REF" >/dev/null
        git -C "$SING_BOX_DIR" checkout -q FETCH_HEAD
    fi

    SING_BOX_COMMIT="$(git -C "$SING_BOX_DIR" rev-parse HEAD)"
    SING_BOX_TAG="$(git -C "$SING_BOX_DIR" describe --tags --abbrev=0 2>/dev/null || true)"
    if [[ -z "$SING_BOX_TAG" ]]; then
        SING_BOX_TAG="$RESOLVED_REF"
    fi
    REQUIRED_GO_VERSION="$(
        awk '/^go[[:space:]]+[0-9]+\.[0-9]+(\.[0-9]+)?/{print $2; exit}' "$SING_BOX_DIR/go.mod"
    )"
    [[ -n "$REQUIRED_GO_VERSION" ]] || die "未能从 sing-box/go.mod 解析 Go 版本"

    local detected_gomobile
    detected_gomobile="$(
        awk -F'@' '/github.com\/sagernet\/gomobile\/cmd\/gomobile@/{print $2; exit}' "$SING_BOX_DIR/Makefile"
    )"
    if [[ -n "$detected_gomobile" ]]; then
        GOMOBILE_VERSION="$detected_gomobile"
    fi

    log "源码标签: $SING_BOX_TAG"
    log "源码提交: $SING_BOX_COMMIT"
    log "Go 版本要求: $REQUIRED_GO_VERSION"
    log "gomobile 版本: $GOMOBILE_VERSION"
}

setup_java() {
    if [[ -n "${JAVA_HOME:-}" && -x "${JAVA_HOME}/bin/java" ]] && java_is_openjdk17 "${JAVA_HOME}/bin/java"; then
        log "使用系统 JAVA_HOME: ${JAVA_HOME} (${JAVA_VERSION_LINE})"
        return
    fi
    if command -v java >/dev/null 2>&1 && java_is_openjdk17 "$(command -v java)"; then
        local java_bin
        java_bin="$(command -v java)"
        JAVA_HOME="$(cd "$(dirname "$java_bin")/.." && pwd)"
        export JAVA_HOME
        log "使用系统 Java: ${JAVA_HOME} (${JAVA_VERSION_LINE})"
        return
    fi

    local host_os host_arch java_os
    host_os="$(detect_host_os)"
    host_arch="$(detect_host_arch_java)"
    case "$host_os" in
        linux) java_os="linux" ;;
        darwin) java_os="mac" ;;
        *) die "不支持的系统: $host_os" ;;
    esac

    local download_dir archive_path extract_dir
    download_dir="${WORKDIR}/downloads"
    archive_path="${download_dir}/jdk17.tar.gz"
    extract_dir="${WORKDIR}/toolchains/jdk"
    mkdir -p "$download_dir" "$extract_dir"

    local jdk_url="https://api.adoptium.net/v3/binary/latest/17/ga/${java_os}/${host_arch}/jdk/hotspot/normal/eclipse"
    log "下载临时 JDK17: $jdk_url"
    curl -fL "$jdk_url" -o "$archive_path"
    tar -xzf "$archive_path" -C "$extract_dir"

    local extracted_home
    extracted_home="$(find "$extract_dir" -mindepth 1 -maxdepth 1 -type d | head -n1 || true)"
    [[ -n "$extracted_home" ]] || die "JDK 解压失败"
    JAVA_HOME="$extracted_home"
    export JAVA_HOME
    export PATH="${JAVA_HOME}/bin:${PATH}"

    java_is_openjdk17 "${JAVA_HOME}/bin/java" || die "下载的 JDK 不是 OpenJDK 17"
    log "已启用临时 JDK: ${JAVA_HOME} (${JAVA_VERSION_LINE})"
}

setup_go() {
    local requested_go_version="$REQUIRED_GO_VERSION"
    if [[ "$requested_go_version" =~ ^[0-9]+\.[0-9]+$ ]]; then
        requested_go_version="${requested_go_version}.0"
    fi

    local system_go_ok=0
    local system_go=""
    if command -v go >/dev/null 2>&1; then
        system_go="$(command -v go)"
        local system_go_ver
        system_go_ver="$(extract_go_version "$system_go")"
        if version_gte "$system_go_ver" "$requested_go_version"; then
            system_go_ok=1
            GO_VERSION_LINE="go${system_go_ver}"
            log "使用系统 Go: ${system_go} (${GO_VERSION_LINE})"
        else
            warn "系统 Go 版本 ${system_go_ver} 低于要求 ${requested_go_version}，将下载临时 Go"
        fi
    fi

    if [[ "$system_go_ok" -eq 0 ]]; then
        local host_os host_arch
        host_os="$(detect_host_os)"
        host_arch="$(detect_host_arch_go)"
        local go_url="https://go.dev/dl/go${requested_go_version}.${host_os}-${host_arch}.tar.gz"
        local download_dir archive_path extract_root goroot
        download_dir="${WORKDIR}/downloads"
        archive_path="${download_dir}/go.tar.gz"
        extract_root="${WORKDIR}/toolchains"
        mkdir -p "$download_dir" "$extract_root"

        log "下载临时 Go: $go_url"
        curl -fL "$go_url" -o "$archive_path"
        tar -xzf "$archive_path" -C "$extract_root"

        goroot="${extract_root}/go"
        [[ -x "${goroot}/bin/go" ]] || die "Go 解压失败"
        export GOROOT="$goroot"
        export PATH="${GOROOT}/bin:${PATH}"
        GO_VERSION_LINE="$("${GOROOT}/bin/go" version | awk '{print $3}')"
        log "已启用临时 Go: ${GOROOT} (${GO_VERSION_LINE})"
    fi

    export GOPATH="${WORKDIR}/gopath"
    export GOCACHE="${WORKDIR}/gocache"
    export GOMODCACHE="${WORKDIR}/gomodcache"
    export GOTOOLCHAIN=local
    mkdir -p "$GOPATH" "$GOCACHE" "$GOMODCACHE"
    export PATH="${GOPATH}/bin:${PATH}"
}

find_sdkmanager() {
    local sdk="$1"
    local candidate
    for candidate in \
        "${sdk}/cmdline-tools/latest/bin/sdkmanager" \
        "${sdk}/tools/bin/sdkmanager"; do
        if [[ -x "$candidate" ]]; then
            echo "$candidate"
            return
        fi
    done

    if [[ -d "${sdk}/cmdline-tools" ]]; then
        candidate="$(find "${sdk}/cmdline-tools" -type f -path "*/bin/sdkmanager" 2>/dev/null | head -n1 || true)"
        if [[ -n "$candidate" ]]; then
            echo "$candidate"
            return
        fi
    fi

    echo ""
}

detect_android_sdk() {
    if [[ -n "$ANDROID_SDK_OVERRIDE" ]]; then
        [[ -d "$ANDROID_SDK_OVERRIDE" ]] || die "--android-sdk 指定目录不存在: $ANDROID_SDK_OVERRIDE"
        echo "$ANDROID_SDK_OVERRIDE"
        return
    fi

    local path
    for path in \
        "${ANDROID_HOME:-}" \
        "${ANDROID_SDK_ROOT:-}" \
        "$HOME/Android/Sdk" \
        "$HOME/.local/lib/android/sdk" \
        "$HOME/Library/Android/sdk"; do
        [[ -n "$path" ]] || continue
        if [[ -d "$path" ]]; then
            echo "$path"
            return
        fi
    done

    echo ""
}

resolve_ndk_path() {
    local sdk="$1"
    local preferred="${sdk}/ndk/${ANDROID_NDK_VERSION}"
    if [[ -f "${preferred}/source.properties" ]]; then
        echo "$preferred"
        return
    fi

    local fallback
    fallback="$(
        find "${sdk}/ndk" -mindepth 1 -maxdepth 1 -type d 2>/dev/null \
            | while read -r ndk_dir; do
                if [[ -f "${ndk_dir}/source.properties" ]]; then
                    basename "$ndk_dir"
                fi
            done | sort -V | tail -n1
    )"

    if [[ -n "$fallback" ]]; then
        echo "${sdk}/ndk/${fallback}"
        return
    fi
    echo ""
}

setup_android_sdk() {
    ANDROID_SDK_PATH="$(detect_android_sdk)"
    [[ -n "$ANDROID_SDK_PATH" ]] || die "未找到 Android SDK，请安装后重试，或通过 --android-sdk 指定路径"
    mkdir -p "${ANDROID_SDK_PATH}"

    local sdkmanager
    sdkmanager="$(find_sdkmanager "$ANDROID_SDK_PATH")"
    if [[ ! -f "${ANDROID_SDK_PATH}/licenses/android-sdk-license" && -n "$sdkmanager" ]]; then
        log "SDK license 缺失，尝试自动接受 licenses"
        yes | "$sdkmanager" --sdk_root="$ANDROID_SDK_PATH" --licenses >/dev/null || true
    fi

    ANDROID_NDK_PATH="$(resolve_ndk_path "$ANDROID_SDK_PATH")"
    if [[ -z "$ANDROID_NDK_PATH" ]]; then
        [[ -n "$sdkmanager" ]] || die "未找到 Android NDK，且无法找到 sdkmanager 自动安装"
        log "未检测到 NDK，尝试自动安装 ndk;${ANDROID_NDK_VERSION}"
        yes | "$sdkmanager" --sdk_root="$ANDROID_SDK_PATH" --licenses >/dev/null || true
        "$sdkmanager" --sdk_root="$ANDROID_SDK_PATH" "ndk;${ANDROID_NDK_VERSION}" >/dev/null
        ANDROID_NDK_PATH="$(resolve_ndk_path "$ANDROID_SDK_PATH")"
    fi

    [[ -n "$ANDROID_NDK_PATH" ]] || die "Android NDK 安装失败"
    [[ -f "${ANDROID_NDK_PATH}/source.properties" ]] || die "NDK 路径无效: $ANDROID_NDK_PATH"
    ANDROID_NDK_VERSION="$(basename "$ANDROID_NDK_PATH")"

    export ANDROID_HOME="$ANDROID_SDK_PATH"
    export ANDROID_SDK_HOME="$ANDROID_SDK_PATH"
    export ANDROID_NDK_HOME="$ANDROID_NDK_PATH"
    export NDK="$ANDROID_NDK_PATH"

    log "使用 Android SDK: $ANDROID_SDK_PATH"
    log "使用 Android NDK: $ANDROID_NDK_PATH"
}

install_gomobile() {
    pushd "$SING_BOX_DIR" >/dev/null
    log "安装 gomobile/gobind: ${GOMOBILE_VERSION}"
    go install -v "github.com/sagernet/gomobile/cmd/gomobile@${GOMOBILE_VERSION}"
    go install -v "github.com/sagernet/gomobile/cmd/gobind@${GOMOBILE_VERSION}"
    popd >/dev/null
}

build_libbox() {
    pushd "$SING_BOX_DIR" >/dev/null
    log "开始构建 libbox.aar（官方入口: cmd/internal/build_libbox）"
    go run ./cmd/internal/build_libbox -target android
    popd >/dev/null
}

write_metadata() {
    local built_at
    built_at="$(date -u +"%Y-%m-%dT%H:%M:%SZ")"
    mkdir -p "$(dirname "$METADATA_FILE")"
    cat >"$METADATA_FILE" <<EOF
{
  "source_repo": "https://github.com/SagerNet/sing-box",
  "ref": "${RESOLVED_REF}",
  "tag": "${SING_BOX_TAG}",
  "commit": "${SING_BOX_COMMIT}",
  "built_at_utc": "${built_at}",
  "go_version": "${GO_VERSION_LINE}",
  "java_version": "${JAVA_VERSION_LINE}",
  "gomobile_version": "${GOMOBILE_VERSION}",
  "android_ndk_version": "${ANDROID_NDK_VERSION}"
}
EOF
}

copy_outputs() {
    [[ -f "${SING_BOX_DIR}/libbox.aar" ]] || die "构建成功但未找到 libbox.aar"
    mkdir -p "$(dirname "$OUTPUT_AAR")"
    install -m 0644 "${SING_BOX_DIR}/libbox.aar" "$OUTPUT_AAR"

    if [[ "$INCLUDE_LEGACY" -eq 1 ]]; then
        [[ -f "${SING_BOX_DIR}/libbox-legacy.aar" ]] || die "缺少 libbox-legacy.aar"
        mkdir -p "$(dirname "$OUTPUT_LEGACY_AAR")"
        install -m 0644 "${SING_BOX_DIR}/libbox-legacy.aar" "$OUTPUT_LEGACY_AAR"
    fi

    write_metadata
    log "产物已更新:"
    log "  - $OUTPUT_AAR"
    if [[ "$INCLUDE_LEGACY" -eq 1 ]]; then
        log "  - $OUTPUT_LEGACY_AAR"
    fi
    log "  - $METADATA_FILE"
}

parse_args() {
    local raw_workdir=""
    while [[ $# -gt 0 ]]; do
        case "$1" in
            --version)
                [[ $# -ge 2 ]] || die "--version 缺少参数"
                TARGET_VERSION="${2:-}"
                shift 2
                ;;
            --ref)
                [[ $# -ge 2 ]] || die "--ref 缺少参数"
                TARGET_REF="${2:-}"
                shift 2
                ;;
            --output)
                [[ $# -ge 2 ]] || die "--output 缺少参数"
                OUTPUT_AAR="${2:-}"
                shift 2
                ;;
            --metadata)
                [[ $# -ge 2 ]] || die "--metadata 缺少参数"
                METADATA_FILE="${2:-}"
                shift 2
                ;;
            --include-legacy)
                INCLUDE_LEGACY=1
                shift
                ;;
            --legacy-output)
                [[ $# -ge 2 ]] || die "--legacy-output 缺少参数"
                OUTPUT_LEGACY_AAR="${2:-}"
                shift 2
                ;;
            --android-sdk)
                [[ $# -ge 2 ]] || die "--android-sdk 缺少参数"
                ANDROID_SDK_OVERRIDE="${2:-}"
                shift 2
                ;;
            --ndk-version)
                [[ $# -ge 2 ]] || die "--ndk-version 缺少参数"
                ANDROID_NDK_VERSION="${2:-}"
                shift 2
                ;;
            --workdir)
                [[ $# -ge 2 ]] || die "--workdir 缺少参数"
                raw_workdir="${2:-}"
                shift 2
                ;;
            --keep-workdir)
                KEEP_WORKDIR=1
                shift
                ;;
            --dry-run)
                DRY_RUN=1
                shift
                ;;
            -h | --help)
                usage
                exit 0
                ;;
            *)
                die "未知参数: $1"
                ;;
        esac
    done

    if [[ -n "$raw_workdir" ]]; then
        WORKDIR="$raw_workdir"
    fi
}

main() {
    parse_args "$@"

    ensure_cmd git
    ensure_cmd curl
    ensure_cmd tar
    ensure_cmd sed
    ensure_cmd awk

    if [[ -n "$TARGET_REF" ]]; then
        RESOLVED_REF="$TARGET_REF"
    else
        if [[ -n "$TARGET_VERSION" ]]; then
            RESOLVED_TAG="$(normalize_tag "$TARGET_VERSION")"
        else
            RESOLVED_TAG="$(resolve_latest_release_tag)"
        fi
        RESOLVED_REF="$RESOLVED_TAG"
    fi

    if [[ "$DRY_RUN" -eq 1 ]]; then
        log "Dry run 模式，不执行任何下载与构建"
        log "目标 ref: $RESOLVED_REF"
        log "输出 AAR: $OUTPUT_AAR"
        if [[ "$INCLUDE_LEGACY" -eq 1 ]]; then
            log "输出 legacy AAR: $OUTPUT_LEGACY_AAR"
        fi
        log "元数据文件: $METADATA_FILE"
        exit 0
    fi

    setup_workdir
    trap cleanup EXIT

    clone_sing_box
    setup_java
    setup_go
    setup_android_sdk
    install_gomobile
    build_libbox
    copy_outputs
    log "构建完成"
}

main "$@"
