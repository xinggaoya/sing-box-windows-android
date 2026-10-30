package cn.moncn.sing_box_windows.v2.domain.subscription

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * 订阅地址归一化。
 *
 * 回归目标：输入法把 `https://` 自动大写成 `Https://` 后，订阅地址直接失效
 * （表现为「添加成功但同步永远失败」）。
 */
class SubscriptionUrlTest {

    @Test
    fun capitalisedScheme_isLowercased() {
        assertEquals(
            "https://example.com/sub",
            SubscriptionUrl.normalize("Https://example.com/sub")
        )
        assertEquals(
            "http://example.com/sub",
            SubscriptionUrl.normalize("HTTP://example.com/sub")
        )
    }

    @Test
    fun pathAndQuery_keepOriginalCase() {
        assertEquals(
            "https://example.com/Sub/Path?Token=AbCd",
            SubscriptionUrl.normalize("https://example.com/Sub/Path?Token=AbCd")
        )
    }

    @Test
    fun surroundingWhitespace_isTrimmed() {
        assertEquals(
            "https://example.com/sub",
            SubscriptionUrl.normalize("  https://example.com/sub  ")
        )
    }

    @Test
    fun nonUrlValue_isKeptAsIs() {
        assertEquals("not-a-url", SubscriptionUrl.normalize("not-a-url"))
        assertEquals("", SubscriptionUrl.normalize("   "))
    }

    @Test
    fun urlWithoutScheme_isKeptAsIs() {
        assertEquals("example.com/sub", SubscriptionUrl.normalize("example.com/sub"))
    }
}
