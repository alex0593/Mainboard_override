package com.aela.mainboardoverride.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class GameEngineTest {
    @Test fun `same seed produces the same level`() {
        assertEquals(LevelGenerator.generate(42), LevelGenerator.generate(42))
        assertNotEquals(LevelGenerator.generate(42).dominoBag, LevelGenerator.generate(43).dominoBag)
    }

    @Test fun `first tile must connect to start with matching value`() {
        val board = BoardState()
        val route = Domino("route", 0, 3)
        assertTrue(GameEngine.canPlace(board, route, Position(1, 3), Orientation.HORIZONTAL))
        assertFalse(GameEngine.canPlace(board, route, Position(4, 3), Orientation.HORIZONTAL))
        assertFalse(GameEngine.canPlace(board, route.rotated(), Position(1, 3), Orientation.HORIZONTAL))
    }

    @Test fun `external mismatches reject placement but domino halves remain internally connected`() {
        val board = BoardState(placed = listOf(PlacedDomino(Domino("existing", 0, 2), Position(1, 3), Orientation.HORIZONTAL)))
        assertTrue(GameEngine.canPlace(board, Domino("match", 2, 5), Position(3, 3), Orientation.HORIZONTAL))
        assertFalse(GameEngine.canPlace(board, Domino("mismatch", 4, 5), Position(3, 3), Orientation.HORIZONTAL))
    }

    @Test fun `generated networks are varied reproducible and winnable across a thousand seeds`() {
        val layouts = mutableSetOf<Set<Position>>()
        val lengths = mutableSetOf<Int>()
        for (seed in 0L until 1000L) {
            val generated = LevelGenerator.generateVerified(seed)
            assertEquals(generated, LevelGenerator.generateVerified(seed))
            var state = generated.state
            layouts += state.board.firewalls
            lengths += generated.solution.size
            assertTrue(state.board.firewalls.size in 4..7)
            assertTrue(state.board.honeypots.size in 2..4)
            assertTrue(state.board.firewalls.intersect(state.board.honeypots).isEmpty())
            assertTrue((state.board.firewalls + state.board.honeypots).none {
                it == state.board.start || it == state.board.extraction || it.x !in 0 until BOARD_WIDTH || it.y !in 0 until BOARD_HEIGHT
            })
            assertTrue(generated.solution.size in 5..7)
            for (action in generated.solution) {
                val placed = GameEngine.reduce(state, action)
                assertTrue(placed.events.none { it is GameEvent.Rejected }, "seed $seed rejected $action")
                state = GameEngine.reduce(placed.state, GameAction.EndTurn).state
            }
            assertEquals(GameResult.VICTORY, state.result, "seed $seed")
            assertEquals(generated.solution.size * 8, state.trace)
        }
        assertTrue(layouts.size > 900)
        assertEquals(setOf(5, 6, 7), lengths)
    }

    @Test fun `ping consumes ram and reveals traps`() {
        val generated = LevelGenerator.generate(7)
        val ping = ScriptCard("test-ping", ScriptType.PING)
        val result = GameEngine.reduce(generated.copy(scriptHand = listOf(ping)), GameAction.PlayPing(ping.id)).state
        assertEquals(2, result.ram)
        assertEquals(result.board.honeypots, result.board.revealedHoneypots)
        assertEquals(result.dominoBag.take(3), result.pingPreview)
    }

    @Test fun `spoof permanently changes one held port`() {
        val generated = LevelGenerator.generate(8)
        val spoof = ScriptCard("test-spoof", ScriptType.SPOOF)
        val target = generated.dominoHand.first()
        val result = GameEngine.reduce(
            generated.copy(scriptHand = listOf(spoof)),
            GameAction.PlaySpoof(spoof.id, target.id, 0, 6),
        ).state
        assertEquals(6, result.dominoHand.first { it.id == target.id }.first)
        assertEquals(1, result.ram)
        assertEquals(5, result.pendingNoise)
    }

    @Test fun `loss takes priority over extraction when trace reaches maximum`() {
        var state = Tutorial.fixture(TutorialStep.FINAL)
        val placements = listOf(
            Position(1, 3) to Orientation.HORIZONTAL,
            Position(3, 3) to Orientation.HORIZONTAL,
            Position(5, 3) to Orientation.HORIZONTAL,
            Position(7, 2) to Orientation.VERTICAL,
        )
        placements.forEachIndexed { index, target ->
            state = GameEngine.reduce(state, GameAction.PlaceDomino("route-${index + 1}", target.first, target.second)).state
            if (index < placements.lastIndex) state = GameEngine.reduce(state, GameAction.EndTurn).state
        }
        state = state.copy(trace = 99)
        assertEquals(GameResult.TRACE_INTERCEPTED, GameEngine.reduce(state, GameAction.EndTurn).state.result)
    }
}
