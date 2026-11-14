#include <gtest/gtest.h>

#include <cmath>

#include "dsp/Constants.h"
#include "dsp/VoiceLeveler.h"

namespace cadence {

namespace {

// Feeds two seconds (10 smoothing time constants) of a constant 0.01 signal and returns the last output.
float settle(VoiceLeveler& leveler) {
  float last = 0.0f;
  for (int i = 0; i < 2 * kSampleRate; ++i) last = leveler.process(0.01f);
  return last;
}

}  // namespace

TEST(VoiceLeveler, StartsAtUnity) {
  VoiceLeveler leveler;
  EXPECT_FLOAT_EQ(settle(leveler), 0.01f);
}

TEST(VoiceLeveler, QuietSpeechIsBoosted) {
  VoiceLeveler leveler;
  leveler.setSpeechLevelDb(kVoiceTargetDb - 12.0f);
  EXPECT_NEAR(settle(leveler), 0.01f * std::pow(10.0f, 12.0f / 20.0f), 1e-4f);
}

TEST(VoiceLeveler, LoudSpeechIsNotAttenuated) {
  VoiceLeveler leveler;
  leveler.setSpeechLevelDb(kVoiceTargetDb + 10.0f);
  EXPECT_FLOAT_EQ(settle(leveler), 0.01f);
}

TEST(VoiceLeveler, BoostIsCapped) {
  VoiceLeveler leveler;
  leveler.setSpeechLevelDb(-90.0f);
  EXPECT_NEAR(settle(leveler), 0.01f * std::pow(10.0f, kMaxVoiceBoostDb / 20.0f), 1e-3f);
}

TEST(VoiceLeveler, ResetReturnsToUnity) {
  VoiceLeveler leveler;
  leveler.setSpeechLevelDb(-48.0f);
  settle(leveler);
  leveler.reset();
  EXPECT_FLOAT_EQ(leveler.process(0.01f), 0.01f);
}

}  // namespace cadence
