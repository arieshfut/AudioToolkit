#pragma once

#include <audio_base.h>

int StartAlsaCapture(std::string path, int card, int device, int sampleRate, int channel, int bit);

void StopAlsaCapture();
