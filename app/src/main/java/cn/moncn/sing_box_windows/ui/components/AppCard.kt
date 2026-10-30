package cn.moncn.sing_box_windows.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import cn.moncn.sing_box_windows.ui.theme.AppLayout
import cn.moncn.sing_box_windows.ui.theme.AppRadius
import cn.moncn.sing_box_windows.ui.theme.AppSpace
import cn.moncn.sing_box_windows.ui.theme.LocalAppColors

/**
 * 统一卡片容器。
 *
 * 视觉规则：
 * - 圆角统一 18dp，内边距统一 16dp；
 * - 亮色用 1px 描边 + 极轻投影表达层级，暗色下取消投影只留描边；
 * - 可点击时通过容器色变化给出明确的选中反馈。
 */
@Composable
fun AppCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    selected: Boolean = false,
    shape: Shape = RoundedCornerShape(AppRadius.lg),
    contentPadding: PaddingValues = PaddingValues(AppLayout.cardPadding),
    containerColor: Color? = null,
    borderColor: Color? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val colors = LocalAppColors.current
    val resolvedContainer = containerColor ?: if (selected) colors.primaryContainer else colors.surface
    val resolvedBorder = borderColor ?: if (selected) colors.primaryBorder else colors.border
    val animatedContainer by animateColorAsState(resolvedContainer, label = "app_card_container")
    val animatedBorder by animateColorAsState(resolvedBorder, label = "app_card_border")

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(animatedContainer, shape)
            .border(1.dp, animatedBorder, shape)
            .then(
                if (onClick != null) {
                    Modifier.clickable(onClick = onClick)
                } else {
                    Modifier
                }
            )
            .padding(contentPadding),
        verticalArrangement = Arrangement.spacedBy(AppSpace.md),
        content = content
    )
}

/**
 * 带标题的分区卡片。
 *
 * 标题区与内容区之间用统一间距建立节奏，
 * 右侧 [trailing] 插槽用于放置状态标签或操作按钮。
 */
@Composable
fun AppSectionCard(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    leadingIcon: ImageVector? = null,
    onClick: (() -> Unit)? = null,
    trailing: (@Composable RowScope.() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val colors = LocalAppColors.current
    AppCard(modifier = modifier, onClick = onClick) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(AppSpace.md),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (leadingIcon != null) {
                Icon(
                    imageVector = leadingIcon,
                    contentDescription = null,
                    tint = colors.primary,
                    modifier = Modifier.size(18.dp)
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = colors.textPrimary
                )
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.textTertiary
                    )
                }
            }
            trailing?.invoke(this)
        }
        content()
    }
}

/**
 * 品牌渐变卡片：用于首页连接 Hero 等需要强视觉锚点的场景。
 */
@Composable
fun GradientCard(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(AppRadius.xxl),
    gradient: Brush? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val colors = LocalAppColors.current
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(
                brush = gradient ?: Brush.verticalGradient(colors.heroGradient),
                shape = shape
            )
            .border(1.dp, colors.primaryBorder.copy(alpha = 0.55f), shape)
            .padding(AppLayout.cardPadding),
        content = content
    )
}
