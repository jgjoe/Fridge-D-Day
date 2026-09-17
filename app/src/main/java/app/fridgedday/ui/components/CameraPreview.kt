package app.fridgedday.ui.components

import android.graphics.Bitmap
import android.view.ViewGroup
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Camera
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

@Composable
fun CameraPreview(
    onImageCaptured: (Bitmap) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val cameraProviderFuture = remember(context) { ProcessCameraProvider.getInstance(context) }
    val mainExecutor = remember(context) { ContextCompat.getMainExecutor(context) }

    var imageCapture by remember { mutableStateOf<ImageCapture?>(null) }
    var isCapturing by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // 컴포지션을 떠나면 CameraX 바인딩을 해제한다. provider가 아직 준비되지 않은 채
    // 해제되면 늦게 도착한 콜백이 카메라를 다시 잡지 않도록 isDisposed로 막는다.
    val isDisposed = remember { mutableStateOf(false) }
    DisposableEffect(cameraProviderFuture) {
        onDispose {
            isDisposed.value = true
            cameraProviderFuture.addListener(
                { runCatching { cameraProviderFuture.get().unbindAll() } },
                mainExecutor
            )
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // Camera Preview
        AndroidView(
            factory = { ctx ->
                val previewView = PreviewView(ctx).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    scaleType = PreviewView.ScaleType.FILL_CENTER
                }

                cameraProviderFuture.addListener({
                    // 해제된 뒤에는 provider가 늦게 준비돼도 카메라를 잡지 않는다.
                    if (isDisposed.value) return@addListener

                    val cameraProvider = try {
                        cameraProviderFuture.get()
                    } catch (e: Exception) {
                        errorMessage = "카메라 초기화 실패: ${e.message}"
                        return@addListener
                    }

                    val preview = Preview.Builder().build().also {
                        it.setSurfaceProvider(previewView.surfaceProvider)
                    }

                    val capture = ImageCapture.Builder()
                        .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                        .build()

                    try {
                        cameraProvider.unbindAll()
                        cameraProvider.bindToLifecycle(
                            lifecycleOwner,
                            CameraSelector.DEFAULT_BACK_CAMERA,
                            preview,
                            capture
                        )
                        // 바인딩이 끝난 뒤에만 촬영을 준비 상태로 노출한다.
                        imageCapture = capture
                    } catch (e: Exception) {
                        errorMessage = "카메라 초기화 실패: ${e.message}"
                    }
                }, mainExecutor)

                previewView
            },
            modifier = Modifier.fillMaxSize()
        )

        // Top Bar with Close Button
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.Black.copy(alpha = 0.5f))
                .windowInsetsPadding(WindowInsets.statusBars)
                .padding(16.dp)
        ) {
            IconButton(
                onClick = onDismiss,
                modifier = Modifier.align(Alignment.CenterStart)
            ) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = "닫기",
                    tint = Color.White
                )
            }
            Text(
                text = "유통기한 촬영",
                style = MaterialTheme.typography.titleMedium,
                color = Color.White,
                modifier = Modifier.align(Alignment.Center)
            )
        }

        // Bottom Bar with Capture Button
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .background(Color.Black.copy(alpha = 0.5f))
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (errorMessage != null) {
                Text(
                    text = errorMessage!!,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }

            Text(
                text = "유통기한이 보이도록 촬영하세요",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            FloatingActionButton(
                onClick = {
                    // ImageCapture가 준비되기 전 입력은 무시하고 촬영 상태로 들어가지 않는다.
                    val capture = imageCapture
                    if (!isCapturing && capture != null) {
                        isCapturing = true
                        capture.takePicture(
                            ContextCompat.getMainExecutor(context),
                            object : ImageCapture.OnImageCapturedCallback() {
                                override fun onCaptureSuccess(image: ImageProxy) {
                                    val bitmap = image.toBitmap()
                                    image.close()
                                    onImageCaptured(bitmap)
                                    isCapturing = false
                                }

                                override fun onError(exception: ImageCaptureException) {
                                    errorMessage = "촬영 실패: ${exception.message}"
                                    isCapturing = false
                                }
                            }
                        )
                    }
                },
                containerColor = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(72.dp)
            ) {
                if (isCapturing) {
                    CircularProgressIndicator(
                        color = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(32.dp)
                    )
                } else {
                    Icon(
                        Icons.Default.Camera,
                        contentDescription = "촬영",
                        modifier = Modifier.size(32.dp)
                    )
                }
            }
        }
    }
}
