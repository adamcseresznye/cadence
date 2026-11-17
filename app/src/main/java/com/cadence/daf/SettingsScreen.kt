package com.cadence.daf

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import java.time.LocalDate
import kotlin.math.roundToInt

@Composable
fun SettingsScreen(viewModel: MainViewModel, onBack: () -> Unit, openDebug: () -> Unit) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val version = remember { context.packageManager.getPackageInfo(context.packageName, 0).versionName.orEmpty() }
    var versionTaps by remember { mutableIntStateOf(0) }
    ScreenScaffold {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
            }
            Text(stringResource(R.string.settings), style = MaterialTheme.typography.headlineMedium)
        }

        SectionCard { MicSection(state) { viewModel.setMicPreset(it) } }
        SectionCard { GateRow(state.settings.gateEnabled, viewModel::setGateEnabled) }
        SectionCard { FadeSection(state.settings, viewModel) }
        SectionCard {
            Text(stringResource(R.string.session_limit), style = MaterialTheme.typography.titleMedium)
            Caption(R.string.session_limit_caption)
            MinuteChips(SESSION_LIMIT_CHOICES, state.settings.sessionLimitMin, viewModel::setSessionLimitMin)
        }
        SectionCard { ReminderSection(state.settings, viewModel) }
        SectionCard {
            Text(stringResource(R.string.about), style = MaterialTheme.typography.titleMedium)
            Text(stringResource(R.string.about_text))
            Text(stringResource(R.string.disclaimer_text), style = MaterialTheme.typography.bodySmall)
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

@Composable
private fun GateRow(enabled: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 56.dp)
            .toggleable(value = enabled, role = Role.Switch, onValueChange = onChange),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(stringResource(R.string.gate_switch))
            Caption(R.string.gate_caption)
        }
        Switch(checked = enabled, onCheckedChange = null)
    }
}


@Composable
private fun FadeSection(settings: Settings, viewModel: MainViewModel) {
    val thisWeek = programDelayMs(settings.fadeStartMs, settings.fadeStepMs, settings.fadeStartDay, LocalDate.now().toEpochDay())
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 56.dp)
            .toggleable(value = settings.fadeEnabled, role = Role.Switch, onValueChange = viewModel::setFadeEnabled),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(stringResource(R.string.fade_switch), modifier = Modifier.weight(1f))
        Switch(checked = settings.fadeEnabled, onCheckedChange = null)
    }
    Caption(R.string.fade_caption)
    val startLabel = stringResource(R.string.fade_start_label, settings.fadeStartMs)
    Text(startLabel)
    CadenceSlider(
        value = settings.fadeStartMs.toFloat(),
        onValueChange = { viewModel.setFadeStartMs(it.roundToInt()) },
        valueRange = 40f..200f,
        steps = 15,
        modifier = Modifier.semantics { contentDescription = startLabel }
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
    Text(stringResource(R.string.fade_this_week, thisWeek))
}

private fun MicPreset.captionRes(): Int = when (this) {
    MicPreset.VOICE_RECOGNITION -> R.string.caption_voice_recognition
    MicPreset.UNPROCESSED -> R.string.caption_unprocessed
}
