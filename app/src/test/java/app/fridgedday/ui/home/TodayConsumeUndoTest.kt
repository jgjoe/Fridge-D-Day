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
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.time.LocalDate

/**
 * Today가 띄우는 소비 완료 `실행 취소`의 저장소 경로(D-015).
 *
 * 되돌리기는 최근에 소비 완료한 행 하나를 다시 활성 상태로 저장할 뿐이고, 목록 구독이 그 변화를
 * 그대로 반영한다. 이미 없는 행을 되돌리려 하면 화면이 조용히 성공한 것처럼 보이지 않도록
 * 실패를 돌려준다.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class TodayConsumeUndoTest {

    private val testDispatcher = UnconfinedTestDispatcher()

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule(testDispatcher)

    private fun consumedItem(id: Long, name: String) = ItemEntity(
        id = id,
        name = name,
        location = StorageLocation.FRIDGE,
        expiryDate = LocalDate.now().plusDays(5),
        isArchived = true,
        consumedDate = LocalDate.now()
    )

    @Test
    fun `undo restores the consumed row to the visible list`() = runBlocking {
        val dao = ConsumedAwareDao(listOf(consumedItem(1, "우유")))
        val viewModel = HomeViewModel(ItemRepository(dao))
        assertTrue(viewModel.uiState.value.items.isEmpty())

        assertTrue(viewModel.restoreConsumed(1L))

        val restored = dao.getById(1L)!!
        assertFalse(restored.isArchived)
        assertNull(restored.consumedDate)
        assertEquals(listOf(1L), viewModel.uiState.value.items.map { it.id })
    }

    @Test
    fun `undo reports failure when the consumed row no longer exists`() = runBlocking {
        val viewModel = HomeViewModel(ItemRepository(ConsumedAwareDao(emptyList())))

        assertFalse(viewModel.restoreConsumed(404L))

        assertTrue(viewModel.uiState.value.items.isEmpty())
    }
}

/** 실제 DAO와 같이 아카이브된 행을 목록에서 감추는 최소 대역. */
private class ConsumedAwareDao(initial: List<ItemEntity>) : ItemDao {

    private val items = MutableStateFlow(initial)

    override fun observeAll(): Flow<List<ItemEntity>> =
        items.map { list -> list.filterNot { it.isArchived } }

    override fun search(keyword: String): Flow<List<ItemEntity>> =
        items.map { list -> list.filter { !it.isArchived && it.name.contains(keyword) } }

    override suspend fun dueBefore(toDate: LocalDate): List<ItemEntity> =
        items.value.filter { !it.isArchived && it.expiryDate <= toDate }

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
