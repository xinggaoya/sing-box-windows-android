package cn.moncn.sing_box_windows.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = Teal400,
    secondary = Amber500,
    tertiary = Moss500,
    background = Ink900,
    surface = Ink700,
    onPrimary = Ink900,
    onSecondary = Ink900,
    onTertiary = Ink900,
    onBackground = Sand100,
    onSurface = Sand100,
    surfaceVariant = Ink700,
    onSurfaceVariant = Sand100
)

private val LightColorScheme = lightColorScheme(
    primary = Teal600,
    secondary = Amber500,
    tertiary = Moss500,
    background = Sand050,
    surface = Sand100,
    surfaceVariant = Sand200,
    onPrimary = Sand050,
    onSecondary = Sand050,
    onTertiary = Sand050,
    onBackground = Ink900,
    onSurface = Ink900,
    onSurfaceVariant = Ink700
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

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
