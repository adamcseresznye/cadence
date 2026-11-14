package com.cadence.daf

import android.content.Context
import android.media.AudioDeviceCallback
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.os.Handler
import android.os.Looper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class HeadsetMonitor(context: Context) {
    private val audioManager = context.getSystemService(AudioManager::class.java)
    private val _state = MutableStateFlow(read())
    val state: StateFlow<HeadsetState> = _state.asStateFlow()

    private val callback = object : AudioDeviceCallback() {
        override fun onAudioDevicesAdded(addedDevices: Array<out AudioDeviceInfo>) = refresh()
        override fun onAudioDevicesRemoved(removedDevices: Array<out AudioDeviceInfo>) = refresh()
    }

    init {
        audioManager.registerAudioDeviceCallback(callback, Handler(Looper.getMainLooper()))
    }

    fun close() = audioManager.unregisterAudioDeviceCallback(callback)

    private fun refresh() {
        _state.value = read()
    }

    private fun read(): HeadsetState =
        HeadsetLogic.classify(
            audioManager.getDevices(AudioManager.GET_DEVICES_INPUTS or AudioManager.GET_DEVICES_OUTPUTS).map {
                AudioDeviceLite(
                    id = it.id,
                    type = it.type,
                    isSource = it.isSource,
                    isSink = it.isSink,
                    name = it.productName.toString(),
                )
            }
        )
}
