#include <gtest/gtest.h>

#include <cmath>
#include <vector>

#include "dsp/Constants.h"
#include "dsp/PitchShifter.h"

namespace cadence {

namespace {

constexpr double kTwoPi = 6.28318530717958647692;
constexpr int kAnalyseStart = 9600;

float inputSample(float hz, int n) {
  return 0.5f * static_cast<float>(std::sin(kTwoPi * hz * n / kSampleRate));
}

// One second of a sine at `hz`, pitch-shifted by `semitones`.
std::vector<float> shiftSine(float hz, int semitones) {
  PitchShifter shifter;
  shifter.setSemitones(semitones);
  std::vector<float> out(kSampleRate);
  for (int n = 0; n < kSampleRate; ++n) {
    out[n] = shifter.process(inputSample(hz, n));
  }
  return out;
}

// Frequency from positive-going zero crossings over samples 9600 to 47999.
double measuredHz(const std::vector<float>& y) {
  int crossings = 0;
  for (int n = kAnalyseStart + 1; n < kSampleRate; ++n) {
    if (y[n - 1] < 0.0f && y[n] >= 0.0f) ++crossings;
  }
  const double seconds = (kSampleRate - kAnalyseStart) / static_cast<double>(kSampleRate);
  return crossings / seconds;
}

// 200 Hz sine, +12 for 24000 samples, then switched to 0. Output is 48000 samples long.
std::vector<float> shiftThenZero() {
  PitchShifter shifter;
  shifter.setSemitones(12);
  std::vector<float> out(kSampleRate);
  for (int n = 0; n < kSampleRate; ++n) {
    if (n == 24000) shifter.setSemitones(0);
    out[n] = shifter.process(inputSample(200.0f, n));
  }
  return out;
}

}  // namespace

TEST(PitchShifter, ZeroIsBypass) {
  const std::vector<float> y = shiftSine(200.0f, 0);
  for (int n = 0; n < kSampleRate; ++n) {
    EXPECT_EQ(y[n], inputSample(200.0f, n)) << "sample " << n;
  }
}

TEST(PitchShifter, OctaveUp) {
  EXPECT_NEAR(measuredHz(shiftSine(200.0f, 12)), 400.0, 400.0 * 0.05);
}

TEST(PitchShifter, OctaveDown) {
  EXPECT_NEAR(measuredHz(shiftSine(200.0f, -12)), 100.0, 100.0 * 0.05);
}

TEST(PitchShifter, FiveUp) {
  EXPECT_NEAR(measuredHz(shiftSine(200.0f, 5)), 266.97, 266.97 * 0.05);
}

TEST(PitchShifter, OutputBounded) {
  for (float y : shiftSine(200.0f, 12)) {
    EXPECT_LE(std::fabs(y), 0.5f + 1e-4f);
  }
}

TEST(PitchShifter, SwitchToZeroEndsInBypass) {
  const std::vector<float> y = shiftThenZero();
  for (int n = 24000 + kDelayFadeSamples; n < kSampleRate; ++n) {
    EXPECT_EQ(y[n], inputSample(200.0f, n)) << "sample " << n;
  }
}

TEST(PitchShifter, FadeOutStillShifted) {
  const std::vector<float> y = shiftThenZero();
  bool allEqual = true;
  for (int n = 24000; n < 24000 + kDelayFadeSamples; ++n) {
    if (y[n] != inputSample(200.0f, n)) allEqual = false;
  }
  EXPECT_FALSE(allEqual);
}

}  // namespace cadence
