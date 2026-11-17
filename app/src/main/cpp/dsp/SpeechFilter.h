#pragma once

#include <cmath>

#include "Constants.h"

namespace cadence {

// Speech-band filter for the played voice (DESIGN.md section 34): a second-order high-pass
// removes hum and rumble, a second-order low-pass removes hiss above the speech band.
// Not thread-safe: use from the audio callback only.
class SpeechFilter {
 public:
  SpeechFilter() {
    highPass_.set(kVoiceHighPassHz, true);
    lowPass_.set(kVoiceLowPassHz, false);
  }

  void reset() {
    highPass_.reset();
    lowPass_.reset();
  }

  float process(float x) { return lowPass_.process(highPass_.process(x)); }

 private:
  // Butterworth biquad (Q = 1/sqrt(2)), transposed direct form II.
  struct Biquad {
    void set(float cutoffHz, bool highPass) {
      const float w0 = 2.0f * 3.14159265358979f * cutoffHz / kSampleRate;
      const float cosW0 = std::cos(w0);
      const float alpha = std::sin(w0) / (2.0f * 0.70710678f);
      const float a0 = 1.0f + alpha;
      const float edge = highPass ? (1.0f + cosW0) / 2.0f : (1.0f - cosW0) / 2.0f;
      b0 = edge / a0;
      b1 = (highPass ? -2.0f : 2.0f) * edge / a0;
      b2 = b0;
      a1 = -2.0f * cosW0 / a0;
      a2 = (1.0f - alpha) / a0;
    }

    void reset() { z1 = z2 = 0.0f; }

    float process(float x) {
      const float y = b0 * x + z1;
      z1 = b1 * x - a1 * y + z2;
      z2 = b2 * x - a2 * y;
      return y;
    }

    float b0 = 1.0f, b1 = 0.0f, b2 = 0.0f, a1 = 0.0f, a2 = 0.0f;
    float z1 = 0.0f, z2 = 0.0f;
  };

  Biquad highPass_;
  Biquad lowPass_;
};

}  // namespace cadence
