package cn.moncn.sing_box_windows.v2.data.core

import android.os.Debug
import cn.moncn.sing_box_windows.v2.domain.core.MemorySnapshot
import java.io.BufferedReader
import java.io.File

/**
 * 进程内存诊断。
 *
 * ## 为什么不能只看 RSS
 *
 * 内核状态里上报的 `memory` 字段（`StatusMessage.memory`）在 Linux 上取自
 * `/proc/self/statm` 的 **RSS（进程常驻内存）**——它是**整个 App 进程**的常驻量，
 * 包含：
 *
 * - `libbox.so` 的代码页（约 50 MB 文件映射，与其它进程共享）；
 * - APK / dex / oat / art 的文件映射（约 100 MB，同样大部分共享）；
 * - EGL 图形驱动的窗口缓冲（约 70 MB）；
 * - ART 的 Java 堆与 native 堆；
 * - Go 运行时堆（这才是 sing-box 内核真正占用的部分）。
 *
 * 因此 **RSS 的大小与内核是否泄漏没有对应关系**，用它判断内核占用会得到
 * 严重误导的结论（实测 350 MB 常驻里，内核只占 ~20 MB）。
 * 上游 `daemon.StartedService.readStatus()` 用的 `memory.Total()` 同样是
 * `/proc/self/statm` 的 RSS。
 *
 * ## 口径对照
 *
 * | 指标 | 来源 | 含义 |
 * | --- | --- | --- |
 * | `rssBytes` | `/proc/self/statm` | 进程常驻内存（含共享代码页），内核上报的就是这个 |
 * | `pssBytes` | `/proc/self/smaps` | 按比例分摊后的占用，与系统「内存占用」统计一致 |
 * | `privateDirtyBytes` | `/proc/self/smaps` | 应用独占的匿名脏页，最接近「真实独占」 |
 * | `coreHeapBytes` | `/proc/self/smaps` | **Go 运行时占用**（未命名匿名映射），即内核自身内存 |
 *
 * `/proc/self/smaps` 需要遍历整个进程的页表，开销远高于 `/proc/self/statm`，
 * 因此必须放在后台线程并降低频率。
 */
object ProcessMemory {

    /** 系统页大小（字节）：Android 15 起部分设备为 16KB 页 */
    private val PAGE_SIZE: Long = runCatching {
        android.system.Os.sysconf(android.system.OsConstants._SC_PAGESIZE)
    }.getOrDefault(4096L)

    /** Go 运行时映射的名字前缀（Android 上由 Go runtime 通过 prctl 设置） */
    private const val CORE_MAPPING_PREFIX = "[anon: Go:"

    /** 进程常驻内存（字节），与内核上报的 memory 字段同源 */
    fun rssBytes(): Long = readStatmField(1)

    /** ART Java 堆已使用量（字节） */
    fun javaHeapUsed(): Long {
        val runtime = Runtime.getRuntime()
        return runtime.totalMemory() - runtime.freeMemory()
    }

    /** ART Java 堆已提交量（字节），系统不会轻易归还这部分空间 */
    fun javaHeapCommitted(): Long = Runtime.getRuntime().totalMemory()

    /** native 堆使用量（字节），Go 运行时与 JNI 都算在内 */
    fun nativeHeap(): Long = Debug.getNativeHeapAllocatedSize()

    /**
     * 一次性采集内存指标。
     *
     * @param includePageTable 是否解析 `/proc/self/smaps`。
     *   该文件需要遍历进程页表，开销高；关闭时只采集廉价指标。
     */
    fun snapshot(includePageTable: Boolean = true): MemorySnapshot {
        val pageTable = if (includePageTable) readPageTable() else PageTable.EMPTY
        return MemorySnapshot(
            rssBytes = rssBytes(),
            pssBytes = pageTable.pssBytes,
            privateDirtyBytes = pageTable.privateDirtyBytes,
            coreHeapBytes = pageTable.coreHeapBytes,
            javaHeapUsedBytes = javaHeapUsed(),
            javaHeapCommittedBytes = javaHeapCommitted(),
            nativeHeapBytes = nativeHeap()
        )
    }

    /**
     * 合并两次采样：页表类指标在未重新采集时沿用上一次的值。
     *
     * 抽成纯函数以便单元测试（不依赖 Android 运行时）。
     */
    fun merge(
        previous: MemorySnapshot?,
        fresh: MemorySnapshot,
        withPageTable: Boolean
    ): MemorySnapshot {
        if (withPageTable || previous == null) return fresh
        return fresh.copy(
            pssBytes = previous.pssBytes,
            privateDirtyBytes = previous.privateDirtyBytes,
            coreHeapBytes = previous.coreHeapBytes
        )
    }

