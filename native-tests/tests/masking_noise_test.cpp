#include <gtest/gtest.h>

#include <cmath>

#include "dsp/Constants.h"
#include "dsp/MaskingNoise.h"

namespace cadence {

TEST(MaskingNoise, OffIsSilent) {
  MaskingNoise noise;
  for (int i = 0; i < kSampleRate; ++i) EXPECT_EQ(noise.process(), 0.0f);
  noise.setGainDb(kMaskingOffDb);
  for (int i = 0; i < kSampleRate; ++i) EXPECT_EQ(noise.process(), 0.0f);
}

TEST(MaskingNoise, LevelMatchesGainAndStaysBelowIt) {
  MaskingNoise noise;
  noise.setGainDb(-20);
  const float gain = 0.1f;
  for (int i = 0; i < kSampleRate; ++i) noise.process();  // 20 smoothing time constants
  double sum = 0.0;
  for (int i = 0; i < kSampleRate; ++i) {
    const float x = noise.process();
    EXPECT_LE(std::fabs(x), gain * 1.0001f);
    sum += static_cast<double>(x) * x;
  }
  // Uniform noise in [-gain, gain] has RMS gain / sqrt(3).
  EXPECT_NEAR(std::sqrt(sum / kSampleRate), gain / std::sqrt(3.0), 0.05 * gain);
}

TEST(MaskingNoise, GainIsCappedAtMaximum) {
  MaskingNoise noise;
  noise.setGainDb(0);
  const float cap = std::pow(10.0f, kMaxMaskingDb / 20.0f);
  for (int i = 0; i < 2 * kSampleRate; ++i) EXPECT_LE(std::fabs(noise.process()), cap * 1.0001f);
}

TEST(MaskingNoise, ResetStartsFromSilence) {
  MaskingNoise noise;
  noise.setGainDb(-10);
  for (int i = 0; i < kSampleRate; ++i) noise.process();
  noise.reset();
  EXPECT_LT(std::fabs(noise.process()), 0.001f);
}

}  // namespace cadence
