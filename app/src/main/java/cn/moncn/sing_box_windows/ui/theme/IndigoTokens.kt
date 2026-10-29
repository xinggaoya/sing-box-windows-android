package cn.moncn.sing_box_windows.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * 语义令牌（Semantic Tokens）。
 *
 * 与桌面端 `tokens.css` 第二层对齐：亮/暗两套取值，
 * 供 [IndigoTheme] 注入 Compose 树，供组件按 `isDark` 读取。
 */
@Immutable
data class IndigoTokens(
    // 强调色
    val primary: Color,
    val primaryHover: Color,
    val primaryActive: Color,
    val primarySoft: Color,
    val primarySoftStrong: Color,
    val primaryContrast: Color,

    // 功能语义色
    val success: Color,
    val successSoft: Color,
    val warning: Color,
    val warningSoft: Color,
    val error: Color,
    val errorSoft: Color,
    val info: Color,
    val infoSoft: Color,
    val critical: Color,
    val criticalSoft: Color,

    // 文本层级
    val textPrimary: Color,
    val textSecondary: Color,
    val textTertiary: Color,
    val textDisabled: Color,
    val textOnPrimary: Color,

    // 背景层级
    val bgBase: Color,
    val bgSurface: Color,
    val bgSurface2: Color,
    val bgElevated: Color,
    val bgOverlay: Color,

    // 边框
    val border: Color,
    val borderStrong: Color,
    val borderHover: Color,

    // Chip
    val chipBg: Color,
    val chipText: Color,

    val isDark: Boolean
) {
    companion object {
        /** 亮色语义令牌 */
        val Light = IndigoTokens(
            primary = IndigoPalette.Indigo500,
            primaryHover = IndigoPalette.Indigo400,
            primaryActive = IndigoPalette.Indigo600,
            primarySoft = Color(0x1A6366F1),
            primarySoftStrong = Color(0x296366F1),
            primaryContrast = Color(0xFFFFFFFF),

            success = IndigoPalette.Green500,
            successSoft = Color(0x1E10B981),
            warning = IndigoPalette.Amber500,
            warningSoft = Color(0x24F59E0B),
            error = IndigoPalette.Red500,
            errorSoft = Color(0x1FEF4444),
            info = IndigoPalette.Blue500,
            infoSoft = Color(0x1F3B82F6),
            critical = IndigoPalette.Orange500,
            criticalSoft = Color(0x1FF97316),

            textPrimary = IndigoPalette.Slate900,
            textSecondary = IndigoPalette.Slate600,
            textTertiary = IndigoPalette.Slate400,
            textDisabled = IndigoPalette.Slate300,
            textOnPrimary = Color(0xFFFFFFFF),

            bgBase = IndigoPalette.Slate50,
            bgSurface = Color(0xFFFFFFFF),
            bgSurface2 = IndigoPalette.Slate100,
            bgElevated = Color(0xFFFFFFFF),
            bgOverlay = Color(0xB8FFFFFF),

            border = Color(0x383CBD5E),
            borderStrong = Color(0x6694A3B8),
            borderHover = Color(0x666366F1),

            chipBg = Color(0x1A6366F1),
            chipText = IndigoPalette.Indigo700,

            isDark = false
        )

        /** 暗色语义令牌 */
        val Dark = IndigoTokens(
            primary = IndigoPalette.Indigo400,
            primaryHover = IndigoPalette.Indigo300,
            primaryActive = IndigoPalette.Indigo500,
            primarySoft = Color(0x29818CF8),
            primarySoftStrong = Color(0x3D818CF8),
            primaryContrast = Color(0xFFFFFFFF),

            success = IndigoPalette.Green400,
            successSoft = Color(0x2634D399),
            warning = IndigoPalette.Amber400,
            warningSoft = Color(0x29FBBF24),
            error = IndigoPalette.Red400,
            errorSoft = Color(0x26F87171),
            info = IndigoPalette.Blue400,
            infoSoft = Color(0x2660A5FA),
            critical = IndigoPalette.Orange500,
            criticalSoft = Color(0x26F97316),

            textPrimary = Color(0xFFF8FAFC),
            textSecondary = IndigoPalette.Slate300,
            textTertiary = IndigoPalette.Slate600,
            textDisabled = IndigoPalette.Slate700,
            textOnPrimary = Color(0xFFFFFFFF),

            bgBase = IndigoPalette.Slate900,
            bgSurface = IndigoPalette.Slate800,
            bgSurface2 = IndigoPalette.Slate700,
            bgElevated = Color(0xFF1A2438),
            bgOverlay = Color(0xB80F172A),

            border = Color(0x1AFFFFFF),
            borderStrong = Color(0x2EFFFFFF),
            borderHover = Color(0x80818CF8),

            chipBg = Color(0x2E818CF8),
            chipText = IndigoPalette.Indigo100,

            isDark = true
        )
    }
}

/** 通过 `MaterialTheme` 向下透传语义令牌 */
val LocalIndigoTokens = staticCompositionLocalOf { IndigoTokens.Light }
