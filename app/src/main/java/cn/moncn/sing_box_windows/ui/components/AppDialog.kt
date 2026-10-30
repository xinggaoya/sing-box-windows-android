package cn.moncn.sing_box_windows.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cn.moncn.sing_box_windows.ui.theme.AppRadius
import cn.moncn.sing_box_windows.ui.theme.LocalAppColors

/** 对话框语气：普通 / 危险（删除等不可逆操作）。 */
enum class DialogTone { NORMAL, DANGER }

/**
 * 统一对话框。
 *
 * 全站所有弹窗（错误提示、操作结果、订阅编辑等）都通过该组件渲染，
 * 保证圆角、标题层级、按钮顺序与配色完全一致。
 */
@Composable
fun AppAlertDialog(
    title: String,
    onDismissRequest: () -> Unit,
    onConfirm: () -> Unit,
    modifier: Modifier = Modifier,
    message: String? = null,
    confirmText: String = "确定",
    dismissText: String? = "取消",
    tone: DialogTone = DialogTone.NORMAL,
    confirmEnabled: Boolean = true,
    content: (@Composable () -> Unit)? = null
) {
    val colors = LocalAppColors.current
    AlertDialog(
        onDismissRequest = onDismissRequest,
        modifier = modifier,
        shape = RoundedCornerShape(AppRadius.xl),
        containerColor = colors.surfaceElevated,
        titleContentColor = colors.textPrimary,
        textContentColor = colors.textSecondary,
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (message != null) {
                    Text(
                        text = message,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                content?.invoke()
            }
        },
        confirmButton = {
            TertiaryButton(
                text = confirmText,
                onClick = onConfirm,
                enabled = confirmEnabled,
                tint = if (tone == DialogTone.DANGER) colors.error.content else colors.primary
            )
        },
        dismissButton = {
            if (dismissText != null) {
                TertiaryButton(
                    text = dismissText,
                    onClick = onDismissRequest,
                    tint = colors.textTertiary
                )
            }
        }
    )
}

/**
 * 轻量提示条：用于「设置已保存」这类非阻断反馈，
 * 避免用模态对话框打断操作节奏。
 */
@Composable
fun AppInlineBanner(
    message: String,
    modifier: Modifier = Modifier,
    tone: StatusTone = StatusTone.INFO,
    onDismiss: (() -> Unit)? = null
) {
    val colors = LocalAppColors.current
    val palette = tone.palette()
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(palette.container, RoundedCornerShape(AppRadius.md))
            .padding(start = 14.dp, end = 6.dp, top = 10.dp, bottom = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodySmall,
            color = palette.onContainer,
            modifier = Modifier.weight(1f)
        )
        if (onDismiss != null) {
            IconActionButton(
                icon = Icons.Rounded.Close,
                contentDescription = "关闭提示",
                onClick = onDismiss,
                tint = palette.onContainer
            )
        }
    }
}
