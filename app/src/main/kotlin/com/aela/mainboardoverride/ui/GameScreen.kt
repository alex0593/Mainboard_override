package com.aela.mainboardoverride.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.horizontalScroll
import androidx.compose.ui.platform.testTag
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.zIndex
import androidx.compose.runtime.remember
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.painterResource
import androidx.compose.foundation.Image
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aela.mainboardoverride.MainViewModel
import com.aela.mainboardoverride.GameUiState
import com.aela.mainboardoverride.R
import com.aela.mainboardoverride.domain.Domino
import com.aela.mainboardoverride.domain.GameEngine
import com.aela.mainboardoverride.domain.Orientation
import com.aela.mainboardoverride.domain.ScriptCard
import com.aela.mainboardoverride.domain.ScriptType
import com.aela.mainboardoverride.domain.BoardBuff
import androidx.compose.animation.core.*

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun GameScreen(state: GameUiState, actions: MainViewModel, onMenu: () -> Unit) {
    val transition = LocalWindowTransition.current
    val game = state.game
    if (game == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("NO SESSION") }
        return
    }
    var helpOpen by rememberSaveable { mutableStateOf(false) }
    var pendingExitAction by rememberSaveable { mutableStateOf<GameExitAction?>(null) }
    val pingProgress = remember { Animatable(0f) }
    LaunchedEffect(game.pingPreview) {
        if (game.pingPreview.isEmpty()) {
            pingProgress.snapTo(0f)
        } else {
            pingProgress.snapTo(1f)
            pingProgress.animateTo(0f, animationSpec = tween(5_000, easing = LinearEasing))
        }
    }
    val pingActive = pingProgress.value > 0f && game.pingPreview.isNotEmpty()
    BackHandler(enabled = pendingExitAction == null) { pendingExitAction = GameExitAction.EXIT }
    CircuitBackground(backgroundResource = if (state.challengeLevel == null) {
        scenarioBackgroundResource(state.scenarioId)
    } else R.drawable.menu_background_v2) {
        Column(Modifier.fillMaxSize().padding(8.dp)) {
            Row(Modifier.fillMaxWidth().testTag("header-row"), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.weight(1f).heightIn(min = 64.dp)) {
                    // PING takes over the whole band while its preview lasts; the header comes back
                    // on its own once the countdown reaches zero.
                    if (pingActive) {
                        PingHeader(
                            tiles = game.pingPreview,
                            skin = state.preferences.dominoSkin,
                            accent = scenarioAccentColor(state.scenarioId),
                            progress = pingProgress.value,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    } else {
                        GameHeader(
                            game,
                            Modifier.fillMaxWidth(),
                            trailing = {
                                val helpInteraction = remember { MutableInteractionSource() }
                                TextButton(
                                    onClick = { helpOpen = true },
                                    modifier = Modifier.size(48.dp).testTag("general-help")
                                        .pressFeedback(helpInteraction, label = "help"),
                                    interactionSource = helpInteraction,
                                ) { Text("?", color = Cyan) }
                            },
                        ) {
                            game.scriptHand.groupBy { it.type }.values.forEach { stack ->
                                ScriptStack(
                                    cards = stack,
                                    selectedId = state.selectedScriptId,
                                    enabled = game.ram >= stack.first().type.ramCost && game.result == null,
                                    onSelect = actions::selectScript,
                                )
                            }
                        }
                    }
                }
                Column(Modifier.width(92.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    LevelActionButton(stringResource(R.string.exit_match), R.drawable.ic_leave_level, { pendingExitAction = GameExitAction.EXIT }, Modifier.testTag("exit-game"))
                    LevelActionButton(stringResource(R.string.retry_level), R.drawable.ic_retry_level, { pendingExitAction = GameExitAction.RESTART }, Modifier.testTag("restart-game"))
                }
            }
            // Only the pending noise line lives between the header and the PCB. The strip keeps a
            // fixed 14 dp and the line is measured unbounded inside it, so a taller font overflows
            // its own band instead of growing it and pushing the PCB down.
            Box(
                Modifier.fillMaxWidth().height(14.dp).testTag("pending-trace-band"),
                contentAlignment = Alignment.Center,
            ) {
                if (game.tilePlacedThisTurn && game.result == null) {
                    Text(
                        stringResource(R.string.pending_trace, game.pendingNoise),
                        color = scenarioAccentColor(state.scenarioId),
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        maxLines = 1,
                        modifier = Modifier.wrapContentHeight(unbounded = true, align = Alignment.CenterVertically)
                            .testTag("pending-trace")
                            .semantics { liveRegion = LiveRegionMode.Polite },
                    )
                }
            }
            BoxWithConstraints(Modifier.weight(1f)) {
                val controlsWidth = (maxWidth * .29f).coerceIn(190.dp, 260.dp)
                Box(Modifier.fillMaxSize()) {
                  Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Box(Modifier.weight(1f).fillMaxHeight()) {
                            Board(
                                board = game.board,
                                skin = state.preferences.boardSkin,
                                dominoSkin = state.preferences.dominoSkin,
                                legalOrigins = state.selectedScriptId?.let {
                                    GameEngine.scriptTargets(game, it, state.bridgeHorizontal)
                                } ?: state.selectedDominoId?.takeIf { !game.tilePlacedThisTurn && state.selectedScriptId == null && game.result == null }?.let { id ->
                                    GameEngine.legalPlacements(game).filter {
                                        it.dominoId == id &&
                                            it.orientation == (if (state.rotationSteps % 2 == 0) Orientation.HORIZONTAL else Orientation.VERTICAL) &&
                                            (it.rotated == (state.rotationSteps >= 2) || game.dominoHand.any { tile -> tile.id == id && tile.first == tile.second })
                                    }.map { it.origin }.toSet()
                                }.orEmpty(),
                                target = null,
                                modifier = Modifier.fillMaxSize(),
                                onCell = actions::placeAt,
                                animatePlacement = !state.preferences.reducedMotion,
                                sessionKey = state.matchId,
                            )
                        }
                    if (state.reviewingBoard) ReviewPanel(controlsWidth, actions)
                    if (!state.reviewingBoard) Column(Modifier.width(controlsWidth).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState())) {
                            state.lastBuff?.let { buff ->
                                Text(stringResource(if (buff == BoardBuff.TRACE_COOLER) R.string.buff_trace_collected else R.string.buff_ram_collected), color = Cyan, fontSize = 11.sp)
                            }
                        }
                        HardwareHand(game.dominoHand, state.selectedDominoId, state.preferences.dominoSkin,
                            Modifier.align(Alignment.CenterHorizontally), onSelect = actions::selectDomino)
                      // Keep the two controls together below the hardware frame.
                      Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(0.dp)) {
                        if (state.selectedScriptId != null) {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                if (game.scriptHand.any { it.id == state.selectedScriptId && it.type == ScriptType.BRIDGE }) {
                                    BridgeControl(state.bridgeHorizontal, actions::toggleBridge, Modifier.weight(1f))
                                }
                                GameControlButton(stringResource(R.string.cancel), actions::cancelScript, Modifier.weight(1f).testTag("cancel-script"))
                            }
                        } else {
                            GameControlButton(stringResource(R.string.rotate), actions::rotate, Modifier.fillMaxWidth().testTag("rotate"), enabled = state.selectedDominoId != null && game.result == null)
                        }
                        GameControlButton(stringResource(R.string.end_turn), actions::endTurn, Modifier.fillMaxWidth().testTag("end-turn"), enabled = game.tilePlacedThisTurn && game.result == null, highlighted = state.endTurnHint)
                      }
                    }
                  }
                  // A floating inspection window sits at the board/hardware junction.
                  game.dominoHand.find { it.id == state.selectedDominoId }?.let { tile ->
                    RotationPreview(tile, state.rotationSteps, state.preferences.dominoSkin,
                        Modifier.align(Alignment.TopEnd).offset(x = (-controlsWidth - 8.dp), y = 10.dp).zIndex(3f))
                  }
                }
            }
        }
    }

    val selected = game.scriptHand.find { it.id == state.selectedScriptId }
    if (selected?.type == ScriptType.SPOOF && state.selectedDominoId != null) {
        game.dominoHand.find { it.id == state.selectedDominoId }?.let { tile ->
            SpoofDialog(tile, state.spoofHalf, state.spoofValue, state.message?.let { rejectionText(it) },
                actions::setSpoofHalf, actions::setSpoofValue, { actions.spoof(state.spoofValue) }, actions::cancelScript)
        }
    }
    if (state.message != null) {
        GameDialog(
            title = stringResource(R.string.action_error),
            onDismiss = actions::dismissMessage,
            modifier = Modifier.testTag("error-dialog"),
            accent = Danger,
            actions = {
                MenuArtworkButton(stringResource(R.string.help_close), actions::dismissMessage, compact = true, fillWidth = false, modifier = Modifier.testTag("close-error"))
            },
        ) { Text(stringResource(rejectionText(state.message)), color = Danger) }
    }
    if (helpOpen) GeneralGameHelp { helpOpen = false }
    pendingExitAction?.let { action ->
        ConfirmGameDialog(
            title = stringResource(if (action == GameExitAction.EXIT) R.string.confirm_exit_title else R.string.confirm_restart_title),
            message = stringResource(R.string.confirm_match_loss),
            confirmLabel = stringResource(if (action == GameExitAction.EXIT) R.string.confirm_exit else R.string.confirm_restart),
            onDismiss = { pendingExitAction = null },
            onConfirm = {
                pendingExitAction = null
                if (action == GameExitAction.EXIT) onMenu() else transition(actions::restartNetwork)
            },
        )
    }
    if (!state.reviewingBoard) game.result?.let { MatchResultDialog(state, actions, onMenu) }
}

