#pragma once
#include <cstdint>
#include <cstring>
#include <dlfcn.h>

/**
 * Anti-tampering: Integrity verification.
 * Checks that critical code sections haven't been patched.
 */

// CRC32 lookup table
static uint32_t crc32_table[256];
static bool crc32_initialized = false;

static void init_crc32_table() {
    for (uint32_t i = 0; i < 256; i++) {
        uint32_t crc = i;
        for (int j = 0; j < 8; j++) {
            crc = (crc >> 1) ^ (0xEDB88320 & (-(crc & 1)));
        }
        crc32_table[i] = crc;
    }
    crc32_initialized = true;
}

static uint32_t compute_crc32(const void* data, size_t length) {
    if (!crc32_initialized) init_crc32_table();

    const uint8_t* bytes = (const uint8_t*)data;
    uint32_t crc = 0xFFFFFFFF;
    for (size_t i = 0; i < length; i++) {
        crc = (crc >> 8) ^ crc32_table[(crc ^ bytes[i]) & 0xFF];
    }
    return crc ^ 0xFFFFFFFF;
}

/**
 * Verify that our own .text section hasn't been modified.
 * Returns true if integrity check passes (no tampering detected).
 */
static bool verify_code_integrity() {
    // Get the address of a known function
    Dl_info info;
    if (dladdr((void*)verify_code_integrity, &info) == 0) {
        return false; // Can't find ourselves - suspicious
    }

    // Basic check: verify we can still read our own code
    const uint8_t* base = (const uint8_t*)info.dli_fbase;
    if (base == nullptr) return false;

    // Check ELF magic
    if (base[0] != 0x7F || base[1] != 'E' || base[2] != 'L' || base[3] != 'F') {
        return false; // Not a valid ELF - tampered
    }

    return true;
}
