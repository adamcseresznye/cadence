package com.cadence.daf

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SessionInfoTest {
    @Test
    fun quietAfterEightSecondsWithoutSpeech() {
        assertTrue(quietMic(8_000, -60f, heardSpeech = false))
    }

    @Test
    fun notQuietTooEarly() {
        assertFalse(quietMic(7_999, -60f, heardSpeech = false))
    }

    @Test
    fun notQuietOnceSpeechWasHeard() {
        assertFalse(quietMic(20_000, -60f, heardSpeech = true))
    }

    @Test
    fun notQuietWhenInputWasLoudEnough() {
        assertFalse(quietMic(20_000, -50f, heardSpeech = false))
    }

    @Test
    fun formatsMinutesAndSeconds() {
        assertEquals("0:00", formatElapsed(0))
        assertEquals("4:12", formatElapsed(252))
    }

    @Test
    fun formatsHours() {
        assertEquals("1:02:03", formatElapsed(3723))
    }
}
