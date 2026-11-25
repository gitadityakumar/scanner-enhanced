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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.scanner.scanner.ParsedScanResult
import com.example.scanner.scanner.ScanContentType
import com.example.scanner.theme.PaytmCyan
import com.example.scanner.theme.PaytmGreen
import com.example.scanner.theme.TextSecondary
import com.example.scanner.theme.XiaomiBlue

private val SheetSurface = Color(0xFF121318)
private val SheetCard = Color(0xFF1A1C23)
private val SheetBorder = Color.White.copy(alpha = 0.08f)
private val SheetDivider = Color.White.copy(alpha = 0.06f)
private val TonalButtonBg = Color(0xFF20222B)
private val ElevatedButtonBg = Color(0xFF2B2E3A)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScanResultSheet(
    result: ParsedScanResult,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val (icon, accent, typeLabel) = badgeMeta(result.type)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        containerColor = SheetSurface,
        contentColor = Color.White,
        scrimColor = Color.Black.copy(alpha = 0.56f),
        dragHandle = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(accent.copy(alpha = 0.09f), Color.Transparent),
                        ),
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    modifier = Modifier
                        .padding(top = 12.dp, bottom = 8.dp)
                        .size(width = 40.dp, height = 4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(Color.White.copy(alpha = 0.22f)),
                )
            }
        },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(top = 4.dp, bottom = 28.dp)
                .navigationBarsPadding(),
        ) {
            // Premium Header Row
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(50.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(
                            Brush.linearGradient(
                                colors = listOf(
                                    accent.copy(alpha = 0.24f),
                                    accent.copy(alpha = 0.08f),
                                ),
                            ),
                        )
                        .border(1.dp, accent.copy(alpha = 0.28f), RoundedCornerShape(16.dp)),
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = accent,
                        modifier = Modifier.size(25.dp),
                    )
                }

                Spacer(Modifier.width(14.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = typeLabel,
                            color = accent,
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = 0.2.sp,
                        )
                        Spacer(Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color.White.copy(alpha = 0.07f))
                                .border(0.8.dp, SheetBorder, RoundedCornerShape(6.dp))
                                .padding(horizontal = 7.dp, vertical = 2.dp),
                        ) {
                            Text(
                                text = result.formatLabel,
                                color = Color(0xFFB5B7C0),
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.Medium,
                            )
                        }
                    }
                    Spacer(Modifier.height(3.dp))
                    Text(
                        text = result.title,
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        lineHeight = 24.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            Spacer(Modifier.height(18.dp))

            // Type-specific rich detail card
            when (result.type) {
                ScanContentType.UPI -> result.upi?.let { upi ->
                    val rows = buildList {
                        add("UPI ID" to upi.pa)
                        upi.pn?.takeIf { it.isNotBlank() }?.let { add("Payee Name" to it) }
                        upi.am?.takeIf { it.isNotBlank() }?.let { add("Amount" to "₹$it (${upi.cu ?: "INR"})") }
                        upi.tn?.takeIf { it.isNotBlank() }?.let { add("Note" to it) }
                    }
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(18.dp))
                            .background(SheetCard)
                            .border(1.dp, PaytmCyan.copy(alpha = 0.28f), RoundedCornerShape(18.dp))
                            .padding(16.dp),
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Rounded.CheckCircle,
                                contentDescription = null,
                                tint = PaytmGreen,
                                modifier = Modifier.size(17.dp),
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
                        rows.forEachIndexed { index, (label, value) ->
                            DetailRow(label, value)
                            if (index < rows.lastIndex) {
                                HorizontalDivider(
                                    color = SheetDivider,
                                    modifier = Modifier.padding(vertical = 4.dp),
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                }

                ScanContentType.WIFI -> result.wifi?.let { wifi ->
                    val rows = buildList {
                        add("Network (SSID)" to wifi.ssid.ifBlank { "Hidden" })
                        add("Security" to wifi.encryption)
                        wifi.password?.let { add("Password" to it) }
                    }
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(18.dp))
                            .background(SheetCard)
                            .border(1.dp, SheetBorder, RoundedCornerShape(18.dp))
                            .padding(16.dp),
                    ) {
                        rows.forEachIndexed { index, (label, value) ->
                            DetailRow(label, value)
                            if (index < rows.lastIndex) {
                                HorizontalDivider(
                                    color = SheetDivider,
                                    modifier = Modifier.padding(vertical = 4.dp),
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                }

                else -> Unit
            }

            // Raw scanned content card (selectable + quick copy chip)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(SheetCard)
                    .border(1.dp, SheetBorder, RoundedCornerShape(18.dp))
                    .padding(16.dp),
            ) {
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        text = "SCANNED CONTENT",
                        color = TextSecondary,
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 1.sp,
                    )
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.White.copy(alpha = 0.06f))
                            .clickable {
                                copyToClipboard(context, "Scanned Text", result.rawValue)
                            }
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Rounded.ContentCopy,
                                contentDescription = "Copy raw content",
                                tint = Color(0xFFB5B7C0),
                                modifier = Modifier.size(12.dp),
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text = "Copy",
                                color = Color(0xFFB5B7C0),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                            )
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
                SelectionContainer {
                    Text(
                        text = result.rawValue,
                        color = Color(0xFFF0F1F5),
                        fontSize = 14.sp,
                        fontFamily = if (result.type == ScanContentType.TEXT || result.type == ScanContentType.UPI) {
                            FontFamily.Monospace
                        } else {
                            FontFamily.Default
                        },
                        lineHeight = 20.sp,
                    )
                }
            }

            Spacer(Modifier.height(18.dp))

            // Primary contextual action
            when (result.type) {
                ScanContentType.UPI -> {
                    PrimaryGradientButton(
                        label = "Pay with UPI App",
                        icon = Icons.Rounded.AccountBalanceWallet,
                        gradientColors = listOf(PaytmCyan, Color(0xFF008ED6)),
                        onClick = {
                            val upiUri = result.upi?.rawUri ?: result.rawValue
                            launchSafe(context, Intent(Intent.ACTION_VIEW, Uri.parse(upiUri)), "No UPI app found on device")
                        },
                    )
                    Spacer(Modifier.height(10.dp))
                }

                ScanContentType.URL -> {
                    val targetUrl = if (result.rawValue.startsWith("http", ignoreCase = true)) {
                        result.rawValue.trim()
                    } else {
                        "https://${result.rawValue.trim()}"
                    }
                    PrimaryGradientButton(
                        label = "Open Link",
                        icon = Icons.AutoMirrored.Rounded.OpenInNew,
                        gradientColors = listOf(Color(0xFF0A9CE8), Color(0xFF0077CC)),
                        onClick = {
                            launchSafe(context, Intent(Intent.ACTION_VIEW, Uri.parse(targetUrl)), "No browser found")
                        },
                    )
                    Spacer(Modifier.height(10.dp))
                }

                ScanContentType.WIFI -> {
                    val wifiPwd = result.wifi?.password
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        if (!wifiPwd.isNullOrEmpty()) {
                            Button(
                                onClick = { copyToClipboard(context, "Wi-Fi Password", wifiPwd) },
                                colors = ButtonDefaults.buttonColors(containerColor = XiaomiBlue, contentColor = Color.White),
                                shape = RoundedCornerShape(14.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .heightIn(min = 48.dp),
                            ) {
                                Icon(Icons.Rounded.ContentCopy, contentDescription = null, modifier = Modifier.size(17.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Copy Password", fontWeight = FontWeight.SemiBold, maxLines = 1, softWrap = false)
                            }
                        }
                        SecondaryActionButton(
                            label = "Wi-Fi Settings",
                            icon = Icons.Rounded.Wifi,
                            containerColor = TonalButtonBg,
                            borderColor = SheetBorder,
                            onClick = {
                                launchSafe(context, Intent(Settings.ACTION_WIFI_SETTINGS), "Unable to open Wi-Fi settings")
                            },
                            modifier = Modifier.weight(1f),
                        )
                    }
                    Spacer(Modifier.height(10.dp))
                }

                ScanContentType.PHONE -> {
                    PrimaryGradientButton(
                        label = "Call Number",
                        icon = Icons.Rounded.Call,
                        gradientColors = listOf(PaytmGreen, Color(0xFF00A846)),
                        onClick = {
                            launchSafe(context, Intent(Intent.ACTION_DIAL, Uri.parse(result.rawValue.trim())), "No dialer found")
                        },
                    )
                    Spacer(Modifier.height(10.dp))
                }

                ScanContentType.EMAIL -> {
                    PrimaryGradientButton(
                        label = "Send Email",
                        icon = Icons.Rounded.Email,
                        gradientColors = listOf(Color(0xFF0A9CE8), Color(0xFF0077CC)),
                        onClick = {
                            val mailUri = if (result.rawValue.startsWith("mailto:", ignoreCase = true)) {
                                result.rawValue.trim()
                            } else {
                                "mailto:${result.title}"
                            }
                            launchSafe(context, Intent(Intent.ACTION_SENDTO, Uri.parse(mailUri)), "No email app found")
                        },
                    )
                    Spacer(Modifier.height(10.dp))
                }

                ScanContentType.GEO -> {
                    PrimaryGradientButton(
                        label = "Open in Maps",
                        icon = Icons.Rounded.LocationOn,
                        gradientColors = listOf(PaytmGreen, Color(0xFF00A846)),
                        onClick = {
                            launchSafe(context, Intent(Intent.ACTION_VIEW, Uri.parse(result.rawValue.trim())), "No maps app found")
                        },
                    )
                    Spacer(Modifier.height(10.dp))
                }

                ScanContentType.PRODUCT_BARCODE -> {
                    PrimaryGradientButton(
                        label = "Search Product Online",
                        icon = Icons.Rounded.Language,
                        gradientColors = listOf(Color(0xFF0A9CE8), Color(0xFF0077CC)),
                        onClick = {
                            val url = "https://www.google.com/search?q=${Uri.encode(result.rawValue.trim())}"
                            launchSafe(context, Intent(Intent.ACTION_VIEW, Uri.parse(url)), "No browser found")
                        },
                    )
                    Spacer(Modifier.height(10.dp))
                }

                else -> Unit
            }

            // Secondary row: Copy + Share + Next
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                SecondaryActionButton(
                    label = if (result.upi != null) "Copy UPI" else "Copy",
                    icon = Icons.Rounded.ContentCopy,
                    containerColor = TonalButtonBg,
                    borderColor = SheetBorder,
                    onClick = {
                        copyToClipboard(
                            context,
                            if (result.upi != null) "UPI String" else "Scanned Text",
                            result.upi?.pa ?: result.rawValue,
                        )
                    },
                    modifier = Modifier.weight(1f),
                )

                SecondaryActionButton(
                    label = "Share",
                    icon = Icons.Rounded.Share,
                    containerColor = TonalButtonBg,
                    borderColor = SheetBorder,
                    onClick = {
                        val send = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, result.rawValue)
                        }
                        launchSafe(context, Intent.createChooser(send, "Share scanned result"), "Unable to share")
                    },
                    modifier = Modifier.weight(1f),
                )

                SecondaryActionButton(
                    label = "Next",
                    icon = Icons.Rounded.QrCodeScanner,
                    containerColor = ElevatedButtonBg,
                    borderColor = Color.White.copy(alpha = 0.13f),
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun PrimaryGradientButton(
    label: String,
    icon: ImageVector,
    gradientColors: List<Color>,
    onClick: () -> Unit,
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Brush.horizontalGradient(gradientColors))
            .clickable(onClick = onClick),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(19.dp),
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = label,
                color = Color.White,
                fontSize = 15.5.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.1.sp,
            )
        }
    }
}

@Composable
private fun SecondaryActionButton(
    label: String,
    icon: ImageVector,
    containerColor: Color,
    borderColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .heightIn(min = 48.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(containerColor)
            .border(1.dp, borderColor, RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 10.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color(0xFFE8E9EE),
                modifier = Modifier.size(16.dp),
            )
            Spacer(Modifier.width(5.dp))
            Text(
                text = label,
                color = Color(0xFFF2F3F7),
                fontSize = 13.5.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                softWrap = false,
            )
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
