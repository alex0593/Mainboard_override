package com.aela.mainboardoverride.ui

import android.app.Application
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.test.core.app.ApplicationProvider
import com.aela.mainboardoverride.MainViewModel
import com.aela.mainboardoverride.R
import com.aela.mainboardoverride.data.*
import com.aela.mainboardoverride.domain.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import org.junit.*
import org.junit.Assert.*
import java.io.File
import java.util.UUID

class PuzzleTutorialUiTest {
    @get:Rule val compose = createComposeRule()
    private val app = ApplicationProvider.getApplicationContext<Application>()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val store = PreferenceDataStoreFactory.create(scope = scope) {
        File(app.cacheDir, "tutorial-${UUID.randomUUID()}.preferences_pb")
    }
    private val repository = DataStorePlayerPreferencesRepository(app, store)
    @After fun close() { scope.cancel() }

    @Test fun challengeOpensDetailsWithoutStartingUntilPlay() {
        val vm = MainViewModel(app, repository)
        compose.setContent {
            val state by vm.uiState.collectAsState()
            MainboardTheme { ChallengeScreen(state, vm, {}, {}) }
        }
        compose.onNodeWithTag("challenge-2").assertIsNotEnabled()
        compose.onNodeWithContentDescription(app.getString(R.string.puzzle_preview)).assertDoesNotExist()
        compose.onNodeWithTag("challenge-1").performClick()
        compose.onNodeWithTag("challenge-dialog").assertIsDisplayed()
        compose.onNodeWithContentDescription(app.getString(R.string.puzzle_preview)).assertDoesNotExist()
        assertDialogActions("play-challenge", "back-challenge")
        assertNull(vm.uiState.value.game)
        compose.onNodeWithTag("play-challenge").performClick()
        compose.waitUntil(5000) { vm.uiState.value.challengeLevel == 1 }
    }

    @Test fun freePlayRequiresDetailsAndLabelsRandomExample() {
        val vm = MainViewModel(app, repository)
        compose.setContent {
            val state by vm.uiState.collectAsState()
            MainboardTheme { ScenarioScreen(state, vm, {}, {}) }
        }
        compose.onNodeWithTag("scenario-lab").assertIsNotEnabled()
        compose.onNodeWithTag("scenario-classic").performClick()
        compose.onNodeWithText(app.getString(R.string.preview_example)).assertDoesNotExist()
        assertDialogActions("play-scenario", "back-scenario")
        compose.onNodeWithText(app.getString(R.string.scenario_route, ScenarioCatalog.get("classic").route)).assertExists()
        assertNull(vm.uiState.value.game)
        compose.onNodeWithTag("play-scenario").performClick()
        compose.waitUntil(5000) { vm.uiState.value.game != null }
    }

    private fun assertDialogActions(play: String, back: String) {
        compose.onNodeWithTag(play).assertIsDisplayed()
        compose.onNodeWithTag(back).assertIsDisplayed()
        val body = compose.onNodeWithTag("dialog-body").fetchSemanticsNode().boundsInRoot
        val actions = compose.onNodeWithTag("dialog-actions").fetchSemanticsNode().boundsInRoot
        assertTrue("Actions remain below the framed body", actions.top >= body.bottom)
    }

    @Test fun tutorialIsPlayableAndCanAdvanceAndRepeat() {
        val controller = TutorialController(repository, scope)
        compose.setContent { MainboardTheme { TutorialScreen(PlayerPreferences(), controller, {}) } }
        compose.onNodeWithTag("tutorial-start").performClick()
        compose.onNodeWithTag("tutorial-action-Next").performClick()
        compose.onNodeWithTag("tutorial-next-lesson").performScrollTo().performClick()
        assertEquals(1, controller.state.value.lesson)
        compose.onNodeWithTag("tutorial-action-Next").performClick()
        compose.onNodeWithTag("tutorial-tile-a").performClick()
        assertEquals(1, controller.state.value.step)
        compose.onNodeWithTag("tutorial-action-Next").performClick()
        compose.onNodeWithText(app.getString(R.string.tutorial_repeat)).performScrollTo().performClick()
        assertEquals(0, controller.state.value.step)
        assertNull(controller.state.value.tile)
    }

    @Test fun completingTutorialPersistsOnlyTutorialProgress() {
        val vm = MainViewModel(app, repository)
        compose.setContent {
            val state by vm.uiState.collectAsState()
            MainboardTheme { TutorialScreen(state.preferences, vm.tutorial, {}) }
        }
        compose.runOnIdle { vm.start(42) }
        compose.waitUntil(5000) { vm.uiState.value.game != null && runBlocking { repository.preferences.first().lastSeed == 42L } }
        val match = vm.uiState.value.game
        val profile = runBlocking { repository.preferences.first() }
        compose.runOnIdle {
            for (lesson in TutorialCatalog.lessons.indices) {
                vm.tutorial.start(lesson)
                TutorialCatalog.lessons[lesson].steps.forEach { vm.tutorial.dispatch(it.input) }
                assertTrue(vm.tutorial.state.value.finished)
            }
        }
        compose.waitUntil(5000) { runBlocking { repository.preferences.first().tutorialCompleted } }
        val after = runBlocking { DataStorePlayerPreferencesRepository(app, store).preferences.first() }
        assertEquals(profile, after.copy(tutorialLesson = profile.tutorialLesson, tutorialCompleted = profile.tutorialCompleted))
        assertEquals(match, vm.uiState.value.game)
        assertEquals(9, after.tutorialLesson)
    }

    @Test fun previewCacheReusesDiskAndSeparatesSkinsWithoutRevealingTraps() = runBlocking {
        val id = "test-${UUID.randomUUID()}"
        val board = BoardState(honeypots = setOf(Position(3, 2)))
        var generations = 0
        val first = PuzzlePreviewCache.load(app, id, "pcb", "kenney") { generations++; board }
        val again = PuzzlePreviewCache.load(app, id, "pcb", "kenney") { error("Memory cache missed") }
        assertSame(first, again)
        PuzzlePreviewCache.clearMemory()
        val disk = PuzzlePreviewCache.load(app, id, "pcb", "kenney") { error("Disk cache missed") }
        assertTrue(first.sameAs(disk))
        assertEquals(1, generations)
        assertEquals(640, disk.width)
        assertEquals(400, disk.height)
        val withoutTrap = PuzzlePreviewCache.load(app, "$id-clean", "pcb", "kenney") { board.copy(honeypots = emptySet()) }
        assertTrue(disk.sameAs(withoutTrap))
        assertNotEquals(PuzzlePreviewCache.key(id, "pcb", "kenney"), PuzzlePreviewCache.key(id, "aurora", "kenney"))
        assertNotEquals(PuzzlePreviewCache.key(id, "pcb", "kenney"), PuzzlePreviewCache.key(id, "pcb", "aurora"))
    }
}
