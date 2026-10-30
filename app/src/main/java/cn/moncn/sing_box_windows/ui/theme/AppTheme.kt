package cn.moncn.sing_box_windows.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

/**
 * 应用主题入口。
 *
 * 视觉体系的核心原则：**语义令牌是唯一事实来源**。
 * - [AppColors] 定义全部色彩语义；
 * - Material3 的 `colorScheme` 由 [AppColors.toColorScheme] 派生，
 *   使得 Switch / Dialog / TextField 等原生组件与自定义组件天然同色；
 * - [LocalAppColors] 向下透传品牌渐变、状态色组等扩展令牌。
 *
 * @param themeMode 用户选择的模式，默认跟随系统。
 * @param dynamicColor 是否启用 Android 12+ 动态取色（会牺牲品牌一致性，默认关闭）。
 */
@Composable
fun SingboxwindowsTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val systemDark = isSystemInDarkTheme()
    val darkTheme = when (themeMode) {
        ThemeMode.SYSTEM -> systemDark
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }

    val appColors = if (darkTheme) AppColors.Dark else AppColors.Light

    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        else -> appColors.toColorScheme()
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            val controller = WindowCompat.getInsetsController(window, view)
            controller.isAppearanceLightStatusBars = !darkTheme
            controller.isAppearanceLightNavigationBars = !darkTheme
        }
    }

    CompositionLocalProvider(LocalAppColors provides appColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = AppTypography,
            shapes = AppShapes,
            content = content
        )
    }
}
