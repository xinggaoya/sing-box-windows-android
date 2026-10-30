package cn.moncn.sing_box_windows.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import cn.moncn.sing_box_windows.ui.theme.AppRadius
import cn.moncn.sing_box_windows.ui.theme.LocalAppColors
import cn.moncn.sing_box_windows.ui.theme.StatusPalette

/** 语义状态色，与设计系统的状态色组一一对应。 */
enum class StatusTone {
    NEUTRAL,
    PRIMARY,
    SUCCESS,
    WARNING,
    ERROR,
    INFO,
    CRITICAL;

    @Composable
    fun palette(): StatusPalette {
        val colors = LocalAppColors.current
        return when (this) {
            NEUTRAL -> StatusPalette(
                content = colors.textSecondary,
                container = colors.surfaceMuted,
                onContainer = colors.textSecondary,
                border = colors.border
            )

            PRIMARY -> StatusPalette(
                content = colors.primary,
                container = colors.primaryContainer,
                onContainer = colors.onPrimaryContainer,
                border = colors.primaryBorder
            )

            SUCCESS -> colors.success
            WARNING -> colors.warning
            ERROR -> colors.error
            INFO -> colors.info
            CRITICAL -> colors.critical
        }
    }
}

/**
 * 状态标签：胶囊底色 + 语义边框 + 语义文字。
 *
 * @param showDot 在文字前附加一个小圆点，用于强调「运行中」这类持续状态，
 *                避免仅靠颜色传达信息（色觉障碍用户友好）。
 */
@Composable
fun StatusChip(
    text: String,
    modifier: Modifier = Modifier,
    tone: StatusTone = StatusTone.NEUTRAL,
    showDot: Boolean = false
) {
    val palette = tone.palette()
    Box(
        modifier = modifier
            .clip(AppRadius.pill)
            .background(palette.container)
            .border(1.dp, palette.border, AppRadius.pill)
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            if (showDot) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(AppRadius.pill)
                        .background(palette.content)
                )
            }
            Text(
                text = text,
                style = MaterialTheme.typography.labelSmall,
                color = palette.onContainer
            )
        }
    }
}

/** 纯文字强调标签，用于卡片内的弱提示（如「规则」「本地」） */
@Composable
fun InlineLabel(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = LocalAppColors.current.textTertiary
) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        color = color,
        modifier = modifier
    )
}
