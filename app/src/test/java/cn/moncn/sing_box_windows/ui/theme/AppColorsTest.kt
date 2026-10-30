package cn.moncn.sing_box_windows.ui.theme

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

/**
 * 设计系统色彩令牌的守卫测试。
 *
 * 视觉体系最容易在迭代中被悄悄破坏（新增语义却忘了暗色档、
 * 调整色值导致正文对比度跌破 AA）。这里把关键约束固化为测试，
 * 让「可读性」成为可回归的契约，而不是靠人眼检查。
 */
class AppColorsTest {

    @Test
    fun lightAndDarkShareIdenticalTokenStructure() {
        val light = tokenNames(AppColors.Light)
        val dark = tokenNames(AppColors.Dark)
        assertEquals("明暗两套令牌的字段必须完全一致", light, dark)
    }

    @Test
    fun bodyTextMeetsWcagAaOnSurface() {
        listOf(AppColors.Light, AppColors.Dark).forEach { colors ->
            val label = if (colors.isDark) "暗色" else "亮色"
            assertTrue(
                "${label}主题：正文主色对比度不足",
                contrast(colors.textPrimary, colors.surface) >= AA_NORMAL
            )
            assertTrue(
                "${label}主题：次级文字对比度不足",
                contrast(colors.textSecondary, colors.surface) >= AA_NORMAL
            )
            assertTrue(
                "${label}主题：三级文字对比度不足",
                contrast(colors.textTertiary, colors.surface) >= AA_NORMAL
            )
        }
    }

    @Test
    fun primaryColorIsLegibleOnSurface() {
        listOf(AppColors.Light, AppColors.Dark).forEach { colors ->
            assertTrue(
                "品牌色在卡片上必须清晰可辨",
                contrast(colors.primary, colors.surface) >= AA_LARGE
            )
            assertTrue(
                "品牌色上的文字对比度不足",
                contrast(colors.onPrimary, colors.primary) >= AA_NORMAL
            )
        }
    }

    @Test
    fun statusColorsAreLegibleOnTheirContainers() {
        listOf(AppColors.Light, AppColors.Dark).forEach { colors ->
            val statuses = listOf(
                "成功" to colors.success,
                "警告" to colors.warning,
                "错误" to colors.error,
                "信息" to colors.info,
                "严重" to colors.critical
            )
            statuses.forEach { (name, palette) ->
                assertTrue(
                    "$name 状态色在浅底上的对比度不足",
                    contrast(palette.onContainer, palette.container) >= AA_NORMAL
                )
                assertTrue(
                    "$name 状态色在卡片上的对比度不足",
                    contrast(palette.content, colors.surface) >= AA_LARGE
                )
            }
        }
    }

    @Test
    fun surfacesAreDistinguishableFromBackground() {
        listOf(AppColors.Light, AppColors.Dark).forEach { colors ->
            val label = if (colors.isDark) "暗色" else "亮色"
            assertTrue(
                "${label}主题：卡片与页面底色无法区分",
                contrast(colors.surface, colors.background) < 1.6
            )
        }
    }

    @Test
    fun colorSchemeIsDerivedFromSemanticTokens() {
        val lightScheme = AppColors.Light.toColorScheme()
        assertEquals(AppColors.Light.primary, lightScheme.primary)
        assertEquals(AppColors.Light.surface, lightScheme.surface)
        assertEquals(AppColors.Light.background, lightScheme.background)
        assertEquals(AppColors.Light.error.content, lightScheme.error)

        val darkScheme = AppColors.Dark.toColorScheme()
        assertEquals(AppColors.Dark.primary, darkScheme.primary)
        assertEquals(AppColors.Dark.surface, darkScheme.surface)
        assertEquals(AppColors.Dark.background, darkScheme.background)
        assertEquals(AppColors.Dark.error.content, darkScheme.error)
    }

    // ---------------------------------------------------------------------
    // 工具方法
    // ---------------------------------------------------------------------

    private fun tokenNames(colors: AppColors): Set<String> =
        AppColors::class.java.declaredFields
            .filter { !it.isSynthetic }
            .map { it.name }
            .toSet()

    private fun luminance(color: Color): Double {
        fun channel(value: Float): Double {
            val v = value.toDouble()
            return if (v <= 0.03928) v / 12.92 else ((v + 0.055) / 1.055).pow(2.4)
        }
        return 0.2126 * channel(color.red) +
            0.7152 * channel(color.green) +
            0.0722 * channel(color.blue)
    }

    private fun contrast(a: Color, b: Color): Double {
        val l1 = luminance(a)
        val l2 = luminance(b)
        return (max(l1, l2) + 0.05) / (min(l1, l2) + 0.05)
    }

    private companion object {
        /** WCAG AA：正常字号文本 4.5:1 */
        const val AA_NORMAL = 4.5

        /** WCAG AA：图形 / 大字号 3:1 */
        const val AA_LARGE = 3.0
    }
}
