package app.fridgedday.ui.scan

import android.Manifest
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import app.fridgedday.ui.components.CameraPreview
import app.fridgedday.ui.navigation.Destinations
import app.fridgedday.util.PermissionUtils
import app.fridgedday.util.ocr.TextRecognitionHelper
import java.time.LocalDate
import kotlinx.coroutines.launch

/**
 * 뷰파인더 최소 높이.
 *
 * 뷰파인더는 이 값 아래로 줄지 않는다. 글자 확대가 커져 안쪽 내용이 이 높이를 넘으면 내용만큼
 * 더 커지고, 그만큼 본문이 길어져 스크롤로 닿는다. 안쪽 촬영 버튼이 잘리지 않도록
 * 아이콘·문구·버튼이 들어가는 높이를 기준으로 잡는다.
 */
private val ViewfinderMinHeight = 240.dp

/**
 * 카메라 OCR task 화면.
 *
 * 카메라가 주 동작이고 갤러리·직접 입력은 라벨이 붙은 보조 동작이다. 촬영·선택한 비트맵은
 * 저장하지 않고 OCR 입력으로만 쓰며, 인식된 날짜만 [Destinations.OCR_CONFIRM] 경로로 넘긴다.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScanScreen(navController: NavHostController) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    var isProcessing by remember { mutableStateOf(false) }
    var showCamera by remember { mutableStateOf(false) }

    /**
     * OCR이 날짜를 못 찾거나 처리에 실패했을 때의 공통 대안.
     *
     * 스캔 화면을 떠나지 않고 기다리며 다시 시도할 수 있도록 실패를 알리고, `직접 입력`을 고르면
     * 등록 화면을 연다. 자동으로 등록 화면으로 보내지 않으며, 스낵바는 읽고 고를 시간을 주도록
     * `Long`으로 유지한다(D-016).
     */
    fun showManualEntryFallback(message: String) {
        scope.launch {
            val result = snackbarHostState.showSnackbar(
                message = message,
                actionLabel = "직접 입력",
                duration = SnackbarDuration.Long
            )
            if (result == SnackbarResult.ActionPerformed) {
                // 스캔 화면은 task 화면이므로 등록 진입 시 스택에서 걷어낸다. 등록이 끝나면
                // 스캔으로 되돌아가지 않고 Today로 복귀한다.
                navController.navigate(Destinations.ADD) {
                    popUpTo(Destinations.HOME)
                }
            }
        }
    }

    /** 처리 중에는 두 번째 OCR을 시작하지 않는다. */
    fun scan(bitmap: Bitmap) {
        if (isProcessing) return
        isProcessing = true
        scope.launch {
            try {
                val evaluation = TextRecognitionHelper.evaluateExpiryDate(bitmap, 0)
                // showSnackbar는 스낵바가 닫힐 때까지 반환하지 않으므로 진행 표시를 먼저 끈다.
                isProcessing = false

                when (val outcome = scanOutcome(evaluation)) {
                    is ScanOutcome.Recognized -> {
                        navController.navigate(Destinations.ocrConfirmRoute(outcome.date))
                    }
                    is ScanOutcome.Failed -> showManualEntryFallback(outcome.message)
                }
            } catch (_: Exception) {
                isProcessing = false
                showManualEntryFallback(ScanOutcome.Failed.IMAGE_PROCESSING_FAILED.message)
            } finally {
                isProcessing = false
            }
        }
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri == null || isProcessing) return@rememberLauncherForActivityResult
        scope.launch {
            val bitmap = try {
                context.contentResolver.openInputStream(uri)?.use { inputStream ->
                    BitmapFactory.decodeStream(inputStream)
                }
            } catch (_: Exception) {
                null
            }

            if (bitmap == null) {
                showManualEntryFallback(ScanOutcome.Failed.IMAGE_PROCESSING_FAILED.message)
            } else {
                scan(bitmap)
            }
        }
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            showCamera = true
        } else {
            scope.launch {
                val result = snackbarHostState.showSnackbar(
                    message = "촬영하려면 카메라 권한이 필요합니다.",
                    actionLabel = "설정"
                )
                if (result == SnackbarResult.ActionPerformed) {
                    PermissionUtils.openAppSettings(context)
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("스캔") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "뒤로가기")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        if (showCamera) {
            CameraPreview(
                onImageCaptured = { bitmap ->
                    showCamera = false
                    scan(bitmap)
                },
                onDismiss = { showCamera = false }
            )
        } else {
            // 시스템 글자 확대에서도 촬영·갤러리·직접 입력이 화면 밖으로 밀리지 않도록 스크롤을 둔다.
            // 스크롤 컨테이너는 자식을 세로 무한 제약으로 측정해서 Column weight가 0으로 풀리고
            // 뷰파인더가 통째로 접힌다. 뷰파인더 높이는 heightIn(min)만으로 잡는다.
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(paddingValues)
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Surface(
                    shape = MaterialTheme.shapes.large,
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(4f / 5f)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CameraAlt,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(40.dp)
                            )
                            Text(
                                text = "유통기한 라벨이 화면에 크게 보이도록 촬영하세요.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                            Button(
                                onClick = {
                                    cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                                },
                                enabled = !isProcessing,
                                modifier = Modifier.heightIn(min = 56.dp)
                            ) {
                                Icon(Icons.Default.CameraAlt, contentDescription = null)
                                Spacer(Modifier.width(8.dp))
                                Text("카메라로 촬영")
                            }
                        }
                    }
                }

                Text(
                    text = "촬영 이미지는 저장하지 않아요",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (isProcessing) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp
                        )
                        Text(
                            text = "날짜를 확인하는 중...",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                HorizontalDivider()

                OutlinedButton(
                    onClick = {
                        galleryLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                    enabled = !isProcessing,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                ) {
                    Icon(Icons.Default.Image, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("갤러리에서 선택")
                }
                TextButton(
                    onClick = {
                        // 등록이 끝나면 스캔 화면으로 되돌아가지 않고 Today로 복귀한다.
                        navController.navigate(Destinations.ADD) {
                            popUpTo(Destinations.HOME)
                        }
                    },
                    enabled = !isProcessing,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                ) {
                    Text("직접 입력")
                }
            }
        }
    }
}

