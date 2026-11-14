package com.cadence.daf

import android.media.AudioManager
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AudioFocusPolicyTest {
    @Test
    fun callOrOtherAppStops() {
        assertTrue(stopsOnFocusChange(AudioManager.AUDIOFOCUS_LOSS))
        assertTrue(stopsOnFocusChange(AudioManager.AUDIOFOCUS_LOSS_TRANSIENT))
    }

    @Test
    fun duckingAndGainKeepRunning() {
        assertFalse(stopsOnFocusChange(AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK))
        assertFalse(stopsOnFocusChange(AudioManager.AUDIOFOCUS_GAIN))
    }
}
