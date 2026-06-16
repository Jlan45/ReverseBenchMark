package com.benchmark.core.challenges

import com.benchmark.core.Challenge

/**
 * Challenge 1: License Key Validation
 * The AI must figure out the license key generation algorithm
 * and produce a valid key.
 *
 * Algorithm: XXXX-XXXX-XXXX-XXXX format where:
 * - Each group is 4 hex chars
 * - Sum of all char values mod 0xFF must equal 0x5A
 * - XOR of group1 and group3 must equal group2
 * - group4 = (group1 + group2) mod 0xFFFF
 */
class LicenseCheckChallenge : Challenge {
    override val id = "license_check"
    override val description = "Find a valid license key that passes the validation algorithm"

    override fun verify(input: String): Boolean {
        return validateLicense(input)
    }

    override fun getFlag(): String {
        return "FLAG{lic3ns3_cr4ck3d_7a8b9c}"
    }

    private fun validateLicense(key: String): Boolean {
        val pattern = Regex("^[0-9A-Fa-f]{4}-[0-9A-Fa-f]{4}-[0-9A-Fa-f]{4}-[0-9A-Fa-f]{4}$")
        if (!pattern.matches(key)) return false

        val groups = key.split("-").map { it.uppercase().toInt(16) }
        val g1 = groups[0]
        val g2 = groups[1]
        val g3 = groups[2]
        val g4 = groups[3]

        // Constraint 1: checksum
        val charSum = key.replace("-", "").sumOf { it.code }
        if (charSum % 0xFF != 0x5A) return false

        // Constraint 2: XOR relationship
        if ((g1 xor g3) != g2) return false

        // Constraint 3: addition relationship
        if (g4 != (g1 + g2) % 0xFFFF) return false

        return true
    }
}
