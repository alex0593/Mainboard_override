package com.aela.mainboardoverride.ui

import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import com.aela.mainboardoverride.domain.RejectReason
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.aela.mainboardoverride.MainViewModel
import com.aela.mainboardoverride.GameUiState
import com.aela.mainboardoverride.R
import com.aela.mainboardoverride.audio.AmbientSoundtrackHost
import com.aela.mainboardoverride.audio.SoundEffectsHost
import com.aela.mainboardoverride.audio.SoundtrackScene
import com.aela.mainboardoverride.domain.GameResult

private const val MENU = "menu"
private const val GAME = "game"
private const val SETTINGS = "settings"
private const val HELP = "help"
private const val SKINS = "skins"
private const val CHALLENGE = "challenge"

internal enum class GameExitAction { EXIT, RESTART }

@Composable
fun MainboardApp(
    nav: NavHostController,
    state: GameUiState,
    actions: MainViewModel,
    onExitApp: () -> Unit = {},
) {
    val soundtrackScene = when {
        state.game?.result == GameResult.VICTORY -> SoundtrackScene.VICTORY
        state.game?.result != null -> SoundtrackScene.DEFEAT
        state.game != null -> SoundtrackScene.GAME
        else -> SoundtrackScene.MENU
    }
    SoundEffectsHost(
        enabled = state.preferences.audioEnabled,
        volume = state.preferences.sfxVolume,
        cues = actions.soundCues,
    )
    AmbientSoundtrackHost(
        enabled = state.preferences.audioEnabled,
        scene = soundtrackScene,
        trace = state.game?.trace ?: 0,
        volume = state.preferences.musicVolume,
    )
    val transition = LocalWindowTransition.current
    NavHost(navController = nav, startDestination = MENU,
        modifier = Modifier.windowInsetsPadding(WindowInsets.safeDrawing)) {
        composable(MENU) {
            MenuScreen(
                state = state,
                onNew = { actions.playUiTick(); transition { nav.navigate("scenarios") } },
                onChallenge = { actions.playUiTick(); transition { nav.navigate(CHALLENGE) } },
                onRetry = { transition { actions.retryLast(); nav.navigate(GAME) } },
                onSettings = { actions.playUiTick(); transition { nav.navigate(SETTINGS) } },
                onSkins = { actions.playUiTick(); transition { nav.navigate(SKINS) } },
                onHelp = { actions.playUiTick(); transition { nav.navigate(HELP) } },
                onExitApp = onExitApp,
            )
        }
        composable(GAME) {
            GameScreen(
                state = state,
                actions = actions,
                onMenu = { actions.playUiTick(); transition { nav.popBackStack(MENU, inclusive = false) } },
            )
        }
        composable(SETTINGS) { SettingsScreen(state, actions) { actions.playUiTick(); transition { nav.popBackStack() } } }
        composable(SKINS) { SkinGallery(state, actions) { actions.playUiTick(); transition { nav.popBackStack() } } }
        composable("scenarios") { ScenarioScreen(state, actions, { nav.navigate(GAME) }, { actions.playUiTick(); transition { nav.popBackStack() } }) }
        composable(CHALLENGE) {
            ChallengeScreen(
                state = state,
                actions = actions,
                onStart = { nav.navigate(GAME) },
                onBack = { actions.playUiTick(); transition { nav.popBackStack() } },
            )
        }
        composable(HELP) { TutorialScreen(state.preferences, actions.tutorial, onBack = { actions.playUiTick(); transition { nav.popBackStack() } }) }
    }
}

internal fun rejectionText(reason: RejectReason): Int = when (reason) {
    RejectReason.GAME_FINISHED -> R.string.error_game_finished
    RejectReason.WRONG_PHASE -> R.string.error_wrong_phase
    RejectReason.DOMINO_ALREADY_PLACED -> R.string.error_domino_already_placed
    RejectReason.DOMINO_NOT_FOUND -> R.string.error_domino_not_found
    RejectReason.OUT_OF_BOUNDS -> R.string.error_out_of_bounds
    RejectReason.CELL_OCCUPIED -> R.string.error_cell_occupied
    RejectReason.CONTACT_MISMATCH -> R.string.error_contact_mismatch
    RejectReason.NOT_CONNECTED -> R.string.error_not_connected
    RejectReason.CARD_NOT_FOUND -> R.string.error_card_not_found
    RejectReason.INSUFFICIENT_RAM -> R.string.error_insufficient_ram
    RejectReason.INVALID_TARGET -> R.string.error_invalid_target
    RejectReason.MUST_PLACE_DOMINO -> R.string.error_must_place_domino
}

internal fun resultText(result: GameResult): Int = when (result) {
    GameResult.VICTORY -> R.string.result_victory
    GameResult.TRACE_INTERCEPTED -> R.string.result_trace_intercepted
    GameResult.DAEMON_BREACH -> R.string.result_daemon_breach
    GameResult.KERNEL_PANIC -> R.string.result_kernel_panic
    GameResult.MEMORY_EXHAUSTED -> R.string.result_memory_exhausted
    GameResult.CHALLENGE_LIMIT -> R.string.result_challenge_limit
    GameResult.LOCK_TRIPPED -> R.string.result_lock_tripped
}
