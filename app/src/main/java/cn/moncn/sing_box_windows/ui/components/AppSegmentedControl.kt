package cn.moncn.sing_box_windows.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import cn.moncn.sing_box_windows.ui.theme.AppRadius
import cn.moncn.sing_box_windows.ui.theme.LocalAppColors

/**
 * 分段控件（Segmented Control）。
 *
 * 用于「运行模式」「连接筛选」「日志级别」「主题」这类 2–4 个互斥选项。
 * 相比一排 FilterChip，分段控件在视觉上是一个整体控件，
 * 更适合表达「在若干选项中择一」而非「可多选筛选项」。
 *
 * @param options 选项列表
 * @param selected 当前选中项
 * @param onSelect 选中回调
 * @param label 选项文本
 */
@Composable
fun <T> AppSegmentedControl(
    options: List<T>,
    selected: T,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
    label: (T) -> String
) {
    val colors = LocalAppColors.current
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(colors.surfaceSunken, RoundedCornerShape(AppRadius.md))
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        options.forEach { option ->
            val isSelected = option == selected
            val background by animateColorAsState(
                // 选中态使用 surfaceElevated：亮色下为白底，暗色下比轨道明显更亮，
                // 保证两种主题下选中项都能被一眼识别。
                targetValue = if (isSelected) colors.surfaceElevated else colors.surfaceSunken,
                label = "segment_background"
            )
            val contentColor by animateColorAsState(
                targetValue = if (isSelected) colors.primary else colors.textTertiary,
                label = "segment_content"
            )
            val interactionSource = remember { MutableInteractionSource() }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(AppRadius.sm))
                    .background(background)
                    .clickable(
                        interactionSource = interactionSource,
                        indication = null,
                        role = Role.RadioButton,
                        onClick = { onSelect(option) }
                    )
                    .semantics { this.selected = isSelected }
                    .padding(vertical = 9.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = label(option),
                    style = MaterialTheme.typography.labelMedium,
                    color = contentColor
                )
            }
        }
    }
}
