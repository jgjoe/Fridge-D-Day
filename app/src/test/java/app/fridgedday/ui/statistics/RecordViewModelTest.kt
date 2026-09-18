package app.fridgedday.ui.statistics

import app.fridgedday.data.db.dao.ItemDao
import app.fridgedday.data.db.entity.ItemEntity
import app.fridgedday.data.db.entity.StorageLocation
import app.fridgedday.data.repo.ItemRepository
import app.fridgedday.ui.addedit.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.ZonedDateTime

/**
 * 기록 화면의 날짜 의존 상태(만료·이번 달 소비 완료·기한 지남)가 DB 방출 없이 날짜 경계를
 * 따라가는지 확인한다.
 *
 * 모든 날짜는 주입된 시계와 가상 시간에서만 오고 sleep을 쓰지 않는다. DB는 인메모리 대역이라
 * 실기기·Room이 필요 없다.
 *
 * runTest는 쓰지 않는다. 이 ViewModel은 다음 자정을 예약하는 무한 타이머를 가지므로, 테스트
 * 종료 시 스케줄러가 빌 때까지 진행하는 runTest 정리가 끝나지 않는다. 대신 규칙이 설치한
 * 디스패처의 가상 시계를 직접 민다.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class RecordViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule(testDispatcher)

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

    private fun recordViewModel(
        clock: Clock,
        dao: FakeItemDao
    ) = RecordViewModel(ItemRepository(dao), clock)

    @Test
    fun `first state is derived from the injected clock, not the device clock`() {
        // 테스트 JVM의 실제 날짜라면 이미 지난 기한이지만, 주입된 시계 기준으로는 아직 아니다.
        val dao = FakeItemDao(
            listOf(
                item(
                    id = 1,
                    createdDate = LocalDate.of(2026, 1, 10),
                    expiryDate = LocalDate.of(2026, 1, 20)
                )
            )
        )
        val viewModel = recordViewModel(MutableClock(Instant.parse("2026-01-15T09:00:00Z")), dao)

        assertEquals(0, viewModel.uiState.value.summary.expired)
        assertEquals(1, viewModel.uiState.value.summary.active)
        assertEquals(listOf(LocalDate.of(2026, 1, 10)), viewModel.uiState.value.days.map { it.date })
    }

    @Test
    fun `crossing midnight refreshes expiry state without a database write`() {
        val dao = FakeItemDao(listOf(item(1, name = "두부", expiryDate = LocalDate.of(2026, 9, 12))))
        val clock = MutableClock(Instant.parse("2026-09-12T23:59:30Z"))
        val viewModel = recordViewModel(clock, dao)

        assertEquals(0, viewModel.uiState.value.summary.expired)
        assertTrue(expiryEvents(viewModel).isEmpty())

        // 자정을 넘긴다. DB에는 아무 변화가 없다.
        clock.now = Instant.parse("2026-09-13T00:00:01Z")
        testDispatcher.scheduler.advanceTimeBy(31_000)

        assertEquals(1, viewModel.uiState.value.summary.expired)
        assertEquals(listOf(LocalDate.of(2026, 9, 12)), expiryEvents(viewModel))
        assertEquals(0, dao.writeCount)
    }

    @Test
    fun `resume refresh follows a changed date without a database write`() {
        val dao = FakeItemDao(
            listOf(
                item(
                    id = 1,
                    name = "요거트",
                    expiryDate = LocalDate.of(2026, 9, 20),
                    consumedDate = LocalDate.of(2026, 9, 30)
                )
            )
        )
        val clock = MutableClock(Instant.parse("2026-09-30T23:59:30Z"))
        val viewModel = recordViewModel(clock, dao)

        assertEquals(1, viewModel.uiState.value.summary.consumedThisMonth)

        // 기기 시각이 바뀌거나 화면이 달을 넘겨 다시 보이는 상황. 예약된 자정 타이머는 지나갔다.
        clock.now = Instant.parse("2026-10-01T00:00:05Z")
        viewModel.refreshDate()

        assertEquals(0, viewModel.uiState.value.summary.consumedThisMonth)
        assertEquals(0, dao.writeCount)
    }

    @Test
    fun `next-date delay waits for the next local midnight, not a fixed day`() {
        val seoul = ZoneId.of("Asia/Seoul")
        assertEquals(
            30_000L,
            millisUntilNextDate(ZonedDateTime.of(2026, 9, 12, 23, 59, 30, 0, seoul))
        )
        assertEquals(
            Duration.ofHours(24).toMillis(),
            millisUntilNextDate(ZonedDateTime.of(2026, 9, 13, 0, 0, 0, 0, seoul))
        )

        // 일광절약이 끝나는 날은 25시간이다. 고정 24시간이면 다음 날짜 경계를 놓친다.
        val newYork = ZoneId.of("America/New_York")
        assertEquals(
            Duration.ofHours(25).toMillis(),
            millisUntilNextDate(ZonedDateTime.of(2026, 11, 1, 0, 0, 0, 0, newYork))
        )
    }

    @Test
    fun `restore returns a consumed item to the active list`() {
        val dao = FakeItemDao(
            listOf(
                item(
                    id = 1,
                    name = "우유",
                    createdDate = LocalDate.of(2026, 9, 1),
                    expiryDate = LocalDate.of(2026, 9, 20),
                    consumedDate = LocalDate.of(2026, 9, 10)
                )
            )
        )
        val viewModel = recordViewModel(MutableClock(Instant.parse("2026-09-12T09:00:00Z")), dao)

        assertEquals(0, viewModel.uiState.value.summary.active)
        assertEquals(1, viewModel.uiState.value.summary.consumedThisMonth)
        assertEquals(listOf(1L), consumedItemIds(viewModel))

        viewModel.restore(1)

        assertEquals(1, viewModel.uiState.value.summary.active)
        assertEquals(0, viewModel.uiState.value.summary.consumedThisMonth)
        assertTrue(consumedItemIds(viewModel).isEmpty())
        assertEquals("복원했어요. 오늘 목록에 다시 표시됩니다.", viewModel.message.value)
    }

    private fun expiryEvents(viewModel: RecordViewModel): List<LocalDate> =
        viewModel.uiState.value.days.flatMap { day ->
            day.events.filter { it.type == RecordEventType.EXPIRED }.map { it.date }
        }

    private fun consumedItemIds(viewModel: RecordViewModel): List<Long> =
        viewModel.uiState.value.days.flatMap { day ->
            day.events.filter { it.type == RecordEventType.CONSUMED }.map { it.item.id }
        }
}

/**
 * Room DAO 대역. 읽기 입력만 제공하고 쓰기 횟수를 세어, 날짜 갱신이 DB 변경을 요구하지 않았음을
 * 테스트가 직접 확인할 수 있게 한다.
 */
