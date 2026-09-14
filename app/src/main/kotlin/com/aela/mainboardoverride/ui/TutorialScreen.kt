package com.aela.mainboardoverride.ui

import androidx.compose.foundation.BorderStroke
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
    val stepDefinition = state.definition.steps.getOrNull(state.step)
    var instructionOpen by remember(running, state.lesson, state.step, repeat) {
        mutableStateOf(stepDefinition?.pauseBeforeAction == true || state.expected == TutorialInput.Next)
    }
    var helpOpen by remember(instructionOpen, state.lesson, state.step) { mutableStateOf(false) }
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
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    GameHeader(state.game, Modifier.weight(1f)) {
                        if (state.lesson in 5..8) state.game.scriptHand.forEach { card ->
                            ScriptCardView(card, state.script == card.id, !instructionOpen,
                                highlighted = expected == TutorialInput.SelectScript(card.id), compact = true) {
                                dispatch(TutorialInput.SelectScript(card.id))
                            }
                        }
                    }
                    TextButton(onBack, Modifier.width(92.dp)) { Text(stringResource(R.string.back)) }
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
                                state.game.pingPreview.forEach { tile ->
                                    Text(stringResource(R.string.ping_preview), color = Muted)
                                    DominoImage(tile, Modifier.width(60.dp), skin = preferences.dominoSkin)
                                }

                            }
                            HardwareHand(state.game.dominoHand, state.tile, preferences.dominoSkin,
                                highlighted = (expected as? TutorialInput.SelectTile)?.id,
                                tagPrefix = "tutorial-tile") { dispatch(TutorialInput.SelectTile(it)) }
                            if (state.script == "BRIDGE") {
                                BridgeControl(state.horizontalBridge, { dispatch(TutorialInput.ToggleBridge) },
                                    enabled = !instructionOpen, highlighted = expected == TutorialInput.ToggleBridge,
                                    tag = "tutorial-action-ToggleBridge")
                            } else if (state.script != "SPOOF") {
                                GameControlButton(stringResource(R.string.rotate), { dispatch(TutorialInput.Rotate) },
                                    Modifier.fillMaxWidth().testTag("tutorial-action-Rotate"),
                                    enabled = !instructionOpen && state.tile != null && state.script == null,
                                    highlighted = expected == TutorialInput.Rotate)
                            }
                            GameControlButton(stringResource(R.string.end_turn), { dispatch(TutorialInput.EndTurn) },
                                Modifier.testTag("tutorial-action-EndTurn"),
                                enabled = !instructionOpen && state.game.tilePlacedThisTurn,
                                highlighted = expected == TutorialInput.EndTurn)
                        }
                    }
                    if (!instructionOpen && !state.finished && state.script == null) {
                        state.game.dominoHand.find { it.id == state.tile }?.let { tile ->
                            RotationPreview(tile, state.rotation, preferences.dominoSkin,
                                Modifier.align(Alignment.TopEnd).offset(x = (-panelWidth - 8.dp), y = 10.dp))
                        }
                    }
                    // In-layout overlay: the board stays mounted, with no modal window or dim layer.
                    if (instructionOpen || state.finished) {
                        Surface(Modifier.align(Alignment.TopEnd).widthIn(max = 360.dp).fillMaxWidth(.55f)
                            .fillMaxHeight().testTag("tutorial-instruction-dialog"),
                            color = Panel.copy(alpha = .98f), shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, Cyan)) {
                            Column(Modifier.fillMaxSize().padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text("${state.lesson + 1}/${titles.size} · ${titles[state.lesson]}", color = Terminal)
                                    Text(if (state.finished) stringResource(R.string.tutorial_complete)
                                        else instructions[state.definition.steps[state.step].instruction],
                                        Modifier.testTag("tutorial-instruction").semantics { liveRegion = LiveRegionMode.Polite },
                                        color = Cyan)
                                }
                                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                    TextButton({ controller.start(state.lesson); repeat++ },
                                        Modifier.weight(1f).testTag("tutorial-repeat")) {
                                        Text(stringResource(R.string.tutorial_repeat))
                                    }
                                    TextButton({ helpOpen = true }, Modifier.size(48.dp).testTag("tutorial-help")) {
                                        Text("?")
                                    }
                                }
                                GameControlButton(
                                    stringResource(if (state.finished) R.string.tutorial_finish else R.string.tutorial_next),
                                    {
                                        if (state.finished) onBack()
                                        else {
                                            instructionOpen = false
                                            if (expected == TutorialInput.Next) controller.dispatch(TutorialInput.Next)
                                        }
                                    }, Modifier.fillMaxWidth().testTag("tutorial-action-Next"))
                            }
                        }
                        if (helpOpen) GeneralGameHelp { helpOpen = false }
                    }
                }
            }
        }
    }
    if (running && !instructionOpen && !state.finished && state.script == "SPOOF") {
        state.game.dominoHand.find { it.id == state.tile }?.let { tile ->
            SpoofDialog(tile, state.half, state.value,
                if (state.incorrect) R.string.tutorial_wrong else null,
                { dispatch(TutorialInput.Half(it)) }, { dispatch(TutorialInput.Value(it)) },
                { dispatch(TutorialInput.ApplySpoof) }, { dispatch(TutorialInput.Cancel) },
                tutorialExpected = expected)
        }
    }
}
