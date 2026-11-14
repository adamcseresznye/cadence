#include <gtest/gtest.h>

#include "dsp/SpeechTimeline.h"

using cadence::SpeechTimeline;

TEST(SpeechTimeline, PushAndRead) {
  SpeechTimeline timeline;
  timeline.push(true);
  timeline.push(false);
  timeline.push(true);
  EXPECT_TRUE(timeline.isSpeech(0));
  EXPECT_FALSE(timeline.isSpeech(1));
  EXPECT_TRUE(timeline.isSpeech(2));
  EXPECT_FALSE(timeline.isSpeech(3));
  EXPECT_FALSE(timeline.isSpeech(-1));
}

TEST(SpeechTimeline, WrapAround) {
  SpeechTimeline timeline;
  for (int i = 0; i < 1100; ++i) {
    timeline.push(i % 2 == 0);
  }
  EXPECT_EQ(timeline.framesCompleted(), 1100);
  EXPECT_FALSE(timeline.isSpeech(75));
  EXPECT_TRUE(timeline.isSpeech(76));
  EXPECT_TRUE(timeline.isSpeech(1098));
  EXPECT_FALSE(timeline.isSpeech(1099));
}
