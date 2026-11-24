package com.example.scanner.ui.history

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.QrCode2
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material3.Icon
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.scanner.scanner.ParsedScanResult
import com.example.scanner.scanner.ScanContentType
import com.example.scanner.scanner.ScannerPreferences
import com.example.scanner.theme.MiuiCardDark
import com.example.scanner.theme.PaytmCyan
import com.example.scanner.theme.PureBlack
import com.example.scanner.theme.TextSecondary
import com.example.scanner.theme.XiaomiBlue
import com.example.scanner.ui.scanner.ScanResultSheet
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HistoryScreen(
    preferences: ScannerPreferences,
    onBack: () -> Unit,
) {
    val history by preferences.history.collectAsState()
    var selectedItem by remember { mutableStateOf<ParsedScanResult?>(null) }
    val timeFormat = remember { SimpleDateFormat("MMM d, HH:mm", Locale.getDefault()) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PureBlack)
            .statusBarsPadding()
            .navigationBarsPadding(),
    ) {
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
                text = "Scan History",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.align(Alignment.Center),
            )

            if (history.isNotEmpty()) {
                TextButton(
                    onClick = { preferences.clearHistory() },
                    modifier = Modifier.align(Alignment.CenterEnd),
                ) {
                    Text("Clear", color = XiaomiBlue, fontSize = 14.sp)
                }
            }
        }

        if (history.isEmpty()) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.fillMaxSize(),
            ) {
                Text(
                    text = "No scanned codes yet",
                    color = TextSecondary,
                    fontSize = 15.sp,
                )
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
            ) {
                items(history, key = { "${it.timestampMs}_${it.rawValue.hashCode()}" }) { item ->
                    val (icon, tint) = when (item.type) {
                        ScanContentType.UPI -> Icons.Rounded.AccountBalanceWallet to PaytmCyan
                        ScanContentType.URL -> Icons.Rounded.Language to XiaomiBlue
                        ScanContentType.WIFI -> Icons.Rounded.Wifi to XiaomiBlue
                        else -> Icons.Rounded.QrCode2 to XiaomiBlue
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(MiuiCardDark)
                            .clickable { selectedItem = item }
                            .padding(horizontal = 14.dp, vertical = 14.dp),
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(tint.copy(alpha = 0.16f)),
                        ) {
                            Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(20.dp))
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = item.title,
                                color = Color.White,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Medium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Spacer(Modifier.height(2.dp))
                            Text(
                                text = "${item.formatLabel} • ${timeFormat.format(Date(item.timestampMs))}",
                                color = TextSecondary,
                                fontSize = 12.sp,
                            )
                        }
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .clickable { preferences.removeHistoryItem(item.timestampMs) },
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.DeleteOutline,
                                contentDescription = "Delete",
                                tint = TextSecondary,
                                modifier = Modifier.size(19.dp),
                            )
                        }
                    }
                }
            }
        }
    }

    selectedItem?.let { item ->
        ScanResultSheet(
            result = item,
            onDismiss = { selectedItem = null },
        )
    }
}
