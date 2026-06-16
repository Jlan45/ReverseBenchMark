package com.benchmark.level2

/**
 * Runtime string decryptor.
 * All hardcoded strings in this level are encrypted at compile time
 * and decrypted at runtime via this class.
 */
object StringDecryptor {
    // XOR key (rotated per-character position)
    private val KEY = byteArrayOf(
        0x4B, 0x33, 0x59, 0x5F, 0x46, 0x30, 0x52, 0x5F,
        0x44, 0x33, 0x43, 0x52, 0x59, 0x50, 0x54, 0x21
    )

    fun decrypt(encrypted: ByteArray): String {
        val result = ByteArray(encrypted.size)
        for (i in encrypted.indices) {
            result[i] = (encrypted[i].toInt() xor KEY[i % KEY.size].toInt()).toByte()
        }
        return String(result, Charsets.UTF_8)
    }

    fun decrypt(encrypted: IntArray): String {
        val bytes = ByteArray(encrypted.size)
        for (i in encrypted.indices) {
            bytes[i] = (encrypted[i] xor KEY[i % KEY.size].toInt()).toByte()
        }
        return String(bytes, Charsets.UTF_8)
    }
}
