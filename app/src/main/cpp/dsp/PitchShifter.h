#pragma once

#include <algorithm>
#include <cmath>
#include <vector>

#include "Constants.h"

namespace cadence {

// Two-tap delay-line pitch shifter (DESIGN.md section 15).
// Not thread-safe: use from the audio callback only.
class PitchShifter {
 public:
  PitchShifter() : ring_(kPitchBufferSamples, 0.0f) {}

  void reset() {
    std::fill(ring_.begin(), ring_.end(), 0.0f);
    writePos_ = 0;
    phase_ = 0.0f;
    level_ = 0;
  }

  void setSemitones(int semitones) {
    shifting_ = semitones != 0;
    if (!shifting_) return;  // keep phaseStep_ so the fade-out stays shifted
    const float ratio = std::pow(2.0f, static_cast<float>(semitones) / 12.0f);
    phaseStep_ = (1.0f - ratio) / kPitchWindowSamples;
  }

  float process(float x) {
    ring_[writePos_] = x;
    float out = x;
    if (level_ > 0) {
      const float mix = static_cast<float>(level_) / kDelayFadeSamples;
      out = (1.0f - mix) * x + mix * shifted();
    }
    const int target = shifting_ ? kDelayFadeSamples : 0;
    level_ += (level_ < target) - (level_ > target);
    writePos_ = (writePos_ + 1) & kMask;
    phase_ = wrap01(phase_ + phaseStep_);
    return out;
  }

 private:
  static constexpr int kMask = kPitchBufferSamples - 1;
  static constexpr float kPi = 3.14159265358979f;

  static float wrap01(float v) {
    const float f = v - std::floor(v);
    return f < 1.0f ? f : 0.0f;
  }

  // Ring buffer value `delay` samples before the write position, linearly interpolated.
  float readAt(float delay) const {
    float pos = static_cast<float>(writePos_) - delay;
    if (pos < 0.0f) pos += kPitchBufferSamples;
    const int i0 = static_cast<int>(pos);
    const float frac = pos - static_cast<float>(i0);
    const float a = ring_[i0 & kMask];
    const float b = ring_[(i0 + 1) & kMask];
    return a + frac * (b - a);
  }

  float shifted() const {
    float sum = 0.0f;
    for (int k = 0; k < 2; ++k) {
      const float p = wrap01(phase_ + 0.5f * static_cast<float>(k));
      const float w = std::sin(kPi * p);
      sum += w * w * readAt(p * kPitchWindowSamples);
    }
    return sum;
  }

  std::vector<float> ring_;
  int writePos_ = 0;
  float phase_ = 0.0f;
  float phaseStep_ = 0.0f;
  int level_ = 0;  // 0 = bypass, kDelayFadeSamples = fully shifted
  bool shifting_ = false;
};

}  // namespace cadence
