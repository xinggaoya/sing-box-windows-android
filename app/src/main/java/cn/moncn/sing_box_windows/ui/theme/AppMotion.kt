package cn.moncn.sing_box_windows.ui.theme

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.tween

/**
 * 动效令牌。
 *
 * 动效纪律：
 * - 微交互（按下、选中、展开）控制在 120–220ms，退出比进入更快；
 * - 页面级转场控制在 260–320ms；
 * - 同一屏幕最多一处持续动画（首页连接按钮呼吸光晕），避免视觉噪音；
 * - 进入用 ease-out（减速进场），退出用 ease-in（加速离场）。
 */
object AppMotion {

    /** 极快：按下反馈、颜色过渡 */
    val fast: Int = 120

    /** 标准：卡片展开、筛选切换、列表项入场 */
    val normal: Int = 220

    /** 慢速：页面转场 */
    val slow: Int = 300

    /** 退出动效统一比进入更快，界面感觉更「利落」 */
    val exit: Int = 160

    /** 元素入场：由下向上 + 淡入 */
    val easingEnter: Easing = CubicBezierEasing(0f, 0f, 0.2f, 1f)

    /** 元素退场：加速离场 */
    val easingExit: Easing = CubicBezierEasing(0.4f, 0f, 1f, 1f)

    fun <T> enterTween(durationMillis: Int = normal) = tween<T>(
        durationMillis = durationMillis,
        easing = easingEnter
    )

    fun <T> exitTween(durationMillis: Int = exit) = tween<T>(
        durationMillis = durationMillis,
        easing = easingExit
    )
}
