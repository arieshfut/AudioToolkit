#include <jni.h>
#include <string>

extern "C" JNIEXPORT jstring JNICALL
Java_com_aries_multicompile_NativeLib_stringFromJNI(
        JNIEnv* env,
        jobject /* this */) {
    std::string hello = "Android4.4：Hello from C++";
    return env->NewStringUTF(hello.c_str());
}