#include <gtest/gtest.h>

#include "dsp/Constants.h"

TEST(Smoke, VadFrameIsTenMilliseconds) {
    EXPECT_EQ(cadence::kVadFrameSamples, cadence::kSampleRate / 100);
}

TEST(Smoke, DelayRangeIsValid) {
    EXPECT_LT(cadence::kMinDelayMs, cadence::kMaxDelayMs);
}
