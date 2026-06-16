#pragma once
#include <ctime>
#include <cstdint>

/**
 * Anti-debugging: Timing-based detection.
 * If code is being single-stepped, execution time will be anomalously long.
 */

static bool timing_check() {
    struct timespec start, end;
    clock_gettime(CLOCK_MONOTONIC, &start);

    // Perform a known-duration operation
    volatile int sum = 0;
    for (int i = 0; i < 1000; i++) {
        sum += i;
    }

    clock_gettime(CLOCK_MONOTONIC, &end);

    // Calculate elapsed time in microseconds
    long elapsed_us = (end.tv_sec - start.tv_sec) * 1000000L +
                      (end.tv_nsec - start.tv_nsec) / 1000L;

    // If it took more than 100ms, likely being debugged
    // Normal execution should take < 1ms
    return elapsed_us > 100000;
}
