package cn.moncn.sing_box_windows.screenshot

import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.AndroidComposeTestRule
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import cn.moncn.sing_box_windows.ui.components.AppBottomBar
import cn.moncn.sing_box_windows.ui.components.AppNavItem
import cn.moncn.sing_box_windows.ui.components.LocalBottomBarPadding
import cn.moncn.sing_box_windows.ui.theme.AppLayout
import cn.moncn.sing_box_windows.ui.theme.SingboxwindowsTheme
import cn.moncn.sing_box_windows.ui.theme.ThemeMode
import cn.moncn.sing_box_windows.v2.app.V2Route
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

/**
 * 界面截图基类。
 *
 * 通过 Robolectric 的原生图形模式在 JVM 上真实渲染 Compose 界面并导出 PNG，
 * 好处是无需真机 / 模拟器即可逐页目检视觉结果。
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w411dp-h891dp-xhdpi")
abstract class ScreenshotHarness {

    @get:Rule
    val composeRule: AndroidComposeTestRule<*, ComponentActivity> =
        createAndroidComposeRule<ComponentActivity>()

    /** 底部导航项（与 [V2AppRoot] 保持一致）。 */
    private val navItems = listOf(
        AppNavItem(V2Route.Home.route, V2Route.Home.title, V2Route.Home.icon),
        AppNavItem(V2Route.Proxy.route, V2Route.Proxy.title, V2Route.Proxy.icon),
        AppNavItem(V2Route.Connections.route, V2Route.Connections.title, V2Route.Connections.icon),
        AppNavItem(V2Route.Subscription.route, V2Route.Subscription.title, V2Route.Subscription.icon),
        AppNavItem(V2Route.Settings.route, V2Route.Settings.title, V2Route.Settings.icon)
    )

    /**
     * 渲染一个顶级页面（含悬浮底部导航）。
     */
    protected fun renderTopLevel(
        route: String,
        themeMode: ThemeMode,
        content: @Composable () -> Unit
    ) {
        composeRule.setContent {
            SingboxwindowsTheme(themeMode = themeMode) {
                CompositionLocalProvider(LocalBottomBarPadding provides AppLayout.bottomBarHeight) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        content()
                        AppBottomBar(
                            items = navItems,
                            currentRoute = route,
                            onNavigate = {},
                            modifier = Modifier.align(Alignment.BottomCenter)
                        )
                    }
                }
            }
        }
        composeRule.waitForIdle()
    }

    /**
     * 渲染一个二级页面（无底部导航）。
     */
    protected fun renderSecondary(
        themeMode: ThemeMode,
        content: @Composable () -> Unit
    ) {
        composeRule.setContent {
            SingboxwindowsTheme(themeMode = themeMode) {
                CompositionLocalProvider(LocalBottomBarPadding provides 0.dpValue()) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        content()
                    }
                }
            }
        }
        composeRule.waitForIdle()
    }

    /** 把当前界面渲染结果导出为 PNG。 */
    protected fun capture(name: String) {
        val view = composeRule.activity.window.decorView
        check(view.width > 0 && view.height > 0) { "视图尺寸异常：${view.width}x${view.height}" }
        val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
        view.draw(Canvas(bitmap))
        val dir = File(OUTPUT_DIR).apply { mkdirs() }
        File(dir, "$name.png").outputStream().use {
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
        }
        println("SCREENSHOT_OK $name ${bitmap.width}x${bitmap.height}")
    }

    companion object {
        const val OUTPUT_DIR = "build/screenshots"
    }
}

private fun Int.dpValue(): androidx.compose.ui.unit.Dp = androidx.compose.ui.unit.Dp(this.toFloat())
