package app.fridgedday.ui.settings

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavHostController
import app.fridgedday.BuildConfig
import app.fridgedday.data.db.AppDatabase
import app.fridgedday.data.pref.AppSettings
import app.fridgedday.data.pref.SettingsDataStore
import app.fridgedday.data.repo.ItemRepository
import app.fridgedday.data.repo.SettingsRepository
import app.fridgedday.ui.components.IconDisc
import app.fridgedday.ui.components.TimePickerDialog
import app.fridgedday.ui.navigation.GroupHairline
import app.fridgedday.ui.navigation.GroupSurface
import app.fridgedday.ui.navigation.LocalTopLevelBottomContentInset
import app.fridgedday.ui.navigation.TopLevelDestination
import app.fridgedday.ui.navigation.TopLevelScaffold
import app.fridgedday.ui.navigation.TopLevelSidePadding
import app.fridgedday.util.PermissionUtils
import app.fridgedday.util.backup.BackupManager
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

private val RowMinHeight = 48.dp

/** shell의 페이지 제목·다른 top-level 화면과 같은 세로선에 서는 좌우 기준 여백. */
private val SidePadding = TopLevelSidePadding

/** 그룹 표면 안쪽 행의 좌우 여백. */
private val GroupContentPadding = 16.dp
private const val DisabledContentAlpha = 0.38f

/** 임박 알림 기준은 1~14일 사이에서 하루 단위로만 움직인다. */
private val LeadTimeRange = 1f..14f
private const val LeadTimeSteps = 12

/**
 * 개인정보 안내 문구.
 *
 * 화면에 그대로 노출되는 계약 문구이므로 다른 문장과 섞지 않고 이 상수 하나로 고정한다.
 */
private const val PrivacyNotice =
    "식품 정보는 이 앱이 외부 서버로 전송하지 않아요. 백업 파일은 사용자가 직접 내보낼 때만 생성됩니다."

/**
 * 설정 화면.
 *
 * 긴 Material 컨트롤 더미 대신, 섹션 제목을 그룹 바깥에 두고 관련 행을 한 장의 둥근 표면으로 묶는
 * v2 Modern Fresh Utility 방향을 따른다. 저장 키, 백업 형식, 알림 예약 규칙은 그대로 두고
 * 문구와 배치만 다듬는다.
 */
