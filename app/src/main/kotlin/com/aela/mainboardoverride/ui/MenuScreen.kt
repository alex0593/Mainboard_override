package com.aela.mainboardoverride.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.painterResource
import androidx.compose.foundation.Image
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aela.mainboardoverride.GameUiState
import com.aela.mainboardoverride.R

@Composable
internal fun MenuScreen(
    state: GameUiState,
    onNew: () -> Unit,
    onChallenge: () -> Unit,
    onRetry: () -> Unit,
    onSettings: () -> Unit,
    onSkins: () -> Unit,
    onHelp: () -> Unit,
    onExitApp: () -> Unit,
) {
    var exitRequested by rememberSaveable { mutableStateOf(false) }
    BackHandler(enabled = !exitRequested) { exitRequested = true }
    MenuArtworkBackground(
        reducedMotion = state.preferences.reducedMotion,
        backgroundResource = menuBackgroundResource(state.preferences.lastScenario),
    ) {
        Row(
            Modifier.fillMaxSize().padding(horizontal = 24.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
                Image(
                    painterResource(R.drawable.menu_module_v1), contentDescription = null,
                    modifier = Modifier.size(88.dp).padding(bottom = 8.dp),
                )
                Text("> ROOT://MAINBOARD", color = Cyan, fontFamily = FontFamily.Monospace)
                Text(
                    "OVERRIDE",
                    color = Terminal,
                    fontSize = 38.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Monospace,
                )
                Text(stringResource(R.string.tagline), color = Muted, letterSpacing = 3.sp)
                Text(stringResource(R.string.credit_balance, state.preferences.credits), color = Warning)
                Spacer(Modifier.height(22.dp))
                if (state.preferences.bestTurns != null) {
                    Text(
                        stringResource(
                            R.string.record,
                            state.preferences.bestTurns,
                            state.preferences.bestTrace ?: 0,
                        ),
                        color = Warning,
                        fontFamily = FontFamily.Monospace,
                    )
                }
            }
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                MenuArtworkButton(stringResource(R.string.new_network), onNew, primary = true)
                MenuArtworkButton(stringResource(R.string.challenge), onChallenge)
                if (state.preferences.lastSeed != null) MenuArtworkButton(stringResource(R.string.retry_seed), onRetry)
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    MenuArtworkButton(stringResource(R.string.settings), onSettings, Modifier.weight(1f), compact = true)
                    MenuArtworkButton(stringResource(R.string.skins), onSkins, Modifier.weight(1f), compact = true)
                    MenuArtworkButton(stringResource(R.string.tutorial), onHelp, Modifier.weight(1.35f), compact = true, icon = R.drawable.ic_tutorial)
                }
            }
        }
    }
    if (exitRequested) {
        ConfirmGameDialog(
            title = stringResource(R.string.confirm_app_exit_title),
            message = stringResource(R.string.confirm_app_exit_message),
            confirmLabel = stringResource(R.string.confirm_app_exit),
            onDismiss = { exitRequested = false },
            onConfirm = { exitRequested = false; onExitApp() },
        )
    }
}
