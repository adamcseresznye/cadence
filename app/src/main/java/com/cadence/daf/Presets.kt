package com.cadence.daf

import kotlin.math.floor

const val PRESET_SLOTS = 3

/** One saved set of the four Fine-tune values (DESIGN.md section 31). */
data class Preset(val delayMs: Int, val sensitivity: Int, val pitchSemitones: Int, val outputGainDb: Int)

fun encodePreset(preset: Preset): String =
    "${preset.delayMs},${preset.sensitivity},${preset.pitchSemitones},${preset.outputGainDb}"

/** Null for a missing or malformed value; numbers out of range are clamped. */
fun parsePreset(text: String?): Preset? {
    val parts = text?.split(",")?.map { it.toIntOrNull() ?: return null } ?: return null
    if (parts.size != 4) return null
    return Preset(
        clampDelayMs(parts[0]), clampSensitivity(parts[1]), clampPitchSemitones(parts[2]), clampOutputGainDb(parts[3])
    )
}

/**
 * The most sensitive level whose margin (20 - 1.5 * level, DESIGN.md section 9) still keeps the
 * measured room noise below the gate's loudness test, with 3 dB to spare (DESIGN.md section 32).
 */
fun suggestSensitivity(peakDb: Float, floorDb: Float): Int {
    val neededMarginDb = peakDb - floorDb + 3f
    return clampSensitivity(floor((20f - neededMarginDb) / 1.5f).toInt())
}
