package com.example.scanner.scanner

import android.net.Uri
import com.google.mlkit.vision.barcode.common.Barcode
import java.net.URLDecoder
import java.nio.charset.StandardCharsets

enum class ScanContentType {
    UPI,
    URL,
    WIFI,
    PHONE,
    EMAIL,
    SMS,
    GEO,
    CONTACT,
    PRODUCT_BARCODE,
    TEXT,
}

data class UpiPayload(
    val pa: String,
    val pn: String?,
    val am: String?,
    val cu: String?,
    val tn: String?,
    val mc: String?,
    val tr: String?,
    val rawUri: String,
)

data class WifiPayload(
    val ssid: String,
    val password: String?,
    val encryption: String,
    val hidden: Boolean,
)

data class ParsedScanResult(
    val rawValue: String,
    val formatLabel: String,
    val type: ScanContentType,
    val title: String,
    val subtitle: String?,
    val upi: UpiPayload? = null,
    val wifi: WifiPayload? = null,
    val timestampMs: Long = System.currentTimeMillis(),
)

object QrPayloadParser {

    fun fromZxingFormat(fmt: com.google.zxing.BarcodeFormat): Int = when (fmt) {
        com.google.zxing.BarcodeFormat.QR_CODE -> Barcode.FORMAT_QR_CODE
        com.google.zxing.BarcodeFormat.AZTEC -> Barcode.FORMAT_AZTEC
        com.google.zxing.BarcodeFormat.DATA_MATRIX -> Barcode.FORMAT_DATA_MATRIX
        com.google.zxing.BarcodeFormat.PDF_417 -> Barcode.FORMAT_PDF417
        com.google.zxing.BarcodeFormat.EAN_13 -> Barcode.FORMAT_EAN_13
        com.google.zxing.BarcodeFormat.EAN_8 -> Barcode.FORMAT_EAN_8
        com.google.zxing.BarcodeFormat.UPC_A -> Barcode.FORMAT_UPC_A
        com.google.zxing.BarcodeFormat.UPC_E -> Barcode.FORMAT_UPC_E
        com.google.zxing.BarcodeFormat.CODE_128 -> Barcode.FORMAT_CODE_128
        com.google.zxing.BarcodeFormat.CODE_39 -> Barcode.FORMAT_CODE_39
        com.google.zxing.BarcodeFormat.CODE_93 -> Barcode.FORMAT_CODE_93
        com.google.zxing.BarcodeFormat.CODABAR -> Barcode.FORMAT_CODABAR
        com.google.zxing.BarcodeFormat.ITF -> Barcode.FORMAT_ITF
        else -> Barcode.FORMAT_UNKNOWN
    }

    fun formatName(mlKitFormat: Int): String = when (mlKitFormat) {
        Barcode.FORMAT_QR_CODE -> "QR Code"
        Barcode.FORMAT_AZTEC -> "Aztec"
        Barcode.FORMAT_DATA_MATRIX -> "Data Matrix"
        Barcode.FORMAT_PDF417 -> "PDF417"
        Barcode.FORMAT_EAN_13 -> "EAN-13"
        Barcode.FORMAT_EAN_8 -> "EAN-8"
        Barcode.FORMAT_UPC_A -> "UPC-A"
        Barcode.FORMAT_UPC_E -> "UPC-E"
        Barcode.FORMAT_CODE_128 -> "Code 128"
        Barcode.FORMAT_CODE_39 -> "Code 39"
        Barcode.FORMAT_CODE_93 -> "Code 93"
        Barcode.FORMAT_CODABAR -> "Codabar"
        Barcode.FORMAT_ITF -> "ITF"
        else -> "Code"
    }

