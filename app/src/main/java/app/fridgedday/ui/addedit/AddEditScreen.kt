package app.fridgedday.ui.addedit

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
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
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import app.fridgedday.data.db.AppDatabase
import app.fridgedday.data.db.entity.StorageLocation
import app.fridgedday.data.repo.ItemRepository
import app.fridgedday.ui.components.DatePickerField
import app.fridgedday.ui.navigation.Destinations
import app.fridgedday.util.DateUtils
import java.time.LocalDate

private val NotifyDayOptions = listOf(3, 5, 7)

/** 메모 필드의 최소 높이. 글자 확대에서는 이 높이 안의 줄 수가 함께 늘어난다. */
private val NoteMinHeight = 100.dp

/**
 * 식품 등록·수정 화면.
 *
 * 기본 노출 필드는 유통기한·식품명·보관 위치뿐이고 수량·메모·알림은 "추가 정보" 뒤에 둔다.
 * 카메라/갤러리 OCR은 [app.fridgedday.ui.scan.ScanScreen]이 담당하므로 이 화면에는 없다.
 *
 * [initialConfirmedDate]는 OCR 확인 화면에서 이미 사용자가 확정한 날짜다. 값이 있으면 그 날짜가
 * 확정 상태로 시작하지만, 저장은 언제나 사용자가 "저장"을 눌렀을 때만 일어난다.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditScreen(
    navController: NavHostController,
    itemId: Long?,
    initialConfirmedDate: LocalDate? = null
) {
    val context = LocalContext.current
    val repository = remember {
        ItemRepository(AppDatabase.getDatabase(context).itemDao())
    }
    val viewModel: AddEditViewModel = viewModel(
        key = "add-edit-${itemId ?: "new"}",
        factory = AddEditViewModelFactory(repository, itemId, initialConfirmedDate)
    )
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val focusManager = LocalFocusManager.current
    var showAdditionalInfo by remember { mutableStateOf(false) }
    var expandedLocation by remember { mutableStateOf(false) }

    // 저장 성공 시 새 등록이면 Today가 그 행을 찾을 수 있도록 이전 화면에 저장소가 돌려준 id를
    // 남긴다. 그다음 입력 포커스와 키보드를 정리하고 뒤로 간다. 저장 직후에도 키보드가 남아 화면
    // 전환이 가려지던 문제도 여기서 없앤다.
    LaunchedEffect(uiState.isSaved) {
        if (!uiState.isSaved) return@LaunchedEffect

        uiState.savedItemId?.let { savedItemId ->
            navController.previousBackStackEntry
                ?.savedStateHandle
                ?.set(Destinations.SAVED_ITEM_ID_KEY, savedItemId)
        }
        focusManager.clearFocus()
        navController.popBackStack()
    }

    // 에러 메시지 표시
    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let { message ->
            snackbarHostState.showSnackbar(message)
            viewModel.clearError()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (uiState.isEditMode) "항목 수정" else "식품 등록") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "뒤로가기")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        // 저장은 폼과 함께 스크롤되지 않는 하단 고정 버튼 하나뿐이다. "추가 정보"를 펼쳐도 버튼은
        // 제자리에 남고, 내비게이션 바와 키보드(IMM) 위에 뜬다.
        bottomBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .imePadding()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Button(
                    onClick = { viewModel.saveItem() },
                    enabled = !uiState.isLoading,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                ) {
                    Text("저장")
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Expiry Date. 화면이 따로 붙이던 "유통기한" 라벨은 없애고 필드 라벨 하나로 합쳤다.
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                DatePickerField(
                    label = "유통기한 *",
                    selectedDate = uiState.expiryDate,
                    onDateSelected = { viewModel.confirmManualExpiryDate(it) },
                    isError = uiState.expiryDateError != null,
                    supportingText = uiState.expiryDateError,
                    modifier = Modifier.fillMaxWidth()
                )
                ExpiryDateStatus(uiState)
            }

            // Name
            OutlinedTextField(
                value = uiState.name,
                onValueChange = { viewModel.updateName(it) },
                label = { Text("식품명 *") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                isError = uiState.nameError != null,
                supportingText = uiState.nameError?.let { nameError ->
                    { Text(nameError) }
                }
            )

            // Storage Location
            ExposedDropdownMenuBox(
                expanded = expandedLocation,
                onExpandedChange = { expandedLocation = it }
            ) {
                OutlinedTextField(
                    value = storageLocationLabel(uiState.location),
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("보관 위치 *") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expandedLocation) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor()
                )
                ExposedDropdownMenu(
                    expanded = expandedLocation,
                    onDismissRequest = { expandedLocation = false }
                ) {
                    StorageLocation.entries.forEach { location ->
                        DropdownMenuItem(
                            text = { Text(storageLocationLabel(location)) },
                            onClick = {
                                viewModel.updateLocation(location)
                                expandedLocation = false
                            }
                        )
                    }
                }
            }

            HorizontalDivider()

            TextButton(
                onClick = { showAdditionalInfo = !showAdditionalInfo },
                modifier = Modifier.heightIn(min = 48.dp)
            ) {
                Icon(
                    imageVector = if (showAdditionalInfo) {
                        Icons.Default.ExpandLess
                    } else {
                        Icons.Default.ExpandMore
                    },
                    contentDescription = null
                )
                Spacer(Modifier.width(8.dp))
                Text(if (showAdditionalInfo) "추가 정보 접기" else "추가 정보")
            }

            if (showAdditionalInfo) {
                // Quantity & Unit
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = uiState.quantity,
                        onValueChange = { viewModel.updateQuantity(it) },
                        label = { Text("수량") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = uiState.unit,
                        onValueChange = { viewModel.updateUnit(it) },
                        label = { Text("단위") },
                        placeholder = { Text("개, g, ml") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }

                // Days Before Notify
                Column {
                    Text(
                        text = "임박 알림: ${uiState.daysBeforeNotify}일 전",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        NotifyDayOptions.forEach { days ->
                            FilterChip(
                                selected = uiState.daysBeforeNotify == days,
                                onClick = { viewModel.updateDaysBeforeNotify(days) },
                                label = { Text("${days}일") }
                            )
                        }
                    }
                }

                // Note
                OutlinedTextField(
                    value = uiState.note,
                    onValueChange = { viewModel.updateNote(it) },
                    label = { Text("메모") },
                    // 높이를 고정하면 글자 확대에서 적은 줄만 보인다. 최소 높이 안에서 늘어나게 두고
                    // 그보다 긴 메모는 필드 자체 스크롤로 읽는다.
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = NoteMinHeight),
                    maxLines = 6
                )
            }

            if (uiState.isLoading) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }
        }
    }
}

/**
 * 저장 전에 유통기한이 확정됐음을 분명히 보여준다. 확정되지 않은 날짜는 저장되지 않으므로 확정
 * 상태에서만 표시하고, 처음 들어온 화면에는 붉은 필수 오류를 미리 띄우지 않는다. 저장을 눌러
 * 빠진 항목이 확인되면 그 오류는 필드 자체([DatePickerField]의 `supportingText`)가 보여준다.
 */
@Composable
private fun ExpiryDateStatus(uiState: AddEditUiState) {
    val expiryDate = uiState.expiryDate
    if (expiryDate == null || !uiState.isExpiryDateConfirmed) return

    Row(
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.CheckCircle,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(16.dp)
        )
        Text(
            text = "확인된 날짜: ${DateUtils.formatKorean(expiryDate)}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

private fun storageLocationLabel(location: StorageLocation): String = when (location) {
    StorageLocation.FRIDGE -> "냉장"
    StorageLocation.FREEZER -> "냉동"
    StorageLocation.PANTRY -> "실온"
}
