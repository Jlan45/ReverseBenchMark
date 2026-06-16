package com.benchmark.native_lib

/**
 * JNI bridge to native challenge implementations.
 * The native library contains the same challenge logic as challenge-core
 * but implemented in C++ (harder to reverse than Java/Kotlin bytecode).
 */
object NativeBridge {
    init {
        System.loadLibrary("challenge_native")
    }

    /** Verify license key natively */
    external fun verifyLicense(key: String): Boolean

    /** Decrypt flag natively */
    external fun decryptFlag(): String

    /** Verify algorithm reversal solution */
    external fun verifyAlgorithm(input: String): Boolean

    /** Generate serial for username */
    external fun generateSerial(username: String): String

    /** Verify math puzzle solution */
    external fun verifyMathPuzzle(x: Long): Boolean

    /** Get flag for a specific challenge (by index 0-4) */
    external fun getFlag(challengeIndex: Int): String

    /** Check if debugger is attached (Level 6+) */
    external fun isDebuggerDetected(): Boolean

    /** Run VMP-protected verification (Level 7) */
    external fun vmpVerify(challengeIndex: Int, input: String): Boolean
}
