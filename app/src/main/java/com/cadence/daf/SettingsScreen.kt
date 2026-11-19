package com.cadence.daf

import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import java.time.LocalDate
import kotlin.math.roundToInt

/** Three groups (DESIGN.md section 36): background noise, sound, practice routine; then About. */
@Composable
fun SettingsScreen(viewModel: MainViewModel, onBack: () -> Unit, openDebug: () -> Unit) {
    val state by viewModel.uiState.collectAsState()
    val settings = state.settings
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current
    val version = remember { context.packageManager.getPackageInfo(context.packageName, 0).versionName.orEmpty() }
    var versionTaps by remember { mutableIntStateOf(0) }
    ScreenScaffold {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
            }
            Text(stringResource(R.string.settings), style = MaterialTheme.typography.headlineMedium)
        }

        GroupTitle(R.string.group_noise)
        SectionCard {
            SwitchRow(
                R.string.noise_reduction_switch, R.string.noise_reduction_caption,
                settings.noiseReduction, viewModel::setNoiseReduction
            )
        }
        SectionCard {
            SwitchRow(R.string.gate_switch, R.string.gate_caption, settings.gateEnabled, viewModel::setGateEnabled)
            if (settings.gateEnabled) SensitivitySection(settings.sensitivity, state.running, viewModel)
        }

        GroupTitle(R.string.group_sound)
        SectionCard { MicSection(state) { viewModel.setMicPreset(it) } }
        SectionCard { MaskingSection(settings, viewModel) }

        GroupTitle(R.string.group_practice)
        SectionCard { FadeSection(settings, viewModel) }
        SectionCard {
            Text(stringResource(R.string.session_limit), style = MaterialTheme.typography.titleMedium)
            Caption(R.string.session_limit_caption)
            MinuteChips(SESSION_LIMIT_CHOICES, settings.sessionLimitMin, viewModel::setSessionLimitMin)
        }
        SectionCard { ReminderSection(settings, viewModel) }

        GroupTitle(R.string.about)
        SectionCard {
            Text(stringResource(R.string.about_text))
            Text(stringResource(R.string.about_disclaimer), style = MaterialTheme.typography.bodySmall)
            TextButton(onClick = { runCatching { uriHandler.openUri("https://cadence-daf.github.io/") } }) {
                Text(stringResource(R.string.check_updates))
            }
            Text(
                stringResource(R.string.version, version),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.defaultMinSize(minHeight = 48.dp).clickable {
                    versionTaps++
                    if (versionTaps == 7) {
                        versionTaps = 0
                        openDebug()
                    }
                }
            )
        }
    }
}

/** Small heading above a group of cards. */
@Composable
private fun GroupTitle(@StringRes id: Int) {
    Text(
        stringResource(id),
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 8.dp).semantics { heading() }
    )
}

/** Title, caption and a switch; the whole row toggles. */
@Composable
private fun SwitchRow(@StringRes title: Int, @StringRes caption: Int, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 56.dp)
            .toggleable(value = checked, role = Role.Switch, onValueChange = onChange),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(stringResource(title))
            Caption(caption)
        }
        Switch(checked = checked, onCheckedChange = null)
    }
}

/** Gate sensitivity and the room-noise button; shown only while the gate is on. */
@Composable
private fun SensitivitySection(sensitivity: Int, running: Boolean, viewModel: MainViewModel) {
    val calibrating by viewModel.calibrating.collectAsState()
    val label = stringResource(R.string.sensitivity_label, sensitivity)
    Text(label)
    CadenceSlider(
        value = sensitivity.toFloat(),
        onValueChange = { viewModel.setSensitivity(it.roundToInt()) },
        valueRange = 0f..10f,
        steps = 9,
        modifier = Modifier.semantics { contentDescription = label }
    )
    Caption(R.string.sensitivity_caption)
    TextButton(onClick = viewModel::calibrateSensitivity, enabled = running && !calibrating) {
        Text(stringResource(if (calibrating) R.string.calibrating else R.string.calibrate))
    }
    Caption(R.string.calibrate_caption)
}

