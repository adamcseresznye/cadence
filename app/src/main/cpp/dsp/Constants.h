#pragma once

namespace cadence {

constexpr int kSampleRate = 48000;
constexpr int kSamplesPerMs = 48;
constexpr int kVadFrameSamples = 480;
constexpr int kLookaheadFrames = 3;
constexpr int kMinDelayMs = 40;
constexpr int kMaxDelayMs = 200;
constexpr int kDelayLineCapacity = 24000;
constexpr int kDelayFadeSamples = 960;
constexpr int kMaxBlock = 8192;
constexpr int kDefaultSensitivity = 5;
constexpr int kDefaultHoldFrames = 20;
constexpr float kAttackMs = 5.0f;
constexpr float kDefaultReleaseMs = 150.0f;
constexpr float kLevelFloorDb = -90.0f;     // lowest level ever reported
constexpr float kGateMinLevelDb = -55.0f;   // absolute minimum level to open the gate
constexpr float kNoiseFloorStartDb = -60.0f; // starting value of the noise floor
constexpr int kMinPitchSemitones = -12;
constexpr int kMaxPitchSemitones = 12;
constexpr int kDefaultPitchSemitones = 0;
constexpr int kPitchWindowSamples = 1440;  // 30 ms
constexpr int kPitchBufferSamples = 4096;  // power of two
constexpr int kMinOutputGainDb = -30;
constexpr int kMaxOutputGainDb = 0;
constexpr int kDefaultOutputGainDb = -6;
constexpr float kLimiterCeiling = 0.708f;  // -3 dBFS
constexpr float kLimiterReleaseMs = 50.0f;
constexpr float kVoiceTargetDb = -16.0f;        // speech level the leveler aims for
constexpr float kMaxVoiceBoostDb = 30.0f;
constexpr float kSpeechLevelRate = 0.05f;       // per speech frame
constexpr float kVoiceLevelerSmoothMs = 200.0f;
constexpr int kMinMaskingDb = -40;
constexpr int kMaxMaskingDb = -10;
constexpr int kMaskingOffDb = -100;  // any value below kMinMaskingDb means off
constexpr float kMaskingSmoothMs = 50.0f;

}  // namespace cadence
