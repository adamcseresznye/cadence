#include <gtest/gtest.h>

#include <cstdint>
#include <vector>

#include "dsp/Constants.h"
#include "dsp/GateStage.h"
#include "dsp/SpeechTimeline.h"

using cadence::GateStage;
using cadence::SpeechTimeline;

namespace {
constexpr int kFrame = cadence::kVadFrameSamples;
}  // namespace

TEST(GateStage, Scenario) {
  SpeechTimeline timeline;
  for (int f = 0; f < 100; ++f) {
    timeline.push(f >= 10 && f <= 19);
  }
  GateStage gate;
  gate.configure(20, 5.0f, 150.0f);
  gate.reset();

  const int64_t samples = 48000;
  std::vector<float> gains(samples);
  for (int64_t p = 0; p < samples; ++p) {
    gains[p] = gate.nextGain(p, timeline);
  }

  for (int64_t p = 0; p < 8 * kFrame; ++p) {
    EXPECT_EQ(gains[p], 0.0f) << "sample " << p;
  }
  EXPECT_NEAR(gains[8 * kFrame], 1.0f / 240.0f, 0.00001f);
  for (int64_t p = 8 * kFrame + kFrame - 1; p < 40 * kFrame; ++p) {
    EXPECT_GE(gains[p], 0.9999f) << "sample " << p;
  }
  EXPECT_NEAR(gains[40 * kFrame], 1.0f - 1.0f / 7200.0f, 0.0001f);
  for (int64_t p = 56 * kFrame; p < samples; ++p) {
    EXPECT_LT(gains[p], 0.001f) << "sample " << p;
  }
  for (int64_t p = 0; p < samples; ++p) {
    EXPECT_GE(gains[p], 0.0f) << "sample " << p;
    EXPECT_LE(gains[p], 1.0f) << "sample " << p;
  }
}

TEST(GateStage, NegativePositionIsClosed) {
  SpeechTimeline timeline;
  for (int f = 0; f < 10; ++f) {
    timeline.push(true);
  }
  GateStage gate;
  gate.configure(20, 5.0f, 150.0f);
  gate.reset();

  EXPECT_EQ(gate.nextGain(-5, timeline), 0.0f);
  EXPECT_EQ(gate.nextGain(-1, timeline), 0.0f);
}
