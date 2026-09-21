package com.aela.mainboardoverride.domain

data class Scenario(val id: String, val required: Int, val width: Int, val height: Int, val route: Int, val firewalls: Int, val traps: Int)

object ScenarioCatalog {
    val all = listOf(
        Scenario("classic", 0, 8, 6, 5, 3, 1),
        Scenario("lab", 5, 9, 6, 5, 4, 2),
        Scenario("data", 10, 9, 6, 6, 6, 2),
        Scenario("industry", 15, 10, 6, 7, 8, 3),
        Scenario("archive", 20, 10, 6, 7, 10, 4),
        Scenario("core", 25, 11, 6, 8, 12, 4),
        Scenario("ghost", 30, 11, 6, 9, 14, 5),
    )
    fun get(id: String) = all.firstOrNull { it.id == id } ?: all.first()
}

/** Campaign phases over the 30 challenge levels; labels only, rules untouched. */
fun challengePhase(number: Int): Int = ((number - 1) / 10).coerceIn(0, 2)

/** Additive achievement ids; grants never pay twice because storage is a set. */
enum class Achievement(val id: String) {
    FIRST_VICTORY("first_victory"),
    FIVE_CHALLENGES("five_challenges"),
    EXPLORER("explorer"),
    CLEAN_RUN("clean_run"),
}

/** Pure evaluation shared by the repository and tests. */
fun earnedAchievements(challenges: Int, scenarios: Int, trace: Int, firstVictory: Boolean): Set<String> = buildSet {
    if (firstVictory) add(Achievement.FIRST_VICTORY.id)
    if (challenges >= 5) add(Achievement.FIVE_CHALLENGES.id)
    if (scenarios >= 1) add(Achievement.EXPLORER.id)
    if (trace <= 40) add(Achievement.CLEAN_RUN.id)
}

object Rewards {
    const val VICTORY = 20
    const val FIRST_CHALLENGE = 40
    const val DAILY_GOAL = 10
    const val SKIN_PRICE = 200
    const val ENTRY_SKIN_PRICE = 80
    val premiumSkins = setOf("copper", "aurora", "titanium", "jade", "ruby")
    val affordableSkins = setOf("graphite", "signal")
    val purchasableSkins = premiumSkins + affordableSkins
    fun skinPrice(id: String) = if (id in affordableSkins) ENTRY_SKIN_PRICE else SKIN_PRICE

    /**
     * Victory score: fewer turns and less trace score higher, floored at zero.
     * Derived from stored records, so no persistence migration is needed.
     */
    fun victoryScore(turns: Int, trace: Int): Int = (1000 - turns * 40 - trace).coerceAtLeast(0)
}

/** Device-local calendar day; the daily goal resets on date change. */
fun todayString(): String = java.time.LocalDate.now().toString()

/** A victory earns the daily bonus only when no claim is recorded for [today]. */
fun dailyGoalEarns(lastClaim: String?, today: String): Boolean = lastClaim != today
