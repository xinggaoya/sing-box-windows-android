package cn.moncn.sing_box_windows.ui.theme

/**
 * 主题模式。
 *
 * [SYSTEM] 跟随系统深浅色，[LIGHT] / [DARK] 由用户在设置页手动覆盖。
 * 覆盖结果持久化在本地偏好中，应用冷启动即生效。
 */
enum class ThemeMode(val storageKey: String, val label: String) {
    SYSTEM("system", "跟随系统"),
    LIGHT("light", "浅色"),
    DARK("dark", "深色");

    companion object {
        fun fromStorageKey(key: String?): ThemeMode =
            entries.firstOrNull { it.storageKey == key } ?: SYSTEM
    }
}
