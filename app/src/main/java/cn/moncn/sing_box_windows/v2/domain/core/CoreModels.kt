package cn.moncn.sing_box_windows.v2.domain.core

/**
 * 内核运行态领域模型。
 *
 * 数据来源为 libbox 自带的 `CommandClient`（底层为 sing-box 官方 daemon gRPC 服务），
 * 与桌面端 `sing-box-windows` 所用的官方 API 同源。
 * 本包不依赖任何 Android / libbox 类型，保持领域层纯净。
 */

/** 内核状态快照（对应 `StatusMessage`） */
data class CoreStatus(
    /** 当前是否可采集流量数据 */
    val trafficAvailable: Boolean = false,
    /** 实时上行速率（字节/秒） */
    val uplink: Long = 0L,
    /** 实时下行速率（字节/秒） */
    val downlink: Long = 0L,
    /** 累计上行流量（字节） */
    val uplinkTotal: Long = 0L,
    /** 累计下行流量（字节） */
    val downlinkTotal: Long = 0L,
    /** Go 堆内存占用（字节） */
    val memory: Long = 0L,
    /** Go 协程数 */
    val goroutines: Int = 0,
    /** 入站连接数 */
    val connectionsIn: Int = 0,
    /** 出站连接数 */
    val connectionsOut: Int = 0
)

/** 代理组（对应 `OutboundGroup`） */
data class OutboundGroup(
    val tag: String,
    val type: String,
    /** 是否可手动选择出站 */
    val selectable: Boolean = false,
    /** 当前选中的出站 tag */
    val selected: String = "",
    /** 组是否展开 */
    val expanded: Boolean = false,
    /** 组内成员 */
    val items: List<OutboundGroupItem> = emptyList()
)

/** 代理组成员 / 出站（对应 `OutboundGroupItem`） */
data class OutboundGroupItem(
    val tag: String,
    val type: String,
    /** 最近一次测速时间戳（毫秒），0 表示未测速 */
    val urlTestTime: Long = 0L,
    /** 最近一次测速延迟（毫秒），-1 表示失败 */
    val urlTestDelay: Int = -1
) {
    /** 是否已完成过测速 */
    val tested: Boolean get() = urlTestTime > 0L
}

/** 活跃连接（对应 `Connection`） */
data class CoreConnection(
    val id: String,
    val inbound: String = "",
    val inboundType: String = "",
    val network: String = "",
    val source: String = "",
    val destination: String = "",
    val domain: String = "",
    val protocol: String = "",
    val user: String = "",
    val fromOutbound: String = "",
    val outbound: String = "",
    val outboundType: String = "",
    val rule: String = "",
    /** 创建时间戳（毫秒） */
    val createdAt: Long = 0L,
    /** 关闭时间戳（毫秒），0 表示仍活跃 */
    val closedAt: Long = 0L,
    val uplink: Long = 0L,
    val downlink: Long = 0L,
    val uplinkTotal: Long = 0L,
    val downlinkTotal: Long = 0L
) {
    /** 是否为已关闭的连接 */
    val closed: Boolean get() = closedAt > 0L

    /** 展示用的目标地址：优先域名，其次 IP */
    val displayTarget: String get() = domain.ifBlank { destination }
}

/** 内核日志条目（对应 `LogEntry`） */
data class CoreLogEntry(
    /** 日志级别，与 libbox `LogEntry.level` 一致 */
    val level: Int,
    val message: String
) {
    companion object {
        const val LEVEL_TRACE = 0
        const val LEVEL_DEBUG = 1
        const val LEVEL_INFO = 2
        const val LEVEL_WARN = 3
        const val LEVEL_ERROR = 4
        const val LEVEL_FATAL = 5
        const val LEVEL_PANIC = 6

        fun levelName(level: Int): String = when (level) {
            LEVEL_TRACE -> "TRACE"
            LEVEL_DEBUG -> "DEBUG"
            LEVEL_INFO -> "INFO"
            LEVEL_WARN -> "WARN"
            LEVEL_ERROR -> "ERROR"
            LEVEL_FATAL -> "FATAL"
            LEVEL_PANIC -> "PANIC"
            else -> "UNKNOWN"
        }
    }
}

/** 内核与命令通道的连接状态 */
enum class CoreConnectionState {
    /** 未启动 */
    DISCONNECTED,
    /** 连接中 */
    CONNECTING,
    /** 已连接，命令通道可用 */
    CONNECTED,
    /** 连接断开 */
    DISCONNECTED_ERROR
}

/** 代理模式（Rule / Global / Direct） */
enum class CoreClashMode(val raw: String, val displayName: String) {
    RULE("rule", "规则"),
    GLOBAL("global", "全局"),
    DIRECT("direct", "直连");

    companion object {
        fun fromRaw(raw: String?): CoreClashMode? =
            entries.firstOrNull { it.raw.equals(raw?.trim(), ignoreCase = true) }
    }
}
