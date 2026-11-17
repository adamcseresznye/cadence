package com.cadence.daf

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PresetsTest {

    @Test
    fun presetSurvivesEncodeAndParse() {
        val preset = Preset(120, 7, -3, -12)
        assertEquals(preset, parsePreset(encodePreset(preset)))
    }

    @Test
    fun parsePresetRejectsMissingOrMalformedText() {
        assertNull(parsePreset(null))
        assertNull(parsePreset(""))
        assertNull(parsePreset("100,5,0"))
        assertNull(parsePreset("100,5,x,-6"))
    }

    @Test
    fun parsePresetClampsOutOfRangeNumbers() {
        assertEquals(Preset(200, 10, -12, 0), parsePreset("900,99,-40,6"))
    }

    @Test
    fun suggestSensitivityPicksMostSensitiveLevelAboveTheNoise() {
        // Steady noise: needs 3 dB, every level's margin covers it.
        assertEquals(10, suggestSensitivity(-55f, -55f))
        // Noise peaks 5 dB over the floor: needs 8 dB, level 8 has exactly 8.0.
        assertEquals(8, suggestSensitivity(-50f, -55f))
        // Peaks 9.5 dB over: needs 12.5 dB, the default level 5.
        assertEquals(5, suggestSensitivity(-45.5f, -55f))
        // Very uneven noise: least sensitive.
        assertEquals(0, suggestSensitivity(-30f, -55f))
    }

    @Test
    fun clampSessionLimitMinKeepsAllowedValues() {
        assertEquals(0, clampSessionLimitMin(0))
        assertEquals(30, clampSessionLimitMin(30))
        assertEquals(0, clampSessionLimitMin(7))
    }
}
