package com.arunrk.simplenote

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge

/**
 * The Android entry point.
 *
 * Everything the user sees lives in the shared module; this class exists only to host it.
 * Koin is started in [NoteApplication], not here, so a rotation does not restart it.
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        setContent {
            App()
        }
    }
}
