#pragma once
#include <string>
#include <cstdint>
#include <cstdio>

/**
 * Native implementation of Serial Generation Challenge.
 * Same algorithm as Kotlin version.
 */

static uint32_t transform_seed(uint32_t seed, uint32_t mask) {
    uint32_t v = seed ^ mask;
    v *= 0x6C078965;
    v ^= (v >> 17);
    v *= 0x27D4EB2F;
    v ^= (v >> 15);
    return v % 100000;
}

static std::string native_generate_serial(const char* username) {
    uint32_t seed = 0x12345678;

    for (int i = 0; username[i] != '\0'; i++) {
        seed = seed * 31 + (uint32_t)username[i];
        seed ^= (seed >> 11);
        seed += (seed << 3);
    }

    uint32_t part1 = transform_seed(seed, 0x5A5A5A5A);
    uint32_t part2 = transform_seed(seed, 0xA5A5A5A5);

    char serial[32];
    snprintf(serial, sizeof(serial), "SERIAL-%05u-%05u", part1, part2);
    return std::string(serial);
}
