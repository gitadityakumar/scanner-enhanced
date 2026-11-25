# Keep ML Kit Barcode Scanning classes and JNI
-keep class com.google.mlkit.** { *; }
-keep class com.google.android.gms.vision.** { *; }
-keep class com.google.android.gms.internal.mlkit_vision_barcode.** { *; }
-dontwarn com.google.mlkit.**

# Keep ZXing Core
-keep class com.google.zxing.** { *; }
-dontwarn com.google.zxing.**

# Keep CameraX
-keep class androidx.camera.** { *; }
-dontwarn androidx.camera.**

# Kotlinx Serialization
-keepattributes *Annotation*,InnerClasses
-dontnote kotlinx.serialization.SerializationKt
-keepclassmembers class * {
    @kotlinx.serialization.Serializable <fields>;
}
-keepclassmembers class * {
    companion <fields>;
}

# Keep Navigation and Data models
-keep class com.example.scanner.NavigationKeys** { *; }
-keep class com.example.scanner.scanner.ScanResult** { *; }
-keep class com.example.scanner.scanner.ParsedPayload** { *; }
-keep class com.example.scanner.scanner.ScanRecord** { *; }
