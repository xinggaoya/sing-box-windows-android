package cn.moncn.sing_box_windows.v2.data.core

import android.os.Debug
import cn.moncn.sing_box_windows.v2.domain.core.MemorySnapshot
import java.io.File

/**
 * 进程内存诊断。
 *
 * 内核状态里上报的 `memory` 字段（`StatusMessage.memory`）在 Linux 上取自
 * `/proc/self/statm` 的 **RSS（进程常驻内存）**——它包含 ART 的 Java 堆、
 * Go 运行时堆以及 native 库代码页，**并不是「内核占了多少内存」**。
 *
 * 单看这个数字无法判断内存增长发生在哪一侧，因此这里把各部分拆开：
 *
 * | 指标 | 来源 | 含义 |
 * | --- | --- | --- |
 * | `rssBytes` | `/proc/self/statm` | 进程常驻内存（内核上报的就是这个） |
 * | `javaHeapUsed` | `Runtime` | ART Java 堆已使用量（Kotlin 侧） |
 * | `javaHeapCommitted` | `Runtime` | ART 已向系统申请的堆空间 |
 * | `nativeHeap` | `Debug` | native（malloc）堆，Go 运行时与 JNI 都算在内 |
 * | `pssKb` | `/proc/self/smaps_rollup` | 按比例分摊后的总占用，更贴近系统统计 |
 */
object ProcessMemory {

    /** 系统页大小（字节）：Android 15 起部分设备为 16KB 页 */
    private val PAGE_SIZE: Long = runCatching {
        android.system.Os.sysconf(android.system.OsConstants._SC_PAGESIZE)
    }.getOrDefault(4096L)

    /** 进程常驻内存（字节），与内核上报的 memory 字段同源 */
    fun rssBytes(): Long = readStatmField(1)

    /** ART Java 堆已使用量（字节） */
    fun javaHeapUsed(): Long {
        val runtime = Runtime.getRuntime()
        return runtime.totalMemory() - runtime.freeMemory()
    }

    /** ART Java 堆已提交量（字节），系统不会轻易归还这部分空间 */
    fun javaHeapCommitted(): Long = Runtime.getRuntime().totalMemory()

    /** native 堆使用量（字节），Go 运行时与 JNI 分配都在其中 */
    fun nativeHeap(): Long = Debug.getNativeHeapAllocatedSize()

    /** 按比例分摊后的总占用（KB），与系统「内存占用」统计口径一致 */
    fun pssKb(): Long = readLongField("/proc/self/smaps_rollup", "Pss:", 1)

    /** 一次性采集全部内存指标 */
    fun snapshot(): MemorySnapshot = MemorySnapshot(
        rssBytes = rssBytes(),
        javaHeapUsedBytes = javaHeapUsed(),
        javaHeapCommittedBytes = javaHeapCommitted(),
        nativeHeapBytes = nativeHeap(),
        pssKb = pssKb()
    )

    /**
     * 读取 `/proc/self/statm` 的第 index 个字段（单位：页）。
     * statm 字段顺序：size resident shared text lib data dt
     */
    private fun readStatmField(index: Int): Long {
        return try {
            val content = File("/proc/self/statm").readText().trim()
            val parts = content.split(Regex("\\s+"))
            val pages = parts.getOrNull(index)?.toLongOrNull() ?: return 0L
            pages * PAGE_SIZE
        } catch (_: Throwable) {
            0L
        }
    }

    /** 从 /proc 文件中读取形如 `Pss:  12345 kB` 的字段 */
    private fun readLongField(path: String, prefix: String, index: Int): Long {
        return try {
            File(path).useLines { lines ->
                lines.firstOrNull { it.startsWith(prefix) }
                    ?.split(Regex("\\s+"))
                    ?.getOrNull(index)
                    ?.toLongOrNull() ?: 0L
            }
        } catch (_: Throwable) {
            0L
        }
    }
}
