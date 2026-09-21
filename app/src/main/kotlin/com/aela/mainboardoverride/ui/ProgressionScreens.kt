package com.aela.mainboardoverride.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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

@Composable
internal fun ProgressionHeader(
    title: String,
    onBack: () -> Unit,
    counter: String? = null,
    trailing: (@Composable () -> Unit)? = null,
) {
    Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp))) {
        androidx.compose.foundation.Image(
            painter = androidx.compose.ui.res.painterResource(R.drawable.menu_header_v1),
            contentDescription = null,
            modifier = Modifier.matchParentSize(),
            contentScale = androidx.compose.ui.layout.ContentScale.FillBounds,
        )
        Row(
            Modifier.fillMaxWidth().heightIn(min = 72.dp).padding(horizontal = 24.dp, vertical = 12.dp),
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            MetalTitle(title, Modifier.weight(1f), fontSize = 20.sp)
            counter?.let { Text(it, color = Warning, fontSize = 12.sp, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace) }
            trailing?.invoke()
            MenuArtworkButton(
                stringResource(R.string.back), onBack, compact = true, fillWidth = false,
                modifier = Modifier.widthIn(min = 92.dp).heightIn(min = 48.dp),
            )
        }
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
                val dominoChip = remember { MutableInteractionSource() }
                val boardChip = remember { MutableInteractionSource() }
                FilterChip(selected = !pcb, onClick = { pcb = false }, interactionSource = dominoChip,
                    modifier = Modifier.pressFeedback(dominoChip, label = "skin filter", pressedScale = .96f),
                    label = { Text(stringResource(R.string.domino_skin)) })
                FilterChip(selected = pcb, onClick = { pcb = true }, interactionSource = boardChip,
                    modifier = Modifier.pressFeedback(boardChip, label = "skin filter", pressedScale = .96f),
                    label = { Text(stringResource(R.string.board_skin)) })
            }
            LazyVerticalGrid(columns = GridCells.Adaptive(220.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(if (pcb) boardSkins else dominoSkins, key = { it.id }) { skin ->
                    val paid = pcb && skin.id in Rewards.purchasableSkins
                    val price = Rewards.skinPrice(skin.id)
                    val owned = !paid || skin.id in prefs.ownedSkins
                    val equipped = skin.id == if (pcb) prefs.boardSkin else prefs.dominoSkin
                    Box(
                        Modifier.fillMaxWidth().heightIn(min = 240.dp).clip(RoundedCornerShape(14.dp))
                            .border(1.dp, Cyan.copy(alpha = if (equipped) .72f else .28f), RoundedCornerShape(14.dp)),
                    ) {
                        androidx.compose.foundation.Image(
                            painter = androidx.compose.ui.res.painterResource(R.drawable.menu_skin_card_v1),
                            contentDescription = null,
                            modifier = Modifier.matchParentSize(),
                            contentScale = androidx.compose.ui.layout.ContentScale.FillBounds,
                        )
                        Column(Modifier.fillMaxWidth().heightIn(min = 240.dp).padding(18.dp), horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp, androidx.compose.ui.Alignment.CenterVertically)) {
                            if (pcb) Board(BoardState(placed = listOf(PlacedDomino(Domino("sample", 0, 3), Position(1, 3), Orientation.HORIZONTAL))), skin.id, prefs.dominoSkin, emptySet(), modifier = Modifier.fillMaxWidth().height(88.dp), onCell = {}, previewOnly = true)
                            else Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                DominoImage(Domino("preview1", 2, 5), Modifier.width(64.dp), skin = skin.id, previewOnly = true)
                                DominoImage(Domino("preview2", 0, 6), Modifier.width(64.dp), skin = skin.id, previewOnly = true)
                            }
                            Text(stringResource(skin.label), Modifier.fillMaxWidth(), color = Cyan, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                            if (paid) Text(stringResource(R.string.skin_price, price), color = Warning, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace, fontSize = 10.sp)
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
                            if (!owned && prefs.credits < price) Text(stringResource(R.string.missing_credits, price - prefs.credits), Modifier.fillMaxWidth(), color = Muted, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
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
                MenuArtworkButton(stringResource(R.string.cancel), { purchase = null }, compact = true, fillWidth = false)
                MenuArtworkButton(
                    label = stringResource(R.string.buy_credits, Rewards.skinPrice(id)),
                    modifier = Modifier.testTag("confirm-purchase"), compact = true, primary = true, fillWidth = false,
                    enabled = prefs.credits >= Rewards.skinPrice(id) && id !in prefs.ownedSkins,
                    onClick = { actions.buySkin(id); purchase = null },
                )
            },
        ) {
            Text(stringResource(R.string.confirm_skin_purchase, Rewards.skinPrice(id), prefs.credits - Rewards.skinPrice(id)))
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
            MenuArtworkButton(stringResource(R.string.main_menu), onMenu, compact = true, fillWidth = false)
            MenuArtworkButton(stringResource(R.string.review_board), actions::reviewBoard, compact = true, fillWidth = false)
            MenuArtworkButton(stringResource(R.string.play_again), { transition(actions::retry) }, compact = true, primary = true, fillWidth = false)
            if (next != null) MenuArtworkButton(stringResource(R.string.next_challenge), { transition { actions.startChallenge(next) } }, compact = true, primary = true, fillWidth = false)
        },
    ) {
        Text(stringResource(resultText(result)), style = MaterialTheme.typography.bodyLarge)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(stringResource(R.string.turn, game.turn), color = Cyan)
            Text(stringResource(R.string.trace, game.trace), color = Cyan)
        }
        if (victory) {
            val score = Rewards.victoryScore(game.turn, game.trace)
            val bestRecord = state.challengeLevel?.let { state.preferences.challengeBest[it] }
                ?.let { Rewards.victoryScore(it.turns, it.trace) }
                ?: state.preferences.bestTurns?.let { turns ->
                    Rewards.victoryScore(turns, state.preferences.bestTrace ?: 0)
                } ?: score
            Text(stringResource(R.string.score_result, score, bestRecord), color = Warning)
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
        state.reward?.newAchievements?.takeIf { it.isNotEmpty() }?.let { fresh ->
            fresh.forEach { Text(stringResource(R.string.achievement_new, stringResource(achievementNameRes(it))), color = Terminal) }
        }
    }
}

/** String resources for an achievement id; unknown ids fall back to first victory. */
internal fun achievementNameRes(id: String): Int = when (id) {
    Achievement.FIVE_CHALLENGES.id -> R.string.ach_five_challenges
    Achievement.EXPLORER.id -> R.string.ach_explorer
    Achievement.CLEAN_RUN.id -> R.string.ach_clean_run
    else -> R.string.ach_first_victory
}

internal fun achievementDescRes(id: String): Int = when (id) {
    Achievement.FIVE_CHALLENGES.id -> R.string.ach_five_challenges_desc
    Achievement.EXPLORER.id -> R.string.ach_explorer_desc
    Achievement.CLEAN_RUN.id -> R.string.ach_clean_run_desc
    else -> R.string.ach_first_victory_desc
}
