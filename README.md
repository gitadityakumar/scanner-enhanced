# Scanner Enhanced

A modern, high-performance QR & barcode scanner Android app inspired by Xiaomi Scanner — rebuilt from scratch with enhanced scanning capabilities and a Paytm-style scan animation.

## ✨ Features

- **Multi-format scanning** — QR codes, barcodes (EAN-13, UPC-A, Code 128, etc.), PDF417, Data Matrix, Aztec
- **Dual-engine decoding** — ML Kit primary + ZXing fallback for inverted/damaged codes
- **Gallery import** — Decode QR/barcodes from saved images with multi-pass processing
- **Paytm-style animation** — Spring-snap viewfinder + ripple burst + checkmark on successful scan
- **Smart payload parsing** — Auto-detects UPI, Wi-Fi, URL, vCard/meCard, and plain text
- **Action sheet** — One-tap actions: open URL, connect Wi-Fi, copy to clipboard, share
- **Scan history** — Persistent history with swipe-to-delete
- **Flashlight toggle** — Quick torch control from the camera screen
- **Dark MIUI-style UI** — Matches Xiaomi Scanner's visual language
- **Low memory footprint** — Reusable buffers, capped resolution, R8 minification

## 📱 Screenshots

The app replicates Xiaomi Scanner's dark UI with a centered viewfinder, bottom action bar (flash / gallery), and a settings screen.

## 🏗️ Tech Stack

| Component | Library |
|-----------|---------|
| Camera | CameraX 1.4.2 |
| ML Barcode | ML Kit Barcode 17.3.0 |
| Fallback decoder | ZXing Core 3.5.3 |
| UI | Jetpack Compose (BOM 2026.03.01) |
| Navigation | Navigation3 |
| Preferences | SharedPreferences |
| Min SDK | 26 (Android 8.0) |
| Target SDK | 36 |

## 🚀 Build

```bash
# Debug APK
./gradlew assembleDebug

# Run unit tests
./gradlew testDebugUnitTest
```

The debug APK is output to `app/build/outputs/apk/debug/app-debug.apk`.

## 📦 Install

```bash
adb install app/build/outputs/apk/debug/app-debug.apk
```

## 📄 License

MIT
