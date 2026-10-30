package cn.moncn.sing_box_windows.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import cn.moncn.sing_box_windows.ui.theme.AppRadius
import cn.moncn.sing_box_windows.ui.theme.AppSpace
import cn.moncn.sing_box_windows.ui.theme.LocalAppColors
import cn.moncn.sing_box_windows.ui.theme.NumericSmallTextStyle
import cn.moncn.sing_box_windows.ui.theme.NumericTextStyle

/**
 * 指标块：首页流量、内核信息、连接统计等场景复用。
 *
 * 结构为「图标 + 标签 / 数值 + 辅助说明」。
 * 数值使用品牌字体 + 等宽数字，刷新时不左右抖动。
 */
@Composable
fun MetricTile(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    accent: Color = LocalAppColors.current.primary,
    secondary: String? = null,
    compact: Boolean = false
) {
    val colors = LocalAppColors.current
    Column(
        modifier = modifier
            .background(colors.surfaceMuted, RoundedCornerShape(AppRadius.md))
            .padding(
                horizontal = if (compact) 12.dp else 14.dp,
                vertical = if (compact) 10.dp else 12.dp
            ),
        verticalArrangement = Arrangement.spacedBy(if (compact) 4.dp else 6.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            if (icon != null) {
                Box(
                    modifier = Modifier
                        .size(22.dp)
                        .background(accent.copy(alpha = 0.14f), RoundedCornerShape(AppRadius.xs)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = accent,
                        modifier = Modifier.size(13.dp)
                    )
                }
            }
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = colors.textTertiary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Text(
            text = value,
            style = if (compact) NumericSmallTextStyle else NumericTextStyle,
            color = colors.textPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        if (secondary != null) {
            Text(
                text = secondary,
                style = MaterialTheme.typography.bodySmall,
                color = colors.textTertiary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/** 指标行：等权两列 / 三列布局。 */
@Composable
fun MetricRow(
    modifier: Modifier = Modifier,
    gap: androidx.compose.ui.unit.Dp = AppSpace.md,
    content: @Composable RowScope.() -> Unit
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(gap),
        content = content
    )
}
