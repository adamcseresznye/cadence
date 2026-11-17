package com.cadence.daf

data class Settings(
    val delayMs: Int = 100,
    val micPreset: Int = 1,
    val gateEnabled: Boolean = true,
    val sensitivity: Int = 5,
    val pitchSemitones: Int = 0,
    val outputGainDb: Int = -6,
    val fadeEnabled: Boolean = false,
    val fadeStartMs: Int = 100,
    val fadeStepMs: Int = 10,
    val fadeStartDay: Long = 0,
    val disclaimerAccepted: Boolean = false,
    val dailyGoalMin: Int = 10,
    val reminderEnabled: Boolean = false,
    val reminderMinute: Int = 18 * 60,
    val sessionLimitMin: Int = 0,
    val presets: List<Preset?> = List(PRESET_SLOTS) { null },
)

val SESSION_LIMIT_CHOICES = listOf(0, 5, 10, 15, 20, 30)

fun clampSessionLimitMin(min: Int): Int = if (min in SESSION_LIMIT_CHOICES) min else 0

fun clampDelayMs(ms: Int): Int = ms.coerceIn(40, 200)

fun clampSensitivity(level: Int): Int = level.coerceIn(0, 10)

fun clampPitchSemitones(semitones: Int): Int = semitones.coerceIn(-12, 12)

fun clampOutputGainDb(db: Int): Int = db.coerceIn(-30, 0)

fun clampFadeStartMs(ms: Int): Int = ms.coerceIn(40, 200)

fun clampFadeStepMs(ms: Int): Int = ms.coerceIn(0, 50)

fun clampMicPreset(index: Int): Int = if (index == 3) 3 else 1

fun clampReminderMinute(minute: Int): Int = minute.coerceIn(0, 24 * 60 - 1)

fun clampDailyGoalMin(min: Int): Int = if (min in listOf(0, 5, 10, 15, 20)) min else 10
