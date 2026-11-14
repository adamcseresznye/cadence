package com.cadence.daf

import org.junit.Assert.assertEquals
import org.junit.Test

class DelayPresetsTest {

    @Test
    fun nextDelayPresetPicksSmallestLargerPreset() {
        assertEquals(60, nextDelayPreset(40))
        assertEquals(100, nextDelayPreset(60))
        assertEquals(150, nextDelayPreset(100))
        assertEquals(150, nextDelayPreset(120))
    }

    @Test
    fun nextDelayPresetWrapsToFirst() {
        assertEquals(60, nextDelayPreset(150))
        assertEquals(60, nextDelayPreset(200))
    }
}
