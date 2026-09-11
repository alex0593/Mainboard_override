package com.aela.mainboardoverride.ui

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.ui.draw.clip
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
    SkinOption("circuit", R.string.skin_circuit), SkinOption("blueprint", R.string.skin_blueprint),
    SkinOption("copper", R.string.skin_copper), SkinOption("ice", R.string.skin_ice), SkinOption("aurora", R.string.skin_aurora),
    SkinOption("obsidian", R.string.skin_obsidian), SkinOption("ceramic", R.string.skin_ceramic),
)
internal val boardSkins = listOf(
    SkinOption("pcb", R.string.skin_pcb), SkinOption("blueprint", R.string.skin_blueprint),
    SkinOption("industrial", R.string.skin_industrial), SkinOption("rust", R.string.skin_rust),
    SkinOption("ice", R.string.skin_ice), SkinOption("graphite", R.string.skin_graphite), SkinOption("signal", R.string.skin_signal),
    SkinOption("copper", R.string.skin_copper), SkinOption("aurora", R.string.skin_aurora),
    SkinOption("obsidian", R.string.skin_obsidian), SkinOption("ceramic", R.string.skin_ceramic),
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

/** Shared fixed height keeps scenario and challenge cards visually aligned. */
internal val ProgressionCardHeight = 320.dp

@Composable
internal fun ProgressionHeader(
    title: String,
    onBack: () -> Unit,
    counter: String? = null,
    trailing: (@Composable () -> Unit)? = null,
) {
    Box(Modifier.fillMaxWidth().heightIn(min = 64.dp).clip(RoundedCornerShape(10.dp))) {
        androidx.compose.foundation.Image(
            painter = androidx.compose.ui.res.painterResource(R.drawable.menu_header_v1),
            contentDescription = null,
            modifier = Modifier.matchParentSize(),
            contentScale = androidx.compose.ui.layout.ContentScale.FillBounds,
        )
        Row(
            Modifier.fillMaxWidth().heightIn(min = 64.dp).padding(horizontal = 24.dp, vertical = 8.dp),
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(title, Modifier.weight(1f), fontSize = 20.sp, color = Terminal, maxLines = 1)
            counter?.let { Text(it, color = Warning, fontSize = 12.sp, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace) }
            trailing?.invoke()
            MenuArtworkButton(
                label = stringResource(R.string.back),
                onClick = onBack,
                compact = true,
                fillWidth = false,
                modifier = Modifier.widthIn(min = 92.dp).heightIn(min = 48.dp),
            )
        }
    }
}

@Composable
internal fun SkinGallery(state: GameUiState, actions: MainViewModel, onBack: () -> Unit) {
    var pcb by rememberSaveable { mutableStateOf(false) }
    var purchase by rememberSaveable { mutableStateOf<String?>(null) }
    var locallyPurchased by remember { mutableStateOf(emptySet<String>()) }
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
                    val paid = pcb && skin.id in Rewards.purchasableSkins
                    val price = Rewards.skinPrice(skin.id)
                    val owned = !paid || skin.id in prefs.ownedSkins || skin.id in locallyPurchased
                    val equipped = skin.id == if (pcb) prefs.boardSkin else prefs.dominoSkin
                    Box(
                        Modifier.fillMaxWidth().height(ProgressionCardHeight).clip(RoundedCornerShape(14.dp))
                            .border(1.dp, Cyan.copy(alpha = if (equipped) .72f else .28f), RoundedCornerShape(14.dp)),
                    ) {
                        androidx.compose.foundation.Image(
                            painter = androidx.compose.ui.res.painterResource(R.drawable.menu_skin_card_v1),
                            contentDescription = null,
                            modifier = Modifier.matchParentSize(),
                            contentScale = androidx.compose.ui.layout.ContentScale.FillBounds,
                        )
                        Column(Modifier.fillMaxSize().padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            if (pcb) Board(BoardState(placed = listOf(PlacedDomino(Domino("sample", 0, 3), Position(1, 3), Orientation.HORIZONTAL))), skin.id, prefs.dominoSkin, emptySet(), modifier = Modifier.fillMaxWidth().height(112.dp), onCell = {}, previewOnly = true)
                            else Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                DominoImage(Domino("preview1", 2, 5), Modifier.width(80.dp), skin = skin.id, previewOnly = true)
                                DominoImage(Domino("preview2", 0, 6), Modifier.width(80.dp), skin = skin.id, previewOnly = true)
                            }
                            Text(stringResource(skin.label), color = Cyan)
                            Spacer(Modifier.weight(1f))
                            MenuArtworkButton(
                                label = if (equipped) stringResource(R.string.equipped) else if (owned) stringResource(R.string.equip) else stringResource(R.string.buy_credits, price),
                                primary = equipped || owned,
                                enabled = !equipped && (owned || prefs.credits >= price),
                                onClick = {
                                    if (!owned) purchase = skin.id
                                    else if (pcb) actions.setBoardSkin(skin.id) else actions.setDominoSkin(skin.id)
                                }, modifier = Modifier.fillMaxWidth().testTag("skin-action-${skin.id}"),
                                compact = true,
                            )
                            if (paid) Text(stringResource(R.string.skin_price, price), color = Warning, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace, fontSize = 10.sp)
                            if (!owned && prefs.credits < price) Text(stringResource(R.string.missing_credits, price - prefs.credits), color = Muted)
                        }
                    }
                }
            }
        }
    }
    purchase?.let { id ->
        GameDialog(
            title = stringResource(boardSkins.first { it.id == id }.label),
            onDismiss = { purchase = null },
            actions = {
                TextButton(onClick = { purchase = null }, modifier = Modifier.heightIn(min = 48.dp)) { Text(stringResource(R.string.cancel)) }
                Button(
                    modifier = Modifier.testTag("confirm-purchase").heightIn(min = 48.dp),
                    enabled = prefs.credits >= Rewards.skinPrice(id) && id !in prefs.ownedSkins,
                    onClick = { locallyPurchased = locallyPurchased + id; actions.buySkin(id); purchase = null },
                ) { Text(stringResource(R.string.buy_credits, Rewards.skinPrice(id))) }
            },
        ) {
            Text(stringResource(R.string.confirm_skin_purchase, Rewards.skinPrice(id), prefs.credits - Rewards.skinPrice(id)))
        }
    }
}

