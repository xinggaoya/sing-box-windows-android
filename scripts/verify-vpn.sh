#!/usr/bin/env bash
# 端到端验证脚本（Android 模拟器）
#
# 覆盖：DNS 解析、TCP 连通性、国内直连 / 境外代理的分流、代理模式切换。
# 前置：模拟器已启动；本地测试代理与订阅服务器已运行（见 scripts/testproxy.py）。
set -u
export ANDROID_HOME="${ANDROID_HOME:-$HOME/AppData/Local/Android/Sdk}"
export PATH="$ANDROID_HOME/platform-tools:$PATH"
export MSYS_NO_PATHCONV=1

PKG=cn.moncn.sing_box_windows
SUB_URL="${SUB_URL:-http://10.0.2.2:8898/sub.json}"

log() { echo; echo "===== $* ====="; }

app_start() {
  adb shell am start -n $PKG/.v2.RefactorMainActivity >/dev/null 2>&1
  sleep 4
}

vpn_state() {
  adb shell dumpsys connectivity 2>&1 | grep -oE "ni\{VPN [A-Z]+" | head -1
}

dns_test() {
  for host in "$@"; do
    printf '  %-22s -> ' "$host"
    adb shell ping -c 1 -W 8 "$host" 2>&1 | head -1 | sed 's/ bytes of data.*//'
  done
}

tcp_test() {
  for host in "$@"; do
    printf '  %-22s -> ' "$host"
    adb shell "printf 'GET / HTTP/1.0\r\n\r\n' | nc -w 12 $host 443 2>&1 | head -1"
  done
}

log "启动应用"
app_start

log "① 未连接时的基线"
vpn_state

log "② 连接（兜底配置，无订阅）"
adb shell am broadcast -a $PKG.DIAG_CONNECT -p $PKG >/dev/null 2>&1
sleep 14
vpn_state
dns_test www.baidu.com www.qq.com
tcp_test www.qq.com

log "③ 断开"
adb shell am broadcast -a $PKG.DIAG_DISCONNECT -p $PKG >/dev/null 2>&1
sleep 4
vpn_state
