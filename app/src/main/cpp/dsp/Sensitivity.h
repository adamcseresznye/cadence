#pragma once

#include <algorithm>
#include <array>

namespace cadence {

// Sensitivity table (DESIGN.md section 9). Levels outside 0..10 are clamped.
inline int vadModeForLevel(int level) {
  static constexpr std::array<int, 11> kModes = {3, 3, 3, 2, 2, 2, 1, 1, 1, 0, 0};
  return kModes[std::clamp(level, 0, 10)];
}

inline float marginDbForLevel(int level) {
  static constexpr std::array<float, 11> kMargins = {20.0f, 18.5f, 17.0f, 15.5f,
                                                     14.0f, 12.5f, 11.0f, 9.5f,
                                                     8.0f,  6.5f,  5.0f};
  return kMargins[std::clamp(level, 0, 10)];
}

}  // namespace cadence
