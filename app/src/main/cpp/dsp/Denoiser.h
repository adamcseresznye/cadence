#pragma once

#include <array>

#include "Constants.h"

#include "rnnoise.h"

namespace cadence {

// Noise reduction with RNNoise (DESIGN.md section 35). Sample in, sample out, always
// kDenoiserLatencySamples late, whether it is enabled or not, so it can be switched while running.
// process() and setEnabled() are for the audio callback. The constructor and reset() allocate.
class Denoiser {
 public:
  Denoiser() { create(); }

  ~Denoiser() {
    if (state_) rnnoise_destroy(state_);
  }

  Denoiser(const Denoiser&) = delete;
  Denoiser& operator=(const Denoiser&) = delete;

  bool ok() const { return state_ != nullptr; }

  // Not for the callback: frees and allocates the RNNoise state.
  void reset() {
    if (state_) rnnoise_destroy(state_);
    create();
    in_.fill(0.0f);
    dry_.fill(0.0f);
    out_.fill(0.0f);
    pos_ = 0;
    wet_ = 0.0f;
  }

  void setEnabled(bool enabled) { enabled_ = enabled; }

  float process(float x) {
    const float y = out_[pos_];
    in_[pos_] = x;
    if (++pos_ == kVadFrameSamples) {
      pos_ = 0;
      processFrame();
    }
    return y;
  }

 private:
  static constexpr float kPcmScale = 32768.0f;  // RNNoise works in 16-bit sample units

  void create() {
    state_ = rnnoise_create(nullptr);
    if (!state_) return;
    // RNNoise builds its shared tables on the first frame; do that here, not in the callback.
    std::array<float, kVadFrameSamples> silence{};
    rnnoise_process_frame(state_, silence.data(), silence.data());
  }

  // out_ = the previous input frame (dry), crossfaded over one frame to or from its denoised version.
  void processFrame() {
    const float target = enabled_ && state_ ? 1.0f : 0.0f;
    const bool run = wet_ > 0.0f || target > 0.0f;
    if (run) {
      for (int i = 0; i < kVadFrameSamples; ++i) scaled_[i] = in_[i] * kPcmScale;
      rnnoise_process_frame(state_, scaled_.data(), scaled_.data());
    }
    for (int i = 0; i < kVadFrameSamples; ++i) {
      const float mix = wet_ + (target - wet_) * static_cast<float>(i + 1) / kVadFrameSamples;
      const float clean = run ? scaled_[i] / kPcmScale : dry_[i];
      out_[i] = dry_[i] + mix * (clean - dry_[i]);
    }
    wet_ = target;
    dry_ = in_;
  }

  DenoiseState* state_ = nullptr;
  std::array<float, kVadFrameSamples> in_{};
  std::array<float, kVadFrameSamples> dry_{};
  std::array<float, kVadFrameSamples> out_{};
  std::array<float, kVadFrameSamples> scaled_{};
  int pos_ = 0;
  float wet_ = 0.0f;  // 0 = dry, 1 = denoised
  bool enabled_ = false;
};

}  // namespace cadence
