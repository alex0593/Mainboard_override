package com.aela.mainboardoverride.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TutorialTest {
    @Test fun `basic placement resolves a turn without ending the exercise`() {
        val initial = Tutorial.fixture(TutorialStep.INTRO)
        val action = GameAction.PlaceDomino("lesson", Position(1, 3), Orientation.VERTICAL)
        val placed = GameEngine.reduce(initial, action)
        assertEquals(TutorialStep.END_TURN, Tutorial.after(TutorialStep.PLACE, action, placed))
        val resolved = GameEngine.reduce(placed.state, GameAction.EndTurn)
        assertEquals(TutorialStep.SYSTEM, Tutorial.after(TutorialStep.END_TURN, GameAction.EndTurn, resolved))
        assertEquals(null, resolved.state.result)
        assertEquals(8, resolved.state.trace)
        assertEquals(MAX_RAM, resolved.state.ram)
    }

    @Test fun `all script exercises accept their objective and preserve the effect for inspection`() {
        val exercises = listOf(
            TutorialStep.PING to GameAction.PlayPing("lesson-PING"),
            TutorialStep.SPOOF to GameAction.PlaySpoof("lesson-SPOOF", "lesson", 0, 0),
            TutorialStep.KILL to GameAction.PlayKillProcess("lesson-KILL_PROCESS", Position(1, 3)),
            TutorialStep.BRIDGE to GameAction.PlayBridge("lesson-BRIDGE", Position(3, 3), true),
        )
        exercises.forEach { (step, action) ->
            val initial = Tutorial.fixture(step)
            assertEquals(initial, Tutorial.fixture(step))
            val result = GameEngine.reduce(initial, action)
            assertTrue(result.events.any { it is GameEvent.ScriptExecuted })
            assertEquals(TutorialStep.entries[step.ordinal + 1], Tutorial.after(step, action, result))
            when (step) {
                TutorialStep.PING -> { assertEquals(initial.dominoBag, result.state.pingPreview); assertEquals(initial.board.honeypots, result.state.board.revealedHoneypots) }
                TutorialStep.SPOOF -> assertEquals(Domino("lesson", 0, 2), result.state.dominoHand.single())
                TutorialStep.KILL -> assertTrue(result.state.board.firewalls.isEmpty())
                TutorialStep.BRIDGE -> { assertEquals(1, result.state.board.bridges.size); assertEquals(initial.board.firewalls, result.state.board.firewalls) }
                else -> error("unexpected exercise")
            }
            assertEquals(step, Tutorial.lessonStart(Tutorial.after(step, action, result)))
        }
    }

    @Test fun `rejected and unrelated actions never advance lessons`() {
        val state = Tutorial.fixture(TutorialStep.PING).copy(ram = 0)
        val action = GameAction.PlayPing("lesson-PING")
        assertEquals(TutorialStep.PING, Tutorial.after(TutorialStep.PING, action, GameEngine.reduce(state, action)))
        assertFalse(Tutorial.allows(TutorialStep.SPOOF, GameAction.PlaySpoof("lesson-SPOOF", "lesson", 1, 0)))
        assertFalse(Tutorial.allows(TutorialStep.BRIDGE, GameAction.PlayBridge("lesson-BRIDGE", Position(3, 3), false)))
        TutorialStep.entries.filter { it.explanation }.forEach {
            assertFalse(Tutorial.allows(it, GameAction.EndTurn))
            assertTrue(Tutorial.continueFrom(it) != it)
        }
        assertEquals(TutorialStep.PING, Tutorial.continueFrom(TutorialStep.PING))
    }

    @Test fun `final practice requires victory after system resolution`() {
        var state = Tutorial.fixture(TutorialStep.FINAL)
        var step = TutorialStep.FINAL
        listOf(Position(1, 3), Position(3, 3), Position(5, 3), Position(7, 2)).forEachIndexed { i, origin ->
            val action = GameAction.PlaceDomino("route-${i + 1}", origin, if (i == 3) Orientation.VERTICAL else Orientation.HORIZONTAL)
            val placed = GameEngine.reduce(state, action)
            assertFalse(placed.events.any { it is GameEvent.Rejected })
            assertEquals(TutorialStep.FINAL, Tutorial.after(step, action, placed))
            val ended = GameEngine.reduce(placed.state, GameAction.EndTurn)
            step = Tutorial.after(step, GameAction.EndTurn, ended)
            state = ended.state
        }
        assertEquals(GameResult.VICTORY, state.result)
        assertEquals(TutorialStep.COMPLETE, step)
    }

    @Test fun `vertical bridge and second spoof port use existing engine rules`() {
        val base = Tutorial.fixture(TutorialStep.BRIDGE)
        val board = BoardState(firewalls = setOf(Position(2, 3)), placed = listOf(
            PlacedDomino(Domino("top", 0, 2), Position(1, 2), Orientation.HORIZONTAL),
            PlacedDomino(Domino("bottom", 2, 6), Position(2, 4), Orientation.HORIZONTAL),
        ))
        val bridge = GameEngine.reduce(base.copy(board = board), GameAction.PlayBridge("lesson-BRIDGE", Position(2, 3), false))
        assertEquals(false, bridge.state.board.bridges.single().horizontal)
        val spoof = GameEngine.reduce(Tutorial.fixture(TutorialStep.SPOOF), GameAction.PlaySpoof("lesson-SPOOF", "lesson", 1, 6))
        assertEquals(Domino("lesson", 3, 6), spoof.state.dominoHand.single())
    }
}
