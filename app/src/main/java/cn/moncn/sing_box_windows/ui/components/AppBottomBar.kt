package cn.moncn.sing_box_windows.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import cn.moncn.sing_box_windows.ui.theme.AppLayout
import cn.moncn.sing_box_windows.ui.theme.AppRadius
import cn.moncn.sing_box_windows.ui.theme.AppSpace
import cn.moncn.sing_box_windows.ui.theme.LocalAppColors

/** 底部导航项 */
data class AppNavItem(
    val route: String,
    val label: String,
    val icon: ImageVector
)

/**
 * 底部导航栏。
 *
 * 相比旧版的「自绘圆角药丸 + 图标文字横排」，本实现对齐 Material3 NavigationBar：
 * - 图标在上、文字在下，层级更清晰，5 个入口在窄屏也不会挤压；
 * - 选中态使用指示器底色 + 品牌色图标/文字，同时叠加语义标记供无障碍服务读取；
 * - 通过 [navigationBarsPadding] 适配全面屏手势条，
 *   旧实现未处理系统导航栏 inset，底栏会被手势区遮挡；
 * - 选中项有轻微放大反馈，交互更有生气。
 */
@Composable
fun AppBottomBar(
    items: List<AppNavItem>,
    currentRoute: String?,
    onNavigate: (AppNavItem) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalAppColors.current
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(colors.surfaceElevated)
            .navigationBarsPadding(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(colors.borderSubtle)
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(AppLayout.bottomBarHeight - 1.dp)
                .padding(horizontal = AppSpace.sm),
            horizontalArrangement = Arrangement.spacedBy(AppSpace.xs)
        ) {
            items.forEach { item ->
                val isSelected = currentRoute == item.route
                BottomBarItem(
                    item = item,
                    isSelected = isSelected,
                    onClick = { onNavigate(item) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun BottomBarItem(
    item: AppNavItem,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalAppColors.current
    val contentColor by animateColorAsState(
        targetValue = if (isSelected) colors.primary else colors.textTertiary,
        label = "bottom_bar_content"
    )
    val indicatorColor by animateColorAsState(
        targetValue = if (isSelected) colors.primaryContainer else Color.Transparent,
        label = "bottom_bar_indicator"
    )
    val scale by animateFloatAsState(
        targetValue = if (isSelected) 1f else 0.94f,
        label = "bottom_bar_scale"
    )
    val interactionSource = remember { MutableInteractionSource() }

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(AppRadius.md))
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                role = Role.Tab,
                onClick = onClick
            )
            .semantics {
                selected = isSelected
                contentDescription = item.label
            }
            .padding(vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(
            modifier = Modifier
                .height(30.dp)
                .background(indicatorColor, AppRadius.pill)
                .padding(horizontal = 14.dp),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = item.icon,
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier
                    .size(21.dp)
                    .scale(scale)
            )
        }
        Text(
            text = item.label,
            style = MaterialTheme.typography.labelMedium,
            color = contentColor,
            maxLines = 1
        )
    }
}
