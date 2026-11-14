package com.cadence.daf

data class Telemetry(
    val inputDb: Float = -90f,
    val floorDb: Float = -60f,
    val gateOpen: Boolean = false,
    val speechDetected: Boolean = false,
    val latencyMs: Float? = null,
    val elapsedSec: Int = 0,
    val quietMic: Boolean = false,
)
