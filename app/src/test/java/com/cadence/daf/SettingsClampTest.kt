package com.cadence.daf

import org.junit.Assert.assertEquals
import org.junit.Test

class SettingsClampTest {

    @Test
    fun clampDelayMsKeepsValueInRange() {
        assertEquals(40, clampDelayMs(10))
        assertEquals(200, clampDelayMs(500))
        assertEquals(125, clampDelayMs(125))
    }

    @Test
    fun clampSensitivityKeepsValueInRange() {
        assertEquals(0, clampSensitivity(-1))
        assertEquals(10, clampSensitivity(11))
        assertEquals(7, clampSensitivity(7))
    }

    @Test
    fun clampPitchKeepsValueInRange() {
        assertEquals(-12, clampPitchSemitones(-20))
        assertEquals(12, clampPitchSemitones(20))
        assertEquals(3, clampPitchSemitones(3))
    }

    @Test
    fun clampOutputGainKeepsValueInRange() {
        assertEquals(-30, clampOutputGainDb(-40))
        assertEquals(0, clampOutputGainDb(5))
        assertEquals(-6, clampOutputGainDb(-6))
    }

    @Test
    fun clampFadeSettingsKeepValueInRange() {
        assertEquals(40, clampFadeStartMs(20))
        assertEquals(200, clampFadeStartMs(300))
        assertEquals(0, clampFadeStepMs(-5))
        assertEquals(50, clampFadeStepMs(80))
    }

    @Test
    fun clampMicPresetKeepsValidIndex() {
        assertEquals(1, clampMicPreset(1))
        assertEquals(3, clampMicPreset(3))
        assertEquals(1, clampMicPreset(0))
        assertEquals(1, clampMicPreset(2))
        assertEquals(1, clampMicPreset(7))
        assertEquals(1, clampMicPreset(-1))
    }

    @Test
    fun clampDailyGoalMinKeepsAllowedValues() {
        assertEquals(0, clampDailyGoalMin(0))
        assertEquals(15, clampDailyGoalMin(15))
        assertEquals(10, clampDailyGoalMin(7))
        assertEquals(10, clampDailyGoalMin(99))
    }

    @Test
    fun clampReminderMinuteKeepsValueInDay() {
        assertEquals(0, clampReminderMinute(-5))
        assertEquals(1439, clampReminderMinute(2000))
        assertEquals(1080, clampReminderMinute(1080))
    }
}
