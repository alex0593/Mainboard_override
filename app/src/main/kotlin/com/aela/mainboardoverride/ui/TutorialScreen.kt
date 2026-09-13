package com.aela.mainboardoverride.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aela.mainboardoverride.R
import com.aela.mainboardoverride.data.PlayerPreferences
import com.aela.mainboardoverride.domain.*

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun TutorialScreen(preferences: PlayerPreferences, controller: TutorialController, onBack: () -> Unit) {
    var running by remember { mutableStateOf(false) }
    var repeat by remember { mutableIntStateOf(0) }
    val state by controller.state.collectAsState()
    var instructionOpen by remember(running, state.lesson, state.step, repeat) { mutableStateOf(true) }
    val expected = state.expected
    val titles = stringArrayResource(R.array.tutorial_titles)
    val instructions = stringArrayResource(R.array.tutorial_instructions)
    val dispatch: (TutorialInput) -> Unit = { input ->
        if (!instructionOpen && !state.finished) controller.dispatch(input)
    }
    LaunchedEffect(running, state.lesson, state.finished) {
        if (running && state.finished && state.lesson < titles.lastIndex) controller.advance()
    }
    CircuitBackground {
        Column(Modifier.fillMaxSize().padding(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            if (!running) {
                ProgressionHeader(stringResource(R.string.tutorial), onBack)
                Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(stringResource(R.string.tutorial_intro), color = Cyan)
                    if (preferences.tutorialCompleted) Text(stringResource(R.string.tutorial_complete), color = Terminal)
                    if (preferences.tutorialLesson >= 0 && !preferences.tutorialCompleted) {
                        MenuArtworkButton(stringResource(R.string.tutorial_continue), {
                            controller.start(preferences.tutorialLesson); running = true
                        }, modifier = Modifier.testTag("tutorial-continue"))
                    }
                    MenuArtworkButton(stringResource(if (preferences.tutorialLesson >= 0) R.string.tutorial_restart else R.string.tutorial_start),
                        { controller.start(0); running = true }, modifier = Modifier.testTag("tutorial-start"))
                }
            } else {
                Surface(color = Panel, shape = RoundedCornerShape(8.dp)) {
                    Row(Modifier.fillMaxWidth().padding(4.dp), verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Column(Modifier.widthIn(max = 180.dp)) {
                            Text("${state.lesson + 1}/${titles.size} · ${titles[state.lesson]}", color = Terminal, fontSize = 12.sp)
                            Text(stringResource(R.string.tutorial_status, state.game.turn, state.game.ram, state.game.trace, state.game.pendingNoise),
                                color = Cyan, fontSize = 11.sp)
                        }
                        Row(Modifier.weight(1f).horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                            if (state.lesson in 5..8) state.game.scriptHand.forEach { card ->
                                ScriptCardView(card, state.script == card.id, !instructionOpen,
                                    highlighted = expected == TutorialInput.SelectScript(card.id), compact = true) { dispatch(TutorialInput.SelectScript(card.id)) }
                            }
                        }
                        TextButton(onBack) { Text(stringResource(R.string.back)) }
                    }
                }
                BoxWithConstraints(Modifier.weight(1f).fillMaxWidth()) {
                    val panelWidth = (maxWidth * .29f).coerceIn(160.dp, 230.dp)
                    Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        val target = (expected as? TutorialInput.Cell)?.position
                        Board(state.game.board, preferences.boardSkin, preferences.dominoSkin,
                            legalOrigins = setOfNotNull(target), target = target,
                            modifier = Modifier.weight(1f).fillMaxHeight().testTag("tutorial-board"),
                            animatePlacement = !preferences.reducedMotion,
                            onCell = { dispatch(TutorialInput.Cell(it)) })
                        Column(Modifier.width(panelWidth).fillMaxHeight(),
                            horizontalAlignment = Alignment.CenterHorizontally) {
                            Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()),
                                verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                if (state.incorrect) Text(stringResource(R.string.tutorial_wrong), color = Warning)
                                if (state.tile != null && state.script == null) {
                                    Text(stringResource(R.string.tutorial_rotation, state.rotation * 90), color = Cyan, fontSize = 11.sp)
                                    state.game.dominoHand.find { it.id == state.tile }?.let { tile ->
                                        val horizontal = state.rotation % 2 == 0
                                        DominoImage(if (state.rotation >= 2) tile.rotated() else tile,
                                            Modifier.width(if (horizontal) 60.dp else 30.dp),
                                            if (horizontal) Orientation.HORIZONTAL else Orientation.VERTICAL,
                                            skin = preferences.dominoSkin)
                                    }
                                }
                                if (state.script == "SPOOF" && state.tile != null) {
                                    Text(stringResource(R.string.tutorial_spoof_selection, state.half + 1, state.value), color = Muted)
                                    state.game.dominoHand.find { it.id == state.tile }?.let { tile ->
                                        DominoImage(if (state.half == 0) tile.copy(first = state.value) else tile.copy(second = state.value),
                                            Modifier.width(60.dp), skin = preferences.dominoSkin)
                                    }
                                }
                                if (state.script == "BRIDGE") TutorialAction(
                                    stringResource(if (state.horizontalBridge) R.string.tutorial_bridge_horizontal else R.string.tutorial_bridge_vertical),
                                    TutorialInput.ToggleBridge, state, dispatch)
                                state.game.pingPreview.forEach { tile ->
                                    Text(stringResource(R.string.ping_preview), color = Muted)
                                    DominoImage(tile, Modifier.width(60.dp), skin = preferences.dominoSkin)
                                }

                            }
                            HardwareHand(state.game.dominoHand, state.tile, preferences.dominoSkin,
                                highlighted = (expected as? TutorialInput.SelectTile)?.id,
                                tagPrefix = "tutorial-tile") { dispatch(TutorialInput.SelectTile(it)) }
                            if (state.script == "SPOOF") {
                                Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    listOf(0..3, 4..6).forEach { rowValues ->
                                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                                            rowValues.forEach { value ->
                                                OutlinedButton({ dispatch(TutorialInput.Value(value)) },
                                                    Modifier.weight(1f).height(44.dp).testTag("tutorial-value-$value")
                                                        .then(if (expected == TutorialInput.Value(value)) Modifier.border(2.dp, Terminal) else Modifier),
                                                    contentPadding = PaddingValues(0.dp)) { Text("$value") }
                                            }
                                        }
                                    }
                                }
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                                    (0..1).forEach { half ->
                                        GameControlButton(stringResource(R.string.tutorial_half, half + 1),
                                            { dispatch(TutorialInput.Half(half)) },
                                            Modifier.weight(1f).testTag("tutorial-half-$half"),
                                            enabled = !instructionOpen,
                                            highlighted = expected == TutorialInput.Half(half))
                                    }
                                }
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                                    GameControlButton(stringResource(R.string.apply), { dispatch(TutorialInput.ApplySpoof) },
                                        Modifier.weight(1f).testTag("tutorial-action-ApplySpoof"),
                                        enabled = !instructionOpen, highlighted = expected == TutorialInput.ApplySpoof)
                                    GameControlButton(stringResource(R.string.cancel), { dispatch(TutorialInput.Cancel) },
                                        Modifier.weight(1f).testTag("tutorial-action-Cancel"),
                                        enabled = !instructionOpen, highlighted = expected == TutorialInput.Cancel)
                                }
                            } else {
                                GameControlButton(stringResource(R.string.rotate), { dispatch(TutorialInput.Rotate) },
                                    Modifier.testTag("tutorial-action-Rotate"),
                                    enabled = !instructionOpen && state.tile != null && state.script == null,
                                    highlighted = expected == TutorialInput.Rotate)
                            }
                            GameControlButton(stringResource(R.string.end_turn), { dispatch(TutorialInput.EndTurn) },
                                Modifier.testTag("tutorial-action-EndTurn"),
                                enabled = !instructionOpen && state.game.tilePlacedThisTurn,
                                highlighted = expected == TutorialInput.EndTurn)
                        }
                    }
                    // In-layout overlay: the board stays mounted, with no modal window or dim layer.
                    if (instructionOpen || state.finished) {
                        Surface(Modifier.align(Alignment.TopEnd).widthIn(max = 360.dp).fillMaxWidth(.55f)
                            .fillMaxHeight().testTag("tutorial-instruction-dialog"),
                            color = Panel.copy(alpha = .98f), shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, Cyan)) {
                            Column(Modifier.padding(12.dp).verticalScroll(rememberScrollState()),
                                verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(titles[state.lesson], color = Terminal)
                                Text(if (state.finished) stringResource(R.string.tutorial_complete)
                                    else instructions[state.definition.steps[state.step].instruction],
                                    Modifier.testTag("tutorial-instruction").semantics { liveRegion = LiveRegionMode.Polite },
                                    color = Cyan)
                            }
                        }
                    }
                }
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    TextButton({ controller.start(state.lesson); repeat++ },
                        Modifier.testTag("tutorial-repeat")) { Text(stringResource(R.string.tutorial_repeat)) }
                    TextButton({ instructionOpen = true }, enabled = !state.finished) { Text("?") }
                    Spacer(Modifier.weight(1f))
                    GameControlButton(stringResource(if (state.finished) R.string.tutorial_finish else R.string.tutorial_next),
                        {
                            if (state.finished) onBack()
                            else {
                                instructionOpen = false
                                if (expected == TutorialInput.Next) controller.dispatch(TutorialInput.Next)
                            }
                        }, Modifier.width(180.dp).testTag("tutorial-action-Next"),
                        enabled = instructionOpen || state.finished)
                }
            }
        }
    }
}

@Composable
private fun TutorialAction(label: String, input: TutorialInput, state: TutorialState, dispatch: (TutorialInput) -> Unit) {
    GameControlButton(label, { dispatch(input) }, highlighted = state.expected == input,
        modifier = Modifier.testTag("tutorial-action-${input::class.simpleName}"))
}
