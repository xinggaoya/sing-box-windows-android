package cn.moncn.sing_box_windows.v2.data.settings

import android.content.Context
import androidx.core.content.edit
import cn.moncn.sing_box_windows.ui.theme.ThemeMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * 主题模式偏好存储。
 *
 * 与内核配置（[cn.moncn.sing_box_windows.v2.domain.settings.AppSettings]）分开保存：
 * 外观属于纯 UI 偏好，不应写进 sing-box 配置文件，也不应触发「重新连接后生效」这类提示。
 *
 * 使用 [StateFlow] 暴露当前模式，设置页修改后界面立即重绘，无需重启。
 */
object ThemeStore {

    private const val PREFS_NAME = "app_appearance"
    private const val KEY_THEME_MODE = "theme_mode"

    @Volatile
    private var appContext: Context? = null

    private val _themeMode = MutableStateFlow(ThemeMode.SYSTEM)

    /** 当前主题模式，供 Activity 收集以驱动主题切换。 */
    val themeMode: StateFlow<ThemeMode> = _themeMode.asStateFlow()

    /**
     * 初始化：在 [androidx.activity.ComponentActivity.onCreate] 中调用，
     * 读取上次选择的主题模式。
     */
    fun init(context: Context) {
        if (appContext == null) {
            appContext = context.applicationContext
        }
        val stored = prefs()?.getString(KEY_THEME_MODE, null)
        _themeMode.value = ThemeMode.fromStorageKey(stored)
    }

    /** 切换主题模式并持久化。 */
    fun setMode(mode: ThemeMode) {
        prefs()?.edit { putString(KEY_THEME_MODE, mode.storageKey) }
        _themeMode.value = mode
    }

    private fun prefs() = appContext?.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
}
