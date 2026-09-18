package app.fridgedday.ui.home

import app.fridgedday.data.db.dao.ItemDao
import app.fridgedday.data.db.entity.ItemEntity
import app.fridgedday.data.db.entity.StorageLocation
import app.fridgedday.data.repo.ItemRepository
import app.fridgedday.ui.addedit.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.time.LocalDate

/**
 * 등록 직후 Today가 저장한 행을 실제로 보여주는 경로(D-016).
 *
 * 정렬은 사용자가 고른 값을 유지하고, 저장한 행을 실제로 가리는 검색·보관 위치·상태 조건만 푼다.
 * 조건이 그 행을 이미 통과시키면 그대로 둔다. 화면은 평탄화된 LazyColumn 인덱스로 그 행까지
 * 스크롤한다.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class TodayRevealTest {

    private val testDispatcher = UnconfinedTestDispatcher()

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule(testDispatcher)

    private fun item(
        id: Long,
        name: String,
        location: StorageLocation = StorageLocation.FRIDGE,
        daysUntilExpiry: Long = 30
    ) = ItemEntity(
        id = id,
        name = name,
        location = location,
        expiryDate = LocalDate.now().plusDays(daysUntilExpiry)
    )

    @Test
    fun `reveal keeps the chosen sort and clears the conditions that hide the saved row`() = runBlocking {
        val dao = FakeItemDao(listOf(item(1, "우유")))
        val viewModel = HomeViewModel(ItemRepository(dao))
        dao.insert(item(2, "두부", daysUntilExpiry = 30))

        viewModel.setSearchKeyword("우유")
        viewModel.setFilter(FilterType.SOON)
        viewModel.setLocationFilter(StorageLocation.FREEZER)
        viewModel.setSortType(SortType.CREATED_DATE)

        viewModel.revealItem(2)

        val state = viewModel.uiState.value
        assertEquals(SortType.CREATED_DATE, state.sortType)
        assertEquals(FilterType.ALL, state.filterType)
        assertNull(state.locationFilter)
        assertEquals("", state.searchKeyword)
        assertEquals(2L, state.pendingRevealItemId)
    }

    @Test
    fun `reveal keeps the conditions the saved row already satisfies`() = runBlocking {
        val dao = FakeItemDao(listOf(item(1, "우유")))
        val viewModel = HomeViewModel(ItemRepository(dao))
        dao.insert(
            item(2, "우유 두부", location = StorageLocation.FREEZER, daysUntilExpiry = 2)
        )

        // 검색어·보관 위치·상태 필터가 모두 이 행을 통과시키므로 하나도 풀지 않는다.
        viewModel.setSearchKeyword("우유")
        viewModel.setFilter(FilterType.SOON)
        viewModel.setLocationFilter(StorageLocation.FREEZER)
        viewModel.setSortType(SortType.NAME)

        viewModel.revealItem(2)

        val state = viewModel.uiState.value
        assertEquals("우유", state.searchKeyword)
        assertEquals(FilterType.SOON, state.filterType)
        assertEquals(StorageLocation.FREEZER, state.locationFilter)
        assertEquals(SortType.NAME, state.sortType)
        assertEquals(2L, state.pendingRevealItemId)
    }

    @Test
    fun `reveal resolves the saved row from the repository and leaves an unknown id untouched`() {
        val viewModel = HomeViewModel(ItemRepository(FakeItemDao(listOf(item(1, "우유")))))

        viewModel.setSearchKeyword("우유")
        viewModel.setFilter(FilterType.NEEDS_ATTENTION)

        viewModel.revealItem(99L)

        // 저장소에 없는 id로는 어떤 조건도 바꾸지 않고, 이동할 대상도 남기지 않는다.
        val state = viewModel.uiState.value
        assertEquals("우유", state.searchKeyword)
        assertEquals(FilterType.NEEDS_ATTENTION, state.filterType)
        assertNull(state.pendingRevealItemId)
    }

    @Test
    fun `saved row becomes visible after the hiding filters are cleared`() = runBlocking {
        val dao = FakeItemDao(listOf(item(1, "우유")))
        val viewModel = HomeViewModel(ItemRepository(dao))

        dao.insert(item(2, "두부", daysUntilExpiry = 5))
        viewModel.setFilter(FilterType.NEEDS_ATTENTION)
        viewModel.setSearchKeyword("우유")

        viewModel.revealItem(2)

        assertTrue(viewModel.uiState.value.items.any { it.id == 2L })
    }

    @Test
    fun `consume reveal stops further scrolling to the same row`() {
        val viewModel = HomeViewModel(ItemRepository(FakeItemDao(listOf(item(1, "우유")))))

        viewModel.revealItem(1)
        viewModel.consumeReveal()

        assertNull(viewModel.uiState.value.pendingRevealItemId)
    }

    @Test
    fun `reveal index counts the section headers above the target row`() {
        val sections = listOf(
            TodaySectionGroup(
                TodaySection.NEEDS_ATTENTION,
                listOf(item(1, "만료"), item(2, "오늘"))
            ),
            TodaySectionGroup(TodaySection.REST, listOf(item(3, "안전")))
        )

        assertEquals(1, revealTargetIndex(sections, 1L))
        assertEquals(2, revealTargetIndex(sections, 2L))
        assertEquals(4, revealTargetIndex(sections, 3L))
        assertNull(revealTargetIndex(sections, 99L))
    }
}

/** Room DAO 대역. 등록 직후 목록 반영만 재현하면 되므로 읽기와 insert만 제공한다. */
private class FakeItemDao(initial: List<ItemEntity> = emptyList()) : ItemDao {

    private val items = MutableStateFlow(initial)

    override fun observeAll(): Flow<List<ItemEntity>> = items

    override fun search(keyword: String): Flow<List<ItemEntity>> =
        items.map { list -> list.filter { it.name.contains(keyword) } }

    override suspend fun dueBefore(toDate: LocalDate): List<ItemEntity> =
        items.value.filter { it.expiryDate <= toDate }

    override suspend fun getById(id: Long): ItemEntity? = items.value.firstOrNull { it.id == id }

    override suspend fun getAllItems(): List<ItemEntity> = items.value

    override suspend fun insert(item: ItemEntity): Long {
        val id = if (item.id == 0L) (items.value.maxOfOrNull { it.id } ?: 0L) + 1 else item.id
        items.value = items.value.filterNot { it.id == id } + item.copy(id = id)
        return id
    }

    override suspend fun update(item: ItemEntity) {
        items.value = items.value.map { if (it.id == item.id) item else it }
    }

    override suspend fun archive(id: Long) {
        items.value = items.value.map { if (it.id == id) it.copy(isArchived = true) else it }
    }

    override suspend fun markConsumed(id: Long, consumedDate: LocalDate) {
        items.value = items.value.map {
            if (it.id == id) it.copy(isArchived = true, consumedDate = consumedDate) else it
        }
    }

    override suspend fun delete(item: ItemEntity) {
        items.value = items.value.filterNot { it.id == item.id }
    }
}
