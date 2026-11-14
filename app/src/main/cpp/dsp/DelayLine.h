#pragma once

#include <algorithm>
#include <cstddef>
#include <cstdint>
#include <vector>

namespace cadence {

// Delay line with click-free delay changes (DESIGN.md section 6).
// Not thread-safe: use from the audio callback only.
class DelayLine {
 public:
  DelayLine(int maxDelaySamples, int fadeSamples)
      : maxDelay_(maxDelaySamples),
        fadeSamples_(fadeSamples),
        size_(bufferSizeFor(maxDelaySamples)),
        mask_(size_ - 1),
        buffer_(size_, 0.0f) {}

  void reset(int delaySamples = 0) {
    std::fill(buffer_.begin(), buffer_.end(), 0.0f);
    position_ = 0;
    setDelayImmediate(delaySamples);
  }

  void setDelayImmediate(int samples) {
    current_ = target_ = clamp(samples);
    fading_ = false;
  }

  void setDelay(int samples) { target_ = clamp(samples); }

  int targetDelay() const { return target_; }

  int gateDelay() const { return fading_ ? std::min(current_, fadeTo_) : current_; }

  int64_t position() const { return position_; }

  float process(float in) {
    buffer_[index(position_)] = in;
    if (!fading_ && target_ != current_) startFade();

    float out = tap(current_);
    if (fading_) {
      const float w = static_cast<float>(fadePos_) / static_cast<float>(fadeSamples_);
      out = (1.0f - w) * out + w * tap(fadeTo_);
      if (++fadePos_ == fadeSamples_) {
        current_ = fadeTo_;
        fading_ = false;
      }
    }
    ++position_;
    return out;
  }

  void process(const float* in, float* out, int numFrames) {
    for (int i = 0; i < numFrames; ++i) {
      out[i] = process(in[i]);
    }
  }

 private:
  static size_t bufferSizeFor(int maxDelaySamples) {
    size_t size = 1;
    while (size < static_cast<size_t>(maxDelaySamples) + 1) {
      size <<= 1;
    }
    return size;
  }

  int clamp(int samples) const { return std::clamp(samples, 0, maxDelay_); }

  size_t index(int64_t sample) const { return static_cast<size_t>(sample) & mask_; }

  // Input sample written `delay` samples ago.
  float tap(int delay) const {
    return buffer_[index(position_ + static_cast<int64_t>(size_) - delay)];
  }

  void startFade() {
    fadeTo_ = target_;
    fadePos_ = 0;
    fading_ = true;
  }

  int maxDelay_;
  int fadeSamples_;
  size_t size_;
  size_t mask_;
  std::vector<float> buffer_;

  int current_ = 0;
  int target_ = 0;
  int fadeTo_ = 0;
  int fadePos_ = 0;
  bool fading_ = false;
  int64_t position_ = 0;
};

}  // namespace cadence
