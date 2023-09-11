//
// Created by Aries on 2023/9/21.
//

#include <WavFile.h>

#ifdef LOG_TAG
#undef LOG_TAG
#define LOG_TAG "OboeManager"
#endif


std::string getCurrentTime() {
    char timeStr[200];
    time_t now = time(NULL);
    struct tm* stime = localtime(&now);
    sprintf(timeStr, "%04d%02d%02d_%02d%02d%02d",
            stime->tm_year + 1900, stime->tm_mon + 1, stime->tm_mday,
            stime->tm_hour, stime->tm_min, stime->tm_sec);
    return std::string(timeStr);
}

WavFile::WavFile() {
    wavPath.clear();
    frames = 0;
    sampleRate = 0;
    channelCount = 0;
    bit = 0;
    state = STATE_DEFAULT;
}

bool WavFile::canWrite() {
    return state == STATE_START;
}

void WavFile::setParam(const std::string& fileDir, const std::string& name, int sample, int channel, int bits) {
    wavPath = fileDir + "/" + name + "_" + getCurrentTime() + ".wav";
    sampleRate = sample;
    channelCount = channel;
    bit = bits;
    state = STATE_INIT;
}

void WavFile::open() {
    file = fopen(wavPath.c_str(), "wb");
    if (!file) {
        ALOGE("Unable to create file %s, error = %s", wavPath.c_str(), strerror(errno));
        state = STATE_ERROR;
        return;
    }

    header.riff_id = ID_RIFF;
    header.riff_sz = 0;
    header.riff_fmt = ID_WAVE;
    header.fmt_id = ID_FMT;
    header.fmt_sz = 16;
    header.audio_format = FORMAT_PCM;
    header.num_channels = channelCount;
    header.sample_rate = sampleRate;
    header.bits_per_sample = bit;
    header.byte_rate = (bit / 8) * channelCount * sampleRate;
    header.block_align = channelCount * (bit / 8);
    header.data_id = ID_DATA;

    /* leave enough room for header */
    fseek(file, sizeof(struct wav_header), SEEK_SET);
    frames = 0;
    state = STATE_START;
}

void WavFile::close() {
    if (file != NULL) {
        if (state == STATE_START) {
            header.data_sz = frames * header.block_align;
            header.riff_sz = header.data_sz + sizeof(header) - 8;
            fseek(file, 0, SEEK_SET);
            fwrite(&header, sizeof(struct wav_header), 1, file);
        }

        fclose(file);
        file = NULL;
    }
    state = STATE_STOP;
}

void WavFile::write(void *buffer, int frameSize) {
    if (state == STATE_ERROR) {
        return;
    }

    if (fwrite(buffer, channelCount * bit/8, frameSize, file) != frameSize) {
        ALOGE("Error write buffer\n");
        state = STATE_ERROR;
    } else {
        frames += frameSize;
    }
}

WavFile::~WavFile() {
    if (state == STATE_INIT || state == STATE_START) {
        close();
    }
}

