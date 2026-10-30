package cn.moncn.sing_box_windows.v2.domain.core

/**
 * 进程内存快照。
 *
 * 内核上报的 `memory` 字段是**进程 RSS**（常驻内存），包含 ART 的 Java 堆、
 * Go 运行时堆与 native 库代码页，并不是「内核独占的内存」。
 * 分解成下面几项才能判断增长发生在哪一侧。
 */
data class MemorySnapshot(
    /** 进程常驻内存（字节）—— 与内核上报的 memory 同源 */
    val rssBytes: Long = 0L,
    /** ART Java 堆已使用量（字节）—— Kotlin 侧对象 */
    val javaHeapUsedBytes: Long = 0L,
    /** ART Java 堆已提交量（字节）—— 系统不会轻易归还 */
    val javaHeapCommittedBytes: Long = 0L,
    /** native 堆使用量（字节）—— Go 运行时与 JNI 分配 */
    val nativeHeapBytes: Long = 0L,
    /** 按比例分摊的总占用（KB）—— 与系统「内存占用」口径一致 */
    val pssKb: Long = 0L
)
