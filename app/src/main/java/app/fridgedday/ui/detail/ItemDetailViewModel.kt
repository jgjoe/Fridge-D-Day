package app.fridgedday.ui.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import app.fridgedday.data.db.entity.ItemEntity
import app.fridgedday.data.repo.ItemDetailRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ItemDetailUiState(
    val isLoading: Boolean = true,
    val item: ItemEntity? = null,
    /** 소비 완료를 실행했다. 화면은 Today로 돌아가고 되돌리기는 Today가 맡는다. */
    val isConsumed: Boolean = false,
    val isDeleted: Boolean = false,
    val errorMessage: String? = null
) {
    /** 잘못된 id이거나 이미 삭제된 항목. */
    val isMissing: Boolean get() = !isLoading && item == null
}

/**
 * 상세 화면 상태.
 *
 * 소비 완료는 확인 모달 없이 즉시 저장하고 화면을 떠난다. 되돌리기는 Today가 띄우는 "실행 취소"가
 * 담당하므로 이 화면은 되돌리기 상태를 소유하지 않는다.
 */
class ItemDetailViewModel(
    private val repository: ItemDetailRepository,
    private val itemId: Long
) : ViewModel() {

    private val _uiState = MutableStateFlow(ItemDetailUiState())
    val uiState: StateFlow<ItemDetailUiState> = _uiState.asStateFlow()

    /** 중복 소비/실행 취소 요청을 막는 단일 요청 게이트. */
    private var consumeRequested = false

    init {
        viewModelScope.launch {
            repository.observeActiveById(itemId).collect { item ->
                _uiState.update { state ->
                    when {
                        // 소비 완료 직후에는 아카이브로 사라진 항목을 화면에서 지우지 않는다.
                        item == null && state.isConsumed -> state.copy(isLoading = false)
                        else -> state.copy(isLoading = false, item = item)
                    }
                }
            }
        }
    }

    fun consume() {
        if (consumeRequested) return
        consumeRequested = true
        viewModelScope.launch {
            try {
                repository.markConsumed(itemId)
                _uiState.update { it.copy(isConsumed = true) }
            } catch (_: Exception) {
                consumeRequested = false
                _uiState.update { it.copy(errorMessage = "소비 완료로 표시하지 못했습니다") }
            }
        }
    }

    fun delete() {
        val item = _uiState.value.item ?: return
        viewModelScope.launch {
            try {
                repository.delete(item)
                _uiState.update { it.copy(isDeleted = true) }
            } catch (_: Exception) {
                _uiState.update { it.copy(errorMessage = "삭제하지 못했습니다") }
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}

class ItemDetailViewModelFactory(
    private val repository: ItemDetailRepository,
    private val itemId: Long
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        require(modelClass.isAssignableFrom(ItemDetailViewModel::class.java))
        return ItemDetailViewModel(repository, itemId) as T
    }
}
