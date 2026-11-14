package com.cadence.daf

import org.junit.Assert.assertEquals
import org.junit.Test

class FadeProgramTest {
    @Test
    fun programDelayStepsDownWeekly() {
        assertEquals(100, programDelayMs(100, 10, 0, 0))
        assertEquals(100, programDelayMs(100, 10, 0, 6))
        assertEquals(90, programDelayMs(100, 10, 0, 7))
        assertEquals(40, programDelayMs(100, 10, 0, 70))
        assertEquals(100, programDelayMs(100, 0, 0, 700))
    }
}
