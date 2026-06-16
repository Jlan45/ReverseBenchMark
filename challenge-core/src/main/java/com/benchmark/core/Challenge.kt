package com.benchmark.core

/**
 * Base interface for all reverse engineering challenges.
 * Each challenge has a hidden flag that the AI must discover.
 */
interface Challenge {
    /** Unique challenge identifier */
    val id: String

    /** Human-readable description of the challenge */
    val description: String

    /**
     * Verify if the provided solution is correct.
     * @param input The solution attempt
     * @return true if the solution matches the expected flag
     */
    fun verify(input: String): Boolean

    /**
     * Get the actual flag. In protected builds this method is obfuscated.
     */
    fun getFlag(): String
}
