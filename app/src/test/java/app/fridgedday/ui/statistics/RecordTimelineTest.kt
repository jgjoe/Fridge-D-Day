package app.fridgedday.ui.statistics

import app.fridgedday.data.db.entity.ItemEntity
import app.fridgedday.data.db.entity.StorageLocation
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

/**
 * 기록 파생은 고정된 today에만 의존하는 순수 계산이다. 모든 케이스가 오늘 날짜를 명시적으로
 * 넘겨 실제 시계와 분리한다.
 */
class RecordTimelineTest {

    private val today = LocalDate.of(2026, 9, 12)

    private fun item(
        id: Long,
        name: String = "식품$id",
        createdDate: LocalDate = LocalDate.of(2026, 9, 1),
        expiryDate: LocalDate,
        consumedDate: LocalDate? = null,
        isArchived: Boolean = consumedDate != null
    ) = ItemEntity(
        id = id,
        name = name,
        location = StorageLocation.FRIDGE,
        expiryDate = expiryDate,
        isArchived = isArchived,
        consumedDate = consumedDate,
        createdDate = createdDate
    )

    private fun dates(days: List<RecordDay>): List<LocalDate> = days.map { it.date }

    private fun eventsOn(days: List<RecordDay>, date: LocalDate): List<RecordEvent> =
        days.first { it.date == date }.events

    private fun allEvents(days: List<RecordDay>): List<RecordEvent> = days.flatMap { it.events }

    @Test
    fun `monthly consumed count uses only the current month`() {
        val items = listOf(
            item(1, expiryDate = LocalDate.of(2026, 9, 20), consumedDate = LocalDate.of(2026, 9, 2)),
            item(2, expiryDate = LocalDate.of(2026, 8, 20), consumedDate = LocalDate.of(2026, 8, 31)),
            item(3, expiryDate = LocalDate.of(2026, 10, 1))
        )

        assertEquals(1, buildRecordSummary(items, today).consumedThisMonth)
    }

    @Test
    fun `expired count is the unconsumed branch of the passed-expiry rule`() {
        val items = listOf(
            // 기한 지남, 미소비
            item(1, expiryDate = LocalDate.of(2026, 9, 11)),
            // 오늘 기한, 아직 지나지 않음
            item(2, expiryDate = today),
            // 기한 후 소비된 항목은 만료 상태가 아니다
            item(3, expiryDate = LocalDate.of(2026, 9, 5), consumedDate = LocalDate.of(2026, 9, 8))
        )

        assertEquals(1, buildRecordSummary(items, today).expired)
    }

    @Test
    fun `active count excludes archived items`() {
        val items = listOf(
            item(1, expiryDate = LocalDate.of(2026, 9, 20)),
            item(2, expiryDate = LocalDate.of(2026, 9, 20), consumedDate = LocalDate.of(2026, 9, 10)),
            item(3, expiryDate = LocalDate.of(2026, 9, 20), isArchived = true)
        )

        assertEquals(1, buildRecordSummary(items, today).active)
    }

    @Test
    fun `expired summary stays consistent with the generated passed-expiry events`() {
        val items = listOf(
            item(1, expiryDate = LocalDate.of(2026, 9, 11)),
            item(2, expiryDate = LocalDate.of(2026, 9, 5), consumedDate = LocalDate.of(2026, 9, 8)),
            item(3, expiryDate = LocalDate.of(2026, 10, 1))
        )

        val unconsumedExpiryEvents = allEvents(buildRecordDays(items, today))
            .count { event -> event.type == RecordEventType.EXPIRED && event.item.consumedDate == null }

        assertEquals(buildRecordSummary(items, today).expired, unconsumedExpiryEvents)
    }

    @Test
    fun `empty ledger stays empty and zeroed`() {
        assertEquals(RecordSummary(consumedThisMonth = 0, expired = 0, active = 0), buildRecordSummary(emptyList(), today))
        assertTrue(buildRecordDays(emptyList(), today).isEmpty())
    }

    @Test
    fun `every item gets a registration event on its created date`() {
        val created = LocalDate.of(2026, 8, 30)
        val days = buildRecordDays(
            listOf(item(1, createdDate = created, expiryDate = LocalDate.of(2026, 10, 1))),
            today
        )

        assertEquals(listOf(created), dates(days))
        assertEquals(
            listOf(RecordEventType.REGISTERED),
            eventsOn(days, created).map { it.type }
        )
    }

