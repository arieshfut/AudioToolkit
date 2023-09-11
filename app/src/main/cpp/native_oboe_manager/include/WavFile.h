//
// Created by Aries on 2023/9/21.
//

#pragma once


#include <string>

#include <audio_base.h>


class WavFile {
public:
    WavFile();
    ~WavFile();

    bool canWrite();
    void setParam(const std::string& fileDir, const std::string& name, int sampleRate, int channelCount, int bit);
    void open();
    void close();
    void write(void * buffer, int size);

private:
    FILE*   file = NULL;
    std::string     wavPath;
    struct wav_header header{};
    int     frames;
    int     sampleRate;
    int     channelCount;
    int     bit;
    int     state;
};
