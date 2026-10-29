package cn.moncn.sing_box_windows.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/**
 * 尺寸令牌（Size Tokens）——圆角层级。
 *
 * 与桌面端 `tokens.css` 的 `--radius-*` 一一对应。
 */
object AppRadius {
    val xs = 6.dp
    val sm = 8.dp
    val md = 12.dp
    val lg = 16.dp
    val xl = 20.dp
    val xxl = 24.dp
    val pill = RoundedCornerShape(percent = 50)
}

/** Material3 形状体系，映射到 Indigo 圆角阶梯 */
val IndigoShapes = Shapes(
    extraSmall = RoundedCornerShape(AppRadius.xs),
    small = RoundedCornerShape(AppRadius.sm),
    medium = RoundedCornerShape(AppRadius.md),
    large = RoundedCornerShape(AppRadius.lg),
    extraLarge = RoundedCornerShape(AppRadius.xl)
)