    fun parse(rawValue: String, mlKitFormat: Int = Barcode.FORMAT_QR_CODE): ParsedScanResult {
        val trimmed = rawValue.trim()
        val formatLabel = formatName(mlKitFormat)

        parseUpi(trimmed)?.let { upi ->
            val payee = upi.pn?.takeIf { it.isNotBlank() } ?: upi.pa
            val amtPart = upi.am?.takeIf { it.isNotBlank() }?.let { " • ₹$it" }.orEmpty()
            return ParsedScanResult(
                rawValue = rawValue,
                formatLabel = formatLabel,
                type = ScanContentType.UPI,
                title = payee,
                subtitle = "${upi.pa}$amtPart",
                upi = upi,
            )
        }

        if (trimmed.startsWith("WIFI:", ignoreCase = true)) {
            val wifi = parseWifi(trimmed)
            return ParsedScanResult(
                rawValue = rawValue,
                formatLabel = formatLabel,
                type = ScanContentType.WIFI,
                title = wifi.ssid.ifBlank { "Wi-Fi Network" },
                subtitle = "Security: ${wifi.encryption}",
                wifi = wifi,
            )
        }

        if (trimmed.startsWith("http://", ignoreCase = true) ||
            trimmed.startsWith("https://", ignoreCase = true)
        ) {
            val host = runCatching { java.net.URI(trimmed).host }.getOrNull()
            return ParsedScanResult(
                rawValue = rawValue,
                formatLabel = formatLabel,
                type = ScanContentType.URL,
                title = host?.takeIf { it.isNotBlank() } ?: "Web Link",
                subtitle = trimmed,
            )
        }

        if (trimmed.startsWith("www.", ignoreCase = true)) {
            return ParsedScanResult(
                rawValue = rawValue,
                formatLabel = formatLabel,
                type = ScanContentType.URL,
                title = trimmed.substringBefore("/"),
                subtitle = "https://$trimmed",
            )
        }

        if (trimmed.startsWith("tel:", ignoreCase = true)) {
            val number = trimmed.removeRange(0, 4).trim()
            return ParsedScanResult(
                rawValue = rawValue,
                formatLabel = formatLabel,
                type = ScanContentType.PHONE,
                title = number.ifBlank { "Phone Number" },
                subtitle = "Tap to call",
            )
        }

        if (trimmed.startsWith("mailto:", ignoreCase = true) || trimmed.startsWith("MATMSG:", ignoreCase = true)) {
            val email = if (trimmed.startsWith("mailto:", ignoreCase = true)) {
                trimmed.substring(7).substringBefore("?")
            } else {
                Regex("TO:([^;]+)", RegexOption.IGNORE_CASE).find(trimmed)?.groupValues?.getOrNull(1) ?: trimmed
            }
            return ParsedScanResult(
                rawValue = rawValue,
                formatLabel = formatLabel,
                type = ScanContentType.EMAIL,
                title = email.ifBlank { "Email" },
                subtitle = "Email address",
            )
        }

        if (trimmed.startsWith("smsto:", ignoreCase = true) || trimmed.startsWith("sms:", ignoreCase = true)) {
            val target = trimmed.substringAfter(":").substringBefore(":")
            return ParsedScanResult(
                rawValue = rawValue,
                formatLabel = formatLabel,
                type = ScanContentType.SMS,
                title = target.ifBlank { "SMS Message" },
                subtitle = "Tap to send message",
            )
        }

        if (trimmed.startsWith("geo:", ignoreCase = true)) {
            val coords = trimmed.substring(4).substringBefore("?")
            return ParsedScanResult(
                rawValue = rawValue,
                formatLabel = formatLabel,
                type = ScanContentType.GEO,
                title = "Location",
                subtitle = coords,
            )
        }

        if (trimmed.startsWith("BEGIN:VCARD", ignoreCase = true) ||
            trimmed.startsWith("MECARD:", ignoreCase = true)
        ) {
            val fn = Regex("(?:^|\\n)FN:(.+)", RegexOption.IGNORE_CASE).find(trimmed)?.groupValues?.getOrNull(1)?.trim()
                ?: Regex("N:([^;]+)", RegexOption.IGNORE_CASE).find(trimmed)?.groupValues?.getOrNull(1)?.trim()
                ?: "Contact Card"
            return ParsedScanResult(
                rawValue = rawValue,
                formatLabel = formatLabel,
                type = ScanContentType.CONTACT,
                title = fn,
                subtitle = "Contact details",
            )
        }

        if (mlKitFormat in setOf(
                Barcode.FORMAT_EAN_13,
                Barcode.FORMAT_EAN_8,
                Barcode.FORMAT_UPC_A,
                Barcode.FORMAT_UPC_E,
                Barcode.FORMAT_CODE_128,
                Barcode.FORMAT_CODE_39,
                Barcode.FORMAT_CODE_93,
                Barcode.FORMAT_ITF,
                Barcode.FORMAT_CODABAR,
            )
        ) {
            return ParsedScanResult(
                rawValue = rawValue,
                formatLabel = formatLabel,
                type = ScanContentType.PRODUCT_BARCODE,
                title = trimmed,
                subtitle = "$formatLabel Barcode",
            )
        }

        return ParsedScanResult(
            rawValue = rawValue,
            formatLabel = formatLabel,
            type = ScanContentType.TEXT,
            title = if (trimmed.length > 48) trimmed.take(48) + "…" else trimmed,
            subtitle = "$formatLabel • ${trimmed.length} chars",
        )
    }

