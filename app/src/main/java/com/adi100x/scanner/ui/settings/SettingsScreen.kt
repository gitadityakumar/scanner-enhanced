package com.adi100x.scanner.ui.settings

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.adi100x.scanner.R
import com.adi100x.scanner.scanner.ScannerPreferences
import com.adi100x.scanner.theme.MiuiCardDark
import com.adi100x.scanner.theme.MiuiDivider
import com.adi100x.scanner.theme.PureBlack
import com.adi100x.scanner.theme.TextSecondary
import com.adi100x.scanner.theme.XiaomiBlue

@Composable
fun SettingsScreen(
    preferences: ScannerPreferences,
    onBack: () -> Unit,
    onOpenHistory: () -> Unit,
) {
    val settings by preferences.settings.collectAsState()
    val history by preferences.history.collectAsState()
    var showAboutDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PureBlack)
            .statusBarsPadding()
            .navigationBarsPadding(),
    ) {
        // Top bar matching Xiaomi Scanner Settings screenshot
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .padding(horizontal = 8.dp),
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .size(44.dp)
                    .clip(CircleShape)
                    .clickable(onClick = onBack),
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.White,
                    modifier = Modifier.size(22.dp),
                )
            }

            Text(
                text = "Settings",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.align(Alignment.Center),
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
        ) {
            // Card 1: Scan History & About (matches Xiaomi rounded card style)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(MiuiCardDark),
            ) {
                SettingsNavRow(
                    title = "Scan History",
                    trailingText = if (history.isEmpty()) "Empty" else "${history.size}",
                    onClick = onOpenHistory,
                )
                HorizontalDivider(color = MiuiDivider.copy(alpha = 0.5f), modifier = Modifier.padding(horizontal = 18.dp))
                SettingsNavRow(
                    title = "About",
                    trailingText = "v1.0",
                    onClick = { showAboutDialog = true },
                )
            }

            Spacer(Modifier.height(16.dp))

            // Card 2: Enhanced Scanning Capabilities
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(MiuiCardDark),
            ) {
                SettingsToggleRow(
                    title = "Smart Auto-Zoom",
                    subtitle = "Automatically zoom in on small or distant QR codes",
                    checked = settings.autoZoomEnabled,
                    onCheckedChange = { enabled ->
                        preferences.updateSettings { it.copy(autoZoomEnabled = enabled) }
                    },
                )
                HorizontalDivider(color = MiuiDivider.copy(alpha = 0.5f), modifier = Modifier.padding(horizontal = 18.dp))
                SettingsToggleRow(
                    title = "Inverted & Low-Contrast QR",
                    subtitle = "Decode light-on-dark and stylized QR codes",
                    checked = settings.invertedQrEnabled,
                    onCheckedChange = { enabled ->
                        preferences.updateSettings { it.copy(invertedQrEnabled = enabled) }
                    },
                )
                HorizontalDivider(color = MiuiDivider.copy(alpha = 0.5f), modifier = Modifier.padding(horizontal = 18.dp))
                SettingsToggleRow(
                    title = "Confirmation Sound",
                    subtitle = "Play chime when a code is captured",
                    checked = settings.soundEnabled,
                    onCheckedChange = { enabled ->
                        preferences.updateSettings { it.copy(soundEnabled = enabled) }
                    },
                )
                HorizontalDivider(color = MiuiDivider.copy(alpha = 0.5f), modifier = Modifier.padding(horizontal = 18.dp))
                SettingsToggleRow(
                    title = "Haptic Vibration",
                    subtitle = "Vibrate on scan lock-on",
                    checked = settings.hapticEnabled,
                    onCheckedChange = { enabled ->
                        preferences.updateSettings { it.copy(hapticEnabled = enabled) }
                    },
                )
                HorizontalDivider(color = MiuiDivider.copy(alpha = 0.5f), modifier = Modifier.padding(horizontal = 18.dp))
                SettingsToggleRow(
                    title = "Continuous Scan",
                    subtitle = "Keep scanning without opening sheet each time",
                    checked = settings.continuousScanEnabled,
                    onCheckedChange = { enabled ->
                        preferences.updateSettings { it.copy(continuousScanEnabled = enabled) }
                    },
                )
            }
        }
    }

    if (showAboutDialog) {
        AlertDialog(
            onDismissRequest = { showAboutDialog = false },
            containerColor = MiuiCardDark,
            titleContentColor = Color.White,
            textContentColor = TextSecondary,
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Image(
                        painter = painterResource(R.drawable.ic_xiaomi_logo_about),
                        contentDescription = null,
                        modifier = Modifier.size(38.dp),
                    )
                    Spacer(Modifier.width(12.dp))
                    Text("Scanner", fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                }
            },
            text = {
                Text(
                    "Enhanced QR & Barcode Scanner powered by CameraX, ML Kit with Smart Auto-Zoom, and ZXing multi-pass inverted decoding. Supports instant UPI QR detection and Paytm-style capture animation.",
                    fontSize = 13.5.sp,
                    lineHeight = 19.sp,
                )
            },
            confirmButton = {
                TextButton(onClick = { showAboutDialog = false }) {
                    Text("OK", color = XiaomiBlue)
                }
            },
        )
    }
}

@Composable
private fun SettingsNavRow(
    title: String,
    trailingText: String? = null,
    onClick: () -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 17.dp),
    ) {
        Text(text = title, color = Color.White, fontSize = 15.5.sp, fontWeight = FontWeight.Normal)
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (!trailingText.isNullOrEmpty()) {
                Text(text = trailingText, color = TextSecondary, fontSize = 13.5.sp)
                Spacer(Modifier.width(4.dp))
            }
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                contentDescription = null,
                tint = TextSecondary,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

@Composable
private fun SettingsToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 18.dp, vertical = 13.dp),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Normal)
            Spacer(Modifier.height(2.dp))
            Text(text = subtitle, color = TextSecondary, fontSize = 12.sp)
        }
        Spacer(Modifier.width(12.dp))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = XiaomiBlue,
                uncheckedThumbColor = Color(0xFFB0B0B6),
                uncheckedTrackColor = Color(0xFF3A3A3C),
            ),
        )
    }
}
