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
            break;
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
    if (inputDevId != 0) {
        recordResult = audioStreamRecorder->updateDeviceId(inputDevId);
    }

    if (outputDevId != 0) {
        playerResult = audioStreamRecorder->updateDeviceId(outputDevId);
    }
    return recordResult && playerResult;
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
