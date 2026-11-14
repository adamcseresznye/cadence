#pragma once

#include <array>
#include <cstddef>
#include <cstdint>

namespace cadence {

// Speech flag per frame, remembering the last kCapacity frames (DESIGN.md section 7, step 7).
class SpeechTimeline {
 public:
  static constexpr int kCapacity = 1024;

  void reset() {
    flags_.fill(false);
    framesCompleted_ = 0;
  }

  void push(bool speech) {
    flags_[slot(framesCompleted_)] = speech;
    ++framesCompleted_;
  }

  int64_t framesCompleted() const { return framesCompleted_; }

  bool isSpeech(int64_t index) const {
    if (index < 0 || index >= framesCompleted_ || index < framesCompleted_ - kCapacity) {
      return false;
    }
    return flags_[slot(index)];
  }

 private:
  static size_t slot(int64_t frame) { return static_cast<size_t>(frame % kCapacity); }

  std::array<bool, kCapacity> flags_{};
  int64_t framesCompleted_ = 0;
};

}  // namespace cadence
