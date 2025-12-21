package cn.moncn.sing_box_windows.ui.theme

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

private val DarkColorScheme = darkColorScheme(
    primary = Ocean400,
    secondary = Mint400,
    tertiary = Coral400,
    background = Midnight950,
    surface = Midnight900,
    surfaceVariant = Midnight800,
    onPrimary = Midnight950,
    onSecondary = Midnight950,
    onTertiary = Midnight950,
    onBackground = Cloud050,
    onSurface = Cloud050,
    onSurfaceVariant = Cloud200,
    error = Rose500,
)

private val LightColorScheme = lightColorScheme(
    primary = Ocean600,
    secondary = Mint500,
    tertiary = Coral500,
    background = Cloud050, // Slightly off-white
    surface = Color.White,
    surfaceVariant = Cloud100,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = Midnight900,
    onSurface = Midnight900,
    onSurfaceVariant = Midnight800,
    error = Rose500,
)

@Composable
fun SingboxwindowsTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = true,
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

    // Optional: Edge-to-Edge status bar coloring could be handled here or in MainActivity.
    // Assuming MainActivity handles logic, but Composable side-effects are good for dynamic theme change.
    // For now, I will keep it simple as the original code didn't have heavy logic here.

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
