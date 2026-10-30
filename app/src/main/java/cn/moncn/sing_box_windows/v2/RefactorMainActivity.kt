package cn.moncn.sing_box_windows.v2

import android.Manifest
import android.content.Context
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cn.moncn.sing_box_windows.v2.data.update.UpdateManager
import cn.moncn.sing_box_windows.v2.data.core.LibboxBootstrap
import cn.moncn.sing_box_windows.v2.data.settings.ThemeStore
import cn.moncn.sing_box_windows.v2.data.update.UpdateStore
import cn.moncn.sing_box_windows.ui.theme.SingboxwindowsTheme
import cn.moncn.sing_box_windows.v2.app.V2AppRoot
import cn.moncn.sing_box_windows.v2.core.di.V2Container

/**
 * v2 重构入口。
 *
 * 主题模式在此处统一接管：主题偏好来自 [ThemeStore]，
 * 用户在设置页切换后立即生效，无需重启应用。
 */
class RefactorMainActivity : ComponentActivity() {

    /**
     * 通知权限申请器。
     *
     * Android 13+ 需要 `POST_NOTIFICATIONS` 才能展示通知，
     * 应用更新下载完成通知与 VPN 前台服务通知都依赖它。
     */
    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* 用户拒绝时仍可通过应用内的按钮完成安装，不做额外拦截 */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        LibboxBootstrap.initialize(this)
        UpdateStore.init(this)
        ThemeStore.init(this)
        enableEdgeToEdge()

        setContent {
            val themeMode by ThemeStore.themeMode.collectAsStateWithLifecycle()
            SingboxwindowsTheme(themeMode = themeMode) {
                val appContext = LocalContext.current.applicationContext
                val graph = remember(appContext) {
                    V2Container.provideGraph(appContext)
                }
                V2AppRoot(graph = graph)
            }
        }

        // 保持与旧入口一致：启动后后台自动检查更新。
        UpdateManager.getInstance(this).autoCheckIfNeeded()

        requestNotificationPermissionIfNeeded()
    }

    /**
     * 首次启动时申请通知权限（仅询问一次）。
     *
     * 未授权时更新流程仍可在应用内完成安装，VPN 也照常运行，
     * 只是通知不可见，因此拒绝不阻断任何功能。
     */
    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        val granted = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.POST_NOTIFICATIONS
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        if (granted) return
        if (getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getBoolean(KEY_NOTIFICATION_PERMISSION_REQUESTED, false)
        ) {
            return
        }
        getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_NOTIFICATION_PERMISSION_REQUESTED, true)
            .apply()
        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
    }

    private companion object {
        const val PREFS_NAME = "app_settings"
        const val KEY_NOTIFICATION_PERMISSION_REQUESTED = "notification_permission_requested"
    }
}
