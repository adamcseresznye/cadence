#include <gtest/gtest.h>

#include <cmath>

#include "dsp/Constants.h"
#include "dsp/SpeechFilter.h"

namespace cadence {

namespace {

// Gain in dB for a sine of the given frequency, measured over the second half of one second.
double gainDb(double hz) {
  SpeechFilter filter;
  double in = 0.0;
  double out = 0.0;
  for (int i = 0; i < kSampleRate; ++i) {
    const float x = static_cast<float>(std::sin(2.0 * 3.14159265358979 * hz * i / kSampleRate));
    const float y = filter.process(x);
    if (i >= kSampleRate / 2) {
      in += static_cast<double>(x) * x;
      out += static_cast<double>(y) * y;
    }
  }
  return 10.0 * std::log10(out / in);
}

}  // namespace

TEST(SpeechFilter, PassesTheSpeechBand) {
  EXPECT_NEAR(gainDb(300.0), 0.0, 0.5);
  EXPECT_NEAR(gainDb(1000.0), 0.0, 0.5);
  EXPECT_NEAR(gainDb(3000.0), 0.0, 0.5);
}

TEST(SpeechFilter, CutoffsAreThreeDbDown) {
  EXPECT_NEAR(gainDb(kVoiceHighPassHz), -3.0, 0.5);
  EXPECT_NEAR(gainDb(kVoiceLowPassHz), -3.0, 0.5);
}

TEST(SpeechFilter, RemovesMainsHum) {
  EXPECT_LT(gainDb(50.0), -14.0);
}

TEST(SpeechFilter, ReducesHighHiss) {
  EXPECT_LT(gainDb(12000.0), -12.0);
}

TEST(SpeechFilter, BlocksDc) {
  SpeechFilter filter;
  float last = 1.0f;
  for (int i = 0; i < kSampleRate; ++i) last = filter.process(0.5f);
  EXPECT_NEAR(last, 0.0f, 1e-4f);
}

TEST(SpeechFilter, ResetClearsState) {
  SpeechFilter filter;
  for (int i = 0; i < 1000; ++i) filter.process(0.5f);
  filter.reset();
  EXPECT_EQ(filter.process(0.0f), 0.0f);
}

}  // namespace cadence
