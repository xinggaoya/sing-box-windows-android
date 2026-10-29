package cn.moncn.sing_box_windows.v2.domain.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 内核领域模型的单元测试。
 */
class CoreModelsTest {

    @Test
    fun clashMode_parsesKnownRawValues() {
        assertEquals(CoreClashMode.RULE, CoreClashMode.fromRaw("rule"))
        assertEquals(CoreClashMode.GLOBAL, CoreClashMode.fromRaw("global"))
        assertEquals(CoreClashMode.DIRECT, CoreClashMode.fromRaw("direct"))
    }

    @Test
    fun clashMode_isCaseInsensitiveAndTrims() {
        assertEquals(CoreClashMode.GLOBAL, CoreClashMode.fromRaw("  GLOBAL "))
    }

    @Test
    fun clashMode_returnsNullForUnknownValue() {
        assertNull(CoreClashMode.fromRaw("unknown"))
        assertNull(CoreClashMode.fromRaw(null))
    }

    @Test
    fun logEntry_exposesLevelName() {
        assertEquals("INFO", CoreLogEntry.levelName(CoreLogEntry.LEVEL_INFO))
        assertEquals("WARN", CoreLogEntry.levelName(CoreLogEntry.LEVEL_WARN))
        assertEquals("ERROR", CoreLogEntry.levelName(CoreLogEntry.LEVEL_ERROR))
        assertEquals("UNKNOWN", CoreLogEntry.levelName(99))
    }

    @Test
    fun outboundItem_testedOnlyWhenTimestampPresent() {
        assertFalse(OutboundGroupItem(tag = "a", type = "shadowsocks").tested)
        assertTrue(
            OutboundGroupItem(tag = "a", type = "shadowsocks", urlTestTime = 100L).tested
        )
    }

    @Test
    fun connection_closedDependsOnClosedAt() {
        assertFalse(CoreConnection(id = "1").closed)
        assertTrue(CoreConnection(id = "1", closedAt = 42L).closed)
    }

    @Test
    fun connection_displayTargetPrefersDomainOverAddress() {
        assertEquals(
            "example.com",
            CoreConnection(id = "1", domain = "example.com", destination = "1.2.3.4").displayTarget
        )
        assertEquals(
            "1.2.3.4",
            CoreConnection(id = "1", destination = "1.2.3.4").displayTarget
        )
    }
}
