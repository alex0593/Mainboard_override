package com.aela.mainboardoverride.ui

import android.app.Application
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.activity.ComponentActivity
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.test.core.app.ApplicationProvider
import com.aela.mainboardoverride.GameUiState
import com.aela.mainboardoverride.MainViewModel
import com.aela.mainboardoverride.R
import com.aela.mainboardoverride.data.DataStorePlayerPreferencesRepository
import com.aela.mainboardoverride.data.PlayerPreferences
import com.aela.mainboardoverride.domain.LevelGenerator
import com.aela.mainboardoverride.domain.ScenarioCatalog
import java.io.File
import java.util.UUID
import kotlinx.coroutines.*
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class ScenarioArtworkUiTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    @Test fun scenarioArtworkRendersWithSkinsAndLockedCards() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        val store = PreferenceDataStoreFactory.create(scope = scope) {
            File(app.cacheDir, "scenario-art-${UUID.randomUUID()}.preferences_pb")
        }
        val vm = MainViewModel(app, DataStorePlayerPreferencesRepository(app, store))
        val state = mutableStateOf(GameUiState())
        val selection = mutableStateOf(true)
        try {
            compose.activityRule.scenario.onActivity {
                it.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
            }
            compose.waitUntil(5000) {
                compose.activity.resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
            }
            val resources = ScenarioCatalog.all.map { scenarioBackgroundResource(it.id) }
            assertEquals(7, resources.toSet().size)
            assertEquals(scenarioBackgroundResource("classic"), scenarioBackgroundResource("unknown"))
            assertEquals(scenarioBackgroundResource("lab"), menuBackgroundResource("lab"))
            assertEquals(scenarioBackgroundResource("classic"), menuBackgroundResource("unknown"))
            resources.forEach { resource ->
                val bitmap = BitmapFactory.decodeResource(app.resources, resource)
                assertEquals(1280, bitmap.width)
                assertEquals(640, bitmap.height)
                bitmap.recycle()
            }
            compose.setContent {
                MainboardTheme {
                    if (selection.value) ScenarioScreen(state.value, vm, {}, {})
                    else GameScreen(state.value, vm) {}
                }
            }
            compose.onNodeWithTag("scenario-classic").assertIsEnabled()
            compose.onNodeWithTag("scenario-lab").assertIsNotEnabled()
            compose.waitUntil(10000) {
                compose.onAllNodesWithContentDescription(app.getString(R.string.puzzle_preview))
                    .fetchSemanticsNodes().isNotEmpty()
            }
            capture(app, "selection")
            compose.onNode(hasScrollToNodeAction()).performScrollToNode(hasTestTag("scenario-ghost"))
            compose.onNodeWithTag("scenario-ghost").assertIsNotEnabled()
            capture(app, "selection-locked")
            for (scenario in ScenarioCatalog.all) {
                for (skin in listOf("pcb", "graphite")) {
                    compose.runOnIdle {
                        selection.value = false
                        state.value = GameUiState(
                            game = LevelGenerator.generateScenario(42, scenario.id),
                            scenarioId = scenario.id,
                            preferences = PlayerPreferences(boardSkin = skin),
                        )
                    }
                    compose.onNodeWithTag("restart-game").assertIsDisplayed()
                    capture(app, "${scenario.id}-$skin")
                }
            }
        } finally {
            scope.cancel()
        }
    }

    private fun capture(app: Application, name: String) {
        compose.waitForIdle()
        val directory = File(app.getExternalFilesDir(null), "scenario-validation").apply { mkdirs() }
        File(directory, "$name.png").outputStream().use {
            compose.onRoot().captureToImage().asAndroidBitmap().compress(Bitmap.CompressFormat.PNG, 100, it)
        }
    }
}
