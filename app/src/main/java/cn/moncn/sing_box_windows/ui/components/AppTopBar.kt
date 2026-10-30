package cn.moncn.sing_box_windows.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cn.moncn.sing_box_windows.ui.theme.AppSpace
import cn.moncn.sing_box_windows.ui.theme.BrandTextStyle
import cn.moncn.sing_box_windows.ui.theme.LocalAppColors

/**
 * 页面页头（大标题模式）。
 *
 * 所有一级 / 二级页面统一使用该组件承载标题、副标题与右侧操作，
 * 避免每个页面各自拼装标题区造成字号、间距不一致。
 *
 * @param onBack 非空时显示返回按钮（用于日志 / 出站等二级页面）。
 * @param brand 标题使用品牌字体（仅用于纯拉丁文字样，如应用名）。
 * @param actions 右侧操作区。
 */
@Composable
fun AppTopBar(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    onBack: (() -> Unit)? = null,
    brand: Boolean = false,
    actions: @Composable RowScope.() -> Unit = {}
) {
    val colors = LocalAppColors.current
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = AppSpace.xs),
        horizontalArrangement = Arrangement.spacedBy(AppSpace.sm),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (onBack != null) {
            IconButton(onClick = onBack, modifier = Modifier.size(40.dp)) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                    contentDescription = "返回",
                    tint = colors.textSecondary
                )
            }
        }
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = title,
                style = if (brand) BrandTextStyle else MaterialTheme.typography.headlineLarge,
                color = colors.textPrimary
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.textTertiary
                )
            }
        }
        actions()
    }
}

/**
 * 紧凑页头：仅用于卡片内部或需要与内容并排的小标题区。
 */
@Composable
fun AppInlineHeader(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    trailing: (@Composable RowScope.() -> Unit)? = null
) {
    val colors = LocalAppColors.current
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
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
}
