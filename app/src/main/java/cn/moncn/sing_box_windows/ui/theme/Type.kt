package cn.moncn.sing_box_windows.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.sp
import cn.moncn.sing_box_windows.R

/**
 * 品牌展示字体：Space Grotesk（与桌面端的 `Inter` 承担同等的标题职责）。
 * 仅用于 display / headline / title，正文使用系统默认无衬线字体以保证小字号可读性。
 */
val SpaceGrotesk = FontFamily(
    Font(R.font.space_grotesk, FontWeight.Normal),
    Font(R.font.space_grotesk, FontWeight.Medium),
    Font(R.font.space_grotesk, FontWeight.SemiBold),
    Font(R.font.space_grotesk, FontWeight.Bold)
)

/** 等宽字体，用于日志、命令行、配置片段 */
val MonoFont = FontFamily.Monospace

private val Default = FontFamily.Default

private val TightLineHeight = LineHeightStyle(
    alignment = LineHeightStyle.Alignment.Center,
    trim = LineHeightStyle.Trim.None
)

/**
 * 排版令牌，对齐桌面端 `tokens.css` 的 `--text-*` 阶梯：
 * xs 12 / sm 13 / base 14 / md 15 / lg 17 / xl 20 / 2xl 24 / 3xl 30。
 */
object AppTextSize {
    val xs = 12.sp
    val sm = 13.sp
    val base = 14.sp
    val md = 15.sp
    val lg = 17.sp
    val xl = 20.sp
    val x2l = 24.sp
    val x3l = 30.sp
}

val IndigoTypography = Typography(
    // ---------------- Display ----------------
    displayLarge = TextStyle(
        fontFamily = SpaceGrotesk,
        fontWeight = FontWeight.SemiBold,
        fontSize = AppTextSize.x3l,
        lineHeight = 38.sp,
        letterSpacing = (-0.4).sp
    ),
    displayMedium = TextStyle(
        fontFamily = SpaceGrotesk,
        fontWeight = FontWeight.SemiBold,
        fontSize = AppTextSize.x2l,
        lineHeight = 32.sp,
        letterSpacing = (-0.2).sp
    ),
    displaySmall = TextStyle(
        fontFamily = SpaceGrotesk,
        fontWeight = FontWeight.SemiBold,
        fontSize = AppTextSize.xl,
        lineHeight = 28.sp
    ),

    // ---------------- Headline ----------------
    headlineLarge = TextStyle(
        fontFamily = SpaceGrotesk,
        fontWeight = FontWeight.SemiBold,
        fontSize = AppTextSize.xl,
        lineHeight = 28.sp
    ),
    headlineMedium = TextStyle(
        fontFamily = SpaceGrotesk,
        fontWeight = FontWeight.SemiBold,
        fontSize = AppTextSize.lg,
        lineHeight = 24.sp
    ),
    headlineSmall = TextStyle(
        fontFamily = SpaceGrotesk,
        fontWeight = FontWeight.Medium,
        fontSize = AppTextSize.md,
        lineHeight = 22.sp
    ),

    // ---------------- Title ----------------
    titleLarge = TextStyle(
        fontFamily = SpaceGrotesk,
        fontWeight = FontWeight.SemiBold,
        fontSize = AppTextSize.lg,
        lineHeight = 24.sp
    ),
    titleMedium = TextStyle(
        fontFamily = Default,
        fontWeight = FontWeight.Medium,
        fontSize = AppTextSize.md,
        lineHeight = 21.sp
    ),
    titleSmall = TextStyle(
        fontFamily = Default,
        fontWeight = FontWeight.Medium,
        fontSize = AppTextSize.base,
        lineHeight = 20.sp
    ),

    // ---------------- Body ----------------
    bodyLarge = TextStyle(
        fontFamily = Default,
        fontWeight = FontWeight.Normal,
        fontSize = AppTextSize.md,
        lineHeight = 22.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = Default,
        fontWeight = FontWeight.Normal,
        fontSize = AppTextSize.base,
        lineHeight = 20.sp
    ),
    bodySmall = TextStyle(
        fontFamily = Default,
        fontWeight = FontWeight.Normal,
        fontSize = AppTextSize.sm,
        lineHeight = 18.sp
    ),

    // ---------------- Label ----------------
    labelLarge = TextStyle(
        fontFamily = Default,
        fontWeight = FontWeight.Medium,
        fontSize = AppTextSize.base,
        lineHeight = 18.sp
    ),
    labelMedium = TextStyle(
        fontFamily = Default,
        fontWeight = FontWeight.Medium,
        fontSize = AppTextSize.sm,
        lineHeight = 16.sp
    ),
    labelSmall = TextStyle(
        fontFamily = Default,
        fontWeight = FontWeight.Normal,
        fontSize = AppTextSize.xs,
        lineHeight = 14.sp
    )
)

/** 日志 / 配置片段使用的等宽样式 */
val MonoTextStyle = TextStyle(
    fontFamily = MonoFont,
    fontWeight = FontWeight.Normal,
    fontSize = AppTextSize.sm,
    lineHeight = 18.sp,
    lineHeightStyle = TightLineHeight
)
