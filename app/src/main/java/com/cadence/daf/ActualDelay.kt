package com.cadence.daf

import kotlin.math.roundToInt

sealed class ActualDelay {
    data class Measured(val ms: Int) : ActualDelay()
    object Bluetooth : ActualDelay()
    object NotMeasured : ActualDelay()
}

private const val PITCH_LATENCY_MS = 15

fun actualDelay(delayMs: Int, latencyMs: Float?, isBluetooth: Boolean, pitchSemitones: Int): ActualDelay = when {
    isBluetooth -> ActualDelay.Bluetooth
    latencyMs == null -> ActualDelay.NotMeasured
    else -> {
        val pitchMs = if (pitchSemitones != 0) PITCH_LATENCY_MS else 0
        ActualDelay.Measured((delayMs + latencyMs + pitchMs).roundToInt())
    }
}

private const val LATENCY_SMOOTHING = 0.1f

/** The reported latency jumps by a burst on every callback; average it so the text stays still. */
fun smoothLatency(previous: Float?, raw: Float): Float =
    if (previous == null) raw else previous + LATENCY_SMOOTHING * (raw - previous)
