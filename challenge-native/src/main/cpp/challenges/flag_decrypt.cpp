#pragma once
#include <string>
#include <cstring>
#include <cstdint>

/**
 * Native implementation of Flag Decrypt Challenge.
 * AES key embedded directly in binary (will be in .rodata for Level 4,
 * obfuscated by OLLVM for Level 5+).
 */

// XOR decryption - key is embedded in .rodata section
static const uint8_t SECRET_KEY[] = {
    0xDE, 0xAD, 0xBE, 0xEF, 0xCA, 0xFE, 0xBA, 0xBE,
    0xDE, 0xAD, 0xBE, 0xEF, 0xCA, 0xFE, 0xBA, 0xBE
};

static const uint8_t ENCRYPTED_FLAG[] = {
    0x98, 0xE1, 0xFF, 0xA8, 0xB1, 0x9F, 0x89, 0xCD,
    0x81, 0xC9, 0x8D, 0x8C, 0xB8, 0x87, 0xCA, 0xCA,
    0xED, 0xC9, 0xE1, 0x9C, 0xF9, 0x9D, 0xC8, 0x8D,
    0xAA, 0xF2, 0xD3, 0x9C, 0xAD, 0x83
};

static const int ENCRYPTED_FLAG_LEN = 30;

static std::string native_decrypt_flag() {
    char result[64] = {0};
    for (int i = 0; i < ENCRYPTED_FLAG_LEN; i++) {
        result[i] = (char)(ENCRYPTED_FLAG[i] ^ SECRET_KEY[i % 16]);
    }
    return std::string(result);
}
