package cn.moncn.sing_box_windows.v2.data.config

import org.json.JSONObject
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test
import java.io.File
import java.util.concurrent.TimeUnit

/**
 * 配置格式的**内核级**校验。
 *
 * 用真实 sing-box 内核对生成的配置做一次 `sing-box check`，
 * 这是唯一能真正确认「配置能被当前内核接受」的手段——
 * 纯字段断言无法发现内核新增的格式约束。
 *
 * 需要可执行的 sing-box CLI：
 * - 设置环境变量 `SING_BOX_CLI` 指向可执行文件；或
 * - 在 PATH 中提供 `sing-box`。
 *
 * 未提供时本测试会被跳过（Assume），不影响常规 CI。
 */
class SingBoxConfigOracleTest {

    private val cli: String? by lazy { resolveCli() }

    private fun resolveCli(): String? {
        val fromEnv = System.getenv("SING_BOX_CLI")
        if (!fromEnv.isNullOrBlank() && File(fromEnv).canExecute()) return fromEnv
        val path = System.getenv("PATH").orEmpty().split(File.pathSeparator)
        for (dir in path) {
            val candidate = File(dir, "sing-box")
            if (candidate.canExecute()) return candidate.absolutePath
        }
        return null
    }

    private fun check(configJson: String, label: String) {
        val binary = cli ?: return
        assumeTrue("未提供 sing-box CLI，跳过内核级校验", binary.isNotBlank())

        val temp = File.createTempFile("singbox-oracle", ".json")
        try {
            temp.writeText(configJson)
            val process = ProcessBuilder(binary, "check", "-c", temp.absolutePath)
                .redirectErrorStream(true)
                .start()
            val output = process.inputStream.bufferedReader().readText()
            val exitCode = process.waitFor(60, TimeUnit.SECONDS)
            assertTrue(
                "$label 未通过 sing-box check：\n$output",
                exitCode && process.exitValue() == 0
            )
        } finally {
            temp.delete()
        }
    }

    private fun sampleSubscription() = """
        {
          "outbounds": [
            { "type": "shadowsocks", "tag": "node-a", "server": "1.2.3.4", "server_port": 8388, "method": "aes-256-gcm", "password": "secret" },
            { "type": "trojan", "tag": "node-b", "server": "5.6.7.8", "server_port": 443, "password": "pwd" }
          ]
        }
    """.trimIndent()

    @Test
    fun generatedTemplateConfig_isAcceptedByKernel() {
        val result = TemplateConfigBuilder.buildFromSubscription(sampleSubscription())
        assertTrue(result.ok)
        check(result.configJson!!, "订阅生成的模板配置")
    }

    @Test
    fun defaultConfig_isAcceptedByKernel() {
        check(ConfigDefaults.defaultConfigJson(), "默认兜底配置")
    }

    @Test
    fun clashConvertedConfig_isAcceptedByKernel() {
        val clash = """
            proxies:
              - name: "节点A"
                type: ss
                server: 1.2.3.4
                port: 8388
                cipher: aes-256-gcm
                password: secret
            proxy-groups:
              - name: PROXY
                type: select
                proxies: ["节点A"]
            rules:
              - DOMAIN-SUFFIX,example.com,PROXY
        """.trimIndent()

        val result = ClashConverter.convert(clash)
        check(result.json, "Clash 转换后的配置")

        val root = JSONObject(result.json)
        val inbounds = root.getJSONArray("inbounds")
        for (i in 0 until inbounds.length()) {
            assertTrue(
                "Clash 转换结果不应含入站 legacy 字段",
                !inbounds.getJSONObject(i).has("sniff")
            )
        }
        val rules = root.getJSONObject("route").getJSONArray("rules")
        assertTrue("应补上 sniff 路由动作", rules.getJSONObject(0).optString("action") == "sniff")
    }

    @Test
    fun migratedLegacyConfig_isAcceptedByKernel() {
        // 模拟 1.12 时代持久化到设备上的历史配置
        val legacy = """
            {
              "log": { "level": "info" },
              "dns": {
                "servers": [
                  { "address": "tls://1.1.1.1", "address_resolver": "local" },
                  { "type": "udp", "server": "114.114.114.114", "tag": "dns_resolver" }
                ],
                "rules": [ { "action": "route", "server": "dns_resolver" } ],
                "independent_cache": true,
                "strategy": "prefer_ipv4",
                "final": "dns_resolver"
              },
              "inbounds": [
                { "type": "tun", "tag": "tun-in", "address": ["172.18.0.1/30"],
                  "mtu": 9000, "auto_route": true, "strict_route": true, "stack": "system",
                  "sniff": true, "sniff_override_destination": true },
                { "type": "mixed", "tag": "mixed-in", "listen": "127.0.0.1", "listen_port": 1082, "sniff": true }
              ],
              "outbounds": [
                { "type": "selector", "tag": "Proxy", "outbounds": [ "direct" ], "default": "direct" },
                { "type": "direct", "tag": "direct" }
              ],
              "route": {
                "rules": [
                  { "action": "sniff" },
                  { "protocol": "dns", "action": "hijack-dns" },
                  { "ip_is_private": true, "outbound": "direct" }
                ],
                "final": "Proxy",
                "auto_detect_interface": true,
                "default_domain_resolver": { "server": "dns_resolver", "strategy": "prefer_ipv4" }
              },
              "experimental": {
                "cache_file": { "enabled": true },
                "clash_api": { "default_mode": "rule", "external_controller": "127.0.0.1:9090" }
              }
            }
        """.trimIndent()

        val migrated = ConfigMigrator.migrate(legacy)
        check(migrated, "迁移后的历史配置")

        // 同时确认迁移结果中不再有 legacy 字段
        val root = JSONObject(migrated)
        val inbounds = root.getJSONArray("inbounds")
        for (i in 0 until inbounds.length()) {
            val inbound = inbounds.getJSONObject(i)
            assertTrue("inbounds[$i] 仍含 sniff", !inbound.has("sniff"))
            assertTrue("inbounds[$i] 仍含 sniff_override_destination", !inbound.has("sniff_override_destination"))
        }
        assertTrue("dns 仍含 independent_cache", !root.getJSONObject("dns").has("independent_cache"))
        assertTrue(
            "experimental 仍含 clash_api",
            !root.getJSONObject("experimental").has("clash_api")
        )
    }
}
