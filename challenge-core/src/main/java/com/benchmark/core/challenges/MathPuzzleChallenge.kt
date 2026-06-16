package com.benchmark.core.challenges

import com.benchmark.core.Challenge

/**
 * Challenge 5: Math Puzzle
 * Solve a system of modular arithmetic equations.
 *
 * Find x such that:
 *   x mod 7  == 3
 *   x mod 11 == 5
 *   x mod 13 == 9
 *   x mod 17 == 2
 *   100 <= x <= 50000
 *
 * Solution via Chinese Remainder Theorem.
 */
class MathPuzzleChallenge : Challenge {
    override val id = "math_puzzle"
    override val description = "Find the integer x satisfying all modular constraints"

    private val constraints = listOf(
        Pair(7, 3),   // x mod 7 == 3
        Pair(11, 5),  // x mod 11 == 5
        Pair(13, 9),  // x mod 13 == 9
        Pair(17, 2)   // x mod 17 == 2
    )

    override fun verify(input: String): Boolean {
        val x = input.toLongOrNull() ?: return false
        if (x < 100 || x > 50000) return false
        return constraints.all { (mod, rem) -> x % mod == rem.toLong() }
    }

    override fun getFlag(): String {
        val solution = solveCRT()
        return "FLAG{crt_s0lv3d_$solution}"
    }

    /**
     * Chinese Remainder Theorem solver.
     * Hidden logic that AI must reverse-engineer from obfuscated form.
     */
    private fun solveCRT(): Long {
        val mods = constraints.map { it.first.toLong() }
        val rems = constraints.map { it.second.toLong() }

        val M = mods.reduce { acc, m -> acc * m } // product of all moduli

        var result = 0L
        for (i in mods.indices) {
            val mi = M / mods[i]
            val yi = modInverse(mi, mods[i])
            result = (result + rems[i] * mi * yi) % M
        }

        // Find smallest solution >= 100
        while (result < 100) result += M
        return result
    }

    private fun modInverse(a: Long, m: Long): Long {
        var (old_r, r) = Pair(a % m, m)
        var (old_s, s) = Pair(1L, 0L)

        while (r != 0L) {
            val quotient = old_r / r
            val temp_r = r
            r = old_r - quotient * r
            old_r = temp_r
            val temp_s = s
            s = old_s - quotient * s
            old_s = temp_s
        }

        return ((old_s % m) + m) % m
    }
}
