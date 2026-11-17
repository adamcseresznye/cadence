#pragma once

#include <algorithm>
#include <cmath>
#include <cstdint>

#include "Constants.h"

namespace cadence {

// Optional white masking noise (DESIGN.md section 33). Generated, never recorded.
// Not thread-safe: use from the audio callback only.
class MaskingNoise {
 public:
  MaskingNoise() : coeff_(1.0f - std::exp(-1.0f / (kMaskingSmoothMs * kSamplesPerMs))) {}

  void reset() {
    gain_ = 0.0f;
    state_ = kSeed;
  }

  // Below kMinMaskingDb (for example kMaskingOffDb) means off.
  void setGainDb(int db) {
    target_ = db < kMinMaskingDb
        ? 0.0f
        : std::pow(10.0f, static_cast<float>(std::min(db, kMaxMaskingDb)) / 20.0f);
  }

  // Next noise sample, uniform in [-gain, gain]; the gain glides to its target.
  float process() {
    gain_ += coeff_ * (target_ - gain_);
    state_ ^= state_ << 13;  // xorshift32
    state_ ^= state_ >> 17;
    state_ ^= state_ << 5;
    const float white = static_cast<float>(state_) * (2.0f / 4294967296.0f) - 1.0f;
    return white * gain_;
  }

 private:
  static constexpr uint32_t kSeed = 0x9E3779B9u;

  float coeff_;
  float gain_ = 0.0f;
  float target_ = 0.0f;
  uint32_t state_ = kSeed;
};

}  // namespace cadence
