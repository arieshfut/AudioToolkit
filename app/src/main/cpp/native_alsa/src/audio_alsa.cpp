#include "audio_alsa.h"

#include "tinyalsa/asoundlib.h"
#include <stdio.h>
#include <stdlib.h>
#include <stdint.h>
#include <signal.h>
#include <string.h>
#include <limits.h>

#ifdef LOG_TAG
#undef LOG_TAG
#define LOG_TAG "AudioALSA"
#endif


int capturing = 1;
int prinfo = 1;
unsigned int default_card = 1;
unsigned int default_device = 0;

unsigned int capture_sample(FILE *file, unsigned int card, unsigned int device,
                            unsigned int channels, unsigned int rate,
                            enum pcm_format format, unsigned int period_size,
                            unsigned int period_count, unsigned int capture_time);

int alsa_main(std::string path, int card_id, int device_id, int sample_rate, int channel, int bit) {
    FILE *file;
    struct wav_header header;
    unsigned int card = card_id;
    unsigned int device = device_id;
    unsigned int channels = channel;
    unsigned int rate = sample_rate;
    unsigned int bits = bit;
    unsigned int frames = 0;
    unsigned int period_size = 1024;
    unsigned int period_count = 8;
    unsigned int capture_time = UINT_MAX;
    enum pcm_format format;
    int no_header = 0;

    file = fopen(path.c_str(), "wb");
    if (!file) {
        ALOGE("Unable to create file '%s'\n", path.c_str());
        return -1;
    }

    header.riff_id = ID_RIFF;
    header.riff_sz = 0;
    header.riff_fmt = ID_WAVE;
    header.fmt_id = ID_FMT;
    header.fmt_sz = 16;
    header.audio_format = FORMAT_PCM;
    header.num_channels = channels;
    header.sample_rate = rate;

    switch (bits) {
        case 32:
            format = PCM_FORMAT_S32_LE;
            break;
        case 24:
            format = PCM_FORMAT_S24_LE;
            break;
        case 16:
            format = PCM_FORMAT_S16_LE;
            break;
        default:
            ALOGE("%u bits is not supported.\n", bits);
            fclose(file);
            return -1;
    }

    header.bits_per_sample = pcm_format_to_bits(format);
    header.byte_rate = (header.bits_per_sample / 8) * channels * rate;
    header.block_align = channels * (header.bits_per_sample / 8);
    header.data_id = ID_DATA;

    /* leave enough room for header */
    if (!no_header) {
        fseek(file, sizeof(struct wav_header), SEEK_SET);
    }

    frames = capture_sample(file, card, device, header.num_channels,
                            header.sample_rate, format,
                            period_size, period_count, capture_time);
    if (prinfo) {
        ALOGI("Captured %u frames\n", frames);
    }

    /* write header now all information is known */
    if (!no_header) {
        header.data_sz = frames * header.block_align;
        header.riff_sz = header.data_sz + sizeof(header) - 8;
        fseek(file, 0, SEEK_SET);
        fwrite(&header, sizeof(struct wav_header), 1, file);
    }

    fclose(file);

    return frames > 0 ? frames : -1;
}

int StartAlsaCapture(std::string path, int card, int device, int sampleRate, int channel, int bit) {
    capturing = 1;
    int result = alsa_main(path, card, device, sampleRate, channel, bit);

    return result;
}

void StopAlsaCapture() {
    capturing = 0;
}

unsigned int capture_sample(FILE *file, unsigned int card, unsigned int device,
                            unsigned int channels, unsigned int rate,
                            enum pcm_format format, unsigned int period_size,
                            unsigned int period_count, unsigned int capture_time)
{
    struct pcm_config config;
    struct pcm *pcm;
    char *buffer;
    unsigned int size;
    unsigned int frames_read;
    unsigned int total_frames_read;
    unsigned int bytes_per_frame;

    memset(&config, 0, sizeof(config));
    config.channels = channels;
    config.rate = rate;
    config.period_size = period_size;
    config.period_count = period_count;
    config.format = format;
    config.start_threshold = 0;
    config.stop_threshold = 0;
    config.silence_threshold = 0;

    pcm = pcm_open(card, device, PCM_IN, &config);
    if (!pcm || !pcm_is_ready(pcm)) {
        ALOGE("Unable to open PCM device (%s)\n", pcm_get_error(pcm));
        return -1;
    }

    size = pcm_frames_to_bytes(pcm, pcm_get_buffer_size(pcm)/2);
    buffer = (char *)malloc(size);
    if (!buffer) {
        ALOGE("Unable to allocate %u bytes\n", size);
        pcm_close(pcm);
        return -1;
    }

    if (prinfo) {
        ALOGI("Capturing sample: %u ch, %u hz, %u bit\n", channels, rate,
               pcm_format_to_bits(format));
    }

    bytes_per_frame = pcm_frames_to_bytes(pcm, 1);
    total_frames_read = 0;
    frames_read = 0;
    while (capturing) {
        frames_read = pcm_readi(pcm, buffer, pcm_get_buffer_size(pcm)/2);
        total_frames_read += frames_read;
        if ((total_frames_read / rate) >= capture_time) {
            capturing = 0;
        }
        if (fwrite(buffer, bytes_per_frame, frames_read, file) != frames_read) {
            ALOGE("Error capturing sample\n");
            break;
        }
    }

    free(buffer);
    pcm_close(pcm);
    return total_frames_read;
}