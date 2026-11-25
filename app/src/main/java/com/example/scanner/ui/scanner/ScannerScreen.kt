package com.example.scanner.ui.scanner

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.util.Size
import android.view.MotionEvent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.AspectRatio
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.FocusMeteringAction
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.core.resolutionselector.AspectRatioStrategy
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.scanner.R
import com.example.scanner.scanner.BarcodeAnalyzer
import com.example.scanner.scanner.DetectedBarcode
import com.example.scanner.scanner.GalleryDecoder
import com.example.scanner.scanner.ParsedScanResult
import com.example.scanner.scanner.ScanContentType
import com.example.scanner.scanner.ScannerPreferences
import com.example.scanner.scanner.SoundFeedback
import com.example.scanner.theme.PaytmCyan
import com.example.scanner.theme.XiaomiBlue
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

@Composable
fun ScannerScreen(
    preferences: ScannerPreferences,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()
    val settings by preferences.settings.collectAsState()

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED,
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { granted ->
        hasCameraPermission = granted
    }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    var torchEnabled by remember { mutableStateOf(false) }
    var camera by remember { mutableStateOf<Camera?>(null) }
    var zoomRatio by remember { mutableFloatStateOf(1f) }
    var maxZoomRatio by remember { mutableFloatStateOf(4f) }
    var showZoomPill by remember { mutableStateOf(false) }
    var tapFocusPoint by remember { mutableStateOf<Offset?>(null) }

    var previewWidth by remember { mutableIntStateOf(1) }
    var previewHeight by remember { mutableIntStateOf(1) }

    var captureState by remember { mutableStateOf<CaptureAnimationState?>(null) }
    var activeResult by remember { mutableStateOf<ParsedScanResult?>(null) }
    var continuousBannerResult by remember { mutableStateOf<ParsedScanResult?>(null) }
    var triggerCounter by remember { mutableLongStateOf(0L) }

    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }
    var previewViewRef by remember { mutableStateOf<PreviewView?>(null) }

    val analyzer = remember {
        BarcodeAnalyzer(
            previewAspectRatioProvider = {
                previewWidth.toFloat() / previewHeight.coerceAtLeast(1).toFloat()
            },
            autoZoomProvider = { settings.autoZoomEnabled },
            invertedScanProvider = { settings.invertedQrEnabled },
            onAutoZoomRequested = { requestedRatio ->
                val cam = camera ?: return@BarcodeAnalyzer
                val current = cam.cameraInfo.zoomState.value?.zoomRatio ?: 1f
                val maxZ = cam.cameraInfo.zoomState.value?.maxZoomRatio ?: 4f
                val target = requestedRatio.coerceIn(current, minOf(maxZ, 3.5f))
                if (target > current + 0.1f) {
                    cam.cameraControl.setZoomRatio(target)
                    zoomRatio = target
                }
            },
            onBarcodeDetected = { detected: DetectedBarcode ->
                scope.launch {
                    if (settings.hapticEnabled) {
                        SoundFeedback.triggerHaptic(context)
                    }
                    if (settings.soundEnabled) {
                        launch { SoundFeedback.playScanChime() }
                    }
                    preferences.addHistoryItem(detected.parsed)

                    val id = ++triggerCounter
                    val label = if (detected.parsed.type == ScanContentType.UPI) {
                        "UPI QR Detected"
                    } else {
                        "${detected.parsed.formatLabel} Captured"
                    }
                    captureState = CaptureAnimationState(
                        normalizedBounds = detected.normalizedBounds,
                        contentType = detected.parsed.type,
                        label = label,
                        triggerId = id,
                    )

                    if (settings.continuousScanEnabled) {
                        continuousBannerResult = detected.parsed
                        delay(700L)
                        captureState = null
                        // Resumes scanning in continuous mode
                    } else {
                        // Let Paytm lock-on + checkmark animation play crisply (~460ms)
                        delay(460L)
                        activeResult = detected.parsed
                    }
                }
            },
        )
    }

    // Keep analyzer paused/resumed in sync with modal sheet and continuous mode
    LaunchedEffect(activeResult, captureState, settings.continuousScanEnabled) {
        val shouldPause = if (settings.continuousScanEnabled) {
            captureState != null
        } else {
            activeResult != null || captureState != null
        }
        analyzer.setPaused(shouldPause)
    }

    // Hide zoom pill shortly after zooming stops
    LaunchedEffect(zoomRatio) {
        if (zoomRatio > 1.05f) {
            showZoomPill = true
            delay(1600L)
            showZoomPill = false
        } else {
            showZoomPill = false
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            analyzer.close()
            cameraExecutor.shutdown()
        }
    }

    // Gallery image picker
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
    ) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            val decoded = GalleryDecoder.decodeUri(context, uri)
            if (decoded != null) {
                if (settings.hapticEnabled) SoundFeedback.triggerHaptic(context)
                if (settings.soundEnabled) launch { SoundFeedback.playScanChime() }
                preferences.addHistoryItem(decoded)
                val id = ++triggerCounter
                captureState = CaptureAnimationState(
                    normalizedBounds = null,
                    contentType = decoded.type,
                    label = if (decoded.type == ScanContentType.UPI) "UPI QR Detected" else "${decoded.formatLabel} Captured",
                    triggerId = id,
                )
                delay(460L)
                activeResult = decoded
            } else {
                Toast.makeText(context, "No QR or barcode found in image", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun toggleTorch() {
        val cam = camera
        if (cam == null || !cam.cameraInfo.hasFlashUnit()) {
            Toast.makeText(context, "Flashlight unavailable on this device", Toast.LENGTH_SHORT).show()
            return
        }
        val next = !torchEnabled
        torchEnabled = next
        cam.cameraControl.enableTorch(next)
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black),
    ) {
        if (hasCameraPermission) {
            // CameraX Preview
            AndroidView(
                factory = { ctx ->
                    PreviewView(ctx).apply {
                        scaleType = PreviewView.ScaleType.FILL_CENTER
                        implementationMode = PreviewView.ImplementationMode.PERFORMANCE
                        previewViewRef = this
                    }
                },
                modifier = Modifier
                    .fillMaxSize()
                    .onSizeChanged { sz ->
                        previewWidth = sz.width
                        previewHeight = sz.height
                    }
                    .pointerInput(Unit) {
                        detectTransformGestures { _, _, zoom, _ ->
                            val cam = camera ?: return@detectTransformGestures
                            if (zoom != 1f) {
                                val nextZoom = (zoomRatio * zoom).coerceIn(1f, maxZoomRatio)
                                zoomRatio = nextZoom
                                cam.cameraControl.setZoomRatio(nextZoom)
                            }
                        }
                    }
                    .pointerInput(Unit) {
                        detectTapGestures { offset ->
                            tapFocusPoint = offset
                            val pv = previewViewRef ?: return@detectTapGestures
                            val cam = camera ?: return@detectTapGestures
                            val point = pv.meteringPointFactory.createPoint(offset.x, offset.y)
                            val action = FocusMeteringAction.Builder(point, FocusMeteringAction.FLAG_AF or FocusMeteringAction.FLAG_AE)
                                .setAutoCancelDuration(3, TimeUnit.SECONDS)
                                .build()
                            cam.cameraControl.startFocusAndMetering(action)
                        }
                    },
            )

            // Bind CameraX lifecycle once PreviewView is ready
            LaunchedEffect(previewViewRef, lifecycleOwner) {
                val pv = previewViewRef ?: return@LaunchedEffect
                val providerFuture = ProcessCameraProvider.getInstance(context)
                providerFuture.addListener(
                    {
                        val provider = providerFuture.get()
                        val resolutionSelector = ResolutionSelector.Builder()
                            .setAspectRatioStrategy(
                                AspectRatioStrategy(
                                    AspectRatio.RATIO_16_9,
                                    AspectRatioStrategy.FALLBACK_RULE_AUTO,
                                ),
                            )
                            .setResolutionStrategy(
                                ResolutionStrategy(
                                    Size(1280, 720),
                                    ResolutionStrategy.FALLBACK_RULE_CLOSEST_HIGHER_THEN_LOWER,
                                ),
                            )
                            .build()

                        val preview = Preview.Builder()
                            .setResolutionSelector(resolutionSelector)
                            .build()
                            .also { it.surfaceProvider = pv.surfaceProvider }

                        val imageAnalysis = ImageAnalysis.Builder()
                            .setResolutionSelector(resolutionSelector)
                            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                            .build()
                            .also { it.setAnalyzer(cameraExecutor, analyzer) }

                        runCatching {
                            provider.unbindAll()
                            val boundCam = provider.bindToLifecycle(
                                lifecycleOwner,
                                CameraSelector.DEFAULT_BACK_CAMERA,
                                preview,
                                imageAnalysis,
                            )
                            camera = boundCam
                            maxZoomRatio = boundCam.cameraInfo.zoomState.value?.maxZoomRatio ?: 4f
                            boundCam.cameraControl.enableTorch(torchEnabled)
                        }.onFailure {
                            Toast.makeText(context, "Unable to open camera", Toast.LENGTH_SHORT).show()
                        }
                    },
                    ContextCompat.getMainExecutor(context),
                )
            }

            // Xiaomi Viewfinder + Paytm Scan Animation Overlay
            PaytmScanOverlay(
                captureState = captureState,
                tapFocusPoint = tapFocusPoint,
            )
        } else {
            // Clean dark camera permission prompt
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
            ) {
                Image(
                    painter = painterResource(R.drawable.ic_xiaomi_logo_about),
                    contentDescription = "Scanner",
                    modifier = Modifier.size(72.dp),
                )
                Spacer(Modifier.height(20.dp))
                Text(
                    text = "Camera permission required",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "Grant camera access to scan QR codes, UPI codes, and barcodes.",
                    color = Color(0xFFA0A0A8),
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(20.dp))
                Button(
                    onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) },
                    colors = ButtonDefaults.buttonColors(containerColor = XiaomiBlue),
                    shape = RoundedCornerShape(14.dp),
                ) {
                    Text("Allow Camera", color = Color.White)
                }
            }
        }

        // Top Bar: Flash (top-left) and More/Settings (top-right) — exact match to Xiaomi Scanner
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 10.dp, vertical = 6.dp),
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = { toggleTorch() },
                    ),
            ) {
                Image(
                    painter = painterResource(
                        if (torchEnabled) R.drawable.ic_xiaomi_flash_on else R.drawable.ic_xiaomi_flash_off,
                    ),
                    contentDescription = "Flash",
                    modifier = Modifier.size(21.dp),
                )
            }

            // Zoom indicator pill only visible during active zoom
            AnimatedVisibility(
                visible = showZoomPill,
                enter = fadeIn(),
                exit = fadeOut(),
            ) {
                Box(
                    modifier = Modifier
                        .background(Color.Black.copy(alpha = 0.55f), RoundedCornerShape(14.dp))
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                ) {
                    Text(
                        text = String.format("%.1fx", zoomRatio),
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                    )
                }
            }

            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onOpenSettings,
                    ),
            ) {
                Image(
                    painter = painterResource(R.drawable.ic_xiaomi_top_more),
                    contentDescription = "Settings",
                    modifier = Modifier.size(22.dp),
                )
            }
        }

        // Continuous scan quick banner (only shown if user enables Continuous Scan in Settings)
        AnimatedVisibility(
            visible = continuousBannerResult != null && settings.continuousScanEnabled,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 180.dp, start = 20.dp, end = 20.dp),
        ) {
            continuousBannerResult?.let { bannerItem ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color(0xE61C1C1E))
                        .clickable { activeResult = bannerItem }
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = bannerItem.title,
                            color = if (bannerItem.type == ScanContentType.UPI) PaytmCyan else Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                        )
                        Text(
                            text = bannerItem.rawValue,
                            color = Color(0xFFB0B0B6),
                            fontSize = 12.sp,
                            maxLines = 1,
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                    Text("Open", color = XiaomiBlue, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }

        // Bottom Controls Stack — exact match to Xiaomi Scanner:
        // 1. Flashlight toggle
        // 2. QR code active icon + blue label
        // 3. Circular Gallery Picker button
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 18.dp),
        ) {
            // Torch button
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(44.dp)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = { toggleTorch() },
                    ),
            ) {
                Image(
                    painter = painterResource(
                        if (torchEnabled) R.drawable.ic_xiaomi_torch_on else R.drawable.ic_xiaomi_torch_off,
                    ),
                    contentDescription = "Flashlight",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.size(width = 18.dp, height = 26.dp),
                )
            }

            Spacer(Modifier.height(10.dp))

            // Active "QR code" mode indicator (matching Xiaomi Scanner blue active icon + text)
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Image(
                    painter = painterResource(R.drawable.ic_xiaomi_scancode_active),
                    contentDescription = "QR code",
                    modifier = Modifier.size(22.dp),
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "QR code",
                    color = XiaomiBlue,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                )
            }

            Spacer(Modifier.height(16.dp))

            // Circular Gallery Picker button
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(50.dp)
                    .clip(CircleShape)
                    .clickable {
                        galleryLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                        )
                    },
            ) {
                Image(
                    painter = painterResource(R.drawable.ic_xiaomi_gallery),
                    contentDescription = "Choose picture",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.size(44.dp),
                )
            }
        }
    }

    // Bottom sheet with parsed UPI / QR / barcode string & actions
    activeResult?.let { result ->
        ScanResultSheet(
            result = result,
            onDismiss = {
                activeResult = null
                captureState = null
            },
        )
    }
}
