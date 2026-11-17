package com.cadence.daf

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.time.LocalDate

private val Context.settingsStore: DataStore<Preferences> by preferencesDataStore(name = "cadence_settings")

class SettingsRepository(context: Context) {
    private val store = context.settingsStore
    private val defaults = Settings()

    val settings: Flow<Settings> = store.data.map { prefs ->
        Settings(
            delayMs = clampDelayMs(prefs[DELAY_MS] ?: defaults.delayMs),
            micPreset = clampMicPreset(prefs[MIC_PRESET] ?: defaults.micPreset),
            gateEnabled = prefs[GATE_ENABLED] ?: defaults.gateEnabled,
            sensitivity = clampSensitivity(prefs[SENSITIVITY] ?: defaults.sensitivity),
            pitchSemitones = clampPitchSemitones(prefs[PITCH_SEMITONES] ?: defaults.pitchSemitones),
            outputGainDb = clampOutputGainDb(prefs[OUTPUT_GAIN_DB] ?: defaults.outputGainDb),
            fadeEnabled = prefs[FADE_ENABLED] ?: defaults.fadeEnabled,
            fadeStartMs = clampFadeStartMs(prefs[FADE_START_MS] ?: defaults.fadeStartMs),
            fadeStepMs = clampFadeStepMs(prefs[FADE_STEP_MS] ?: defaults.fadeStepMs),
            fadeStartDay = prefs[FADE_START_DAY] ?: defaults.fadeStartDay,
            disclaimerAccepted = prefs[DISCLAIMER_ACCEPTED] ?: defaults.disclaimerAccepted,
            dailyGoalMin = clampDailyGoalMin(prefs[DAILY_GOAL_MIN] ?: defaults.dailyGoalMin),
            reminderEnabled = prefs[REMINDER_ENABLED] ?: defaults.reminderEnabled,
            reminderMinute = clampReminderMinute(prefs[REMINDER_MINUTE] ?: defaults.reminderMinute),
            sessionLimitMin = clampSessionLimitMin(prefs[SESSION_LIMIT_MIN] ?: defaults.sessionLimitMin),
            presets = PRESETS.map { parsePreset(prefs[it]) },
        )
    }

    suspend fun setSessionLimitMin(min: Int) {
        store.edit { it[SESSION_LIMIT_MIN] = clampSessionLimitMin(min) }
    }

    /** Stores the current delay, sensitivity, pitch and volume in slot 0..2. */
    suspend fun savePreset(slot: Int) {
        val s = settings.first()
        store.edit { it[PRESETS[slot]] = encodePreset(Preset(s.delayMs, s.sensitivity, s.pitchSemitones, s.outputGainDb)) }
    }

    /** Applies slot 0..2. The delay is left alone while the fading program owns it. */
    suspend fun applyPreset(slot: Int) {
        val s = settings.first()
        val preset = s.presets[slot] ?: return
        store.edit {
            if (!s.fadeEnabled) it[DELAY_MS] = preset.delayMs
            it[SENSITIVITY] = preset.sensitivity
            it[PITCH_SEMITONES] = preset.pitchSemitones
            it[OUTPUT_GAIN_DB] = preset.outputGainDb
        }
    }

    suspend fun setDelayMs(ms: Int) {
        store.edit { it[DELAY_MS] = clampDelayMs(ms) }
    }

    suspend fun setMicPreset(index: Int) {
        store.edit { it[MIC_PRESET] = clampMicPreset(index) }
    }

    suspend fun setGateEnabled(enabled: Boolean) {
        store.edit { it[GATE_ENABLED] = enabled }
    }

    suspend fun setSensitivity(level: Int) {
        store.edit { it[SENSITIVITY] = clampSensitivity(level) }
    }

    suspend fun setPitchSemitones(semitones: Int) {
        store.edit { it[PITCH_SEMITONES] = clampPitchSemitones(semitones) }
    }

    suspend fun setOutputGainDb(db: Int) {
        store.edit { it[OUTPUT_GAIN_DB] = clampOutputGainDb(db) }
    }

    suspend fun setFadeEnabled(enabled: Boolean) {
        store.edit {
            it[FADE_ENABLED] = enabled
            if (enabled) it[FADE_START_DAY] = LocalDate.now().toEpochDay()
        }
    }

    suspend fun setFadeStartMs(ms: Int) {
        store.edit { it[FADE_START_MS] = clampFadeStartMs(ms) }
    }

    suspend fun setFadeStepMs(ms: Int) {
        store.edit { it[FADE_STEP_MS] = clampFadeStepMs(ms) }
    }

    suspend fun setDisclaimerAccepted() {
        store.edit { it[DISCLAIMER_ACCEPTED] = true }
    }

    suspend fun setDailyGoalMin(min: Int) {
        store.edit { it[DAILY_GOAL_MIN] = clampDailyGoalMin(min) }
    }

    suspend fun setReminderEnabled(enabled: Boolean) {
        store.edit { it[REMINDER_ENABLED] = enabled }
    }

    suspend fun setReminderMinute(minute: Int) {
        store.edit { it[REMINDER_MINUTE] = clampReminderMinute(minute) }
    }

    suspend fun applyFadeProgram() {
        val s = settings.first()
        if (s.fadeEnabled) {
            setDelayMs(programDelayMs(s.fadeStartMs, s.fadeStepMs, s.fadeStartDay, LocalDate.now().toEpochDay()))
        }
    }

    private companion object {
        val DELAY_MS = intPreferencesKey("delay_ms")
        val MIC_PRESET = intPreferencesKey("mic_preset")
        val GATE_ENABLED = booleanPreferencesKey("gate_enabled")
        val SENSITIVITY = intPreferencesKey("sensitivity")
        val PITCH_SEMITONES = intPreferencesKey("pitch_semitones")
        val OUTPUT_GAIN_DB = intPreferencesKey("output_gain_db")
        val FADE_ENABLED = booleanPreferencesKey("fade_enabled")
        val FADE_START_MS = intPreferencesKey("fade_start_ms")
        val FADE_STEP_MS = intPreferencesKey("fade_step_ms")
        val FADE_START_DAY = longPreferencesKey("fade_start_day")
        val DISCLAIMER_ACCEPTED = booleanPreferencesKey("disclaimer_accepted")
        val DAILY_GOAL_MIN = intPreferencesKey("daily_goal_min")
        val REMINDER_ENABLED = booleanPreferencesKey("reminder_enabled")
        val REMINDER_MINUTE = intPreferencesKey("reminder_minute")
        val SESSION_LIMIT_MIN = intPreferencesKey("session_limit_min")
        val PRESETS = List(PRESET_SLOTS) { stringPreferencesKey("preset_${it + 1}") }
    }
}
