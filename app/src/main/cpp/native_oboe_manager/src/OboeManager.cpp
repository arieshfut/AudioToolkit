#include <jni.h>
#include <string>
#include <utility>
#include <OboeManager.h>

#ifdef LOG_TAG
#undef LOG_TAG
#define LOG_TAG "OboeManager"
#endif


#define MAX_RESTART_LIMIT           (5)
#define SLEEP_MICRO_SECONDS         (20 * 1000)    // 20MS


/**
 * OboeManager
 */
OboeManager::OboeManager() {
    audioStreamRecorder = new AudioStreamRecorder();
    audioStreamPlayer = new AudioStreamPlayer();
    audioApi = oboe::AudioApi::Unspecified;
    recordFileDir = std::string("/sdcard/audiotoolkit/");

    state = STATE_DEFAULT;
}

OboeManager::~OboeManager() {
    if (audioStreamRecorder != nullptr) {
        delete audioStreamRecorder;
        audioStreamRecorder = nullptr;
    }
    if (audioStreamPlayer != nullptr) {
        delete audioStreamPlayer;
        audioStreamPlayer = nullptr;
    }
    state = STATE_DEFAULT;
}

void OboeManager::setProp(std::string path, int api, bool needRecord, bool needPlay) {
    switch (api) {
        case 1:
            audioApi = oboe::AudioApi::AAudio;
        case 2:
            audioApi = oboe::AudioApi::OpenSLES;
            break;
        default:
            audioApi = oboe::AudioApi::Unspecified;
            break;
    }
    recordFileDir = std::move(path);
    recordEnable = needRecord;
    playEnable = needPlay;
}

void OboeManager::setRecordParameter(int devId, int sample, int channel, int bit) {
    audioStreamRecorder->setParameter(recordFileDir, static_cast<oboe::AudioApi>(audioApi), devId, sample, channel, bit);
}

void OboeManager::setPlayerParameter(std::string path, int devId) {
    audioStreamPlayer->setParameter(static_cast<oboe::AudioApi>(audioApi), std::move(path), devId);
}

bool OboeManager::updateDeviceId(int inputDevId, int outputDevId) {
    bool recordResult = false, playerResult = false;
    if (recordEnable) {
        recordResult = audioStreamRecorder->updateDeviceId(inputDevId);
    }
    if (playEnable) {
        playerResult = audioStreamPlayer->updateDeviceId(outputDevId);
    }
    return true;
}

int OboeManager::start() {
    int result = 0;
    if (recordEnable) {
        result = audioStreamRecorder->start();
        if (result < 0) {
            return result;
        }
    }

    if (playEnable) {
        result = audioStreamPlayer->start();
    }

    if (result >= 0) {
        state = STATE_START;
    } else {
        state = STATE_ERROR;
    }
    return result;
}

void OboeManager::stop() {
    if (state == STATE_STOP || state == STATE_DEFAULT) {
        return;
    }

    if (recordEnable) {
        audioStreamRecorder->stop();
    }

    if (playEnable) {
        audioStreamPlayer->stop();
    }
    state = STATE_STOP;
}

bool OboeManager::isLatencyDetectionSupported() {
    if (playEnable) {
        return audioStreamPlayer->isLatencyDetectionSupported();
    }

    if (recordEnable) {
        return audioStreamRecorder->isLatencyDetectionSupported();
    }
    return false;
}

double OboeManager::getCurrentOutputLatencyMillis() {
    if (playEnable) {
        return audioStreamPlayer->getCurrentOutputLatencyMillis();
    }

    if (recordEnable) {
        return audioStreamRecorder->getCurrentOutputLatencyMillis();
    }

    return 0.0f;
}
