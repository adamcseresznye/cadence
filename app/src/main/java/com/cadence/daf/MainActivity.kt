package com.cadence.daf

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import kotlinx.coroutines.flow.MutableStateFlow

internal const val ACTION_START_PRACTICE = "com.cadence.daf.action.START_PRACTICE"

/** True after the "Start practice" launcher shortcut was used; Main starts practice and clears it. */
internal val startRequested = MutableStateFlow(false)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null) noteStartRequest(intent)
        setContent {
            CadenceTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    CadenceApp()
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        noteStartRequest(intent)
    }

    // A request that could not start (no headphones) must not fire later by surprise.
    override fun onStop() {
        super.onStop()
        startRequested.value = false
    }

    private fun noteStartRequest(intent: Intent?) {
        if (intent?.action == ACTION_START_PRACTICE) startRequested.value = true
    }
}
