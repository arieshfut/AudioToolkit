//
// Created by 1602 on 2023/5/24.
//
#pragma once

#include <string>
#include <android/log.h>
#include <stdlib.h>
#include <jni.h>

#define LOG_TAG "AudioBase"
#define ANDROID_DEBUG
#define FAILED MAXME_FAILED

#ifdef ANDROID_DEBUG
    #define ALOGD(...) ((void)__android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, __VA_ARGS__))
    #define ALOGI(...) ((void)__android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__))
    #define ALOGW(...) ((void)__android_log_print(ANDROID_LOG_WARN, LOG_TAG, __VA_ARGS__))
    #define ALOGE(...) ((void)__android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__))
    #define ALOGV(...) ((void)__android_log_print(ANDROID_LOG_VERBOSE, LOG_TAG, __VA_ARGS__))
#else
    #define LOGD(...);
    #define LOGI(...) ((void)__android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__))
    #define LOGW(...) ((void)__android_log_print(ANDROID_LOG_WARN, LOG_TAG, __VA_ARGS__))
    #define LOGE(...) ((void)__android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__))
    #define LOGV(...);
#endif    // end of #ifdef ANDROID_DEBUG

#define DEFAULT_OBOE_SAMPLERATE                (48000)
#define DEFAULT_OBOE_CHANNEL                   (2)
#define DEFAULT_OBOE_BIT                       (16)
#define DEFAULT_OBOE_DEVICE_ID                 (0)

enum AudioState {
    STATE_ERROR = -1,
    STATE_DEFAULT = 0,
    STATE_INIT,
    STATE_START,
    STATE_STOP
};


#define ID_RIFF 0x46464952
#define ID_WAVE 0x45564157
#define ID_FMT  0x20746d66
#define ID_DATA 0x61746164

#define FORMAT_PCM 1

struct wav_header {
    uint32_t riff_id;
    uint32_t riff_sz;
    uint32_t riff_fmt;
    uint32_t fmt_id;
    uint32_t fmt_sz;
    uint16_t audio_format;
    uint16_t num_channels;
    uint32_t sample_rate;
    uint32_t byte_rate;
    uint16_t block_align;
    uint16_t bits_per_sample;
    uint32_t data_id;
    uint32_t data_sz;
};

