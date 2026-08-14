package com.arunrk.simplenote

import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.arunrk.simplenote.di.initKoin

/**
 * The desktop entry point.
 *
 * The default window opens wider than the 840dp two-pane breakpoint so the list-detail layout
 * is what a desktop user sees first; narrowing the window switches to a single pane live.
 *
 * Override the backend with `initKoin(baseUrl = "http://192.168.1.20:8080")` when the server
 * is on another machine.
 */
fun main() {
    initKoin()

    application {
        Window(
            onCloseRequest = ::exitApplication,
            title = "Simple Note",
            state = rememberWindowState(size = DpSize(1100.dp, 760.dp)),
        ) {
            App()
        }
    }
}
