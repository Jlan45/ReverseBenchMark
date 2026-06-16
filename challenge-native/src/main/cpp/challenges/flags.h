#pragma once
#include <string>

/**
 * Get flag by challenge index.
 */
static std::string native_get_flag(int challengeIndex) {
    switch (challengeIndex) {
        case 0: return "FLAG{lic3ns3_cr4ck3d_7a8b9c}";
        case 1: return native_decrypt_flag();
        case 2: return "FLAG{h4sh_c0ll1s10n_f0und}";
        case 3: {
            std::string serial = native_generate_serial("benchmark_user");
            return "FLAG{k3yg3n_" + serial + "}";
        }
        case 4: return "FLAG{crt_s0lv3d_6275}";
        default: return "FLAG{unknown}";
    }
}
