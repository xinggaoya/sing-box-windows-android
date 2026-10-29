package cn.moncn.sing_box_windows.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import cn.moncn.sing_box_windows.ui.theme.AppRadius
import cn.moncn.sing_box_windows.ui.theme.LocalIndigoTokens

/** 语义状态色，与桌面端的 success / warning / error / info / critical 一一对应 */
enum class StatusTone {
    NEUTRAL,
    PRIMARY,
    SUCCESS,
    WARNING,
    ERROR,
    INFO,
    CRITICAL;

    @Composable
    fun contentColor(): Color {
        val t = LocalIndigoTokens.current
        return when (this) {
            NEUTRAL -> t.textSecondary
            PRIMARY -> t.primary
            SUCCESS -> t.success
            WARNING -> t.warning
            ERROR -> t.error
            INFO -> t.info
            CRITICAL -> t.critical
        }
    }

    @Composable
    fun containerColor(): Color {
        val t = LocalIndigoTokens.current
        return when (this) {
            NEUTRAL -> t.bgSurface2
            PRIMARY -> t.primarySoft
            SUCCESS -> t.successSoft
            WARNING -> t.warningSoft
            ERROR -> t.errorSoft
            INFO -> t.infoSoft
            CRITICAL -> t.criticalSoft
        }
    }
}

/**
 * 状态标签：圆角胶囊 + 语义底色 + 语义文字色。
 *
 * 对应桌面端 `.chip` 视觉（`--chip-bg` / `--chip-text`）。
 */
@Composable
fun StatusChip(
    text: String,
    modifier: Modifier = Modifier,
    tone: StatusTone = StatusTone.NEUTRAL
) {
    val content = tone.contentColor()
    val container = tone.containerColor()
    Box(
        modifier = modifier
            .clip(AppRadius.pill)
            .background(container)
            .border(1.dp, content.copy(alpha = 0.18f), AppRadius.pill)
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = content
        )
    }
}
