#include <jni.h>
#include <string>
#include <audio_base.h>
#include "audio_alsa.h"

#ifdef LOG_TAG
#undef LOG_TAG
#define LOG_TAG "native-lib"
#endif


void JstingToCstring(JNIEnv* env, std::string& cstr, jstring jstr) {
    if (nullptr != jstr) {
        const char* native_str = env->GetStringUTFChars(jstr, nullptr);
        cstr = std::string(native_str);
        env->ReleaseStringUTFChars(jstr, native_str);
    }
}

extern "C" jint JNIEXPORT JNICALL AlsaCaptureStart(JNIEnv *env,
                                                   jobject /* this */,
                                                   jstring path,
                                                   jint card,
                                                   jint device,
                                                   jint sample_rate,
                                                   jint channel,
                                                   jint bit) {
    std::string file_path;
    JstingToCstring(env, file_path, path);
    ALOGI("NativeAlsaCapture start now");
    int result = StartAlsaCapture(file_path, card, device, sample_rate, channel, bit);
    ALOGI("NativeAlsaCapture result = %d", result);
    return result;
}

extern "C" void JNIEXPORT JNICALL AlsaCaptureStop(JNIEnv *env, jobject /* this */) {
    StopAlsaCapture();
    ALOGI("AlsaCaptureStop done.");
}

static JNINativeMethod gMethods[] = {
        {"NativeAlsaCaptureStart", "(Ljava/lang/String;IIIII)I", (void*)AlsaCaptureStart},
        {"NativeAlsaCaptureStop", "()V", (void*)AlsaCaptureStop}
};

int registerNativeMethods(JNIEnv *env) {
    jclass javaMediaEngineClass = env->FindClass("com/aries/audiotoolkit/PreResearch/AudioAlsaManager");
    if (javaMediaEngineClass == nullptr) {
        return -1;
    }

    if ((*env).RegisterNatives(javaMediaEngineClass, gMethods, sizeof(gMethods) / sizeof(gMethods[0])) < 0) {
        return -1;
    }

    env->DeleteLocalRef(javaMediaEngineClass);
    return 0;
}

extern "C" jint JNIEXPORT JNICALL JNI_OnLoad(JavaVM *jvm, void *reserved) {
    JNIEnv *env = nullptr;
    if (jvm->GetEnv(reinterpret_cast<void **>(&env), JNI_VERSION_1_6) != JNI_OK)
        return -1;

    registerNativeMethods(env);
    return JNI_VERSION_1_6;
}

extern "C" void JNIEXPORT JNICALL JNI_OnUnLoad(JavaVM *jvm, void *reserved) {
    // do release
}