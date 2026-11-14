#pragma once

#include <algorithm>
#include <cstdint>

#include "Constants.h"
#include "SpeechTimeline.h"

namespace cadence {

// Gate with hold, attack and release, per output sample (DESIGN.md section 8).
// Not thread-safe: use from the audio callback only.
class GateStage {
 public:
  void configure(int holdFrames, float attackMs, float releaseMs) {
    holdFrames_ = holdFrames;
    attackStep_ = 1.0f / (attackMs * kSamplesPerMs);
    releaseStep_ = 1.0f / (releaseMs * kSamplesPerMs);
  }

  void reset() {
    gain_ = 0.0f;
    hold_ = 0;
    open_ = false;
    frame_ = -1;
  }

  float nextGain(int64_t playPos, const SpeechTimeline& timeline) {
    if (playPos < 0) {
      open_ = false;
      frame_ = -1;
    } else {
      const int64_t frame = playPos / kVadFrameSamples;
      if (frame != frame_) {
        frame_ = frame;
        decide(frame, timeline);
      }
    }
    if (open_) {
      gain_ = std::min(1.0f, gain_ + attackStep_);
    } else {
      gain_ = std::max(0.0f, gain_ - releaseStep_);
    }
    return gain_;
  }

 private:
  void decide(int64_t frame, const SpeechTimeline& timeline) {
    bool anySpeech = false;
    for (int k = 0; k < kLookaheadFrames; ++k) {
      anySpeech = anySpeech || timeline.isSpeech(frame + k);
    }
    if (anySpeech) {
      hold_ = holdFrames_;
      open_ = true;
    } else if (hold_ > 0) {
      --hold_;
      open_ = true;
    } else {
      open_ = false;
    }
  }

  int holdFrames_ = 0;
  float attackStep_ = 0.0f;
  float releaseStep_ = 0.0f;
  float gain_ = 0.0f;
  int hold_ = 0;
  bool open_ = false;
  int64_t frame_ = -1;
};

}  // namespace cadence
