package cn.moncn.sing_box_windows.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import cn.moncn.sing_box_windows.ui.theme.AppRadius
import cn.moncn.sing_box_windows.ui.theme.LocalAppColors

/**
 * 统一输入框。
 *
 * 统一圆角、描边与聚焦色，使设置页、订阅编辑对话框的表单观感一致。
 *
 * @param keyboardCapitalization 首字母大写策略。**URL 等大小写敏感的内容必须传
 *   [KeyboardCapitalization.None]**：输入法默认会把 `/` 后的首字母自动大写
 *   （`https://host/api/...` → `https://host/API/...`），导致地址失效。
 */
@Composable
fun AppTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    placeholder: String? = null,
    enabled: Boolean = true,
    singleLine: Boolean = true,
    keyboardType: KeyboardType = KeyboardType.Text,
    keyboardCapitalization: KeyboardCapitalization = KeyboardCapitalization.Sentences,
    autoCorrect: Boolean = true,
    visualTransformation: VisualTransformation = VisualTransformation.None
) {
    val colors = LocalAppColors.current
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        enabled = enabled,
        singleLine = singleLine,
        label = { Text(label) },
        placeholder = placeholder?.let { { Text(it) } },
        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
            keyboardType = keyboardType,
            capitalization = keyboardCapitalization,
            autoCorrect = autoCorrect
        ),
        visualTransformation = visualTransformation,
        shape = RoundedCornerShape(AppRadius.md),
        modifier = modifier.fillMaxWidth(),
        textStyle = MaterialTheme.typography.bodyLarge,
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = colors.primary,
            unfocusedBorderColor = colors.borderStrong,
            disabledBorderColor = colors.border,
            focusedContainerColor = colors.surfaceSunken,
            unfocusedContainerColor = colors.surfaceSunken,
            disabledContainerColor = colors.surfaceMuted,
            focusedTextColor = colors.textPrimary,
            unfocusedTextColor = colors.textPrimary,
            focusedLabelColor = colors.primary,
            unfocusedLabelColor = colors.textTertiary,
            cursorColor = colors.primary
        )
    )
}

/**
 * 下拉选择框：与 [SettingsRow] 同风格，整行可点击展开菜单。
 */
@Composable
fun AppDropdownField(
    label: String,
    selectedLabel: String,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    options: List<String>,
    modifier: Modifier = Modifier,
    optionLabels: Map<String, String> = emptyMap(),
    onSelected: (String) -> Unit
) {
    val colors = LocalAppColors.current
    Box(modifier = modifier) {
        SettingsRow(
            title = label,
            subtitle = selectedLabel,
            onClick = { onExpandedChange(true) },
            trailing = {
                androidx.compose.material3.Icon(
                    imageVector = Icons.Rounded.ExpandMore,
                    contentDescription = null,
                    tint = colors.textTertiary,
                    modifier = Modifier.size(20.dp)
                )
            }
        )
        androidx.compose.material3.DropdownMenu(
            expanded = expanded,
            onDismissRequest = { onExpandedChange(false) }
        ) {
            options.forEach { option ->
                androidx.compose.material3.DropdownMenuItem(
                    text = { Text(optionLabels[option] ?: option) },
                    onClick = {
                        onSelected(option)
                        onExpandedChange(false)
                    }
                )
            }
        }
    }
}

/** 进度条：品牌色 + 圆角轨道。 */
@Composable
fun AppProgressBar(
    progress: Float,
    modifier: Modifier = Modifier,
    trackColor: Color? = null
) {
    val colors = LocalAppColors.current
    androidx.compose.material3.LinearProgressIndicator(
        progress = { progress.coerceIn(0f, 1f) },
        modifier = modifier,
        color = colors.primary,
        trackColor = trackColor ?: colors.surfaceMuted
    )
}
