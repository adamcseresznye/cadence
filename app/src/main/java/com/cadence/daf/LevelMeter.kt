package com.cadence.daf

/** Maps input level in dB (-90 up to 0) to 0..1 for the Main screen glow. */
fun levelFraction(db: Float): Float = ((db + 90f) / 90f).coerceIn(0f, 1f)
