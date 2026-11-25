package com.adi100x.scanner

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import com.adi100x.scanner.scanner.ScannerPreferences
import com.adi100x.scanner.ui.history.HistoryScreen
import com.adi100x.scanner.ui.scanner.ScannerScreen
import com.adi100x.scanner.ui.settings.SettingsScreen

@Composable
fun MainNavigation() {
    val context = LocalContext.current
    val preferences = remember(context) { ScannerPreferences(context.applicationContext) }
    val backStack = rememberNavBackStack(Main)

    NavDisplay(
        backStack = backStack,
        onBack = { backStack.removeLastOrNull() },
        entryProvider = entryProvider {
            entry<Main> {
                ScannerScreen(
                    preferences = preferences,
                    onOpenSettings = { backStack.add(Settings) },
                )
            }
            entry<Settings> {
                SettingsScreen(
                    preferences = preferences,
                    onBack = { backStack.removeLastOrNull() },
                    onOpenHistory = { backStack.add(History) },
                )
            }
            entry<History> {
                HistoryScreen(
                    preferences = preferences,
                    onBack = { backStack.removeLastOrNull() },
                )
            }
        },
    )
}
