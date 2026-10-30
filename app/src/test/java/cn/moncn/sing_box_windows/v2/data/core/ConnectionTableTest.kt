package cn.moncn.sing_box_windows.v2.data.core

import cn.moncn.sing_box_windows.v2.domain.core.CoreConnection
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 连接表的回归测试。
 *
 * 背景：内核只推送「连接关闭」事件，不会推送「从列表删除」事件。
 * App 侧若不主动淘汰，连接表会随会话持续增长，且每次事件批次的整表拷贝
 * 让垃圾产生速率不断加快，表现为进程内存「越涨越快」的锯齿。
 */
class ConnectionTableTest {

    private fun connection(id: String, closedAt: Long = 0L) =
        CoreConnection(id = id, closedAt = closedAt)

    @Test
    fun onNew_addsConnection() {
        val table = ConnectionTable()
        table.onNew(connection("a"))
        assertEquals(1, table.size)
        assertEquals(1, table.snapshot().size)
    }

    @Test
    fun onTraffic_accumulatesTotals() {
        val table = ConnectionTable()
        table.onNew(connection("a"))
        table.onTraffic("a", uplink = 100, downlink = 200)
        table.onTraffic("a", uplink = 50, downlink = 20)
        val updated = table.snapshot().first()
        assertEquals(150L, updated.uplinkTotal)
        assertEquals(220L, updated.downlinkTotal)
        assertEquals(50L, updated.uplink)
    }

    @Test
    fun onClosed_marksClosedAndReportsStructuralChange() {
        val table = ConnectionTable()
        table.onNew(connection("a"))
        assertTrue(table.onClosed("a", closedAt = 1_000L))
        assertTrue(table.snapshot().first().closed)
    }

    @Test
    fun onClosed_unknownIdIsIgnored() {
        val table = ConnectionTable()
        assertFalse(table.onClosed("missing", closedAt = 1L))
    }

    @Test
    fun evictExpired_removesClosedConnectionsAfterRetention() {
        val table = ConnectionTable(closedRetentionMs = 60_000L)
        table.onNew(connection("a"))
        table.onNew(connection("b"))
        table.onClosed("a", closedAt = 1_000L)

        // 未到保留时长：仍在表中
        assertFalse(table.evictExpired(now = 30_000L))
        assertEquals(2, table.size)

        // 超过保留时长：被淘汰
        assertTrue(table.evictExpired(now = 61_000L))
        assertEquals(1, table.size)
        assertEquals("b", table.snapshot().first().id)
    }

    @Test
    fun evictExpired_keepsActiveConnections() {
        val table = ConnectionTable(closedRetentionMs = 1_000L)
        table.onNew(connection("active"))
        assertFalse(table.evictExpired(now = 10_000_000L))
        assertEquals(1, table.size)
    }

    @Test
    fun evictExpired_onEmptyTableIsNoop() {
        val table = ConnectionTable()
        assertFalse(table.evictExpired(now = 1L))
        assertEquals(0, table.size)
    }

    @Test
    fun publishIfNeeded_throttlesNonStructuralUpdates() {
        val table = ConnectionTable()
        table.onNew(connection("a"))

        // 首次发布
        assertNotNull(table.publishIfNeeded(force = false, now = 1_000L, minIntervalMs = 1_000L))

        // 1 秒内再次请求 → 被节流
        assertNull(table.publishIfNeeded(force = false, now = 1_500L, minIntervalMs = 1_000L))

        // 超过间隔 → 允许发布
        assertNotNull(table.publishIfNeeded(force = false, now = 2_100L, minIntervalMs = 1_000L))
    }

    @Test
    fun publishIfNeeded_forceBypassesThrottle() {
        val table = ConnectionTable()
        table.onNew(connection("a"))
        table.publishIfNeeded(force = false, now = 1_000L, minIntervalMs = 1_000L)
        assertNotNull(table.publishIfNeeded(force = true, now = 1_100L, minIntervalMs = 1_000L))
    }

    @Test
    fun table_doesNotGrowWithoutBound() {
        // 模拟长时间会话：不断新建并关闭连接
        val table = ConnectionTable(closedRetentionMs = 1_000L)
        var now = 0L
        repeat(500) { index ->
            now += 10L
            val id = "conn-$index"
            table.onNew(connection(id))
            table.onClosed(id, closedAt = now)
            table.evictExpired(now)
        }
        // 保留期内最多保留 100 条（10ms 一个 / 保留 1000ms）
        assertTrue("连接表不应无界增长，实际 ${table.size}", table.size <= 101)
    }
}
