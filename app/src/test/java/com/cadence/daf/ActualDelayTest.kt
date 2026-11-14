package com.cadence.daf

import org.junit.Assert.assertEquals
import org.junit.Test

class ActualDelayTest {
    @Test
    fun measured() {
        assertEquals(ActualDelay.Measured(117), actualDelay(100, 17.4f, false, pitchSemitones = 0))
    }

    @Test
    fun roundsHalfUp() {
        assertEquals(ActualDelay.Measured(118), actualDelay(100, 17.5f, false, pitchSemitones = 0))
    }

    @Test
    fun bluetoothWins() {
        assertEquals(ActualDelay.Bluetooth, actualDelay(100, 17.4f, true, pitchSemitones = 0))
    }

    @Test
    fun notMeasured() {
        assertEquals(ActualDelay.NotMeasured, actualDelay(100, null, false, pitchSemitones = 0))
    }

    @Test
    fun pitchAddsLatency() {
        assertEquals(ActualDelay.Measured(132), actualDelay(100, 17.4f, false, pitchSemitones = 3))
    }

    @Test
    fun firstLatencyIsTakenAsIs() {
        assertEquals(20f, smoothLatency(null, 20f), 0f)
    }

    @Test
    fun latencyMovesTenPercentTowardNewValue() {
        assertEquals(21f, smoothLatency(20f, 30f), 1e-4f)
    }
}
