package com.aela.mainboardoverride.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * D02 balance criteria, measured 2026-09-21 over the D01 seed set before any
 * generation tuning. The reference solution replays with no scripts, so a
 * clean reference wins with exactly 8 trace per turn and no trap hits:
 *
 * - Free play (1000 classic seeds + 7 scenarios x 100 seeds): every opening
 *   hand offers at least one legal placement, and every reference victory
 *   keeps at least 28 trace of headroom (ghost route 9 closes at 72).
 * - Challenges (30 levels): every reference solution fits its turn and trace
 *   budgets, including the exact-limit levels (2 at 48/48, 10 and 20-30 at
 *   9/9 turns). Any deviation in real play (noise, traps, detours) spends
 *   that margin, which is the intended difficulty gradient.
 */
class BalanceTest {
    private fun replay(state: GameState, solution: List<GameAction.PlaceDomino>): GameState {
        var current = state
        for (action in solution) {
            current = GameEngine.reduce(GameEngine.reduce(current, action).state, GameAction.EndTurn).state
        }
        return current
    }

    @Test fun `reference victories keep headroom and need no scripts`() {
        val sets = listOf("classic" to null) + ScenarioCatalog.all.map { it.id to it.id }
        for ((label, scenarioId) in sets) {
            val seeds = if (scenarioId == null) 0L until 1000L else 0L until 100L
            var minHeadroom = Int.MAX_VALUE
            for (seed in seeds) {
                val generated = if (scenarioId == null) LevelGenerator.generateVerified(seed)
                else LevelGenerator.generateVerified(seed, scenarioId = scenarioId)
                assertTrue(
                    GameEngine.legalPlacements(generated.state).isNotEmpty(),
                    "$label seed $seed opens with no legal placement",
                )
                val end = replay(generated.state, generated.solution)
                assertEquals(GameResult.VICTORY, end.result, "$label seed $seed")
                assertEquals(
                    generated.solution.size * 8, end.trace,
                    "$label seed $seed reference is not clean (traps or noise)",
                )
                minHeadroom = minOf(minHeadroom, 100 - end.trace)
            }
            println("BALANCE $label seeds=${seeds.count()} minHeadroom=$minHeadroom")
            assertTrue(minHeadroom >= 28, "$label closest victory leaves $minHeadroom headroom")
        }
    }

    @Test fun `challenge references fit their budgets`() {
        for (level in ChallengeCatalog.levels) {
            val generated = LevelGenerator.generateVerified(level.seed, level.number)
            val end = replay(generated.state, generated.solution)
            assertEquals(GameResult.VICTORY, end.result, "challenge ${level.number}")
            val turns = generated.solution.size
            level.rules.maxTurns?.let { assertTrue(turns <= it, "challenge ${level.number} needs $turns of $it turns") }
            level.rules.maxTrace?.let { assertTrue(end.trace <= it, "challenge ${level.number} traces ${end.trace} of $it") }
            println("BALANCE challenge ${level.number} turns=$turns trace=${end.trace} maxTurns=${level.rules.maxTurns} maxTrace=${level.rules.maxTrace}")
        }
    }
}
