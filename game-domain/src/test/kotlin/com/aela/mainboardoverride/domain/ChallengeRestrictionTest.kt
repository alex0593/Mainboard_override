package com.aela.mainboardoverride.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** E02 hand restrictions: banned script types are never dealt in those challenges. */
class ChallengeRestrictionTest {
    private val restricted = setOf(7, 17, 27)

    private fun fullReplay(level: ChallengeLevel): GameState {
        val generated = LevelGenerator.generateVerified(level.seed, level.number)
        var state = generated.state
        for (action in generated.solution) {
            assertTrue(
                (state.scriptHand + state.scriptDeck).none { it.type == ScriptType.SPOOF },
                "challenge ${level.number} dealt SPOOF before $action",
            )
            state = GameEngine.reduce(GameEngine.reduce(state, action).state, GameAction.EndTurn).state
        }
        assertTrue(
            (state.scriptHand + state.scriptDeck).none { it.type == ScriptType.SPOOF },
            "challenge ${level.number} dealt SPOOF by the end",
        )
        return state
    }
    @Test fun `restricted challenges never deal the banned script`() {
        for (number in restricted) {
            val level = ChallengeCatalog.level(number) ?: error("missing level $number")
            assertEquals(setOf(ScriptType.SPOOF), level.rules.bannedScripts)
            val end = fullReplay(level)
            assertEquals(GameResult.VICTORY, end.result, "challenge $number")
        }
    }

    @Test fun `unrestricted challenges still deal every script type`() {
        val dealt = mutableSetOf<ScriptType>()
        for (level in ChallengeCatalog.levels) {
            if (level.number in restricted) continue
            val generated = LevelGenerator.generateVerified(level.seed, level.number)
            dealt += generated.state.scriptHand.map { it.type }
            dealt += generated.state.scriptDeck.map { it.type }
        }
        assertEquals(ScriptType.entries.toSet(), dealt)
    }
}