    @Test
    fun `consumed before expiry has no passed-expiry event`() {
        val days = buildRecordDays(
            listOf(
                item(
                    1,
                    createdDate = LocalDate.of(2026, 8, 20),
                    expiryDate = LocalDate.of(2026, 9, 20),
                    consumedDate = LocalDate.of(2026, 9, 5)
                )
            ),
            today
        )

        assertEquals(listOf(LocalDate.of(2026, 9, 5), LocalDate.of(2026, 8, 20)), dates(days))
        assertEquals(
            listOf(RecordEventType.CONSUMED),
            eventsOn(days, LocalDate.of(2026, 9, 5)).map { it.type }
        )
        assertTrue(allEvents(days).none { it.type == RecordEventType.EXPIRED })
    }

    @Test
    fun `consumed on the expiry date has no passed-expiry event`() {
        val expiry = LocalDate.of(2026, 9, 5)
        val days = buildRecordDays(
            listOf(
                item(
                    1,
                    createdDate = LocalDate.of(2026, 8, 20),
                    expiryDate = expiry,
                    consumedDate = expiry
                )
            ),
            today
        )

        assertEquals(listOf(LocalDate.of(2026, 9, 5), LocalDate.of(2026, 8, 20)), dates(days))
        assertEquals(
            listOf(RecordEventType.CONSUMED),
            eventsOn(days, expiry).map { it.type }
        )
        assertTrue(allEvents(days).none { it.type == RecordEventType.EXPIRED })
    }

    @Test
    fun `consumed after expiry keeps the expiry event and the later consume event`() {
        val expiry = LocalDate.of(2026, 9, 1)
        val consumed = LocalDate.of(2026, 9, 5)
        val days = buildRecordDays(
            listOf(
                item(
                    1,
                    createdDate = LocalDate.of(2026, 8, 20),
                    expiryDate = expiry,
                    consumedDate = consumed
                )
            ),
            today
        )

        assertEquals(
            listOf(consumed, expiry, LocalDate.of(2026, 8, 20)),
            dates(days)
        )
        assertEquals(listOf(RecordEventType.CONSUMED), eventsOn(days, consumed).map { it.type })
        assertEquals(listOf(RecordEventType.EXPIRED), eventsOn(days, expiry).map { it.type })
    }

    @Test
    fun `unconsumed item past expiry gets a passed-expiry event on the expiry date`() {
        val expiry = LocalDate.of(2026, 9, 11)
        val days = buildRecordDays(
            listOf(item(1, createdDate = LocalDate.of(2026, 9, 1), expiryDate = expiry)),
            today
        )

        assertEquals(listOf(expiry, LocalDate.of(2026, 9, 1)), dates(days))
        assertEquals(listOf(RecordEventType.EXPIRED), eventsOn(days, expiry).map { it.type })
    }

    @Test
    fun `an item expiring today has not passed expiry yet`() {
        val days = buildRecordDays(
            listOf(item(1, createdDate = LocalDate.of(2026, 9, 1), expiryDate = today)),
            today
        )

        assertEquals(listOf(LocalDate.of(2026, 9, 1)), dates(days))
        assertTrue(allEvents(days).none { it.type == RecordEventType.EXPIRED })
    }

    @Test
    fun `days are grouped newest first and merge the same date`() {
        val days = buildRecordDays(
            listOf(
                item(
                    1,
                    createdDate = LocalDate.of(2026, 9, 2),
                    expiryDate = LocalDate.of(2026, 9, 10),
                    consumedDate = LocalDate.of(2026, 9, 10)
                ),
                item(2, createdDate = LocalDate.of(2026, 9, 2), expiryDate = LocalDate.of(2026, 10, 1)),
                item(3, createdDate = LocalDate.of(2026, 8, 25), expiryDate = LocalDate.of(2026, 9, 30))
            ),
            today
        )

        assertEquals(
            listOf(
                LocalDate.of(2026, 9, 10),
                LocalDate.of(2026, 9, 2),
                LocalDate.of(2026, 8, 25)
            ),
            dates(days)
        )
        assertTrue(dates(days).zipWithNext().all { (newer, older) -> newer.isAfter(older) })
        assertEquals(listOf(1L, 2L), eventsOn(days, LocalDate.of(2026, 9, 2)).map { it.item.id })
    }

    @Test
    fun `same-day registration is listed before the consume event`() {
        val day = LocalDate.of(2026, 9, 5)
        val days = buildRecordDays(
            listOf(
                item(
                    1,
                    createdDate = day,
                    expiryDate = LocalDate.of(2026, 9, 20),
                    consumedDate = day
                )
            ),
            today
        )

        assertEquals(
            listOf(RecordEventType.REGISTERED, RecordEventType.CONSUMED),
            eventsOn(days, day).map { it.type }
        )
    }
}
