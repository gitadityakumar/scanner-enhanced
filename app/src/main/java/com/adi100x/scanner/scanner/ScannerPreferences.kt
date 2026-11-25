package com.adi100x.scanner.scanner

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject

data class ScannerSettings(
    val autoZoomEnabled: Boolean = true,
    val invertedQrEnabled: Boolean = true,
    val soundEnabled: Boolean = true,
    val hapticEnabled: Boolean = true,
    val continuousScanEnabled: Boolean = false,
)

class ScannerPreferences(context: Context) {
    private val prefs = context.getSharedPreferences("scanner_enhanced_prefs", Context.MODE_PRIVATE)

    private val _settings = MutableStateFlow(loadSettings())
    val settings: StateFlow<ScannerSettings> = _settings.asStateFlow()

    private val _history = MutableStateFlow(loadHistory())
    val history: StateFlow<List<ParsedScanResult>> = _history.asStateFlow()

    private fun loadSettings(): ScannerSettings = ScannerSettings(
        autoZoomEnabled = prefs.getBoolean(KEY_AUTO_ZOOM, true),
        invertedQrEnabled = prefs.getBoolean(KEY_INVERTED_QR, true),
        soundEnabled = prefs.getBoolean(KEY_SOUND, true),
        hapticEnabled = prefs.getBoolean(KEY_HAPTIC, true),
        continuousScanEnabled = prefs.getBoolean(KEY_CONTINUOUS, false),
    )

    fun updateSettings(transform: (ScannerSettings) -> ScannerSettings) {
        val updated = transform(_settings.value)
        _settings.value = updated
        prefs.edit()
            .putBoolean(KEY_AUTO_ZOOM, updated.autoZoomEnabled)
            .putBoolean(KEY_INVERTED_QR, updated.invertedQrEnabled)
            .putBoolean(KEY_SOUND, updated.soundEnabled)
            .putBoolean(KEY_HAPTIC, updated.hapticEnabled)
            .putBoolean(KEY_CONTINUOUS, updated.continuousScanEnabled)
            .apply()
    }

    fun addHistoryItem(item: ParsedScanResult) {
        val current = _history.value.toMutableList()
        // Deduplicate recent identical rawValue at top
        current.removeAll { it.rawValue == item.rawValue }
        current.add(0, item)
        val trimmed = current.take(100)
        _history.value = trimmed
        saveHistory(trimmed)
    }

    fun removeHistoryItem(timestampMs: Long) {
        val updated = _history.value.filterNot { it.timestampMs == timestampMs }
        _history.value = updated
        saveHistory(updated)
    }

    fun clearHistory() {
        _history.value = emptyList()
        prefs.edit().remove(KEY_HISTORY).apply()
    }

    private fun loadHistory(): List<ParsedScanResult> {
        val rawJson = prefs.getString(KEY_HISTORY, null) ?: return emptyList()
        return runCatching {
            val arr = JSONArray(rawJson)
            buildList {
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    val rawVal = obj.getString("rawValue")
                    val fmtInt = obj.optInt("formatInt", com.google.mlkit.vision.barcode.common.Barcode.FORMAT_QR_CODE)
                    val parsed = QrPayloadParser.parse(rawVal, fmtInt).copy(
                        formatLabel = obj.optString("formatLabel", "QR Code"),
                        timestampMs = obj.optLong("timestampMs", System.currentTimeMillis()),
                    )
                    add(parsed)
                }
            }
        }.getOrDefault(emptyList())
    }

    private fun saveHistory(items: List<ParsedScanResult>) {
        val arr = JSONArray()
        for (item in items) {
            arr.put(
                JSONObject().apply {
                    put("rawValue", item.rawValue)
                    put("formatLabel", item.formatLabel)
                    put("timestampMs", item.timestampMs)
                },
            )
        }
        prefs.edit().putString(KEY_HISTORY, arr.toString()).apply()
    }

    companion object {
        private const val KEY_AUTO_ZOOM = "auto_zoom"
        private const val KEY_INVERTED_QR = "inverted_qr"
        private const val KEY_SOUND = "sound_enabled"
        private const val KEY_HAPTIC = "haptic_enabled"
        private const val KEY_CONTINUOUS = "continuous_scan"
        private const val KEY_HISTORY = "scan_history_json"
    }
}
