package com.icoffee.app.ui.screens.scanner

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.icoffee.app.R
import com.icoffee.app.data.menu.MenuScanRepository
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.security.MessageDigest
import java.util.concurrent.Executors

@Composable
fun MenuScannerScreen(
    onBack: () -> Unit,
    onMenuScanned: (scanId: String) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        )
    }
    var isProcessing by rememberSaveable { mutableStateOf(false) }
    var imageCapture by remember { mutableStateOf<ImageCapture?>(null) }

    val recognizer = remember {
        TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCameraPermission = granted
    }

    BackHandler(onBack = onBack)

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    DisposableEffect(recognizer) {
        onDispose {
            recognizer.close()
        }
    }

    if (!hasCameraPermission) {
        MenuPermissionFallback(
            onBack = onBack,
            onGrant = { permissionLauncher.launch(Manifest.permission.CAMERA) }
        )
        return
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF140C08))
    ) {
        MenuCameraPreview(onImageCaptureReady = { capture -> imageCapture = capture })

        ScannerScrimCutout(
            frameWidth = 300.dp,
            frameHeight = 210.dp,
            cornerRadius = 24.dp
        )

        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .size(width = 300.dp, height = 210.dp)
                .clip(RoundedCornerShape(24.dp))
                .border(
                    width = 1.5.dp,
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Color(0xE8E1B987),
                            Color(0xCCB67A4D),
                            Color(0xDCE1B987)
                        )
                    ),
                    shape = RoundedCornerShape(24.dp)
                )
        )

        MenuScanTopBar(
            onBack = onBack,
            modifier = Modifier
                .align(Alignment.TopStart)
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 10.dp)
        )

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(horizontal = 20.dp, vertical = 48.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = stringResource(R.string.menu_scan_instruction),
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                color = Color(0xFFEAD8C5)
            )

            IconButton(
                enabled = !isProcessing,
                onClick = {
                    val capture = imageCapture ?: return@IconButton
                    isProcessing = true
                    capture.takePicture(
                        Executors.newSingleThreadExecutor(),
                        object : ImageCapture.OnImageCapturedCallback() {
                            override fun onCaptureSuccess(imageProxy: androidx.camera.core.ImageProxy) {
                                val imageHash = imageHashFromProxy(imageProxy)
                                val cached = MenuScanRepository.getCachedByImageHash(imageHash)
                                if (cached != null) {
                                    imageProxy.close()
                                    scope.launch {
                                        isProcessing = false
                                        onMenuScanned(cached.scanId)
                                    }
                                    return
                                }

                                val mediaImage = imageProxy.image
                                if (mediaImage == null) {
                                    imageProxy.close()
                                    scope.launch { isProcessing = false }
                                    return
                                }

                                val inputImage = InputImage.fromMediaImage(
                                    mediaImage,
                                    imageProxy.imageInfo.rotationDegrees
                                )
                                recognizer.process(inputImage)
                                    .addOnSuccessListener { visionText ->
                                        val rawText = visionText.text.orEmpty()
                                        scope.launch {
                                            val result = withContext(Dispatchers.Default) {
                                                MenuScanRepository.processRawMenuText(
                                                    rawText = rawText,
                                                    imageHash = imageHash
                                                )
                                            }
                                            onMenuScanned(result.scanId)
                                        }
                                    }
                                    .addOnCompleteListener {
                                        imageProxy.close()
                                        isProcessing = false
                                    }
                            }

                            override fun onError(exception: ImageCaptureException) {
                                scope.launch { isProcessing = false }
                            }
                        }
                    )
                },
                modifier = Modifier
                    .size(74.dp)
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(Color(0xFFC58A58), Color(0xFFAA6F42))
                        ),
                        shape = CircleShape
                    )
                    .border(1.dp, Color(0x55FFF0DC), CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Default.PhotoCamera,
                    contentDescription = stringResource(R.string.menu_scan_capture),
                    tint = Color(0xFFFFF6EA),
                    modifier = Modifier.size(30.dp)
                )
            }

            if (isProcessing) {
                Text(
                    text = stringResource(R.string.menu_scan_processing),
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFFD9BEA0)
                )
            }
        }
    }
}

