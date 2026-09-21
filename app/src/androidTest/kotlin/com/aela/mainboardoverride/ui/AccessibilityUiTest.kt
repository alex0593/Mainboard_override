package com.aela.mainboardoverride.ui

import android.app.Application
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ApplicationProvider
import com.aela.mainboardoverride.MainViewModel
import com.aela.mainboardoverride.data.PlayerPreferences
import com.aela.mainboardoverride.data.PlayerPreferencesRepository
import com.aela.mainboardoverride.domain.GameEngine
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/** C02 evidence: large fonts keep controls usable and actionable nodes expose their role. */
class AccessibilityUiTest {
    @get:Rule val compose = createComposeRule()
    private val app = ApplicationProvider.getApplicationContext<Application>()

    @Test fun largeFontsKeepGameControlsUsableAndExposed() {
        val vm = MainViewModel(app, InMemoryPreferences())
        compose.runOnIdle { vm.start(123L) }
        compose.setContent {
            val state by vm.uiState.collectAsState()
            CompositionLocalProvider(LocalDensity provides Density(1f, fontScale = 1.3f)) {
                MainboardTheme {
                    Box(Modifier.requiredSize(640.dp, 360.dp)) { GameScreen(state, vm) {} }
                }
            }
        }
        compose.waitUntil(5_000) { vm.uiState.value.game != null }
        val game = vm.uiState.value.game!!
        compose.runOnIdle { vm.selectDomino(game.dominoHand.first().id) }
        compose.waitForIdle()

        val fixed = compose.onNodeWithTag("end-turn").fetchSemanticsNode().boundsInRoot
        val rotate = compose.onNodeWithTag("rotate").fetchSemanticsNode().boundsInRoot
        val inventory = compose.onNodeWithTag("hardware-panel").fetchSemanticsNode().boundsInRoot
        assertTrue("Controls must not overlap at 1.3x font: rotate=$rotate, endTurn=$fixed", rotate.bottom <= fixed.top)
        assertTrue("Inventory must not overlap controls at 1.3x font", inventory.bottom <= rotate.top)
        assertTrue("Touch targets stay at least 48dp at 1.3x font", rotate.height >= 48f && fixed.height >= 48f)
        compose.onNodeWithTag("general-help").assertIsDisplayed()

        val card = game.scriptHand.first()
        val isButton = SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button)
        compose.onNodeWithTag("script-${card.id}").assert(isButton)
        compose.onNodeWithTag("game-tile-${game.dominoHand.first().id}").assert(isButton)
        val cell = GameEngine.legalPlacements(game).first()
        compose.onNodeWithTag("board-cell-${cell.origin.x}-${cell.origin.y}").assert(isButton)
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
