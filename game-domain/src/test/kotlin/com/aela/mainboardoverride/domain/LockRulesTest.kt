package com.aela.mainboardoverride.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * E01 lock threat: exact-value cells that punish mismatched contact.
 *
 * Fixture: route-1 (0,0) at (0,2)H and route-2 (0,5) at (2,2)V chain the
 * start network; the lock at (2,1) demands 0. Cell (3,1) touches the lock
 * while (3,2) connects through (2,2), so a (4,0) tile trips and a (0,0)
 * tile satisfies; a (3,0) tile at (2,0)V covers the lock while matching
 * (2,2) through its lower half.
 */
class LockRulesTest {
    private fun lockFixture(): GameState {
        val board = BoardState(
            width = 8, height = 6, start = Position(-1, 2), extraction = Position(8, 2),
            placed = listOf(
                PlacedDomino(Domino("route-1", 0, 0), Position(0, 2), Orientation.HORIZONTAL),
                PlacedDomino(Domino("route-2", 0, 5), Position(2, 2), Orientation.VERTICAL),
            ),
            locks = mapOf(Position(2, 1) to 0),
        )
        return GameState(
            seed = 1L, board = board,
            dominoHand = listOf(Domino("wrong", 4, 0), Domino("right", 0, 0), Domino("cover", 3, 0), Domino("badcover", 4, 1)),
            dominoBag = emptyList(),
            scriptHand = listOf(ScriptCard("kill", ScriptType.KILL_PROCESS)),
            scriptDeck = emptyList(),
        )
    }

    @Test fun `contacting a lock with the wrong value trips defeat`() {
        val transition = GameEngine.reduce(lockFixture(), GameAction.PlaceDomino("wrong", Position(3, 1), Orientation.VERTICAL))
        assertEquals(GameResult.LOCK_TRIPPED, transition.state.result)
        assertTrue(transition.events.any { it == GameEvent.Finished(GameResult.LOCK_TRIPPED) })
    }

    @Test fun `the demanded value places normally and consumes the lock`() {
        val transition = GameEngine.reduce(lockFixture(), GameAction.PlaceDomino("right", Position(3, 1), Orientation.VERTICAL))
        assertEquals(null, transition.state.result)
        assertTrue(transition.state.board.locks.isEmpty())
    }

    @Test fun `covering a lock cell consumes it quietly`() {
        val transition = GameEngine.reduce(lockFixture(), GameAction.PlaceDomino("cover", Position(2, 0), Orientation.VERTICAL))
        assertEquals(null, transition.state.result)
        assertTrue(transition.state.board.locks.isEmpty())
    }

    @Test fun `covering a lock with the wrong value trips defeat`() {
        val transition = GameEngine.reduce(lockFixture(), GameAction.PlaceDomino("badcover", Position(2, 1), Orientation.HORIZONTAL))
        assertEquals(GameResult.LOCK_TRIPPED, transition.state.result)
        assertTrue(transition.events.any { it == GameEvent.Finished(GameResult.LOCK_TRIPPED) })
    }

    @Test fun `kill removes a lock without touching tiles`() {
        val transition = GameEngine.reduce(lockFixture(), GameAction.PlayKillProcess("kill", Position(2, 1)))
        assertTrue(transition.events.any { it is GameEvent.ScriptExecuted })
        assertTrue(transition.state.board.locks.isEmpty())
        assertEquals(2, transition.state.board.placed.size)
    }

    @Test fun `free generation carries exactly one reference-safe lock`() {
        for (seed in 0L until 50L) {
            val classic = LevelGenerator.generate(seed)
            assertEquals(1, classic.board.locks.size, "classic seed $seed")
            for (scenario in ScenarioCatalog.all) {
                val generated = LevelGenerator.generateScenario(seed, scenario.id)
                assertEquals(1, generated.board.locks.size, "${scenario.id} seed $seed")
            }
        }
        for (level in ChallengeCatalog.levels) {
            assertTrue(LevelGenerator.generateChallenge(level.number).board.locks.isEmpty(), "challenge ${level.number}")
        }
    }
}