@Composable
internal fun ScenarioScreen(state: GameUiState, actions: MainViewModel, onStart: () -> Unit, onBack: () -> Unit) {
    val transition = LocalWindowTransition.current
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
                    Card(
                        modifier = Modifier.fillMaxWidth().height(ProgressionCardHeight),
                        colors = CardDefaults.cardColors(containerColor = Panel.copy(alpha = if (unlocked) .94f else .72f)),
                        border = BorderStroke(1.dp, if (unlocked) Cyan.copy(alpha = .35f) else Muted.copy(alpha = .35f)),
                        shape = RoundedCornerShape(12.dp),
                    ) {
                        Column(Modifier.fillMaxSize().padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            if (preview != null) Board(preview, state.preferences.boardSkin, state.preferences.dominoSkin, emptySet(), modifier = Modifier.fillMaxWidth().height(110.dp), onCell = {}, previewOnly = true)
                            else Box(Modifier.fillMaxWidth().height(110.dp), contentAlignment = androidx.compose.ui.Alignment.Center) { CircularProgressIndicator(Modifier.size(24.dp)) }
                            Text(stringResource(scenarioLabel(scenario.id)), color = Cyan)
                            Text(stringResource(R.string.scenario_difficulty, ScenarioCatalog.all.indexOf(scenario) + 1, preview?.width ?: scenario.width, preview?.height ?: scenario.height))
                            if (!unlocked) Text(stringResource(R.string.scenario_locked, completed, scenario.required), color = Warning)
                            Spacer(Modifier.weight(1f))
                            Button(enabled = unlocked, onClick = { transition { actions.startScenario(scenario.id); onStart() } }, modifier = Modifier.fillMaxWidth()) {
                                Text(stringResource(if (unlocked) R.string.play_scenario else R.string.locked_scenario))
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun MatchResultDialog(state: GameUiState, actions: MainViewModel, onMenu: () -> Unit) {
    val game = state.game ?: return
    val result = game.result ?: return
    val victory = result == GameResult.VICTORY
    val next = state.challengeLevel?.plus(1)?.takeIf { victory && it <= ChallengeCatalog.COUNT && it <= state.preferences.challengeUnlocked }
    val transition = LocalWindowTransition.current
    GameDialog(
        title = stringResource(if (victory) R.string.victory else R.string.defeat),
        onDismiss = actions::reviewBoard,
        modifier = Modifier.testTag("match-result-dialog"),
        accent = if (victory) Terminal else Danger,
        actions = {
            // Secondary navigation gets its own row, apart from continuing play.
            FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = onMenu, modifier = Modifier.heightIn(min = 48.dp)) { Text(stringResource(R.string.main_menu)) }
                TextButton(onClick = actions::reviewBoard, modifier = Modifier.heightIn(min = 48.dp)) { Text(stringResource(R.string.review_board)) }
            }
            OutlinedButton(onClick = { transition(actions::retry) }, modifier = Modifier.heightIn(min = 48.dp)) { Text(stringResource(R.string.play_again)) }
            if (next != null) Button(onClick = { transition { actions.startChallenge(next) } }, modifier = Modifier.heightIn(min = 48.dp)) {
                Text(stringResource(R.string.next_challenge))
            }
        },
    ) {
        Text(stringResource(resultText(result)), style = MaterialTheme.typography.bodyLarge)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(stringResource(R.string.turn, game.turn), color = Cyan)
            Text(stringResource(R.string.trace, game.trace), color = Cyan)
        }
        Surface(color = Void.copy(alpha = .6f), shape = RoundedCornerShape(12.dp)) {
            Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(R.string.credit_balance, state.preferences.credits), color = Warning, style = MaterialTheme.typography.titleMedium)
                if (victory && state.reward == null) Text(stringResource(R.string.saving_reward))
                else if (victory) Text(stringResource(R.string.reward_breakdown, state.reward?.base ?: 0, state.reward?.bonus ?: 0))
            }
        }
        state.reward?.unlockedScenario?.let {
            Text(stringResource(R.string.scenario_unlocked, stringResource(scenarioLabel(it))), color = Cyan)
        }
    }
}
