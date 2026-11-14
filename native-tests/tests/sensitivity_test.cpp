#include <gtest/gtest.h>

#include "dsp/Sensitivity.h"

namespace cadence {

TEST(Sensitivity, EndpointsAndMiddle) {
  EXPECT_EQ(vadModeForLevel(0), 3);
  EXPECT_FLOAT_EQ(marginDbForLevel(0), 20.0f);
  EXPECT_EQ(vadModeForLevel(5), 2);
  EXPECT_FLOAT_EQ(marginDbForLevel(5), 12.5f);
  EXPECT_EQ(vadModeForLevel(10), 0);
  EXPECT_FLOAT_EQ(marginDbForLevel(10), 5.0f);
}

TEST(Sensitivity, OutOfRangeIsClamped) {
  EXPECT_EQ(vadModeForLevel(-3), vadModeForLevel(0));
  EXPECT_FLOAT_EQ(marginDbForLevel(-3), marginDbForLevel(0));
  EXPECT_EQ(vadModeForLevel(99), vadModeForLevel(10));
  EXPECT_FLOAT_EQ(marginDbForLevel(99), marginDbForLevel(10));
}

TEST(Sensitivity, MarginsDecreaseWithLevel) {
  for (int level = 1; level <= 10; ++level) {
    EXPECT_LT(marginDbForLevel(level), marginDbForLevel(level - 1));
  }
}

}  // namespace cadence
