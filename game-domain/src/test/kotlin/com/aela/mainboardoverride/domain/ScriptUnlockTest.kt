package com.aela.mainboardoverride.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

/** Pin: the band filter must not move a single board of the live seed set. */
class ScriptUnlockTest {
    private fun fingerprint(state: GameState): String = listOf(
        state.board.start, state.board.extraction, state.board.width, state.board.daemon,
        state.board.firewalls, state.board.honeypots, state.board.locks, state.board.waypoints,
        state.dominoHand, state.dominoBag,
    ).joinToString("|")

    @Test fun `challenge boards stay byte-identical to the pre-band baseline`() {
        for (level in listOf(1, 31)) {
            val state = LevelGenerator.generateVerified(ChallengeCatalog.level(level)!!.seed, level).state
            assertEquals(LEVEL_BASELINE[level], fingerprint(state), "challenge $level board drifted")
        }
    }

    @Test fun `the approved band table gates each campaign tier`() {
        assertEquals(setOf(ScriptType.PING, ScriptType.BRIDGE), unlockedScripts(1))
        assertEquals(setOf(ScriptType.PING, ScriptType.BRIDGE), unlockedScripts(30))
        assertEquals(setOf(ScriptType.PING, ScriptType.BRIDGE, ScriptType.SPOOF, ScriptType.CLOAK), unlockedScripts(31))
        assertEquals(setOf(ScriptType.PING, ScriptType.BRIDGE, ScriptType.SPOOF, ScriptType.CLOAK), unlockedScripts(60))
        assertEquals(ScriptType.entries.toSet(), unlockedScripts(61))
        assertEquals(ScriptType.entries.toSet(), unlockedScripts(100))
        assertEquals(ScriptType.entries.toSet(), unlockedScripts(null))
    }

    @Test fun `a challenge only deals scripts from its band`() {
        fun dealt(level: Int, unlockAll: Boolean = false) =
            LevelGenerator.generateVerified(ChallengeCatalog.level(level)!!.seed, level, unlockAll = unlockAll)
                .let { it.state.scriptHand + it.state.scriptDeck }.map { it.type }.toSet()
        assertEquals(setOf(ScriptType.PING, ScriptType.BRIDGE), dealt(1))
        assertEquals(setOf(ScriptType.PING, ScriptType.BRIDGE, ScriptType.SPOOF, ScriptType.CLOAK), dealt(31))
        assertEquals(setOf(ScriptType.PING, ScriptType.BRIDGE, ScriptType.CLOAK), dealt(37)) // banned SPOOF stacks on the band
        assertEquals(ScriptType.entries.toSet(), dealt(61))
        assertEquals(ScriptType.entries.toSet(), dealt(5, unlockAll = true)) // the flag ignores bands…
        assertFalse(ScriptType.SPOOF in dealt(7, unlockAll = true)) // …but never banned scripts
    }

    @Test fun `free play and scenarios keep every script`() {
        val free = LevelGenerator.generate(42L)
        assertEquals(ScriptType.entries.toSet(), (free.scriptHand + free.scriptDeck).map { it.type }.toSet())
        val scenario = LevelGenerator.generateScenario(42L, "ghost")
        assertEquals(ScriptType.entries.toSet(), (scenario.scriptHand + scenario.scriptDeck).map { it.type }.toSet())
    }

    @Test fun `the band filter consumes no randomness`() {
        for (level in listOf(1, 31)) {
            val off = LevelGenerator.generateVerified(ChallengeCatalog.level(level)!!.seed, level)
            val on = LevelGenerator.generateVerified(ChallengeCatalog.level(level)!!.seed, level, unlockAll = true)
            assertEquals(off.state.board, on.state.board, "challenge $level board depends on the flag")
            assertEquals(off.state.dominoHand to off.state.dominoBag, on.state.dominoHand to on.state.dominoBag)
            assertNotEquals(off.state.scriptDeck.map { it.type }.toSet(), on.state.scriptDeck.map { it.type }.toSet())
        }
    }