@Composable
private fun MenuPermissionFallback(
    onBack: () -> Unit,
    onGrant: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF140C08))
    ) {
        MenuScanTopBar(
            onBack = onBack,
            modifier = Modifier
                .align(Alignment.TopStart)
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 10.dp)
        )

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier
                .align(Alignment.Center)
                .padding(horizontal = 28.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(Color(0xB3281A12))
                .border(1.dp, Color(0x33F5E6D3), RoundedCornerShape(24.dp))
                .padding(horizontal = 20.dp, vertical = 22.dp)
        ) {
            Text(
                text = stringResource(R.string.menu_scan_camera_permission_required),
                style = MaterialTheme.typography.bodyLarge,
                color = Color(0xFFF5E6D3)
            )
            Button(
                onClick = onGrant,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFB67A4D),
                    contentColor = Color(0xFFFDF8F2)
                )
            ) {
                Text(text = stringResource(R.string.menu_scan_camera_permission_action))
            }
        }
    }
}

@Composable
private fun MenuScanTopBar(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            onClick = onBack,
            modifier = Modifier
                .size(42.dp)
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color(0xB83A2518),
                            Color(0x9D21150F)
                        )
                    ),
                    shape = CircleShape
                )
                .border(1.dp, Color(0x2DF5E6D3), CircleShape)
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = stringResource(R.string.scan_back),
                tint = Color(0xFFF4E4D1)
            )
        }
        Text(
            text = stringResource(R.string.menu_scan_title),
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
            color = Color(0xFFF4E4D1)
        )
    }
}

@Composable
private fun MenuCameraPreview(
    onImageCaptureReady: (ImageCapture) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val previewView = remember {
        PreviewView(context).apply {
            scaleType = PreviewView.ScaleType.FILL_CENTER
        }
    }
    val latestImageCapture by rememberUpdatedState(onImageCaptureReady)

    DisposableEffect(lifecycleOwner, previewView) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        val cameraProvider = cameraProviderFuture.get()
        val preview = Preview.Builder().build().also { it.setSurfaceProvider(previewView.surfaceProvider) }
        val imageCapture = ImageCapture.Builder()
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
            .build()

        try {
            cameraProvider.unbindAll()
            cameraProvider.bindToLifecycle(
                lifecycleOwner,
                CameraSelector.DEFAULT_BACK_CAMERA,
                preview,
                imageCapture
            )
            latestImageCapture(imageCapture)
        } catch (_: Exception) {
            // Camera binding failures are handled by fallback UI state.
        }

        onDispose {
            try {
                cameraProvider.unbindAll()
            } catch (_: Exception) {
                // No-op
            }
        }
    }

    AndroidView(
        factory = { previewView },
        modifier = Modifier.fillMaxSize()
    )
}

@Composable
private fun ScannerScrimCutout(
    frameWidth: Dp,
    frameHeight: Dp,
    cornerRadius: Dp
) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val frameWidthPx = frameWidth.toPx()
        val frameHeightPx = frameHeight.toPx()
        val left = (size.width - frameWidthPx) / 2f
        val top = (size.height - frameHeightPx) / 2f
        val right = left + frameWidthPx
        val bottom = top + frameHeightPx
        val radius = cornerRadius.toPx()

        val scrimPath = Path().apply {
            fillType = PathFillType.EvenOdd
            addRect(Rect(offset = Offset.Zero, size = size))
            addRoundRect(
                RoundRect(
                    left = left,
                    top = top,
                    right = right,
                    bottom = bottom,
                    topLeftCornerRadius = CornerRadius(radius, radius),
                    topRightCornerRadius = CornerRadius(radius, radius),
                    bottomLeftCornerRadius = CornerRadius(radius, radius),
                    bottomRightCornerRadius = CornerRadius(radius, radius)
                )
            )
        }
        drawPath(scrimPath, color = Color(0xA5130B08))
    }
}

private fun imageHashFromProxy(imageProxy: androidx.camera.core.ImageProxy): String {
    val plane = imageProxy.planes.firstOrNull() ?: return "image-${System.currentTimeMillis()}"
    val buffer = plane.buffer
    val bytes = ByteArray(buffer.remaining())
    buffer.get(bytes)
    val digest = MessageDigest.getInstance("SHA-256").digest(bytes)
    return digest.joinToString(separator = "") { b -> "%02x".format(b) }
}
