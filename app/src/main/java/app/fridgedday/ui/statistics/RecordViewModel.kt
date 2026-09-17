package app.fridgedday.ui.statistics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.fridgedday.data.repo.ItemRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.Duration
import java.time.LocalDate
import java.time.ZonedDateTime

enum class RecordFilter { ALL, CONSUMED, EXPIRED }

data class RecordUiState(
    val summary: RecordSummary = RecordSummary(consumedThisMonth = 0, expired = 0, active = 0),
    val days: List<RecordDay> = emptyList(),
    val filter: RecordFilter = RecordFilter.ALL,
    val searchKeyword: String = ""
)

internal fun millisUntilNextDate(now: ZonedDateTime): Long {
    val nextMidnight = now.toLocalDate().plusDays(1).atStartOfDay(now.zone)
    return Duration.between(now, nextMidnight).toMillis().coerceAtLeast(1L)
}

internal fun filterRecordDays(
    days: List<RecordDay>,
    filter: RecordFilter,
    searchKeyword: String
): List<RecordDay> {
    val keyword = searchKeyword.trim()
    return days.mapNotNull { day ->
        val events = day.events.filter { event ->
            val typeMatches = when (filter) {
                RecordFilter.ALL -> true
                RecordFilter.CONSUMED -> event.type == RecordEventType.CONSUMED
                RecordFilter.EXPIRED -> event.type == RecordEventType.EXPIRED
            }
            typeMatches && (keyword.isBlank() || event.item.name.contains(keyword, ignoreCase = true))
        }
        events.takeIf { it.isNotEmpty() }?.let { day.copy(events = it) }
    }
}

class RecordViewModel(
    private val repository: ItemRepository,
    private val clock: Clock = Clock.systemDefaultZone()
) : ViewModel() {
    private val _uiState = MutableStateFlow(RecordUiState())
    val uiState: StateFlow<RecordUiState> = _uiState.asStateFlow()
    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()
    private val today = MutableStateFlow(LocalDate.now(clock))
    private var allDays: List<RecordDay> = emptyList()

    init {
        viewModelScope.launch {
            combine(repository.observeAll(), today) { _, _ -> Unit }.collect { refresh() }
        }
        viewModelScope.launch {
            while (true) {
                delay(millisUntilNextDate(ZonedDateTime.now(clock)))
                refreshDate()
            }
        }
    }

    fun refreshDate() { today.value = LocalDate.now(clock) }

    fun setFilter(filter: RecordFilter) {
        _uiState.update { state ->
            state.copy(filter = filter, days = filterRecordDays(allDays, filter, state.searchKeyword))
        }
    }

    fun setSearchKeyword(keyword: String) {
        _uiState.update { state ->
            state.copy(
                searchKeyword = keyword,
                days = filterRecordDays(allDays, state.filter, keyword)
            )
        }
    }

    fun restore(itemId: Long) {
        viewModelScope.launch {
            try {
                repository.restoreConsumed(itemId)
                _message.value = "복원했어요. 오늘 목록에 다시 표시됩니다."
            } catch (_: Exception) {
                _message.value = "복원하지 못했습니다. 다시 시도해 주세요."
            }
        }
    }

    fun clearMessage() { _message.value = null }

    private suspend fun refresh() {
        val date = today.value
        val items = repository.getAllItems()
        allDays = buildRecordDays(items, date)
        _uiState.update { state ->
            state.copy(
                summary = buildRecordSummary(items, date),
                days = filterRecordDays(allDays, state.filter, state.searchKeyword)
            )
        }
    }
}