    @Test fun `a banded challenge plays its whole reference with the six-card pool`() {
        val generated = LevelGenerator.generateVerified(ChallengeCatalog.level(1)!!.seed, 1)
        val pool = generated.state.scriptHand + generated.state.scriptDeck
        assertEquals(6, pool.size)
        assertEquals(setOf(ScriptType.PING, ScriptType.BRIDGE), pool.map { it.type }.toSet())
        var state = generated.state
        for (action in generated.solution) {
            val transition = GameEngine.reduce(state, action)
            assertTrue(transition.events.none { it is GameEvent.Rejected }, "rejection at $action")
            state = GameEngine.reduce(transition.state, GameAction.EndTurn).state
            assertEquals(6, (state.scriptHand + state.scriptDeck).size, "cards created or destroyed")
        }
        assertEquals(GameResult.VICTORY, state.result)
    }

    private companion object {
        val LEVEL_BASELINE = mapOf(
            1 to "Position(x=-1, y=1)|Position(x=9, y=1)|9|Daemon(position=Position(x=9, y=1))|[Position(x=5, y=4), Position(x=7, y=5), Position(x=7, y=4), Position(x=7, y=3), Position(x=3, y=3), Position(x=0, y=5)]|[Position(x=6, y=4), Position(x=1, y=3)]|{}|[]|[Domino(id=hardware-0, first=1, second=0), Domino(id=hardware-5, first=4, second=4), Domino(id=hardware-6, first=1, second=3)]|[Domino(id=hardware-1, first=1, second=6), Domino(id=hardware-2, first=6, second=3), Domino(id=hardware-3, first=6, second=3), Domino(id=hardware-4, first=6, second=3), Domino(id=hardware-7, first=1, second=4), Domino(id=hardware-8, first=0, second=4), Domino(id=hardware-9, first=2, second=6), Domino(id=hardware-10, first=3, second=4), Domino(id=hardware-11, first=3, second=3), Domino(id=hardware-12, first=6, second=6), Domino(id=hardware-13, first=0, second=3), Domino(id=hardware-14, first=1, second=6), Domino(id=hardware-15, first=2, second=4), Domino(id=hardware-16, first=0, second=0)]",
            31 to "Position(x=-1, y=4)|Position(x=11, y=1)|11|Daemon(position=Position(x=11, y=1))|[Position(x=6, y=3), Position(x=0, y=5), Position(x=8, y=0), Position(x=2, y=2), Position(x=7, y=3), Position(x=1, y=5), Position(x=8, y=3), Position(x=10, y=5), Position(x=7, y=4), Position(x=6, y=5), Position(x=3, y=2), Position(x=10, y=2), Position(x=2, y=1), Position(x=8, y=4), Position(x=5, y=5), Position(x=6, y=2), Position(x=7, y=5)]|[Position(x=9, y=3), Position(x=10, y=3), Position(x=3, y=4)]|{}|[]|[Domino(id=hardware-10, first=3, second=5), Domino(id=hardware-11, first=2, second=5), Domino(id=hardware-0, first=0, second=3)]|[Domino(id=hardware-1, first=3, second=3), Domino(id=hardware-2, first=3, second=0), Domino(id=hardware-3, first=6, second=0), Domino(id=hardware-4, first=6, second=6), Domino(id=hardware-5, first=6, second=6), Domino(id=hardware-6, first=6, second=2), Domino(id=hardware-7, first=0, second=2), Domino(id=hardware-8, first=2, second=0), Domino(id=hardware-9, first=2, second=6), Domino(id=hardware-12, first=0, second=2), Domino(id=hardware-13, first=0, second=4), Domino(id=hardware-14, first=0, second=0), Domino(id=hardware-15, first=3, second=6), Domino(id=hardware-16, first=4, second=4), Domino(id=hardware-17, first=2, second=3), Domino(id=hardware-18, first=5, second=5), Domino(id=hardware-19, first=1, second=6), Domino(id=hardware-20, first=0, second=6), Domino(id=hardware-21, first=3, second=3)]",
        )
    }
}
