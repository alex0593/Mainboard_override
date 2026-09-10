package com.aela.mainboardoverride

import android.os.Bundle
import androidx.activity.ComponentActivity
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

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
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