/**
 * 한 번의 OCR 시도의 결과.
 *
 * 성공은 확인 화면으로 넘길 날짜 하나뿐이고, 실패는 스낵바 문구와 `직접 입력` 대안 하나로 모인다.
 * 실패 문구는 원인을 구분하되 대안은 항상 같다(D-015).
 */
internal sealed interface ScanOutcome {

    data class Recognized(val date: LocalDate) : ScanOutcome

    enum class Failed(val message: String) : ScanOutcome {
        /** 시도한 이미지가 하나도 처리되지 않고 실패만 남은 경우. */
        RECOGNITION_UNAVAILABLE(
            "문자를 인식하지 못했습니다. 다시 시도하거나 직접 입력해주세요."
        ),

        /** 인식은 됐지만 저장할 만한 날짜 후보를 찾지 못한 경우. */
        NO_DATE_FOUND("날짜를 찾지 못했습니다. 날짜를 직접 선택해주세요."),

        /** 이미지 자체를 읽지 못했거나 처리 중 예외가 난 경우. */
        IMAGE_PROCESSING_FAILED("이미지를 처리하지 못했습니다. 날짜를 직접 선택해주세요.")
    }
}

/** OCR 진단 결과를 화면이 쓸 결정으로 낮춘다. 날짜가 있으면 항상 인식 성공이다. */
internal fun scanOutcome(evaluation: TextRecognitionHelper.OcrEvaluation): ScanOutcome {
    val recognizedDate = evaluation.selectedDate
    if (recognizedDate != null) return ScanOutcome.Recognized(recognizedDate)
    return if (evaluation.processedVariantCount == 0 && evaluation.failedVariantCount > 0) {
        ScanOutcome.Failed.RECOGNITION_UNAVAILABLE
    } else {
        ScanOutcome.Failed.NO_DATE_FOUND
    }
}
