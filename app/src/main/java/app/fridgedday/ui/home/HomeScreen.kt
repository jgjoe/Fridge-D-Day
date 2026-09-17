package app.fridgedday.ui.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import app.fridgedday.R
import app.fridgedday.data.db.AppDatabase
import app.fridgedday.data.db.entity.ItemEntity
import app.fridgedday.data.db.entity.StorageLocation
import app.fridgedday.data.pref.AppSettings
import app.fridgedday.data.pref.SettingsDataStore
import app.fridgedday.data.repo.ItemRepository
import app.fridgedday.ui.components.BundledArtwork
import app.fridgedday.ui.components.FoodRow
import app.fridgedday.ui.components.todayHeroDateLabel
import app.fridgedday.ui.navigation.Destinations
import app.fridgedday.ui.navigation.LocalTopLevelBottomContentInset
import app.fridgedday.ui.navigation.TopLevelDestination
import app.fridgedday.ui.navigation.TopLevelScaffold
import app.fridgedday.ui.navigation.TopLevelSearchField
import app.fridgedday.ui.navigation.TopLevelSidePadding
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate

private val SidePadding = TopLevelSidePadding
private val EmptyStateMaxWidth = 360.dp
private val EmptyStateHeroSize = 196.dp
private const val EmptyStateHeroZoom = 2.35f

@Composable
fun HomeScreen(navController: NavHostController) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val dataStore = remember { SettingsDataStore(context) }
    val settings by dataStore.settingsFlow.collectAsState(initial = AppSettings())
    val viewModel: HomeViewModel = viewModel(factory = homeViewModelFactory)
    val uiState by viewModel.uiState.collectAsState()
    val listState = rememberLazyListState()
    val snackbarHostState = remember { SnackbarHostState() }
    var showFilterSheet by remember { mutableStateOf(false) }
    var searchExpanded by remember { mutableStateOf(false) }

    val backStackEntry by navController.currentBackStackEntryAsState()
    LaunchedEffect(backStackEntry) {
        val handle = backStackEntry?.savedStateHandle ?: return@LaunchedEffect
        val savedItemId = handle.get<Long>(Destinations.SAVED_ITEM_ID_KEY) ?: return@LaunchedEffect
        handle.remove<Long>(Destinations.SAVED_ITEM_ID_KEY)
        viewModel.revealItem(savedItemId)
    }
    LaunchedEffect(backStackEntry) {
        val handle = backStackEntry?.savedStateHandle ?: return@LaunchedEffect
        val consumedItemId =
            handle.get<Long>(Destinations.CONSUMED_ITEM_ID_KEY) ?: return@LaunchedEffect
        handle.remove<Long>(Destinations.CONSUMED_ITEM_ID_KEY)
        scope.launch {
            val result = snackbarHostState.showSnackbar(
                message = "소비 완료로 표시했어요",
                actionLabel = "실행 취소",
                duration = SnackbarDuration.Long
            )
            if (result == SnackbarResult.ActionPerformed) {
                val restored = withContext(NonCancellable) {
                    viewModel.restoreConsumed(consumedItemId)
                }
                if (!restored) snackbarHostState.showSnackbar("실행 취소를 완료하지 못했습니다")
            }
        }
    }

    LaunchedEffect(uiState.pendingRevealItemId, uiState.items) {
        val id = uiState.pendingRevealItemId ?: return@LaunchedEffect
        val index = uiState.items.indexOfFirst { it.id == id }
        if (index >= 0) {
            listState.animateScrollToItem(index)
            viewModel.consumeReveal()
        }
    }


    val completeFirstLaunch: suspend () -> Unit = {
        if (settings.isFirstLaunch) dataStore.setFirstLaunchCompleted()
    }
    val openManualEntry: () -> Unit = {
        scope.launch {
            completeFirstLaunch()
            navController.navigate(Destinations.ADD)
        }
    }
    val openScan: () -> Unit = {
        scope.launch {
            completeFirstLaunch()
            navController.navigate(Destinations.SCAN) { launchSingleTop = true }
        }
    }

    LaunchedEffect(settings.isFirstLaunch, uiState.totalItemCount) {
        if (settings.isFirstLaunch && uiState.totalItemCount > 0) {
            withContext(NonCancellable) { dataStore.setFirstLaunchCompleted() }
        }
    }

    val resetFilters = {
        viewModel.setSearchKeyword("")
        viewModel.setLocationFilter(null)
        viewModel.setSortType(SortType.EXPIRY_DATE)
        viewModel.setFilter(FilterType.ALL)
    }

    TopLevelScaffold(
        selected = TopLevelDestination.TODAY,
        navController = navController,
        onSearchClick = {
            if (searchExpanded) viewModel.setSearchKeyword("")
            searchExpanded = !searchExpanded
        },
        showAddAction = uiState.totalItemCount > 0,
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (searchExpanded) {
                TopLevelSearchField(
                    value = uiState.searchKeyword,
                    onValueChange = viewModel::setSearchKeyword,
                    onClose = {
                        viewModel.setSearchKeyword("")
                        searchExpanded = false
                    }
                )
            }

            if (uiState.totalItemCount == 0) {
                when (todayEmptyState(settings.isFirstLaunch, uiState.totalItemCount)) {
                    TodayEmptyState.FIRST_USE -> FirstUseState(openManualEntry, openScan)
                    TodayEmptyState.NO_ITEMS, TodayEmptyState.NO_RESULTS ->
                        EmptyState(openManualEntry, openScan)
                }
            } else {
                TodayHeader(
                    totalItemCount = uiState.totalItemCount,
                    filterSortActive = isFilterSortActive(uiState),
                    onOpenFilterSheet = { showFilterSheet = true }
                )
                TodayFilterChips(
                    selected = uiState.filterType,
                    counts = uiState.bucketCounts,
                    onSelect = viewModel::setFilter
                )
                if (uiState.items.isEmpty()) {
                    NoResultsState(onResetClick = resetFilters)
                } else {
                    TodayFoodList(
                        items = uiState.items,
                        listState = listState,
                        onItemClick = { item ->
                            navController.navigate(Destinations.detailRoute(item.id))
                        }
                    )
                }
            }
        }
    }

    if (showFilterSheet) {
        FilterSortSheet(
            filterType = uiState.filterType,
            locationFilter = uiState.locationFilter,
            sortType = uiState.sortType,
            onFilterChange = viewModel::setFilter,
            onLocationChange = viewModel::setLocationFilter,
            onSortChange = viewModel::setSortType,
            onReset = resetFilters,
            onDismiss = { showFilterSheet = false }
        )
    }
}

