package com.cadence.daf

data class HeadsetState(
    val wiredOutputId: Int? = null,
    val headsetInputId: Int? = null,
    val name: String = "",
    val isBluetooth: Boolean = false,
    val headsetInputIsBluetooth: Boolean = false,
) {
    val canStart: Boolean get() = wiredOutputId != null
    val hasHeadsetMic: Boolean get() = headsetInputId != null
}
