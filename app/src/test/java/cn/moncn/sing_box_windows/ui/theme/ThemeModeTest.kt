package cn.moncn.sing_box_windows.ui.theme

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * 主题模式枚举的单元测试。
 */
class ThemeModeTest {

    @Test
    fun storageKeysAreStable() {
        assertEquals("system", ThemeMode.SYSTEM.storageKey)
        assertEquals("light", ThemeMode.LIGHT.storageKey)
        assertEquals("dark", ThemeMode.DARK.storageKey)
    }

    @Test
    fun fromStorageKeyFallsBackToSystem() {
        assertEquals(ThemeMode.SYSTEM, ThemeMode.fromStorageKey(null))
        assertEquals(ThemeMode.SYSTEM, ThemeMode.fromStorageKey(""))
        assertEquals(ThemeMode.SYSTEM, ThemeMode.fromStorageKey("unknown"))
    }

    @Test
    fun fromStorageKeyParsesKnownKeys() {
        assertEquals(ThemeMode.SYSTEM, ThemeMode.fromStorageKey("system"))
        assertEquals(ThemeMode.LIGHT, ThemeMode.fromStorageKey("light"))
        assertEquals(ThemeMode.DARK, ThemeMode.fromStorageKey("dark"))
    }

    @Test
    fun everyModeHasUserFacingLabel() {
        ThemeMode.entries.forEach { mode ->
            assertEquals(true, mode.label.isNotBlank())
        }
    }
}
