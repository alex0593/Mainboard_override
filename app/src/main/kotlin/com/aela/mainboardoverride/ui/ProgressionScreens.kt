package com.aela.mainboardoverride.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aela.mainboardoverride.GameUiState
import com.aela.mainboardoverride.MainViewModel
import com.aela.mainboardoverride.R
import com.aela.mainboardoverride.domain.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal data class SkinOption(val id: String, val label: Int)
internal val dominoSkins = listOf(
    SkinOption("kenney", R.string.skin_kenney), SkinOption("dark", R.string.skin_dark),
    SkinOption("gingerbread", R.string.skin_gingerbread), SkinOption("hearts", R.string.skin_hearts), SkinOption("stars", R.string.skin_stars),
)
internal val boardSkins = listOf(
    SkinOption("pcb", R.string.skin_pcb), SkinOption("blueprint", R.string.skin_blueprint),
    SkinOption("industrial", R.string.skin_industrial), SkinOption("rust", R.string.skin_rust),
    SkinOption("ice", R.string.skin_ice), SkinOption("copper", R.string.skin_copper), SkinOption("aurora", R.string.skin_aurora),
)

internal fun scenarioLabel(id: String) = when (id) {
    "lab" -> R.string.scenario_lab
    "data" -> R.string.scenario_data
    "industry" -> R.string.scenario_industry
    "archive" -> R.string.scenario_archive
    "core" -> R.string.scenario_core
    "ghost" -> R.string.scenario_ghost
    else -> R.string.scenario_classic
}

@Composable
internal fun ProgressionHeader(
    title: String,
    onBack: () -> Unit,
    trailing: (@Composable () -> Unit)? = null,
) {
    Row(
        Modifier.fillMaxWidth()
            .height(48.dp)
            .background(Panel.copy(alpha = .92f), RoundedCornerShape(8.dp))
            .border(1.dp, Cyan.copy(alpha = .28f), RoundedCornerShape(8.dp))
            .padding(horizontal = 10.dp),
        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
    ) {
        Text(title, Modifier.weight(1f), fontSize = 20.sp, color = Terminal, maxLines = 1)
        trailing?.invoke()
        TextButton(
            onClick = onBack,
            modifier = Modifier.height(36.dp),
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
        ) { Text(stringResource(R.string.back), fontSize = 12.sp) }
    }
}

@Composable
internal fun SkinGallery(state: GameUiState, actions: MainViewModel, onBack: () -> Unit) {
    var pcb by rememberSaveable { mutableStateOf(false) }
    var purchase by rememberSaveable { mutableStateOf<String?>(null) }
    val prefs = state.preferences
    CircuitBackground {
        Column(Modifier.fillMaxSize().padding(horizontal = 10.dp, vertical = 4.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            ProgressionHeader(stringResource(R.string.skins), onBack) {
                Text(stringResource(R.string.credit_balance, prefs.credits), color = Warning, fontSize = 12.sp)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                FilterChip(selected = !pcb, onClick = { pcb = false }, label = { Text(stringResource(R.string.domino_skin)) })
                FilterChip(selected = pcb, onClick = { pcb = true }, label = { Text(stringResource(R.string.board_skin)) })
            }
            LazyVerticalGrid(columns = GridCells.Adaptive(210.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(if (pcb) boardSkins else dominoSkins, key = { it.id }) { skin ->
                    val owned = !pcb || skin.id !in Rewards.premiumSkins || skin.id in prefs.ownedSkins
                    val equipped = skin.id == if (pcb) prefs.boardSkin else prefs.dominoSkin
                    Card {
                        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            if (pcb) Board(BoardState(placed = listOf(PlacedDomino(Domino("sample", 0, 3), Position(1, 3), Orientation.HORIZONTAL))), skin.id, prefs.dominoSkin, emptySet(), modifier = Modifier.fillMaxWidth().height(112.dp), onCell = {}, previewOnly = true)
                            else Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                DominoImage(Domino("preview1", 2, 5), Modifier.width(80.dp), skin = skin.id)
                                DominoImage(Domino("preview2", 0, 6), Modifier.width(80.dp), skin = skin.id)
                            }
                            Text(stringResource(skin.label), color = Cyan)
                            Button(
                                enabled = !equipped && (owned || prefs.credits >= Rewards.SKIN_PRICE),
                                onClick = {
                                    if (!owned) purchase = skin.id
                                    else if (pcb) actions.setBoardSkin(skin.id) else actions.setDominoSkin(skin.id)
                                }, modifier = Modifier.fillMaxWidth().testTag("skin-action-${skin.id}"),
                            ) {
                                Text(if (equipped) stringResource(R.string.equipped) else if (owned) stringResource(R.string.equip) else stringResource(R.string.buy_credits, Rewards.SKIN_PRICE))
                            }
                            if (!owned && prefs.credits < Rewards.SKIN_PRICE) Text(stringResource(R.string.missing_credits, Rewards.SKIN_PRICE - prefs.credits), color = Muted)
                        }
                    }
                }
            }
        }
    }
    purchase?.let { id ->
        AlertDialog(onDismissRequest = { purchase = null },
            title = { Text(stringResource(boardSkins.first { it.id == id }.label)) },
            text = { Text(stringResource(R.string.confirm_skin_purchase, Rewards.SKIN_PRICE, prefs.credits - Rewards.SKIN_PRICE)) },
            confirmButton = { Button(modifier = Modifier.testTag("confirm-purchase"), enabled = prefs.credits >= Rewards.SKIN_PRICE && id !in prefs.ownedSkins, onClick = { actions.buySkin(id); purchase = null }) { Text(stringResource(R.string.buy_credits, Rewards.SKIN_PRICE)) } },
            dismissButton = { TextButton(onClick = { purchase = null }) { Text(stringResource(R.string.cancel)) } },
        )
    }
}

