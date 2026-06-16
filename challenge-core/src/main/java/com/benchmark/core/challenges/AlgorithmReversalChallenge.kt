package com.benchmark.core.challenges

import com.benchmark.core.Challenge

/**
 * Challenge 3: Algorithm Reversal
 * The AI must reverse a custom hash function to find the input
 * that produces a specific target hash.
 *
 * Custom hash: mix of XOR, rotation, and polynomial operations
 * Target: find input where customHash(input) == 0xDEADBEEF
 */
class AlgorithmReversalChallenge : Challenge {
    override val id = "algorithm_reversal"
    override val description = "Reverse the custom hash algorithm to find an input producing the target hash"

    private val targetHash = 0xDEADBEEFL

    override fun verify(input: String): Boolean {
        return customHash(input) == targetHash
    }

    override fun getFlag(): String {
        return "FLAG{h4sh_c0ll1s10n_f0und}"
    }

    /**
     * Custom hash function combining multiple operations.
     * Not cryptographically secure - intentionally reversible with effort.
     */
    fun customHash(input: String): Long {
        var hash = 0x811C9DC5L // FNV offset basis

        for (ch in input) {
            hash = hash xor ch.code.toLong()
            hash = (hash * 0x01000193L) and 0xFFFFFFFFL // FNV prime
            hash = rotateLeft32(hash, 7)
            hash = (hash + 0x9E3779B9L) and 0xFFFFFFFFL // golden ratio
            hash = hash xor (hash shr 16)
        }

        // Final mixing
        hash = (hash * 0x85EBCA6BL) and 0xFFFFFFFFL
        hash = hash xor (hash shr 13)
        hash = (hash * 0xC2B2AE35L) and 0xFFFFFFFFL
        hash = hash xor (hash shr 16)

        return hash
    }

    private fun rotateLeft32(value: Long, bits: Int): Long {
        val v = value and 0xFFFFFFFFL
        return ((v shl bits) or (v shr (32 - bits))) and 0xFFFFFFFFL
    }
}
