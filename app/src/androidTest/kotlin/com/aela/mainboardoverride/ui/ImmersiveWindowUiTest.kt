package com.aela.mainboardoverride.ui

import android.os.Build
import android.view.inspector.WindowInspector
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.Lifecycle
import androidx.test.platform.app.InstrumentationRegistry
import com.aela.mainboardoverride.MainActivity
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test

class ImmersiveWindowUiTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()

    private fun awaitHiddenBars(focusedWindow: Boolean = false) {
        compose.waitUntil(5000) {
            var hidden = false
            InstrumentationRegistry.getInstrumentation().runOnMainSync {
                val view = if (focusedWindow && Build.VERSION.SDK_INT >= 29) {
                    WindowInspector.getGlobalWindowViews().firstOrNull { it.hasWindowFocus() }
                } else compose.activity.window.decorView
                val insets = view?.let { ViewCompat.getRootWindowInsets(it) }
                hidden = insets != null &&
                    !insets.isVisible(WindowInsetsCompat.Type.navigationBars()) &&
                    !insets.isVisible(WindowInsetsCompat.Type.statusBars())
            }
            hidden
        }
    }

    @Test fun launchAndResumeHideSystemBars() {
        awaitHiddenBars()
        compose.activityRule.scenario.moveToState(Lifecycle.State.CREATED)
        compose.activityRule.scenario.moveToState(Lifecycle.State.RESUMED)
        awaitHiddenBars()
        compose.activityRule.scenario.recreate()
        awaitHiddenBars()
    }

    @Test fun dialogAndReturnToMenuKeepSystemBarsHidden() {
        assumeTrue(Build.VERSION.SDK_INT >= 29)
        awaitHiddenBars()
        compose.runOnIdle { compose.activity.onBackPressedDispatcher.onBackPressed() }
        compose.onNodeWithTag("cancel-dialog").assertIsDisplayed()
        awaitHiddenBars(focusedWindow = true)
        compose.onNodeWithTag("cancel-dialog").performClick()
        awaitHiddenBars()
    }
}
