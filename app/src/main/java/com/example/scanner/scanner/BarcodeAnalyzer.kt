package com.example.scanner.scanner

import android.graphics.Rect
import android.graphics.RectF
import android.os.SystemClock
import androidx.annotation.OptIn
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.barcode.BarcodeScanner
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.ZoomSuggestionOptions
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import com.google.zxing.BarcodeFormat
import com.google.zxing.BinaryBitmap
import com.google.zxing.DecodeHintType
import com.google.zxing.InvertedLuminanceSource
import com.google.zxing.MultiFormatReader
import com.google.zxing.PlanarYUVLuminanceSource
import com.google.zxing.common.HybridBinarizer
import java.util.concurrent.atomic.AtomicBoolean

data class DetectedBarcode(
    val parsed: ParsedScanResult,
    val normalizedBounds: RectF?,
)

class BarcodeAnalyzer(
    private val previewAspectRatioProvider: () -> Float,
    private val autoZoomProvider: () -> Boolean,
    private val invertedScanProvider: () -> Boolean,
    private val onAutoZoomRequested: (Float) -> Unit,
    private val onBarcodeDetected: (DetectedBarcode) -> Unit,
) : ImageAnalysis.Analyzer {

    private val isPaused = AtomicBoolean(false)
    private var frameCounter = 0
    private var lastAutoZoomMs = 0L
    private var reusableYBuffer = ByteArray(0)
    private var reusableRowBuffer = ByteArray(0)

    private val mlKitScanner: BarcodeScanner = BarcodeScanning.getClient(
        BarcodeScannerOptions.Builder()
            .setBarcodeFormats(Barcode.FORMAT_ALL_FORMATS)
            .setZoomSuggestionOptions(
                ZoomSuggestionOptions.Builder { zoomRatio ->
                    val now = SystemClock.elapsedRealtime()
                    if (!autoZoomProvider() || isPaused.get() || now - lastAutoZoomMs < 650L) {
                        false
                    } else {
                        lastAutoZoomMs = now
                        onAutoZoomRequested(zoomRatio)
                        true
                    }
                }
                    .setMaxSupportedZoomRatio(4.0f)
                    .build(),
            )
            .build(),
    )

    private val zxingReader = MultiFormatReader().apply {
        setHints(
            mapOf(
                DecodeHintType.TRY_HARDER to true,
                DecodeHintType.POSSIBLE_FORMATS to listOf(
                    BarcodeFormat.QR_CODE,
                    BarcodeFormat.DATA_MATRIX,
                    BarcodeFormat.AZTEC,
                    BarcodeFormat.PDF_417,
                    BarcodeFormat.EAN_13,
                    BarcodeFormat.EAN_8,
                    BarcodeFormat.UPC_A,
                    BarcodeFormat.CODE_128,
                    BarcodeFormat.CODE_39,
                ),
            ),
        )
    }

    fun setPaused(paused: Boolean) {
        isPaused.set(paused)
    }

    fun close() {
        runCatching { mlKitScanner.close() }
    }

    @OptIn(ExperimentalGetImage::class)
    override fun analyze(imageProxy: ImageProxy) {
        if (isPaused.get()) {
            imageProxy.close()
            return
        }
        val mediaImage = imageProxy.image
        if (mediaImage == null) {
            imageProxy.close()
            return
        }

        val rotation = imageProxy.imageInfo.rotationDegrees
        val imgW = imageProxy.width
        val imgH = imageProxy.height
        val inputImage = InputImage.fromMediaImage(mediaImage, rotation)

        // Pre-copy Y plane only every 3rd frame if inverted fallback is enabled
        val shouldTryZxingFallback = invertedScanProvider() && (++frameCounter % 3 == 0)
        val yDataSnapshot: ByteArray? = if (shouldTryZxingFallback) extractYPlane(imageProxy) else null

        mlKitScanner.process(inputImage)
            .addOnSuccessListener { barcodes ->
                if (isPaused.get()) return@addOnSuccessListener
                val candidate = barcodes.firstOrNull { !it.rawValue.isNullOrBlank() }
                if (candidate != null) {
                    val raw = candidate.rawValue!!
                    val parsed = QrPayloadParser.parse(raw, candidate.format)
                    val normBox = candidate.boundingBox?.let { rect ->
                        mapRectToNormalizedPreview(
                            rect = rect,
                            imageWidth = imgW,
                            imageHeight = imgH,
                            rotationDegrees = rotation,
                            previewAspectRatio = previewAspectRatioProvider(),
                        )
                    }
                    isPaused.set(true)
                    onBarcodeDetected(DetectedBarcode(parsed, normBox))
                } else if (yDataSnapshot != null) {
                    val zxingResult = tryZxingDecode(yDataSnapshot, imgW, imgH, rotation)
                    if (zxingResult != null && !isPaused.get()) {
                        isPaused.set(true)
                        onBarcodeDetected(zxingResult)
                    }
                }
            }
            .addOnCompleteListener {
                imageProxy.close()
            }
    }

    private fun extractYPlane(imageProxy: ImageProxy): ByteArray? {
        return runCatching {
            val plane = imageProxy.planes[0]
            val buffer = plane.buffer
            val rowStride = plane.rowStride
            val width = imageProxy.width
            val height = imageProxy.height
            val needed = width * height
            if (reusableYBuffer.size != needed) {
                reusableYBuffer = ByteArray(needed)
            }
            val out = reusableYBuffer
            if (rowStride == width) {
                buffer.rewind()
                buffer.get(out, 0, needed)
            } else {
                if (reusableRowBuffer.size < rowStride) {
                    reusableRowBuffer = ByteArray(rowStride)
                }
                val row = reusableRowBuffer
                buffer.rewind()
                for (y in 0 until height) {
                    val remaining = buffer.remaining()
                    val toRead = minOf(rowStride, remaining)
                    buffer.get(row, 0, toRead)
                    System.arraycopy(row, 0, out, y * width, width)
                }
            }
            out
        }.getOrNull()
    }

    private fun tryZxingDecode(
        yData: ByteArray,
        width: Int,
        height: Int,
        rotationDegrees: Int,
    ): DetectedBarcode? {
        return runCatching {
            // Crop central 80% for fast inverted QR decoding
            val cropW = (width * 0.85f).toInt()
            val cropH = (height * 0.85f).toInt()
            val left = (width - cropW) / 2
            val top = (height - cropH) / 2

            val source = PlanarYUVLuminanceSource(
                yData,
                width,
                height,
                left,
                top,
                cropW,
                cropH,
                false,
            )

            val invertedBitmap = BinaryBitmap(HybridBinarizer(InvertedLuminanceSource(source)))
            val decoded = runCatching { zxingReader.decodeWithState(invertedBitmap) }.getOrNull()
                ?: runCatching {
                    val normalBitmap = BinaryBitmap(HybridBinarizer(source))
                    zxingReader.decodeWithState(normalBitmap)
                }.getOrNull()
                ?: return null

            val text = decoded.text?.takeIf { it.isNotBlank() } ?: return null
            val mlFormat = QrPayloadParser.fromZxingFormat(decoded.barcodeFormat)
            val parsed = QrPayloadParser.parse(text, mlFormat)

            // Estimate bounding box from ZXing resultPoints if available
            val pts = decoded.resultPoints
            val normBox = if (pts != null && pts.isNotEmpty()) {
                var minX = Float.MAX_VALUE
                var minY = Float.MAX_VALUE
                var maxX = Float.MIN_VALUE
                var maxY = Float.MIN_VALUE
                for (p in pts) {
                    if (p == null) continue
                    val px = p.x + left
                    val py = p.y + top
                    if (px < minX) minX = px
                    if (py < minY) minY = py
                    if (px > maxX) maxX = px
                    if (py > maxY) maxY = py
                }
                if (minX < maxX && minY < maxY) {
                    val pad = maxOf(maxX - minX, maxY - minY) * 0.15f
                    val rawRect = Rect(
                        (minX - pad).toInt(),
                        (minY - pad).toInt(),
                        (maxX + pad).toInt(),
                        (maxY + pad).toInt(),
                    )
                    // Result points in PlanarYUVLuminanceSource are in sensor orientation before rotation
                    mapSensorRectToNormalizedPreview(
                        rect = rawRect,
                        sensorWidth = width,
                        sensorHeight = height,
                        rotationDegrees = rotationDegrees,
                        previewAspectRatio = previewAspectRatioProvider(),
                    )
                } else {
                    null
                }
            } else {
                null
            }

            DetectedBarcode(parsed, normBox)
        }.getOrNull()
    }

    companion object {
        /**
         * Maps an ML Kit bounding box (already in upright/rotated image coordinates)
         * to normalized [0f..1f] coordinates matching PreviewView ScaleType.FILL_CENTER.
         */
        fun mapRectToNormalizedPreview(
            rect: Rect,
            imageWidth: Int,
            imageHeight: Int,
            rotationDegrees: Int,
            previewAspectRatio: Float,
        ): RectF {
            val isRotated = rotationDegrees == 90 || rotationDegrees == 270
            val uprightW = (if (isRotated) imageHeight else imageWidth).toFloat().coerceAtLeast(1f)
            val uprightH = (if (isRotated) imageWidth else imageHeight).toFloat().coerceAtLeast(1f)

            val imageAspect = uprightW / uprightH
            val viewAspect = previewAspectRatio.coerceAtLeast(0.1f)

            // FILL_CENTER crops either horizontally or vertically
            var scaleX = 1f
            var scaleY = 1f
            var offsetX = 0f
            var offsetY = 0f

            if (imageAspect > viewAspect) {
                // Image is wider than preview -> cropped left & right
                val visibleFraction = viewAspect / imageAspect
                scaleX = 1f / visibleFraction
                offsetX = (1f - visibleFraction) / 2f
            } else if (imageAspect < viewAspect) {
                // Image is taller than preview -> cropped top & bottom
                val visibleFraction = imageAspect / viewAspect
                scaleY = 1f / visibleFraction
                offsetY = (1f - visibleFraction) / 2f
            }

            val l = (((rect.left / uprightW) - offsetX) * scaleX).coerceIn(0.02f, 0.98f)
            val t = (((rect.top / uprightH) - offsetY) * scaleY).coerceIn(0.02f, 0.98f)
            val r = (((rect.right / uprightW) - offsetX) * scaleX).coerceIn(0.02f, 0.98f)
            val b = (((rect.bottom / uprightH) - offsetY) * scaleY).coerceIn(0.02f, 0.98f)
            return RectF(l, t, r, b)
        }

        private fun mapSensorRectToNormalizedPreview(
            rect: Rect,
            sensorWidth: Int,
            sensorHeight: Int,
            rotationDegrees: Int,
            previewAspectRatio: Float,
        ): RectF {
            val rotatedRect = when (rotationDegrees) {
                90 -> Rect(
                    sensorHeight - rect.bottom,
                    rect.left,
                    sensorHeight - rect.top,
                    rect.right,
                )
                180 -> Rect(
                    sensorWidth - rect.right,
                    sensorHeight - rect.bottom,
                    sensorWidth - rect.left,
                    sensorHeight - rect.top,
                )
                270 -> Rect(
                    rect.top,
                    sensorWidth - rect.right,
                    rect.bottom,
                    sensorWidth - rect.left,
                )
                else -> rect
            }
            return mapRectToNormalizedPreview(
                rect = rotatedRect,
                imageWidth = sensorWidth,
                imageHeight = sensorHeight,
                rotationDegrees = rotationDegrees,
                previewAspectRatio = previewAspectRatio,
            )
        }
    }
}
