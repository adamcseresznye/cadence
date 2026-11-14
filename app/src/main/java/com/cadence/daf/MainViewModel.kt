package com.cadence.daf

import android.app.Application
import android.os.SystemClock
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

data class UiState(
    val settings: Settings,
    val headset: HeadsetState,
    val running: Boolean,
    val error: String?,
    val telemetry: Telemetry,
    val practice: PracticeStats = PracticeStats(0, 0, 0, 0, 0, 0),
    val last30Days: List<DayTotal> = emptyList(),
)

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = SettingsRepository(application)
    private val headsetMonitor = HeadsetMonitor(application)
    private val telemetry = MutableStateFlow(Telemetry())
    private val practiceLog = PracticeLogStore(application)
    private val practice = MutableStateFlow(PracticeStats(0, 0, 0, 0, 0, 0) to emptyList<DayTotal>())
    private val sessionEvents = Channel<Int>(Channel.BUFFERED)

    /** Minutes practised today, once per logged session; the screen shows the Snackbar. */
    val sessionSummary: Flow<Int> = sessionEvents.receiveAsFlow()

    val uiState: StateFlow<UiState> = combine(
        repository.settings,
        headsetMonitor.state,
        AudioService.running,
        AudioService.lastError,
        combine(telemetry, practice) { latest, days -> latest to days },
    ) { settings, headset, running, error, (latest, days) ->
        UiState(settings, headset, running, error, latest, days.first, days.second)
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        UiState(Settings(disclaimerAccepted = true), HeadsetState(), running = false, error = null, telemetry = Telemetry()),
    )

    init {
        viewModelScope.launch { repository.applyFadeProgram() }
        viewModelScope.launch {
            repository.settings.map { it.reminderEnabled to it.reminderMinute }.distinctUntilChanged()
                .collect { (enabled, minute) -> Reminders.schedule(application, enabled, minute) }
        }
        viewModelScope.launch {
            // Refreshes on start/stop and every 15 s, so the day rolls over at midnight.
            AudioService.running.collectLatest {
                while (true) {
                    refreshPractice()
                    delay(15_000)
                }
            }
        }
        viewModelScope.launch {
            AudioService.sessionLogged.collect {
                refreshPractice()
                sessionEvents.send(practice.value.first.todayMin)
            }
        }
        viewModelScope.launch {
            repository.settings.collect { settings ->
                NativeEngine.nativeSetDelayMs(settings.delayMs)
                NativeEngine.nativeSetSensitivity(settings.sensitivity)
                NativeEngine.nativeSetGateEnabled(settings.gateEnabled)
                NativeEngine.nativeSetPitchSemitones(settings.pitchSemitones)
                NativeEngine.nativeSetOutputGainDb(settings.outputGainDb)
            }
        }
        viewModelScope.launch {
            var peakDb = -90f
            var heardSpeech = false
            while (true) {
                if (AudioService.running.value) {
                    val raw = NativeEngine.nativeGetLatencyMs()
                    val inputDb = NativeEngine.nativeGetInputRmsDb()
                    val speech = NativeEngine.nativeGetSpeechDetected()
                    peakDb = maxOf(peakDb, inputDb)
                    heardSpeech = heardSpeech || speech
                    val elapsedMs = SystemClock.elapsedRealtime() - AudioService.startedAtMs.value
                    telemetry.value = Telemetry(
                        latencyMs = if (raw >= 0f) smoothLatency(telemetry.value.latencyMs, raw) else null,
                        inputDb = inputDb,
                        floorDb = NativeEngine.nativeGetFloorDb(),
                        gateOpen = NativeEngine.nativeGetGateOpen(),
                        speechDetected = speech,
                        elapsedSec = (elapsedMs / 1000).toInt(),
                        quietMic = quietMic(elapsedMs, peakDb, heardSpeech),
                    )
                    delay(100)
                } else {
                    peakDb = -90f
                    heardSpeech = false
                    telemetry.value = Telemetry()
                    delay(250)
                }
            }
        }
    }

    private suspend fun refreshPractice() {
        val lines = practiceLog.readLines()
        val today = LocalDate.now()
        practice.value = practiceStats(lines, today) to dailyMinutes(lines, today, 30)
    }

    fun setDailyGoalMin(min: Int) {
        viewModelScope.launch { repository.setDailyGoalMin(min) }
    }

    fun clearPracticeHistory() {
        viewModelScope.launch {
            practiceLog.clear()
            refreshPractice()
        }
    }

    fun setReminderEnabled(enabled: Boolean) {
        viewModelScope.launch { repository.setReminderEnabled(enabled) }
    }

    fun setReminderMinute(minute: Int) {
        viewModelScope.launch { repository.setReminderMinute(minute) }
    }

    fun setDelayMs(ms: Int) {
        viewModelScope.launch { repository.setDelayMs(ms) }
    }

    fun setSensitivity(level: Int) {
        viewModelScope.launch { repository.setSensitivity(level) }
    }

    fun setGateEnabled(enabled: Boolean) {
        viewModelScope.launch { repository.setGateEnabled(enabled) }
    }

    fun setPitchSemitones(semitones: Int) {
        viewModelScope.launch { repository.setPitchSemitones(semitones) }
    }

    fun setOutputGainDb(db: Int) {
        viewModelScope.launch { repository.setOutputGainDb(db) }
    }

    fun acceptDisclaimer() {
        viewModelScope.launch { repository.setDisclaimerAccepted() }
    }

    fun setFadeEnabled(enabled: Boolean) = fade { repository.setFadeEnabled(enabled) }

    fun setFadeStartMs(ms: Int) = fade { repository.setFadeStartMs(ms) }

    fun setFadeStepMs(ms: Int) = fade { repository.setFadeStepMs(ms) }

    private fun fade(change: suspend () -> Unit) {
        viewModelScope.launch {
            change()
            repository.applyFadeProgram()
        }
    }

    fun setMicPreset(index: Int) {
        viewModelScope.launch { repository.setMicPreset(index) }
    }

    fun start() {
        val headset = headsetMonitor.state.value
        val outputId = headset.wiredOutputId ?: return
        viewModelScope.launch {
            val preset = repository.settings.first().micPreset
            AudioService.start(
                getApplication(), preset, headset.headsetInputId ?: 0, outputId, headset.headsetInputIsBluetooth
            )
        }
    }

    fun stop() {
        AudioService.stop(getApplication())
    }

    override fun onCleared() {
        headsetMonitor.close()
        super.onCleared()
    }
}
