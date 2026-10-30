package cn.moncn.sing_box_windows.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * 状态语义色组。
 *
 * 把「强调色 / 浅底 / 底上文字」三件套打包，避免在每个组件里重复拼装
 * 且出现底色与文字色不匹配的问题。
 */
@Immutable
data class StatusPalette(
    /** 图标、强调文字、进度条等强强调元素 */
    val content: Color,
    /** 标签、提示条的浅色底 */
    val container: Color,
    /** 浅色底之上的文字色 */
    val onContainer: Color,
    /** 浅色底之上的边框色 */
    val border: Color = content.copy(alpha = 0.22f)
)

/**
 * 语义令牌（Semantic Tokens）——设计系统的第二层。
 *
 * 这是整套视觉体系唯一的事实来源：
 * - [MaterialTheme] 的 `colorScheme` 由本对象派生，M3 原生组件与自定义组件共用同一套色值；
 * - 组件通过 [LocalAppColors] 读取品牌渐变、状态色组等扩展令牌。
 *
 * 明暗两套取值共享同一份结构定义，保证新增语义时不会出现「只加了一半」的问题。
 */
@Immutable
data class AppColors(
    // ---------------- 品牌强调 ----------------
    val primary: Color,
    val onPrimary: Color,
    val primaryContainer: Color,
    val onPrimaryContainer: Color,
    val primaryHover: Color,
    val primaryPressed: Color,
    val primaryBorder: Color,

    // ---------------- 背景层级 ----------------
    /** 页面底色 */
    val background: Color,
    /** 卡片等常规容器 */
    val surface: Color,
    /** 次级填充：指标块、胶囊标签底 */
    val surfaceMuted: Color,
    /** 凹陷容器：终端日志、输入框底 */
    val surfaceSunken: Color,
    /** 浮层容器：导航栏、对话框 */
    val surfaceElevated: Color,
    val scrim: Color,

    // ---------------- 文本层级 ----------------
    val textPrimary: Color,
    val textSecondary: Color,
    val textTertiary: Color,
    val textDisabled: Color,

    // ---------------- 描边层级 ----------------
    val border: Color,
    val borderStrong: Color,
    val borderSubtle: Color,

    // ---------------- 状态语义 ----------------
    val success: StatusPalette,
    val warning: StatusPalette,
    val error: StatusPalette,
    val info: StatusPalette,
    val critical: StatusPalette,

    // ---------------- 品牌装饰 ----------------
    /** 品牌主渐变（横向），用于强调型按钮与 Hero 装饰 */
    val brandGradient: List<Color>,
    /** Hero 卡片底色渐变（纵向） */
    val heroGradient: List<Color>,
    /** Hero 光晕色 */
    val heroGlow: Color,

    val isDark: Boolean
) {

    /** 由语义令牌派生 Material3 配色方案，保证 M3 组件与自定义组件同源。 */
    fun toColorScheme(): ColorScheme {
        val scheme = if (isDark) {
            darkColorScheme(
                primary = primary,
                onPrimary = onPrimary,
                primaryContainer = primaryContainer,
                onPrimaryContainer = onPrimaryContainer,
                inversePrimary = AppPalette.Indigo300,
                secondary = info.content,
                onSecondary = onPrimary,
                secondaryContainer = info.container,
                onSecondaryContainer = info.onContainer,
                tertiary = success.content,
                onTertiary = onPrimary,
                tertiaryContainer = success.container,
                onTertiaryContainer = success.onContainer,
                error = error.content,
                onError = onPrimary,
                errorContainer = error.container,
                onErrorContainer = error.onContainer,
                background = background,
                onBackground = textPrimary,
                surface = surface,
                onSurface = textPrimary,
                surfaceVariant = surfaceMuted,
                onSurfaceVariant = textSecondary,
                surfaceContainerLowest = surfaceSunken,
                surfaceContainerLow = surface,
                surfaceContainer = surfaceMuted,
                surfaceContainerHigh = surfaceElevated,
                surfaceContainerHighest = surfaceElevated,
                outline = borderStrong,
                outlineVariant = border,
                scrim = scrim
            )
        } else {
            lightColorScheme(
                primary = primary,
                onPrimary = onPrimary,
                primaryContainer = primaryContainer,
                onPrimaryContainer = onPrimaryContainer,
                inversePrimary = AppPalette.Indigo800,
                secondary = info.content,
                onSecondary = Color.White,
                secondaryContainer = info.container,
                onSecondaryContainer = info.onContainer,
                tertiary = success.content,
                onTertiary = Color.White,
                tertiaryContainer = success.container,
                onTertiaryContainer = success.onContainer,
                error = error.content,
                onError = Color.White,
                errorContainer = error.container,
                onErrorContainer = error.onContainer,
                background = background,
                onBackground = textPrimary,
                surface = surface,
                onSurface = textPrimary,
                surfaceVariant = surfaceMuted,
                onSurfaceVariant = textSecondary,
                surfaceContainerLowest = surfaceSunken,
                surfaceContainerLow = surface,
                surfaceContainer = surfaceMuted,
                surfaceContainerHigh = surfaceElevated,
                surfaceContainerHighest = surfaceElevated,
                outline = borderStrong,
                outlineVariant = border,
                scrim = scrim
            )
        }
        return scheme
    }

    companion object {

        /** 亮色主题令牌 */
        val Light = AppColors(
            primary = AppPalette.Indigo600,
            onPrimary = Color.White,
            primaryContainer = AppPalette.Indigo50,
            onPrimaryContainer = AppPalette.Indigo800,
            primaryHover = AppPalette.Indigo500,
            primaryPressed = AppPalette.Indigo700,
            primaryBorder = AppPalette.Indigo200,

            background = Color(0xFFF4F5FA),
            surface = Color.White,
            surfaceMuted = Color(0xFFF1F3F9),
            surfaceSunken = Color(0xFFEDEFF6),
            surfaceElevated = Color.White,
            scrim = Color(0x99101828),

            textPrimary = AppPalette.Slate900,
            textSecondary = AppPalette.Slate600,
            textTertiary = AppPalette.Slate500,
            textDisabled = AppPalette.Slate400,

            border = AppPalette.Slate200,
            borderStrong = AppPalette.Slate300,
            borderSubtle = Color(0xFFEFF2F7),

            success = StatusPalette(
                content = AppPalette.Green600,
                container = AppPalette.Green100,
                onContainer = AppPalette.Green900
            ),
            warning = StatusPalette(
                content = AppPalette.Amber700,
                container = AppPalette.Amber100,
                onContainer = AppPalette.Amber900
            ),
            error = StatusPalette(
                content = AppPalette.Red600,
                container = AppPalette.Red100,
                onContainer = AppPalette.Red900
            ),
            info = StatusPalette(
                content = AppPalette.Blue600,
                container = AppPalette.Blue100,
                onContainer = AppPalette.Blue900
            ),
            critical = StatusPalette(
                content = AppPalette.Orange600,
                container = Color(0xFFFFEDD5),
                onContainer = AppPalette.Orange900
            ),

            brandGradient = listOf(AppPalette.Indigo500, AppPalette.Indigo600, AppPalette.Indigo700),
            heroGradient = listOf(Color(0xFFEEF2FF), Color(0xFFF7F8FF), Color.White),
            heroGlow = AppPalette.Indigo200,

            isDark = false
        )

        /** 暗色主题令牌 */
        val Dark = AppColors(
            primary = AppPalette.Indigo400,
            onPrimary = Color(0xFF14162B),
            primaryContainer = AppPalette.Indigo900,
            onPrimaryContainer = AppPalette.Indigo200,
            primaryHover = AppPalette.Indigo300,
            primaryPressed = AppPalette.Indigo500,
            primaryBorder = AppPalette.Indigo700,

            background = Color(0xFF0A0E1A),
            surface = Color(0xFF121828),
            surfaceMuted = Color(0xFF1A2234),
            surfaceSunken = Color(0xFF070B14),
            surfaceElevated = Color(0xFF1A2236),
            scrim = Color(0xCC050810),

            textPrimary = AppPalette.Slate100,
            textSecondary = AppPalette.Slate300,
            textTertiary = AppPalette.Slate400,
            textDisabled = AppPalette.Slate600,

            border = Color(0xFF232C3D),
            borderStrong = Color(0xFF33415A),
            borderSubtle = Color(0xFF1A2130),

            success = StatusPalette(
                content = AppPalette.Green400,
                container = Color(0xFF10352B),
                onContainer = AppPalette.Green300
            ),
            warning = StatusPalette(
                content = AppPalette.Amber400,
                container = Color(0xFF3A2E0A),
                onContainer = AppPalette.Amber300
            ),
            error = StatusPalette(
                content = AppPalette.Red400,
                container = Color(0xFF3F1D1D),
                onContainer = AppPalette.Red300
            ),
            info = StatusPalette(
                content = AppPalette.Blue400,
                container = Color(0xFF102A43),
                onContainer = AppPalette.Blue300
            ),
            critical = StatusPalette(
                content = AppPalette.Orange400,
                container = Color(0xFF3A2410),
                onContainer = Color(0xFFFCD9B6)
            ),

            brandGradient = listOf(AppPalette.Indigo400, AppPalette.Indigo500, AppPalette.Indigo700),
            heroGradient = listOf(Color(0xFF1A1F3C), Color(0xFF141A2C), Color(0xFF121828)),
            heroGlow = AppPalette.Indigo700,

            isDark = true
        )
    }
}

/** 通过 [androidx.compose.material3.MaterialTheme] 向下透传扩展语义令牌。 */
val LocalAppColors = staticCompositionLocalOf { AppColors.Light }
