package com.cadence.daf

import java.util.Locale

private const val QUIET_AFTER_MS = 8_000L
private const val QUIET_PEAK_DB = -50f

/**
 * True when, after 8 s of running, no speech was heard and the input never got above -50 dB:
 * the microphone is likely too quiet for the gate to open (its floor is -55 dB).
 */
fun quietMic(elapsedMs: Long, peakDb: Float, heardSpeech: Boolean): Boolean =
    !heardSpeech && elapsedMs >= QUIET_AFTER_MS && peakDb < QUIET_PEAK_DB

/** "4:12", or "1:02:03" from one hour on. */
fun formatElapsed(seconds: Int): String {
    val h = seconds / 3600
    val m = seconds / 60 % 60
    val s = seconds % 60
    return if (h > 0) String.format(Locale.US, "%d:%02d:%02d", h, m, s) else String.format(Locale.US, "%d:%02d", m, s)
}
