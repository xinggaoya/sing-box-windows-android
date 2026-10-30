package cn.moncn.sing_box_windows.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import cn.moncn.sing_box_windows.ui.theme.AppRadius
import cn.moncn.sing_box_windows.ui.theme.LocalAppColors

/**
 * 主按钮：品牌渐变填充。
 *
 * 渐变让主操作在众多次要操作中一眼可辨；
 * [loading] 态会禁用点击并显示进度指示，避免重复提交。
 */
@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    enabled: Boolean = true,
    loading: Boolean = false
) {
    val colors = LocalAppColors.current
    val interactive = enabled && !loading
    val contentColor = if (interactive) colors.onPrimary else colors.textDisabled
    val shape = RoundedCornerShape(AppRadius.md)
    Box(
        modifier = modifier
            .clip(shape)
            .background(
                brush = if (interactive) {
                    Brush.horizontalGradient(colors.brandGradient)
                } else {
                    Brush.horizontalGradient(listOf(colors.surfaceMuted, colors.surfaceMuted))
                },
                shape = shape
            )
            .clickable(
                enabled = interactive,
                role = Role.Button,
                onClick = onClick
            )
            .padding(horizontal = 20.dp, vertical = 13.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (loading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    strokeWidth = 2.dp,
                    color = contentColor
                )
                Text(
                    text = text,
                    style = MaterialTheme.typography.labelLarge,
                    color = contentColor,
                    modifier = Modifier.padding(start = 10.dp)
                )
            } else {
                if (icon != null) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = contentColor,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Text(
                    text = text,
                    style = MaterialTheme.typography.labelLarge,
                    color = contentColor,
                    modifier = if (icon != null) Modifier.padding(start = 8.dp) else Modifier
                )
            }
        }
    }
}

/** 次按钮：描边样式，视觉权重低于主按钮。 */
@Composable
fun SecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    enabled: Boolean = true,
    loading: Boolean = false
) {
    val colors = LocalAppColors.current
    OutlinedButton(
        onClick = onClick,
        enabled = enabled && !loading,
        modifier = modifier,
        shape = RoundedCornerShape(AppRadius.md),
        border = androidx.compose.foundation.BorderStroke(1.dp, colors.borderStrong),
        colors = androidx.compose.material3.ButtonDefaults.outlinedButtonColors(
            contentColor = colors.textPrimary,
            disabledContentColor = colors.textDisabled
        ),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp)
    ) {
        if (loading) {
            CircularProgressIndicator(
                modifier = Modifier.size(16.dp),
                strokeWidth = 2.dp,
                color = colors.primary
            )
        } else {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = colors.textSecondary,
                    modifier = Modifier.size(18.dp)
                )
            }
            Text(
                text = text,
                style = MaterialTheme.typography.labelLarge,
                modifier = if (icon != null) Modifier.padding(start = 8.dp) else Modifier
            )
        }
    }
}

/** 文字按钮：用于卡片内的次要操作。 */
@Composable
fun TertiaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    enabled: Boolean = true,
    tint: Color? = null
) {
    val colors = LocalAppColors.current
    TextButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier,
        shape = RoundedCornerShape(AppRadius.sm),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
    ) {
        val contentColor = tint ?: colors.primary
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (enabled) contentColor else colors.textDisabled,
                modifier = Modifier.size(16.dp)
            )
        }
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            color = if (enabled) contentColor else colors.textDisabled,
            modifier = if (icon != null) Modifier.padding(start = 6.dp) else Modifier
        )
    }
}

/**
 * 图标操作按钮：统一 40dp 视觉尺寸，触控区域由 IconButton 保证 ≥48dp。
 */
@Composable
fun IconActionButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color? = null,
    enabled: Boolean = true
) {
    val colors = LocalAppColors.current
    IconButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.size(40.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = when {
                !enabled -> colors.textDisabled
                tint != null -> tint
                else -> colors.textSecondary
            },
            modifier = Modifier.size(20.dp)
        )
    }
}

/**
 * 图标块按钮：用于首页快捷入口等需要「图标 + 文字」垂直排布的场景。
 */
@Composable
fun IconTileButton(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    accent: Color? = null
) {
    val colors = LocalAppColors.current
    val tint = accent ?: colors.primary
    val shape = RoundedCornerShape(AppRadius.md)
    Column(
        modifier = modifier
            .background(colors.surfaceMuted, shape)
            .border(1.dp, colors.borderSubtle, shape)
            .clip(shape)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .background(tint.copy(alpha = 0.14f), RoundedCornerShape(AppRadius.sm)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(19.dp)
            )
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = colors.textSecondary
        )
    }
}

/** 横向按钮组：等分排列。 */
@Composable
fun ButtonRow(
    modifier: Modifier = Modifier,
    content: @Composable RowScope.() -> Unit
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        content = content
    )
}
