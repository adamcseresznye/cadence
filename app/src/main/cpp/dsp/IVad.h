#pragma once

#include <cstdint>

namespace cadence {

// Voice detector interface (DESIGN.md section 7, step 5).
class IVad {
 public:
  virtual ~IVad() = default;
  virtual void setMode(int mode) = 0;
  virtual bool isSpeech(const int16_t* frame, int numSamples) = 0;
};

}  // namespace cadence
