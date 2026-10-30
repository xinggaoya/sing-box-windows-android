package cn.moncn.sing_box_windows.v2.domain.core

/**
 * 进程内存快照。
 *
 * 所有指标均为**整个 App 进程**的占用口径，注意区分：
 * 进程常驻内存（RSS）里绝大部分是共享代码页与图形缓冲，
 * 不能等同于 sing-box 内核的占用；内核自身的占用见 [coreHeapBytes]。
 */
data class MemorySnapshot(
    /** 进程常驻内存（字节）—— 含共享代码页，与内核上报的 memory 同源 */
    val rssBytes: Long = 0L,
    /** 按比例分摊后的总占用（字节）—— 与系统「内存占用」统计口径一致 */
    val pssBytes: Long = 0L,
    /** 应用独占的匿名脏页（字节）—— 最接近「真实独占」 */
    val privateDirtyBytes: Long = 0L,
    /** Go 运行时占用（字节）—— 内核自身内存，= 未命名匿名映射的 RSS */
    val coreHeapBytes: Long = 0L,
    /** ART Java 堆已使用量（字节）—— Kotlin 侧对象 */
    val javaHeapUsedBytes: Long = 0L,
    /** ART Java 堆已提交量（字节）—— 系统不会轻易归还 */
    val javaHeapCommittedBytes: Long = 0L,
    /** native 堆使用量（字节）—— Go 运行时与 JNI 分配 */
    val nativeHeapBytes: Long = 0L
)