@Composable
private fun ReviewPanel(width: androidx.compose.ui.unit.Dp, actions: MainViewModel) {
    val transition = LocalWindowTransition.current
    Box(Modifier.width(width).fillMaxHeight().clip(RoundedCornerShape(10.dp)).border(1.dp, Cyan.copy(alpha = .3f), RoundedCornerShape(10.dp))) {
        Image(painterResource(R.drawable.menu_card_v1), contentDescription = null, modifier = Modifier.matchParentSize(), contentScale = androidx.compose.ui.layout.ContentScale.FillBounds)
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(stringResource(R.string.review_board), color = Cyan, fontSize = 13.sp)
            SmallButton(stringResource(R.string.result_summary), actions::showResult, Modifier.fillMaxWidth().height(42.dp))
            SmallButton(stringResource(R.string.play_again), { transition(actions::retry) }, Modifier.fillMaxWidth().height(42.dp))
        }
    }
}

@Composable
private fun ScriptStack(
    cards: List<ScriptCard>,
    selectedId: String?,
    enabled: Boolean,
    onSelect: (String) -> Unit,
) {
    val overlap = 10.dp
    Box(
        Modifier.width(88.dp + overlap * (cards.size - 1).coerceAtLeast(0))
            .heightIn(min = 48.dp),
    ) {
        cards.forEachIndexed { index, card ->
            ScriptCardView(
                card = card,
                selected = selectedId == card.id,
                enabled = enabled,
                compact = true,
                modifier = Modifier.offset(x = overlap * index).zIndex(index.toFloat()),
            ) { onSelect(card.id) }
        }
        if (cards.size > 1) {
            Surface(
                modifier = Modifier.align(Alignment.TopEnd).zIndex(cards.size.toFloat() + 1f),
                shape = RoundedCornerShape(8.dp),
                color = Cyan,
                contentColor = Void,
            ) {
                Text(
                    "×${cards.size}",
                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                )
            }
        }
    }
}