@Composable
fun SettingsScreen(navController: NavHostController) {
    val context = LocalContext.current
    val viewModel: SettingsViewModel = viewModel(factory = settingsViewModelFactory)
    val settings by viewModel.settings.collectAsState()
    val dataStore = remember { SettingsDataStore(context) }
    val lifecycleOwner = LocalLifecycleOwner.current
    var hasNotificationPermission by remember {
        mutableStateOf(PermissionUtils.hasNotificationPermission(context))
    }
    var shouldShowNotificationRationale by remember {
        mutableStateOf(PermissionUtils.shouldShowNotificationPermissionRationale(context))
    }
    var showTimePicker by remember { mutableStateOf(false) }
    var isExporting by remember { mutableStateOf(false) }
    var isImporting by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    val itemRepository = remember {
        ItemRepository(AppDatabase.getDatabase(context).itemDao())
    }

    DisposableEffect(lifecycleOwner, context, settings.notificationPermissionDenied) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                hasNotificationPermission = PermissionUtils.hasNotificationPermission(context)
                shouldShowNotificationRationale =
                    PermissionUtils.shouldShowNotificationPermissionRationale(context)
                if (hasNotificationPermission && settings.notificationPermissionDenied) {
                    coroutineScope.launch {
                        withContext(NonCancellable) {
                            dataStore.recordNotificationPermissionResult(
                                isGranted = true,
                                shouldShowRationale = false
                            )
                        }
                    }
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        val rationale = PermissionUtils.shouldShowNotificationPermissionRationale(context)
        hasNotificationPermission = isGranted
        shouldShowNotificationRationale = rationale
        coroutineScope.launch {
            withContext(NonCancellable) {
                dataStore.recordNotificationPermissionResult(
                    isGranted = isGranted,
                    shouldShowRationale = rationale
                )
            }
        }
    }

    // 내보내기: 사용자가 고른 위치에만 쓰고, 진행 중에는 두 버튼을 함께 잠근다.
    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        coroutineScope.launch {
            isExporting = true
            try {
                val items = itemRepository.getAllItems()
                BackupManager.exportToJson(context, items, uri)
                    .onSuccess { snackbarHostState.showSnackbar("기록을 내보냈어요") }
                    .onFailure { snackbarHostState.showSnackbar("내보내지 못했어요. 다시 시도해 주세요.") }
            } finally {
                isExporting = false
            }
        }
    }

    // 가져오기: 파일에서 읽은 기록을 기존 목록에 그대로 더한다.
    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        coroutineScope.launch {
            isImporting = true
            try {
                BackupManager.importFromJson(context, uri)
                    .onSuccess { items ->
                        items.forEach { item -> itemRepository.insert(item) }
                        snackbarHostState.showSnackbar("${items.size}개 항목을 가져왔어요")
                    }
                    .onFailure {
                        snackbarHostState.showSnackbar("가져오지 못했어요. 이 앱에서 내보낸 파일인지 확인해 주세요.")
                    }
            } finally {
                isImporting = false
            }
        }
    }

    TopLevelScaffold(
        selected = TopLevelDestination.SETTINGS,
        navController = navController,
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(bottom = LocalTopLevelBottomContentInset.current)
        ) {
            Text(
                text = "설정",
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontSize = 22.sp,
                    lineHeight = 28.sp,
                    fontWeight = FontWeight.Bold
                ),
                modifier = Modifier
                    .padding(horizontal = SidePadding, vertical = 10.dp)
                    .semantics { heading() }
            )
            NotificationSection(
                settings = settings,
                hasPermission = hasNotificationPermission,
                onPermissionClick = {
                    val mustOpenSettings =
                        settings.notificationPermissionDenied && !shouldShowNotificationRationale
                    if (mustOpenSettings) {
                        PermissionUtils.openAppSettings(context)
                    } else {
                        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                },
                onDailyNotifyChange = { viewModel.toggleDailyNotify(it) },
                onDefaultDaysChange = { viewModel.updateDefaultDaysBefore(it) },
                onTimeClick = { showTimePicker = true }
            )
            DataSection(
                isExporting = isExporting,
                isImporting = isImporting,
                onExport = {
                    val timestamp = LocalDateTime.now()
                        .format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"))
                    // 확장자는 가져오기 선택기의 파일 형식 필터와 맞물려 있으므로 그대로 둔다.
                    exportLauncher.launch("fridge_dday_backup_$timestamp.json")
                },
                onImport = { importLauncher.launch(arrayOf("application/json")) }
            )
            PrivacySection()
            AppInfoSection()
            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    if (showTimePicker) {
        TimePickerDialog(
            currentTime = settings.notifyTime,
            onConfirm = { hour, minute ->
                viewModel.updateNotifyTime(hour, minute)
                showTimePicker = false
            },
            onDismiss = { showTimePicker = false }
        )
    }
}

@Composable
private fun NotificationSection(
    settings: AppSettings,
    hasPermission: Boolean,
    onPermissionClick: () -> Unit,
    onDailyNotifyChange: (Boolean) -> Unit,
    onDefaultDaysChange: (Int) -> Unit,
    onTimeClick: () -> Unit
) {
    SettingsSectionHeader(title = "알림", icon = Icons.Filled.Notifications)
    SettingsGroup {
        SettingsSwitchRow(
            title = "매일 알림 받기",
            description = "유통기한이 임박하거나 지난 식품을 알려드려요",
            checked = settings.dailyNotify,
            onCheckedChange = onDailyNotifyChange
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            SettingsHairline()
            SettingsValueRow(
                title = "알림 권한",
                value = if (hasPermission) "허용됨" else "허용 필요",
                hint = if (hasPermission) null else "눌러서 알림 권한을 설정하세요",
                onClick = if (hasPermission) null else onPermissionClick
            )
        }
        SettingsHairline()
        SettingsValueRow(
            title = "알림 시간",
            value = formatNotifyTime(settings.notifyTime),
            enabled = settings.dailyNotify,
            hint = if (settings.dailyNotify) null else "매일 알림을 켜면 설정할 수 있어요",
            onClick = onTimeClick
        )
        SettingsHairline()
        LeadTimeRow(
            days = settings.defaultDaysBefore,
            onDaysChange = onDefaultDaysChange
        )
    }
}