@Composable
private fun TodayHeader(
    totalItemCount: Int,
    filterSortActive: Boolean,
    onOpenFilterSheet: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val actionContainer = if (filterSortActive) {
        MaterialTheme.colorScheme.secondaryContainer
    } else {
        MaterialTheme.colorScheme.surfaceContainerLow
    }
    val actionContent = if (filterSortActive) {
        MaterialTheme.colorScheme.onSecondaryContainer
    } else {
        MaterialTheme.colorScheme.primary
    }
    val actionBorder = if (filterSortActive) {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)
    } else {
        MaterialTheme.colorScheme.outlineVariant
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = SidePadding, end = 8.dp, top = 10.dp),
        verticalAlignment = Alignment.Top
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "오늘 확인할 식품",
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontSize = 22.sp,
                    lineHeight = 28.sp,
                    fontWeight = FontWeight.Bold
                ),
                modifier = Modifier.semantics { heading() }
            )
            Text(
                text = "${todayHeroDateLabel(LocalDate.now())} · 관리 중 ${totalItemCount}개",
                style = MaterialTheme.typography.bodySmall.copy(
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Normal
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 3.dp)
            )
        }
        Box(
            modifier = Modifier
                .size(48.dp)
                .clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    role = Role.Button,
                    onClick = onOpenFilterSheet
                )
                .semantics {
                    contentDescription = "필터 및 정렬"
                    stateDescription = if (filterSortActive) {
                        "필터 또는 정렬 적용됨"
                    } else {
                        "기본값"
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            Surface(
                color = actionContainer,
                contentColor = actionContent,
                shape = CircleShape,
                border = BorderStroke(1.dp, actionBorder),
                tonalElevation = 0.dp
            ) {
                Icon(
                    imageVector = Icons.Filled.Tune,
                    contentDescription = null,
                    modifier = Modifier
                        .size(36.dp)
                        .padding(8.dp)
                )
            }
        }
    }
}

