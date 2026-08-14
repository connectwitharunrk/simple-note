package com.arunrk.simplenote

import android.app.Application
import com.arunrk.simplenote.di.initKoin

/**
 * Starts Koin once per process.
 *
 * Deliberately here rather than in `MainActivity`: an activity is recreated on every rotation
 * and configuration change, and calling `startKoin` a second time throws.
 *
 * No `androidContext(...)` is registered because nothing in the graph needs one — the client
 * is pure Kotlin plus Ktor — which keeps the `koin-android` dependency out of the project.
 */
class NoteApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        initKoin()
    }
}