@Composable
private fun DataSection(
    isExporting: Boolean,
    isImporting: Boolean,
    onExport: () -> Unit,
    onImport: () -> Unit
) {
    SettingsSectionHeader(title = "데이터", icon = Icons.Filled.Storage)
    SettingsGroup {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = GroupContentPadding, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "백업과 복원",
                    style = MaterialTheme.typography.bodyLarge
                )
                Text(
                    text = "모든 식품 기록을 파일로 내보내거나 가져올 수 있어요.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            BackupActionRow(
                label = "기록 내보내기",
                icon = Icons.Filled.Upload,
                inProgress = isExporting,
                enabled = !isExporting && !isImporting,
                onClick = onExport
            )
            BackupActionRow(
                label = "기록 가져오기",
                icon = Icons.Filled.Download,
                inProgress = isImporting,
                enabled = !isExporting && !isImporting,
                onClick = onImport
            )
        }
    }
}

/**
 * 백업·복원 액션 한 줄.
 *
 * 같은 그룹 안에서 전체 폭 OutlinedButton 두 장 대신 아이콘 원 + 라벨 + affordance를 가진 액션
 * 행으로 낮춘다. 실행 동작, 진행 중 잠금, 성공·실패 문구는 그대로다.
 */
@Composable
private fun BackupActionRow(
    label: String,
    icon: ImageVector,
    inProgress: Boolean,
    enabled: Boolean,
    onClick: () -> Unit
) {
    val contentAlpha = if (enabled) 1f else DisabledContentAlpha
    val labelColor = MaterialTheme.colorScheme.onSurface.copy(alpha = contentAlpha)
    val affordanceColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = contentAlpha)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = RowMinHeight)
            .clickable(
                enabled = enabled,
                role = Role.Button,
                onClick = onClick
            )
            .semantics { if (!enabled) disabled() }
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 원은 진행 중에도 제자리에 남는다. 비활성일 때는 라벨과 같은 세기로 함께 흐려진다.
        IconDisc(
            icon = icon,
            size = 28.dp,
            modifier = Modifier.alpha(contentAlpha)
        )
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ButtonProgressLabel(
                inProgress = inProgress,
                label = label,
                contentColor = labelColor
            )
        }
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = affordanceColor
        )
    }
}

@Composable
private fun PrivacySection() {
    SettingsSectionHeader(title = "개인정보", icon = Icons.Filled.Shield)
    SettingsGroup {
        Text(
            text = PrivacyNotice,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = GroupContentPadding, vertical = 14.dp)
        )
    }
}

@Composable
private fun AppInfoSection() {
    SettingsSectionHeader(title = "앱 정보")
    SettingsGroup {
        SettingsValueRow(
            title = "앱 버전",
            value = "v${BuildConfig.VERSION_NAME}"
        )
    }
}

/**
 * 그룹 바깥에 놓이는 섹션 제목.
 *
 * Fresh Ledger: 섹션마다 작은 원 하나를 둔다. 행마다 아이콘을 붙이지 않고 그룹 단위로만 리듬을
 * 주기 위해서다. 아이콘 원은 장식이라 제목 텍스트가 의미를 그대로 전달하고, heading semantics도
 * 계속 제목 쪽에 남는다.
 */
@Composable
private fun SettingsSectionHeader(title: String, icon: ImageVector? = null) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = SidePadding, end = SidePadding, top = 24.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        icon?.let { IconDisc(icon = it, size = 28.dp) }
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.semantics { heading() }
        )
    }
}

/** 한 섹션의 행들을 묶는 둥근 표면. */
@Composable
private fun SettingsGroup(content: @Composable ColumnScope.() -> Unit) {
    GroupSurface(
        modifier = Modifier.padding(horizontal = SidePadding),
        content = content
    )
}

@Composable
private fun SettingsHairline() {
    GroupHairline(insetStart = GroupContentPadding)
}

/** 스위치 하나짜리 설정 행. 행 전체가 48dp 이상의 조작 대상이다. */
@Composable
private fun SettingsSwitchRow(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = RowMinHeight)
            .toggleable(
                value = checked,
                role = Role.Switch,
                onValueChange = onCheckedChange
            )
            .padding(horizontal = GroupContentPadding, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = null
        )
    }
}