    /**
     * `/proc/self/smaps` 的聚合结果（单位：字节）。
     */
    data class PageTable(
        /** 按比例分摊后的总占用 */
        val pssBytes: Long = 0L,
        /** 应用独占的匿名脏页 */
        val privateDirtyBytes: Long = 0L,
        /** Go 运行时占用（未命名匿名映射的 RSS） */
        val coreHeapBytes: Long = 0L
    ) {
        companion object {
            val EMPTY = PageTable()
        }
    }

    /**
     * 读取并解析 `/proc/self/smaps`。
     *
     * 采用流式读取：该文件在大型进程下可达数 MB、十万行级别，
     * 一次性 `readText()` 会产生巨大临时字符串。
     */
    fun readPageTable(): PageTable {
        val parser = SmapsParser()
        try {
            BufferedReader(File("/proc/self/smaps").reader()).use { reader ->
                while (true) {
                    val line = reader.readLine() ?: break
                    parser.accept(line)
                }
            }
        } catch (_: Throwable) {
            // 读取失败时返回已解析到的部分
        }
        return parser.result()
    }

    /**
     * `/proc/self/smaps` 的流式解析器。
     *
     * Android 上 **Go 运行时的内存映射带名字**（`[anon: Go: heap]`、
     * `[anon: Go: immortal metadata]` 等，由 Go runtime 通过 `prctl(PR_SET_VMA_ANON_NAME)`
     * 设置），因此可以精确地把这部分归给内核，而不是像之前那样把整个进程 RSS
     * 当成「内核内存」。
     */
    class SmapsParser {
        private var pss = 0L
        private var privateDirty = 0L
        private var coreHeap = 0L
        private var currentIsCore = false
        private var currentRss = 0L

        fun accept(line: String) {
            if (isMappingHeader(line)) {
                flush()
                currentIsCore = isCoreMapping(line)
                currentRss = 0L
                return
            }
            when {
                line.startsWith("Rss:") -> currentRss = parseKb(line)
                line.startsWith("Pss:") -> pss += parseKb(line)
                line.startsWith("Private_Dirty:") -> privateDirty += parseKb(line)
            }
        }

        private fun flush() {
            if (currentIsCore) coreHeap += currentRss
        }

        fun result(): PageTable {
            flush()
            return PageTable(
                pssBytes = pss,
                privateDirtyBytes = privateDirty,
                coreHeapBytes = coreHeap
            )
        }
    }

    /**
     * 判断映射是否属于 Go 运行时（内核）。
     *
     * 形如 `[anon: Go: heap]`、`[anon: Go: stack]` 等。
     *
     * 注意：路径部分**含空格**（`[anon: Go: heap]`），不能按空白直接切分，
     * 必须取前 5 个固定字段之后的剩余部分。
     */
    private fun isCoreMapping(header: String): Boolean =
        mappingPath(header).startsWith(CORE_MAPPING_PREFIX)

    /**
     * 取映射头行的路径部分。
     *
     * 头行格式：`start-end perms offset dev inode path`，
     * 前 5 段按空白分隔，路径为剩余部分（可能为空，也可能含空格）。
     */
    private fun mappingPath(header: String): String {
        var index = 0
        var field = 0
        while (field < 5 && index < header.length) {
            while (index < header.length && header[index] == ' ') index++
            while (index < header.length && header[index] != ' ') index++
            field++
        }
        return header.substring(index).trim()
    }

    /** 映射头行：以 `start-end` 开头 */
    private fun isMappingHeader(line: String): Boolean {
        val dash = line.indexOf('-')
        if (dash <= 0) return false
        val space = line.indexOf(' ')
        if (space <= dash) return false
        val hex = "0123456789abcdefABCDEF"
        return line.substring(0, dash).all { it in hex } &&
            line.substring(dash + 1, space).all { it in hex }
    }

    /** 解析形如 `Pss:   161933 kB` 的行，返回字节数 */
    private fun parseKb(line: String): Long {
        val parts = line.split(' ').filter { it.isNotEmpty() }
        val kb = parts.getOrNull(1)?.toLongOrNull() ?: return 0L
        return kb * 1024L
    }

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
}
