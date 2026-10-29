package cn.moncn.sing_box_windows.v2.domain

import cn.moncn.sing_box_windows.v2.domain.subscription.SubscriptionItem
import cn.moncn.sing_box_windows.v2.domain.subscription.SubscriptionState
import cn.moncn.sing_box_windows.v2.domain.update.DownloadProgress
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 领域模型（订阅 / 更新）的单元测试。
 */
class DomainModelsTest {

    @Test
    fun subscriptionState_selectedReturnsMatchingItem() {
        val a = SubscriptionItem(id = "a", name = "A", url = "http://a")
        val b = SubscriptionItem(id = "b", name = "B", url = "http://b")
        val state = SubscriptionState(items = listOf(a, b), selectedId = "b")
        assertEquals(b, state.selected())
    }

    @Test
    fun subscriptionState_selectedIsNullWhenIdNotFound() {
        val state = SubscriptionState(
            items = listOf(SubscriptionItem(id = "a", name = "A", url = "http://a")),
            selectedId = "missing"
        )
        assertNull(state.selected())
    }

    @Test
    fun subscriptionState_emptyHasNoSelection() {
        val state = SubscriptionState.empty()
        assertTrue(state.items.isEmpty())
        assertNull(state.selected())
    }

    @Test
    fun downloadProgress_initialIsZeroPercent() {
        val progress = DownloadProgress.initial(totalBytes = 2048)
        assertEquals(0L, progress.bytesDownloaded)
        assertEquals(0, progress.percentage)
        assertFalse(progress.isComplete)
    }

    @Test
    fun downloadProgress_completedReachesHundredPercent() {
        val progress = DownloadProgress.completed(totalBytes = 2048)
        assertEquals(2048L, progress.bytesDownloaded)
        assertEquals(100, progress.percentage)
        assertTrue(progress.isComplete)
    }

    @Test
    fun downloadProgress_formatsReadableSizes() {
        val progress = DownloadProgress(bytesDownloaded = 512, totalBytes = 1024, percentage = 50)
        assertEquals("512B", progress.getDownloadedSizeReadable())
        assertEquals("1.0KB", progress.getTotalSizeReadable())
        assertEquals("50%", progress.getPercentageString())
    }
}
