#include <gtest/gtest.h>

#include <cmath>

#include "dsp/Constants.h"
#include "dsp/OutputLimiter.h"

namespace cadence {

TEST(OutputLimiter, QuietUnchanged) {
  OutputLimiter limiter;
  for (int i = 0; i < 4800; ++i) {
    EXPECT_EQ(limiter.process(0.5f), 0.5f);
  }
}

TEST(OutputLimiter, LoudIsBounded) {
  OutputLimiter limiter;
  for (int n = 0; n < 48000; ++n) {
    const float v = static_cast<float>(std::sin(6.28318530717958647692 * 1000.0 * n / kSampleRate));
    EXPECT_LE(std::fabs(limiter.process(v)), kLimiterCeiling + 1e-6f);
  }
}

TEST(OutputLimiter, ReleaseRecovers) {
  OutputLimiter limiter;
  for (int i = 0; i < 4800; ++i) limiter.process(1.0f);
  float last = 0.0f;
  for (int i = 0; i < 48000; ++i) last = limiter.process(0.1f);
  EXPECT_NEAR(last, 0.1f, 1e-3f);
}

}  // namespace cadence
