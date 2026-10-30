package cn.moncn.sing_box_windows.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import cn.moncn.sing_box_windows.ui.theme.AppRadius
import cn.moncn.sing_box_windows.ui.theme.AppSpace
import cn.moncn.sing_box_windows.ui.theme.LocalAppColors

/**
 * 空状态：图标底座 + 主文案 + 辅助说明 + 可选操作。
 *
 * 图标放在圆形浅底容器中，避免纯线框图标在大片留白中显得单薄。
 */
@Composable
fun EmptyState(
    icon: ImageVector,
    title: String,
    modifier: Modifier = Modifier,
    description: String? = null,
    action: (@Composable () -> Unit)? = null
) {
    val colors = LocalAppColors.current
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(AppSpace.xxl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .background(colors.surfaceMuted, RoundedCornerShape(AppRadius.xl)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = colors.textTertiary,
                modifier = Modifier.size(28.dp)
            )
        }
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = colors.textSecondary,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = AppSpace.lg)
        )
        if (description != null) {
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = colors.textTertiary,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = AppSpace.xs)
            )
        }
        if (action != null) {
            Box(modifier = Modifier.padding(top = AppSpace.xl)) { action() }
        }
    }
}
