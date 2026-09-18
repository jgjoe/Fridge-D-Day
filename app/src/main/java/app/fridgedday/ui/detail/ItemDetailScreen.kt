package app.fridgedday.ui.detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import app.fridgedday.data.db.AppDatabase
import app.fridgedday.data.db.entity.ItemEntity
import app.fridgedday.data.db.entity.StorageLocation
import app.fridgedday.data.repo.ItemRepository
import app.fridgedday.ui.components.quantityLabel
import app.fridgedday.ui.navigation.Destinations
import app.fridgedday.ui.theme.containerColor
import app.fridgedday.ui.theme.contentColor
import app.fridgedday.util.DateUtils
import app.fridgedday.util.getDDayState
import java.time.LocalDate

private val ActionMinHeight = 48.dp
private val InfoLabelWidth = 72.dp

/**
 * 식품 상세 화면. 저장된 정보만 보여주고, 소비 완료·수정·삭제를 이 화면에서 수행한다.
 *
 * 소비 완료는 확인 모달 없이 즉시 저장되고 "실행 취소" 스낵바로만 되돌린다. 되돌리지 않으면
 * 오늘 화면으로 돌아간다. 삭제만 명시적 확인을 거친다.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ItemDetailScreen(
    navController: NavHostController,
    itemId: Long
) {
    val context = LocalContext.current
    val repository = remember {
        ItemRepository(AppDatabase.getDatabase(context).itemDao())
    }
    val viewModel: ItemDetailViewModel = viewModel(
        key = "item-detail-$itemId",
        factory = ItemDetailViewModelFactory(repository, itemId)
    )
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var showDeleteDialog by remember { mutableStateOf(false) }

    // 삭제는 되돌릴 수 없으므로 확인 직후 오늘 화면으로 돌아간다.
    LaunchedEffect(uiState.isDeleted) {
        if (uiState.isDeleted) {
            navController.popBackStack()
        }
    }

    // 소비 완료는 즉시 처리하고 곧바로 Today로 돌아간다(D-015). 되돌리기 `실행 취소`는 Today가
    // 이 id를 받아 그 화면에서 띄우므로, 여기서는 이동 전에 id만 남긴다.
    LaunchedEffect(uiState.isConsumed) {
        if (!uiState.isConsumed) return@LaunchedEffect
        navController.previousBackStackEntry
            ?.savedStateHandle
            ?.set(Destinations.CONSUMED_ITEM_ID_KEY, itemId)
        navController.popBackStack()
    }

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let { message ->
            snackbarHostState.showSnackbar(message)
            viewModel.clearError()
        }
    }

    val item = uiState.item

    if (showDeleteDialog && item != null) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("삭제 확인") },
            text = { Text("'${item.name}'을(를) 삭제하시겠습니까?\n이 작업은 되돌릴 수 없습니다.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        viewModel.delete()
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("삭제")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("취소")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("식품 상세") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "뒤로가기")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        when {
            uiState.isLoading -> Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }

            item != null -> ItemDetailContent(
                item = item,
                actionsEnabled = !uiState.isConsumed,
                onConsume = { viewModel.consume() },
                onEdit = { navController.navigate(Destinations.editRoute(item.id)) },
                onDeleteRequest = { showDeleteDialog = true },
                modifier = Modifier.padding(paddingValues)
            )

            // 삭제 직후에는 잠깐 항목이 사라지므로 곧 돌아갈 화면을 다시 그리지 않는다.
            uiState.isDeleted -> Box(modifier = Modifier.fillMaxSize())

            else -> MissingItemState(
                onBackToToday = { navController.popBackStack() },
                modifier = Modifier.padding(paddingValues)
            )
        }
    }
}

@Composable
private fun ItemDetailContent(
    item: ItemEntity,
    actionsEnabled: Boolean,
    onConsume: () -> Unit,
    onEdit: () -> Unit,
    onDeleteRequest: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                text = item.name,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold
            )
            ExpiryHeadline(expiryDate = item.expiryDate)
        }

        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            DetailInfoRow(label = "보관 위치", value = storageLocationLabel(item.location))
            quantityLabel(item)?.let { DetailInfoRow(label = "수량", value = it) }
            DetailInfoRow(label = "알림", value = "유통기한 ${item.daysBeforeNotify}일 전")
            item.note?.let { DetailInfoRow(label = "메모", value = it) }
        }

        // 소비 완료 직후에는 실행 취소 스낵바가 유일한 선택지다. 확인 모달은 두지 않는다.
        if (actionsEnabled) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(
                    onClick = onConsume,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = ActionMinHeight)
                ) {
                    Text("소비 완료")
                }
                OutlinedButton(
                    onClick = onEdit,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = ActionMinHeight)
                ) {
                    Text("수정")
                }
                TextButton(
                    onClick = onDeleteRequest,
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = ActionMinHeight)
                ) {
                    Text("삭제")
                }
            }
        }
    }
}

/** 큰 D-Day와 절대 유통기한. 상태는 배지 문구 자체가 전달하므로 색에만 기대지 않는다. */
@Composable
private fun ExpiryHeadline(expiryDate: LocalDate) {
    val state = getDDayState(DateUtils.daysUntil(expiryDate))

    Surface(
        color = state.containerColor(),
        contentColor = state.contentColor(),
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = "유통기한",
                style = MaterialTheme.typography.labelLarge
            )
            Text(
                text = DateUtils.formatDDay(expiryDate),
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = DateUtils.formatKorean(expiryDate),
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
private fun DetailInfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(InfoLabelWidth)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f)
        )
    }
}

/** 잘못된 id·이미 삭제된 항목으로 상세에 들어온 경우. */
@Composable
private fun MissingItemState(
    onBackToToday: () -> Unit,
    modifier: Modifier = Modifier
) {
    // 글자 확대에서도 돌아가기 동작이 화면 밖으로 나가지 않도록 빈 상태를 스크롤 가능하게 둔다.
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "식품을 찾을 수 없어요",
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center
        )
        Text(
            text = "이미 삭제되었거나 잘못된 주소예요.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 8.dp)
        )
        TextButton(
            onClick = onBackToToday,
            modifier = Modifier
                .padding(top = 16.dp)
                .heightIn(min = ActionMinHeight)
        ) {
            Text("오늘로 돌아가기")
        }
    }
}

private fun storageLocationLabel(location: StorageLocation): String = when (location) {
    StorageLocation.FRIDGE -> "냉장"
    StorageLocation.FREEZER -> "냉동"
    StorageLocation.PANTRY -> "실온"
}
