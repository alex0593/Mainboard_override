package com.aela.mainboardoverride.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.LiveRegionMode
import com.aela.mainboardoverride.domain.Tutorial
import com.aela.mainboardoverride.domain.TutorialStep
import com.aela.mainboardoverride.domain.RejectReason
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.aela.mainboardoverride.MainViewModel
import com.aela.mainboardoverride.GameUiState
import com.aela.mainboardoverride.R
import com.aela.mainboardoverride.domain.BOARD_HEIGHT
import com.aela.mainboardoverride.domain.BOARD_WIDTH
import com.aela.mainboardoverride.domain.BoardState
import com.aela.mainboardoverride.domain.Domino
import com.aela.mainboardoverride.domain.GameEngine
import com.aela.mainboardoverride.domain.GameResult
import com.aela.mainboardoverride.domain.MAX_RAM
import com.aela.mainboardoverride.domain.Orientation
import com.aela.mainboardoverride.domain.Position
import com.aela.mainboardoverride.domain.ScriptCard
import com.aela.mainboardoverride.domain.ScriptType

private const val MENU = "menu"
private const val GAME = "game"
private const val SETTINGS = "settings"
private const val HELP = "help"

@Composable
fun MainboardApp(nav: NavHostController, state: GameUiState, actions: MainViewModel) {
    NavHost(navController = nav, startDestination = MENU) {
        composable(MENU) {
            MenuScreen(
                state = state,
                onNew = { actions.start(); nav.navigate(GAME) },
                onRetry = { state.preferences.lastSeed?.let(actions::start); nav.navigate(GAME) },
                onTutorial = { actions.start(tutorial = true); nav.navigate(GAME) },
                onSettings = { nav.navigate(SETTINGS) },
                onHelp = { nav.navigate(HELP) },
            )
        }
        composable(GAME) {
            GameScreen(
                state = state,
                actions = actions,
                onMenu = { nav.popBackStack(MENU, inclusive = false) },
            )
        }
        composable(SETTINGS) { SettingsScreen(state, actions) { nav.popBackStack() } }
        composable(HELP) { InfoScreen { nav.popBackStack() } }
    }
}

