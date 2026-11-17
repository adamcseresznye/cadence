#include "AudioEngine.h"

#include <algorithm>
#include <cmath>

#include "dsp/Sensitivity.h"

namespace cadence {

namespace {

constexpr int kMaxDiscardReads = 64;

oboe::InputPreset presetFor(int index) {
  switch (index) {
    case 3:
      return oboe::InputPreset::Unprocessed;
    default:
      return oboe::InputPreset::VoiceRecognition;
  }
}

void configureCommon(oboe::AudioStreamBuilder& builder, oboe::Direction direction) {
  builder.setDirection(direction)
      ->setFormat(oboe::AudioFormat::Float)
      ->setChannelCount(oboe::ChannelCount::Mono)
      ->setSampleRate(kSampleRate)
      ->setPerformanceMode(oboe::PerformanceMode::LowLatency)
      ->setSharingMode(oboe::SharingMode::Exclusive)
      ->setSampleRateConversionQuality(oboe::SampleRateConversionQuality::Medium);
}

float rmsDb(const float* x, int32_t n) {
  double sum = 0.0;
  for (int32_t i = 0; i < n; ++i) {
    sum += static_cast<double>(x[i]) * x[i];
  }
  const double rms = std::sqrt(sum / n);
  const double db = 20.0 * std::log10(std::max(rms, 1e-9));
  return static_cast<float>(std::max(db, static_cast<double>(kLevelFloorDb)));
}

}  // namespace

class AudioEngine::DataCallback : public oboe::AudioStreamDataCallback {
 public:
  explicit DataCallback(AudioEngine* engine) : engine_(engine) {}

  oboe::DataCallbackResult onAudioReady(oboe::AudioStream* /* stream */,
                                        void* audioData,
                                        int32_t numFrames) override {
    engine_->process(static_cast<float*>(audioData), numFrames);
    return oboe::DataCallbackResult::Continue;
  }

 private:
  AudioEngine* engine_;
};

class AudioEngine::ErrorCallback : public oboe::AudioStreamErrorCallback {
 public:
  explicit ErrorCallback(AudioEngine* engine) : engine_(engine) {}

  void onErrorAfterClose(oboe::AudioStream* /* stream */, oboe::Result /* error */) override {
    engine_->state_.store(static_cast<int>(EngineState::Error));
  }

