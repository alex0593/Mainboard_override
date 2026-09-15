package com.aela.mainboardoverride.ui

import android.app.Application
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import com.aela.mainboardoverride.MainViewModel
import com.aela.mainboardoverride.data.PlayerPreferences
import com.aela.mainboardoverride.data.PlayerPreferencesRepository
import com.aela.mainboardoverride.domain.GameAction
import com.aela.mainboardoverride.domain.GameEngine
import com.aela.mainboardoverride.domain.GameState
import com.aela.mainboardoverride.domain.LevelGenerator
import com.aela.mainboardoverride.domain.Orientation
import com.aela.mainboardoverride.domain.PlacedDomino
import com.aela.mainboardoverride.domain.ScriptType
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Rule
import org.junit.Test

/** KILL must burst exactly where it destroyed something, and never when a match is rebuilt. */
class KillBurstUiTest {
    @get:Rule val compose = createComposeRule()
    private val app = ApplicationProvider.getApplicationContext<Application>()
    private val repository = InMemoryPreferences()

    @Test fun killBurstsOnTheDestroyedFootprintAndFadesAway() {
        val vm = game(killSeed())
        val kill = vm.uiState.value.game!!.scriptHand.first { it.type == ScriptType.KILL_PROCESS }
        val placed = placeDomino(vm)
        compose.onNodeWithTag("kill-burst").assertDoesNotExist()

        compose.mainClock.autoAdvance = false
        compose.onNodeWithTag("script-${kill.id}").performClick()
        compose.waitForIdle()
        // Aim at one half of the domino KILL removes.
        compose.onNodeWithTag("board-cell-${placed.positions.first.x}-${placed.positions.first.y}").performClick()
        compose.waitForIdle()
        compose.mainClock.advanceTimeBy(60)
        compose.onNodeWithTag("kill-burst").assertExists()

        compose.mainClock.autoAdvance = true
        compose.waitUntil(3_000) { compose.onAllNodesWithTag("kill-burst").fetchSemanticsNodes().isEmpty() }
    }

    @Test fun restartingAMatchNeverBurstsOnItsOwn() {
        val vm = game(123L)
        placeDomino(vm)
        compose.onNodeWithTag("kill-burst").assertDoesNotExist()
        val previousMatch = vm.uiState.value.matchId

        compose.onNodeWithTag("restart-game").performClick()
        compose.onNodeWithTag("confirm-dialog").performClick()
        compose.waitUntil(5_000) { vm.uiState.value.matchId != previousMatch }
        // A rebuilt board removes firewalls and dominoes from the diff, never a hidden kill.
        compose.mainClock.autoAdvance = false
        compose.waitForIdle()
        compose.mainClock.advanceTimeBy(120)
        compose.onNodeWithTag("kill-burst").assertDoesNotExist()
        compose.mainClock.autoAdvance = true
    }

    private fun game(seed: Long): MainViewModel {
        val vm = MainViewModel(app, repository)
        compose.setContent {
            val state by vm.uiState.collectAsState()
            MainboardTheme { GameScreen(state, vm) {} }
        }
        compose.runOnIdle { vm.start(seed) }
        compose.waitUntil(5_000) { vm.uiState.value.game != null }
        return vm
    }

    /** Plays a legal opening domino that cannot trigger a trap, so the script hand survives. */
    private fun placeDomino(vm: MainViewModel): PlacedDomino {
        val game = vm.uiState.value.game!!
        val placement = GameEngine.legalPlacements(game).first { legal -> placedDomino(game, legal).positions.toList().none { it in game.board.honeypots } }
        val tile = game.dominoHand.first { it.id == placement.dominoId }
        val steps = when {
            placement.orientation == Orientation.HORIZONTAL && !placement.rotated -> 0
            placement.orientation == Orientation.VERTICAL && !placement.rotated -> 1
            placement.orientation == Orientation.HORIZONTAL -> 2
            else -> 3
        }
        compose.runOnIdle { vm.selectDomino(tile.id); repeat(steps) { vm.rotate() } }
        compose.runOnIdle { vm.placeAt(placement.origin) }
        compose.waitForIdle()
        return placedDomino(game, placement)
    }

    private fun placedDomino(game: GameState, legal: GameAction.PlaceDomino): PlacedDomino {
        val tile = game.dominoHand.first { it.id == legal.dominoId }
        return PlacedDomino(if (legal.rotated) tile.rotated() else tile, legal.origin, legal.orientation)
    }

    private fun killSeed(): Long = generateSequence(1L) { it + 1 }.first { candidate ->
        runCatching { LevelGenerator.generate(candidate) }.getOrNull()
            ?.scriptHand?.any { card -> card.type == ScriptType.KILL_PROCESS } == true
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
