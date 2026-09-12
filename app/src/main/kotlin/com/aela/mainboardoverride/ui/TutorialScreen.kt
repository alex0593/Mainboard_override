package com.aela.mainboardoverride.ui

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
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
import com.aela.mainboardoverride.R
import com.aela.mainboardoverride.data.PlayerPreferences
import com.aela.mainboardoverride.domain.*

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun TutorialScreen(preferences: PlayerPreferences, controller: TutorialController, onBack: () -> Unit) {
    var running by remember { mutableStateOf(false) }
    var instructionOpen by remember { mutableStateOf(false) }
    val state by controller.state.collectAsState()
    val lessonScroll = rememberScrollState()
    LaunchedEffect(state.lesson, state.step) {
        lessonScroll.scrollTo(0)
        if (!state.finished) instructionOpen = true
    }
    val expected = state.expected
    val titles = stringArrayResource(R.array.tutorial_titles)
    val instructions = stringArrayResource(R.array.tutorial_instructions)
    CircuitBackground {
        Column(Modifier.fillMaxSize().padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            ProgressionHeader(stringResource(R.string.tutorial), onBack,
                counter = if (running) "${state.lesson + 1}/${titles.size}" else null)
            if (!running) {
                Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(stringResource(R.string.tutorial_intro), color = Cyan)
                    if (preferences.tutorialCompleted) Text(stringResource(R.string.tutorial_complete), color = Terminal)
                    if (preferences.tutorialLesson >= 0 && !preferences.tutorialCompleted) {
                        MenuArtworkButton(stringResource(R.string.tutorial_continue), {
                            controller.start(preferences.tutorialLesson); running = true; instructionOpen = true
                        }, modifier = Modifier.testTag("tutorial-continue"))
                    }
                    MenuArtworkButton(stringResource(if (preferences.tutorialLesson >= 0) R.string.tutorial_restart else R.string.tutorial_start),
                        { controller.start(0); running = true; instructionOpen = true }, modifier = Modifier.testTag("tutorial-start"))
                }
            } else Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                val target = (expected as? TutorialInput.Cell)?.position
                Board(state.game.board, preferences.boardSkin, preferences.dominoSkin,
                    legalOrigins = setOfNotNull(target), target = target,
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                    animatePlacement = !preferences.reducedMotion,
                    onCell = { controller.dispatch(TutorialInput.Cell(it)) })
                Column(Modifier.weight(.65f).fillMaxHeight().verticalScroll(lessonScroll).padding(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(titles[state.lesson], color = Terminal)
                    Text(stringResource(R.string.tutorial_status, state.game.turn, state.game.ram, state.game.trace, state.game.pendingNoise), color = Cyan)
                    if (state.incorrect) Text(stringResource(R.string.tutorial_wrong), color = Warning)
                    if (state.finished) {
                        MenuArtworkButton(stringResource(if (state.lesson == titles.lastIndex) R.string.tutorial_finish else R.string.tutorial_next_lesson),
                            { if (state.lesson == titles.lastIndex) onBack() else controller.advance() },
                            modifier = Modifier.testTag("tutorial-next-lesson"), compact = true)
                    } else {
                        if (state.tile != null && state.script == null) {
                            Text(stringResource(R.string.tutorial_rotation, state.rotation * 90), color = Muted)
                            state.game.dominoHand.find { it.id == state.tile }?.let { tile ->
                                val horizontal = state.rotation % 2 == 0
                                DominoImage(if (state.rotation >= 2) tile.rotated() else tile,
                                    Modifier.width(if (horizontal) 60.dp else 30.dp),
                                    if (horizontal) Orientation.HORIZONTAL else Orientation.VERTICAL, skin = preferences.dominoSkin)
                            }
                            TutorialAction(stringResource(R.string.rotate), TutorialInput.Rotate, state, controller)
                        }
                        if (state.lesson in 5..8) FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            state.game.scriptHand.forEach { card ->
                                ScriptCardView(card, state.script == card.id, true,
                                    highlighted = expected == TutorialInput.SelectScript(card.id), compact = true) {
                                    controller.dispatch(TutorialInput.SelectScript(card.id))
                                }
                            }
                        }
                        if (state.script == "SPOOF" && state.tile != null) {
                            Text(stringResource(R.string.tutorial_spoof_selection, state.half + 1, state.value), color = Muted)
                            state.game.dominoHand.find { it.id == state.tile }?.let { tile ->
                                DominoImage(if (state.half == 0) tile.copy(first = state.value) else tile.copy(second = state.value),
                                    Modifier.width(60.dp), skin = preferences.dominoSkin)
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                (0..1).forEach { half ->
                                    Button({ controller.dispatch(TutorialInput.Half(half)) },
                                        Modifier.heightIn(min = 48.dp).then(if (expected == TutorialInput.Half(half)) Modifier.border(2.dp, Terminal) else Modifier)) {
                                        Text(stringResource(R.string.tutorial_half, half + 1))
                                    }
                                }
                            }
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                (0..6).forEach { value ->
                                    OutlinedButton({ controller.dispatch(TutorialInput.Value(value)) },
                                        Modifier.sizeIn(minWidth = 48.dp, minHeight = 48.dp).then(if (expected == TutorialInput.Value(value)) Modifier.border(2.dp, Terminal) else Modifier),
                                        contentPadding = PaddingValues(8.dp)) { Text("$value") }
                                }
                            }
                            TutorialAction(stringResource(R.string.apply), TutorialInput.ApplySpoof, state, controller)
                            TutorialAction(stringResource(R.string.cancel), TutorialInput.Cancel, state, controller)
                        }
                        if (state.script == "BRIDGE") TutorialAction(
                            stringResource(if (state.horizontalBridge) R.string.tutorial_bridge_horizontal else R.string.tutorial_bridge_vertical),
                            TutorialInput.ToggleBridge, state, controller)
                        if (state.game.tilePlacedThisTurn) TutorialAction(stringResource(R.string.end_turn), TutorialInput.EndTurn, state, controller)
                        state.game.pingPreview.forEach { tile ->
                            Text(stringResource(R.string.ping_preview), color = Muted)
                            DominoImage(tile, Modifier.width(60.dp), skin = preferences.dominoSkin)
                        }
                    }
                    TextButton({ controller.start(state.lesson); instructionOpen = true }, Modifier.heightIn(min = 48.dp)) {
                        Text(stringResource(R.string.tutorial_repeat))
                    }
                }
            }
            if (!state.finished) {
                Row(
                    Modifier.fillMaxWidth().heightIn(min = 72.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    FlowRow(
                        Modifier.weight(1f),
                        maxItemsInEachRow = 3,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp),
                    ) {
                        state.game.dominoHand.forEach { tile ->
                            DominoView(tile, state.tile == tile.id, preferences.dominoSkin,
                                highlighted = expected == TutorialInput.SelectTile(tile.id),
                                modifier = Modifier.weight(1f).testTag("tutorial-tile-${tile.id}")) {
                                controller.dispatch(TutorialInput.SelectTile(tile.id))
                            }
                        }
                    }
                    if (expected == TutorialInput.Next || instructionOpen) {
                        TutorialAction(stringResource(R.string.tutorial_next), TutorialInput.Next, state, controller,
                            tag = "tutorial-next-fixed",
                            onClick = {
                                instructionOpen = false
                                if (expected == TutorialInput.Next) controller.dispatch(TutorialInput.Next)
                            })
                    }
                }
            }
        }
    }
    if (running && instructionOpen && !state.finished) {
        GameDialog(
            title = titles[state.lesson],
            onDismiss = { instructionOpen = false },
            modifier = Modifier.testTag("tutorial-instruction-dialog"),
            actions = {
                GameControlButton(
                    stringResource(R.string.tutorial_next),
                    {
                        instructionOpen = false
                        if (expected == TutorialInput.Next) controller.dispatch(TutorialInput.Next)
                    },
                    Modifier.testTag("tutorial-action-Next"),
                )
            },
        ) {
            Text(
                instructions[state.definition.steps[state.step].instruction],
                Modifier.semantics { liveRegion = LiveRegionMode.Polite }.testTag("tutorial-instruction"),
                color = Cyan,
            )
        }
    }
}

@Composable
private fun TutorialAction(label: String, input: TutorialInput, state: TutorialState, controller: TutorialController, tag: String = "tutorial-action-${input::class.simpleName}", onClick: (() -> Unit)? = null) {
    GameControlButton(label, onClick ?: { controller.dispatch(input) }, highlighted = state.expected == input,
        modifier = Modifier.testTag(tag))
}
