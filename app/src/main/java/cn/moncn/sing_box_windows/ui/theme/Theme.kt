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

/** 主题模式，对齐桌面端的亮/暗/跟随系统三态 */
enum class ThemeMode {
    SYSTEM,
    LIGHT,
    DARK;

    companion object {
        fun fromName(raw: String?): ThemeMode = when (raw?.lowercase()) {
            "light" -> LIGHT
            "dark" -> DARK
            else -> SYSTEM
        }
    }
}

/**
 * 应用主题入口。
 *
 * 默认采用 Indigo 语义令牌（与桌面端一致），
 * [dynamicColor] 开启时回退到 Android 12+ 动态取色。
 */
@Composable
fun SingboxwindowsTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val tokens = if (darkTheme) IndigoTokens.Dark else IndigoTokens.Light

    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> indigoDarkColorScheme(tokens)
        else -> indigoLightColorScheme(tokens)
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

    CompositionLocalProvider(LocalIndigoTokens provides tokens) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = IndigoTypography,
            shapes = IndigoShapes,
            content = content
        )
    }
}
