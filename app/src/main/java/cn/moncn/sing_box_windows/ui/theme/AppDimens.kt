package cn.moncn.sing_box_windows.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * 间距阶梯（4dp 网格）。
 *
 * 全站只允许使用这里定义的档位，杜绝「随手写 18.dp / 22.dp」造成的节奏断裂。
 */
object AppSpace {
    val xxs: Dp = 2.dp
    val xs: Dp = 4.dp
    val sm: Dp = 8.dp
    val md: Dp = 12.dp
    val lg: Dp = 16.dp
    val xl: Dp = 20.dp
    val xxl: Dp = 24.dp
    val x3l: Dp = 32.dp
    val x4l: Dp = 40.dp
    val x5l: Dp = 48.dp
}

/**
 * 圆角层级。
 *
 * 移动端整体比桌面端更圆润：卡片 18dp、控件 12dp、胶囊 pill，
 * 大圆角是当前移动端设计语言的趋势，同时提升可点击区域的视觉暗示。
 */
object AppRadius {
    val xs = 6.dp
    val sm = 10.dp
    val md = 14.dp
    val lg = 18.dp
    val xl = 22.dp
    val xxl = 28.dp
    val xxxl = 34.dp
    val pill = RoundedCornerShape(percent = 50)
}

/**
 * 布局常量。
 *
 * 统一页面级留白，保证所有页面左右边距、卡片间距、列表行间距完全一致。
 */
object AppLayout {
    /** 页面左右安全边距 */
    val gutter: Dp = 20.dp

    /** 页面上下留白 */
    val pagePadding: Dp = 20.dp

    /** 区块之间的纵向间距 */
    val sectionGap: Dp = 16.dp

    /** 卡片内部留白 */
    val cardPadding: Dp = 16.dp

    /** 紧凑卡片内部留白 */
    val cardPaddingCompact: Dp = 12.dp

    /** 列表行之间的间距 */
    val listGap: Dp = 10.dp

    /** 列表容器内部留白 */
    val listPadding: Dp = 10.dp

    /** 底部导航栏内容高度 */
    val bottomBarHeight: Dp = 64.dp

    /** 最小可点击区域（无障碍要求 ≥ 48dp） */
    val minTouchTarget: Dp = 48.dp

    /** 内容最大宽度：平板 / 横屏等宽屏下限制行长并居中 */
    val contentMaxWidth: Dp = 560.dp
}

/**
 * 阴影层级。
 *
 * 暗色主题下阴影几乎不可见，因此暗色下依靠描边而非阴影表达层级。
 */
object AppElevation {
    val none = 0.dp
    val low = 1.dp
    val medium = 4.dp
    val high = 12.dp

    /** 浮层（底栏）的投影高度 */
    val overlay = 16.dp
}

/** Material3 形状体系，映射到统一圆角阶梯 */
val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(AppRadius.xs),
    small = RoundedCornerShape(AppRadius.sm),
    medium = RoundedCornerShape(AppRadius.md),
    large = RoundedCornerShape(AppRadius.lg),
    extraLarge = RoundedCornerShape(AppRadius.xl)
)
