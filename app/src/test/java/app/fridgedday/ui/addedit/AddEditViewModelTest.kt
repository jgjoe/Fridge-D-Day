package app.fridgedday.ui.addedit

import app.fridgedday.data.db.entity.ItemEntity
import app.fridgedday.data.db.entity.StorageLocation
import app.fridgedday.data.repo.AddEditItemRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class AddEditViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun initialEntryShowsNoRequiredFieldErrors() {
        val repository = FakeAddEditItemRepository()
        val viewModel = AddEditViewModel(repository, itemId = null)

        // 저장을 누르기 전에는 이름·유통기한 어느 쪽도 붉게 표시하지 않는다.
        val state = viewModel.uiState.value
        assertFalse(state.hasAttemptedSave)
        assertNull(state.nameError)
        assertNull(state.expiryDateError)
        assertNull(state.errorMessage)
        assertFalse(state.isSaved)
    }

    @Test
    fun submitReportsTheNameAndExpiryErrorsIndependently() {
        val repository = FakeAddEditItemRepository()
        val viewModel = AddEditViewModel(repository, itemId = null)

        viewModel.saveItem()

        // 한 번의 저장 시도가 비어 있는 필수 필드를 각각 표시한다. 먼저 걸린 하나만 보이지 않는다.
        assertTrue(viewModel.uiState.value.hasAttemptedSave)
        assertEquals("이름을 입력해주세요", viewModel.uiState.value.nameError)
        assertEquals("유통기한을 선택하고 확인해주세요", viewModel.uiState.value.expiryDateError)
        assertNull(viewModel.uiState.value.errorMessage)
        assertNull(viewModel.uiState.value.savedItemId)
        assertTrue(repository.insertedItems.isEmpty())
    }

    @Test
    fun blankEditBeforeSubmitStaysNeutral() {
        val repository = FakeAddEditItemRepository()
        val viewModel = AddEditViewModel(repository, itemId = null)

        // 저장을 누르기 전에는 이름을 입력했다가 공백으로 되돌려도 필수 오류가 붙지 않는다.
        viewModel.updateName("우유")
        viewModel.updateName("   ")

        assertNull(viewModel.uiState.value.nameError)
    }

    @Test
    fun blankEditAfterSubmitKeepsTheRequiredNameError() {
        val repository = FakeAddEditItemRepository()
        val viewModel = AddEditViewModel(repository, itemId = null)
        viewModel.saveItem()

        // 저장 실패 뒤 유효한 이름으로 고쳤다가 다시 공백으로 되돌리면, 저장 시도 기록을 기준으로
        // 필수 오류가 다시 켜진다. 유효 → 공백 단계에서 오류가 사라진 채 남지 않는다.
        viewModel.updateName("우유")
        assertNull(viewModel.uiState.value.nameError)

        viewModel.updateName("   ")
        assertEquals("이름을 입력해주세요", viewModel.uiState.value.nameError)
    }

    @Test
    fun fixingTheNameClearsOnlyTheNameError() {
        val repository = FakeAddEditItemRepository()
        val viewModel = AddEditViewModel(repository, itemId = null)
        viewModel.saveItem()

        viewModel.updateName("우유")

        assertNull(viewModel.uiState.value.nameError)
        assertEquals("유통기한을 선택하고 확인해주세요", viewModel.uiState.value.expiryDateError)
    }

    @Test
    fun confirmingTheDateClearsOnlyTheExpiryError() {
        val repository = FakeAddEditItemRepository()
        val viewModel = AddEditViewModel(repository, itemId = null)
        viewModel.saveItem()

        viewModel.confirmManualExpiryDate(LocalDate.of(2026, 9, 30))

        assertNull(viewModel.uiState.value.expiryDateError)
        assertEquals("이름을 입력해주세요", viewModel.uiState.value.nameError)
    }

    @Test
    fun expiryValidationStaysIndependentOfTheRetainedNameError() {
        val repository = FakeAddEditItemRepository()
        val viewModel = AddEditViewModel(repository, itemId = null)
        viewModel.saveItem()

        // 이름이 공백으로 남아 오류가 유지돼도 유통기한 검증은 따로 움직인다.
        viewModel.updateName("   ")
        viewModel.confirmManualExpiryDate(LocalDate.of(2026, 9, 30))

        assertEquals("이름을 입력해주세요", viewModel.uiState.value.nameError)
        assertNull(viewModel.uiState.value.expiryDateError)
    }

    @Test
    fun secondSubmitKeepsOnlyTheStillMissingFieldError() {
        val repository = FakeAddEditItemRepository()
        val viewModel = AddEditViewModel(repository, itemId = null)
        viewModel.saveItem()

        viewModel.updateName("우유")
        viewModel.saveItem()

        assertNull(viewModel.uiState.value.nameError)
        assertEquals("유통기한을 선택하고 확인해주세요", viewModel.uiState.value.expiryDateError)
        assertTrue(repository.insertedItems.isEmpty())
    }

    @Test
    fun newRegistrationKeepsTheIdReturnedByInsert() = runTest {
        val repository = FakeAddEditItemRepository()
        val viewModel = AddEditViewModel(repository, itemId = null)

        viewModel.updateName("우유")
        viewModel.confirmManualExpiryDate(LocalDate.of(2026, 9, 30))
        viewModel.saveItem()
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.isSaved)
        assertEquals(FakeAddEditItemRepository.InsertedId, viewModel.uiState.value.savedItemId)
    }

    @Test
    fun ocrProposalIsNotPersistedUntilUserConfirms() = runTest {
        val repository = FakeAddEditItemRepository()
        val viewModel = AddEditViewModel(repository, itemId = null)
        val ocrDate = LocalDate.of(2026, 8, 20)

        viewModel.updateName("요거트")
        viewModel.proposeOcrDate(ocrDate)
        viewModel.saveItem()

        assertEquals(ocrDate, viewModel.uiState.value.pendingOcrDate)
        assertNull(viewModel.uiState.value.expiryDate)
        assertTrue(repository.insertedItems.isEmpty())

        viewModel.confirmPendingOcrDate()
        viewModel.saveItem()
        advanceUntilIdle()

        assertEquals(ocrDate, repository.insertedItems.single().expiryDate)
        assertTrue(viewModel.uiState.value.isSaved)
    }

    @Test
    fun cancellingOcrProposalKeepsPreviouslyConfirmedDate() {
        val repository = FakeAddEditItemRepository()
        val viewModel = AddEditViewModel(repository, itemId = null)
        val manualDate = LocalDate.of(2026, 9, 1)

        viewModel.confirmManualExpiryDate(manualDate)
        viewModel.proposeOcrDate(LocalDate.of(2026, 8, 1))
        viewModel.cancelPendingOcrDate()

        assertEquals(manualDate, viewModel.uiState.value.expiryDate)
        assertTrue(viewModel.uiState.value.isExpiryDateConfirmed)
        assertNull(viewModel.uiState.value.pendingOcrDate)
    }

    @Test
    fun correctingOcrProposalPersistsCorrectedDate() = runTest {
        val repository = FakeAddEditItemRepository()
        val viewModel = AddEditViewModel(repository, itemId = null)
        val correctedDate = LocalDate.of(2026, 10, 15)

        viewModel.updateName("두부")
        viewModel.proposeOcrDate(LocalDate.of(2026, 10, 5))
        viewModel.confirmManualExpiryDate(correctedDate)
        viewModel.saveItem()
        advanceUntilIdle()

        assertEquals(correctedDate, repository.insertedItems.single().expiryDate)
        assertNull(viewModel.uiState.value.pendingOcrDate)
    }

    @Test
    fun ocrConfirmedInitialDateStartsConfirmedButStillNeedsTheNormalSaveRequirements() = runTest {
        val repository = FakeAddEditItemRepository()
        val confirmedDate = LocalDate.of(2026, 8, 20)
        val viewModel = AddEditViewModel(
            repository,
            itemId = null,
            initialConfirmedDate = confirmedDate
        )

        assertEquals(confirmedDate, viewModel.uiState.value.expiryDate)
        assertTrue(viewModel.uiState.value.isExpiryDateConfirmed)

        // 확정된 날짜만으로는 저장되지 않는다. 이름 필드 오류가 저장을 막는다.
        viewModel.saveItem()
        advanceUntilIdle()
        assertEquals("이름을 입력해주세요", viewModel.uiState.value.nameError)
        assertNull(viewModel.uiState.value.expiryDateError)
        assertTrue(repository.insertedItems.isEmpty())
        assertFalse(viewModel.uiState.value.isSaved)

        viewModel.updateName("요거트")
        viewModel.saveItem()
        advanceUntilIdle()

        assertEquals(confirmedDate, repository.insertedItems.single().expiryDate)
        assertTrue(viewModel.uiState.value.isSaved)
        assertEquals(FakeAddEditItemRepository.InsertedId, viewModel.uiState.value.savedItemId)
    }

    @Test
    fun editingItemPreservesExistingMetadata() = runTest {
        val createdDate = LocalDate.of(2025, 11, 25)
        val consumedDate = LocalDate.of(2026, 7, 1)
        val existingItem = ItemEntity(
            id = 7,
            name = "기존 항목",
            location = StorageLocation.FREEZER,
            expiryDate = LocalDate.of(2026, 8, 1),
            isArchived = true,
            consumedDate = consumedDate,
            createdDate = createdDate
        )
        val repository = FakeAddEditItemRepository(existingItem)
        val viewModel = AddEditViewModel(repository, itemId = existingItem.id)
        advanceUntilIdle()

        viewModel.updateName("수정 항목")
        viewModel.confirmManualExpiryDate(LocalDate.of(2026, 8, 15))
        viewModel.saveItem()
        advanceUntilIdle()

        val updated = repository.updatedItems.single()
        assertEquals(existingItem.id, updated.id)
        assertEquals(createdDate, updated.createdDate)
        assertEquals(consumedDate, updated.consumedDate)
        assertTrue(updated.isArchived)
        assertEquals("수정 항목", updated.name)

        // 수정 저장은 새 행을 만들지 않는다. Today에 알릴 새 등록 id도 남지 않는다.
        assertTrue(viewModel.uiState.value.isSaved)
        assertNull(viewModel.uiState.value.savedItemId)
    }
}

private class FakeAddEditItemRepository(
    private val existingItem: ItemEntity? = null
) : AddEditItemRepository {

    val insertedItems = mutableListOf<ItemEntity>()
    val updatedItems = mutableListOf<ItemEntity>()

    override suspend fun getById(id: Long): ItemEntity? =
        existingItem?.takeIf { it.id == id }

    override suspend fun insert(item: ItemEntity): Long {
        insertedItems += item
        return InsertedId
    }

    override suspend fun update(item: ItemEntity) {
        updatedItems += item
    }

    companion object {
        /** 저장소가 새 행에 부여하는 id 대역. 그 값이 그대로 화면까지 가는지 확인한다. */
        const val InsertedId = 42L
    }
}
