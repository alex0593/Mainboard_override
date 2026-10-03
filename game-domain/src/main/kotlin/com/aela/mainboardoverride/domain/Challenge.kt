package com.aela.mainboardoverride.domain

data class ChallengeRules(
    val maxTurns: Int? = null,
    val maxTrace: Int? = null,
    val bannedScripts: Set<ScriptType> = emptySet(),
)

data class ChallengeLevel(
    val number: Int,
    val seed: Long,
    val difficulty: Int,
    val rules: ChallengeRules,
)

object ChallengeCatalog {
    const val COUNT = 100
    val levels = (1..COUNT).map { number ->
        ChallengeLevel(
            number = number,
            seed = 0x4D41494EL + number * 7919L,
            difficulty = number,
            rules = when (number) {
                1 -> ChallengeRules(maxTurns = 6)
                2 -> ChallengeRules(maxTrace = 48)
                3 -> ChallengeRules(maxTurns = 6, maxTrace = 55)
                4 -> ChallengeRules(maxTrace = 56)
                5 -> ChallengeRules(maxTurns = 7)
                6 -> ChallengeRules(maxTrace = 64)
                7 -> ChallengeRules(maxTurns = 8, maxTrace = 72, bannedScripts = setOf(ScriptType.SPOOF))
                8 -> ChallengeRules(maxTrace = 80)
                9 -> ChallengeRules(maxTurns = 9)
                10 -> ChallengeRules(maxTurns = 9, maxTrace = 80)
                17 -> ChallengeRules(maxTurns = 10, maxTrace = 88, bannedScripts = setOf(ScriptType.SPOOF))
                27 -> ChallengeRules(maxTurns = 9, maxTrace = 80, bannedScripts = setOf(ScriptType.SPOOF))
                // 31+ rides the wide and wider board tiers: the witness corridor grows to
                // 10 then 11 dominoes, so every fifth level runs the exact reference budget
                // while the rest keep one spare turn and eight points of trace headroom.
                in 31..60 -> ChallengeRules(
                    maxTurns = if (number % 5 == 0) 10 else 11,
                    maxTrace = if (number % 5 == 0) 80 else 88,
                    bannedScripts = if (number % 10 == 7) setOf(ScriptType.SPOOF) else emptySet(),
                )
                in 61..100 -> ChallengeRules(
                    maxTurns = if (number % 5 == 0) 11 else 12,
                    maxTrace = if (number % 5 == 0) 88 else 96,
                    bannedScripts = if (number % 10 == 7) setOf(ScriptType.SPOOF) else emptySet(),
                )
                else -> ChallengeRules(maxTurns = if (number < 20) 10 else 9, maxTrace = if (number < 25) 88 else 80)
            },
        )
    }

    fun level(number: Int): ChallengeLevel? = levels.getOrNull(number - 1)
}

/**
 * Data nodes the connected route must sweep before extraction counts: the mechanic opens
 * with phase 3 of the campaign and every band after it adds one more node.
 */
fun dataNodeCount(level: Int): Int = when {
    level >= 81 -> 3
    level >= 61 -> 2
    level >= 41 -> 1
    else -> 0
}
