package cn.moncn.sing_box_windows.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import cn.moncn.sing_box_windows.ui.theme.AppRadius
import cn.moncn.sing_box_windows.ui.theme.AppSpace
import cn.moncn.sing_box_windows.ui.theme.LocalAppColors

/**
 * 通用列表行。
 *
 * 结构：左侧图标底座 → 标题 + 副标题 → 右侧尾部区域。
 * 用于订阅项、连接项、节点项等列表场景。
 */
@Composable
fun AppListItem(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    leadingIcon: ImageVector? = null,
    leadingTint: Color? = null,
    trailing: (@Composable () -> Unit)? = null,
    onClick: (() -> Unit)? = null,
    selected: Boolean = false,
    showDivider: Boolean = true
) {
    val colors = LocalAppColors.current
    val container by animateColorAsState(
        targetValue = if (selected) colors.primaryContainer else colors.surface,
        label = "list_item_container"
    )
    val borderColor by animateColorAsState(
        targetValue = if (selected) colors.primaryBorder else colors.border,
        label = "list_item_border"
    )
    val shape = RoundedCornerShape(AppRadius.md)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(container, shape)
            .border(1.dp, borderColor, shape)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .then(
                    if (onClick != null) {
                        Modifier.clickable(role = Role.Button, onClick = onClick)
                    } else {
                        Modifier
                    }
                )
                .padding(horizontal = 14.dp, vertical = 13.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(AppSpace.md)
        ) {
            if (leadingIcon != null) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .background(
                            (leadingTint ?: colors.primary).copy(alpha = 0.13f),
                            RoundedCornerShape(AppRadius.sm)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    androidx.compose.material3.Icon(
                        imageVector = leadingIcon,
                        contentDescription = null,
                        tint = leadingTint ?: colors.primary,
                        modifier = Modifier.size(17.dp)
                    )
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    color = colors.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.textTertiary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            trailing?.invoke()
        }
        if (showDivider) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 14.dp)
                    .height(1.dp)
                    .background(colors.borderSubtle)
            )
        }
    }
}

/**
 * 单选指示器：选中态为实心圆点 + 描边环，未选中为空心圆环。
 * 用于节点选择等单选场景，比默认 RadioButton 更轻量。
 */
@Composable
fun SelectionIndicator(
    selected: Boolean,
    modifier: Modifier = Modifier,
    accent: Color = LocalAppColors.current.primary
) {
    val colors = LocalAppColors.current
    Box(
        modifier = modifier
            .size(20.dp)
            .clip(CircleShape)
            .border(
                width = if (selected) 6.dp else 1.5.dp,
                color = if (selected) accent else colors.borderStrong,
                shape = CircleShape
            )
    )
}
