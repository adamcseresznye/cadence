#include <array>
#include <cstdint>

#include <gtest/gtest.h>

#include "dsp/Constants.h"
#include "dsp/FvadVad.h"

namespace cadence {

TEST(FvadVad, OkAfterCreation) {
  FvadVad vad;
  EXPECT_TRUE(vad.ok());
}

TEST(FvadVad, SilenceIsNeverSpeech) {
  FvadVad vad;
  std::array<int16_t, kVadFrameSamples> frame{};
  for (int i = 0; i < 100; ++i) {
    EXPECT_FALSE(vad.isSpeech(frame.data(), kVadFrameSamples));
  }
}

}  // namespace cadence
