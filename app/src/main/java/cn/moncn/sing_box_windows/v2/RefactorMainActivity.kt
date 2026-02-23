package cn.moncn.sing_box_windows.v2

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import cn.moncn.sing_box_windows.core.LibboxManager
import cn.moncn.sing_box_windows.update.UpdateManager
import cn.moncn.sing_box_windows.update.UpdateStore
import cn.moncn.sing_box_windows.ui.theme.SingboxwindowsTheme
import cn.moncn.sing_box_windows.v2.app.V2AppRoot
import cn.moncn.sing_box_windows.v2.core.di.V2Container

/**
 * v2 重构入口（M1 样板）。
 * 先独立并行开发，避免与旧页面相互影响。
 */
class RefactorMainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        LibboxManager.initialize(this)
        UpdateStore.init(this)
        enableEdgeToEdge()

        setContent {
            SingboxwindowsTheme {
                val appContext = LocalContext.current.applicationContext
                val graph = remember(appContext) {
                    V2Container.provideGraph(appContext)
                }
                V2AppRoot(graph = graph)
            }
        }

        // 保持与旧入口一致：启动后后台自动检查更新。
        UpdateManager.getInstance(this).autoCheckIfNeeded()
    }
}