/**
 * 현재 값을 오른쪽에 두는 설정 행.
 *
 * [onClick]이 없으면 읽기 전용 정보 행이 되고, [enabled]가 false면 눌러도 열리지 않는다는 것을
 * 흐리게 표시하면서 접근성 트리에도 비활성으로 남긴다.
 */
@Composable
private fun SettingsValueRow(
    title: String,
    value: String,
    enabled: Boolean = true,
    hint: String? = null,
    onClick: (() -> Unit)? = null
) {
    val contentAlpha = if (enabled) 1f else DisabledContentAlpha
    val interactionModifier = if (onClick != null) {
        Modifier
            .clickable(
                enabled = enabled,
                onClickLabel = "$title 변경",
                role = Role.Button,
                onClick = onClick
            )
            .semantics { if (!enabled) disabled() }
    } else {
        Modifier
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = RowMinHeight)
            .then(interactionModifier)
            .padding(horizontal = GroupContentPadding, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = contentAlpha)
            )
            hint?.let { text ->
                Text(
                    text = text,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = contentAlpha)
                )
            }
        }
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = contentAlpha)
        )
        if (onClick != null) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = contentAlpha)
            )
        }
    }
}


/**
 * 임박 알림 기준.
 *
 * 값 범위(1~14일)와 한 칸 단위 이동은 그대로 두고, 노출된 Material 슬라이더 대신 현재 값을 작은
 * 알약으로 받은 그룹 설정처럼 보이게 한다.
 */
@Composable
private fun LeadTimeRow(
    days: Int,
    onDaysChange: (Int) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                start = GroupContentPadding,
                end = GroupContentPadding,
                top = 12.dp,
                bottom = 16.dp
            ),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "임박 알림 기준",
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f)
            )
            // 현재 값은 슬라이더 손잡이가 아니라 이 알약에서 읽는다.
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer
            ) {
                Text(
                    text = "${days}일 전",
                    style = MaterialTheme.typography.labelLarge,
                    maxLines = 1,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                )
            }
        }
        Text(
            text = "유통기한이 이만큼 남은 식품을 알림에 포함해요.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Slider(
            value = days.toFloat(),
            onValueChange = { onDaysChange(it.toInt()) },
            valueRange = LeadTimeRange,
            steps = LeadTimeSteps,
            colors = SliderDefaults.colors(
                activeTickColor = Color.Transparent,
                inactiveTickColor = Color.Transparent,
                inactiveTrackColor = MaterialTheme.colorScheme.surfaceContainerHigh
            ),
            modifier = Modifier.fillMaxWidth()
        )
    }
}

/** 진행 중에도 버튼 문구는 남기고 앞에만 진행 표시를 둔다. */
@Composable
private fun ButtonProgressLabel(
    inProgress: Boolean,
    label: String,
    contentColor: Color
) {
    if (inProgress) {
        CircularProgressIndicator(
            modifier = Modifier.size(18.dp),
            color = contentColor,
            strokeWidth = 2.dp
        )
        Spacer(modifier = Modifier.width(8.dp))
    }
    Text(text = label, color = contentColor)
}

/** 알림 시간은 기기 로케일과 무관하게 24시간제 HH:mm으로만 표기한다. */
internal fun formatNotifyTime(time: LocalTime): String =
    String.format(Locale.ROOT, "%02d:%02d", time.hour, time.minute)


/**
 * 설정 ViewModel의 좁은 factory.
 *
 * top-level 화면의 ViewModel은 composition의 remember가 아니라 NavBackStackEntry가 소유한다.
 * 그래야 saveState/restoreState로 목적지를 오갈 때 같은 인스턴스가 유지되고, 화면이 실제로
 * 사라질 때 viewModelScope까지 함께 정리된다. DataStore는 Application 컨텍스트로만 연다.
 */
private val settingsViewModelFactory: ViewModelProvider.Factory = viewModelFactory {
    initializer {
        val application = requireNotNull(
            this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY]
        )
        SettingsViewModel(SettingsRepository(SettingsDataStore(application)))
    }
}
