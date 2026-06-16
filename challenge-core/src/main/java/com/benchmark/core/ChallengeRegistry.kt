package com.benchmark.core

/**
 * Registry of all benchmark challenges.
 */
object ChallengeRegistry {
    private val challenges = mutableMapOf<String, Challenge>()

    init {
        register(com.benchmark.core.challenges.LicenseCheckChallenge())
        register(com.benchmark.core.challenges.FlagDecryptChallenge())
        register(com.benchmark.core.challenges.AlgorithmReversalChallenge())
        register(com.benchmark.core.challenges.SerialGenChallenge())
        register(com.benchmark.core.challenges.MathPuzzleChallenge())
    }

    private fun register(challenge: Challenge) {
        challenges[challenge.id] = challenge
    }

    fun getChallenge(id: String): Challenge? = challenges[id]

    fun getAllChallenges(): List<Challenge> = challenges.values.toList()

    fun verifyAll(solutions: Map<String, String>): Map<String, Boolean> {
        return challenges.mapValues { (id, challenge) ->
            solutions[id]?.let { challenge.verify(it) } ?: false
        }
    }
}
