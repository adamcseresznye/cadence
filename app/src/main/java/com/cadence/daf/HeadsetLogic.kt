package com.cadence.daf

object HeadsetLogic {
    const val TYPE_WIRED_HEADSET = 3
    const val TYPE_WIRED_HEADPHONES = 4
    const val TYPE_USB_HEADSET = 22
    const val TYPE_BLUETOOTH_SCO = 7
    const val TYPE_BLUETOOTH_A2DP = 8

    private val OUTPUT_TYPES = setOf(
        TYPE_WIRED_HEADSET, TYPE_WIRED_HEADPHONES, TYPE_USB_HEADSET, TYPE_BLUETOOTH_A2DP
    )
    private val INPUT_TYPES = setOf(TYPE_WIRED_HEADSET, TYPE_USB_HEADSET, TYPE_BLUETOOTH_SCO)
    private val BLUETOOTH_TYPES = setOf(TYPE_BLUETOOTH_A2DP, TYPE_BLUETOOTH_SCO)

    fun classify(devices: List<AudioDeviceLite>): HeadsetState {
        val output = pick(devices.filter { it.isSink && it.type in OUTPUT_TYPES })
        // DESIGN.md 13.2: a SCO mic is used only with a Bluetooth output.
        val scoAllowed = output?.type == TYPE_BLUETOOTH_A2DP
        val input = pick(devices.filter {
            it.isSource && it.type in INPUT_TYPES && (scoAllowed || it.type != TYPE_BLUETOOTH_SCO)
        })
        return HeadsetState(
            wiredOutputId = output?.id,
            headsetInputId = input?.id,
            name = output?.name.orEmpty(),
            isBluetooth = output?.type == TYPE_BLUETOOTH_A2DP,
            headsetInputIsBluetooth = input?.type == TYPE_BLUETOOTH_SCO,
        )
    }

    // DESIGN.md section 13: wired/USB first, Bluetooth only when nothing else is there.
    private fun pick(candidates: List<AudioDeviceLite>): AudioDeviceLite? =
        candidates.minWithOrNull(compareBy<AudioDeviceLite>({ it.type in BLUETOOTH_TYPES }, { it.id }))
}
