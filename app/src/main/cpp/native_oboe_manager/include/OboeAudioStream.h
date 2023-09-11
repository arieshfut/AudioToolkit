//
// Created by Aries on 2023/9/20.
//

#pragma once

#include <audio_base.h>
#include <oboe/Oboe.h>
#include <WavFile.h>


class OboeAudioStream : public oboe::AudioStreamCallback {
public:
    OboeAudioStream();

    ~OboeAudioStream() = default;

    virtual bool updateDeviceId(int devId);
    virtual int start() = 0;
    virtual void stop() = 0;
    virtual bool restart() = 0;
    virtual oboe::DataCallbackResult onAudioReady(oboe::AudioStream *audioStream, void *audioData, int32_t numFrames) = 0;
    virtual void onErrorBeforeClose(oboe::AudioStream* audioStream, oboe::Result error);
    virtual void onErrorAfterClose(oboe::AudioStream* audioStream, oboe::Result error);

protected:
    std::shared_ptr<oboe::AudioStream>  mAudioStream;
    oboe::AudioApi audioApi;
    oboe::SharingMode shareMode;
    oboe::PerformanceMode perform;
    oboe::SampleRateConversionQuality srcLevel;
    oboe::AudioFormat format;
    int deviceId;
    int sampleRate;
    int channelCount;
    int restartCount;
    int state;
};

class AudioStreamRecorder : public OboeAudioStream {
public:
    AudioStreamRecorder();
    ~AudioStreamRecorder() = default;
    void setParameter(std::string recordDir, oboe::AudioApi api, int devId, int sample, int channel, int bit);
    int start() override;
    void stop() override;
    bool restart() override;
    oboe::DataCallbackResult onAudioReady(oboe::AudioStream *audioStream, void *audioData, int32_t numFrames) override;

private:
    std::mutex  mLock;
    std::string     recordFileDir;
    WavFile*        wavFile;
};

class AudioStreamPlayer : public OboeAudioStream {
public:
    AudioStreamPlayer();
    ~AudioStreamPlayer() = default;
    void setParameter(oboe::AudioApi api, std::string path, int devId);
    int start() override;
    void stop() override;
    bool restart() override;
    oboe::DataCallbackResult onAudioReady(oboe::AudioStream *audioStream, void *audioData, int32_t numFrames) override;

private:
    std::mutex  mLock;
    std::string playFile;

    float kAmplitude = 0.5f;
    float kFrequent = 440;
    float kPI = M_PI;
    float kTwoPi = kPI * 2;
    double mPhaseIncrement; //  = kFrequent * kTwoPi / (double) sampleRate;
    float mPhase = 0.0;

};
