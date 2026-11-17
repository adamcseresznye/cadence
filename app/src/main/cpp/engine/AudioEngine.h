#pragma once

#include <atomic>
#include <memory>
#include <mutex>
#include <vector>

#include <oboe/Oboe.h>

#include "dsp/Constants.h"
#include "dsp/DelayLine.h"
#include "dsp/FrameAnalyzer.h"
#include "dsp/FvadVad.h"
#include "dsp/GateStage.h"
#include "dsp/MaskingNoise.h"
#include "dsp/OutputLimiter.h"
#include "dsp/PitchShifter.h"
#include "dsp/SpeechFilter.h"
#include "dsp/SpeechTimeline.h"
#include "dsp/VoiceLeveler.h"

namespace cadence {

enum class EngineState { Idle = 0, Running = 1, Error = 2 };

constexpr int kStartOk = 0;
constexpr int kStartInputOpenFailed = 1;
constexpr int kStartOutputOpenFailed = 2;
constexpr int kStartStreamStartFailed = 3;
constexpr int kStartBadSampleRate = 4;

struct EngineConfig {
  int inputPreset = 1;
  int inputDeviceId = 0;
  int outputDeviceId = 0;
};

class AudioEngine {
 public:
  AudioEngine() = default;
  ~AudioEngine();

  AudioEngine(const AudioEngine&) = delete;
  AudioEngine& operator=(const AudioEngine&) = delete;

  int start(const EngineConfig& config);
  void stop();
  void setDelayMs(int ms);
  void setSensitivityLevel(int level);
  void setGateEnabled(bool enabled);
  void setPitchSemitones(int semitones);
  void setOutputGainDb(int db);
  void setMuted(bool muted);
  void setMaskingDb(int db);
  // Input + output latency reported by Oboe, or -1 when not running. Not for the callback.
  float latencyMs();

  EngineState state() const { return static_cast<EngineState>(state_.load()); }
  float inputRmsDb() const { return inputRmsDb_.load(); }
  int underruns() const { return underruns_.load(); }
  int sampleRate() const { return sampleRate_.load(); }
  int framesPerBurst() const { return framesPerBurst_.load(); }
  float floorDb() const { return floorDb_.load(); }
  bool gateOpen() const { return gateOpen_.load(); }
  bool speechDetected() const { return speechDetected_.load(); }

 private:
  class DataCallback;
  class ErrorCallback;

  void process(float* out, int32_t numFrames);
  void discardPendingInput(int32_t numFrames);
  void applySettings();
  float renderGated(const float* in, float* out, int32_t numFrames);
  int openStreams(const EngineConfig& config,
                  std::shared_ptr<oboe::AudioStreamDataCallback> dataCallback,
                  std::shared_ptr<oboe::AudioStreamErrorCallback> errorCallback);
  int fail(int code);
  void closeStreams();

  std::shared_ptr<oboe::AudioStream> input_;
  std::shared_ptr<oboe::AudioStream> output_;
  std::shared_ptr<DataCallback> dataCallback_;
  std::shared_ptr<ErrorCallback> errorCallback_;
  std::vector<float> scratch_;
  std::mutex streamsMutex_;  // guards input_/output_ against latencyMs(); never taken in the callback
  DelayLine delayLine_{kDelayLineCapacity, kDelayFadeSamples};
  FvadVad vad_;
  SpeechTimeline timeline_;
  FrameAnalyzer analyzer_{&vad_, &timeline_};
  GateStage gate_;
  PitchShifter pitch_;
  OutputLimiter limiter_;
  VoiceLeveler leveler_;
  MaskingNoise masking_;
  SpeechFilter speechFilter_;
  int appliedMaskingDb_ = kMaskingOffDb;
  float outputGain_ = 1.0f;
  int64_t sampleIndex_ = 0;
  int appliedSensitivity_ = -1;
  int appliedPitch_ = kDefaultPitchSemitones;
  int appliedOutputGainDb_ = kMaxOutputGainDb + 1;  // out of range: forces first apply
  bool firstCallback_ = true;

  std::atomic<int> requestedDelaySamples_{100 * kSamplesPerMs};
  std::atomic<int> state_{static_cast<int>(EngineState::Idle)};
  std::atomic<float> inputRmsDb_{kLevelFloorDb};
  std::atomic<int> underruns_{0};
  std::atomic<int> sampleRate_{0};
  std::atomic<int> framesPerBurst_{0};
  std::atomic<int> sensitivityLevel_{kDefaultSensitivity};
  std::atomic<bool> gateEnabled_{true};
  std::atomic<int> pitchSemitones_{kDefaultPitchSemitones};
  std::atomic<int> outputGainDb_{kDefaultOutputGainDb};
  std::atomic<bool> muted_{false};
  std::atomic<int> maskingDb_{kMaskingOffDb};
  std::atomic<float> floorDb_{kNoiseFloorStartDb};
  std::atomic<bool> gateOpen_{false};
  std::atomic<bool> speechDetected_{false};
};

}  // namespace cadence
