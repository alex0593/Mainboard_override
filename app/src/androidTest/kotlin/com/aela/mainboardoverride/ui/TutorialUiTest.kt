package com.aela.mainboardoverride.ui

import android.app.Application
import android.content.res.Configuration
import java.util.Locale
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performScrollTo
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import com.aela.mainboardoverride.MainViewModel
import com.aela.mainboardoverride.R
import com.aela.mainboardoverride.data.PlayerPreferences
import com.aela.mainboardoverride.data.PlayerPreferencesRepository
import com.aela.mainboardoverride.domain.Domino
import com.aela.mainboardoverride.domain.Position
import com.aela.mainboardoverride.domain.TutorialStep
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class TutorialUiTest {
    @get:Rule val compose = createComposeRule()
    private val app: Application = ApplicationProvider.getApplicationContext()
    private fun text(id: Int) = app.getString(id)

    @Test fun spoofPreviewsBothPortsAndCancellationDoesNotApply() {
        var applied = 0
        var cancelled = false
        compose.setContent {
            var half by remember { mutableStateOf(0) }
            var value by remember { mutableStateOf(0) }
            SpoofDialog(Domino("test", 3, 2), half, value, null, false,
                { half = it }, { value = it }, { applied++ }, { cancelled = true })
        }
        compose.onNodeWithText(text(R.string.second_half)).performClick().assertIsSelected()
        compose.onNodeWithText("6").performClick()
        compose.onNodeWithContentDescription(app.getString(R.string.domino_description, 3, 6)).assertIsDisplayed()
        compose.onNodeWithText(text(R.string.first_half)).performClick().assertIsSelected()
        compose.onNodeWithContentDescription(app.getString(R.string.domino_description, 6, 2)).assertIsDisplayed()
        compose.onNodeWithText(text(R.string.cancel)).performClick()
        compose.runOnIdle { assertTrue(cancelled); assertEquals(0, applied) }
    }

    @Test fun bridgeOrientationCanBeToggledBothWays() {
        compose.setContent {
            var horizontal by remember { mutableStateOf(true) }
            BridgeControl(horizontal) { horizontal = !horizontal }
        }
        val horizontal = app.getString(R.string.bridge_label, text(R.string.horizontal))
        val vertical = app.getString(R.string.bridge_label, text(R.string.vertical))
        compose.onNodeWithText(horizontal).performClick()
        compose.onNodeWithText(vertical).performClick()
        compose.onNodeWithText(horizontal).assertIsDisplayed()
    }

    @Test fun pingShowsUpcomingHardware() {
        compose.setContent { PingPreview(listOf(Domino("one", 2, 3), Domino("two", 4, 6))) }
        compose.onNodeWithText(text(R.string.ping_preview)).assertIsDisplayed()
        compose.onNodeWithContentDescription(app.getString(R.string.domino_description, 2, 3)).assertIsDisplayed()
        compose.onNodeWithContentDescription(app.getString(R.string.domino_description, 4, 6)).assertIsDisplayed()
    }

    @Test fun completeTutorialDoesNotWriteNormalRecordsAndCompletionIsWrittenOnce() {
        val repository = FakePreferences()
        lateinit var vm: MainViewModel
        compose.runOnIdle { vm = MainViewModel(app, repository); vm.start(tutorial = true) }
        compose.setContent {
            val state by vm.uiState.collectAsState()
            state.tutorialStep?.let { TutorialPanel(it, state.tutorialBlocked, vm::continueTutorial, vm::restartLesson) }
        }
        compose.onNodeWithText(text(R.string.continue_lesson)).performClick()
        compose.runOnIdle { assertEquals(TutorialStep.SELECT, vm.uiState.value.tutorialStep); vm.endTurn() }
        compose.waitForIdle()
        compose.runOnIdle { assertTrue(vm.uiState.value.tutorialBlocked); vm.selectDomino("lesson"); vm.rotate(); vm.placeAt(Position(1, 3)); vm.endTurn() }
        compose.waitForIdle()
        compose.runOnIdle { assertEquals(TutorialStep.SYSTEM, vm.uiState.value.tutorialStep); vm.continueTutorial(); vm.selectScript("lesson-PING") }
        compose.waitForIdle()
        compose.runOnIdle { assertEquals(TutorialStep.PING_RESULT, vm.uiState.value.tutorialStep); vm.restartLesson() }
        compose.waitForIdle()
        compose.runOnIdle { assertEquals(TutorialStep.PING, vm.uiState.value.tutorialStep); vm.selectScript("lesson-PING"); vm.continueTutorial(); vm.selectScript("lesson-SPOOF"); vm.selectDomino("lesson"); vm.cancelScript() }
        compose.waitForIdle()
        compose.runOnIdle {
            assertEquals(3, vm.uiState.value.game!!.ram)
            assertEquals(3, vm.uiState.value.game!!.dominoHand.single().first)
            vm.selectScript("lesson-SPOOF"); vm.selectDomino("lesson"); vm.spoof(0); vm.continueTutorial()
            vm.selectScript("lesson-KILL_PROCESS"); vm.placeAt(Position(1, 3)); vm.continueTutorial()
            vm.selectScript("lesson-BRIDGE"); vm.placeAt(Position(3, 3)); vm.continueTutorial()
        }
        compose.waitForIdle()
        compose.runOnIdle {
            assertEquals(TutorialStep.FINAL, vm.uiState.value.tutorialStep)
            listOf(Position(1, 3), Position(3, 3), Position(5, 3), Position(7, 2)).forEachIndexed { index, position ->
                vm.selectDomino("route-${index + 1}")
                if (index == 3) vm.rotate()
                vm.placeAt(position); vm.endTurn()
            }
        }
        compose.waitForIdle()
        compose.runOnIdle { assertEquals(TutorialStep.COMPLETE, vm.uiState.value.tutorialStep); vm.endTurn() }
        compose.waitForIdle()
        compose.runOnIdle {
            assertEquals(1, repository.completions)
            assertEquals(0, repository.records)
            assertNull(repository.preferences.value.lastSeed)
        }
    }

    @Test fun guidedBoardWorksInSpanishWithLargeText() = guidedBoard("es")
    @Test fun guidedBoardWorksInEnglishWithLargeText() = guidedBoard("en")

    private fun guidedBoard(language: String) {
        val configuration = Configuration(app.resources.configuration).apply { setLocale(Locale.forLanguageTag(language)) }
        val context = app.createConfigurationContext(configuration)
        lateinit var vm: MainViewModel
        compose.runOnIdle { vm = MainViewModel(app, FakePreferences()); vm.start(tutorial = true) }
        compose.setContent {
            val state by vm.uiState.collectAsState()
            CompositionLocalProvider(LocalContext provides context, LocalConfiguration provides configuration,
                LocalDensity provides Density(1f, fontScale = 1.3f)) {
                MainboardTheme {
                    Box(Modifier.requiredSize(640.dp, 360.dp)) { GameScreen(state, vm) {} }
                }
            }
        }
        fun localized(id: Int) = context.getString(id)
        compose.onNodeWithText(localized(R.string.continue_lesson)).performScrollTo().performClick()
        compose.onNodeWithContentDescription(context.getString(R.string.domino_description, 0, 2)).performScrollTo().performClick()
        compose.onNodeWithText(localized(R.string.rotate)).performScrollTo().performClick()
        val prefix = if (language == "es") "Nodo" else "Node"
        val start = compose.onNode(hasContentDescription("$prefix 1, 4:", substring = true)).fetchSemanticsNode().boundsInRoot
        val target = compose.onNode(hasContentDescription("$prefix 2, 4:", substring = true))
        assertTrue(target.fetchSemanticsNode().boundsInRoot.left > start.left)
        target.performClick()
        compose.onNodeWithText(localized(R.string.end_turn)).performScrollTo().performClick()
        compose.waitForIdle()
        compose.runOnIdle { assertEquals(TutorialStep.SYSTEM, vm.uiState.value.tutorialStep) }
    }

    private class FakePreferences : PlayerPreferencesRepository {
        override val preferences = MutableStateFlow(PlayerPreferences())
        var completions = 0
        var records = 0
        override suspend fun setLanguage(value: String) {}
        override suspend fun setAudio(value: Boolean) {}
        override suspend fun setVibration(value: Boolean) {}
        override suspend fun setReducedMotion(value: Boolean) {}
        override suspend fun markTutorialComplete() { completions++; preferences.value = preferences.value.copy(tutorialComplete = true) }
        override suspend fun setLastSeed(seed: Long) { preferences.value = preferences.value.copy(lastSeed = seed) }
        override suspend fun recordVictory(turns: Int, trace: Int) { records++ }
    }
}
