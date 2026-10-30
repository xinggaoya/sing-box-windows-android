package cn.moncn.sing_box_windows.v2.data.settings

import cn.moncn.sing_box_windows.ui.theme.ThemeMode
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

/**
 * 主题模式持久化测试。
 *
 * 覆盖「设置页切换主题 → 应用重启后仍保持」这一关键链路。
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ThemeStoreTest {

    private val context get() = RuntimeEnvironment.getApplication()

    @Before
    fun setUp() {
        ThemeStore.init(context)
        ThemeStore.setMode(ThemeMode.SYSTEM)
    }

    @Test
    fun modeIsPersistedAcrossReinitialization() {
        ThemeStore.setMode(ThemeMode.DARK)
        assertEquals(ThemeMode.DARK, ThemeStore.themeMode.value)

        // 模拟冷启动：重新读取本地偏好
        ThemeStore.init(context)
        assertEquals(ThemeMode.DARK, ThemeStore.themeMode.value)

        ThemeStore.setMode(ThemeMode.LIGHT)
        ThemeStore.init(context)
        assertEquals(ThemeMode.LIGHT, ThemeStore.themeMode.value)
    }

    @Test
    fun modeEmitsUpdatesImmediately() {
        ThemeStore.setMode(ThemeMode.LIGHT)
        assertEquals(ThemeMode.LIGHT, ThemeStore.themeMode.value)
        ThemeStore.setMode(ThemeMode.SYSTEM)
        assertEquals(ThemeMode.SYSTEM, ThemeStore.themeMode.value)
    }
}
