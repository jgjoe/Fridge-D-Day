package app.fridgedday.ui.addedit

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import app.fridgedday.data.db.entity.ItemEntity
import app.fridgedday.data.db.entity.StorageLocation
import app.fridgedday.data.repo.AddEditItemRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate

data class AddEditUiState(
    val isEditMode: Boolean = false,
    val name: String = "",
    val category: String = "",
    val location: StorageLocation = StorageLocation.FRIDGE,
    val quantity: String = "",
    val unit: String = "",
    val expiryDate: LocalDate? = null,
    val isExpiryDateConfirmed: Boolean = false,
    val pendingOcrDate: LocalDate? = null,
    val daysBeforeNotify: Int = 3,
    val note: String = "",
    val isLoading: Boolean = false,

    /**
     * 저장(제출)을 한 번이라도 시도했는지. 시도 전에는 이름 입력이 중립이고, 시도 뒤에는 이름이
     * 공백이 될 때마다 필수 오류가 다시 켜진다.
     */
    val hasAttemptedSave: Boolean = false,

    /** 이름 필드에 붙는 검증 오류. 저장 시도 전에는 null이라 처음 들어온 화면이 붉게 보이지 않는다. */
    val nameError: String? = null,

    /** 유통기한 필드에 붙는 검증 오류. 이름 오류와 독립적으로 켜지고 그 필드를 고치면 꺼진다. */
    val expiryDateError: String? = null,

    /** 저장소/불러오기 실패처럼 필드에 귀속되지 않는 오류. 화면은 이 값만 Snackbar로 보여준다. */
    val errorMessage: String? = null,
    val isSaved: Boolean = false,

    /**
     * 새 등록 저장이 성공했을 때 [AddEditItemRepository.insert]가 돌려준 id.
     *
     * 수정 저장은 기존 행이라 새 id가 없으므로 계속 null이다. 화면은 이 값이 있을 때만 Today에
     * 방금 등록한 행을 알린다.
     */
    val savedItemId: Long? = null
)

