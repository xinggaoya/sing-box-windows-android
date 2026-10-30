package cn.moncn.sing_box_windows.diag

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Debug
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.net.VpnService
import cn.moncn.sing_box_windows.v2.data.core.CoreCommandBridge
import cn.moncn.sing_box_windows.vpn.VpnController
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import java.io.File

/**
 * 诊断构建专用：进程内内存探针。
 *
 * 只存在于 `diagnostic` 构建（`src/diagnostic`），正式包不包含该类。
 *
 * 提供两类能力：
 * 1. 广播驱动连接 / 断开（绕开界面点击，便于自动化复现）；
 * 2. 采集**进程自身视角**的内存快照（ART 堆 / native 堆 / smaps 明细 / ART GC 统计），
 *    写入应用私有目录并输出到 logcat，用于定位内存增长发生在哪一侧。
 */
class DiagnosticReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            ACTION_CONNECT -> {
                val ctx = context.applicationContext
                // 走一遍系统 VPN 授权流程：prepare() 返回非空说明尚未授权，
                // 此时拉起系统确认弹窗；授权完成后再发一次广播即可启动。
                val prepareIntent = runCatching { VpnService.prepare(ctx) }.getOrNull()
                if (prepareIntent != null) {
                    runCatching { ctx.startActivity(prepareIntent) }
                    Log.i(TAG, "diag: vpn consent dialog requested")
                } else {
                    Log.i(TAG, "diag: connect requested")
                    VpnController.start(ctx)
                }
            }

            ACTION_DISCONNECT -> {
                Log.i(TAG, "diag: disconnect requested")
                VpnController.stop(context.applicationContext)
            }

            ACTION_MEM_ONESHOT -> {
                val text = MemoryProbe.snapshot(context, includeSmaps = true)
                Log.i(TAG, "diag mem:\n$text")
                MemoryProbe.dumpToFile(context, "mem-oneshot.txt", text)
            }

            ACTION_MEM_START -> {
                MemoryProbe.startSampling(context)
                Log.i(TAG, "diag: memory sampling started")
            }

            ACTION_MEM_STOP -> {
                MemoryProbe.stopSampling()
                Log.i(TAG, "diag: memory sampling stopped")
            }
        }
    }

    companion object {
        private const val TAG = "SingBoxDiag"

        const val ACTION_CONNECT = "cn.moncn.sing_box_windows.DIAG_CONNECT"
        const val ACTION_DISCONNECT = "cn.moncn.sing_box_windows.DIAG_DISCONNECT"
        const val ACTION_MEM_ONESHOT = "cn.moncn.sing_box_windows.DIAG_MEM_ONESHOT"
        const val ACTION_MEM_START = "cn.moncn.sing_box_windows.DIAG_MEM_START"
        const val ACTION_MEM_STOP = "cn.moncn.sing_box_windows.DIAG_MEM_STOP"
    }
}

/**
 * 进程内存探针实现。
 */
object MemoryProbe {

    private const val TAG = "SingBoxDiag"
    private const val SAMPLE_INTERVAL_MS = 2_000L

    private val handler = Handler(Looper.getMainLooper())

    @Volatile
    private var sampling = false

    private val ticker = object : Runnable {
        override fun run() {
            if (!sampling) return
            try {
                val text = snapshot(appContext, includeSmaps = false)
                val status = latestStatus
                val extra = if (status == null) {
                    "core| not-connected"
                } else {
                    "core| goroutines=${status.goroutines} connIn=${status.connectionsIn} " +
                        "connOut=${status.connectionsOut} coreMem=${status.memory} " +
                        "up=${status.uplinkTotal} down=${status.downlinkTotal}"
                }
                Log.i(TAG, "mem| $text$extra")
                appendToFile(appContext, "mem-samples.log", text + extra + "\n")
            } catch (error: Throwable) {
                Log.e(TAG, "mem sample failed", error)
            }
            handler.postDelayed(this, SAMPLE_INTERVAL_MS)
        }
    }

    /** 内核状态快照（协程数 / 连接数 / 内核上报内存） */
    @Volatile
    private var latestStatus: cn.moncn.sing_box_windows.v2.domain.core.CoreStatus? = null

    private val statusJob = kotlinx.coroutines.CoroutineScope(
        kotlinx.coroutines.Dispatchers.IO + kotlinx.coroutines.SupervisorJob()
    ).launch {
        CoreCommandBridge.gateway.statusFlow.collect { status ->
            latestStatus = status
        }
    }

    private lateinit var appContext: Context

    fun startSampling(context: Context) {
        appContext = context.applicationContext
        if (sampling) return
        sampling = true
        handler.post(ticker)
    }

    fun stopSampling() {
        sampling = false
        handler.removeCallbacks(ticker)
    }

