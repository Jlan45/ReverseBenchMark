#pragma once
#include <sys/ptrace.h>
#include <unistd.h>
#include <cstdio>
#include <cstring>

/**
 * Anti-debugging: ptrace-based detection.
 * If a debugger is already attached, ptrace(TRACEME) will fail.
 */

static bool is_ptrace_traced() {
    // Method 1: Try to ptrace ourselves
    if (ptrace(PTRACE_TRACEME, 0, nullptr, nullptr) == -1) {
        return true; // A debugger is attached
    }
    // Detach from ourselves
    ptrace(PTRACE_DETACH, 0, nullptr, nullptr);

    // Method 2: Check /proc/self/status for TracerPid
    FILE* f = fopen("/proc/self/status", "r");
    if (f) {
        char line[256];
        while (fgets(line, sizeof(line), f)) {
            if (strncmp(line, "TracerPid:", 10) == 0) {
                int tracerPid = 0;
                sscanf(line + 10, "%d", &tracerPid);
                fclose(f);
                return tracerPid != 0;
            }
        }
        fclose(f);
    }

    return false;
}
