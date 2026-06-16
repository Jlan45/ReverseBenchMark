#include <jni.h>
#include <string>
#include <android/log.h>

#include "challenges/license_check.cpp"
#include "challenges/flag_decrypt.cpp"
#include "challenges/algorithm_reversal.cpp"
#include "challenges/serial_gen.cpp"
#include "challenges/math_puzzle.cpp"
#include "challenges/flags.h"

#ifdef ANTI_DEBUG_ENABLED
#include "anti_debug/ptrace_detect.cpp"
#include "anti_debug/debugger_detect.cpp"
#include "anti_debug/integrity_check.cpp"
#include "anti_debug/timing_check.cpp"
#endif

#ifdef VMP_ENABLED
#include "vmp/vm_interpreter.cpp"
#endif

#define TAG "BenchmarkNative"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, TAG, __VA_ARGS__)

extern "C" {

JNIEXPORT jboolean JNICALL
Java_com_benchmark_native_1lib_NativeBridge_verifyLicense(JNIEnv *env, jobject, jstring key) {
    const char *keyStr = env->GetStringUTFChars(key, nullptr);
    bool result = native_verify_license(keyStr);
    env->ReleaseStringUTFChars(key, keyStr);
    return result ? JNI_TRUE : JNI_FALSE;
}

JNIEXPORT jstring JNICALL
Java_com_benchmark_native_1lib_NativeBridge_decryptFlag(JNIEnv *env, jobject) {
    std::string flag = native_decrypt_flag();
    return env->NewStringUTF(flag.c_str());
}

JNIEXPORT jboolean JNICALL
Java_com_benchmark_native_1lib_NativeBridge_verifyAlgorithm(JNIEnv *env, jobject, jstring input) {
    const char *inputStr = env->GetStringUTFChars(input, nullptr);
    bool result = native_verify_algorithm(inputStr);
    env->ReleaseStringUTFChars(input, inputStr);
    return result ? JNI_TRUE : JNI_FALSE;
}

JNIEXPORT jstring JNICALL
Java_com_benchmark_native_1lib_NativeBridge_generateSerial(JNIEnv *env, jobject, jstring username) {
    const char *usernameStr = env->GetStringUTFChars(username, nullptr);
    std::string serial = native_generate_serial(usernameStr);
    env->ReleaseStringUTFChars(username, usernameStr);
    return env->NewStringUTF(serial.c_str());
}

JNIEXPORT jboolean JNICALL
Java_com_benchmark_native_1lib_NativeBridge_verifyMathPuzzle(JNIEnv *env, jobject, jlong x) {
    bool result = native_verify_math_puzzle((long)x);
    return result ? JNI_TRUE : JNI_FALSE;
}

JNIEXPORT jstring JNICALL
Java_com_benchmark_native_1lib_NativeBridge_getFlag(JNIEnv *env, jobject, jint challengeIndex) {
#ifdef ANTI_DEBUG_ENABLED
    if (is_debugger_attached() || is_ptrace_traced()) {
        return env->NewStringUTF("FLAG{nice_try_debugger}");
    }
#endif
    std::string flag = native_get_flag(challengeIndex);
    return env->NewStringUTF(flag.c_str());
}

JNIEXPORT jboolean JNICALL
Java_com_benchmark_native_1lib_NativeBridge_isDebuggerDetected(JNIEnv *env, jobject) {
#ifdef ANTI_DEBUG_ENABLED
    return (is_debugger_attached() || is_ptrace_traced() || is_frida_detected()) ? JNI_TRUE : JNI_FALSE;
#else
    return JNI_FALSE;
#endif
}

JNIEXPORT jboolean JNICALL
Java_com_benchmark_native_1lib_NativeBridge_vmpVerify(JNIEnv *env, jobject, jint challengeIndex, jstring input) {
#ifdef VMP_ENABLED
    const char *inputStr = env->GetStringUTFChars(input, nullptr);
    bool result = vm_execute_challenge(challengeIndex, inputStr);
    env->ReleaseStringUTFChars(input, inputStr);
    return result ? JNI_TRUE : JNI_FALSE;
#else
    return JNI_FALSE;
#endif
}

} // extern "C"
