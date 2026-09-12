package com.aela.mainboardoverride.domain

import kotlin.test.*

class TutorialTest {
    private fun complete(lesson: Int): TutorialState {
        var state = TutorialCatalog.start(lesson)
        for (step in state.definition.steps) {
            val before = state.step
            state = TutorialCatalog.reduce(state, step.input)
            assertFalse(state.incorrect, "Lesson $lesson instruction ${step.instruction} rejected")
            assertEquals(before + 1, state.step)
        }
        assertTrue(state.finished)
        return state
    }
    @Test fun everyLessonRunsThroughRealEngine() {
        TutorialCatalog.lessons.indices.forEach { complete(it) }
    }
    @Test fun unexpectedActionCannotMutatePractice() {
        val original = TutorialCatalog.start(1)
        assertEquals(original.copy(incorrect = true), TutorialCatalog.reduce(original, TutorialInput.EndTurn))
        assertEquals(original, TutorialCatalog.start(1))
    }
    @Test fun spoofCancellationSpendsNothing() {
        var state = TutorialCatalog.start(6)
        val initial = state.game
        state.definition.steps.take(5).forEach { state = TutorialCatalog.reduce(state, it.input) }
        assertEquals(initial, state.game)
        assertNull(state.tile)
        assertNull(state.script)
        val done = complete(6)
        assertEquals(1, done.game.ram)
        assertEquals(5, done.game.pendingNoise)
        assertEquals(0, done.game.board.placed.single().domino.first)
    }
    @Test fun scriptsHaveRealEffects() {
        assertEquals(GameResult.VICTORY, complete(9).game.result)
        assertTrue(complete(7).game.board.firewalls.isEmpty())
        assertEquals(1, complete(8).game.board.bridges.size)
        assertTrue(complete(5).game.board.revealedHoneypots.isNotEmpty())
    }
    @Test fun restartingLessonRestoresFixture() {
        complete(4)
        assertEquals(TutorialCatalog.lessons[4].initial, TutorialCatalog.start(4).game)
        assertEquals(0, TutorialCatalog.start(-1).lesson)
        assertEquals(9, TutorialCatalog.start(100).lesson)
    }
}
