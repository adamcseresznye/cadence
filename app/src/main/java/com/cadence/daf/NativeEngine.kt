package com.cadence.daf

object NativeEngine {
    init {
        System.loadLibrary("cadence_native")
    }

    // Start codes: 0 ok, 1 input open failed, 2 output open failed,
    // 3 stream start failed, 4 sample rate is not 48000.
    external fun nativeStart(preset: Int, inputDeviceId: Int, outputDeviceId: Int): Int
    external fun nativeStop()
    external fun nativeSetDelayMs(ms: Int)
    external fun nativeSetSensitivity(level: Int)
    external fun nativeSetGateEnabled(enabled: Boolean)
    external fun nativeSetPitchSemitones(semitones: Int)
    external fun nativeSetOutputGainDb(db: Int)
    external fun nativeSetMuted(muted: Boolean)

    // State codes: 0 Idle, 1 Running, 2 Error.
    external fun nativeGetState(): Int
    external fun nativeGetInputRmsDb(): Float
    external fun nativeGetUnderruns(): Int
    external fun nativeGetSampleRate(): Int
    external fun nativeGetFramesPerBurst(): Int
    external fun nativeGetFloorDb(): Float
    external fun nativeGetGateOpen(): Boolean
    external fun nativeGetSpeechDetected(): Boolean
    // Input + output latency in ms as Android reports it, or -1 when not running.
    external fun nativeGetLatencyMs(): Float
}
