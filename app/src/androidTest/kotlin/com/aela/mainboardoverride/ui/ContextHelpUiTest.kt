package com.aela.mainboardoverride.ui

import android.app.Application
import android.content.res.Configuration
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import android.view.KeyEvent
import com.aela.mainboardoverride.MainViewModel
import com.aela.mainboardoverride.R
import com.aela.mainboardoverride.data.PlayerPreferences
import com.aela.mainboardoverride.data.PlayerPreferencesRepository
import com.aela.mainboardoverride.domain.Domino
import com.aela.mainboardoverride.domain.Orientation
import com.aela.mainboardoverride.domain.ScriptCard
import com.aela.mainboardoverride.domain.ScriptType
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.util.Locale

class ContextHelpUiTest {
    @get:Rule val compose = createComposeRule()
    private val app: Application = ApplicationProvider.getApplicationContext()

    @Test fun allHelpTopicsInSpanishPreserveSelectionAndResources() = checkHelp("es", 3)
    @Test fun allHelpTopicsInEnglishWithNoRamRemainAvailable() = checkHelp("en", 0)

    private fun checkHelp(language: String, ram: Int) {
        val config = Configuration(app.resources.configuration).apply { setLocale(Locale.forLanguageTag(language)) }
        val context = app.createConfigurationContext(config)
        lateinit var vm: MainViewModel
        compose.runOnIdle { vm = MainViewModel(app, HelpPreferences()); vm.start(123L) }
        compose.setContent {
            val state by vm.uiState.collectAsState()
            // Include every card and a disabled state without adding test hooks to the ViewModel.
            val display = state.copy(preferences = state.preferences.copy(contextHelpEnabled = false), game = state.game?.copy(ram = ram,
                scriptHand = ScriptType.entries.map { type -> state.game!!.scriptHand.find { it.type == type } ?: ScriptCard("help-${type.name}", type) }))
            CompositionLocalProvider(LocalContext provides context, LocalConfiguration provides config,
                LocalDensity provides Density(1f, fontScale = 1.3f)) {
                MainboardTheme {
                    Box(Modifier.requiredSize(640.dp, 360.dp)) { GameScreen(display, vm) {} }
                }
            }
        }
        compose.runOnIdle { vm.selectDomino(vm.uiState.value.game!!.dominoHand.first().id); vm.rotate() }
        compose.waitForIdle()
        val before = vm.uiState.value
        val header = compose.onNodeWithTag("game-header").fetchSemanticsNode().boundsInRoot
        val scripts = compose.onNodeWithTag("script-hand").fetchSemanticsNode().boundsInRoot
        assertTrue(scripts.top >= header.top && scripts.bottom <= header.bottom)
        val fixed = compose.onNodeWithTag("end-turn").fetchSemanticsNode().boundsInRoot
        compose.onNodeWithText(context.getString(R.string.seed, before.game!!.seed)).assertDoesNotExist()
        listOf(R.string.help_board_body, R.string.help_ram_body, R.string.help_hardware_body, R.string.help_spoof_body).forEachIndexed { index, body ->
            compose.onNodeWithTag("general-help").assertIsDisplayed().performClick()
            compose.onNodeWithTag("help-section-$index").performScrollTo().performClick()
            compose.onNodeWithText(context.getString(body), substring = true).assertExists()
            if (index % 2 == 0) compose.onNodeWithText(context.getString(R.string.help_close)).performClick()
            else InstrumentationRegistry.getInstrumentation().sendKeyDownUpSync(KeyEvent.KEYCODE_BACK)
            compose.runOnIdle { assertEquals(before, vm.uiState.value) }
            assertEquals(fixed, compose.onNodeWithTag("end-turn").fetchSemanticsNode().boundsInRoot)
        }
    }

    @Test fun renderedPipsFollowAllFourRotationsAndDoubles() {
        val tile = mutableStateOf(Domino("sprite", 0, 6))
        val orientation = mutableStateOf(Orientation.HORIZONTAL)
        compose.setContent {
            DominoImage(tile.value, Modifier.width(if (orientation.value == Orientation.HORIZONTAL) 120.dp else 60.dp)
                .testTag("sprite"), orientation.value)
        }
        listOf(0 to 6, 6 to 0, 6 to 6, 0 to 0).forEach { (first, second) ->
            Orientation.entries.forEach { direction ->
                compose.runOnIdle { tile.value = Domino("sprite", first, second); orientation.value = direction }
                val pixels = compose.onNodeWithTag("sprite").captureToImage().toPixelMap()
                fun darkPixels(half: Int): Int {
                    var count = 0
                    // Ignore the border and center divider; count black pips inside each port.
                    val horizontal = direction == Orientation.HORIZONTAL
                    val width = if (horizontal) pixels.width / 2 else pixels.width
                    val height = if (horizontal) pixels.height else pixels.height / 2
                    for (x in width / 8 until width * 7 / 8) for (y in height / 8 until height * 7 / 8) {
                        val color = pixels[x + (if (horizontal) half * width else 0), y + (if (horizontal) 0 else half * height)]
                        if (color.red < .3f && color.green < .3f && color.blue < .3f && color.alpha > .9f) count++
                    }
                    return count
                }
                listOf(first, second).forEachIndexed { half, value ->
                    if (value == 0) assertEquals("Blank port $half in $direction", 0, darkPixels(half))
                    else assertTrue("Pips in port $half in $direction", darkPixels(half) > 20)
                }
            }
        }
    }

    private class HelpPreferences : PlayerPreferencesRepository {
        override val preferences = MutableStateFlow(PlayerPreferences())
        override suspend fun setLanguage(value: String) {}
        override suspend fun setAudio(value: Boolean) {}
        override suspend fun setVibration(value: Boolean) {}
        override suspend fun setReducedMotion(value: Boolean) {}
        override suspend fun markTutorialComplete() {}
        override suspend fun setLastSeed(seed: Long) {}
        override suspend fun recordVictory(turns: Int, trace: Int) {}
        override suspend fun setDominoSkin(value: String) {}
        override suspend fun setBoardSkin(value: String) {}
        override suspend fun setContextHelpEnabled(value: Boolean) {}
        override suspend fun recordChallengeVictory(level: Int, turns: Int, trace: Int) {}
    }
}
