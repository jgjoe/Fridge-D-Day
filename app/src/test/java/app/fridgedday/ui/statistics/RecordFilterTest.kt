package app.fridgedday.ui.statistics

import app.fridgedday.data.db.entity.ItemEntity
import app.fridgedday.data.db.entity.StorageLocation
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class RecordFilterTest {
    private val date = LocalDate.of(2026, 9, 16)

    private fun event(name: String, type: RecordEventType) = RecordEvent(
        date = date,
        type = type,
        item = ItemEntity(
            name = name,
            location = StorageLocation.FRIDGE,
            expiryDate = date
        )
    )

    @Test
    fun `status chips preserve day grouping while filtering event types`() {
        val days = listOf(
            RecordDay(
                date,
                listOf(
                    event("우유", RecordEventType.REGISTERED),
                    event("우유", RecordEventType.CONSUMED),
                    event("두부", RecordEventType.EXPIRED)
                )
            )
        )

        val consumed = filterRecordDays(days, RecordFilter.CONSUMED, "")
        val expired = filterRecordDays(days, RecordFilter.EXPIRED, "")

        assertEquals(listOf(RecordEventType.CONSUMED), consumed.single().events.map { it.type })
        assertEquals(listOf(RecordEventType.EXPIRED), expired.single().events.map { it.type })
    }

    @Test
    fun `search filters item names and removes empty date groups`() {
        val days = listOf(
            RecordDay(date, listOf(event("우유", RecordEventType.CONSUMED))),
            RecordDay(date.minusDays(1), listOf(event("두부", RecordEventType.EXPIRED)))
        )

        val result = filterRecordDays(days, RecordFilter.ALL, "우유")

        assertEquals(listOf(date), result.map { it.date })
        assertEquals("우유", result.single().events.single().item.name)
    }
}
