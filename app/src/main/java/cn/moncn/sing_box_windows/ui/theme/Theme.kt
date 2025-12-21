package cn.moncn.sing_box_windows.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = Sky400,
    onPrimary = Ink950,
    primaryContainer = Color(0xFF1E3D6B),
    onPrimaryContainer = Color(0xFFD6E6FF),
    secondary = Teal400,
    onSecondary = Color(0xFF00302A),
    secondaryContainer = Color(0xFF114640),
    onSecondaryContainer = Color(0xFFC9F2EA),
    tertiary = Amber400,
    onTertiary = Color(0xFF3A1E00),
    tertiaryContainer = Color(0xFF4A2A00),
    onTertiaryContainer = Color(0xFFFFE6CC),
    background = Ink950,
    surface = Ink900,
    surfaceVariant = Ink800,
    onBackground = Color(0xFFE7EEF5),
    onSurface = Color(0xFFE7EEF5),
    onSurfaceVariant = Color(0xFFB5C3D1),
    outline = Color(0xFF3A4B58),
    error = Rose400,
    onError = Color(0xFF450A0A),
    errorContainer = Color(0xFF7F1D1D),
    onErrorContainer = Color(0xFFFEE2E2)
)

private val LightColorScheme = lightColorScheme(
    primary = Sky600,
    onPrimary = Sand000,
    primaryContainer = Color(0xFFD6E6FF),
    onPrimaryContainer = Color(0xFF0A1C36),
    secondary = Teal500,
    onSecondary = Sand000,
    secondaryContainer = Color(0xFFC9F2EA),
    onSecondaryContainer = Color(0xFF003A34),
    tertiary = Amber500,
    onTertiary = Color(0xFF1A0C00),
    tertiaryContainer = Color(0xFFFFE6CC),
    onTertiaryContainer = Color(0xFF4A2A00),
    background = Sand050,
    surface = Sand000,
    surfaceVariant = Sand100,
    onBackground = Ink900,
    onSurface = Ink900,
    onSurfaceVariant = Ink800,
    outline = Color(0xFFB5C0CC),
    error = Rose500,
    onError = Sand000,
    errorContainer = Color(0xFFFEE2E2),
    onErrorContainer = Color(0xFF7A1010)
)

@Composable
fun SingboxwindowsTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> DarkColorScheme
        else -> LightColorScheme
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

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
