package cn.moncn.sing_box_windows.v2.data.core

import cn.moncn.sing_box_windows.v2.domain.core.MemorySnapshot
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * 进程内存口径的回归测试。
 *
 * 背景：首页「内核」卡片原先把**整个进程的 RSS**（内核上报的 memory 字段同源）
 * 直接标注为「内核上报」，导致 300MB 的常驻量看起来像内核泄漏。
 * 修正后「内核内存」必须取 Go 运行时的真实占用。
 *
 * 关键事实：Android 上 Go 运行时的内存映射**带名字**——
 * `[anon: Go: heap]`、`[anon: Go: immortal metadata]` 等，
 * 因此可以精确地把这部分归给内核。
 */
class ProcessMemoryTest {

    private val smapsSample = """
        55a4a00000-55a4a21000 r--p 00000000 103:03 1269   /data/app/~~x/base.apk
        Size:                132 kB
        Rss:                 1588 kB
        Pss:                 412 kB
        Private_Clean:       1160 kB
        Private_Dirty:         8 kB
        Anonymous:             0 kB
        7a4b00000-7a4b21000 rw-p 00000000 00:00 0        [anon: Go: heap]
        Size:               4096 kB
        Rss:                2048 kB
        Pss:                2048 kB
        Private_Dirty:       2048 kB
        Anonymous:           2048 kB
        7a4c00000-7a4c21000 rw-p 00000000 00:00 0        [anon: Go: immortal metadata]
        Size:                256 kB
        Rss:                 256 kB
        Pss:                 256 kB
        Private_Dirty:        256 kB
        7b4c00000-7b4c21000 rw-p 00000000 00:00 0        [anon:dalvik-main space]
        Size:               4096 kB
        Rss:                4096 kB
        Pss:                4096 kB
        Private_Dirty:       4096 kB
        7c4d00000-7c4d21000 rw-p 00000000 00:00 0        [anon:scudo:primary]
        Size:                512 kB
        Rss:                 512 kB
        Pss:                 512 kB
        Private_Dirty:        512 kB
    """.trimIndent()

    private fun parse(content: String) =
        ProcessMemory.SmapsParser().apply {
            content.lineSequence().forEach { accept(it) }
        }.result()

    @Test
    fun parseSmaps_coreHeapOnlyCountsGoRuntimeMappings() {
        val table = parse(smapsSample)

        // [anon: Go: heap] 2048 KB + [anon: Go: immortal metadata] 256 KB
        assertEquals((2048L + 256L) * 1024L, table.coreHeapBytes)
    }

    @Test
    fun parseSmaps_sumsPssAndPrivateDirtyAcrossAllMappings() {
        val table = parse(smapsSample)

        assertEquals((412L + 2048L + 256L + 4096L + 512L) * 1024L, table.pssBytes)
        assertEquals((8L + 2048L + 256L + 4096L + 512L) * 1024L, table.privateDirtyBytes)
    }

    @Test
    fun parseSmaps_handlesEmptyContent() {
        val table = parse("")
        assertEquals(0L, table.pssBytes)
        assertEquals(0L, table.privateDirtyBytes)
        assertEquals(0L, table.coreHeapBytes)
    }

    @Test
    fun parseSmaps_toleratesMalformedLines() {
        val table = parse(
            """
            Pss:
            Pss:  not-a-number kB
            Private_Dirty: abc kB
            """.trimIndent()
        )

        assertEquals(0L, table.pssBytes)
        assertEquals(0L, table.privateDirtyBytes)
        assertEquals(0L, table.coreHeapBytes)
    }

    @Test
    fun parseSmaps_ignoresUnnamedAnonymousMappings() {
        // 未命名匿名映射不属于 Go 运行时，不应计入内核内存
        val table = parse(
            """
            7a4b00000-7a4b21000 rw-p 00000000 00:00 0
            Rss:                4096 kB
            Pss:                4096 kB
            Private_Dirty:      4096 kB
            """.trimIndent()
        )

        assertEquals(0L, table.coreHeapBytes)
        assertEquals(4096L * 1024L, table.pssBytes)
    }

    @Test
    fun merge_keepsPreviousPageTableMetricsWhenNotResampled() {
        val previous = MemorySnapshot(
            rssBytes = 100L,
            pssBytes = 200L,
            privateDirtyBytes = 300L,
            coreHeapBytes = 400L
        )
        val fresh = MemorySnapshot(rssBytes = 150L)

        val merged = ProcessMemory.merge(previous, fresh, withPageTable = false)

        assertEquals(150L, merged.rssBytes)
        assertEquals(200L, merged.pssBytes)
        assertEquals(300L, merged.privateDirtyBytes)
        assertEquals(400L, merged.coreHeapBytes)
    }

    @Test
    fun merge_replacesEverythingWhenPageTableIsResampled() {
        val previous = MemorySnapshot(pssBytes = 200L, coreHeapBytes = 400L)
        val fresh = MemorySnapshot(rssBytes = 150L, pssBytes = 250L, coreHeapBytes = 450L)

        val merged = ProcessMemory.merge(previous, fresh, withPageTable = true)

        assertEquals(250L, merged.pssBytes)
        assertEquals(450L, merged.coreHeapBytes)
    }

    @Test
    fun merge_usesFreshSnapshotWhenNoPreviousExists() {
        val fresh = MemorySnapshot(rssBytes = 150L, coreHeapBytes = 450L)
        val merged = ProcessMemory.merge(null, fresh, withPageTable = false)

        assertEquals(150L, merged.rssBytes)
        assertEquals(450L, merged.coreHeapBytes)
    }

    @Test
    fun pageTableEmpty_hasZeroMetrics() {
        assertEquals(0L, ProcessMemory.PageTable.EMPTY.pssBytes)
        assertEquals(0L, ProcessMemory.PageTable.EMPTY.privateDirtyBytes)
        assertEquals(0L, ProcessMemory.PageTable.EMPTY.coreHeapBytes)
    }
}
