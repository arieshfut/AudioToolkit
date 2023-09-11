#include <jni.h>
#include <string>
#include <audio_base.h>
#include <OboeManager.h>

#ifdef LOG_TAG
#undef LOG_TAG
#define LOG_TAG "oboe_jni"
#endif


static OboeManager* oboeManager = nullptr;

static void createOboeInstance() {
    if (oboeManager == nullptr) {
        oboeManager = new OboeManager();
    }
}

void JstingToCstring(JNIEnv* env, std::string& cstr, jstring jstr) {
    if (nullptr != jstr) {
        const char* native_str = env->GetStringUTFChars(jstr, nullptr);
        cstr = std::string(native_str);
        env->ReleaseStringUTFChars(jstr, native_str);
    }
}

extern "C"
JNIEXPORT void JNICALL
Java_com_aries_audiotoolkit_PreResearch_AudioOboeManager_NativeOboeCreate(JNIEnv *env,
                                                                               jobject thiz) {
    createOboeInstance();
}

extern "C"
JNIEXPORT void JNICALL
Java_com_aries_audiotoolkit_PreResearch_AudioOboeManager_NativeSetProp(JNIEnv *env,
                                                                            jobject thiz,
                                                                            jstring path,
                                                                            jint audio_api,
                                                                            jboolean need_record,
                                                                            jboolean need_play) {
    std::string file_path;
    JstingToCstring(env, file_path, path);
    oboeManager->setProp(file_path, audio_api, need_record, need_play);
    ALOGI("NativeSetProp done.");
}

extern "C"
JNIEXPORT void JNICALL
Java_com_aries_audiotoolkit_PreResearch_AudioOboeManager_NativeUpdateDeviceId(JNIEnv *env,
                                                                                   jobject thiz,
                                                                                   jint input_id,
                                                                                   jint output_id) {
    oboeManager->updateDeviceId(input_id, output_id);
    ALOGI("NativeUpdateDeviceId done.");
}

extern "C"
JNIEXPORT void JNICALL
Java_com_aries_audiotoolkit_PreResearch_AudioOboeManager_NativeOboeInitRecorder(JNIEnv *env,
                                                                                     jobject thiz,
                                                                                     jint dev_id,
                                                                                     jint sample,
                                                                                     jint channel,
                                                                                     jint bit) {
    oboeManager->setRecordParameter(dev_id, sample, channel, bit);
    ALOGI("NativeOboeInitRecorder done.");
}

extern "C"
JNIEXPORT void JNICALL
Java_com_aries_audiotoolkit_PreResearch_AudioOboeManager_NativeOboeInitPlayer(JNIEnv *env,
                                                                                   jobject thiz,
                                                                                   jstring path,
                                                                                   jint dev_id) {
    std::string file_path;
    JstingToCstring(env, file_path, path);
    oboeManager->setPlayerParameter(file_path, dev_id);
    ALOGI("NativeOboeInitPlayer done.");
}

extern "C"
JNIEXPORT jint JNICALL
Java_com_aries_audiotoolkit_PreResearch_AudioOboeManager_NativeOboeStart(JNIEnv *env,
                                                                              jobject thiz) {
    int result = oboeManager->start();
    ALOGI("NativeOboeStart result = %d", result);
    return result;
}

extern "C"
JNIEXPORT void JNICALL
Java_com_aries_audiotoolkit_PreResearch_AudioOboeManager_NativeOboeStop(JNIEnv *env,
                                                                             jobject thiz) {
    oboeManager->stop();
    ALOGI("NativeOboeStop done.");
}
