package app.fridgedday.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.fridgedday.data.db.entity.ItemEntity
import app.fridgedday.data.db.entity.StorageLocation
import app.fridgedday.data.repo.ItemRepository
import app.fridgedday.util.DDayState
import app.fridgedday.util.DateUtils
import app.fridgedday.util.getDDayState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class FilterType { ALL, NEEDS_ATTENTION, SOON, REST }

enum class SortType { EXPIRY_DATE, NAME, CREATED_DATE }

data class TodayBucketCounts(
    val all: Int = 0,
    val needsAttention: Int = 0,
    val soon: Int = 0,
    val rest: Int = 0
) {
    fun count(filter: FilterType): Int = when (filter) {
        FilterType.ALL -> all
        FilterType.NEEDS_ATTENTION -> needsAttention
        FilterType.SOON -> soon
        FilterType.REST -> rest
    }
}

data class HomeUiState(
    val items: List<ItemEntity> = emptyList(),
    val totalItemCount: Int = 0,
    val filterType: FilterType = FilterType.ALL,
    val locationFilter: StorageLocation? = null,
    val sortType: SortType = SortType.EXPIRY_DATE,
    val searchKeyword: String = "",
    val isLoading: Boolean = false,
    val bucketCounts: TodayBucketCounts = TodayBucketCounts(),
    val sections: List<TodaySectionGroup> = emptyList(),
    val pendingRevealItemId: Long? = null
)

enum class TodaySection(val title: String) {
    NEEDS_ATTENTION("오늘 확인할 식품"),
    SOON("곧 만료돼요"),
    REST("여유 있어요")
}

data class TodaySectionGroup(
    val section: TodaySection,
    val items: List<ItemEntity>
)

internal fun buildTodaySections(items: List<ItemEntity>): List<TodaySectionGroup> {
    val grouped = items.groupBy { item ->
        when (getDDayState(DateUtils.daysUntil(item.expiryDate))) {
            DDayState.EXPIRED -> TodaySection.NEEDS_ATTENTION
            DDayState.WARNING -> TodaySection.SOON
            DDayState.SAFE -> TodaySection.REST
        }
    }
    return TodaySection.entries.mapNotNull { section ->
        grouped[section]?.takeIf { it.isNotEmpty() }?.let { TodaySectionGroup(section, it) }
    }
}

internal fun revealTargetIndex(sections: List<TodaySectionGroup>, itemId: Long): Int? {
    var headerOffset = 0
    sections.forEach { group ->
        val rowIndex = group.items.indexOfFirst { it.id == itemId }
        if (rowIndex >= 0) return headerOffset + 1 + rowIndex
        headerOffset += 1 + group.items.size
    }
    return null
}

internal fun itemMatchesFilter(item: ItemEntity, filter: FilterType): Boolean {
    if (filter == FilterType.ALL) return true
    val state = getDDayState(DateUtils.daysUntil(item.expiryDate))
    return when (filter) {
        FilterType.ALL -> true
        FilterType.NEEDS_ATTENTION -> state == DDayState.EXPIRED
        FilterType.SOON -> state == DDayState.WARNING
        FilterType.REST -> state == DDayState.SAFE
    }
}

internal fun todayBucketCounts(items: List<ItemEntity>): TodayBucketCounts {
    var needsAttention = 0
    var soon = 0
    var rest = 0
    items.forEach { item ->
        when (getDDayState(DateUtils.daysUntil(item.expiryDate))) {
            DDayState.EXPIRED -> needsAttention++
            DDayState.WARNING -> soon++
            DDayState.SAFE -> rest++
        }
    }
    return TodayBucketCounts(items.size, needsAttention, soon, rest)
}

class HomeViewModel(private val repository: ItemRepository) : ViewModel() {
    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init { loadItems() }

    private fun loadItems() {
        viewModelScope.launch {
            repository.observeAll()
                .combine(
                    _uiState.map { Triple(it.filterType, it.locationFilter, it.sortType) }
                ) { allItems, (filter, location, sort) ->
                    var visible = allItems.filter { itemMatchesFilter(it, filter) }
                    if (location != null) visible = visible.filter { it.location == location }
                    visible = sortItems(visible, sort)
                    Triple(visible, allItems.size, todayBucketCounts(allItems))
                }
                .combine(_uiState.map { it.searchKeyword }) { (items, total, counts), keyword ->
                    val visible = if (keyword.isBlank()) items else {
                        items.filter { it.name.contains(keyword.trim(), ignoreCase = true) }
                    }
                    Triple(visible, total, counts)
                }
                .collect { (items, total, counts) ->
                    _uiState.update {
                        it.copy(
                            items = items,
                            totalItemCount = total,
                            bucketCounts = counts,
                            sections = buildTodaySections(items),
                            isLoading = false
                        )
                    }
                }
        }
    }

    private fun sortItems(items: List<ItemEntity>, sort: SortType): List<ItemEntity> = when (sort) {
        SortType.EXPIRY_DATE -> items.sortedBy { it.expiryDate }
        SortType.NAME -> items.sortedBy { it.name }
        SortType.CREATED_DATE -> items.sortedByDescending { it.createdDate }
    }

    fun setFilter(filter: FilterType) = _uiState.update { it.copy(filterType = filter) }
    fun setLocationFilter(location: StorageLocation?) =
        _uiState.update { it.copy(locationFilter = location) }
    fun setSortType(sort: SortType) = _uiState.update { it.copy(sortType = sort) }
    fun setSearchKeyword(keyword: String) = _uiState.update { it.copy(searchKeyword = keyword) }

    fun revealItem(id: Long) {
        viewModelScope.launch {
            val item = repository.getById(id) ?: return@launch
            _uiState.update { state ->
                state.copy(
                    filterType = if (itemMatchesFilter(item, state.filterType)) state.filterType else FilterType.ALL,
                    locationFilter = state.locationFilter.takeIf { it == null || it == item.location },
                    searchKeyword = state.searchKeyword.takeIf {
                        it.isBlank() || item.name.contains(it, ignoreCase = true)
                    }.orEmpty(),
                    pendingRevealItemId = id
                )
            }
        }
    }

    fun consumeReveal() = _uiState.update { it.copy(pendingRevealItemId = null) }

    suspend fun restoreConsumed(id: Long): Boolean = try {
        if (repository.getById(id) == null) false else {
            repository.restoreConsumed(id)
            true
        }
    } catch (_: Exception) {
        false
    }

    fun archiveItem(id: Long) {
        viewModelScope.launch { repository.archive(id) }
    }
}
