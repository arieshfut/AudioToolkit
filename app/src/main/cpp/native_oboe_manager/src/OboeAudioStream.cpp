#include <jni.h>
#include <string>
#include <utility>
#include <OboeAudioStream.h>

#ifdef LOG_TAG
#undef LOG_TAG
#define LOG_TAG "OboeAudioStream"
#endif


#define MAX_RESTART_LIMIT           (5)
#define SLEEP_MICRO_SECONDS         (20 * 1000)    // 20MS


oboe::AudioFormat bitToFormat(int bit) {
    oboe::AudioFormat format;
    switch (bit) {
        case 24:
            format = oboe::AudioFormat::I24;
            break;
        case 32:
            format = oboe::AudioFormat::I32;
            break;
        default:
            format = oboe::AudioFormat::I16;
            break;
    }
    return format;
}

OboeAudioStream::OboeAudioStream() :
        mAudioStream(nullptr),
        audioApi(oboe::AudioApi::Unspecified),
        deviceId(DEFAULT_OBOE_DEVICE_ID),
        shareMode(oboe::SharingMode::Shared),
        perform(oboe::PerformanceMode::LowLatency),
        srcLevel(oboe::SampleRateConversionQuality::Medium),
        format(oboe::AudioFormat::I16),
        sampleRate(DEFAULT_OBOE_SAMPLERATE),
        channelCount(DEFAULT_OBOE_CHANNEL),
        restartCount(0),
        state(STATE_DEFAULT) {
    // do something
}

bool OboeAudioStream::updateDeviceId(int devId) {
    deviceId = devId;

    if (state == STATE_START) {
        return restart();
    }
    return true;
}

void OboeAudioStream::onErrorBeforeClose(oboe::AudioStream *audioStream, oboe::Result error) {
    ALOGE("OboeAudioStream onErrorBeforeClose error = %s", oboe::convertToText(error));
}

void OboeAudioStream::onErrorAfterClose(oboe::AudioStream *audioStream, oboe::Result error) {
    ALOGE("OboeAudioStream onErrorAfterClose error = %s", oboe::convertToText(error));
    if (error == oboe::Result::ErrorDisconnected) {
        if (restartCount <= MAX_RESTART_LIMIT) {
            usleep(SLEEP_MICRO_SECONDS);
            restart();
            restartCount++;
        } else {
            ALOGE("onErrorAfterClose restart too much time.");
        }
    }
}

/**
 * AudioStreamRecorder
 */
AudioStreamRecorder::AudioStreamRecorder() : OboeAudioStream() {
    wavFile = new WavFile();
}

void
AudioStreamRecorder::setParameter(std::string recordDir, oboe::AudioApi api, int devId, int sample, int channel, int bit) {
    audioApi = api;
    deviceId = devId;
    format = bitToFormat(bit);
    sampleRate = sample;
    channelCount = channel;

    recordFileDir = std::move(recordDir);
    wavFile->setParam(recordFileDir, "oboe", sampleRate, channelCount, bit);
    restartCount = 0;
    state = STATE_INIT;
}

int AudioStreamRecorder::start() {
    std::lock_guard<std::mutex> lock(mLock);
    wavFile->open();
    oboe::AudioStreamBuilder builder;
    oboe::Result result = builder.setAudioApi(audioApi)
            ->setDirection(oboe::Direction::Input)
            ->setSharingMode(shareMode)
            ->setPerformanceMode(perform)
            ->setSampleRateConversionQuality(srcLevel)
            ->setDeviceId(deviceId)
            ->setFormat(format)
            ->setSampleRate(sampleRate)
            ->setChannelCount(channelCount)
            ->setDataCallback(this)
            ->openStream(mAudioStream);
    if (result == oboe::Result::OK && mAudioStream) {
        result = mAudioStream->requestStart();
    }

    state = (result == oboe::Result::OK ? STATE_START : STATE_ERROR);
    return static_cast<int>(result);
}

void AudioStreamRecorder::stop() {
    std::lock_guard<std::mutex> lock(mLock);
    if (mAudioStream && mAudioStream->getState() == oboe::StreamState::Started) {
        mAudioStream->requestStop();
        mAudioStream->close();
        mAudioStream.reset();
    }
    wavFile->close();
    state = STATE_STOP;
}

bool AudioStreamRecorder::restart() {
    stop();
    return (start() == 0);
}

oboe::DataCallbackResult
AudioStreamRecorder::onAudioReady(oboe::AudioStream *audioStream, void *audioData,
                                  int32_t numFrames) {
    if (audioData == nullptr || numFrames <= 0) {
        return oboe::DataCallbackResult::Continue;
    }

    if (wavFile->canWrite()) {
        wavFile->write(audioData, numFrames);
    }

    return oboe::DataCallbackResult::Continue;
}


/**
 * AudioStreamPlayer
 */
AudioStreamPlayer::AudioStreamPlayer() : OboeAudioStream() {
    sampleRate = 48000;
    format = oboe::AudioFormat::Float;
    mPhaseIncrement = kFrequent * kTwoPi / (double) sampleRate;
}

void AudioStreamPlayer::setParameter(oboe::AudioApi api, std::string path, int devId) {
    audioApi = api;
    playFile = std::move(path);
    deviceId = devId;
    restartCount = 0;

    state = STATE_INIT;
}

int AudioStreamPlayer::start() {
    std::lock_guard<std::mutex> lock(mLock);

    oboe::AudioStreamBuilder builder;
    oboe::Result result = builder.setAudioApi(audioApi)
            ->setDirection(oboe::Direction::Output)
            ->setSharingMode(shareMode)
            ->setPerformanceMode(perform)
            ->setSampleRateConversionQuality(srcLevel)
            ->setDeviceId(deviceId)
            ->setFormat(format)
            ->setSampleRate(sampleRate)
            ->setChannelCount(channelCount)
            ->setDataCallback(this)
            ->openStream(mAudioStream);
    if (result == oboe::Result::OK && mAudioStream) {
        result = mAudioStream->requestStart();
    }

    state = (result == oboe::Result::OK ? STATE_START : STATE_ERROR);
    return static_cast<int>(result);
}

void AudioStreamPlayer::stop() {
    std::lock_guard<std::mutex> lock(mLock);
    if (mAudioStream && mAudioStream->getState() == oboe::StreamState::Started) {
        mAudioStream->requestStop();
        mAudioStream->close();
        mAudioStream.reset();
    }
    state = STATE_STOP;
}

bool AudioStreamPlayer::restart() {
    stop();
    return (start() == 0);
}

oboe::DataCallbackResult
AudioStreamPlayer::onAudioReady(oboe::AudioStream *audioStream, void *audioData,
                                int32_t numFrames) {
    auto floatData = (float *) audioData;
    for (int i = 0; i < numFrames; i++) {
        float sampleValue = kAmplitude * sinf(mPhase);
        for (int j = 0; j < channelCount; j++) {
            floatData[i * channelCount + j] = sampleValue;
        }
        mPhase += mPhaseIncrement;
        if (mPhase >= kTwoPi) mPhase -= kTwoPi;
    }
    return oboe::DataCallbackResult::Continue;
}
