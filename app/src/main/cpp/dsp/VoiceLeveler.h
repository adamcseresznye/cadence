#pragma once

#include <algorithm>
#include <cmath>

#include "Constants.h"

namespace cadence {

// Automatic voice leveling (DESIGN.md section 16a): raises quiet speech toward kVoiceTargetDb.
// Never attenuates. Not thread-safe: use from the audio callback only.
class VoiceLeveler {
 public:
  VoiceLeveler() : coeff_(1.0f - std::exp(-1.0f / (kVoiceLevelerSmoothMs * kSamplesPerMs))) {}

  void reset() {
    gain_ = 1.0f;
    target_ = 1.0f;
  }

  void setSpeechLevelDb(float speechLevelDb) {
    const float boostDb = std::clamp(kVoiceTargetDb - speechLevelDb, 0.0f, kMaxVoiceBoostDb);
    target_ = std::pow(10.0f, boostDb / 20.0f);
  }

  float process(float x) {
    gain_ += coeff_ * (target_ - gain_);
    return x * gain_;
  }

 private:
  float coeff_;
  float gain_ = 1.0f;
  float target_ = 1.0f;
};

}  // namespace cadence
