#pragma once

#include <algorithm>
#include <cstddef>
#include <cstdint>

#include "Constants.h"
#include "IVad.h"

extern "C" {
#include "fvad.h"
}

namespace cadence {

// Voice detector backed by libfvad (DESIGN.md section 7, step 5).
class FvadVad : public IVad {
 public:
  FvadVad() : fvad_(fvad_new()) { configure(); }

  ~FvadVad() override {
    if (fvad_) fvad_free(fvad_);
  }

  FvadVad(const FvadVad&) = delete;
  FvadVad& operator=(const FvadVad&) = delete;

  bool ok() const { return fvad_ != nullptr; }

  void setMode(int mode) override {
    const int clamped = std::clamp(mode, 0, 3);
    if (clamped == mode_) return;
    mode_ = clamped;
    if (fvad_) fvad_set_mode(fvad_, mode_);
  }

  bool isSpeech(const int16_t* frame, int numSamples) override {
    return fvad_ && fvad_process(fvad_, frame, static_cast<size_t>(numSamples)) == 1;
  }

  void reset() {
    if (!fvad_) return;
    fvad_reset(fvad_);
    configure();
  }

 private:
  void configure() {
    if (!fvad_) return;
    fvad_set_sample_rate(fvad_, kSampleRate);
    fvad_set_mode(fvad_, mode_);
  }

  Fvad* fvad_;
  int mode_ = 2;
};

}  // namespace cadence