@Composable
private fun MenuScreen(
    state: GameUiState,
    onNew: () -> Unit,
    onRetry: () -> Unit,
    onTutorial: () -> Unit,
    onSettings: () -> Unit,
    onHelp: () -> Unit,
) {
    CircuitBackground {
        Row(
            Modifier.fillMaxSize().padding(horizontal = 24.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
                Text("> ROOT://MAINBOARD", color = Cyan, fontFamily = FontFamily.Monospace)
                Text(
                    "OVERRIDE",
                    color = Terminal,
                    fontSize = 38.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Monospace,
                )
                Text(stringResource(R.string.tagline), color = Muted, letterSpacing = 3.sp)
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
                TerminalButton(stringResource(R.string.new_network), onNew)
                if (state.preferences.lastSeed != null) TerminalButton(stringResource(R.string.retry_seed), onRetry, primary = false)
                TerminalButton(stringResource(R.string.tutorial), onTutorial, primary = false)
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    SmallButton(stringResource(R.string.settings), onSettings, Modifier.weight(1f))
                    SmallButton(stringResource(R.string.help), onHelp, Modifier.weight(1f))
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun GameScreen(state: GameUiState, actions: MainViewModel, onMenu: () -> Unit) {
    val game = state.game
    if (game == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("NO SESSION") }
        return
    }
    var showHelp by remember { mutableStateOf(false) }
    CircuitBackground {
        Column(Modifier.fillMaxSize().padding(8.dp)) {
            FlowRow(
                Modifier.fillMaxWidth().heightIn(min = 44.dp)
                    .background(Panel.copy(alpha = .95f), RoundedCornerShape(8.dp))
                    .border(1.dp, Cyan.copy(alpha = .25f), RoundedCornerShape(8.dp)).padding(horizontal = 8.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                TextButton(onClick = onMenu) { Text("< ${stringResource(R.string.main_menu)}") }
                StatusModule(stringResource(R.string.turn, game.turn), Cyan)
                StatusModule(stringResource(R.string.ram, game.ram, MAX_RAM), Terminal) {
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        repeat(MAX_RAM) { index ->
                            Box(Modifier.size(22.dp, 4.dp).background(if (index < game.ram) Terminal else Muted.copy(alpha = .2f)))
                        }
                    }
                }
                val traceColor = if (game.trace >= 80) Danger else Warning
                StatusModule(stringResource(R.string.trace, game.trace), traceColor) {
                    Box(Modifier.width(96.dp).height(4.dp).background(Muted.copy(alpha = .2f))) {
                        Box(Modifier.fillMaxWidth(game.trace / 100f).fillMaxHeight().background(traceColor))
                    }
                }
                TextButton(onClick = { showHelp = true }) { Text(stringResource(R.string.help)) }
            }
            Spacer(Modifier.height(8.dp))
            BoxWithConstraints(Modifier.weight(1f)) {
            val controlsWidth = (maxWidth * .29f).coerceIn(190.dp, 260.dp)
            Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Board(
                    board = game.board,
                    legalOrigins = state.selectedDominoId?.takeIf { !game.tilePlacedThisTurn && state.selectedScriptId == null && game.result == null }?.let { id ->
                        GameEngine.legalPlacements(game).filter {
                            it.dominoId == id &&
                                it.orientation == (if (state.rotationSteps % 2 == 0) Orientation.HORIZONTAL else Orientation.VERTICAL) &&
                                (it.rotated == (state.rotationSteps >= 2) || game.dominoHand.any { tile -> tile.id == id && tile.first == tile.second })
                        }.map { it.origin }.toSet()
                    }.orEmpty(),
                    target = state.tutorialStep?.let(Tutorial::target),
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                    onCell = actions::placeAt,
                )
                Column(
                    Modifier.width(controlsWidth).fillMaxHeight()
                        .background(Brush.verticalGradient(listOf(Panel, Void)), RoundedCornerShape(8.dp))
                        .border(1.dp, Cyan.copy(alpha = .3f), RoundedCornerShape(8.dp))
                        .verticalScroll(rememberScrollState()).padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    state.tutorialStep?.let { step ->
                        TutorialPanel(step, state.tutorialBlocked, actions::continueTutorial, actions::restartLesson)
                    }
                    Text(stringResource(R.string.seed, game.seed), color = Muted, fontSize = 11.sp)
                    Text(stringResource(R.string.pending_noise, game.pendingNoise), color = Warning, fontSize = 12.sp)
                    PanelHeading(stringResource(R.string.scripts))
                    game.scriptHand.forEach { card ->
                        ScriptCardView(card, state.selectedScriptId == card.id, game.ram >= card.type.ramCost && game.result == null, highlighted = state.tutorialStep?.let { step -> when(card.type) { ScriptType.PING -> step == TutorialStep.PING; ScriptType.SPOOF -> step == TutorialStep.SPOOF; ScriptType.KILL_PROCESS -> step == TutorialStep.KILL; ScriptType.BRIDGE -> step == TutorialStep.BRIDGE } } == true) {
                            actions.selectScript(card.id)
                        }
                    }
                    state.message?.let {
                        Text(stringResource(rejectionText(it)), color = Danger, fontSize = 11.sp)
                    }
                    if (game.scriptHand.any { it.id == state.selectedScriptId && it.type == ScriptType.BRIDGE }) {
                        BridgeControl(state.bridgeHorizontal, actions::toggleBridge)
                    }
                    if (state.selectedScriptId != null) SmallButton(stringResource(R.string.cancel), actions::cancelScript)
                    if (game.pingPreview.isNotEmpty()) PingPreview(game.pingPreview)
                    PanelHeading(stringResource(R.string.dominoes))
                    game.dominoHand.forEach { tile ->
                        DominoView(tile, state.selectedDominoId == tile.id, highlighted = state.tutorialStep == TutorialStep.SELECT || state.tutorialStep == TutorialStep.SPOOF) { actions.selectDomino(tile.id) }
                    }
                    game.dominoHand.find { it.id == state.selectedDominoId }?.let { tile ->
                        val preview = if (state.rotationSteps >= 2) tile.rotated() else tile
                        Text(stringResource(R.string.orientation_preview, stringResource(if (state.rotationSteps % 2 == 0) R.string.horizontal else R.string.vertical), preview.first, preview.second), color = Cyan)
                    }
                    HorizontalDivider(color = Cyan.copy(alpha = .2f))
                    Column {
                        SmallButton(stringResource(R.string.rotate), actions::rotate, Modifier.then(if (state.tutorialStep == TutorialStep.ROTATE) Modifier.border(2.dp, Warning) else Modifier))
                        Button(
                            onClick = actions::endTurn,
                            enabled = game.tilePlacedThisTurn,
                            modifier = Modifier.fillMaxWidth().then(if (state.tutorialStep == TutorialStep.END_TURN) Modifier.border(2.dp, Warning) else Modifier),
                            colors = ButtonDefaults.buttonColors(containerColor = Terminal, contentColor = Void),
                        ) { Text(stringResource(R.string.end_turn), fontSize = 10.sp) }
                    }
                }
            }
            }
        }
    }

    val selected = game.scriptHand.find { it.id == state.selectedScriptId }
    if (selected?.type == ScriptType.SPOOF && state.selectedDominoId != null) {
        game.dominoHand.find { it.id == state.selectedDominoId }?.let { tile ->
            SpoofDialog(tile, state.spoofHalf, state.spoofValue, state.message?.let { rejectionText(it) }, state.tutorialBlocked,
                actions::setSpoofHalf, actions::setSpoofValue, { actions.spoof(state.spoofValue) }, actions::cancelScript)
        }
    }
    if (showHelp) AlertDialog(
        onDismissRequest = { showHelp = false }, title = { Text(stringResource(R.string.help)) },
        text = { Column(Modifier.verticalScroll(rememberScrollState())) {
            ProtocolContent()
        } }, confirmButton = { TextButton(onClick = { showHelp = false }) { Text(stringResource(R.string.back)) } },
    )
    game.result?.let { ResultDialog(it, game.turn, game.trace, actions::retry, onMenu, state.tutorialStep == TutorialStep.COMPLETE) }
}

@Composable
private fun Board(
    board: BoardState,
    legalOrigins: Set<Position>,
    target: Position? = null,
    modifier: Modifier,
    onCell: (Position) -> Unit,
) {
    BoxWithConstraints(
        modifier.background(Brush.linearGradient(listOf(Panel, Void, Panel)), RoundedCornerShape(10.dp))
            .border(1.dp, Cyan.copy(alpha = .4f), RoundedCornerShape(10.dp)).padding(4.dp),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val inset = 6.dp.toPx()
            val rail = 15.dp.toPx()
            drawRect(Muted.copy(alpha = .25f), Offset(rail, rail), Size((size.width - rail * 2).coerceAtLeast(0f), (size.height - rail * 2).coerceAtLeast(0f)), style = Stroke(1.dp.toPx()))
            for (x in listOf(inset, size.width - inset)) for (y in listOf(inset, size.height - inset)) {
                drawCircle(Muted.copy(alpha = .7f), 3.dp.toPx(), Offset(x, y), style = Stroke(1.dp.toPx()))
                drawLine(Void, Offset(x - 2.dp.toPx(), y), Offset(x + 2.dp.toPx(), y), 1.dp.toPx())
            }
            repeat(12) { index ->
                val x = size.width * (index + 1) / 13
                drawLine(Terminal.copy(alpha = .4f), Offset(x, 0f), Offset(x, inset), 2.dp.toPx())
                drawLine(Cyan.copy(alpha = .3f), Offset(x, size.height - inset), Offset(x, size.height), 2.dp.toPx())
            }
        }
        val margin = if (maxHeight < 250.dp) 12.dp else 20.dp
        val cell = minOf((maxWidth - margin * 2) / BOARD_WIDTH, (maxHeight - margin * 2) / BOARD_HEIGHT).coerceAtLeast(0.dp)
        Box(Modifier.width(cell * BOARD_WIDTH).height(cell * BOARD_HEIGHT).background(Void).border(1.dp, Muted.copy(alpha = .4f))) {
            Canvas(Modifier.fillMaxSize()) {
                for (x in 0..BOARD_WIDTH) drawLine(Muted.copy(alpha = .18f), Offset(x * size.width / BOARD_WIDTH, 0f), Offset(x * size.width / BOARD_WIDTH, size.height))
                for (y in 0..BOARD_HEIGHT) drawLine(Muted.copy(alpha = .18f), Offset(0f, y * size.height / BOARD_HEIGHT), Offset(size.width, y * size.height / BOARD_HEIGHT))
            }
            for (y in 0 until BOARD_HEIGHT) for (x in 0 until BOARD_WIDTH) {
                val position = Position(x, y)
                BoardCell(board, position, cell, position in legalOrigins, position == target, onCell)
            }
        }
    }
}

@Composable
private fun BoardCell(board: BoardState, position: Position, size: Dp, legal: Boolean, target: Boolean, onCell: (Position) -> Unit) {
    val value = board.valueAt(position)
    val isStart = position == board.start
    val isExit = position == board.extraction
    val firewall = position in board.firewalls
    val trap = position in board.revealedHoneypots
    val daemon = board.daemon?.position == position
    val bridge = board.bridges.find { it.center == position }
    val description = listOfNotNull(
        if (isStart) stringResource(R.string.node_start) else null,
        if (isExit) stringResource(R.string.node_exit) else null,
        if (firewall) stringResource(R.string.node_firewall) else null,
        if (trap) stringResource(R.string.node_trap) else null,
        if (daemon) stringResource(R.string.node_daemon) else null,
        bridge?.let { stringResource(R.string.bridge_label, stringResource(if (it.horizontal) R.string.horizontal else R.string.vertical)) },
        value?.let { stringResource(R.string.node_value, it) },
        if (legal) stringResource(R.string.node_legal) else null,
        if (target) stringResource(R.string.node_target) else null,
    ).joinToString(", ").ifEmpty { stringResource(R.string.node_empty) }
    val label = stringResource(R.string.node_description, position.x + 1, position.y + 1, description)
    val placedColor = when {
        firewall -> Danger
        trap -> Warning
        isStart || isExit -> Cyan
        value != null -> Terminal
        legal -> Terminal.copy(alpha = .16f)
        else -> Color.Transparent
    }
    Box(
        Modifier
            .offset(x = size * position.x, y = size * position.y)
            .size(size)
            .padding(2.dp)
            .background(placedColor.copy(alpha = if (value != null || firewall || trap) .22f else placedColor.alpha))
            .then(if (target) Modifier.border(3.dp, Warning) else if (legal) Modifier.border(1.dp, Terminal) else Modifier)
            .clickable { onCell(position) }
            .semantics { contentDescription = label },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            when {
                bridge != null -> if (bridge.horizontal) "═" else "║"
                firewall -> "██"
                daemon -> "D!"
                isStart -> "S0"
                isExit -> "X6"
                trap -> "HP"
                value != null -> "$value"
                else -> "·"
            },
            color = placedColor.takeIf { it != Color.Transparent } ?: Muted.copy(alpha = .35f),
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Black,
        )
    }
}

