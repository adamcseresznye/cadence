#include <gtest/gtest.h>

#include <algorithm>
#include <cmath>
#include <cstdint>
#include <cstdlib>
#include <vector>

#include "dsp/Constants.h"
#include "dsp/FrameAnalyzer.h"
#include "dsp/IVad.h"
#include "dsp/SpeechTimeline.h"

using cadence::FrameAnalyzer;
using cadence::IVad;
using cadence::SpeechTimeline;

namespace {

constexpr double kTwoPi = 6.283185307179586;

// Speech when any sample is louder than 3000 (about -20 dBFS).
class FakeVad : public IVad {
 public:
  void setMode(int mode) override { mode_ = mode; }
  bool isSpeech(const int16_t* frame, int numSamples) override {
    for (int i = 0; i < numSamples; ++i) {
      if (std::abs(frame[i]) > 3000) return true;
    }
    return false;
  }

 private:
  int mode_ = 0;
};

class AlwaysVad : public IVad {
 public:
  void setMode(int) override {}
  bool isSpeech(const int16_t*, int) override { return true; }
};

// 1000 Hz sine; 480 samples hold exactly 10 cycles, so every frame starts at the same phase.
std::vector<float> sineFrames(int frames, float amplitude) {
  std::vector<float> samples(frames * cadence::kVadFrameSamples);
  for (size_t i = 0; i < samples.size(); ++i) {
    samples[i] = amplitude * static_cast<float>(std::sin(kTwoPi * 1000.0 * i / cadence::kSampleRate));
  }
  return samples;
}

std::vector<float> silentFrames(int frames) {
  return std::vector<float>(frames * cadence::kVadFrameSamples, 0.0f);
}

std::vector<float> concat(std::vector<float> first, const std::vector<float>& second) {
  first.insert(first.end(), second.begin(), second.end());
  return first;
}

std::vector<float> toneAfterSilence() {
  return concat(silentFrames(100), sineFrames(50, 0.3f));
}

}  // namespace

TEST(FrameAnalyzer, SilenceGivesNoSpeechAndLowFloor) {
  FakeVad vad;
  SpeechTimeline timeline;
  FrameAnalyzer analyzer(&vad, &timeline);
  const auto signal = silentFrames(100);
  analyzer.push(signal.data(), static_cast<int>(signal.size()));

  EXPECT_EQ(timeline.framesCompleted(), 100);
  for (int i = 0; i < 100; ++i) {
    EXPECT_FALSE(timeline.isSpeech(i));
  }
  EXPECT_NEAR(analyzer.floorDb(), -90.0f, 0.5f);
}

TEST(FrameAnalyzer, ToneAfterSilenceIsSpeech) {
  FakeVad vad;
  SpeechTimeline timeline;
  FrameAnalyzer analyzer(&vad, &timeline);
  const auto signal = toneAfterSilence();
  analyzer.push(signal.data(), static_cast<int>(signal.size()));

  for (int i = 0; i < 100; ++i) {
    EXPECT_FALSE(timeline.isSpeech(i)) << "frame " << i;
  }
  for (int i = 100; i < 150; ++i) {
    EXPECT_TRUE(timeline.isSpeech(i)) << "frame " << i;
  }
}

TEST(FrameAnalyzer, BlockSizeDoesNotMatter) {
  FakeVad vad;
  const auto signal = toneAfterSilence();

  SpeechTimeline wholeTimeline;
  FrameAnalyzer whole(&vad, &wholeTimeline);
  for (size_t pos = 0; pos < signal.size(); pos += 480) {
    whole.push(signal.data() + pos, 480);
  }

  SpeechTimeline oddTimeline;
  FrameAnalyzer odd(&vad, &oddTimeline);
  const int total = static_cast<int>(signal.size());
  for (int pos = 0; pos < total; pos += 97) {
    odd.push(signal.data() + pos, std::min(97, total - pos));
  }

  ASSERT_EQ(wholeTimeline.framesCompleted(), 150);
  ASSERT_EQ(oddTimeline.framesCompleted(), 150);
  for (int i = 0; i < 150; ++i) {
    EXPECT_EQ(wholeTimeline.isSpeech(i), oddTimeline.isSpeech(i)) << "frame " << i;
  }
}

TEST(FrameAnalyzer, SpeechNeedsDetector) {
  FakeVad vad;
  SpeechTimeline timeline;
  FrameAnalyzer analyzer(&vad, &timeline);
  const auto signal = concat(silentFrames(100), sineFrames(50, 0.05f));
  analyzer.push(signal.data(), static_cast<int>(signal.size()));

  for (int i = 100; i < 150; ++i) {
    EXPECT_FALSE(timeline.isSpeech(i)) << "frame " << i;
  }
}

TEST(FrameAnalyzer, MinimumLevelIsRequired) {
  AlwaysVad vad;
  SpeechTimeline quietTimeline;
  FrameAnalyzer quiet(&vad, &quietTimeline);
  const auto quietSignal = concat(silentFrames(100), sineFrames(50, 0.0005f));
  quiet.push(quietSignal.data(), static_cast<int>(quietSignal.size()));
  for (int i = 100; i < 150; ++i) {
    EXPECT_FALSE(quietTimeline.isSpeech(i)) << "frame " << i;
  }

  SpeechTimeline loudTimeline;
  FrameAnalyzer loud(&vad, &loudTimeline);
  const auto loudSignal = concat(silentFrames(100), sineFrames(50, 0.3f));
  loud.push(loudSignal.data(), static_cast<int>(loudSignal.size()));
  for (int i = 100; i < 150; ++i) {
    EXPECT_TRUE(loudTimeline.isSpeech(i)) << "frame " << i;
  }
}

TEST(FrameAnalyzer, FloorRisesSlowly) {
  FakeVad vad;
  SpeechTimeline timeline;
  FrameAnalyzer analyzer(&vad, &timeline);
  const auto signal = concat(silentFrames(300), sineFrames(100, 0.3f));
  analyzer.push(signal.data(), static_cast<int>(signal.size()));

  EXPECT_LE(analyzer.floorDb(), -86.9f);
  EXPECT_GE(analyzer.floorDb(), -90.0f);
}

TEST(FrameAnalyzer, SpeechLevelTracksSpeechFrames) {
  AlwaysVad vad;
  SpeechTimeline timeline;
  FrameAnalyzer analyzer(&vad, &timeline);
  const auto signal = concat(silentFrames(100), sineFrames(200, 0.02f));  // RMS -37.0 dB
  analyzer.push(signal.data(), static_cast<int>(signal.size()));

  EXPECT_NEAR(analyzer.speechLevelDb(), -37.0f, 0.1f);
}

TEST(FrameAnalyzer, SilenceKeepsSpeechLevelAtTarget) {
  AlwaysVad vad;
  SpeechTimeline timeline;
  FrameAnalyzer analyzer(&vad, &timeline);
  const auto signal = silentFrames(100);
  analyzer.push(signal.data(), static_cast<int>(signal.size()));

  EXPECT_EQ(analyzer.speechLevelDb(), cadence::kVoiceTargetDb);
}
