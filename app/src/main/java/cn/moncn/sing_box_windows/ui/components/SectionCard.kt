package cn.moncn.sing_box_windows.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import cn.moncn.sing_box_windows.ui.theme.AppLayout
import cn.moncn.sing_box_windows.ui.theme.AppRadius
import cn.moncn.sing_box_windows.ui.theme.LocalIndigoTokens

/**
 * 卡片容器：对应桌面端 `.panel`（surface 背景 + 细边框 + 小圆角）。
 */
@Composable
fun SectionCard(
    modifier: Modifier = Modifier,
    contentPadding: androidx.compose.foundation.layout.PaddingValues =
        androidx.compose.foundation.layout.PaddingValues(AppLayout.cardPadding),
    content: @Composable ColumnScope.() -> Unit
) {
    val t = LocalIndigoTokens.current
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(t.bgSurface, MaterialTheme.shapes.medium)
            .border(1.dp, t.border, MaterialTheme.shapes.medium)
            .padding(contentPadding),
        content = content
    )
}

/**
 * 带标题的分区卡片，标题行右侧可放置操作。
 */
@Composable
fun SectionCardWithHeader(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    trailing: (@Composable () -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val t = LocalIndigoTokens.current
    SectionCard(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = t.textPrimary
                )
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = t.textTertiary
                    )
                }
            }
            trailing?.invoke()
        }
        content()
    }
}

/** 卡片内的小节标题（用于卡片内的二级分组） */
@Composable
fun CardSectionLabel(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = LocalIndigoTokens.current.textTertiary
) {
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        color = color,
        modifier = modifier
    )
}
