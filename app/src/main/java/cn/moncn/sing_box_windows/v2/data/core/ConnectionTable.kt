package cn.moncn.sing_box_windows.v2.data.core

import cn.moncn.sing_box_windows.v2.domain.core.CoreConnection

/**
 * 连接表：维护内核上报的连接快照。
 *
 * 抽成独立纯逻辑（不依赖 libbox / Android）以便单元测试。
 *
 * ### 为什么需要它
 *
 * 内核只会推送「连接关闭」事件，而不会推送「从列表中删除」事件；
 * 内核自身的连接表在关闭 5 分钟后才淘汰。若 App 侧不主动淘汰，
 * 连接表会随会话持续增长：
 *
 * 1. 内存（ART 堆）被已关闭连接的快照长期占用；
 * 2. 每次事件批次都需要整表拷贝生成快照，**分配量随连接数线性增长**，
 *    于是垃圾产生速率不断加快，进程 RSS 表现为「越涨越快」的锯齿。
 *
 * 因此这里做两件事：
 * - 已关闭连接保留 [closedRetentionMs] 后淘汰；
 * - 快照发布带节流，且内容未变化时不发布。
 */
class ConnectionTable(
    /** 已关闭连接的保留时长（毫秒） */
    private val closedRetentionMs: Long = 60_000L
) {
    private val connections = LinkedHashMap<String, CoreConnection>()

    /** 上次发布快照的时刻 */
    private var lastPublishAt = 0L

    /** 当前连接数 */
    val size: Int get() = connections.size

    /** 清空 */
    fun clear() {
        connections.clear()
    }

    /**
     * 记录一条新连接。
     */
    fun onNew(connection: CoreConnection) {
        connections[connection.id] = connection
    }

    /**
     * 更新一条连接的流量。
     */
    fun onTraffic(
        id: String,
        uplink: Long,
        downlink: Long
    ): Boolean {
        val existing = connections[id] ?: return false
        connections[id] = existing.copy(
            uplink = uplink,
            downlink = downlink,
            uplinkTotal = existing.uplinkTotal + uplink,
            downlinkTotal = existing.downlinkTotal + downlink
        )
        return false
    }

    /**
     * 标记一条连接已关闭。
     */
    fun onClosed(id: String, closedAt: Long): Boolean {
        val existing = connections[id] ?: return false
        connections[id] = existing.copy(
            closedAt = closedAt,
            uplink = 0L,
            downlink = 0L
        )
        return true
    }

    /**
     * 淘汰过期的已关闭连接。
     *
     * @return 是否发生过淘汰
     */
    fun evictExpired(now: Long): Boolean {
        if (connections.isEmpty()) return false
        var removed = false
        val iterator = connections.entries.iterator()
        while (iterator.hasNext()) {
            val connection = iterator.next().value
            if (connection.closed && now - connection.closedAt >= closedRetentionMs) {
                iterator.remove()
                removed = true
            }
        }
        return removed
    }

    /**
     * 生成当前快照。
     */
    fun snapshot(): List<CoreConnection> = ArrayList(connections.values)

    /**
     * 按节流策略决定是否发布快照。
     *
     * @param force 结构性变化（新增 / 关闭 / 淘汰）时立即发布
     * @param now 当前时刻
     * @param minIntervalMs 最小发布间隔
     * @return 需要发布的快照；返回 null 表示本次无需发布
     */
    fun publishIfNeeded(
        force: Boolean,
        now: Long,
        minIntervalMs: Long
    ): List<CoreConnection>? {
        if (!force && now - lastPublishAt < minIntervalMs) {
            return null
        }
        lastPublishAt = now
        return snapshot()
    }
}
