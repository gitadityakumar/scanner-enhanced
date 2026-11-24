package com.example.scanner

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import com.example.scanner.scanner.ScannerPreferences
import com.example.scanner.ui.history.HistoryScreen
import com.example.scanner.ui.scanner.ScannerScreen
import com.example.scanner.ui.settings.SettingsScreen

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
