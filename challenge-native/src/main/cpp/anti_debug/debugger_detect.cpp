#pragma once
#include <cstdio>
#include <cstring>
#include <dirent.h>
#include <unistd.h>

/**
 * Anti-debugging: Detect common debuggers and instrumentation frameworks.
 * Checks for:
 * - Frida gadget in /proc/self/maps
 * - Xposed framework
 * - Common debugger ports
 */

static bool is_debugger_attached() {
    // Check for common debugger indicators in maps
    FILE* maps = fopen("/proc/self/maps", "r");
    if (maps) {
        char line[512];
        while (fgets(line, sizeof(line), maps)) {
            if (strstr(line, "frida") || strstr(line, "xposed") ||
                strstr(line, "substrate") || strstr(line, "magisk")) {
                fclose(maps);
                return true;
            }
        }
        fclose(maps);
    }
    return false;
}

static bool is_frida_detected() {
    // Check for Frida default port (27042)
    FILE* f = fopen("/proc/net/tcp", "r");
    if (f) {
        char line[256];
        while (fgets(line, sizeof(line), f)) {
            // 69B2 = 27042 in hex (Frida default port)
            if (strstr(line, "69B2") || strstr(line, "69b2")) {
                fclose(f);
                return true;
            }
        }
        fclose(f);
    }

    // Check for Frida named thread
    char path[64];
    DIR* dir = opendir("/proc/self/task");
    if (dir) {
        struct dirent* entry;
        while ((entry = readdir(dir)) != nullptr) {
            if (entry->d_type == DT_DIR && strcmp(entry->d_name, ".") != 0 &&
                strcmp(entry->d_name, "..") != 0) {
                snprintf(path, sizeof(path), "/proc/self/task/%s/comm", entry->d_name);
                FILE* comm = fopen(path, "r");
                if (comm) {
                    char name[64] = {0};
                    fgets(name, sizeof(name), comm);
                    fclose(comm);
                    if (strstr(name, "frida") || strstr(name, "gmain")) {
                        closedir(dir);
                        return true;
                    }
                }
            }
        }
        closedir(dir);
    }

    return false;
}
