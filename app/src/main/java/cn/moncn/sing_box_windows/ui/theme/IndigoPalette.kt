package cn.moncn.sing_box_windows.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * 原始色板（Primitive Palette）。
 *
 * 与桌面端 `sing-box-windows` 的 `src/assets/tokens.css` 第一层完全对齐。
 * 该层为固定值，不随明暗主题变化；业务代码应只引用语义层与尺寸层。
 */
object IndigoPalette {

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
    val Green400 = Color(0xFF34D399)
    val Green500 = Color(0xFF10B981)
    val Green600 = Color(0xFF059669)

    val Amber50 = Color(0xFFFFFBEB)
    val Amber400 = Color(0xFFFBBF24)
    val Amber500 = Color(0xFFF59E0B)
    val Amber600 = Color(0xFFD97706)

    val Red50 = Color(0xFFFEF2F2)
    val Red400 = Color(0xFFF87171)
    val Red500 = Color(0xFFEF4444)
    val Red600 = Color(0xFFDC2626)

    val Blue50 = Color(0xFFEFF6FF)
    val Blue400 = Color(0xFF60A5FA)
    val Blue500 = Color(0xFF3B82F6)
    val Blue600 = Color(0xFF2563EB)

    /** 仅用于内核崩溃等特殊状态 */
    val Orange500 = Color(0xFFF97316)
}
