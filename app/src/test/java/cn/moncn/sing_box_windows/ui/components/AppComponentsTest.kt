package cn.moncn.sing_box_windows.ui.components

import androidx.activity.ComponentActivity
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import cn.moncn.sing_box_windows.ui.theme.SingboxwindowsTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * 核心交互组件的 Compose UI 测试。
 *
 * 覆盖设计系统中最关键的三类交互：
 * 分段控件的单选语义、底部导航的选中态与回调、状态标签的文案呈现。
 *
 * 基于 Robolectric 原生图形模式在 JVM 上运行，无需真机 / 模拟器。
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w411dp-h891dp-xhdpi")
class AppComponentsTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun segmentedControlExposesSingleSelection() {
        // 使用可变状态：普通 var 不会触发重组，组件语义也就不会更新
        var selected by mutableStateOf("全部")
        composeRule.setContent {
            SingboxwindowsTheme {
                AppSegmentedControl(
                    options = listOf("全部", "活跃", "已关闭"),
                    selected = selected,
                    onSelect = { selected = it },
                    label = { it }
                )
            }
        }

        composeRule.onNodeWithText("全部").assertIsSelected()
        composeRule.onNodeWithText("活跃").assertIsNotSelected()

        composeRule.onNodeWithText("活跃").performClick()
        composeRule.waitForIdle()

        assertEquals("活跃", selected)
        composeRule.onNodeWithText("活跃").assertIsSelected()
        composeRule.onNodeWithText("全部").assertIsNotSelected()
    }

    @Test
    fun bottomBarReportsSelectedRouteAndHandlesClicks() {
        val items = listOf(
            AppNavItem("home", "首页", Icons.Rounded.Home),
            AppNavItem("settings", "设置", Icons.Rounded.Settings)
        )
        var current by mutableStateOf("home")
        var clicked by mutableStateOf<String?>(null)

        composeRule.setContent {
            SingboxwindowsTheme {
                AppBottomBar(
                    items = items,
                    currentRoute = current,
                    onNavigate = { clicked = it.route }
                )
            }
        }

        composeRule.onNodeWithText("首页").assertIsDisplayed()
        composeRule.onNodeWithText("设置").assertIsDisplayed()

        composeRule.onNodeWithText("设置").performClick()
        composeRule.waitForIdle()

        assertEquals("settings", clicked)
    }

    @Test
    fun statusChipShowsProvidedText() {
        composeRule.setContent {
            SingboxwindowsTheme {
                StatusChip(text = "已连接", tone = StatusTone.SUCCESS, showDot = true)
            }
        }
        composeRule.onNodeWithText("已连接").assertIsDisplayed()
    }

    @Test
    fun primaryButtonDisablesInteractionWhenNotEnabled() {
        var clicks by mutableStateOf(0)
        composeRule.setContent {
            SingboxwindowsTheme {
                PrimaryButton(text = "保存并应用", onClick = { clicks++ }, enabled = false)
            }
        }

        composeRule.onNodeWithText("保存并应用").assertIsDisplayed()
        composeRule.onNodeWithText("保存并应用").performClick()
        composeRule.waitForIdle()

        assertEquals("禁用状态不应触发点击", 0, clicks)
    }
}