 private:
  AudioEngine* engine_;
};

AudioEngine::~AudioEngine() {
  stop();
}

int AudioEngine::start(const EngineConfig& config) {
  std::lock_guard<std::mutex> lock(streamsMutex_);
  closeStreams();
  state_.store(static_cast<int>(EngineState::Idle));
  scratch_.assign(kMaxBlock, 0.0f);
  delayLine_.reset(requestedDelaySamples_.load(std::memory_order_relaxed));
  underruns_.store(0);
  inputRmsDb_.store(kLevelFloorDb);
  firstCallback_ = true;
  if (!vad_.ok()) {
    return fail(kStartStreamStartFailed);
  }
  vad_.reset();
  timeline_.reset();
  analyzer_.reset();
  gate_.configure(kDefaultHoldFrames, kAttackMs, kDefaultReleaseMs);
  gate_.reset();
  pitch_.reset();
  limiter_.reset();
  leveler_.reset();
  masking_.reset();
  speechFilter_.reset();
  appliedOutputGainDb_ = kMaxOutputGainDb + 1;
  muted_.store(false);
  sampleIndex_ = 0;
  appliedSensitivity_ = -1;
  dataCallback_ = std::make_shared<DataCallback>(this);
  errorCallback_ = std::make_shared<ErrorCallback>(this);
  const int code = openStreams(config, dataCallback_, errorCallback_);
  if (code != kStartOk) {
    return fail(code);
  }

  if (input_->requestStart() != oboe::Result::OK) {
    return fail(kStartStreamStartFailed);
  }
  if (output_->requestStart() != oboe::Result::OK) {
    return fail(kStartStreamStartFailed);
  }

  state_.store(static_cast<int>(EngineState::Running));
  return kStartOk;
}

void AudioEngine::setSensitivityLevel(int level) {
  sensitivityLevel_.store(level);
}

void AudioEngine::setGateEnabled(bool enabled) {
  gateEnabled_.store(enabled);
}

void AudioEngine::setPitchSemitones(int semitones) {
  pitchSemitones_.store(std::clamp(semitones, kMinPitchSemitones, kMaxPitchSemitones));
}

void AudioEngine::setOutputGainDb(int db) {
  outputGainDb_.store(std::clamp(db, kMinOutputGainDb, kMaxOutputGainDb));
}

void AudioEngine::setMuted(bool muted) {
  muted_.store(muted);
}

void AudioEngine::setMaskingDb(int db) {
  maskingDb_.store(db);
}

void AudioEngine::stop() {
  std::lock_guard<std::mutex> lock(streamsMutex_);
  closeStreams();
  state_.store(static_cast<int>(EngineState::Idle));
}

float AudioEngine::latencyMs() {
  std::lock_guard<std::mutex> lock(streamsMutex_);
  if (!input_ || !output_ || state() != EngineState::Running) {
    return -1.0f;
  }
  const auto in = input_->calculateLatencyMillis();
  const auto out = output_->calculateLatencyMillis();
  if (!in || !out) {
    return -1.0f;
  }
  return static_cast<float>(in.value() + out.value());
}

void AudioEngine::setDelayMs(int ms) {
  const int clamped = std::clamp(ms, kMinDelayMs, kMaxDelayMs);
  requestedDelaySamples_.store(clamped * kSamplesPerMs, std::memory_order_relaxed);
}

int AudioEngine::fail(int code) {
  closeStreams();
  state_.store(static_cast<int>(EngineState::Error));
  return code;
}

void AudioEngine::closeStreams() {
  if (output_) {
    output_->requestStop();
    output_->close();
    output_.reset();
  }
  if (input_) {
    input_->requestStop();
    input_->close();
    input_.reset();
  }
}

void AudioEngine::discardPendingInput(int32_t numFrames) {
  for (int i = 0; i < kMaxDiscardReads; ++i) {
    auto result = input_->read(scratch_.data(), numFrames, 0);
    if (!result || result.value() == 0) {
      return;
    }
  }
}

void AudioEngine::process(float* out, int32_t numFrames) {
  if (numFrames <= 0) {
    return;
  }
  if (numFrames > kMaxBlock || !input_) {
    std::fill_n(out, numFrames, 0.0f);
    return;
  }
  if (firstCallback_) {
    firstCallback_ = false;
    discardPendingInput(numFrames);
  }

  float* in = scratch_.data();
  auto result = input_->read(in, numFrames, 0);
  const int32_t framesRead = result ? result.value() : 0;
  if (framesRead < numFrames) {
    std::fill(in + framesRead, in + numFrames, 0.0f);
    underruns_.fetch_add(1);
  }

  inputRmsDb_.store(rmsDb(in, numFrames));

  applySettings();
  analyzer_.push(in, numFrames);
  leveler_.setSpeechLevelDb(analyzer_.speechLevelDb());
  const float lastGain = renderGated(in, out, numFrames);

  gateOpen_.store(lastGain > 0.5f);
  floorDb_.store(analyzer_.floorDb());
  speechDetected_.store(analyzer_.lastSpeech());
}

void AudioEngine::applySettings() {
  const int requested = requestedDelaySamples_.load(std::memory_order_relaxed);
  if (requested != delayLine_.targetDelay()) {
    delayLine_.setDelay(requested);
  }
  const int level = sensitivityLevel_.load();
  if (level != appliedSensitivity_) {
    appliedSensitivity_ = level;
    vad_.setMode(vadModeForLevel(level));
    analyzer_.setMarginDb(marginDbForLevel(level));
  }
  const int pitch = pitchSemitones_.load();
  if (pitch != appliedPitch_) {
    appliedPitch_ = pitch;
    pitch_.setSemitones(pitch);
  }
  const int gainDb = outputGainDb_.load();
  if (gainDb != appliedOutputGainDb_) {
    appliedOutputGainDb_ = gainDb;
    outputGain_ = std::pow(10.0f, static_cast<float>(gainDb) / 20.0f);
  }
  const int maskingDb = maskingDb_.load();
  if (maskingDb != appliedMaskingDb_) {
    appliedMaskingDb_ = maskingDb;
    masking_.setGainDb(maskingDb);
  }
}

float AudioEngine::renderGated(const float* in, float* out, int32_t numFrames) {
  const bool gateOn = gateEnabled_.load();
  const bool muted = muted_.load();
  float gain = 1.0f;
  for (int32_t i = 0; i < numFrames; ++i) {
    const int delay = delayLine_.gateDelay();
    const float delayed = delayLine_.process(in[i]);
    const int64_t playPos = sampleIndex_ - delay;
    gain = gateOn ? gate_.nextGain(playPos, timeline_) : 1.0f;
    // The masking noise follows the gate, so it sounds only while the voice does.
    const float voice = leveler_.process(pitch_.process(speechFilter_.process(delayed))) * outputGain_;
    const float processed = limiter_.process((voice + masking_.process()) * gain);
    out[i] = muted ? 0.0f : processed;
    ++sampleIndex_;
  }
  return gain;
}

int AudioEngine::openStreams(const EngineConfig& config,
                             std::shared_ptr<oboe::AudioStreamDataCallback> dataCallback,
                             std::shared_ptr<oboe::AudioStreamErrorCallback> errorCallback) {
  oboe::AudioStreamBuilder inputBuilder;
  configureCommon(inputBuilder, oboe::Direction::Input);
  inputBuilder.setInputPreset(presetFor(config.inputPreset))
      ->setDeviceId(config.inputDeviceId);
  if (inputBuilder.openStream(input_) != oboe::Result::OK) {
    return kStartInputOpenFailed;
  }

  oboe::AudioStreamBuilder outputBuilder;
  configureCommon(outputBuilder, oboe::Direction::Output);
  outputBuilder.setUsage(oboe::Usage::Media)
      ->setContentType(oboe::ContentType::Speech)
      ->setDeviceId(config.outputDeviceId)
      ->setDataCallback(dataCallback)
      ->setErrorCallback(errorCallback);
  if (outputBuilder.openStream(output_) != oboe::Result::OK) {
    return kStartOutputOpenFailed;
  }

  if (input_->getSampleRate() != kSampleRate || output_->getSampleRate() != kSampleRate) {
    return kStartBadSampleRate;
  }
  sampleRate_.store(output_->getSampleRate());
  framesPerBurst_.store(output_->getFramesPerBurst());
  output_->setBufferSizeInFrames(2 * output_->getFramesPerBurst());
  return kStartOk;
}

}  // namespace cadence
