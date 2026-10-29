package cn.moncn.sing_box_windows.ui

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * 展示层格式化工具的单元测试。
 */
class FormatTest {

    @Test
    fun bytes_formatsZeroValue() {
        assertEquals("0 B", Format.bytes(0L))
        assertEquals("0 B", Format.bytes(-1L))
    }

    @Test
    fun bytes_formatsPlainBytesWithoutDecimal() {
        assertEquals("512 B", Format.bytes(512L))
    }

    @Test
    fun bytes_scalesToBinaryUnits() {
        assertEquals("1.00 KB", Format.bytes(1024L))
        assertEquals("1.00 MB", Format.bytes(1024L * 1024))
        assertEquals("1.00 GB", Format.bytes(1024L * 1024 * 1024))
    }

    @Test
    fun speed_appendsPerSecondSuffix() {
        assertEquals("1.00 KB/s", Format.speed(1024L))
        assertEquals("0 B/s", Format.speed(0L))
    }

    @Test
    fun duration_formatsByMagnitude() {
        assertEquals("-", Format.duration(0L))
        assertEquals("250ms", Format.duration(250L))
        assertEquals("1.5s", Format.duration(1500L))
        assertEquals("1m30s", Format.duration(90_000L))
        assertEquals("1h01m", Format.duration(3_660_000L))
    }
}
