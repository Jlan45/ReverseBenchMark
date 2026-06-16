package com.benchmark.level3

/**
 * Control Flow Obfuscation Engine.
 * Implements control flow flattening and opaque predicates manually.
 *
 * In a real build, the buildSrc ControlFlowPlugin would apply
 * this transformation automatically via ASM bytecode manipulation.
 * Here we demonstrate the pattern at source level.
 */
object ControlFlowEngine {

    /**
     * Opaque predicate: always returns true but hard to prove statically.
     * Based on the fact that x^2 + x is always even.
     */
    fun opaqueTrue(x: Int): Boolean {
        return (x * x + x) % 2 == 0
    }

    /**
     * Opaque predicate: always returns false.
     * Based on the fact that (x^2 - 1) % 4 != 2 for any integer x.
     */
    fun opaqueFalse(x: Int): Boolean {
        return (x * x - 1) % 4 == 2
    }

    /**
     * Control-flow-flattened license verification.
     * Original linear logic is converted to a state machine.
     */
    fun flattenedVerify(input: String, challengeId: Int): Boolean {
        var state = 0x7A3F
        var result = false
        var idx = 0
        var accumulator = 0L
        val data = input.toByteArray()

        while (true) {
            when (state) {
                0x7A3F -> {
                    // Entry: validate input length
                    state = if (data.isNotEmpty()) 0x2B1C else 0xFFFF
                    if (opaqueFalse(data.size)) state = 0x9999 // dead branch
                }
                0x2B1C -> {
                    // Process character
                    if (idx < data.size) {
                        accumulator = (accumulator * 31 + data[idx]) and 0xFFFFFFFFL
                        accumulator = accumulator xor (accumulator shr 11)
                        idx++
                        state = if (opaqueTrue(idx)) 0x2B1C else 0x4D8E
                    } else {
                        state = 0x4D8E
                    }
                }
                0x4D8E -> {
                    // Check result based on challenge
                    result = when (challengeId) {
                        0 -> accumulator == 0x1A2B3C4DL
                        1 -> accumulator == 0x5E6F7A8BL
                        2 -> accumulator == 0x9C0D1E2FL
                        else -> false
                    }
                    state = 0xFFFF
                }
                0x9999 -> {
                    // Bogus block - never reached
                    accumulator = (accumulator * 0xDEAD + 0xBEEF) and 0xFFFFFFFFL
                    state = if (opaqueFalse(idx)) 0x2B1C else 0xFFFF
                }
                0xFFFF -> {
                    // Exit
                    return result
                }
                else -> {
                    // Confusion: random state
                    state = 0xFFFF
                }
            }
        }
    }
}
