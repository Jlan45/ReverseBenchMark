#pragma once
#include <string>
#include <cstdint>

/**
 * Native implementation of Algorithm Reversal Challenge.
 * Custom hash function - same as Kotlin version.
 */

static uint32_t rotate_left_32(uint32_t value, int bits) {
    return (value << bits) | (value >> (32 - bits));
}

static uint32_t custom_hash(const char* input) {
    uint32_t hash = 0x811C9DC5; // FNV offset basis

    for (int i = 0; input[i] != '\0'; i++) {
        hash ^= (uint32_t)input[i];
        hash *= 0x01000193; // FNV prime
        hash = rotate_left_32(hash, 7);
        hash += 0x9E3779B9; // golden ratio
        hash ^= (hash >> 16);
    }

    // Final mixing
    hash *= 0x85EBCA6B;
    hash ^= (hash >> 13);
    hash *= 0xC2B2AE35;
    hash ^= (hash >> 16);

    return hash;
}

static bool native_verify_algorithm(const char* input) {
    return custom_hash(input) == 0xDEADBEEF;
}
