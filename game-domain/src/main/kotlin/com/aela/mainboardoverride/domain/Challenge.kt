package com.aela.mainboardoverride.domain

data class ChallengeRules(
    val maxTurns: Int? = null,
    val maxTrace: Int? = null,
)

data class ChallengeLevel(
    val number: Int,
    val seed: Long,
    val difficulty: Int,
    val rules: ChallengeRules,
)

object ChallengeCatalog {
    const val COUNT = 30
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
                7 -> ChallengeRules(maxTurns = 8, maxTrace = 72)
                8 -> ChallengeRules(maxTrace = 80)
                9 -> ChallengeRules(maxTurns = 9)
                10 -> ChallengeRules(maxTurns = 9, maxTrace = 80)
                else -> ChallengeRules(maxTurns = if (number < 20) 10 else 9, maxTrace = if (number < 25) 88 else 80)
            },
        )
    }

    fun level(number: Int): ChallengeLevel? = levels.getOrNull(number - 1)
}
