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
    primary = Ocean400,
    secondary = Coral400,
    tertiary = Mint400,
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
    onError = Midnight950
)

private val LightColorScheme = lightColorScheme(
    primary = Ocean600,
    secondary = Coral500,
    tertiary = Mint500,
    background = Cloud050,
    surface = Cloud000,
    surfaceVariant = Cloud100,
    onPrimary = Cloud000,
    onSecondary = Cloud000,
    onTertiary = Cloud000,
    onBackground = Midnight900,
    onSurface = Midnight900,
    onSurfaceVariant = Midnight800,
    error = Rose500,
    onError = Cloud000
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
