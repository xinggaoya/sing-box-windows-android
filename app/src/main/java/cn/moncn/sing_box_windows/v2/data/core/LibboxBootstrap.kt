package cn.moncn.sing_box_windows.v2.data.core

import android.content.Context
import android.util.Log
import go.Seq
import io.nekohasekai.libbox.Libbox
import io.nekohasekai.libbox.SetupOptions
import java.util.Locale

/**
 * libbox 运行时引导。
 *
 * 必须在任何内核接口调用之前执行一次：
 * 设置 gomobile 上下文、本地化以及内核的目录、日志缓冲等全局选项。
 */
object LibboxBootstrap {
    private const val TAG = "LibboxBootstrap"

    @Volatile
    private var initialized = false

    /** 内核日志环形缓冲的最大行数 */
    private const val LOG_MAX_LINES = 2000L

    fun initialize(context: Context) {
        if (initialized) return
        synchronized(this) {
            if (initialized) return
            Seq.setContext(context)
            // 语言包匹配失败不应阻断初始化（内核会回退到英文）
            runCatching {
                Libbox.setLocale(Locale.getDefault().toLanguageTag().replace("-", "_"))
            }.onFailure { Log.w(TAG, "set locale failed, fallback to default", it) }

            val baseDir = context.filesDir.also { it.mkdirs() }
            val workingDir = context.getExternalFilesDir(null)?.also { it.mkdirs() } ?: baseDir
            val tempDir = context.cacheDir.also { it.mkdirs() }
            val options = SetupOptions().apply {
                basePath = baseDir.path
                workingPath = workingDir.path
                tempPath = tempDir.path
                fixAndroidStack = true
                // 0 表示使用 basePath 下的 unix socket 通信，不额外暴露 TCP 端口
                commandServerListenPort = 0
                logMaxLines = LOG_MAX_LINES
            }
            Libbox.setup(options)
            initialized = true
        }
    }
}
