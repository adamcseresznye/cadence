package com.cadence.daf

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class HeadsetLogicTest {

    @Test
    fun emptyListHasNoHeadset() {
        val state = HeadsetLogic.classify(emptyList())

        assertFalse(state.canStart)
        assertFalse(state.hasHeadsetMic)
        assertEquals("", state.name)
    }

    @Test
    fun wiredHeadsetHasOutputAndMic() {
        val state = HeadsetLogic.classify(
            listOf(
                AudioDeviceLite(5, 3, isSource = false, isSink = true, name = "Wired"),
                AudioDeviceLite(6, 3, isSource = true, isSink = false, name = "Wired"),
            )
        )

        assertEquals(5, state.wiredOutputId)
        assertEquals(6, state.headsetInputId)
        assertTrue(state.canStart)
        assertTrue(state.hasHeadsetMic)
    }

    @Test
    fun wiredHeadphonesWithoutMicrophone() {
        val state = HeadsetLogic.classify(
            listOf(AudioDeviceLite(7, 4, isSource = false, isSink = true, name = "Phones"))
        )

        assertTrue(state.canStart)
        assertFalse(state.hasHeadsetMic)
        assertNull(state.headsetInputId)
    }

    @Test
    fun bluetoothHeadphonesStartAndAreFlagged() {
        val state = HeadsetLogic.classify(
            listOf(AudioDeviceLite(8, 8, isSource = false, isSink = true, name = "BT"))
        )

        assertTrue(state.canStart)
        assertTrue(state.isBluetooth)
        assertFalse(state.hasHeadsetMic)
    }

    @Test
    fun bluetoothHeadsetWithScoMicrophone() {
        val state = HeadsetLogic.classify(
            listOf(
                AudioDeviceLite(8, 8, isSource = false, isSink = true, name = "BT"),
                AudioDeviceLite(12, 7, isSource = true, isSink = false, name = "BT"),
            )
        )

        assertEquals(12, state.headsetInputId)
        assertTrue(state.isBluetooth)
    }

    @Test
    fun scoMicrophoneIsFlaggedAsBluetoothInput() {
        val state = HeadsetLogic.classify(
            listOf(
                AudioDeviceLite(8, 8, isSource = false, isSink = true, name = "BT"),
                AudioDeviceLite(12, 7, isSource = true, isSink = false, name = "BT"),
            )
        )

        assertTrue(state.headsetInputIsBluetooth)
    }

    @Test
    fun wiredMicrophoneIsNotFlaggedAsBluetoothInput() {
        val state = HeadsetLogic.classify(
            listOf(
                AudioDeviceLite(5, 3, isSource = false, isSink = true, name = "Wired"),
                AudioDeviceLite(6, 3, isSource = true, isSink = false, name = "Wired"),
            )
        )

        assertFalse(state.headsetInputIsBluetooth)
    }

    @Test
    fun wiredHeadsetIsNotFlaggedAsBluetooth() {
        val state = HeadsetLogic.classify(
            listOf(AudioDeviceLite(5, 3, isSource = false, isSink = true, name = "Wired"))
        )

        assertFalse(state.isBluetooth)
    }

    @Test
    fun speakerAndBuiltInMicrophoneDoNotCount() {
        val state = HeadsetLogic.classify(
            listOf(
                AudioDeviceLite(1, 2, isSource = false, isSink = true, name = "Speaker"),
                AudioDeviceLite(2, 15, isSource = true, isSink = false, name = "Mic"),
            )
        )

        assertFalse(state.canStart)
        assertFalse(state.hasHeadsetMic)
    }

    @Test
    fun usbHeadsetHasOutputAndMic() {
        val state = HeadsetLogic.classify(
            listOf(
                AudioDeviceLite(10, 22, isSource = false, isSink = true, name = "USB"),
                AudioDeviceLite(11, 22, isSource = true, isSink = false, name = "USB"),
            )
        )

        assertTrue(state.canStart)
        assertTrue(state.hasHeadsetMic)
        assertEquals("USB", state.name)
    }

    @Test
    fun lowestIdWins() {
        val state = HeadsetLogic.classify(
            listOf(
                AudioDeviceLite(9, 3, isSource = false, isSink = true, name = "B"),
                AudioDeviceLite(5, 3, isSource = false, isSink = true, name = "A"),
            )
        )

        assertEquals(5, state.wiredOutputId)
        assertEquals("A", state.name)
    }

    @Test
    fun bluetoothOnly_canStart_isBluetooth() {
        val state = HeadsetLogic.classify(
            listOf(AudioDeviceLite(10, 8, isSource = false, isSink = true, name = "BT"))
        )

        assertEquals(10, state.wiredOutputId)
        assertTrue(state.isBluetooth)
    }

    @Test
    fun wiredPreferredOverBluetooth() {
        val state = HeadsetLogic.classify(
            listOf(
                AudioDeviceLite(5, 8, isSource = false, isSink = true, name = "BT"),
                AudioDeviceLite(9, 3, isSource = true, isSink = true, name = "Wired"),
            )
        )

        assertEquals(9, state.wiredOutputId)
        assertEquals(9, state.headsetInputId)
        assertFalse(state.isBluetooth)
    }

    @Test
    fun headphonesWithoutMic_inputNull() {
        val state = HeadsetLogic.classify(
            listOf(AudioDeviceLite(4, 4, isSource = false, isSink = true, name = "Phones"))
        )

        assertEquals(4, state.wiredOutputId)
        assertNull(state.headsetInputId)
    }

    @Test
    fun speakerOnly_cannotStart() {
        val state = HeadsetLogic.classify(
            listOf(AudioDeviceLite(1, 2, isSource = false, isSink = true, name = "Speaker"))
        )

        assertNull(state.wiredOutputId)
    }

    @Test
    fun wiredOutputWithBluetoothMic_usesPhoneMic() {
        val state = HeadsetLogic.classify(
            listOf(
                AudioDeviceLite(4, 4, isSource = false, isSink = true, name = "Phones"),
                AudioDeviceLite(7, 7, isSource = true, isSink = false, name = "BT"),
                AudioDeviceLite(8, 8, isSource = false, isSink = true, name = "BT"),
            )
        )

        assertEquals(4, state.wiredOutputId)
        assertNull(state.headsetInputId)
        assertFalse(state.headsetInputIsBluetooth)
    }

    @Test
    fun bluetoothOutputWithBluetoothMic_usesScoMic() {
        val state = HeadsetLogic.classify(
            listOf(
                AudioDeviceLite(7, 7, isSource = true, isSink = false, name = "BT"),
                AudioDeviceLite(8, 8, isSource = false, isSink = true, name = "BT"),
            )
        )

        assertEquals(8, state.wiredOutputId)
        assertEquals(7, state.headsetInputId)
        assertTrue(state.headsetInputIsBluetooth)
    }
}