class AddEditViewModel(
    private val repository: AddEditItemRepository,
    private val itemId: Long?,
    initialConfirmedDate: LocalDate? = null
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        AddEditUiState(
            isLoading = itemId != null,
            expiryDate = initialConfirmedDate,
            isExpiryDateConfirmed = initialConfirmedDate != null
        )
    )
    val uiState: StateFlow<AddEditUiState> = _uiState.asStateFlow()
    private var originalItem: ItemEntity? = null

    init {
        if (itemId != null) {
            loadItem(itemId)
        }
    }

    private fun loadItem(id: Long) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val item = repository.getById(id)
                if (item != null) {
                    originalItem = item
                    _uiState.update {
                        it.copy(
                            isEditMode = true,
                            name = item.name,
                            category = item.category ?: "",
                            location = item.location,
                            quantity = item.quantity?.toString() ?: "",
                            unit = item.unit ?: "",
                            expiryDate = item.expiryDate,
                            isExpiryDateConfirmed = true,
                            daysBeforeNotify = item.daysBeforeNotify,
                            note = item.note ?: "",
                            isLoading = false
                        )
                    }
                } else {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = "항목을 찾을 수 없습니다"
                        )
                    }
                }
            } catch (_: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = "항목을 불러오지 못했습니다"
                    )
                }
            }
        }
    }

    fun updateName(name: String) {
        // 저장 시도 전에는 중립이라 입력만으로는 화면이 붉어지지 않는다. 저장 시도 뒤에는 이름이
        // 공백이 될 때마다 필수 오류를 다시 켜고, 유효한 값에서는 지운다. 이전 오류가 남아 있는지가
        // 아니라 저장 시도 여부가 기준이라, 유효한 값으로 고쳤다가 공백으로 되돌려도 오류가 보인다.
        _uiState.update { state ->
            state.copy(
                name = name,
                nameError = when {
                    !state.hasAttemptedSave -> state.nameError
                    name.isBlank() -> "이름을 입력해주세요"
                    else -> null
                }
            )
        }
    }

    fun updateLocation(location: StorageLocation) {
        _uiState.update { it.copy(location = location) }
    }

    fun updateQuantity(quantity: String) {
        _uiState.update { it.copy(quantity = quantity) }
    }

    fun updateUnit(unit: String) {
        _uiState.update { it.copy(unit = unit) }
    }

    fun confirmManualExpiryDate(date: LocalDate) {
        _uiState.update {
            it.copy(
                expiryDate = date,
                isExpiryDateConfirmed = true,
                pendingOcrDate = null,
                expiryDateError = null,
                errorMessage = null
            )
        }
    }

    fun proposeOcrDate(date: LocalDate) {
        _uiState.update {
            it.copy(
                pendingOcrDate = date,
                errorMessage = null
            )
        }
    }

    fun confirmPendingOcrDate() {
        _uiState.update { state ->
            val pendingDate = state.pendingOcrDate ?: return@update state
            state.copy(
                expiryDate = pendingDate,
                isExpiryDateConfirmed = true,
                pendingOcrDate = null,
                expiryDateError = null,
                errorMessage = null
            )
        }
    }

    fun cancelPendingOcrDate() {
        _uiState.update { it.copy(pendingOcrDate = null) }
    }

    fun updateDaysBeforeNotify(days: Int) {
        _uiState.update { it.copy(daysBeforeNotify = days) }
    }

    fun updateNote(note: String) {
        _uiState.update { it.copy(note = note) }
    }

    fun saveItem() {
        // 저장 시도 자체를 검증보다 먼저 기록한다. 검증에서 돌아가도 이 값이 남아 이후 이름 입력이
        // 공백에 대해 필수 오류를 다시 켤 수 있다.
        _uiState.update { it.copy(hasAttemptedSave = true) }

        val state = _uiState.value

        // 두 필수 필드를 서로 독립적으로 검사한다. 한쪽만 고친 사용자는 다른 쪽 오류를 그대로
        // 보고, 저장 시도 전에는 둘 다 null이라 처음 들어온 화면이 붉게 보이지 않는다.
        val nameError = if (state.name.isBlank()) "이름을 입력해주세요" else null
        val expiryDate = state.expiryDate
        val expiryDateError = if (expiryDate == null || !state.isExpiryDateConfirmed) {
            "유통기한을 선택하고 확인해주세요"
        } else {
            null
        }

        if (nameError != null || expiryDate == null || !state.isExpiryDateConfirmed) {
            _uiState.update { it.copy(nameError = nameError, expiryDateError = expiryDateError) }
            return
        }

        if (itemId != null && originalItem == null) {
            _uiState.update { it.copy(errorMessage = "수정할 항목을 불러오지 못했습니다") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            try {
                val quantityValue = state.quantity.toFloatOrNull()

                val existingItem = originalItem
                val item = existingItem?.copy(
                    name = state.name.trim(),
                    category = state.category.trim().ifBlank { null },
                    location = state.location,
                    quantity = quantityValue,
                    unit = state.unit.trim().ifBlank { null },
                    expiryDate = expiryDate,
                    daysBeforeNotify = state.daysBeforeNotify,
                    note = state.note.trim().ifBlank { null }
                ) ?: ItemEntity(
                    name = state.name.trim(),
                    category = state.category.trim().ifBlank { null },
                    location = state.location,
                    quantity = quantityValue,
                    unit = state.unit.trim().ifBlank { null },
                    expiryDate = expiryDate,
                    daysBeforeNotify = state.daysBeforeNotify,
                    note = state.note.trim().ifBlank { null }
                )

                if (existingItem != null) {
                    // 수정은 기존 행을 덮어쓸 뿐 새 id를 만들지 않는다. 화면이 Today에 알릴 id도 없다.
                    repository.update(item)
                    _uiState.update { it.copy(isLoading = false, isSaved = true) }
                } else {
                    // 새 등록은 저장소가 돌려준 id를 그대로 보관한다. 화면은 이 id로 Today에 알린다.
                    val newItemId = repository.insert(item)
                    _uiState.update {
                        it.copy(isLoading = false, isSaved = true, savedItemId = newItemId)
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = "저장 중 오류가 발생했습니다: ${e.message}"
                    )
                }
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}

class AddEditViewModelFactory(
    private val repository: AddEditItemRepository,
    private val itemId: Long?,
    private val initialConfirmedDate: LocalDate? = null
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        require(modelClass.isAssignableFrom(AddEditViewModel::class.java))
        return AddEditViewModel(repository, itemId, initialConfirmedDate) as T
    }
}
