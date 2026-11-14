package com.cadence.daf

import org.junit.Assert.assertEquals
import org.junit.Test

class LevelMeterTest {

    @Test
    fun minimumLevelIsZero() {
        assertEquals(0f, levelFraction(-90f), 0.0001f)
    }

    @Test
    fun fullScaleIsOne() {
        assertEquals(1f, levelFraction(0f), 0.0001f)
    }

    @Test
    fun midLevelIsHalf() {
        assertEquals(0.5f, levelFraction(-45f), 0.0001f)
    }

    @Test
    fun levelsOutsideRangeAreClamped() {
        assertEquals(0f, levelFraction(-120f), 0.0001f)
        assertEquals(1f, levelFraction(6f), 0.0001f)
    }
}
