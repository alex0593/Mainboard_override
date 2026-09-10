package com.aela.mainboardoverride.ui

import android.app.Application
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
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

class ProgressionUiTest {
    @get:Rule val compose = createComposeRule()
    private val app = ApplicationProvider.getApplicationContext<Application>()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val store = PreferenceDataStoreFactory.create(scope = scope) {
        File(app.cacheDir, "progression-${UUID.randomUUID()}.preferences_pb")
    }
    private val repository = DataStorePlayerPreferencesRepository(app, store)

    @After fun closeStore() { scope.cancel() }

    @Test fun rewardsPurchasesAndMilestonesAreAtomic() = runBlocking {
        assertFalse(repository.buySkin("copper"))
        repository.setBoardSkin("copper")
        assertEquals("pcb", repository.preferences.first().boardSkin)
        for (level in 1..30) {
            val reward = repository.finishMatch("challenge-$level", level, 9, 72)
            assertEquals(60, reward.base + reward.bonus)
            assertEquals(ScenarioCatalog.all.firstOrNull { it.required == level }?.id, reward.unlockedScenario)
            assertEquals(MatchReward(), repository.finishMatch("challenge-$level", level, 9, 72))
        }
        assertEquals(1800, repository.preferences.first().credits)
        assertEquals(30, repository.preferences.first().challengeBest.size)
        val replay = repository.finishMatch("replay", 1, 5, 40)
        assertEquals(20, replay.base)
        assertEquals(0, replay.bonus)
        assertNull(replay.unlockedScenario)
        val purchases = coroutineScope { List(4) { async { repository.buySkin("copper") } }.awaitAll() }
        assertEquals(1, purchases.count { it })
        assertEquals(1620, repository.preferences.first().credits)
        repository.setBoardSkin("copper")
        assertEquals("copper", repository.preferences.first().boardSkin)
        assertFalse(repository.buySkin("unknown"))
        val reread = DataStorePlayerPreferencesRepository(app, store).preferences.first()
        assertEquals(setOf("copper"), reread.ownedSkins)
        assertEquals(1620, reread.credits)
        val payouts = coroutineScope { List(4) { async { repository.finishMatch("free-match", null, 6, 48) } }.awaitAll() }
        assertEquals(20, payouts.sumOf { it.base + it.bonus })
        assertEquals(1640, repository.preferences.first().credits)
        assertEquals(6, repository.preferences.first().bestTurns)
    }

    @Test fun existingProgressMigratesWithoutRetroactiveCredits() = runBlocking {
        store.edit {
            it[stringPreferencesKey("domino_skin")] = "neon"
            it[intPreferencesKey("challenge_unlocked")] = 10
            it[stringPreferencesKey("challenge_best")] = (1..10).joinToString(",") { level -> "$level:9:72" }
        }
        val prefs = repository.preferences.first()
        assertEquals("kenney", prefs.dominoSkin)
        assertEquals(11, prefs.challengeUnlocked)
        assertEquals(0, prefs.credits)
        assertEquals(3, ScenarioCatalog.all.count { it.required <= prefs.challengeBest.size })
    }

    @Test fun galleryCanBuyAndEquipPcb() {
        runBlocking { for (level in 1..4) repository.finishMatch("fund-$level", level, 5, 40) }
        val vm = MainViewModel(app, repository)
        compose.setContent {
            val state by vm.uiState.collectAsState()
            MainboardTheme { SkinGallery(state, vm) {} }
        }
        compose.waitUntil(5000) { vm.uiState.value.preferences.credits == 240 }
        compose.onNodeWithText(app.getString(R.string.board_skin)).performClick()
        compose.onNode(hasScrollToNodeAction()).performScrollToNode(hasTestTag("skin-action-copper"))
        compose.onNodeWithTag("skin-action-copper").assertIsDisplayed().assertIsEnabled().performClick()
        compose.onNodeWithText(app.getString(R.string.confirm_skin_purchase, 200, 40)).assertIsDisplayed()
        compose.onNodeWithTag("confirm-purchase").performClick()
        compose.waitUntil(5000) { runBlocking { "copper" in repository.preferences.first().ownedSkins } }
        compose.onNodeWithTag("skin-action-copper").performClick()
        compose.waitUntil(5000) { runBlocking { repository.preferences.first().boardSkin == "copper" } }
        assertEquals(40, runBlocking { repository.preferences.first().credits })
    }

