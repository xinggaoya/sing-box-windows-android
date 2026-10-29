package cn.moncn.sing_box_windows.ui.theme

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * 尺寸令牌（Size Tokens）——间距阶梯（4px 网格）。
 *
 * 与桌面端 `tokens.css` 的 `--space-*` 一一对应。
 */
object AppSpace {
    val xs: Dp = 4.dp
    val sm: Dp = 8.dp
    val md: Dp = 12.dp
    val lg: Dp = 16.dp
    val xl: Dp = 20.dp
    val xxl: Dp = 24.dp
    val x3l: Dp = 28.dp
    val x4l: Dp = 32.dp
    val x6l: Dp = 40.dp
    val x8l: Dp = 48.dp
}

/** 常用布局常量 */
object AppLayout {
    /** 底部导航栏水平外边距 */
    val bottomBarPadding = 18.dp

    /** 底部导航栏外边距（垂直） */
    val bottomBarVerticalPadding = 12.dp

    /** 内容区水平内边距 */
    val contentPadding = 16.dp

    /** 卡片内边距 */
    val cardPadding = 16.dp

    /** 列表项之间的间距 */
    val listItemSpacing = 10.dp
}
