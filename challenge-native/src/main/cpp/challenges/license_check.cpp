#pragma once
#include <string>
#include <cstring>
#include <cstdint>
#include <regex>

/**
 * Native implementation of License Check Challenge.
 * Same algorithm as the Kotlin version in challenge-core.
 *
 * Format: XXXX-XXXX-XXXX-XXXX (hex)
 * Constraints:
 *   1. Sum of char values mod 0xFF == 0x5A
 *   2. group1 XOR group3 == group2
 *   3. group4 == (group1 + group2) mod 0xFFFF
 */
static bool native_verify_license(const char* key) {
    std::string keyStr(key);

    // Validate format
    if (keyStr.length() != 19) return false;
    if (keyStr[4] != '-' || keyStr[9] != '-' || keyStr[14] != '-') return false;

    // Parse hex groups
    uint32_t groups[4];
    for (int i = 0; i < 4; i++) {
        std::string group = keyStr.substr(i * 5, 4);
        for (char c : group) {
            if (!isxdigit(c)) return false;
        }
        groups[i] = (uint32_t)strtol(group.c_str(), nullptr, 16);
    }

    // Constraint 1: checksum
    int charSum = 0;
    for (char c : keyStr) {
        if (c != '-') charSum += (int)c;
    }
    if (charSum % 0xFF != 0x5A) return false;

    // Constraint 2: XOR relationship
    if ((groups[0] ^ groups[2]) != groups[1]) return false;

    // Constraint 3: addition relationship
    if (groups[3] != (groups[0] + groups[1]) % 0xFFFF) return false;

    return true;
}
