package com.cadence.daf

data class AudioDeviceLite(
    val id: Int,
    val type: Int,
    val isSource: Boolean,
    val isSink: Boolean,
    val name: String,
)
