package com.arunrk.simplenote

import androidx.compose.ui.window.ComposeUIViewController
import com.arunrk.simplenote.di.initKoin

/**
 * Tracks whether Koin has been started for this process.
 *
 * SwiftUI can create the view controller more than once — on a rotation, or when the hosting
 * view is rebuilt — and `startKoin` throws if it is already running. A plain flag is used
 * rather than querying Koin's global context, which is not part of its multiplatform API.
 *
 * Safe as top-level mutable state because Compose creates the view controller on the main
 * thread, which is the only place this is read or written.
 */
private var isKoinStarted = false

/** The iOS entry point, called from `ContentView.swift`. */
fun MainViewController() = ComposeUIViewController {
    if (!isKoinStarted) {
        isKoinStarted = true
        initKoin()
    }
    App()
}
