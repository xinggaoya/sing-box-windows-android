package cn.moncn.sing_box_windows.ui

import java.util.Locale
import kotlin.math.ln
import kotlin.math.pow

/** 展示层的格式化工具，供各页面共用 */
object Format {

    private val BYTE_UNITS = arrayOf("B", "KB", "MB", "GB", "TB", "PB")

    /** 字节数 → 可读文本 */
    fun bytes(value: Long): String {
        if (value <= 0L) return "0 B"
        val group = (ln(value.toDouble()) / ln(1024.0)).toInt().coerceAtMost(BYTE_UNITS.lastIndex)
        val scaled = value / 1024.0.pow(group.toDouble())
        return if (group == 0) {
            String.format(Locale.US, "%d %s", value, BYTE_UNITS[group])
        } else {
            String.format(Locale.US, "%.2f %s", scaled, BYTE_UNITS[group])
        }
    }

    /** 速率（字节/秒）→ 可读文本 */
    fun speed(bytesPerSecond: Long): String = "${bytes(bytesPerSecond)}/s"

    /** 毫秒时长 → 可读文本 */
    fun duration(millis: Long): String {
        if (millis <= 0L) return "-"
        return when {
            millis < 1000 -> "${millis}ms"
            millis < 60_000 -> String.format(Locale.US, "%.1fs", millis / 1000.0)
            millis < 3_600_000 -> String.format(Locale.US, "%dm%02ds", millis / 60_000, (millis % 60_000) / 1000)
            else -> String.format(Locale.US, "%dh%02dm", millis / 3_600_000, (millis % 3_600_000) / 60_000)
        }
    }

    /** 内存字节数 → 可读文本 */
    fun memory(bytes: Long): String = bytes(bytes)
}
