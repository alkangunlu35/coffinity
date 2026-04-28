package com.icoffee.app.ui.screens.scanner

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.BackHandler
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Button
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
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.foundation.Canvas
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.icoffee.app.R
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

@Composable
fun BarcodeScannerScreen(
    onBack: () -> Unit,
    onBarcodeScanned: (String) -> Unit
) {
    val context = LocalContext.current
    var hasScanned by rememberSaveable { mutableStateOf(false) }
    BackHandler(onBack = onBack)

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCameraPermission = granted
    }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    if (!hasCameraPermission) {
        PermissionFallback(
            onBack = onBack,
            onGrant = { permissionLauncher.launch(Manifest.permission.CAMERA) }
        )
        return
    }

    Box(modifier = Modifier.fillMaxSize().background(Color(0xFF140C08))) {
        val frameWidth = 292.dp
        val frameHeight = 182.dp
        val frameShape = RoundedCornerShape(22.dp)

        BarcodeCameraPreview(
            scanningEnabled = !hasScanned,
            onBarcodeDetected = { value ->
                if (!hasScanned) {
                    hasScanned = true
                    onBarcodeScanned(value)
                }
            }
        )

        ScannerScrimCutout(
            frameWidth = frameWidth,
            frameHeight = frameHeight,
            cornerRadius = 22.dp
        )

        if (hasCameraPermission) {
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(width = frameWidth, height = frameHeight)
                    .shadow(
                        elevation = 12.dp,
                        shape = frameShape,
                        ambientColor = Color(0x3DB67A4D),
                        spotColor = Color(0x2EB67A4D)
                    )
                    .clip(frameShape)
                    .border(
                        width = 1.5.dp,
                        brush = Brush.linearGradient(
                            colors = listOf(
                                Color(0xE8E1B987),
                                Color(0xCCB67A4D),
                                Color(0xDCE1B987)
                            )
                        ),
                        shape = frameShape
                    )
                    .padding(horizontal = 10.dp, vertical = 12.dp)
            ) {
                ScanLine(
                    modifier = Modifier.fillMaxSize(),
                    scanningEnabled = !hasScanned
                )
            }
        }

        ScanTopBar(
            onBack = onBack,
            modifier = Modifier
                .align(Alignment.TopStart)
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 10.dp)
        )

        Text(
            text = stringResource(R.string.scan_camera_instruction),
            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
            color = Color(0xFFEAD8C5),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(horizontal = 24.dp, vertical = 72.dp)
        )
    }
}

@Composable
private fun PermissionFallback(
    onBack: () -> Unit,
    onGrant: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF140C08))
    ) {
        ScanTopBar(
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
                text = stringResource(R.string.scan_camera_permission_required),
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
                Text(text = stringResource(R.string.scan_camera_permission_action))
            }
        }
    }
}

@Composable
private fun ScanTopBar(
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
            text = stringResource(R.string.scan_camera_title),
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
            color = Color(0xFFF4E4D1)
        )
    }
}

@Composable
private fun BarcodeCameraPreview(
    scanningEnabled: Boolean,
    onBarcodeDetected: (String) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val previewView = remember {
        PreviewView(context).apply {
            scaleType = PreviewView.ScaleType.FILL_CENTER
        }
    }

    val currentScanningEnabled by rememberUpdatedState(scanningEnabled)
    val currentOnBarcodeDetected by rememberUpdatedState(onBarcodeDetected)

    DisposableEffect(lifecycleOwner, previewView) {
        val cameraExecutor = Executors.newSingleThreadExecutor()
        val hasDeliveredResult = AtomicBoolean(false)
        val scannerOptions = BarcodeScannerOptions.Builder()
            .setBarcodeFormats(
                Barcode.FORMAT_EAN_13,
                Barcode.FORMAT_EAN_8,
                Barcode.FORMAT_UPC_A,
                Barcode.FORMAT_UPC_E
            )
            .build()
        val scanner = BarcodeScanning.getClient(scannerOptions)

        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        val cameraProvider = cameraProviderFuture.get()

        val preview = Preview.Builder().build().also { p ->
            p.setSurfaceProvider(previewView.surfaceProvider)
        }

        val analysis = ImageAnalysis.Builder()
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build()
            .also { imageAnalysis ->
                imageAnalysis.setAnalyzer(cameraExecutor) { imageProxy ->
                    if (!currentScanningEnabled || hasDeliveredResult.get()) {
                        imageProxy.close()
                        return@setAnalyzer
                    }

                    @androidx.annotation.OptIn(androidx.camera.core.ExperimentalGetImage::class)
                    val mediaImage = imageProxy.image
                    if (mediaImage == null) {
                        imageProxy.close()
                        return@setAnalyzer
                    }

                    val inputImage = InputImage.fromMediaImage(
                        mediaImage,
                        imageProxy.imageInfo.rotationDegrees
                    )

                    scanner.process(inputImage)
                        .addOnSuccessListener { barcodes ->
                            if (!currentScanningEnabled) return@addOnSuccessListener

                            val value = barcodes
                                .firstOrNull { it.rawValue != null }
                                ?.rawValue
                                ?.trim()
                                .orEmpty()

                            if (value.isNotEmpty() && hasDeliveredResult.compareAndSet(false, true)) {
                                imageAnalysis.clearAnalyzer()
                                currentOnBarcodeDetected(value)
                            }
                        }
                        .addOnCompleteListener {
                            imageProxy.close()
                        }
                }
            }

        try {
            cameraProvider.unbindAll()
            cameraProvider.bindToLifecycle(
                lifecycleOwner,
                CameraSelector.DEFAULT_BACK_CAMERA,
                preview,
                analysis
            )
        } catch (_: Exception) {
            // No-op: screen caller handles scan callbacks only when camera is available.
        }

        onDispose {
            try {
                cameraProvider.unbindAll()
            } catch (_: Exception) {
                // No-op
            }
            scanner.close()
            cameraExecutor.shutdown()
        }
    }

    AndroidView(
        factory = { previewView },
        modifier = Modifier.fillMaxSize()
    )
}

@Composable
private fun ScannerScrimCutout(
    frameWidth: androidx.compose.ui.unit.Dp,
    frameHeight: androidx.compose.ui.unit.Dp,
    cornerRadius: androidx.compose.ui.unit.Dp
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

@Composable
private fun ScanLine(
    modifier: Modifier = Modifier,
    scanningEnabled: Boolean
) {
    val infiniteTransition = rememberInfiniteTransition(label = "scanLineTransition")
    val progress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scanLineProgress"
    )

    val travel = 132.dp
    val lineOffset = if (scanningEnabled) travel * progress else travel / 2f

    Canvas(modifier = modifier) {
        val y = lineOffset.toPx().coerceIn(8.dp.toPx(), size.height - 8.dp.toPx())
        drawLine(
            brush = Brush.horizontalGradient(
                colors = listOf(
                    Color(0x00E5AA74),
                    Color(0xF5D8A97E),
                    Color(0x00E5AA74)
                )
            ),
            start = Offset(10.dp.toPx(), y),
            end = Offset(size.width - 10.dp.toPx(), y),
            strokeWidth = 3.dp.toPx(),
            cap = StrokeCap.Round
        )
    }
}