@Composable
private fun TodayFilterChips(
    selected: FilterType,
    counts: TodayBucketCounts,
    onSelect: (FilterType) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .selectableGroup()
            .padding(horizontal = SidePadding, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        FilterType.entries.forEach { filter ->
            val isSelected = selected == filter
            val interactionSource = remember(filter) { MutableInteractionSource() }
            Box(
                modifier = Modifier
                    .heightIn(min = 48.dp)
                    .selectable(
                        selected = isSelected,
                        interactionSource = interactionSource,
                        indication = null,
                        role = Role.RadioButton,
                        onClick = { onSelect(filter) }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    color = if (isSelected) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant
                    },
                    contentColor = if (isSelected) {
                        MaterialTheme.colorScheme.onPrimary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    shape = RoundedCornerShape(12.dp),
                    tonalElevation = 0.dp
                ) {
                    Box(
                        modifier = Modifier
                            .height(40.dp)
                            .padding(horizontal = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "${filterLabel(filter)} ${counts.count(filter)}",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TodayFoodList(
    items: List<ItemEntity>,
    listState: androidx.compose.foundation.lazy.LazyListState,
    onItemClick: (ItemEntity) -> Unit
) {
    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = PaddingValues(
            start = SidePadding,
            end = SidePadding,
            bottom = 16.dp + LocalTopLevelBottomContentInset.current
        )
    ) {
        items(items.size, key = { items[it].id }) { index ->
            val item = items[index]
            Surface(
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                contentColor = MaterialTheme.colorScheme.onSurface,
                shape = RoundedCornerShape(18.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                tonalElevation = 0.dp,
                shadowElevation = 1.dp
            ) {
                FoodRow(item = item, onClick = { onItemClick(item) })
            }
        }
    }
}

@Composable
private fun TodayBlankState(onManualEntry: () -> Unit, onScan: () -> Unit) {
    val scanInteractionSource = remember { MutableInteractionSource() }
    val manualInteractionSource = remember { MutableInteractionSource() }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = EmptyStateMaxWidth)
                .padding(horizontal = 24.dp, vertical = 18.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "어떤 식품을 기록할까요?",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Start,
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics { heading() }
            )
            Text(
                text = "식품을 등록하고\n더 신선한 일상을 시작해보세요.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Start,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp)
            )
            BundledArtwork(
                resId = R.drawable.grocery_hero,
                size = EmptyStateHeroSize,
                zoom = EmptyStateHeroZoom,
                modifier = Modifier.padding(vertical = 10.dp)
            )
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = CircleShape,
                tonalElevation = 0.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 52.dp)
                        .clickable(
                            interactionSource = scanInteractionSource,
                            indication = null,
                            role = Role.Button,
                            onClick = onScan
                        )
                        .padding(horizontal = 24.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Filled.CameraAlt, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("바로 스캔하기")
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
            Surface(
                color = MaterialTheme.colorScheme.background,
                contentColor = MaterialTheme.colorScheme.primary,
                shape = CircleShape,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                tonalElevation = 0.dp
            ) {
                Row(
                    modifier = Modifier
                        .heightIn(min = 48.dp)
                        .clickable(
                            interactionSource = manualInteractionSource,
                            indication = null,
                            role = Role.Button,
                            onClick = onManualEntry
                        )
                        .padding(horizontal = 22.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Filled.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("직접 입력하기")
                }
            }
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.45f),
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                shape = MaterialTheme.shapes.medium
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Filled.Lightbulb,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "작은 기록이\n더 맛있는 일상을 만들어줘요.",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(start = 10.dp)
                    )
                }
            }
        }
    }
}

enum class TodayEmptyState { FIRST_USE, NO_ITEMS, NO_RESULTS }

fun todayEmptyState(isFirstLaunch: Boolean, totalItemCount: Int): TodayEmptyState = when {
    totalItemCount > 0 -> TodayEmptyState.NO_RESULTS
    isFirstLaunch -> TodayEmptyState.FIRST_USE
    else -> TodayEmptyState.NO_ITEMS
}

@Composable
fun FirstUseState(onManualEntry: () -> Unit, onScan: () -> Unit = {}) {
    TodayBlankState(onManualEntry = onManualEntry, onScan = onScan)
}

@Composable
fun EmptyState(onManualEntry: () -> Unit, onScan: () -> Unit = {}) {
    TodayBlankState(onManualEntry = onManualEntry, onScan = onScan)
}

@Composable
fun NoResultsState(onResetClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(bottom = LocalTopLevelBottomContentInset.current)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "조건에 맞는 식품이 없어요",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            text = "검색어 또는 필터를 바꿔보세요.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 6.dp)
        )
        Surface(
            modifier = Modifier.padding(top = 14.dp),
            color = MaterialTheme.colorScheme.background,
            contentColor = MaterialTheme.colorScheme.primary,
            shape = CircleShape,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            tonalElevation = 0.dp
        ) {
            Box(
                modifier = Modifier
                    .heightIn(min = 48.dp)
                    .clickable(
                        interactionSource = interactionSource,
                        indication = null,
                        role = Role.Button,
                        onClick = onResetClick
                    )
                    .padding(horizontal = 22.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("필터 초기화")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FilterSortSheet(
    filterType: FilterType,
    locationFilter: StorageLocation?,
    sortType: SortType,
    onFilterChange: (FilterType) -> Unit,
    onLocationChange: (StorageLocation?) -> Unit,
    onSortChange: (SortType) -> Unit,
    onReset: () -> Unit,
    onDismiss: () -> Unit
) {
    val resetInteractionSource = remember { MutableInteractionSource() }
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 32.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(start = 24.dp, end = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "필터 · 정렬",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f)
                )
                Box(
                    modifier = Modifier
                        .heightIn(min = 48.dp)
                        .clickable(
                            interactionSource = resetInteractionSource,
                            indication = null,
                            role = Role.Button,
                            onClick = onReset
                        )
                        .padding(horizontal = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("초기화", color = MaterialTheme.colorScheme.primary)
                }
            }
            FilterChoiceGroup("상태") {
                FilterType.entries.forEach { option ->
                    FilterChoiceRow(
                        label = filterLabel(option),
                        selected = filterType == option,
                        onSelect = { onFilterChange(option) }
                    )
                }
            }
            FilterGroupSeparator()
            FilterChoiceGroup("보관 위치") {
                FilterChoiceRow("전체", locationFilter == null) { onLocationChange(null) }
                StorageLocation.entries.forEach { option ->
                    FilterChoiceRow(
                        label = locationLabel(option),
                        selected = locationFilter == option,
                        onSelect = { onLocationChange(option) }
                    )
                }
            }
            FilterGroupSeparator()
            FilterChoiceGroup("정렬") {
                SortType.entries.forEach { option ->
                    FilterChoiceRow(
                        label = sortLabel(option),
                        selected = sortType == option,
                        onSelect = { onSortChange(option) }
                    )
                }
            }
        }
    }
}

@Composable
private fun FilterChoiceGroup(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 10.dp)
        )
        Column(modifier = Modifier.fillMaxWidth().selectableGroup(), content = content)
    }
}

