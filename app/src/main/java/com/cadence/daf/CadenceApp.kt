package com.cadence.daf

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntOffset
import androidx.lifecycle.viewmodel.compose.viewModel

internal const val MAIN = "main"
internal const val SETTINGS = "settings"
internal const val PROGRESS = "progress"
private const val DEBUG = "debug"

/** Every screen except Main sits one level deeper: it slides in from the right and back out to it. */
internal fun slidesForward(target: String) = target != MAIN

@Composable
fun CadenceApp() {
    var screen by rememberSaveable { mutableStateOf(MAIN) }
    val viewModel: MainViewModel = viewModel()
    BackHandler(enabled = screen != MAIN) { screen = MAIN }
    val state by viewModel.uiState.collectAsState()
    if (!state.settings.disclaimerAccepted) {
        AlertDialog(
            onDismissRequest = {},
            shape = MaterialTheme.shapes.extraLarge,
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            text = { Text(stringResource(R.string.disclaimer_text)) },
            confirmButton = {
                TextButton(onClick = viewModel::acceptDisclaimer) { Text(stringResource(R.string.disclaimer_ok)) }
            }
        )
    }
    val slide = motionTween<IntOffset>(250)
    val fade = motionTween<Float>(250)
    val quickFade = motionTween<Float>(150)
    AnimatedContent(
        targetState = screen,
        transitionSpec = {
            val dir = if (slidesForward(targetState)) 1 else -1
            (slideInHorizontally(slide) { it / 4 * dir } + fadeIn(fade)) togetherWith
                (slideOutHorizontally(slide) { -it / 4 * dir } + fadeOut(quickFade))
        },
        label = "screen"
    ) { target ->
        when (target) {
            SETTINGS -> SettingsScreen(viewModel, onBack = { screen = MAIN }, openDebug = { screen = DEBUG })
            PROGRESS -> ProgressScreen(viewModel, onBack = { screen = MAIN })
            DEBUG -> DebugScreen()
            else -> MainScreen(viewModel, openSettings = { screen = SETTINGS }, openProgress = { screen = PROGRESS })
        }
    }
}
