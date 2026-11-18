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

    @Test
    fun stepBackRaisesDelayOneStep() {
        // day 21, 3 steps taken: 70 ms. One back: 80 ms.
        val start = stepBackStartDay(100, 10, 0, 21, 40, 7)
        assertEquals(80, programDelayMs(100, 10, start, 21, 40, 7))
        // already at the target after many days: goes back one step from the target.
        val atTarget = stepBackStartDay(100, 10, 0, 700, 40, 7)
        assertEquals(50, programDelayMs(100, 10, atTarget, 700, 40, 7))
        // nothing taken yet: unchanged.
        assertEquals(0L, stepBackStartDay(100, 10, 0, 3, 40, 7))
    }

    @Test
    fun customTargetAndInterval() {
        assertEquals(80, programDelayMs(100, 10, 0, 6, 70, 3))
        assertEquals(70, programDelayMs(100, 10, 0, 30, 70, 3))
        assertEquals(60, programDelayMs(60, 10, 0, 30, 70, 3))
    }
}
