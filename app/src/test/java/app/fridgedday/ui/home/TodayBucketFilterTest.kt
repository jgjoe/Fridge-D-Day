package app.fridgedday.ui.home

import app.fridgedday.data.db.entity.ItemEntity
import app.fridgedday.data.db.entity.StorageLocation
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class TodayBucketFilterTest {
    private fun item(name: String, days: Long) = ItemEntity(
        name = name,
        location = StorageLocation.FRIDGE,
        expiryDate = LocalDate.now().plusDays(days)
    )

    @Test
    fun `four chip counts partition every managed item`() {
        val items = listOf(item("지난 식품", -1), item("보통 식품", 3), item("여유 식품", 20))

        val counts = todayBucketCounts(items)

        assertEquals(TodayBucketCounts(all = 3, needsAttention = 1, soon = 1, rest = 1), counts)
        assertEquals(counts.all, counts.needsAttention + counts.soon + counts.rest)
    }

    @Test
    fun `chip filters use the same three freshness states as their counts`() {
        val expired = item("지난 식품", -1)
        val warning = item("보통 식품", 3)
        val safe = item("여유 식품", 20)

        assertTrue(itemMatchesFilter(expired, FilterType.NEEDS_ATTENTION))
        assertTrue(itemMatchesFilter(warning, FilterType.SOON))
        assertTrue(itemMatchesFilter(safe, FilterType.REST))
        assertFalse(itemMatchesFilter(safe, FilterType.SOON))
    }
}
