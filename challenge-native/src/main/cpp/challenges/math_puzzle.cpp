#pragma once
#include <string>
#include <cstdint>

/**
 * Native implementation of Math Puzzle Challenge.
 * Chinese Remainder Theorem constraints:
 *   x mod 7 == 3
 *   x mod 11 == 5
 *   x mod 13 == 9
 *   x mod 17 == 2
 *   100 <= x <= 50000
 */

static bool native_verify_math_puzzle(long x) {
    if (x < 100 || x > 50000) return false;
    if (x % 7 != 3) return false;
    if (x % 11 != 5) return false;
    if (x % 13 != 9) return false;
    if (x % 17 != 2) return false;
    return true;
}
