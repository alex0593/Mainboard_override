package com.aela.mainboardoverride.ui

import android.app.Application
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.ScrollAxisRange
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ApplicationProvider
import com.aela.mainboardoverride.MainViewModel
import com.aela.mainboardoverride.R
import com.aela.mainboardoverride.data.PlayerPreferences
import com.aela.mainboardoverride.data.PlayerPreferencesRepository
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.util.Locale

/** Verifies the Settings screen remains scrollable and all toggles are fully legible. */
class SettingsScreenUiTest {
    @get:Rule val compose = createComposeRule()
    private val app: Application = ApplicationProvider.getApplicationContext()

    @Test fun settingsScreenSupportsScrollAndShowsAllToggles() {
        val contextEs = app.createConfigurationContext(
            android.content.res.Configuration(app.resources.configuration).apply {
                setLocale(Locale.forLanguageTag("es"))
            },
        )

        lateinit var vm: MainViewModel
        compose.runOnIdle { vm = MainViewModel(app, InMemoryPreferences()) }

        compose.setContent {
            val state by vm.uiState.collectAsState()
            CompositionLocalProvider(
                LocalContext provides contextEs,
                LocalConfiguration provides contextEs.resources.configuration,
                LocalDensity provides Density(1f, fontScale = 1.0f),
            ) {
                MainboardTheme {
                    Box(Modifier.requiredSize(804.dp, 360.dp)) {
                        SettingsScreen(state, vm) {}
                    }
                }
            }
        }

        compose.waitForIdle()

        // The settings root container must be scrollable (vertical)
        val scrollMatcher = hasScrollAction()
        val settingsRoot = compose.onRoot()
        assertTrue("Settings container should support vertical scroll", settingsRoot.fetchSemanticsNode().config.contains(SemanticsProperties.VerticalScrollAxisRange))

        // Scroll to the AboutStrip / version footer to ensure it's reachable and not clipped
        val versionText = contextEs.getString(R.string.app_version, "0.1.2")
        compose.onNode(hasScrollAction()).performScrollToNode(hasText(versionText))
        compose.onNodeWithText(versionText).assertIsDisplayed()

        // Ensure all four switch labels are fully displayed
        val audio = compose.onNodeWithText(contextEs.getString(R.string.audio)).assertIsDisplayed()
        val vibration = compose.onNodeWithText(contextEs.getString(R.string.vibration)).assertIsDisplayed()
        val reducedMotion = compose.onNodeWithText(contextEs.getString(R.string.reduced_motion)).assertIsDisplayed()
        val contextHelp = compose.onNodeWithText(contextEs.getString(R.string.context_help)).assertIsDisplayed()

        val boundsAudio = audio.getUnclippedBoundsInRoot()
        val boundsVib = vibration.getUnclippedBoundsInRoot()
        val boundsReduced = reducedMotion.getUnclippedBoundsInRoot()
        val boundsHelp = contextHelp.getUnclippedBoundsInRoot()

        // Each label should have substantial width (not severely truncated on ~350dp panel)
        assertTrue("Audio label should not be truncated", (boundsAudio.right - boundsAudio.left) > 120.dp)
        assertTrue("Vibration label should not be truncated", (boundsVib.right - boundsVib.left) > 120.dp)
        assertTrue("Reduced motion label should not be truncated", (boundsReduced.right - boundsReduced.left) > 120.dp)
        assertTrue("Context help label should not be truncated", (boundsHelp.right - boundsHelp.left) > 120.dp)
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
