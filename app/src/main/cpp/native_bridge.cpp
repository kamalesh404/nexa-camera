#include <jni.h>
#include <string>

extern "C" JNIEXPORT jstring JNICALL
Java_com_nexa_camera_NativeBridge_nativeBuildInfo(JNIEnv* env, jclass) {
    return env->NewStringUTF("Nexa native layer: C++17 / NDK");
}
