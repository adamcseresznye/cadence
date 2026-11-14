package com.cadence.daf

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import kotlinx.coroutines.delay
import java.util.Locale
import kotlin.math.roundToInt

@Composable
fun DebugScreen() {
    val context = LocalContext.current
    var permissionGranted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { permissionGranted = it }

    var preset by remember { mutableStateOf(MicPreset.VOICE_RECOGNITION) }
    var delayMs by remember { mutableFloatStateOf(100f) }
    val running by AudioService.running.collectAsState()
    val lastError by AudioService.lastError.collectAsState()
    var inputLevel by remember { mutableFloatStateOf(-90f) }
    var underruns by remember { mutableIntStateOf(0) }
    var sampleRate by remember { mutableIntStateOf(0) }
    var framesPerBurst by remember { mutableIntStateOf(0) }
    var sensitivity by remember { mutableFloatStateOf(5f) }
    var gateOn by remember { mutableStateOf(true) }
    var floorDb by remember { mutableFloatStateOf(-60f) }
    var gateOpen by remember { mutableStateOf(false) }
    var speechDetected by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf("") }
    val headsetMonitor = remember { HeadsetMonitor(context) }
    DisposableEffect(headsetMonitor) {
        onDispose { headsetMonitor.close() }
    }
    val headset by headsetMonitor.state.collectAsState()

    LaunchedEffect(headset.canStart) {
        if (running && !headset.canStart) {
            AudioService.stop(context)
            message = "Earbuds unplugged. Stopped."
        }
    }

    LaunchedEffect(running) {
        while (running) {
            inputLevel = NativeEngine.nativeGetInputRmsDb()
            underruns = NativeEngine.nativeGetUnderruns()
            sampleRate = NativeEngine.nativeGetSampleRate()
            framesPerBurst = NativeEngine.nativeGetFramesPerBurst()
            floorDb = NativeEngine.nativeGetFloorDb()
            gateOpen = NativeEngine.nativeGetGateOpen()
            speechDetected = NativeEngine.nativeGetSpeechDetected()
            delay(100)
        }
    }

    val startAudio: () -> Unit = {
        NativeEngine.nativeSetDelayMs(delayMs.roundToInt())
        NativeEngine.nativeSetSensitivity(sensitivity.roundToInt())
        NativeEngine.nativeSetGateEnabled(gateOn)
        AudioService.start(
            context,
            preset.index,
            headset.headsetInputId ?: 0,
            checkNotNull(headset.wiredOutputId),
            headset.headsetInputIsBluetooth
        )
        message = ""
    }
    val notificationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { startAudio() }

    val onButtonClick: () -> Unit = {
        when {
            !permissionGranted -> permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            running -> AudioService.stop(context)
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
                PackageManager.PERMISSION_GRANTED ->
                notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            else -> startAudio()
        }
    }

    val buttonLabel = when {
        !permissionGranted -> "Allow microphone"
        running -> "Stop"
        else -> "Start"
    }
    val stateName = if (running) "Running" else "Idle"

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Cadence debug", style = MaterialTheme.typography.headlineMedium)
        Text("Use wired earbuds. Never use the phone speaker: it will howl. Keep the volume low.")
        Text(
            if (headset.canStart) "Headset: ${headset.name}"
            else "Headset: none. Plug in wired earbuds to start."
        )
        if (headset.canStart && headset.isBluetooth) {
            Text("Bluetooth headphones: test only. Latency is higher, not the best. Use wired earbuds for practice.")
        }
        if (headset.canStart && !headset.hasHeadsetMic) {
            Text("No headset microphone found. The phone microphone will be used.")
        }

        MicPreset.entries.forEach { option ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .selectable(
                        selected = preset == option,
                        enabled = !running,
                        role = Role.RadioButton,
                        onClick = { preset = option }
                    ),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(selected = preset == option, onClick = null, enabled = !running)
                Text(option.label)
            }
        }

        Button(
            onClick = onButtonClick,
            enabled = headset.canStart,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 72.dp)
        ) {
            Text(buttonLabel)
        }

        Text("Delay: ${delayMs.roundToInt()} ms")
        CadenceSlider(
            value = delayMs,
            onValueChange = {
                delayMs = it
                NativeEngine.nativeSetDelayMs(it.roundToInt())
            },
            valueRange = 40f..200f,
            steps = 15
        )

        Text("Sensitivity: ${sensitivity.roundToInt()}")
        CadenceSlider(
            value = sensitivity,
            onValueChange = {
                sensitivity = it
                NativeEngine.nativeSetSensitivity(it.roundToInt())
            },
            valueRange = 0f..10f,
            steps = 9
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Gate on")
            Switch(
                checked = gateOn,
                onCheckedChange = {
                    gateOn = it
                    NativeEngine.nativeSetGateEnabled(it)
                }
            )
        }

        Text("State: $stateName")
        Text("Input level: ${String.format(Locale.US, "%.1f", inputLevel)} dB")
        Text("Noise floor: ${String.format(Locale.US, "%.1f", floorDb)} dB")
        Text(if (gateOpen) "Gate: open" else "Gate: closed")
        Text(if (speechDetected) "Voice detected: yes" else "Voice detected: no")
        Text("Underruns: $underruns")
        Text("Sample rate: $sampleRate Hz, frames per burst: $framesPerBurst")
        Text(lastError ?: message)
    }
}
