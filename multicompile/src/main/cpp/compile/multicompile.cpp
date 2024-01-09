#include <jni.h>
#include <string>
#include <android/log.h>


int    *buffer = NULL;
size_t bytes = 0;


void initBuffer() {
    bytes = 160 * 16 * 2 / 8;
    buffer = static_cast<int *>(malloc(bytes));
    if (!buffer) {
        __android_log_print (ANDROID_LOG_INFO, "initBuffer", "%s", "null buffer now.");
    }
}

void checkBuffer() {
    int length = 64;
    int *subBuffer = static_cast<int *>(malloc(64));

    // memcpy start buffer
    __android_log_print (ANDROID_LOG_INFO, "initBuffer", "%s", "memcpy start buffer now.");
    memcpy(buffer, subBuffer, length);

    // memcpy sub buffer
    __android_log_print (ANDROID_LOG_INFO, "initBuffer", "%s", "memcpy sub buffer now.");
    memcpy(buffer + bytes / 2, subBuffer, length);

    // memcpy end buffer
    __android_log_print (ANDROID_LOG_INFO, "initBuffer", "%s", "memcpy end buffer now.");
    memcpy(buffer + bytes - 32, subBuffer, length);

    // memcpy tail buffer
    __android_log_print (ANDROID_LOG_INFO, "initBuffer", "%s", "memcpy tail buffer now.");
    memcpy(buffer + bytes, subBuffer, length);

    // memcpy more buffer
    __android_log_print (ANDROID_LOG_INFO, "initBuffer", "%s", "memcpy more buffer now.");
    memcpy(buffer + bytes + 1, subBuffer, length);

    // memcpy NULL buffer
    if (subBuffer) {
        free(subBuffer);
        subBuffer = NULL;
    }
    __android_log_print (ANDROID_LOG_INFO, "initBuffer", "%s", "memcpy more buffer now.");
    memcpy(buffer, subBuffer, length);
}


extern "C" JNIEXPORT jstring JNICALL
Java_com_aries_multicompile_NativeLib_stringFromJNI(
        JNIEnv* env,
        jobject /* this */) {
    std::string hello = "Hello from C++";

    __android_log_print (ANDROID_LOG_INFO, "stringFromJNI", "%s", "init buffer now.");
    initBuffer();

    __android_log_print (ANDROID_LOG_INFO, "stringFromJNI", "%s", "check buffer now.");
    checkBuffer();

    return env->NewStringUTF(hello.c_str());
}
