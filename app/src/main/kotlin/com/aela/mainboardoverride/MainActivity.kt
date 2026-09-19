package com.aela.mainboardoverride

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.rememberNavController
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import com.aela.mainboardoverride.ui.MainboardApp
import com.aela.mainboardoverride.ui.MainboardTheme
import com.aela.mainboardoverride.ui.WindowTransitionHost
import com.aela.mainboardoverride.ui.bindGameImmersion
import com.aela.mainboardoverride.ui.hideGameSystemBars

// Per-app locales only take effect through AppCompatDelegate, which requires
// an AppCompatActivity; with ComponentActivity the language switch was a silent no-op.
class MainActivity : AppCompatActivity() {
    private var unbindImmersion: (() -> Unit)? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.Theme_MainboardOverride)
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        unbindImmersion = window.bindGameImmersion()
        setContent {
            val viewModel: MainViewModel = viewModel()
            val state by viewModel.uiState.collectAsStateWithLifecycle()
            LaunchedEffect(state.preferences.language) {
                val locales = LocaleListCompat.forLanguageTags(state.preferences.language)
                if (AppCompatDelegate.getApplicationLocales() != locales) {
                    AppCompatDelegate.setApplicationLocales(locales)
                }
            }
            MainboardTheme {
                WindowTransitionHost(reducedMotion = state.preferences.reducedMotion) {
                    MainboardApp(
                        nav = rememberNavController(),
                        state = state,
                        actions = viewModel,
                        onExitApp = { finishAndRemoveTask() },
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (window.decorView.hasWindowFocus()) window.hideGameSystemBars()
    }

    override fun onDestroy() {
        unbindImmersion?.invoke()
        super.onDestroy()
    }
}
