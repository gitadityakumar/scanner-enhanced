package com.adi100x.scanner.scanner

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.util.Log
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import com.google.zxing.BinaryBitmap
import com.google.zxing.DecodeHintType
import com.google.zxing.InvertedLuminanceSource
import com.google.zxing.MultiFormatReader
import com.google.zxing.RGBLuminanceSource
import com.google.zxing.common.GlobalHistogramBinarizer
import com.google.zxing.common.HybridBinarizer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume

object GalleryDecoder {

    suspend fun decodeUri(context: Context, uri: Uri): ParsedScanResult? = withContext(Dispatchers.IO) {
        val scanner = BarcodeScanning.getClient(
            BarcodeScannerOptions.Builder()
                .setBarcodeFormats(Barcode.FORMAT_ALL_FORMATS)
                .build(),
        )
        try {
            // Pass 1: Direct ML Kit from URI (handles EXIF cleanly)
            val directImage = runCatching { InputImage.fromFilePath(context, uri) }.getOrNull()
            if (directImage != null) {
                val directHit = scanWithMlKit(scanner, directImage)
                if (directHit != null) return@withContext directHit
            }

            // Pass 2: Scaled Bitmap + multi-rotation ML Kit + ZXing (normal, inverted, global histogram)
            val bitmap = loadScaledBitmap(context, uri, maxDim = 1280) ?: return@withContext null
            try {
                for (angle in intArrayOf(90, 180, 270, 0)) {
                    val candidateBmp = if (angle == 0) bitmap else rotateBitmap(bitmap, angle.toFloat())
                    try {
                        if (angle != 0) {
                            val mlHit = scanWithMlKit(scanner, InputImage.fromBitmap(candidateBmp, 0))
                            if (mlHit != null) return@withContext mlHit
                        }
                    } finally {
                        if (candidateBmp !== bitmap) candidateBmp.recycle()
                    }
                }

                // Pass 3: ZXing multi-pass (Hybrid, Inverted, GlobalHistogram)
                val zxingHit = decodeWithZxing(bitmap)
                if (zxingHit != null) return@withContext zxingHit
            } finally {
                bitmap.recycle()
            }
            null
        } catch (e: Throwable) {
            Log.w("GalleryDecoder", "Failed decoding gallery URI", e)
            null
        } finally {
            runCatching { scanner.close() }
        }
    }

    private suspend fun scanWithMlKit(
        scanner: com.google.mlkit.vision.barcode.BarcodeScanner,
        image: InputImage,
    ): ParsedScanResult? = suspendCancellableCoroutine { cont ->
        scanner.process(image)
            .addOnSuccessListener { list ->
                val hit = list.firstOrNull { !it.rawValue.isNullOrBlank() }
                if (hit != null) {
                    cont.resume(QrPayloadParser.parse(hit.rawValue!!, hit.format))
                } else {
                    cont.resume(null)
                }
            }
            .addOnFailureListener {
                cont.resume(null)
            }
    }

    private fun decodeWithZxing(bitmap: Bitmap): ParsedScanResult? {
        val w = bitmap.width
        val h = bitmap.height
        val pixels = IntArray(w * h)
        bitmap.getPixels(pixels, 0, w, 0, 0, w, h)
        val source = RGBLuminanceSource(w, h, pixels)

        val reader = MultiFormatReader().apply {
            setHints(
                mapOf(
                    DecodeHintType.TRY_HARDER to true,
                    DecodeHintType.ALSO_INVERTED to true,
                ),
            )
        }

        val candidates = listOf(
            BinaryBitmap(HybridBinarizer(source)),
            BinaryBitmap(HybridBinarizer(InvertedLuminanceSource(source))),
            BinaryBitmap(GlobalHistogramBinarizer(source)),
        )

        for (bin in candidates) {
            val res = runCatching { reader.decodeWithState(bin) }.getOrNull()
            val text = res?.text?.takeIf { it.isNotBlank() }
            if (text != null) {
                return QrPayloadParser.parse(text, QrPayloadParser.fromZxingFormat(res.barcodeFormat))
            }
        }
        return null
    }

    private fun loadScaledBitmap(context: Context, uri: Uri, maxDim: Int): Bitmap? {
        val boundsOpts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, boundsOpts)
        }
        val origW = boundsOpts.outWidth
        val origH = boundsOpts.outHeight
        if (origW <= 0 || origH <= 0) return null

        var sampleSize = 1
        while (origW / sampleSize > maxDim || origH / sampleSize > maxDim) {
            sampleSize *= 2
        }
        val decodeOpts = BitmapFactory.Options().apply {
            inSampleSize = sampleSize
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }
        return context.contentResolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, decodeOpts)
        }
    }

    private fun rotateBitmap(src: Bitmap, degrees: Float): Bitmap {
        val matrix = Matrix().apply { postRotate(degrees) }
        return Bitmap.createBitmap(src, 0, 0, src.width, src.height, matrix, true)
    }
}
