#pragma once

#include <algorithm>
#include <cmath>

#include "Constants.h"

namespace cadence {

// Peak limiter with instant attack and exponential release (DESIGN.md section 16).
// Not thread-safe: use from the audio callback only.
class OutputLimiter {
 public:
  void reset() { env_ = 0.0f; }

  float process(float v) {
    env_ = std::max(std::fabs(v), env_ * kRelease);
    return env_ > kLimiterCeiling ? v * kLimiterCeiling / env_ : v;
  }

 private:
  static inline const float kRelease = std::exp(-1.0f / (kLimiterReleaseMs * kSamplesPerMs));
  float env_ = 0.0f;
};

}  // namespace cadence
