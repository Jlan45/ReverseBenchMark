package com.benchmark.core.challenges

import com.benchmark.core.Challenge

/**
 * Challenge 4: Serial Number Generator (Keygen)
 * Given a username, generate the correct serial number.
 *
 * Algorithm:
 * 1. Compute username hash via custom function
 * 2. Apply transformations to produce serial segments
 * 3. Format as SERIAL-XXXXX-XXXXX
 */
class SerialGenChallenge : Challenge {
    override val id = "serial_gen"
    override val description = "Given username 'benchmark_user', generate the correct serial number"

    private val targetUsername = "benchmark_user"

    override fun verify(input: String): Boolean {
        return input == generateSerial(targetUsername)
    }

    override fun getFlag(): String {
        return "FLAG{k3yg3n_${generateSerial(targetUsername)}}"
    }

    fun generateSerial(username: String): String {
        // Step 1: username to seed
        var seed = 0x12345678L
        for (ch in username) {
            seed = (seed * 31 + ch.code) and 0xFFFFFFFFL
            seed = seed xor (seed shr 11)
            seed = (seed + (seed shl 3)) and 0xFFFFFFFFL
        }

        // Step 2: generate serial parts
        val part1 = transformSeed(seed, 0x5A5A5A5AL)
        val part2 = transformSeed(seed, 0xA5A5A5A5L)

        // Step 3: format
        return "SERIAL-${formatPart(part1)}-${formatPart(part2)}"
    }

    private fun transformSeed(seed: Long, mask: Long): Long {
        var v = seed xor mask
        v = (v * 0x6C078965L) and 0xFFFFFFFFL
        v = v xor (v shr 17)
        v = (v * 0x27D4EB2FL) and 0xFFFFFFFFL
        v = v xor (v shr 15)
        return v % 100000 // 5 digits
    }

    private fun formatPart(value: Long): String {
        return value.toString().padStart(5, '0')
    }
}
