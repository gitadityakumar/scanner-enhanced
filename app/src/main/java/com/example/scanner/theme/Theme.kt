package com.example.scanner.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val ScannerDarkColorScheme = darkColorScheme(
    primary = XiaomiBlue,
    onPrimary = Color.White,
    secondary = PaytmCyan,
    onSecondary = Color.White,
    tertiary = PaytmGreen,
    background = PureBlack,
    onBackground = TextPrimary,
    surface = MiuiCardDark,
    onSurface = TextPrimary,
    surfaceVariant = Color(0xFF262629),
    onSurfaceVariant = TextSecondary,
)

@Composable
fun MyApplicationTheme(
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = ScannerDarkColorScheme,
        typography = Typography,
        content = content,
    )
}
