package com.aela.mainboardoverride.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aela.mainboardoverride.GameUiState
import com.aela.mainboardoverride.R
import com.aela.mainboardoverride.domain.Rewards
import com.aela.mainboardoverride.domain.todayString

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
        AppWindow {
            Row(
                Modifier.fillMaxSize().padding(bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(24.dp),
            ) {
                IdentityPane(state, Modifier.weight(1f))
                ActionsPane(state, onNew, onChallenge, onRetry, onSettings, onSkins, onHelp, Modifier.weight(1f))
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

/** Left panel: brand identity plus the live status lines (credit, daily goal, record) — no scroll. */
@Composable
private fun IdentityPane(state: GameUiState, modifier: Modifier = Modifier) {
    val prefs = state.preferences
    Column(
        modifier.fillMaxHeight().fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Image(
            painterResource(R.drawable.menu_module_v1), contentDescription = null,
            modifier = Modifier.size(76.dp),
        )
        Text("> ROOT://MAINBOARD", color = Cyan, fontFamily = FontFamily.Monospace, fontSize = 13.sp)
        Text(
            "OVERRIDE",
            color = Terminal,
            fontSize = 36.sp,
            fontWeight = FontWeight.Black,
            fontFamily = FontFamily.Monospace,
        )
        Text(stringResource(R.string.tagline), color = Muted, letterSpacing = 3.sp)
        Spacer(Modifier.height(10.dp))
        Text(stringResource(R.string.credit_balance, animatedCount(prefs.credits)), color = Warning, fontFamily = FontFamily.Monospace)
        Text(
            if (todayString() == prefs.dailyGoalDate) stringResource(R.string.daily_done)
            else stringResource(R.string.daily_pending, Rewards.DAILY_GOAL),
            color = Muted,
            fontFamily = FontFamily.Monospace,
            fontSize = 13.sp,
            maxLines = 1,
        )
        if (prefs.bestTurns != null) {
            Text(
                stringResource(R.string.record, prefs.bestTurns, prefs.bestTrace ?: 0),
                color = Warning,
                fontFamily = FontFamily.Monospace,
                fontSize = 13.sp,
                maxLines = 1,
            )
        } else {
            Text(stringResource(R.string.record_empty), color = Muted, fontFamily = FontFamily.Monospace, fontSize = 13.sp)
        }
    }
}

/** Right panel: primary actions and the compact toolbar row. */
@Composable
private fun ActionsPane(
    state: GameUiState,
    onNew: () -> Unit,
    onChallenge: () -> Unit,
    onRetry: () -> Unit,
    onSettings: () -> Unit,
    onSkins: () -> Unit,
    onHelp: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier.fillMaxHeight().fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        MenuArtworkButton(stringResource(R.string.new_network), onNew, primary = true)
        MenuArtworkButton(stringResource(R.string.challenge), onChallenge)
        if (state.preferences.lastSeed != null) {
            MenuArtworkButton(stringResource(R.string.retry_seed), onRetry)
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            MenuArtworkButton(stringResource(R.string.settings), onSettings, Modifier.weight(1f), compact = true)
            MenuArtworkButton(stringResource(R.string.store), onSkins, Modifier.weight(1f), compact = true)
            MenuArtworkButton(stringResource(R.string.tutorial), onHelp, Modifier.weight(1.35f), compact = true, icon = R.drawable.ic_tutorial)
        }
    }
}