    /**
     * 采集一次内存快照。
     *
     * @param includeSmaps 是否解析完整的 `/proc/self/smaps`（开销大，仅在需要归因时开启）
     */
    fun snapshot(context: Context, includeSmaps: Boolean): String {
        val sb = StringBuilder()
        sb.append("--- ").append(System.currentTimeMillis()).append(" ---\n")

        // 1) /proc/self/status：进程级内存与线程数
        val status = readFile("/proc/self/status")
        listOf("VmRSS:", "VmHWM:", "VmSize:", "VmData:", "VmSwap:", "Threads:").forEach { key ->
            status.lineSequence()
                .firstOrNull { it.startsWith(key) }
                ?.let { sb.append(it.trim()).append(" | ") }
        }
        sb.append('\n')

        // 2) /proc/self/statm：与内核上报的 memory 同源（RSS）
        sb.append("statm| ").append(readFile("/proc/self/statm").trim()).append('\n')

        // 3) /proc/self/smaps_rollup：按比例分摊的总量
        val rollup = readFile("/proc/self/smaps_rollup")
        if (rollup.isNotBlank()) {
            listOf("Rss:", "Pss:", "Private_Clean:", "Private_Dirty:", "Shared_Clean:", "Shared_Dirty:", "Anonymous:", "Swap:")
                .forEach { key ->
                    rollup.lineSequence()
                        .firstOrNull { it.startsWith(key) }
                        ?.let { sb.append(it.trim()).append(" | ") }
                }
            sb.append('\n')
        }

        // 4) ART 堆
        val runtime = Runtime.getRuntime()
        sb.append("javaHeap| used=").append(runtime.totalMemory() - runtime.freeMemory())
            .append(" committed=").append(runtime.totalMemory())
            .append(" max=").append(runtime.maxMemory())
            .append(" | nativeAlloc=").append(Debug.getNativeHeapAllocatedSize())
            .append(" nativeSize=").append(Debug.getNativeHeapSize())
            .append('\n')

        // 5) ART GC 统计：确认 GC 是否在跑、回收了多少
        val gcStats = runCatching { Debug.getRuntimeStats() }.getOrDefault(emptyMap())
        gcStats.entries
            .filter { (key, _) ->
                key.contains("gc", true) || key.contains("alloc", true) ||
                    key.contains("freed", true) || key.contains("bytes", true)
            }
            .take(8)
            .forEach { (key, value) -> sb.append("gc| ").append(key).append('=').append(value).append('\n') }

        // 6) 线程
        val threadNames = readFile("/proc/self/task").isBlank()
        sb.append("threads| ").append(threadNames).append('\n')

        // 7) smaps 明细：定位增长发生在哪一类映射
        if (includeSmaps) {
            val top = topMappingsByRss()
            if (top.isNotEmpty()) {
                sb.append("--- top smaps mappings by RSS ---\n")
                top.forEach { sb.append(it).append('\n') }
            }
        }

        return sb.toString()
    }

    /**
     * 解析 `/proc/self/smaps`，按 RSS 排序返回前 N 条映射。
     *
     * 用来区分：Go 堆（匿名映射）、ART 堆（[anon:dalvik-*]）、malloc（[anon:libc_malloc]）、
     * 代码页（.so/.apk/.dex）等，从而定位增长的具体来源。
     */
    private fun topMappingsByRss(limit: Int = 20): List<String> {
        val entries = mutableListOf<Pair<String, Long>>()
        val text = readFile("/proc/self/smaps")
        if (text.isBlank()) return emptyList()
        var header = ""
        val headerRegex = Regex("^[0-9a-f]+-[0-9a-f]+\\s")
        text.lineSequence().forEach { line ->
            // 映射头行形如 "7f00-7f01 rw-p 00000000 00:00 0"
            if (headerRegex.containsMatchIn(line)) {
                header = line
            }
            if (line.startsWith("Rss:")) {
                val kb = line.split(' ').getOrNull(1)?.toLongOrNull() ?: 0L
                if (kb > 0) {
                    val path = header.substringAfterLast(' ').trim().ifEmpty { "[anon]" }
                    entries += path to kb
                }
            }
        }
        return entries.groupBy({ it.first }, { it.second })
            .map { (path, values) -> "$path -> ${values.sum()} KB" }
            .sortedByDescending { it.substringAfter("-> ").substringBefore(" KB").trim().toLongOrNull() ?: 0L }
            .take(limit)
    }

    private fun readFile(path: String): String = runCatching {
        File(path).readText()
    }.getOrDefault("")

    fun dumpToFile(context: Context, name: String, text: String) {
        runCatching {
            val file = File(context.filesDir, name)
            file.writeText(text)
            Log.i(TAG, "diag mem written to ${file.absolutePath}")
        }
    }

    private fun appendToFile(context: Context, name: String, text: String) {
        runCatching {
            val file = File(context.filesDir, name)
            file.appendText(text + "\n")
        }
    }
}
