package app.fridgedday.ui.statistics

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavHostController
import app.fridgedday.data.db.AppDatabase
import app.fridgedday.data.repo.ItemRepository
import app.fridgedday.ui.components.FoodCategoryMark
import app.fridgedday.ui.components.IconDisc
import app.fridgedday.ui.components.foodMetadata
import app.fridgedday.ui.components.resolveFoodGroup
import app.fridgedday.ui.navigation.LocalTopLevelBottomContentInset
import app.fridgedday.ui.navigation.TopLevelDestination
import app.fridgedday.ui.navigation.TopLevelScaffold
import app.fridgedday.ui.navigation.TopLevelSearchField
import app.fridgedday.ui.navigation.TopLevelSidePadding
import app.fridgedday.util.DateUtils
import java.time.LocalDate

private val SidePadding = TopLevelSidePadding
private val RailWidth = 14.dp
private val RecordArtSize = 48.dp

@Composable
fun RecordScreen(navController: NavHostController) {
    val viewModel: RecordViewModel = viewModel(factory = recordViewModelFactory)
    val uiState by viewModel.uiState.collectAsState()
    val message by viewModel.message.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var searchExpanded by remember { mutableStateOf(false) }

    LaunchedEffect(message) {
        message?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessage()
        }
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, viewModel) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) viewModel.refreshDate()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    TopLevelScaffold(
        selected = TopLevelDestination.LOG,
        navController = navController,
        onSearchClick = {
            if (searchExpanded) viewModel.setSearchKeyword("")
            searchExpanded = !searchExpanded
        },
        showAddAction = true,
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
            RecordHeading()
            RecordFilterChips(selected = uiState.filter, onSelect = viewModel::setFilter)
            if (uiState.days.isEmpty()) {
                RecordEmptyState(filtered = uiState.filter != RecordFilter.ALL || uiState.searchKeyword.isNotBlank())
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        start = SidePadding,
                        end = SidePadding,
                        bottom = 16.dp + LocalTopLevelBottomContentInset.current
                    )
                ) {
                    items(uiState.days, key = { it.date }) { day ->
                        RecordDayGroup(
                            day = day,
                            onRestore = viewModel::restore
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RecordHeading() {
    Column(modifier = Modifier.padding(horizontal = SidePadding, vertical = 8.dp)) {
        Text(
            text = "식품 기록",
            style = MaterialTheme.typography.headlineSmall.copy(
                fontSize = 22.sp,
                lineHeight = 28.sp,
                fontWeight = FontWeight.Bold
            ),
            modifier = Modifier.semantics { heading() }
        )
        Text(
            text = "지금까지 이런 기록이 있어요.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 3.dp)
        )
    }
}

@Composable
private fun RecordFilterChips(selected: RecordFilter, onSelect: (RecordFilter) -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = SidePadding, vertical = 6.dp)
            .height(48.dp)
            .selectableGroup(),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(40.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
            shape = RoundedCornerShape(12.dp),
            tonalElevation = 0.dp
        ) {}
        Row(modifier = Modifier.fillMaxSize()) {
            RecordFilter.entries.forEach { filter ->
                val isSelected = selected == filter
                val interactionSource = remember(filter) { MutableInteractionSource() }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
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
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(40.dp),
                        color = if (isSelected) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            Color.Transparent
                        },
                        contentColor = if (isSelected) {
                            MaterialTheme.colorScheme.onPrimary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        shape = RoundedCornerShape(10.dp),
                        tonalElevation = 0.dp
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = recordFilterLabel(filter),
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = if (isSelected) {
                                    FontWeight.SemiBold
                                } else {
                                    FontWeight.Medium
                                },
                                maxLines = 1,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RecordDayGroup(
    day: RecordDay,
    onRestore: (Long) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp)
    ) {
        RecordRail()
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = dayLabel(day.date),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier
                    .padding(start = 8.dp, bottom = 8.dp)
                    .semantics { contentDescription = DateUtils.formatKorean(day.date) }
            )
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                contentColor = MaterialTheme.colorScheme.onSurface,
                shape = RoundedCornerShape(18.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                tonalElevation = 0.dp,
                shadowElevation = 1.dp
            ) {
                Column {
                    day.events.forEachIndexed { index, event ->
                        RecordEventRow(
                            event = event,
                            onRestore = { onRestore(event.item.id) }
                        )
                        if (index != day.events.lastIndex) {
                            HorizontalDivider(
                                modifier = Modifier.padding(start = 72.dp),
                                color = MaterialTheme.colorScheme.outlineVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RecordRail() {
    Box(
        modifier = Modifier
            .width(RailWidth)
            .fillMaxHeight(),
        contentAlignment = Alignment.TopCenter
    ) {
        Box(
            modifier = Modifier
                .padding(top = 5.dp)
                .width(1.dp)
                .fillMaxHeight()
                .background(MaterialTheme.colorScheme.outlineVariant)
        )
        Box(
            modifier = Modifier
                .padding(top = 4.dp)
                .size(10.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary)
        )
    }
}

@Composable
private fun RecordEventRow(
    event: RecordEvent,
    onRestore: () -> Unit
) {
    val restoreInteractionSource = remember { MutableInteractionSource() }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 68.dp)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        FoodCategoryMark(
            group = resolveFoodGroup(event.item.category, event.item.name),
            size = RecordArtSize
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = event.item.name,
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontSize = 16.sp,
                    lineHeight = 22.sp,
                    fontWeight = FontWeight.Medium
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = if (event.type == RecordEventType.REGISTERED) {
                    "${foodMetadata(event.item.location, event.item.category, event.item.name)} · " +
                        recordEventLabel(event.type)
                } else {
                    foodMetadata(event.item.location, event.item.category, event.item.name)
                },
                style = MaterialTheme.typography.bodySmall.copy(
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                    fontWeight = FontWeight.Normal
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        when (event.type) {
            RecordEventType.CONSUMED -> {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    RecordEventTypeLabel(event.type)
                    Box(
                        modifier = Modifier
                            .heightIn(min = 48.dp)
                            .clickable(
                                interactionSource = restoreInteractionSource,
                                indication = null,
                                role = Role.Button,
                                onClick = onRestore
                            )
                            .padding(horizontal = 12.dp)
                            .semantics { contentDescription = "${event.item.name} 복원" },
                        contentAlignment = Alignment.Center
                    ) {
                        Text("복원", color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
            RecordEventType.EXPIRED -> RecordEventTypeLabel(event.type)
            RecordEventType.REGISTERED -> Unit
        }
    }
}

@Composable
private fun RecordEventTypeLabel(type: RecordEventType) {
    if (type == RecordEventType.REGISTERED) return
    val isConsumed = type == RecordEventType.CONSUMED
    val container = if (isConsumed) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.errorContainer
    }
    val content = if (isConsumed) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.error
    }
    Surface(color = container, contentColor = content, shape = RoundedCornerShape(8.dp)) {
        Text(
            text = recordEventLabel(type),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

@Composable
private fun RecordEmptyState(filtered: Boolean) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(bottom = LocalTopLevelBottomContentInset.current)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        IconDisc(icon = Icons.Filled.History, size = 44.dp)
        Text(
            text = if (filtered) "조건에 맞는 기록이 없어요" else "아직 기록이 없습니다",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 10.dp)
        )
        Text(
            text = if (filtered) "검색어나 상태를 바꿔보세요." else "식품을 등록하면 기록이 여기에 쌓입니다",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 5.dp)
        )
    }
}

private fun recordFilterLabel(filter: RecordFilter): String = when (filter) {
    RecordFilter.ALL -> "전체"
    RecordFilter.CONSUMED -> "소비 완료"
    RecordFilter.EXPIRED -> "기한 지남"
}

private fun recordEventLabel(type: RecordEventType): String = when (type) {
    RecordEventType.REGISTERED -> "등록"
    RecordEventType.CONSUMED -> "완료"
    RecordEventType.EXPIRED -> "지남"
}

private fun dayLabel(date: LocalDate): String =
    if (date == LocalDate.now()) "${date.monthValue}월 ${date.dayOfMonth}일 (오늘)"
    else "${date.monthValue}월 ${date.dayOfMonth}일"


private val recordViewModelFactory: ViewModelProvider.Factory = viewModelFactory {
    initializer {
        val application = requireNotNull(
            this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY]
        )
        RecordViewModel(ItemRepository(AppDatabase.getDatabase(application).itemDao()))
    }
}
