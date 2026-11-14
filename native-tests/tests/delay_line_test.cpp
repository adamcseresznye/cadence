#include <gtest/gtest.h>

#include <cmath>
#include <vector>

#include "dsp/DelayLine.h"

namespace {
constexpr double kPi = 3.14159265358979323846;
}

TEST(DelayLine, ImpulseArrivesExactlyNSamplesLater) {
    cadence::DelayLine line(1000, 64);
    line.setDelayImmediate(100);
    for (int i = 0; i < 300; ++i) {
        const float out = line.process(i == 0 ? 1.0f : 0.0f);
        EXPECT_EQ(out, i == 100 ? 1.0f : 0.0f) << "index " << i;
    }
}

TEST(DelayLine, ZeroDelayIsPassthrough) {
    cadence::DelayLine line(1000, 64);
    for (int n = 0; n < 100; ++n) {
        const float in = 0.001f * n;
        EXPECT_FLOAT_EQ(line.process(in), in) << "sample " << n;
    }
}

TEST(DelayLine, DelayIsClamped) {
    cadence::DelayLine line(1000, 64);
    line.setDelayImmediate(5000);
    EXPECT_EQ(line.targetDelay(), 1000);
    line.setDelayImmediate(-7);
    EXPECT_EQ(line.targetDelay(), 0);
}

TEST(DelayLine, CrossfadeEndsOnNewDelay) {
    cadence::DelayLine line(1000, 64);
    line.setDelayImmediate(100);
    std::vector<float> in(600);
    std::vector<float> out(600);
    for (int n = 0; n < 600; ++n) {
        in[n] = 0.001f * n;
        if (n == 200) {
            line.setDelay(200);
        }
        out[n] = line.process(in[n]);
    }
    for (int n = 100; n < 200; ++n) {
        EXPECT_NEAR(out[n], in[n - 100], 0.000001f) << "sample " << n;
    }
    for (int n = 264; n < 600; ++n) {
        EXPECT_NEAR(out[n], in[n - 200], 0.000001f) << "sample " << n;
    }
}

TEST(DelayLine, GateDelayIsSmallerTapDuringFade) {
    cadence::DelayLine line(1000, 64);
    line.setDelayImmediate(100);
    line.setDelay(300);
    EXPECT_EQ(line.gateDelay(), 100);
    for (int i = 0; i < 64; ++i) {
        line.process(0.0f);
    }
    EXPECT_EQ(line.gateDelay(), 300);
}

TEST(DelayLine, LatestRequestWinsDuringFade) {
    cadence::DelayLine line(1000, 64);
    line.setDelayImmediate(100);
    line.setDelay(200);
    line.setDelay(300);
    line.setDelay(150);
    EXPECT_EQ(line.targetDelay(), 150);
    for (int i = 0; i < 200; ++i) {
        line.process(0.0f);
    }
    EXPECT_EQ(line.gateDelay(), 150);
    EXPECT_EQ(line.targetDelay(), 150);
}

TEST(DelayLine, DelayChangeHasNoClick) {
    cadence::DelayLine line(10000, 960);
    line.setDelayImmediate(2000);
    const int total = 30000;
    std::vector<float> in(total);
    for (int n = 0; n < total; ++n) {
        in[n] = static_cast<float>(0.5 * std::sin(2.0 * kPi * 1000.0 * n / 48000.0));
    }
    std::vector<float> out(total);
    for (int n = 0; n < total; ++n) {
        if (n == 10000) {
            line.setDelay(4000);
        }
        out[n] = line.process(in[n]);
    }
    for (int n = 5001; n < total; ++n) {
        EXPECT_LT(std::fabs(out[n] - out[n - 1]), 0.1f) << "sample " << n;
    }
}
