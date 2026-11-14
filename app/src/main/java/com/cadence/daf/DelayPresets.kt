package com.cadence.daf

private val DELAY_PRESETS_MS = listOf(60, 100, 150)

fun nextDelayPreset(currentMs: Int): Int = DELAY_PRESETS_MS.firstOrNull { it > currentMs } ?: DELAY_PRESETS_MS.first()
