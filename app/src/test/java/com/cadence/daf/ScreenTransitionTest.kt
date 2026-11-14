package com.cadence.daf

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ScreenTransitionTest {

    @Test
    fun settingsSlidesForward() {
        assertTrue(slidesForward(SETTINGS))
    }

    @Test
    fun mainSlidesBack() {
        assertFalse(slidesForward(MAIN))
    }

    @Test
    fun progressSlidesForward() {
        assertTrue(slidesForward(PROGRESS))
    }
}
