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
 * 品牌字体：Space Grotesk。
 *
 * 仅用于「纯拉丁 / 纯数字」的场景（品牌字样、指标数值、品牌微标签），
 * 因为该字体不含中文字形，正文与标题一律使用系统字体，
 * 避免中英混排时出现字重、字高不一致的割裂感。
 */
val BrandFont = FontFamily(
    Font(R.font.space_grotesk, FontWeight.Normal),
    Font(R.font.space_grotesk, FontWeight.Medium),
    Font(R.font.space_grotesk, FontWeight.SemiBold),
    Font(R.font.space_grotesk, FontWeight.Bold)
)

/** 等宽字体，用于日志、命令行、配置片段 */
val MonoFont = FontFamily.Monospace

/** 系统字体：正文与标题统一使用，保证中英文混排的字重与行高一致。 */
private val SystemFont = FontFamily.Default

private val TightLineHeight = LineHeightStyle(
    alignment = LineHeightStyle.Alignment.Center,
    trim = LineHeightStyle.Trim.None
)

/**
 * 字号阶梯。
 *
 * 移动端相比桌面端整体下调一档（正文 14sp、标题 18/20sp），
 * 以适配小屏阅读距离，同时保留足够的层级差异。
 */
object AppTextSize {
    val xs = 11.sp
    val sm = 12.sp
    val base = 14.sp
    val md = 15.sp
    val lg = 17.sp
    val xl = 20.sp
    val x2l = 24.sp
    val x3l = 30.sp
}

/**
 * 排版令牌。
 *
 * 设计要点：
 * - 标题层级通过「字号 + 字重 + 字距」三重区分，避免依赖加粗制造层级；
 * - 所有正文色值由 `AppColors` 提供，此处只负责排版本身；
 * - 数值类文本使用等宽数字特性（`tnum`），避免数值跳动时左右抖动。
 */
val AppTypography = Typography(
    // ---------------- Display ----------------
    displayLarge = TextStyle(
        fontFamily = SystemFont,
        fontWeight = FontWeight.SemiBold,
        fontSize = AppTextSize.x3l,
        lineHeight = 38.sp,
        letterSpacing = (-0.6).sp
    ),
    displayMedium = TextStyle(
        fontFamily = SystemFont,
        fontWeight = FontWeight.SemiBold,
        fontSize = AppTextSize.x2l,
        lineHeight = 32.sp,
        letterSpacing = (-0.4).sp
    ),
    displaySmall = TextStyle(
        fontFamily = SystemFont,
        fontWeight = FontWeight.SemiBold,
        fontSize = AppTextSize.xl,
        lineHeight = 28.sp,
        letterSpacing = (-0.2).sp
    ),

    // ---------------- Headline ----------------
    headlineLarge = TextStyle(
        fontFamily = SystemFont,
        fontWeight = FontWeight.SemiBold,
        fontSize = AppTextSize.xl,
        lineHeight = 28.sp,
        letterSpacing = (-0.2).sp
    ),
    headlineMedium = TextStyle(
        fontFamily = SystemFont,
        fontWeight = FontWeight.SemiBold,
        fontSize = AppTextSize.lg,
        lineHeight = 24.sp,
        letterSpacing = (-0.1).sp
    ),
    headlineSmall = TextStyle(
        fontFamily = SystemFont,
        fontWeight = FontWeight.Medium,
        fontSize = AppTextSize.md,
        lineHeight = 22.sp
    ),

    // ---------------- Title ----------------
    titleLarge = TextStyle(
        fontFamily = SystemFont,
        fontWeight = FontWeight.SemiBold,
        fontSize = AppTextSize.lg,
        lineHeight = 24.sp
    ),
    titleMedium = TextStyle(
        fontFamily = SystemFont,
        fontWeight = FontWeight.SemiBold,
        fontSize = AppTextSize.md,
        lineHeight = 21.sp
    ),
    titleSmall = TextStyle(
        fontFamily = SystemFont,
        fontWeight = FontWeight.Medium,
        fontSize = AppTextSize.base,
        lineHeight = 20.sp
    ),

    // ---------------- Body ----------------
    bodyLarge = TextStyle(
        fontFamily = SystemFont,
        fontWeight = FontWeight.Normal,
        fontSize = AppTextSize.md,
        lineHeight = 22.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = SystemFont,
        fontWeight = FontWeight.Normal,
        fontSize = AppTextSize.base,
        lineHeight = 20.sp
    ),
    bodySmall = TextStyle(
        fontFamily = SystemFont,
        fontWeight = FontWeight.Normal,
        fontSize = AppTextSize.sm,
        lineHeight = 18.sp
    ),

    // ---------------- Label ----------------
    labelLarge = TextStyle(
        fontFamily = SystemFont,
        fontWeight = FontWeight.Medium,
        fontSize = AppTextSize.base,
        lineHeight = 18.sp
    ),
    labelMedium = TextStyle(
        fontFamily = SystemFont,
        fontWeight = FontWeight.Medium,
        fontSize = AppTextSize.sm,
        lineHeight = 16.sp
    ),
    labelSmall = TextStyle(
        fontFamily = SystemFont,
        fontWeight = FontWeight.Medium,
        fontSize = AppTextSize.xs,
        lineHeight = 14.sp,
        letterSpacing = 0.4.sp
    )
)

/** 品牌字样：应用名、启动标识 */
val BrandTextStyle = TextStyle(
    fontFamily = BrandFont,
    fontWeight = FontWeight.Bold,
    fontSize = AppTextSize.x2l,
    lineHeight = 30.sp,
    letterSpacing = (-0.6).sp
)

/** 指标数值：等宽数字，数值刷新时不抖动 */
val NumericTextStyle = TextStyle(
    fontFamily = BrandFont,
    fontWeight = FontWeight.SemiBold,
    fontSize = AppTextSize.xl,
    lineHeight = 26.sp,
    letterSpacing = (-0.3).sp,
    fontFeatureSettings = "tnum"
)

/** 指标数值的小号变体，用于三列紧凑布局 */
val NumericSmallTextStyle = NumericTextStyle.copy(
    fontSize = AppTextSize.lg,
    lineHeight = 24.sp
)

/** 日志 / 配置片段使用的等宽样式 */
val MonoTextStyle = TextStyle(
    fontFamily = MonoFont,
    fontWeight = FontWeight.Normal,
    fontSize = AppTextSize.sm,
    lineHeight = 18.sp,
    lineHeightStyle = TightLineHeight
)
