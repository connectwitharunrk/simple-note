package com.arunrk.simplenote

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.arunrk.simplenote.presentation.home.HomeScreen
import com.arunrk.simplenote.presentation.theme.AppTheme
import org.koin.compose.KoinContext

/**
 * The whole app, shared by Android, iOS and desktop.
 *
 * Each platform's entry point calls `initKoin()` first and then renders this — so the only
 * platform-specific code is the few lines that create a window, an activity, or a view
 * controller.
 *
 * [KoinContext] makes the started Koin instance available to `koinViewModel()` further down
 * the tree.
 */
@Composable
@Preview
fun App() {
    KoinContext {
        AppTheme {
            HomeScreen()
        }
    }
}
