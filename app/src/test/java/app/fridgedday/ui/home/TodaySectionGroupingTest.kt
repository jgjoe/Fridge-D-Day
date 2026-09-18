package app.fridgedday.ui.home

import app.fridgedday.data.db.entity.ItemEntity
import app.fridgedday.data.db.entity.StorageLocation
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class TodaySectionGroupingTest {

    private fun item(name: String, daysUntilExpiry: Long) = ItemEntity(
        name = name,
        location = StorageLocation.FRIDGE,
        expiryDate = LocalDate.now().plusDays(daysUntilExpiry)
    )

    @Test
    fun `section titles match the release polish copy`() {
        assertEquals(
            listOf("오늘 확인할 식품", "곧 만료돼요", "여유 있어요"),
            TodaySection.entries.map { it.title }
        )
    }

    @Test
    fun `splits items into the fixed Today section order`() {
        val safe = item("안전", 30)
        val expired = item("만료", -2)
        val warning = item("임박", 4)

        val sections = buildTodaySections(listOf(safe, expired, warning))

        assertEquals(
            listOf(TodaySection.NEEDS_ATTENTION, TodaySection.SOON, TodaySection.REST),
            sections.map { it.section }
        )
        assertEquals(listOf("만료"), sections[0].items.map { it.name })
        assertEquals(listOf("임박"), sections[1].items.map { it.name })
        assertEquals(listOf("안전"), sections[2].items.map { it.name })
    }

    @Test
    fun `groups today and already expired items together as needs attention`() {
        val sections = buildTodaySections(listOf(item("오늘", 0), item("어제", -1)))

        assertEquals(listOf(TodaySection.NEEDS_ATTENTION), sections.map { it.section })
        assertEquals(listOf("오늘", "어제"), sections.single().items.map { it.name })
    }

    @Test
    fun `omits sections without items`() {
        val sections = buildTodaySections(listOf(item("안전", 12)))

        assertEquals(listOf(TodaySection.REST), sections.map { it.section })
    }

    @Test
    fun `keeps the incoming order inside each section`() {
        val sections = buildTodaySections(
            listOf(item("임박-B", 2), item("임박-A", 6), item("안전-B", 10), item("안전-A", 40))
        )

        assertEquals(listOf("임박-B", "임박-A"), sections[0].items.map { it.name })
        assertEquals(listOf("안전-B", "안전-A"), sections[1].items.map { it.name })
    }

    @Test
    fun `returns no sections for an empty list`() {
        assertTrue(buildTodaySections(emptyList()).isEmpty())
    }
}
