package app.fridgedday.ui.detail

import app.fridgedday.data.db.entity.ItemEntity
import app.fridgedday.data.db.entity.StorageLocation
import app.fridgedday.data.repo.ItemDetailRepository
import app.fridgedday.ui.addedit.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class ItemDetailViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val milk = ItemEntity(
        id = 7,
        name = "우유",
        location = StorageLocation.FRIDGE,
        expiryDate = LocalDate.of(2026, 9, 20)
    )

    @Test
    fun consumeArchivesTheItemAndSignalsTheReturnToToday() = runTest {
        val repository = FakeItemDetailRepository(milk)
        val viewModel = ItemDetailViewModel(repository, milk.id)

        viewModel.consume()

        // 화면은 Today로 돌아가지만, 돌아가기 전까지 빈 상세가 아니라 방금 소비한 항목을 계속 보여준다.
        assertTrue(viewModel.uiState.value.isConsumed)
        assertEquals("우유", viewModel.uiState.value.item?.name)
        assertEquals(1, repository.consumeCount)
        assertTrue(repository.activeItems().isEmpty())
    }

    @Test
    fun deleteRemovesTheItemAndSignalsTheReturnToToday() = runTest {
        val repository = FakeItemDetailRepository(milk)
        val viewModel = ItemDetailViewModel(repository, milk.id)

        viewModel.delete()

        assertTrue(viewModel.uiState.value.isDeleted)
        assertNull(repository.storedItem(milk.id))
    }

    @Test
    fun unknownItemReportsMissingInsteadOfStayingInLoading() = runTest {
        val viewModel = ItemDetailViewModel(FakeItemDetailRepository(item = null), itemId = 404)

        assertTrue(viewModel.uiState.value.isMissing)
    }

    @Test
    fun persistedEditsReachTheScreenInsteadOfStaleValues() = runTest {
        val repository = FakeItemDetailRepository(milk)
        val viewModel = ItemDetailViewModel(repository, milk.id)

        repository.rename(milk.id, "저지방 우유")

        assertEquals("저지방 우유", viewModel.uiState.value.item?.name)
    }
}

private class FakeItemDetailRepository(item: ItemEntity?) : ItemDetailRepository {

    private val stored = MutableStateFlow(item?.let { listOf(it) } ?: emptyList())

    var consumeCount = 0

    override fun observeActiveById(id: Long): Flow<ItemEntity?> =
        stored.map { items -> items.firstOrNull { it.id == id && !it.isArchived } }

    override suspend fun getById(id: Long): ItemEntity? = storedItem(id)

    override suspend fun markConsumed(id: Long) {
        consumeCount++
        stored.value = stored.value.map {
            if (it.id == id) it.copy(isArchived = true, consumedDate = LocalDate.now()) else it
        }
    }

    override suspend fun restoreConsumed(id: Long) {
        stored.value = stored.value.map {
            if (it.id == id) it.copy(isArchived = false, consumedDate = null) else it
        }
    }

    override suspend fun delete(item: ItemEntity) {
        stored.value = stored.value.filterNot { it.id == item.id }
    }

    fun storedItem(id: Long): ItemEntity? = stored.value.firstOrNull { it.id == id }

    fun activeItems(): List<ItemEntity> = stored.value.filterNot { it.isArchived }

    fun rename(id: Long, name: String) {
        stored.value = stored.value.map { if (it.id == id) it.copy(name = name) else it }
    }
}