@Composable
private fun MicSection(state: UiState, onSelect: (Int) -> Unit) {
    Text(stringResource(R.string.microphone_mode), style = MaterialTheme.typography.titleMedium)
    MicPreset.entries.forEach { option ->
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = 56.dp)
                .selectable(
                    selected = state.settings.micPreset == option.index,
                    enabled = !state.running,
                    role = Role.RadioButton,
                    onClick = { onSelect(option.index) }
                ),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RadioButton(
                selected = state.settings.micPreset == option.index,
                onClick = null,
                enabled = !state.running
            )
            Column {
                Text(option.label)
                Caption(option.captionRes())
            }
        }
    }
    if (state.running) {
        Text(stringResource(R.string.mic_locked))
    }
}

/** The volume slider shows only while masking is on. */
@Composable
private fun MaskingSection(settings: Settings, viewModel: MainViewModel) {
    SwitchRow(R.string.masking_switch, R.string.masking_caption, settings.maskingEnabled, viewModel::setMaskingEnabled)
    if (!settings.maskingEnabled) return
    val label = stringResource(R.string.masking_label, settings.maskingDb)
    Text(label)
    CadenceSlider(
        value = settings.maskingDb.toFloat(),
        onValueChange = { viewModel.setMaskingDb(it.roundToInt()) },
        valueRange = -40f..-10f,
        steps = 29,
        modifier = Modifier.semantics { contentDescription = label }
    )
}

/** The two sliders and this week's delay show only while the program is on. */
@Composable
private fun FadeSection(settings: Settings, viewModel: MainViewModel) {
    SwitchRow(R.string.fade_switch, R.string.fade_caption, settings.fadeEnabled, viewModel::setFadeEnabled)
    if (!settings.fadeEnabled) return
    val now = settings.programDelay(LocalDate.now().toEpochDay())
    val startLabel = stringResource(R.string.fade_start_label, settings.fadeStartMs)
    Text(startLabel)
    CadenceSlider(
        value = settings.fadeStartMs.toFloat(),
        onValueChange = { viewModel.setFadeStartMs(it.roundToInt()) },
        valueRange = 40f..200f,
        steps = 15,
        modifier = Modifier.semantics { contentDescription = startLabel }
    )
    val targetLabel = stringResource(R.string.fade_target_label, settings.fadeTargetMs)
    Text(targetLabel)
    CadenceSlider(
        value = settings.fadeTargetMs.toFloat(),
        onValueChange = { viewModel.setFadeTargetMs(it.roundToInt()) },
        valueRange = 40f..200f,
        steps = 15,
        modifier = Modifier.semantics { contentDescription = targetLabel }
    )
    val stepLabel = stringResource(R.string.fade_step_label, settings.fadeStepMs)
    Text(stepLabel)
    CadenceSlider(
        value = settings.fadeStepMs.toFloat(),
        onValueChange = { viewModel.setFadeStepMs(it.roundToInt()) },
        valueRange = 0f..50f,
        steps = 9,
        modifier = Modifier.semantics { contentDescription = stepLabel }
    )
    val intervalLabel = stringResource(R.string.fade_interval_label, settings.fadeIntervalDays)
    Text(intervalLabel)
    CadenceSlider(
        value = settings.fadeIntervalDays.toFloat(),
        onValueChange = { viewModel.setFadeIntervalDays(it.roundToInt()) },
        valueRange = 1f..14f,
        steps = 12,
        modifier = Modifier.semantics { contentDescription = intervalLabel }
    )
    Text(stringResource(R.string.fade_now, now))
    val paused = settings.fadePauseDay >= 0
    TextButton(onClick = { viewModel.setFadePaused(!paused) }) {
        Text(stringResource(if (paused) R.string.fade_resume else R.string.fade_pause))
    }
    TextButton(onClick = viewModel::stepFadeBack) { Text(stringResource(R.string.fade_back)) }
}

private fun MicPreset.captionRes(): Int = when (this) {
    MicPreset.VOICE_RECOGNITION -> R.string.caption_voice_recognition
    MicPreset.UNPROCESSED -> R.string.caption_unprocessed
}