@Composable
private fun ScriptCardView(card: ScriptCard, selected: Boolean, enabled: Boolean, highlighted: Boolean = false, onClick: () -> Unit) {
    Card(
        Modifier.fillMaxWidth().heightIn(min = 48.dp).semantics { this.selected = selected }.alpha(if (enabled) 1f else .4f).clickable(enabled = enabled, onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = if (selected) Cyan.copy(alpha = .22f) else Void),
        border = BorderStroke(1.dp, if (highlighted) Warning else if (selected) Cyan else Muted),
        shape = RoundedCornerShape(0.dp),
    ) {
        Row(Modifier.padding(7.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("> ${card.type.name}", color = Cyan, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Text(stringResource(R.string.noise, card.type.traceNoise), color = Muted, fontSize = 9.sp)
            }
            Text("${card.type.ramCost}R", color = Terminal, fontWeight = FontWeight.Black)
        }
    }
}

@Composable
private fun DominoView(tile: Domino, selected: Boolean, highlighted: Boolean = false, onClick: () -> Unit) {
    val label = stringResource(R.string.domino_description, tile.first, tile.second)
    Row(
        Modifier.fillMaxWidth().heightIn(min = 48.dp).semantics { contentDescription = label; this.selected = selected }.background(if (selected) Terminal.copy(alpha = .2f) else Void)
            .border(1.dp, if (highlighted) Warning else if (selected) Terminal else Muted).clickable(onClick = onClick).padding(6.dp),
        horizontalArrangement = Arrangement.Center,
    ) {
        Text("[ ${tile.first} │ ${tile.second} ]", color = if (selected) Terminal else Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun SettingsScreen(state: GameUiState, actions: MainViewModel, onBack: () -> Unit) {
    CircuitBackground {
        Column(Modifier.fillMaxSize().padding(36.dp).verticalScroll(rememberScrollState())) {
            Text(stringResource(R.string.settings), color = Terminal, fontSize = 32.sp, fontWeight = FontWeight.Black)
            Spacer(Modifier.height(20.dp))
            Text(stringResource(R.string.language), color = Cyan)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TerminalButton(stringResource(R.string.spanish), { actions.setLanguage("es") }, Modifier.weight(1f), state.preferences.language == "es")
                TerminalButton(stringResource(R.string.english), { actions.setLanguage("en") }, Modifier.weight(1f), state.preferences.language == "en")
            }
            SettingSwitch(stringResource(R.string.audio), state.preferences.audioEnabled, actions::setAudio)
            SettingSwitch(stringResource(R.string.vibration), state.preferences.vibrationEnabled, actions::setVibration)
            SettingSwitch(stringResource(R.string.reduced_motion), state.preferences.reducedMotion, actions::setReducedMotion)
            Spacer(Modifier.height(20.dp))
            SmallButton(stringResource(R.string.back), onBack)
        }
    }
}

@Composable
private fun SettingSwitch(label: String, value: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, Modifier.weight(1f), color = Color.White)
        Switch(checked = value, onCheckedChange = onChange)
    }
    HorizontalDivider(color = Muted.copy(alpha = .3f))
}

@Composable
private fun InfoScreen(onBack: () -> Unit) {
    CircuitBackground {
        Column(Modifier.fillMaxSize().padding(24.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.Center) {
            Text("// ${stringResource(R.string.help)}", color = Terminal, fontSize = 30.sp, fontWeight = FontWeight.Black)
            Spacer(Modifier.height(16.dp))
            ProtocolContent()
            Spacer(Modifier.height(22.dp))
            SmallButton(stringResource(R.string.back), onBack)
        }
    }
}

@Composable
private fun ResultDialog(result: GameResult, turns: Int, trace: Int, onRetry: () -> Unit, onMenu: () -> Unit, tutorialComplete: Boolean = false) {
    val victory = result == GameResult.VICTORY
    AlertDialog(
        onDismissRequest = {},
        title = { Text(stringResource(if (victory) R.string.victory else R.string.defeat), color = if (victory) Terminal else Danger) },
        text = { Text("${if (tutorialComplete) stringResource(R.string.lesson_complete) else stringResource(resultText(result))}\n${stringResource(R.string.turn, turns)} · ${stringResource(R.string.trace, trace)}") },
        confirmButton = { Button(onClick = onRetry) { Text(stringResource(R.string.play_again)) } },
        dismissButton = { TextButton(onClick = onMenu) { Text(stringResource(R.string.main_menu)) } },
    )
}

@Composable
private fun TerminalButton(label: String, onClick: () -> Unit, modifier: Modifier = Modifier, primary: Boolean = true) {
    Button(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(0.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (primary) Terminal else Panel,
            contentColor = if (primary) Void else Terminal,
        ),
        border = if (primary) null else BorderStroke(1.dp, Terminal),
    ) { Text(label, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold) }
}

@Composable
private fun SmallButton(label: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    OutlinedButton(onClick = onClick, modifier = modifier, shape = RoundedCornerShape(0.dp), border = BorderStroke(1.dp, Muted)) {
        Text(label, fontSize = 11.sp)
    }
}

@Composable
private fun StatusModule(label: String, tint: Color, detail: @Composable () -> Unit = {}) {
    Column(
        Modifier.background(Void.copy(alpha = .7f), RoundedCornerShape(4.dp)).padding(horizontal = 10.dp, vertical = 5.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(label, color = tint, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 12.sp)
        detail()
    }
}

@Composable
private fun PanelHeading(label: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Box(Modifier.size(3.dp, 14.dp).background(Cyan))
        Text(label, color = Cyan, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, fontSize = 12.sp)
        HorizontalDivider(Modifier.weight(1f), color = Cyan.copy(alpha = .25f))
    }
}

@Composable
private fun CircuitBackground(content: @Composable () -> Unit) {
    Box(Modifier.fillMaxSize().background(Brush.linearGradient(listOf(Void, Panel, Void)))) {
        Canvas(Modifier.fillMaxSize()) {
            val step = 48.dp.toPx()
            var x = -step
            var index = 0
            while (x < size.width) {
                val bend = size.height * (.2f + (index % 5) * .13f)
                val tint = if (index % 3 == 0) Cyan.copy(alpha = .09f) else Terminal.copy(alpha = .07f)
                drawLine(tint, Offset(x, 0f), Offset(x, bend), 1.dp.toPx())
                drawLine(tint, Offset(x, bend), Offset(x + step / 2, bend + step / 2), 1.dp.toPx())
                drawLine(tint, Offset(x + step / 2, bend + step / 2), Offset(x + step / 2, size.height), 1.dp.toPx())
                drawCircle(tint, 3.dp.toPx(), Offset(x, bend), style = Stroke(1.dp.toPx()))
                x += step
                index++
            }
        }
        content()
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

private fun resultText(result: GameResult): Int = when (result) {
    GameResult.VICTORY -> R.string.result_victory
    GameResult.TRACE_INTERCEPTED -> R.string.result_trace_intercepted
    GameResult.DAEMON_BREACH -> R.string.result_daemon_breach
    GameResult.KERNEL_PANIC -> R.string.result_kernel_panic
    GameResult.MEMORY_EXHAUSTED -> R.string.result_memory_exhausted
}

private fun lessonText(step: TutorialStep): Int = when (step) {
    TutorialStep.INTRO -> R.string.lesson_intro
    TutorialStep.SELECT -> R.string.lesson_select
    TutorialStep.ROTATE -> R.string.lesson_rotate
    TutorialStep.PLACE -> R.string.lesson_place
    TutorialStep.END_TURN -> R.string.lesson_end_turn
    TutorialStep.SYSTEM -> R.string.lesson_system
    TutorialStep.PING -> R.string.lesson_ping
    TutorialStep.PING_RESULT -> R.string.lesson_ping_result
    TutorialStep.SPOOF -> R.string.lesson_spoof
    TutorialStep.SPOOF_RESULT -> R.string.lesson_spoof_result
    TutorialStep.KILL -> R.string.lesson_kill
    TutorialStep.KILL_RESULT -> R.string.lesson_kill_result
    TutorialStep.BRIDGE -> R.string.lesson_bridge
    TutorialStep.BRIDGE_RESULT -> R.string.lesson_bridge_result
    TutorialStep.FINAL -> R.string.lesson_final
    TutorialStep.COMPLETE -> R.string.lesson_complete
}

@Composable
internal fun TutorialPanel(step: TutorialStep, blocked: Boolean, onContinue: () -> Unit, onRestart: () -> Unit) {
    Column(Modifier.fillMaxWidth().border(2.dp, Warning).padding(8.dp).semantics { liveRegion = LiveRegionMode.Polite }) {
        Text(stringResource(R.string.lesson_progress, step.ordinal + 1, TutorialStep.entries.size), color = Cyan)
        Text(stringResource(lessonText(step)), color = Color.White)
        if (blocked) Text(stringResource(R.string.follow_step), color = Warning)
        if (step.explanation) SmallButton(stringResource(R.string.continue_lesson), onContinue)
        SmallButton(stringResource(R.string.restart_lesson), onRestart)
    }
}

@Composable
internal fun PingPreview(tiles: List<Domino>) {
    Column {
        Text(stringResource(R.string.ping_preview), color = Cyan)
        tiles.forEach { Text("[ ${it.first} | ${it.second} ]", color = Terminal) }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun SpoofDialog(tile: Domino, half: Int, value: Int, error: Int?, blocked: Boolean,
    onHalf: (Int) -> Unit, onValue: (Int) -> Unit, onApply: () -> Unit, onCancel: () -> Unit) {
    AlertDialog(
        onDismissRequest = onCancel,
        title = { Text(stringResource(R.string.spoof_value)) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                Text("[ ${tile.first} | ${tile.second} ] → [ ${if (half == 0) value else tile.first} | ${if (half == 1) value else tile.second} ]")
                FlowRow {
                    listOf(R.string.first_half, R.string.second_half).forEachIndexed { index, label ->
                        OutlinedButton(onClick = { onHalf(index) }, modifier = Modifier.semantics { selected = half == index }) { Text(stringResource(label)) }
                    }
                }
                FlowRow {
                    (0..6).forEach { candidate ->
                        OutlinedButton(onClick = { onValue(candidate) }, modifier = Modifier.semantics { selected = value == candidate }) { Text("$candidate") }
                    }
                }
                error?.let { Text(stringResource(it), color = Danger) }
                if (blocked) Text(stringResource(R.string.lesson_spoof), color = Warning)
            }
        },
        confirmButton = { TextButton(onClick = onApply) { Text(stringResource(R.string.apply)) } },
        dismissButton = { TextButton(onClick = onCancel) { Text(stringResource(R.string.cancel)) } },
    )
}

@Composable
internal fun BridgeControl(horizontal: Boolean, onToggle: () -> Unit) {
    SmallButton(stringResource(R.string.bridge_label, stringResource(if (horizontal) R.string.horizontal else R.string.vertical)), onToggle)
}

@Composable
private fun ProtocolContent() {
    listOf(R.string.protocol_text, R.string.lesson_system, R.string.lesson_ping,
        R.string.lesson_spoof_result, R.string.lesson_kill_result, R.string.lesson_bridge_result,
        R.string.lesson_final).forEach { Text(stringResource(it), color = Color.White, modifier = Modifier.padding(bottom = 12.dp)) }
}