    @Test fun resultCanBeDismissedToInspectBoardWithoutRewardingAgain() {
        val vm = MainViewModel(app, repository)
        compose.runOnIdle { vm.start(42) }
        compose.setContent {
            val state by vm.uiState.collectAsState()
            MainboardTheme {
                GameScreen(state.copy(game = LevelGenerator.generate(42).copy(result = GameResult.VICTORY), reward = MatchReward(20)), vm) {}
            }
        }
        compose.onNodeWithText(app.getString(R.string.review_board)).performClick()
        compose.onNodeWithText(app.getString(R.string.result_summary)).assertIsDisplayed().performClick()
        compose.onNodeWithText(app.getString(R.string.review_board)).assertIsDisplayed()
        assertEquals(0, runBlocking { repository.preferences.first().credits })
    }

    @Test fun lockedScenarioCannotStartAndUnlocksAtFiveWins() {
        val vm = MainViewModel(app, repository)
        compose.setContent {
            val state by vm.uiState.collectAsState()
            MainboardTheme { ScenarioScreen(state, vm, {}, {}) }
        }
        compose.onAllNodesWithText(app.getString(R.string.locked_scenario))[0].assertIsNotEnabled()
        compose.runOnIdle { vm.startScenario("lab") }
        assertNull(vm.uiState.value.game)
        runBlocking { for (level in 1..5) repository.finishMatch("unlock-$level", level, 5, 40) }
        compose.waitUntil(5000) { vm.uiState.value.preferences.challengeBest.size == 5 }
        compose.runOnIdle { vm.startScenario("lab", 42) }
        compose.waitUntil(5000) { vm.uiState.value.scenarioId == "lab" }
        assertEquals(8, vm.uiState.value.game!!.board.width)
        compose.runOnIdle { vm.retry() }
        compose.waitForIdle()
        assertEquals(42L, vm.uiState.value.game!!.seed)
        assertEquals("lab", vm.uiState.value.scenarioId)
    }

    @Test fun restartingChallengeKeepsLevelAndGeneratesNewSeed() {
        val vm = MainViewModel(app, repository)
        compose.setContent {
            val state by vm.uiState.collectAsState()
            MainboardTheme { GameScreen(state, vm) {} }
        }
        compose.runOnIdle { vm.startChallenge(1) }
        compose.waitUntil(5000) { vm.uiState.value.game != null }
        val firstSeed = vm.uiState.value.game!!.seed
        compose.onNodeWithTag("restart-game").performClick()
        compose.waitForIdle()
        assertEquals(1, vm.uiState.value.challengeLevel)
        assertNotEquals(firstSeed, vm.uiState.value.game!!.seed)
    }

    @Test fun restartingFreeNetworkKeepsModeAndGeneratesNewSeed() {
        val vm = MainViewModel(app, repository)
        compose.setContent {
            val state by vm.uiState.collectAsState()
            MainboardTheme { GameScreen(state, vm) {} }
        }
        compose.runOnIdle { vm.startScenario("classic", 42L) }
        compose.waitUntil(5000) { vm.uiState.value.game != null }
        val firstSeed = vm.uiState.value.game!!.seed
        compose.onNodeWithTag("restart-game").performClick()
        compose.waitForIdle()
        assertEquals("classic", vm.uiState.value.scenarioId)
        assertNull(vm.uiState.value.challengeLevel)
        assertNotEquals(firstSeed, vm.uiState.value.game!!.seed)
    }

    @Test fun menuPulsesStopWithReducedMotion() {
        val reduced = mutableStateOf(false)
        compose.mainClock.autoAdvance = false
        compose.setContent { MainboardTheme { CircuitBackground(animated = !reduced.value) {} } }
        compose.mainClock.advanceTimeBy(32)
        val first = compose.onRoot().captureToImage().toPixelMap()
        compose.mainClock.advanceTimeBy(1000)
        val second = compose.onRoot().captureToImage().toPixelMap()
        var changed = false
        for (x in 0 until first.width step 3) for (y in 0 until first.height step 3) {
            if (first[x, y] != second[x, y]) changed = true
        }
        assertTrue(changed)
        compose.runOnUiThread { reduced.value = true }
        compose.mainClock.advanceTimeBy(32)
        val still = compose.onRoot().captureToImage().toPixelMap()
        compose.mainClock.advanceTimeBy(1000)
        val later = compose.onRoot().captureToImage().toPixelMap()
        for (x in 0 until still.width step 3) for (y in 0 until still.height step 3) assertEquals(still[x, y], later[x, y])
        compose.mainClock.autoAdvance = true
    }
}
