package cn.moncn.sing_box_windows.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import cn.moncn.sing_box_windows.ui.theme.AppLayout

/**
 * 底部导航栏占用的高度（含系统手势条）。
 *
 * 由应用根节点注入，页面据此预留底部留白，
 * 使内容既不被导航栏遮挡，也不会与导航栏重叠。
 */
val LocalBottomBarPadding = staticCompositionLocalOf { 0.dp }

/**
 * 页面统一留白：顶部状态栏 + 底部导航栏 / 手势条 + 左右安全边距。
 *
 * 所有页面的滚动容器都使用该值，从而保证页面之间的
 * 左右边距、顶部起始位置、底部收尾空间完全一致。
 */
@Composable
fun appPagePadding(): PaddingValues {
    val topInset = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val navBarInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val bottomInset = LocalBottomBarPadding.current
    return PaddingValues(
        start = AppLayout.gutter,
        end = AppLayout.gutter,
        top = topInset + AppLayout.pagePadding,
        bottom = bottomInset + navBarInset + AppLayout.pagePadding
    )
}

/** 页面区块之间的纵向间距，全站统一。 */
val AppPageArrangement: Arrangement.Vertical
    get() = Arrangement.spacedBy(AppLayout.sectionGap)

/**
 * 页面容器（Column 变体）。
 *
 * 用于内部已经是滚动 Column 的页面（如日志终端面板），
 * 保持与 [appPagePadding] 完全一致的留白规则。
 */
@Composable
fun AppPageColumn(
    modifier: Modifier = Modifier,
    glow: Boolean = true,
    horizontalPadding: Dp = AppLayout.gutter,
    verticalPadding: Dp = AppLayout.pagePadding,
    content: @Composable ColumnScope.() -> Unit
) {
    val topInset = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val navBarInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val bottomInset = LocalBottomBarPadding.current

    AppBackground(glow = glow) {
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(
                    start = horizontalPadding,
                    end = horizontalPadding,
                    top = topInset + verticalPadding,
                    bottom = bottomInset + navBarInset + verticalPadding
                ),
            verticalArrangement = AppPageArrangement,
            content = content
        )
    }
}
