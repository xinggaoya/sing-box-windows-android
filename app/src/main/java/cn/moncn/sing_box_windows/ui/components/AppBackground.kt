package cn.moncn.sing_box_windows.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import cn.moncn.sing_box_windows.ui.theme.AppLayout
import cn.moncn.sing_box_windows.ui.theme.LocalAppColors

/**
 * 页面背景容器。
 *
 * 全站统一的背景策略：**纯色分层 + 顶部品牌光晕**。
 * 旧实现使用 `bgBase → bgSurface2 → bgBase` 的纵向渐变，
 * 在滚动过程中底色不断变化，既制造脏污的中间色带，也让卡片与背景的层级关系变模糊。
 *
 * 同时承担响应式适配：内容最大宽度限制为 [AppLayout.contentMaxWidth]，
 * 在平板 / 横屏等宽屏场景下内容水平居中，避免一行文字横跨整个屏幕导致行长过长；
 * 手机等窄屏场景下该约束不生效，布局与窄屏设计一致。
 *
 * @param glow 是否在顶部绘制品牌光晕，营造纵深感（首页等主页面开启）。
 */
@Composable
fun AppBackground(
    modifier: Modifier = Modifier,
    glow: Boolean = true,
    content: @Composable BoxScope.() -> Unit
) {
    val colors = LocalAppColors.current
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background)
            .then(
                if (glow) {
                    Modifier.drawBehind {
                        val radius = size.width * 0.85f
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    colors.heroGlow.copy(alpha = if (colors.isDark) 0.28f else 0.42f),
                                    Color.Transparent
                                ),
                                center = Offset(size.width * 0.5f, -size.height * 0.08f),
                                radius = radius
                            ),
                            radius = radius,
                            center = Offset(size.width * 0.5f, -size.height * 0.08f)
                        )
                    }
                } else {
                    Modifier
                }
            ),
        contentAlignment = Alignment.TopCenter
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = AppLayout.contentMaxWidth)
        ) {
            content()
        }
    }
}
