#pragma once

#include <algorithm>
#include <array>
#include <cmath>
#include <cstdint>

#include "Constants.h"
#include "IVad.h"
#include "SpeechTimeline.h"

namespace cadence {

// Per-frame level, noise floor and speech decision (DESIGN.md section 7).
// Not thread-safe: use from the audio callback only.
class FrameAnalyzer {
 public:
  FrameAnalyzer(IVad* vad, SpeechTimeline* timeline) : vad_(vad), timeline_(timeline) { reset(); }

  void reset() {
    count_ = 0;
    floorDb_ = kNoiseFloorStartDb;
    lastLevelDb_ = kLevelFloorDb;
    lastSpeech_ = false;
    speechLevelDb_ = kVoiceTargetDb;
  }

  void setMarginDb(float marginDb) { marginDb_ = marginDb; }

  void push(const float* samples, int count) {
    for (int i = 0; i < count; ++i) {
      frame_[count_++] = samples[i];
      if (count_ == kVadFrameSamples) {
        processFrame();
        count_ = 0;
      }
    }
  }

  float lastLevelDb() const { return lastLevelDb_; }
  float floorDb() const { return floorDb_; }
  bool lastSpeech() const { return lastSpeech_; }
  // Smoothed level of speech frames only; starts at kVoiceTargetDb.
  float speechLevelDb() const { return speechLevelDb_; }

 private:
  static constexpr float kFloorFallRate = 0.3f;
  static constexpr float kFloorRisePerFrameDb = 0.03f;

  static float levelDb(const std::array<float, kVadFrameSamples>& frame) {
    float sum = 0.0f;
    for (float s : frame) sum += s * s;
    const float rms = std::sqrt(sum / kVadFrameSamples);
    return std::max(kLevelFloorDb, 20.0f * std::log10(std::max(rms, 1e-9f)));
  }

  void updateFloor(float levelDb) {
    if (levelDb < floorDb_) {
      floorDb_ += kFloorFallRate * (levelDb - floorDb_);
    } else {
      floorDb_ = std::min(floorDb_ + kFloorRisePerFrameDb, levelDb);
    }
  }

  void toPcm() {
    for (int i = 0; i < kVadFrameSamples; ++i) {
      const float scaled = std::round(frame_[i] * 32768.0f);
      pcm_[i] = static_cast<int16_t>(std::clamp(scaled, -32768.0f, 32767.0f));
    }
  }

  void processFrame() {
    lastLevelDb_ = levelDb(frame_);
    updateFloor(lastLevelDb_);
    const bool loud = lastLevelDb_ > floorDb_ + marginDb_ && lastLevelDb_ > kGateMinLevelDb;
    toPcm();
    // Ask the detector for every frame, even quiet ones, so it keeps learning.
    const bool voice = vad_->isSpeech(pcm_.data(), kVadFrameSamples);
    lastSpeech_ = voice && loud;
    if (lastSpeech_) {
      speechLevelDb_ += kSpeechLevelRate * (lastLevelDb_ - speechLevelDb_);
    }
    timeline_->push(lastSpeech_);
  }

  IVad* vad_;
  SpeechTimeline* timeline_;
  std::array<float, kVadFrameSamples> frame_{};
  std::array<int16_t, kVadFrameSamples> pcm_{};
  int count_ = 0;
  float marginDb_ = 12.5f;
  float floorDb_ = kNoiseFloorStartDb;
  float lastLevelDb_ = kLevelFloorDb;
  bool lastSpeech_ = false;
  float speechLevelDb_ = kVoiceTargetDb;
};

}  // namespace cadence