private class FakeItemDao(initial: List<ItemEntity> = emptyList()) : ItemDao {

    private val items = MutableStateFlow(initial)

    var writeCount = 0
        private set

    override fun observeAll(): Flow<List<ItemEntity>> = items

    override fun search(keyword: String): Flow<List<ItemEntity>> =
        items.map { list -> list.filter { it.name.contains(keyword) } }

    override suspend fun dueBefore(toDate: LocalDate): List<ItemEntity> =
        items.value.filter { it.expiryDate <= toDate }

    override suspend fun getById(id: Long): ItemEntity? = items.value.firstOrNull { it.id == id }

    override suspend fun getAllItems(): List<ItemEntity> = items.value

    override suspend fun insert(item: ItemEntity): Long {
        writeCount++
        val id = if (item.id == 0L) (items.value.maxOfOrNull { it.id } ?: 0L) + 1 else item.id
        items.value = items.value.filterNot { it.id == id } + item.copy(id = id)
        return id
    }

    override suspend fun update(item: ItemEntity) {
        writeCount++
        items.value = items.value.map { if (it.id == item.id) item else it }
    }

    override suspend fun archive(id: Long) {
        writeCount++
        items.value = items.value.map { if (it.id == id) it.copy(isArchived = true) else it }
    }

    override suspend fun markConsumed(id: Long, consumedDate: LocalDate) {
        writeCount++
        items.value = items.value.map {
            if (it.id == id) it.copy(isArchived = true, consumedDate = consumedDate) else it
        }
    }

    override suspend fun delete(item: ItemEntity) {
        writeCount++
        items.value = items.value.filterNot { it.id == item.id }
    }
}

/** 시각을 직접 옮길 수 있는 시계. 기기 시각 변경과 자정 통과를 sleep 없이 재현한다. */
private class MutableClock(
    var now: Instant,
    private val zone: ZoneId = ZoneOffset.UTC
) : Clock() {

    override fun getZone(): ZoneId = zone

    override fun withZone(zone: ZoneId): Clock = MutableClock(now, zone)

    override fun instant(): Instant = now
}
