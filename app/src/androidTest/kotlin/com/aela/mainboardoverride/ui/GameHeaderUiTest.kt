package com.aela.mainboardoverride.ui

import android.app.Application
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import com.aela.mainboardoverride.MainViewModel
import com.aela.mainboardoverride.data.PlayerPreferences
import com.aela.mainboardoverride.data.PlayerPreferencesRepository
import com.aela.mainboardoverride.domain.GameEngine
import com.aela.mainboardoverride.domain.LevelGenerator
import com.aela.mainboardoverride.domain.Orientation
import com.aela.mainboardoverride.domain.ScriptType
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/** Layout contracts of the band that sits between the header and the PCB. */
class GameHeaderUiTest {
    @get:Rule val compose = createComposeRule()
    private val app = ApplicationProvider.getApplicationContext<Application>()
    private val repository = InMemoryPreferences()

    @Test fun pingPreviewReplacesTheHeaderUntilTheCountdownEnds() {
        val vm = MainViewModel(app, repository)
        compose.setContent {
            val state by vm.uiState.collectAsState()
            MainboardTheme { GameScreen(state, vm) {} }
        }
        // The opening hand comes from a shuffled deck, so pick a seed that starts with PING.
        val seed = generateSequence(1L) { it + 1 }.first { candidate ->
            runCatching { LevelGenerator.generate(candidate) }.getOrNull()
                ?.scriptHand?.any { card -> card.type == ScriptType.PING } == true
        }
        compose.runOnIdle { vm.start(seed) }
        compose.waitUntil(5_000) { vm.uiState.value.game != null }
        val ping = vm.uiState.value.game!!.scriptHand.first { it.type == ScriptType.PING }
        val header = compose.onNodeWithTag("game-header").fetchSemanticsNode().boundsInRoot
        compose.onNodeWithTag("game-header").assertExists()
        // Only the pending noise line fits between the header and the PCB.
        val strip = compose.onNodeWithTag("pending-trace-band").fetchSemanticsNode().boundsInRoot
        val density = app.resources.displayMetrics.density
        assertTrue("Header and PCB stay close: header=$header, strip=$strip",
            strip.top >= header.bottom && strip.bottom - header.bottom <= 16f * density)

        compose.mainClock.autoAdvance = false
        compose.onNodeWithTag("script-${ping.id}").performClick()
        compose.waitForIdle()
        compose.mainClock.advanceTimeBy(200)
        compose.onNodeWithTag("ping-header").assertExists()
        compose.onNodeWithTag("game-header").assertDoesNotExist()

        // Draining the five second preview needs the virtual clock, not the device clock.
        compose.mainClock.autoAdvance = true
        compose.waitForIdle()
        compose.onNodeWithTag("game-header").assertExists()
        compose.onNodeWithTag("ping-header").assertDoesNotExist()
    }

    @Test fun pendingNoiseLineDoesNotPushThePcbDown() {
        val vm = MainViewModel(app, repository)
        compose.setContent {
            val state by vm.uiState.collectAsState()
            MainboardTheme { GameScreen(state, vm) {} }
        }
        compose.runOnIdle { vm.start(123L) }
        compose.waitUntil(5_000) { vm.uiState.value.game != null }
        val before = compose.onNodeWithTag("pending-trace-band").fetchSemanticsNode().boundsInRoot
        val game = vm.uiState.value.game!!
        val placement = GameEngine.legalPlacements(game)
            .first { legal -> game.dominoHand.any { it.id == legal.dominoId } }
        val tile = game.dominoHand.first { it.id == placement.dominoId }
        // Rotation 0 is horizontal and unrotated, 1 vertical, 2 and 3 rotate the halves.
        val steps = when {
            placement.orientation == Orientation.HORIZONTAL && !placement.rotated -> 0
            placement.orientation == Orientation.VERTICAL && !placement.rotated -> 1
            placement.orientation == Orientation.HORIZONTAL -> 2
            else -> 3
        }
        compose.runOnIdle { vm.selectDomino(tile.id); repeat(steps) { vm.rotate() } }
        compose.runOnIdle { vm.placeAt(placement.origin) }
        compose.waitForIdle()
        assertTrue("The tile must be on the board", vm.uiState.value.game!!.tilePlacedThisTurn)
        compose.onNodeWithTag("pending-trace").assertExists()
        assertEquals("The noise line must not move the PCB", before,
            compose.onNodeWithTag("pending-trace-band").fetchSemanticsNode().boundsInRoot)
    }

    /** Keeps the test independent from the real profile store. */
    private class InMemoryPreferences : PlayerPreferencesRepository {
        override val preferences = MutableStateFlow(PlayerPreferences())
        override suspend fun setLanguage(value: String) {}
        override suspend fun setAudio(value: Boolean) {}
        override suspend fun setVibration(value: Boolean) {}
        override suspend fun setReducedMotion(value: Boolean) {}
        override suspend fun setLastSeed(seed: Long) {}
        override suspend fun recordVictory(turns: Int, trace: Int) {}
        override suspend fun setDominoSkin(value: String) {}
        override suspend fun setBoardSkin(value: String) {}
        override suspend fun setContextHelpEnabled(value: Boolean) {}
        override suspend fun recordChallengeVictory(level: Int, turns: Int, trace: Int) {}
    }
}
