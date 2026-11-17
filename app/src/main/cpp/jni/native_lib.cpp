#include <jni.h>

#include "engine/AudioEngine.h"

namespace {
cadence::AudioEngine engine;

cadence::EngineConfig configFor(jint preset, jint inputDeviceId, jint outputDeviceId) {
  cadence::EngineConfig config;
  config.inputPreset = preset;
  config.inputDeviceId = inputDeviceId;
  config.outputDeviceId = outputDeviceId;
  return config;
}
}  // namespace

extern "C" JNIEXPORT jint JNICALL
Java_com_cadence_daf_NativeEngine_nativeStart(JNIEnv* /* env */, jobject /* thiz */,
                                              jint preset, jint inputDeviceId,
                                              jint outputDeviceId) {
  return engine.start(configFor(preset, inputDeviceId, outputDeviceId));
}

extern "C" JNIEXPORT void JNICALL
Java_com_cadence_daf_NativeEngine_nativeStop(JNIEnv* /* env */, jobject /* thiz */) {
  engine.stop();
}

extern "C" JNIEXPORT void JNICALL
Java_com_cadence_daf_NativeEngine_nativeSetDelayMs(JNIEnv* /* env */, jobject /* thiz */,
                                                   jint ms) {
  engine.setDelayMs(ms);
}

extern "C" JNIEXPORT void JNICALL
Java_com_cadence_daf_NativeEngine_nativeSetSensitivity(JNIEnv* /* env */, jobject /* thiz */,
                                                       jint level) {
  engine.setSensitivityLevel(level);
}

extern "C" JNIEXPORT void JNICALL
Java_com_cadence_daf_NativeEngine_nativeSetGateEnabled(JNIEnv* /* env */, jobject /* thiz */,
                                                       jboolean enabled) {
  engine.setGateEnabled(enabled);
}

extern "C" JNIEXPORT void JNICALL
Java_com_cadence_daf_NativeEngine_nativeSetPitchSemitones(JNIEnv* /* env */, jobject /* thiz */,
                                                          jint semitones) {
  engine.setPitchSemitones(semitones);
}

extern "C" JNIEXPORT void JNICALL
Java_com_cadence_daf_NativeEngine_nativeSetOutputGainDb(JNIEnv* /* env */, jobject /* thiz */,
                                                        jint db) {
  engine.setOutputGainDb(db);
}

extern "C" JNIEXPORT void JNICALL
Java_com_cadence_daf_NativeEngine_nativeSetMuted(JNIEnv* /* env */, jobject /* thiz */,
                                                 jboolean muted) {
  engine.setMuted(muted == JNI_TRUE);
}

extern "C" JNIEXPORT void JNICALL
Java_com_cadence_daf_NativeEngine_nativeSetMaskingDb(JNIEnv* /* env */, jobject /* thiz */,
                                                     jint db) {
  engine.setMaskingDb(db);
}

extern "C" JNIEXPORT jint JNICALL
Java_com_cadence_daf_NativeEngine_nativeGetState(JNIEnv* /* env */, jobject /* thiz */) {
  return static_cast<jint>(engine.state());
}

extern "C" JNIEXPORT jfloat JNICALL
Java_com_cadence_daf_NativeEngine_nativeGetInputRmsDb(JNIEnv* /* env */, jobject /* thiz */) {
  return engine.inputRmsDb();
}

extern "C" JNIEXPORT jint JNICALL
Java_com_cadence_daf_NativeEngine_nativeGetUnderruns(JNIEnv* /* env */, jobject /* thiz */) {
  return engine.underruns();
}

extern "C" JNIEXPORT jint JNICALL
Java_com_cadence_daf_NativeEngine_nativeGetSampleRate(JNIEnv* /* env */, jobject /* thiz */) {
  return engine.sampleRate();
}

extern "C" JNIEXPORT jint JNICALL
Java_com_cadence_daf_NativeEngine_nativeGetFramesPerBurst(JNIEnv* /* env */, jobject /* thiz */) {
  return engine.framesPerBurst();
}

extern "C" JNIEXPORT jfloat JNICALL
Java_com_cadence_daf_NativeEngine_nativeGetFloorDb(JNIEnv* /* env */, jobject /* thiz */) {
  return engine.floorDb();
}

extern "C" JNIEXPORT jboolean JNICALL
Java_com_cadence_daf_NativeEngine_nativeGetGateOpen(JNIEnv* /* env */, jobject /* thiz */) {
  return engine.gateOpen() ? JNI_TRUE : JNI_FALSE;
}

extern "C" JNIEXPORT jboolean JNICALL
Java_com_cadence_daf_NativeEngine_nativeGetSpeechDetected(JNIEnv* /* env */, jobject /* thiz */) {
  return engine.speechDetected() ? JNI_TRUE : JNI_FALSE;
}

extern "C" JNIEXPORT jfloat JNICALL
Java_com_cadence_daf_NativeEngine_nativeGetLatencyMs(JNIEnv* /* env */, jobject /* thiz */) {
  return engine.latencyMs();
}