    fun parseUpi(raw: String): UpiPayload? {
        if (!raw.startsWith("upi://pay", ignoreCase = true) &&
            !raw.startsWith("upi:pay", ignoreCase = true)
        ) {
            return null
        }
        val query = raw.substringAfter("?", "")
        if (query.isBlank()) return null

        val params = mutableMapOf<String, String>()
        for (pair in query.split("&")) {
            val idx = pair.indexOf('=')
            if (idx > 0) {
                val key = pair.substring(0, idx).lowercase()
                val rawVal = pair.substring(idx + 1)
                val decoded = runCatching {
                    URLDecoder.decode(rawVal, StandardCharsets.UTF_8.name())
                }.getOrDefault(rawVal)
                params[key] = decoded
            }
        }
        val pa = params["pa"]?.takeIf { it.isNotBlank() } ?: return null
        return UpiPayload(
            pa = pa,
            pn = params["pn"],
            am = params["am"],
            cu = params["cu"] ?: "INR",
            tn = params["tn"],
            mc = params["mc"],
            tr = params["tr"],
            rawUri = raw,
        )
    }

    fun parseWifi(raw: String): WifiPayload {
        // Format: WIFI:T:WPA;S:MySSID;P:MyPass;H:true;;
        val body = raw.removePrefix("WIFI:").removePrefix("wifi:")
        var ssid = ""
        var password: String? = null
        var encryption = "Open"
        var hidden = false

        val tokens = splitEscaped(body, ';')
        for (token in tokens) {
            when {
                token.startsWith("S:", ignoreCase = true) -> ssid = unescapeWifi(token.substring(2))
                token.startsWith("P:", ignoreCase = true) -> password = unescapeWifi(token.substring(2))
                token.startsWith("T:", ignoreCase = true) -> {
                    encryption = when (token.substring(2).uppercase()) {
                        "WPA", "WPA2", "WPA/WPA2" -> "WPA/WPA2"
                        "WEP" -> "WEP"
                        "NOPASS", "" -> "Open"
                        else -> token.substring(2)
                    }
                }
                token.startsWith("H:", ignoreCase = true) -> hidden = token.substring(2).equals("true", ignoreCase = true)
            }
        }
        return WifiPayload(
            ssid = ssid,
            password = password?.takeIf { it.isNotEmpty() },
            encryption = encryption,
            hidden = hidden,
        )
    }

    private fun splitEscaped(input: String, delimiter: Char): List<String> {
        val result = mutableListOf<String>()
        val current = StringBuilder()
        var escaped = false
        for (ch in input) {
            if (escaped) {
                current.append('\\').append(ch)
                escaped = false
            } else if (ch == '\\') {
                escaped = true
            } else if (ch == delimiter) {
                if (current.isNotEmpty()) {
                    result.add(current.toString())
                    current.clear()
                }
            } else {
                current.append(ch)
            }
        }
        if (current.isNotEmpty()) {
            result.add(current.toString())
        }
        return result
    }

    private fun unescapeWifi(value: String): String =
        value.replace("\\;", ";")
            .replace("\\:", ":")
            .replace("\\,", ",")
            .replace("\\\\", "\\")
}