/**
 * Stands in for [GameHeader] while PING keeps a preview on screen. The opaque panel hides the turn,
 * RAM and trace indicators, the script hand, the help button and the header artwork, leaving only
 * the revealed tiles and the countdown; the slot keeps its 64 dp floor like the header itself, so
 * the PCB underneath never moves.
 */
@Composable
private fun PingHeader(
    tiles: List<Domino>,
    skin: String,
    accent: Color,
    progress: Float,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(10.dp)
    Box(
        modifier
            .heightIn(min = 64.dp)
            .testTag("ping-header")
            .clip(shape)
            .background(Panel.copy(alpha = .98f))
            .border(BorderStroke(1.dp, accent.copy(alpha = .6f)), shape),
    ) {
        Row(
            Modifier.fillMaxWidth().heightIn(min = 64.dp).padding(horizontal = 18.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(stringResource(R.string.ping_preview), color = accent, fontSize = 11.sp)
            Row(
                Modifier.weight(1f).horizontalScroll(rememberScrollState()).testTag("ping-tiles"),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                tiles.forEach { tile ->
                    DominoImage(
                        tile,
                        Modifier.width(60.dp),
                        orientation = Orientation.HORIZONTAL,
                        skin = skin,
                    )
                }
            }
        }
        Box(
            Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(3.dp)
                .background(accent.copy(alpha = .18f))
                .testTag("ping-timer"),
        ) {
            Box(
                Modifier.fillMaxWidth(progress.coerceIn(0f, 1f))
                    .fillMaxHeight()
                    .background(accent),
            )
        }
    }
}
