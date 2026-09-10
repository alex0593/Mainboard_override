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
import androidx.compose.foundation.horizontalScroll
import androidx.compose.ui.platform.testTag
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
import androidx.compose.runtime.saveable.rememberSaveable
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.runtime.remember
import androidx.compose.runtime.LaunchedEffect
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
import com.aela.mainboardoverride.domain.BoardState
import com.aela.mainboardoverride.domain.Domino
import com.aela.mainboardoverride.domain.GameEngine
import com.aela.mainboardoverride.domain.GameResult
import com.aela.mainboardoverride.domain.MAX_RAM
import com.aela.mainboardoverride.domain.Orientation
import com.aela.mainboardoverride.domain.Position
import com.aela.mainboardoverride.domain.ScriptCard
import com.aela.mainboardoverride.domain.ScriptType
import com.aela.mainboardoverride.domain.ChallengeCatalog
import com.aela.mainboardoverride.domain.BoardBuff
import androidx.compose.animation.core.*
import kotlinx.coroutines.delay

private const val MENU = "menu"
private const val GAME = "game"
private const val SETTINGS = "settings"
private const val HELP = "help"
private const val SKINS = "skins"
private const val CHALLENGE = "challenge"

@Composable
fun MainboardApp(nav: NavHostController, state: GameUiState, actions: MainViewModel) {
    NavHost(navController = nav, startDestination = MENU) {
        composable(MENU) {
            MenuScreen(
                state = state,
                onNew = { nav.navigate("scenarios") },
                onChallenge = { nav.navigate(CHALLENGE) },
                onRetry = { actions.retryLast(); nav.navigate(GAME) },
                onSettings = { nav.navigate(SETTINGS) },
                onSkins = { nav.navigate(SKINS) },
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
        composable(SKINS) { SkinGallery(state, actions) { nav.popBackStack() } }
        composable("scenarios") { ScenarioScreen(state, actions, { nav.navigate(GAME) }, { nav.popBackStack() }) }
        composable(CHALLENGE) {
            ChallengeScreen(
                state = state,
                actions = actions,
                onStart = { nav.navigate(GAME) },
                onBack = { nav.popBackStack() },
            )
        }
        composable(HELP) { InfoScreen { nav.popBackStack() } }
    }
}

@Composable
private fun MenuScreen(
    state: GameUiState,
    onNew: () -> Unit,
    onChallenge: () -> Unit,
    onRetry: () -> Unit,
    onSettings: () -> Unit,
    onSkins: () -> Unit,
    onHelp: () -> Unit,
) {
    CircuitBackground(animated = !state.preferences.reducedMotion) {
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
                TerminalButton(stringResource(R.string.new_network), onNew)
                TerminalButton(stringResource(R.string.challenge), onChallenge, primary = false)
                if (state.preferences.lastSeed != null) TerminalButton(stringResource(R.string.retry_seed), onRetry, primary = false)
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    SmallButton(stringResource(R.string.settings), onSettings, Modifier.weight(1f))
                    SmallButton(stringResource(R.string.skins), onSkins, Modifier.weight(1f))
                    SmallButton(stringResource(R.string.help), onHelp, Modifier.weight(1.35f))
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
    val restartDescription = stringResource(R.string.restart_network)
    var helpOpen by rememberSaveable { mutableStateOf(false) }
    CircuitBackground {
        Column(Modifier.fillMaxSize().padding(8.dp)) {
            Row(
                Modifier.fillMaxWidth().heightIn(min = 64.dp).testTag("game-header")
                    .background(Panel.copy(alpha = .95f), RoundedCornerShape(8.dp))
                    .border(1.dp, Cyan.copy(alpha = .25f), RoundedCornerShape(8.dp)).padding(horizontal = 8.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextButton(onClick = onMenu, modifier = Modifier.width(48.dp).semantics { contentDescription = "Menu" }) { Text("‹", fontSize = 24.sp) }
                if (!state.tutorial && game.result == null) {
                    TextButton(
                        onClick = actions::restartNetwork,
                        modifier = Modifier.size(48.dp).testTag("restart-game")
                            .semantics { contentDescription = restartDescription },
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp),
                    ) {
                        Text("↻", color = Cyan, fontSize = 25.sp, fontFamily = FontFamily.Monospace)
                    }
                }
                if (game.result != null && state.reviewingBoard) {
                    TextButton(onClick = actions::showResult) { Text(stringResource(R.string.result_summary)) }
                    TextButton(onClick = actions::retry) { Text(stringResource(R.string.play_again)) }
                }
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(stringResource(R.string.turn, game.turn), color = Cyan, fontSize = 11.sp)
                    Text(stringResource(R.string.ram, game.ram, MAX_RAM), color = Terminal, fontSize = 11.sp)
                    Text(stringResource(R.string.trace, game.trace), color = if (game.trace >= 80) Danger else Warning, fontSize = 11.sp)
                }
            Row(
                Modifier.weight(1f).horizontalScroll(rememberScrollState()).testTag("script-hand"),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                game.scriptHand.forEach { card ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        ScriptCardView(
                            card,
                            state.selectedScriptId == card.id,
                            game.ram >= card.type.ramCost && game.result == null,
                            highlighted = state.tutorialStep?.let { step -> when (card.type) {
                                ScriptType.PING -> step == TutorialStep.PING
                                ScriptType.SPOOF -> step == TutorialStep.SPOOF
                                ScriptType.KILL_PROCESS -> step == TutorialStep.KILL
                                ScriptType.BRIDGE -> step == TutorialStep.BRIDGE
                            } } == true,
                            compact = true,
                        ) { actions.selectScript(card.id) }
                    }
                }
            }
                TextButton(onClick = { helpOpen = true }, modifier = Modifier.size(48.dp).testTag("general-help")) { Text("?", color = Cyan) }
            }
            Spacer(Modifier.height(8.dp))
            BoxWithConstraints(Modifier.weight(1f)) {
                val controlsWidth = (maxWidth * .29f).coerceIn(190.dp, 260.dp)
                Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Board(
                            board = game.board,
                            skin = state.preferences.boardSkin,
                            dominoSkin = state.preferences.dominoSkin,
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
                            animatePlacement = !state.preferences.reducedMotion,
                    )
                    if (!state.reviewingBoard) Box(
                        Modifier.width(controlsWidth).fillMaxHeight()
                            .background(Brush.verticalGradient(listOf(Panel, Void)), RoundedCornerShape(8.dp))
                            .border(1.dp, Cyan.copy(alpha = .3f), RoundedCornerShape(8.dp))
                            .padding(6.dp),
                    ) {
                        Column(Modifier.fillMaxSize().padding(bottom = 88.dp).verticalScroll(rememberScrollState()).testTag("hardware-panel"), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            state.tutorialStep?.let { step -> TutorialPanel(step, state.tutorialBlocked, actions::continueTutorial, actions::restartLesson) }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(stringResource(R.string.pending_noise, game.pendingNoise), Modifier.weight(1f), color = Warning, fontSize = 12.sp)
                            }
                            state.message?.let { Text(stringResource(rejectionText(it)), color = Danger, fontSize = 11.sp) }
                            state.lastBuff?.let { buff ->
                                Text(stringResource(if (buff == BoardBuff.TRACE_COOLER) R.string.buff_trace_collected else R.string.buff_ram_collected), color = Cyan, fontSize = 11.sp)
                            }
                            if (game.scriptHand.any { it.id == state.selectedScriptId && it.type == ScriptType.BRIDGE }) BridgeControl(state.bridgeHorizontal, actions::toggleBridge)
                            if (state.selectedScriptId != null) SmallButton(stringResource(R.string.cancel), actions::cancelScript)
                            if (game.pingPreview.isNotEmpty()) PingPreview(game.pingPreview)
                            Text(stringResource(R.string.dominoes), color = Cyan, fontSize = 11.sp)
                            FlowRow(
                                Modifier.fillMaxWidth(),
                                maxItemsInEachRow = 2,
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp),
                            ) {
                                game.dominoHand.forEach { tile ->
                                    DominoView(
                                        tile,
                                        state.selectedDominoId == tile.id,
                                        state.preferences.dominoSkin,
                                        highlighted = state.tutorialStep == TutorialStep.SELECT || state.tutorialStep == TutorialStep.SPOOF,
                                        modifier = Modifier.fillMaxWidth(.48f),
                                    ) { actions.selectDomino(tile.id) }
                                }
                            }
                            game.dominoHand.find { it.id == state.selectedDominoId }?.let { tile ->
                                val preview = if (state.rotationSteps >= 2) tile.rotated() else tile
                                val orientation = if (state.rotationSteps % 2 == 0) Orientation.HORIZONTAL else Orientation.VERTICAL
                                Text(stringResource(if (orientation == Orientation.HORIZONTAL) R.string.horizontal else R.string.vertical), color = Cyan)
                                DominoImage(preview, Modifier.align(Alignment.CenterHorizontally).width(if (orientation == Orientation.HORIZONTAL) 76.dp else 38.dp), orientation, skin = state.preferences.dominoSkin)
                            }
                        }
                        Box(
                            Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(84.dp).background(Panel),
                        ) {
                            Column(
                                Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 4.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp),
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    SmallButton(stringResource(R.string.rotate), actions::rotate, Modifier.weight(1f).height(36.dp).testTag("rotate").then(if (state.tutorialStep == TutorialStep.ROTATE) Modifier.border(2.dp, Warning) else Modifier))
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Button(
                                        onClick = actions::endTurn,
                                        enabled = game.tilePlacedThisTurn && game.result == null,
                                        modifier = Modifier.weight(1f).height(36.dp).testTag("end-turn").then(if (state.tutorialStep == TutorialStep.END_TURN || state.endTurnHint) Modifier.border(2.dp, Warning) else Modifier),
                                        colors = ButtonDefaults.buttonColors(containerColor = Terminal, contentColor = Void),
                                    ) { Text(stringResource(R.string.end_turn), fontSize = 10.sp) }
                                }
                            }
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
    if (helpOpen) GeneralGameHelp { helpOpen = false }
    if (!state.reviewingBoard) game.result?.let { MatchResultDialog(state, actions, onMenu) }
}

@Composable
internal fun Board(
    board: BoardState,
    skin: String = "pcb",
    dominoSkin: String = "kenney",
    legalOrigins: Set<Position>,
    target: Position? = null,
    modifier: Modifier,
    onCell: (Position) -> Unit,
    previewOnly: Boolean = false,
    animatePlacement: Boolean = true,
) {
    val placementKeys = remember(board.placed) {
        board.placed.map { placed ->
            "${placed.domino.id}:${placed.origin.x}:${placed.origin.y}:${placed.orientation}"
        }.toSet()
    }
    var knownPlacementKeys by remember { mutableStateOf<Set<String>?>(null) }
    var animatedPlacementKeys by remember { mutableStateOf(emptySet<String>()) }
    LaunchedEffect(placementKeys, animatePlacement, previewOnly) {
        if (!animatePlacement || previewOnly) {
            knownPlacementKeys = placementKeys
            animatedPlacementKeys = emptySet()
        } else {
            val previous = knownPlacementKeys
            knownPlacementKeys = placementKeys
            if (previous != null) {
                val added = placementKeys - previous
                if (added.isNotEmpty()) {
                    animatedPlacementKeys = animatedPlacementKeys + added
                    delay(230L)
                    animatedPlacementKeys = animatedPlacementKeys - added
                }
            }
        }
    }
    BoxWithConstraints(
        modifier.background(Brush.linearGradient(when (skin) {
            "copper" -> listOf(Color(0xFF40271B), Color(0xFF130E0B), Color(0xFF9D6336))
            "aurora" -> listOf(Color(0xFF301550), Color(0xFF100D26), Color(0xFF12646B))
            "blueprint" -> listOf(Color(0xFF102A4A), Color(0xFF07111F), Color(0xFF164D73))
            "industrial" -> listOf(Color(0xFF3A2914), Color(0xFF17120B), Color(0xFF60421A))
            "rust" -> listOf(Color(0xFF572616), Color(0xFF1C0E0A), Color(0xFF873D1E))
            "ice" -> listOf(Color(0xFF123B56), Color(0xFF071721), Color(0xFF2B7895))
            else -> listOf(Panel, Void, Panel)
        }), RoundedCornerShape(10.dp))
            .border(1.dp, Cyan.copy(alpha = .4f), RoundedCornerShape(10.dp)).padding(4.dp),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.fillMaxSize()) {
            if (skin == "copper" || skin == "aurora") {
                val tint = if (skin == "copper") Color(0xFFE6A466) else Color(0xFF8DEBF0)
                repeat(16) { index ->
                    val x = size.width * (index + 1) / 17
                    val bend = size.height * (.2f + (index % 4) * .17f)
                    drawLine(tint.copy(alpha = .35f), Offset(x, 0f), Offset(x, bend), 2.dp.toPx())
                    drawLine(tint.copy(alpha = .35f), Offset(x, bend), Offset(x + 12.dp.toPx(), bend + 12.dp.toPx()), 2.dp.toPx())
                    drawCircle(tint, 3.dp.toPx(), Offset(x + 12.dp.toPx(), bend + 12.dp.toPx()), style = Stroke(1.dp.toPx()))
                }
            }
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
        val cell = minOf((maxWidth - margin * 2) / board.width, (maxHeight - margin * 2) / board.height).coerceAtLeast(0.dp)
            Box(Modifier.width(cell * board.width).height(cell * board.height).background(when (skin) {
                "copper" -> Color(0xFF211710)
                "aurora" -> Color(0xFF1D1231)
                "blueprint" -> Color(0xFF0B1D31)
                "industrial" -> Color(0xFF241A0D)
                "rust" -> Color(0xFF24110C)
                "ice" -> Color(0xFF0A2635)
                else -> Void
            }).border(1.dp, Muted.copy(alpha = .4f))) {
            Canvas(Modifier.fillMaxSize()) {
                for (x in 0..board.width) drawLine(Muted.copy(alpha = .18f), Offset(x * size.width / board.width, 0f), Offset(x * size.width / board.width, size.height))
                for (y in 0..board.height) drawLine(Muted.copy(alpha = .18f), Offset(0f, y * size.height / board.height), Offset(size.width, y * size.height / board.height))
            }
            // Sprites sit below the cell hit targets and threat badges.
            board.placed.forEach { placed ->
                val horizontal = placed.orientation == Orientation.HORIZONTAL
                val placementKey = "${placed.domino.id}:${placed.origin.x}:${placed.origin.y}:${placed.orientation}"
                val animating = placementKey in animatedPlacementKeys
                val scale = remember(placementKey) { Animatable(1f) }
                LaunchedEffect(placementKey, animating) {
                    if (animating) {
                        scale.snapTo(.86f)
                        scale.animateTo(1.04f, tween(110, easing = FastOutSlowInEasing))
                        scale.animateTo(1f, tween(100, easing = FastOutSlowInEasing))
                    } else {
                        scale.snapTo(1f)
                    }
                }
                DominoImage(
                    placed.domino,
                    Modifier.offset(cell * placed.origin.x, cell * placed.origin.y)
                        .size(cell * (if (horizontal) 2 else 1), cell * (if (horizontal) 1 else 2))
                        .padding(2.dp)
                        .graphicsLayer {
                            scaleX = scale.value
                            scaleY = scale.value
                        }
                        .drawBehind {
                            if (animating) drawCircle(Cyan.copy(alpha = .22f), radius = size.minDimension * .42f)
                        },
                    placed.orientation,
                    describe = false,
                    skin = dominoSkin,
                )
            }
            if (previewOnly) {
                val ink = remember { android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
                    textAlign = android.graphics.Paint.Align.CENTER
                    typeface = android.graphics.Typeface.MONOSPACE
                } }
                Canvas(Modifier.fillMaxSize()) {
                    val unit = size.width / board.width
                    ink.textSize = unit * .5f
                    ink.color = android.graphics.Color.rgb(57, 231, 224)
                    for (y in 0 until board.height) for (x in 0 until board.width) {
                        val p = Position(x, y)
                        val label = when {
                            p == board.start -> "S0"
                            p == board.extraction -> "X6"
                            p in board.firewalls || p in board.revealedHoneypots -> null
                            p in board.buffs -> if (board.buffs[p] == BoardBuff.TRACE_COOLER) "−8" else "+R"
                            else -> null
                        }
                        if (label != null) drawContext.canvas.nativeCanvas.drawText(label, (x + .5f) * unit, (y + .65f) * unit, ink)
                    }
                }
                (board.firewalls + board.revealedHoneypots).forEach { position ->
                    val isPlaced = board.placed.any { it.valueAt(position) != null }
                    Box(Modifier.offset(cell * position.x, cell * position.y).size(cell).padding(2.dp)) {
                        BoardThreatImage(
                            firewall = position in board.firewalls,
                            modifier = if (isPlaced) Modifier.align(Alignment.TopEnd).size(cell * .4f)
                                .background(Void.copy(alpha = .9f), RoundedCornerShape(2.dp))
                            else Modifier.fillMaxSize().padding(1.dp),
                        )
                        board.bridges.find { it.center == position }?.let { bridge ->
                            Text(
                                if (bridge.horizontal) "═" else "║",
                                modifier = Modifier.align(Alignment.Center).background(Void.copy(alpha = .85f)),
                                color = Warning,
                                fontWeight = FontWeight.Black,
                            )
                        }
                    }
                }
            } else for (y in 0 until board.height) for (x in 0 until board.width) {
                val position = Position(x, y)
                BoardCell(board, position, cell, position in legalOrigins, position == target, onCell)
            }
        }
    }
}

@Composable
private fun BoardCell(board: BoardState, position: Position, size: Dp, legal: Boolean, target: Boolean, onCell: (Position) -> Unit) {
    val value = board.valueAt(position)
    val isPlaced = board.placed.any { it.valueAt(position) != null }
    val isStart = position == board.start
    val isExit = position == board.extraction
    val firewall = position in board.firewalls
    val trap = position in board.revealedHoneypots
    val buff = board.buffs[position]?.takeUnless { position in board.collectedBuffs }
    val daemon = board.daemon?.position == position
    val bridge = board.bridges.find { it.center == position }
    val description = listOfNotNull(
        if (isStart) stringResource(R.string.node_start) else null,
        if (isExit) stringResource(R.string.node_exit) else null,
        if (firewall) stringResource(R.string.node_firewall) else null,
        if (trap) stringResource(R.string.node_trap) else null,
        buff?.let { stringResource(if (it == BoardBuff.TRACE_COOLER) R.string.node_trace_cooler else R.string.node_ram_reserve) },
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
        buff != null -> Cyan
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
            .background(if (isPlaced) Color.Transparent else placedColor.copy(alpha = if (value != null || firewall || trap || buff != null) .22f else placedColor.alpha))
            .then(if (target) Modifier.border(3.dp, Warning) else if (legal) Modifier.border(1.dp, Terminal) else Modifier)
            .clickable { onCell(position) }
            .semantics { contentDescription = label },
        contentAlignment = Alignment.Center,
    ) {
        if (isPlaced) {
            if (trap || daemon) {
                Row(Modifier.align(Alignment.TopEnd).background(Void.copy(alpha = .9f), RoundedCornerShape(2.dp))) {
                    if (trap) BoardThreatImage(firewall = false, modifier = Modifier.size(size * .4f))
                    if (daemon) Text("D!", color = Danger, fontSize = 9.sp)
                }
            }
        } else if (firewall || (trap && !daemon && !isStart && !isExit)) {
            BoardThreatImage(firewall, Modifier.fillMaxSize().padding(1.dp))
            if (bridge != null) Text(
                if (bridge.horizontal) "═" else "║",
                modifier = Modifier.background(Void.copy(alpha = .85f)),
                color = Warning,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Black,
            )
        } else Text(
            when {
                bridge != null -> if (bridge.horizontal) "═" else "║"
                daemon -> "D!"
                isStart -> "S0"
                isExit -> "X6"
                buff != null -> if (buff == BoardBuff.TRACE_COOLER) "−8" else "+1R"
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
private fun ScriptCardView(card: ScriptCard, selected: Boolean, enabled: Boolean, highlighted: Boolean = false, compact: Boolean = false, onClick: () -> Unit) {
    Card(
        (if (compact) Modifier.width(88.dp) else Modifier.fillMaxWidth()).heightIn(min = 48.dp).testTag("script-${card.id}").semantics { this.selected = selected }.alpha(if (enabled) 1f else .4f).clickable(enabled = enabled, onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = if (selected) Cyan.copy(alpha = .22f) else Void),
        border = BorderStroke(1.dp, if (highlighted) Warning else if (selected) Cyan else Muted),
        shape = RoundedCornerShape(0.dp),
    ) {
        Column(Modifier.padding(4.dp)) {
            Column {
                Text(if (card.type == ScriptType.KILL_PROCESS) "KILL" else card.type.name, color = Cyan, fontSize = 10.sp, fontWeight = FontWeight.Bold, maxLines = 1)
            }
            Text("${card.type.ramCost}R · +${card.type.traceNoise}", color = Terminal, fontSize = 10.sp)
        }
    }
}

@Composable
private fun DominoView(tile: Domino, selected: Boolean, skin: String = "kenney", highlighted: Boolean = false, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val label = stringResource(R.string.domino_description, tile.first, tile.second)
    Row(
        modifier.heightIn(min = 48.dp).semantics { contentDescription = label; this.selected = selected }.background(if (selected) Terminal.copy(alpha = .2f) else Void)
            .border(1.dp, if (highlighted) Warning else if (selected) Terminal else Muted).clickable(onClick = onClick).padding(3.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
            DominoImage(tile, Modifier.width(60.dp), describe = false, skin = skin)
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
            SettingSwitch(stringResource(R.string.context_help), state.preferences.contextHelpEnabled, actions::setContextHelpEnabled)
            Spacer(Modifier.height(20.dp))
            SmallButton(stringResource(R.string.back), onBack)
        }
    }
}

@Composable
private fun ChallengeScreen(state: GameUiState, actions: MainViewModel, onStart: () -> Unit, onBack: () -> Unit) {
    CircuitBackground {
        Column(Modifier.fillMaxSize().padding(horizontal = 10.dp, vertical = 4.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            ProgressionHeader(stringResource(R.string.challenge), onBack) {
                Text("${state.preferences.challengeBest.size}/${ChallengeCatalog.COUNT}", color = Warning, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
            }
            Text(stringResource(R.string.challenge_description), color = Color.White, modifier = Modifier.padding(horizontal = 4.dp))
            Text(stringResource(R.string.challenge_progress, state.preferences.challengeBest.size, ChallengeCatalog.COUNT), color = Cyan, modifier = Modifier.padding(horizontal = 4.dp))
            LazyVerticalGrid(
                columns = GridCells.Adaptive(220.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f),
            ) {
                items(ChallengeCatalog.levels, key = { it.number }) { challenge ->
                    val level = challenge.number
                    val unlocked = level <= state.preferences.challengeUnlocked
                    val record = state.preferences.challengeBest[level]
                    val rules = challenge.rules
                    val maxTurns = rules.maxTurns
                    val maxTrace = rules.maxTrace
                    val ruleText = when {
                        maxTurns != null && maxTrace != null -> stringResource(R.string.challenge_rules_both, maxTurns, maxTrace)
                        maxTurns != null -> stringResource(R.string.challenge_rules_turns, maxTurns)
                        else -> stringResource(R.string.challenge_rules_trace, maxTrace ?: 0)
                    }
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = if (unlocked) Panel.copy(alpha = .94f) else Void.copy(alpha = .72f)),
                        border = BorderStroke(1.dp, if (record != null) Terminal.copy(alpha = .8f) else if (unlocked) Cyan.copy(alpha = .35f) else Muted.copy(alpha = .35f)),
                        shape = RoundedCornerShape(8.dp),
                    ) {
                        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("%02d".format(level), color = if (unlocked) Terminal else Muted, fontSize = 26.sp, fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace)
                                Spacer(Modifier.width(8.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(stringResource(R.string.challenge_level, level), color = if (unlocked) Cyan else Muted, fontWeight = FontWeight.Bold)
                                    Text("DIFFICULTY ${challenge.difficulty}", color = if (unlocked) Warning else Muted, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                                }
                                Text(if (record != null) "✓" else if (unlocked) "•" else "▣", color = if (record != null) Terminal else if (unlocked) Cyan else Muted, fontSize = 20.sp)
                            }
                            Box(Modifier.fillMaxWidth().height(3.dp).background(if (record != null) Terminal else if (unlocked) Cyan.copy(alpha = .35f) else Muted.copy(alpha = .25f), RoundedCornerShape(2.dp)))
                            Text(ruleText, color = if (unlocked) Warning else Muted, fontFamily = FontFamily.Monospace, fontSize = 10.sp)
                            Text(
                                record?.let { "BEST  ${it.turns}T / ${it.trace}%" } ?: stringResource(R.string.expected_reward, com.aela.mainboardoverride.domain.Rewards.VICTORY + if (record == null) com.aela.mainboardoverride.domain.Rewards.FIRST_CHALLENGE else 0),
                                color = if (record != null) Terminal else Warning,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 10.sp,
                            )
                            Button(
                                enabled = unlocked,
                                onClick = { actions.startChallenge(level); onStart() },
                                modifier = Modifier.fillMaxWidth().testTag("challenge-$level"),
                                colors = ButtonDefaults.buttonColors(containerColor = if (unlocked) Terminal else Muted.copy(alpha = .25f), contentColor = if (unlocked) Void else Muted),
                            ) {
                                Text(stringResource(if (unlocked) R.string.play_scenario else R.string.locked_scenario), fontSize = 11.sp)
                            }
                        }
                    }
                }
            }
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
private fun TerminalButton(label: String, onClick: () -> Unit, modifier: Modifier = Modifier, primary: Boolean = true, enabled: Boolean = true) {
    Button(
        onClick = onClick,
        enabled = enabled,
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
        Text(label, fontSize = 11.sp, maxLines = 1, softWrap = false)
    }
}

@Composable
private fun StatusModule(label: String, tint: Color, topic: HelpTopic, onHelp: (HelpTopic) -> Unit, visible: Boolean = true, detail: @Composable () -> Unit = {}) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(
            Modifier.background(Void.copy(alpha = .7f), RoundedCornerShape(4.dp)).padding(horizontal = 10.dp, vertical = 5.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(label, color = tint, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 12.sp)
            detail()
        }
        HelpButton(topic, onHelp, visible)
    }
}

@Composable
private fun PanelHeading(label: String, topic: HelpTopic, onHelp: (HelpTopic) -> Unit, visible: Boolean = true) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Box(Modifier.size(3.dp, 14.dp).background(Cyan))
        Text(label, color = Cyan, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, fontSize = 12.sp)
        HorizontalDivider(Modifier.weight(1f), color = Cyan.copy(alpha = .25f))
        HelpButton(topic, onHelp, visible)
    }
}

@Composable
internal fun CircuitBackground(animated: Boolean = false, content: @Composable () -> Unit) {
    val progress = if (animated) {
        val transition = rememberInfiniteTransition(label = "menu circuits")
        val phase by transition.animateFloat(0f, 1f, infiniteRepeatable(tween(6000, easing = LinearEasing)), label = "pulse")
        phase
    } else 0f
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
                if (animated) {
                    val distance = ((progress + index * .13f) % 1f) * (size.height + step / 2)
                    val point = when {
                        distance < bend -> Offset(x, distance)
                        distance < bend + step / 2 -> Offset(x + distance - bend, distance)
                        else -> Offset(x + step / 2, distance)
                    }
                    drawCircle(Cyan.copy(alpha = .12f), 8.dp.toPx(), point)
                    drawCircle(Cyan.copy(alpha = .6f), 2.dp.toPx(), point)
                }
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

internal fun resultText(result: GameResult): Int = when (result) {
    GameResult.VICTORY -> R.string.result_victory
    GameResult.TRACE_INTERCEPTED -> R.string.result_trace_intercepted
    GameResult.DAEMON_BREACH -> R.string.result_daemon_breach
    GameResult.KERNEL_PANIC -> R.string.result_kernel_panic
    GameResult.MEMORY_EXHAUSTED -> R.string.result_memory_exhausted
    GameResult.CHALLENGE_LIMIT -> R.string.result_challenge_limit
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
        tiles.forEach { DominoImage(it, Modifier.width(96.dp).padding(vertical = 3.dp)) }
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
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Column(Modifier.weight(1f)) {
                        Text(stringResource(R.string.spoof_before))
                        DominoImage(tile, Modifier.widthIn(max = 112.dp).fillMaxWidth())
                    }
                    Column(Modifier.weight(1f)) {
                        Text(stringResource(R.string.spoof_after))
                        DominoImage(if (half == 0) tile.copy(first = value) else tile.copy(second = value), Modifier.widthIn(max = 112.dp).fillMaxWidth())
                    }
                }
                FlowRow {
                    listOf(R.string.first_half, R.string.second_half).forEachIndexed { index, label ->
                        OutlinedButton(onClick = { onHalf(index) }, modifier = Modifier.semantics { selected = half == index }) { Text(stringResource(label)) }
                    }
                }
                Text(stringResource(R.string.spoof_scroll_hint), color = Cyan, fontSize = 11.sp)
                val valueList = rememberLazyListState(initialFirstVisibleItemIndex = value.coerceIn(0, 6))
                LazyColumn(
                    state = valueList,
                    modifier = Modifier.fillMaxWidth().height(196.dp).border(1.dp, Cyan.copy(alpha = .35f)),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    items((0..6).toList()) { candidate ->
                        TextButton(
                            onClick = { onValue(candidate) },
                            modifier = Modifier.fillMaxWidth().height(28.dp).semantics { selected = value == candidate },
                        ) { Text("$candidate", color = if (value == candidate) Terminal else Muted, fontSize = if (value == candidate) 22.sp else 14.sp) }
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
    Card(colors = CardDefaults.cardColors(containerColor = Color(0xFFE7FFF2)), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            HelpTopic.entries.forEach { topic ->
                Text(stringResource(topic.title), color = Color.Black, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 12.dp, bottom = 4.dp))
                HelpBody(topic, darkText = true)
            }
        }
    }
}