@Composable
private fun FilterGroupSeparator() {
    HorizontalDivider(
        modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
        color = MaterialTheme.colorScheme.outlineVariant
    )
}

@Composable
private fun FilterChoiceRow(label: String, selected: Boolean, onSelect: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .selectable(
                selected = selected,
                interactionSource = interactionSource,
                indication = null,
                role = Role.RadioButton,
                onClick = onSelect
            )
            .padding(horizontal = 24.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
        if (selected) Icon(Icons.Filled.Check, contentDescription = null)
    }
}

internal fun isFilterSortActive(uiState: HomeUiState): Boolean =
    uiState.filterType != FilterType.ALL ||
        uiState.locationFilter != null ||
        uiState.sortType != SortType.EXPIRY_DATE

private fun filterLabel(filter: FilterType): String = when (filter) {
    FilterType.ALL -> "전체"
    FilterType.NEEDS_ATTENTION -> "임박"
    FilterType.SOON -> "보통"
    FilterType.REST -> "여유"
}

private fun sortLabel(sort: SortType): String = when (sort) {
    SortType.EXPIRY_DATE -> "유통기한 임박순"
    SortType.NAME -> "이름순"
    SortType.CREATED_DATE -> "등록일순"
}

private fun locationLabel(location: StorageLocation): String = when (location) {
    StorageLocation.FRIDGE -> "냉장"
    StorageLocation.FREEZER -> "냉동"
    StorageLocation.PANTRY -> "실온"
}

private val homeViewModelFactory: ViewModelProvider.Factory = viewModelFactory {
    initializer {
        val application = requireNotNull(
            this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY]
        )
        HomeViewModel(ItemRepository(AppDatabase.getDatabase(application).itemDao()))
    }
}
