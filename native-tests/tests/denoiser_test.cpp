#include <gtest/gtest.h>

#include <cmath>
#include <cstdint>
#include <vector>

#include "dsp/Constants.h"
#include "dsp/Denoiser.h"

namespace cadence {

namespace {

// Deterministic white noise in [-amplitude, amplitude].
struct Noise {
  uint32_t state = 12345u;
  float next(float amplitude) {
    state = state * 1664525u + 1013904223u;
    return amplitude * (static_cast<float>(state >> 8) / 8388608.0f - 1.0f);
  }
};

// A voiced-speech-like sound: 150 Hz with harmonics, loudness moving at 4 Hz (syllable rate).
float voiced(int i) {
  const double t = static_cast<double>(i) / kSampleRate;
  double sum = 0.0;
  for (int h = 1; h <= 20; ++h) sum += std::sin(2.0 * 3.14159265358979 * 150.0 * h * t) / h;
  return static_cast<float>(0.1 * sum * (0.6 + 0.4 * std::sin(2.0 * 3.14159265358979 * 4.0 * t)));
}

double rms(const std::vector<float>& x, int from) {
  double sum = 0.0;
  for (size_t i = from; i < x.size(); ++i) sum += static_cast<double>(x[i]) * x[i];
  return std::sqrt(sum / (x.size() - from));
}

}  // namespace

TEST(Denoiser, CreatesItsState) {
  Denoiser denoiser;
  EXPECT_TRUE(denoiser.ok());
}

TEST(Denoiser, DisabledIsAnExactDelay) {
  Denoiser denoiser;
  Noise noise;
  std::vector<float> in(4 * kSampleRate / 10);
  for (float& x : in) x = noise.next(0.3f);
  for (size_t i = 0; i < in.size(); ++i) {
    const float y = denoiser.process(in[i]);
    const float expected = i < kDenoiserLatencySamples ? 0.0f : in[i - kDenoiserLatencySamples];
    ASSERT_EQ(y, expected) << "sample " << i;
  }
}

TEST(Denoiser, EnabledReducesSteadyNoise) {
  Denoiser denoiser;
  denoiser.setEnabled(true);
  Noise noise;
  std::vector<float> in(3 * kSampleRate), out(3 * kSampleRate);
  for (size_t i = 0; i < in.size(); ++i) {
    in[i] = noise.next(0.05f);
    out[i] = denoiser.process(in[i]);
  }
  // Measured over the last second, after RNNoise has settled.
  const double reductionDb = 20.0 * std::log10(rms(in, 2 * kSampleRate) / rms(out, 2 * kSampleRate));
  EXPECT_GT(reductionDb, 10.0) << "reduction " << reductionDb << " dB";
}

TEST(Denoiser, EnabledKeepsVoicedSoundAtTheSameDelay) {
  Denoiser denoiser;
  denoiser.setEnabled(true);
  const int n = 2 * kSampleRate;
  std::vector<float> in(n), out(n);
  for (int i = 0; i < n; ++i) {
    in[i] = voiced(i);
    out[i] = denoiser.process(in[i]);
  }
  // The lag with the highest correlation over the second second is the latency.
  int bestLag = -1;
  double best = -1e30;
  for (int lag = 0; lag <= 2 * kDenoiserLatencySamples; ++lag) {
    double sum = 0.0;
    for (int i = kSampleRate; i < n; ++i) sum += static_cast<double>(out[i]) * in[i - lag];
    if (sum > best) {
      best = sum;
      bestLag = lag;
    }
  }
  // One sample of tolerance: RNNoise's own input high-pass shifts the phase slightly.
  EXPECT_NEAR(bestLag, kDenoiserLatencySamples, 1);
  // The voiced sound is kept: less than 6 dB lost.
  EXPECT_GT(rms(out, kSampleRate), 0.5 * rms(in, kSampleRate));
}

TEST(Denoiser, SilenceStaysSilentAndFinite) {
  Denoiser denoiser;
  denoiser.setEnabled(true);
  for (int i = 0; i < kSampleRate; ++i) {
    const float y = denoiser.process(0.0f);
    ASSERT_TRUE(std::isfinite(y));
    ASSERT_LT(std::fabs(y), 1e-3f);
  }
}

TEST(Denoiser, ResetReturnsToSilence) {
  Denoiser denoiser;
  denoiser.setEnabled(true);
  for (int i = 0; i < kSampleRate; ++i) denoiser.process(voiced(i));
  denoiser.reset();
  // Nothing of the old sound is left: the first latency period is silent.
  for (int i = 0; i < kDenoiserLatencySamples; ++i) ASSERT_LT(std::fabs(denoiser.process(0.0f)), 1e-6f);
}

}  // namespace cadence