@Composable
internal fun ScenarioScreen(state: GameUiState, actions: MainViewModel, onStart: () -> Unit, onBack: () -> Unit) {
    val completed = state.preferences.challengeBest.size
    val previews by produceState<Map<String, BoardState>>(emptyMap()) {
        ScenarioCatalog.all.forEach { scenario ->
            val board = withContext(Dispatchers.Default) { LevelGenerator.generateScenario(42L, scenario.id).board }
            value = value + (scenario.id to board)
        }
    }
    CircuitBackground {
        Column(Modifier.fillMaxSize().padding(horizontal = 10.dp, vertical = 4.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            ProgressionHeader(stringResource(R.string.free_scenarios), onBack)
            Text(stringResource(R.string.expected_reward, Rewards.VICTORY), color = Warning)
            LazyVerticalGrid(columns = GridCells.Adaptive(220.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(ScenarioCatalog.all, key = { it.id }) { scenario ->
                    val unlocked = completed >= scenario.required
                    val preview = previews[scenario.id]
                    Card {
                        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            if (preview != null) Board(preview, state.preferences.boardSkin, state.preferences.dominoSkin, emptySet(), modifier = Modifier.fillMaxWidth().height(110.dp), onCell = {}, previewOnly = true)
                            else Box(Modifier.fillMaxWidth().height(110.dp), contentAlignment = androidx.compose.ui.Alignment.Center) { CircularProgressIndicator(Modifier.size(24.dp)) }
                            Text(stringResource(scenarioLabel(scenario.id)), color = Cyan)
                            Text(stringResource(R.string.scenario_difficulty, ScenarioCatalog.all.indexOf(scenario) + 1, preview?.width ?: scenario.width, preview?.height ?: scenario.height))
                            if (!unlocked) Text(stringResource(R.string.scenario_locked, completed, scenario.required), color = Warning)
                            Button(enabled = unlocked, onClick = { actions.startScenario(scenario.id); onStart() }, modifier = Modifier.fillMaxWidth()) {
                                Text(stringResource(if (unlocked) R.string.play_scenario else R.string.locked_scenario))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun MatchResultDialog(state: GameUiState, actions: MainViewModel, onMenu: () -> Unit) {
    val game = state.game ?: return
    val result = game.result ?: return
    val victory = result == GameResult.VICTORY
    val next = state.challengeLevel?.plus(1)?.takeIf { victory && it <= ChallengeCatalog.COUNT && it <= state.preferences.challengeUnlocked }
    AlertDialog(onDismissRequest = actions::reviewBoard,
        title = { Text(stringResource(if (victory) R.string.victory else R.string.defeat), color = if (victory) Terminal else Danger) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(if (state.tutorialStep == TutorialStep.COMPLETE) R.string.lesson_complete else resultText(result)))
                Text("${stringResource(R.string.turn, game.turn)} · ${stringResource(R.string.trace, game.trace)}")
                if (!state.tutorial) {
                    if (victory && state.reward == null) Text(stringResource(R.string.saving_reward))
                    else Text(stringResource(R.string.reward_breakdown, state.reward?.base ?: 0, state.reward?.bonus ?: 0))
                    Text(stringResource(R.string.credit_balance, state.preferences.credits), color = Warning)
                    state.reward?.unlockedScenario?.let { Text(stringResource(R.string.scenario_unlocked, stringResource(scenarioLabel(it))), color = Cyan) }
                }
                TextButton(onClick = actions::retry) { Text(stringResource(R.string.play_again)) }
                if (next != null) TextButton(onClick = { actions.startChallenge(next) }) { Text(stringResource(R.string.next_challenge)) }
            }
        },
        confirmButton = { Button(onClick = actions::reviewBoard) { Text(stringResource(R.string.review_board)) } },
        dismissButton = { TextButton(onClick = onMenu) { Text(stringResource(R.string.main_menu)) } },
    )
}
