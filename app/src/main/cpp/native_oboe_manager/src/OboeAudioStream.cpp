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
    ALOGI("OboeAudioStream updateDeviceId and restart");
    return restart();
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
    ALOGI("AudioStreamRecorder start");
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
        ALOGI("AudioStreamRecorder start, ");
    }

    state = (result == oboe::Result::OK ? STATE_START : STATE_ERROR);
    return static_cast<int>(result);
}

void AudioStreamRecorder::stop() {
    ALOGI("AudioStreamRecorder stop");
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
    ALOGI("AudioStreamRecorder restart.");
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
    ALOGI("AudioStreamPlayer start, devId = %d", deviceId);
    std::lock_guard<std::mutex> lock(mLock);
    oboe::Result result = oboe::Result::OK;

    // It is possible for a stream's device to become disconnected during the open or between
    // the Open and the Start.
    // So if it fails to start, close the old stream and try again.
    int tryCount = 0;
    do {
        if (tryCount > 0) {
            usleep(20 * 1000); // Sleep between tries to give the system time to settle.
        }

        // open stream
        oboe::AudioStreamBuilder builder;
        result = builder.setAudioApi(audioApi)
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
        if (result == oboe::Result::OK) {
            ALOGI("Stream opened: AudioAPI = %d, channelCount = %d, deviceID = %d",
                 mAudioStream->getAudioApi(),
                 mAudioStream->getChannelCount(),
                 mAudioStream->getDeviceId());
            result = mAudioStream->requestStart();
            if (result != oboe::Result::OK) {
                ALOGE("Error starting playback stream. Error: %s", oboe::convertToText(result));
                mAudioStream->close();
                mAudioStream.reset();
            }
        } else {
            ALOGE("Error creating playback stream. Error: %s", oboe::convertToText(result));
        }
    } while (result != oboe::Result::OK && tryCount++ < 3);

    state = (result == oboe::Result::OK ? STATE_START : STATE_ERROR);
    return static_cast<int>(result);
}

void AudioStreamPlayer::stop() {
    ALOGI("AudioStreamPlayer stop");
    std::lock_guard<std::mutex> lock(mLock);
    if (mAudioStream && mAudioStream->getState() != oboe::StreamState::Stopped) {
        mAudioStream->requestStop();
        mAudioStream->close();
        mAudioStream.reset();
    }
    state = STATE_STOP;
}

bool AudioStreamPlayer::restart() {
    ALOGI("AudioStreamPlayer restart");
    stop();
    usleep(500 * 1000);
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
