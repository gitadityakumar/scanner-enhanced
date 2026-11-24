package com.example.scanner.ui.scanner

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Email
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.QrCode2
import androidx.compose.material.icons.rounded.QrCodeScanner
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Sms
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.scanner.scanner.ParsedScanResult
import com.example.scanner.scanner.ScanContentType
import com.example.scanner.theme.MiuiCardDark
import com.example.scanner.theme.MiuiDivider
import com.example.scanner.theme.PaytmCyan
import com.example.scanner.theme.PaytmGreen
import com.example.scanner.theme.TextSecondary
import com.example.scanner.theme.XiaomiBlue

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScanResultSheet(
    result: ParsedScanResult,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color(0xFF141416),
        contentColor = Color.White,
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(top = 10.dp, bottom = 6.dp)
                    .size(width = 38.dp, height = 4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Color(0xFF48484A)),
            )
        },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 20.dp)
                .navigationBarsPadding(),
        ) {
            // Header badge row
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
            ) {
                val (icon, accent, typeLabel) = badgeMeta(result.type)
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(accent.copy(alpha = 0.16f)),
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = accent,
                        modifier = Modifier.size(24.dp),
                    )
                }
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = typeLabel,
                            color = accent,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Spacer(Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .background(Color(0xFF262629), RoundedCornerShape(6.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp),
                        ) {
                            Text(
                                text = result.formatLabel,
                                color = TextSecondary,
                                fontSize = 11.sp,
                            )
                        }
                    }
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = result.title,
                        color = Color.White,
                        fontSize = 19.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            Spacer(Modifier.height(14.dp))

            // Type-specific rich detail card
            when (result.type) {
                ScanContentType.UPI -> result.upi?.let { upi ->
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(MiuiCardDark)
                            .border(1.dp, PaytmCyan.copy(alpha = 0.35f), RoundedCornerShape(16.dp))
                            .padding(16.dp),
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Rounded.CheckCircle,
                                contentDescription = null,
                                tint = PaytmGreen,
                                modifier = Modifier.size(18.dp),
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = "Verified UPI QR Details",
                                color = PaytmGreen,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                        Spacer(Modifier.height(10.dp))
                        DetailRow("UPI ID", upi.pa)
                        upi.pn?.takeIf { it.isNotBlank() }?.let { DetailRow("Payee Name", it) }
                        upi.am?.takeIf { it.isNotBlank() }?.let { DetailRow("Amount", "₹$it (${upi.cu ?: "INR"})") }
                        upi.tn?.takeIf { it.isNotBlank() }?.let { DetailRow("Note", it) }
                    }
                    Spacer(Modifier.height(12.dp))
                }

                ScanContentType.WIFI -> result.wifi?.let { wifi ->
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(MiuiCardDark)
                            .border(1.dp, MiuiDivider, RoundedCornerShape(16.dp))
                            .padding(16.dp),
                    ) {
                        DetailRow("Network (SSID)", wifi.ssid.ifBlank { "Hidden" })
                        DetailRow("Security", wifi.encryption)
                        wifi.password?.let { DetailRow("Password", it) }
                    }
                    Spacer(Modifier.height(12.dp))
                }

                else -> Unit
            }

            // Raw scanned string block (always visible & selectable)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0xFF1B1B1E))
                    .border(1.dp, MiuiDivider, RoundedCornerShape(14.dp))
                    .padding(14.dp),
            ) {
                Text(
                    text = "SCANNED STRING",
                    color = TextSecondary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 0.8.sp,
                )
                Spacer(Modifier.height(6.dp))
                SelectionContainer {
                    Text(
                        text = result.rawValue,
                        color = Color(0xFFEAEAEF),
                        fontSize = 13.5.sp,
                        fontFamily = if (result.type == ScanContentType.TEXT || result.type == ScanContentType.UPI) {
                            FontFamily.Monospace
                        } else {
                            FontFamily.Default
                        },
                        lineHeight = 19.sp,
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            // Primary contextual action
            when (result.type) {
                ScanContentType.UPI -> {
                    Button(
                        onClick = {
                            val upiUri = result.upi?.rawUri ?: result.rawValue
                            launchSafe(context, Intent(Intent.ACTION_VIEW, Uri.parse(upiUri)), "No UPI app found on device")
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = PaytmCyan, contentColor = Color.White),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                    ) {
                        Icon(Icons.Rounded.AccountBalanceWallet, contentDescription = null, modifier = Modifier.size(19.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Pay with UPI App", fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                    }
                    Spacer(Modifier.height(10.dp))
                }

                ScanContentType.URL -> {
                    val targetUrl = if (result.rawValue.startsWith("http", ignoreCase = true)) {
                        result.rawValue.trim()
                    } else {
                        "https://${result.rawValue.trim()}"
                    }
                    Button(
                        onClick = {
                            launchSafe(context, Intent(Intent.ACTION_VIEW, Uri.parse(targetUrl)), "No browser found")
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = XiaomiBlue, contentColor = Color.White),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                    ) {
                        Icon(Icons.AutoMirrored.Rounded.OpenInNew, contentDescription = null, modifier = Modifier.size(19.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Open Link", fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                    }
                    Spacer(Modifier.height(10.dp))
                }

                ScanContentType.WIFI -> {
                    val wifiPwd = result.wifi?.password
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                        if (!wifiPwd.isNullOrEmpty()) {
                            Button(
                                onClick = { copyToClipboard(context, "Wi-Fi Password", wifiPwd) },
                                colors = ButtonDefaults.buttonColors(containerColor = XiaomiBlue, contentColor = Color.White),
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp),
                            ) {
                                Icon(Icons.Rounded.ContentCopy, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Copy Password", fontWeight = FontWeight.SemiBold)
                            }
                        }
                        OutlinedButton(
                            onClick = {
                                launchSafe(context, Intent(Settings.ACTION_WIFI_SETTINGS), "Unable to open Wi-Fi settings")
                            },
                            border = ButtonDefaults.outlinedButtonBorder(enabled = true).copy(brush = SolidColor(MiuiDivider)),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp),
                        ) {
                            Icon(Icons.Rounded.Wifi, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Wi-Fi Settings", color = Color.White)
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                }

                ScanContentType.PHONE -> {
                    Button(
                        onClick = {
                            launchSafe(context, Intent(Intent.ACTION_DIAL, Uri.parse(result.rawValue.trim())), "No dialer found")
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = XiaomiBlue, contentColor = Color.White),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                    ) {
                        Icon(Icons.Rounded.Call, contentDescription = null, modifier = Modifier.size(19.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Call Number", fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                    }
                    Spacer(Modifier.height(10.dp))
                }

                ScanContentType.EMAIL -> {
                    Button(
                        onClick = {
                            val mailUri = if (result.rawValue.startsWith("mailto:", ignoreCase = true)) {
                                result.rawValue.trim()
                            } else {
                                "mailto:${result.title}"
                            }
                            launchSafe(context, Intent(Intent.ACTION_SENDTO, Uri.parse(mailUri)), "No email app found")
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = XiaomiBlue, contentColor = Color.White),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                    ) {
                        Icon(Icons.Rounded.Email, contentDescription = null, modifier = Modifier.size(19.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Send Email", fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                    }
                    Spacer(Modifier.height(10.dp))
                }

                ScanContentType.GEO -> {
                    Button(
                        onClick = {
                            launchSafe(context, Intent(Intent.ACTION_VIEW, Uri.parse(result.rawValue.trim())), "No maps app found")
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = XiaomiBlue, contentColor = Color.White),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                    ) {
                        Icon(Icons.Rounded.LocationOn, contentDescription = null, modifier = Modifier.size(19.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Open in Maps", fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                    }
                    Spacer(Modifier.height(10.dp))
                }

                ScanContentType.PRODUCT_BARCODE -> {
                    Button(
                        onClick = {
                            val url = "https://www.google.com/search?q=${Uri.encode(result.rawValue.trim())}"
                            launchSafe(context, Intent(Intent.ACTION_VIEW, Uri.parse(url)), "No browser found")
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = XiaomiBlue, contentColor = Color.White),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                    ) {
                        Icon(Icons.Rounded.Language, contentDescription = null, modifier = Modifier.size(19.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Search Product Online", fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                    }
                    Spacer(Modifier.height(10.dp))
                }

                else -> Unit
            }

            // Secondary row: Copy + Share + Scan Again
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                OutlinedButton(
                    onClick = {
                        copyToClipboard(
                            context,
                            if (result.upi != null) "UPI String" else "Scanned Text",
                            result.upi?.pa ?: result.rawValue,
                        )
                    },
                    border = ButtonDefaults.outlinedButtonBorder(enabled = true).copy(brush = SolidColor(MiuiDivider)),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(46.dp),
                ) {
                    Icon(Icons.Rounded.ContentCopy, contentDescription = null, tint = Color.White, modifier = Modifier.size(17.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(if (result.upi != null) "Copy UPI" else "Copy", color = Color.White, fontSize = 14.sp)
                }

                OutlinedButton(
                    onClick = {
                        val send = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, result.rawValue)
                        }
                        launchSafe(context, Intent.createChooser(send, "Share scanned result"), "Unable to share")
                    },
                    border = ButtonDefaults.outlinedButtonBorder(enabled = true).copy(brush = SolidColor(MiuiDivider)),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(46.dp),
                ) {
                    Icon(Icons.Rounded.Share, contentDescription = null, tint = Color.White, modifier = Modifier.size(17.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Share", color = Color.White, fontSize = 14.sp)
                }

                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2C2C2E), contentColor = Color.White),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(46.dp),
                ) {
                    Text("Scan Next", fontSize = 14.sp, fontWeight = FontWeight.Medium)
                }
            }
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
    ) {
        Text(text = label, color = TextSecondary, fontSize = 13.sp)
        Spacer(Modifier.width(12.dp))
        Text(
            text = value,
            color = Color.White,
            fontSize = 13.5.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

private fun badgeMeta(type: ScanContentType): Triple<ImageVector, Color, String> = when (type) {
    ScanContentType.UPI -> Triple(Icons.Rounded.AccountBalanceWallet, PaytmCyan, "UPI Payment QR")
    ScanContentType.URL -> Triple(Icons.Rounded.Language, XiaomiBlue, "Website Link")
    ScanContentType.WIFI -> Triple(Icons.Rounded.Wifi, XiaomiBlue, "Wi-Fi Network")
    ScanContentType.PHONE -> Triple(Icons.Rounded.Call, PaytmGreen, "Phone Number")
    ScanContentType.EMAIL -> Triple(Icons.Rounded.Email, XiaomiBlue, "Email Address")
    ScanContentType.SMS -> Triple(Icons.Rounded.Sms, XiaomiBlue, "SMS Message")
    ScanContentType.GEO -> Triple(Icons.Rounded.LocationOn, PaytmGreen, "Location")
    ScanContentType.CONTACT -> Triple(Icons.Rounded.Person, XiaomiBlue, "Contact Card")
    ScanContentType.PRODUCT_BARCODE -> Triple(Icons.Rounded.QrCodeScanner, XiaomiBlue, "Product Barcode")
    ScanContentType.TEXT -> Triple(Icons.Rounded.QrCode2, XiaomiBlue, "Decoded Text")
}

private fun copyToClipboard(context: Context, label: String, text: String) {
    val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    cm.setPrimaryClip(ClipData.newPlainText(label, text))
    Toast.makeText(context, "Copied to clipboard", Toast.LENGTH_SHORT).show()
}

private fun launchSafe(context: Context, intent: Intent, fallbackMsg: String) {
    runCatching {
        context.startActivity(intent)
    }.onFailure {
        Toast.makeText(context, fallbackMsg, Toast.LENGTH_SHORT).show()
    }
}
