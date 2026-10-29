package cn.moncn.sing_box_windows.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

/**
 * Material3 配色方案，由 [IndigoTokens] 语义层映射而来。
 *
 * 映射策略：
 * - `primary` = Indigo 主色；`onPrimary` = 白色
 * - `secondary` = 信息色（桌面端以强调色为主，Android 端保留主色唯一强调）
 * - `surface*` = 三层背景（base / surface / surface2）
 * - `outline*` = 边框层级
 */
internal fun indigoLightColorScheme(t: IndigoTokens = IndigoTokens.Light) = lightColorScheme(
    primary = t.primary,
    onPrimary = t.textOnPrimary,
    primaryContainer = t.primarySoftStrong,
    onPrimaryContainer = t.chipText,

    secondary = t.info,
    onSecondary = t.textOnPrimary,
    secondaryContainer = t.infoSoft,
    onSecondaryContainer = t.textPrimary,

    tertiary = t.success,
    onTertiary = t.textOnPrimary,
    tertiaryContainer = t.successSoft,
    onTertiaryContainer = t.textPrimary,

    error = t.error,
    onError = Color.White,
    errorContainer = t.errorSoft,
    onErrorContainer = t.error,

    background = t.bgBase,
    onBackground = t.textPrimary,

    surface = t.bgSurface,
    onSurface = t.textPrimary,
    surfaceVariant = t.bgSurface2,
    onSurfaceVariant = t.textSecondary,

    surfaceContainerLowest = t.bgElevated,
    surfaceContainerLow = t.bgSurface,
    surfaceContainer = t.bgSurface2,
    surfaceContainerHigh = t.bgSurface2,
    surfaceContainerHighest = t.bgSurface2,

    outline = t.borderStrong,
    outlineVariant = t.border,

    scrim = t.bgOverlay
)

internal fun indigoDarkColorScheme(t: IndigoTokens = IndigoTokens.Dark) = darkColorScheme(
    primary = t.primary,
    onPrimary = t.textOnPrimary,
    primaryContainer = t.primarySoftStrong,
    onPrimaryContainer = t.chipText,

    secondary = t.info,
    onSecondary = t.textOnPrimary,
    secondaryContainer = t.infoSoft,
    onSecondaryContainer = t.textPrimary,

    tertiary = t.success,
    onTertiary = t.textOnPrimary,
    tertiaryContainer = t.successSoft,
    onTertiaryContainer = t.textPrimary,

    error = t.error,
    onError = Color.White,
    errorContainer = t.errorSoft,
    onErrorContainer = t.error,

    background = t.bgBase,
    onBackground = t.textPrimary,

    surface = t.bgSurface,
    onSurface = t.textPrimary,
    surfaceVariant = t.bgSurface2,
    onSurfaceVariant = t.textSecondary,

    surfaceContainerLowest = t.bgElevated,
    surfaceContainerLow = t.bgSurface,
    surfaceContainer = t.bgSurface2,
    surfaceContainerHigh = t.bgSurface2,
    surfaceContainerHighest = t.bgSurface2,

    outline = t.borderStrong,
    outlineVariant = t.border,

    scrim = t.bgOverlay
)
