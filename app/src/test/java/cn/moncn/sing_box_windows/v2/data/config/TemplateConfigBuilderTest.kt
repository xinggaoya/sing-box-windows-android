package cn.moncn.sing_box_windows.v2.data.config

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 订阅 → sing-box 配置模板生成的单元测试。
 *
 * 关键约束：生成的配置不得包含 `experimental.clash_api`，
 * 内核能力统一走 libbox 自带的 CommandServer/CommandClient。
 */
class TemplateConfigBuilderTest {

    private val singBoxSubscription = """
        {
          "outbounds": [
            { "type": "shadowsocks", "tag": "node-a", "server": "1.2.3.4", "server_port": 8388, "method": "aes-256-gcm", "password": "secret" },
            { "type": "direct", "tag": "direct" }
          ]
        }
    """.trimIndent()

    @Test
    fun buildFromSubscription_buildsConfigWithoutClashApi() {
        val result = TemplateConfigBuilder.buildFromSubscription(singBoxSubscription)
        assertTrue(result.ok)
        val config = JSONObject(result.configJson!!)
        val experimental = config.getJSONObject("experimental")
        assertFalse("不应生成 clash_api", experimental.has("clash_api"))
        assertTrue("应保留 cache_file", experimental.has("cache_file"))
    }

    @Test
    fun buildFromSubscription_keepsNodeOutbounds() {
        val result = TemplateConfigBuilder.buildFromSubscription(singBoxSubscription)
        assertTrue(result.ok)
        val outbounds = JSONObject(result.configJson!!).getJSONArray("outbounds")
        val tags = (0 until outbounds.length()).map { outbounds.getJSONObject(it).getString("tag") }
        assertTrue("应包含订阅节点", tags.any { it.contains("node-a") })
    }

    @Test
    fun buildFromSubscription_rejectsUnrecognisedContent() {
        val result = TemplateConfigBuilder.buildFromSubscription("just some plain text")
        assertFalse(result.ok)
        assertNotNull(result.error)
    }

    @Test
    fun buildFromSubscription_rejectsEmptyNodeList() {
        val result = TemplateConfigBuilder.buildFromSubscription("""{"outbounds": []}""")
        assertFalse(result.ok)
    }
}
