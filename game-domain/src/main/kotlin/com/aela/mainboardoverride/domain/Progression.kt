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

object Rewards {
    const val VICTORY = 20
    const val FIRST_CHALLENGE = 40
    const val SKIN_PRICE = 200
    const val ENTRY_SKIN_PRICE = 80
    val premiumSkins = setOf("copper", "aurora")
    val affordableSkins = setOf("graphite", "signal")
    val purchasableSkins = premiumSkins + affordableSkins
    fun skinPrice(id: String) = if (id in affordableSkins) ENTRY_SKIN_PRICE else SKIN_PRICE
}
