package cn.moncn.sing_box_windows.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * 原始色板（Primitive Palette）——设计系统的第一层。
 *
 * 只包含固定的基础色阶，不随明暗主题变化。
 * 业务代码不应直接引用本文件，统一走 [AppColors] 语义层。
 *
 * 色相选择遵循两个原则：
 * 1. **品牌延续**：Indigo 主色阶与桌面端 `sing-box-windows` 的 `tokens.css` 保持一致，
 *    保证同一品牌在桌面与移动端的品牌识别一致。
 * 2. **对比度达标**：所有用于正文的色阶在对应背景上的对比度均 ≥ 4.5:1（WCAG AA）。
 */
object AppPalette {

    // ---------------- Indigo 强调色阶（主品牌色） ----------------
    val Indigo50 = Color(0xFFEEF2FF)
    val Indigo100 = Color(0xFFE0E7FF)
    val Indigo200 = Color(0xFFC7D2FE)
    val Indigo300 = Color(0xFFA5B4FC)
    val Indigo400 = Color(0xFF818CF8)
    val Indigo500 = Color(0xFF6366F1)
    val Indigo600 = Color(0xFF4F46E5)
    val Indigo700 = Color(0xFF4338CA)
    val Indigo800 = Color(0xFF3730A3)
    val Indigo900 = Color(0xFF312E81)

    // ---------------- Slate 中性灰阶 ----------------
    val Slate50 = Color(0xFFF8FAFC)
    val Slate100 = Color(0xFFF1F5F9)
    val Slate200 = Color(0xFFE2E8F0)
    val Slate300 = Color(0xFFCBD5E1)
    val Slate400 = Color(0xFF94A3B8)
    val Slate500 = Color(0xFF64748B)
    val Slate600 = Color(0xFF475569)
    val Slate700 = Color(0xFF334155)
    val Slate800 = Color(0xFF1E293B)
    val Slate900 = Color(0xFF0F172A)
    val Slate950 = Color(0xFF020617)

    // ---------------- 语义功能色阶 ----------------
    val Green50 = Color(0xFFECFDF5)
    val Green100 = Color(0xFFD1FAE5)
    val Green300 = Color(0xFF6EE7B7)
    val Green400 = Color(0xFF34D399)
    val Green600 = Color(0xFF059669)
    val Green700 = Color(0xFF047857)
    val Green900 = Color(0xFF064E3B)

    val Amber100 = Color(0xFFFEF3C7)
    val Amber300 = Color(0xFFFCD34D)
    val Amber400 = Color(0xFFFBBF24)
    val Amber600 = Color(0xFFD97706)
    val Amber700 = Color(0xFFB45309)
    val Amber900 = Color(0xFF78350F)

    val Red50 = Color(0xFFFEF2F2)
    val Red100 = Color(0xFFFEE2E2)
    val Red300 = Color(0xFFFCA5A5)
    val Red400 = Color(0xFFF87171)
    val Red600 = Color(0xFFDC2626)
    val Red700 = Color(0xFFB91C1C)
    val Red900 = Color(0xFF7F1D1D)

    val Blue50 = Color(0xFFEFF6FF)
    val Blue100 = Color(0xFFDBEAFE)
    val Blue300 = Color(0xFF93C5FD)
    val Blue400 = Color(0xFF60A5FA)
    val Blue600 = Color(0xFF2563EB)
    val Blue700 = Color(0xFF1D4ED8)
    val Blue900 = Color(0xFF1E3A8A)

    val Orange400 = Color(0xFFFB923C)
    val Orange600 = Color(0xFFEA580C)
    val Orange900 = Color(0xFF7C2D12)
}
