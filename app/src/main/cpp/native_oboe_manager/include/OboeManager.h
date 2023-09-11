//
// Created by Aries on 2023/9/20.
//

#pragma once

#include <audio_base.h>
#include <oboe/Oboe.h>
#include <OboeAudioStream.h>


class OboeManager {
public:
    OboeManager();
    ~OboeManager();

    void setProp(std::string path, int api, bool needRecord, bool needPlay);
    void setRecordParameter(int devId, int sample, int channel, int bit);
    void setPlayerParameter(std::string path, int devId);
    bool updateDeviceId(int inputDeviceId, int outputDeviceId);
    int start();
    void stop();

private:
    AudioStreamRecorder*    audioStreamRecorder;
    AudioStreamPlayer*      audioStreamPlayer;
    oboe::AudioApi      audioApi;
    std::string         recordFileDir;
    bool    recordEnable = false;
    bool    playEnable = false;
    int state;
};
