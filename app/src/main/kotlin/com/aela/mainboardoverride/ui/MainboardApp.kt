package com.aela.mainboardoverride.ui

import androidx.activity.compose.BackHandler
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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.LiveRegionMode
import com.aela.mainboardoverride.domain.RejectReason
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.zIndex
import androidx.compose.runtime.remember
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.painterResource
import androidx.compose.foundation.Image
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

private enum class GameExitAction { EXIT, RESTART }

@Composable
fun MainboardApp(
    nav: NavHostController,
    state: GameUiState,
    actions: MainViewModel,
    onExitApp: () -> Unit = {},
) {
    val transition = LocalWindowTransition.current
    NavHost(navController = nav, startDestination = MENU) {
        composable(MENU) {
            MenuScreen(
                state = state,
                onNew = { transition { nav.navigate("scenarios") } },
                onChallenge = { transition { nav.navigate(CHALLENGE) } },
                onRetry = { transition { actions.retryLast(); nav.navigate(GAME) } },
                onSettings = { transition { nav.navigate(SETTINGS) } },
                onSkins = { transition { nav.navigate(SKINS) } },
                onHelp = { transition { nav.navigate(HELP) } },
                onExitApp = onExitApp,
            )
        }
        composable(GAME) {
            GameScreen(
                state = state,
                actions = actions,
                onMenu = { transition { nav.popBackStack(MENU, inclusive = false) } },
            )
        }
        composable(SETTINGS) { SettingsScreen(state, actions) { transition { nav.popBackStack() } } }
        composable(SKINS) { SkinGallery(state, actions) { transition { nav.popBackStack() } } }
        composable("scenarios") { ScenarioScreen(state, actions, { nav.navigate(GAME) }, { transition { nav.popBackStack() } }) }
        composable(CHALLENGE) {
            ChallengeScreen(
                state = state,
                actions = actions,
                onStart = { nav.navigate(GAME) },
                onBack = { transition { nav.popBackStack() } },
            )
        }
        composable(HELP) { InfoScreen { transition { nav.popBackStack() } } }
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
    onExitApp: () -> Unit,
) {
    var exitRequested by rememberSaveable { mutableStateOf(false) }
    BackHandler(enabled = !exitRequested) { exitRequested = true }
    MenuArtworkBackground(reducedMotion = state.preferences.reducedMotion) {
        Row(
            Modifier.fillMaxSize().padding(horizontal = 24.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
                Image(
                    painterResource(R.drawable.menu_module_v1), contentDescription = null,
                    modifier = Modifier.size(88.dp).padding(bottom = 8.dp),
                )
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
                MenuArtworkButton(stringResource(R.string.new_network), onNew, primary = true)
                MenuArtworkButton(stringResource(R.string.challenge), onChallenge)
                if (state.preferences.lastSeed != null) MenuArtworkButton(stringResource(R.string.retry_seed), onRetry)
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    MenuArtworkButton(stringResource(R.string.settings), onSettings, Modifier.weight(1f), compact = true)
                    MenuArtworkButton(stringResource(R.string.skins), onSkins, Modifier.weight(1f), compact = true)
                    MenuArtworkButton(stringResource(R.string.help), onHelp, Modifier.weight(1.35f), compact = true)
                }
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
    BackHandler(enabled = pendingExitAction == null) { pendingExitAction = GameExitAction.EXIT }
    CircuitBackground {
        Column(Modifier.fillMaxSize().padding(8.dp)) {
            Box(Modifier.fillMaxWidth().heightIn(min = 64.dp).testTag("game-header").clip(RoundedCornerShape(10.dp))) {
                Image(
                    painterResource(R.drawable.menu_header_v1), contentDescription = null,
                    modifier = Modifier.matchParentSize(), contentScale = androidx.compose.ui.layout.ContentScale.FillBounds,
                )
                Row(
                    Modifier.fillMaxWidth().heightIn(min = 64.dp).padding(horizontal = 16.dp, vertical = 9.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                LevelActionButton(stringResource(R.string.exit_match), R.drawable.ic_leave_level,
                    { pendingExitAction = GameExitAction.EXIT }, Modifier.align(Alignment.CenterVertically).testTag("exit-game"))
                LevelActionButton(stringResource(R.string.retry_level), R.drawable.ic_retry_level,
                    { pendingExitAction = GameExitAction.RESTART }, Modifier.align(Alignment.CenterVertically).testTag("restart-game"))
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(stringResource(R.string.turn, game.turn), color = Cyan, fontSize = 11.sp)
                    Text(stringResource(R.string.ram, game.ram, MAX_RAM), color = Terminal, fontSize = 11.sp)
                    Text(stringResource(R.string.trace, game.trace), color = if (game.trace >= 80) Danger else Warning, fontSize = 11.sp)
                }
                FlowRow(
                    Modifier.weight(1f).testTag("script-hand"),
                    maxItemsInEachRow = 4,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
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
                    TextButton(onClick = { helpOpen = true }, modifier = Modifier.size(46.dp).testTag("general-help")) { Text("?", color = Cyan) }
                }
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
                            target = null,
                            modifier = Modifier.weight(1f).fillMaxHeight(),
                            onCell = actions::placeAt,
                            animatePlacement = !state.preferences.reducedMotion,
                    )
                    if (state.reviewingBoard) ReviewPanel(controlsWidth, actions)
                    if (!state.reviewingBoard) Box(
                        Modifier.width(controlsWidth).fillMaxHeight()
                            .clip(RoundedCornerShape(10.dp))
                            .border(1.dp, Cyan.copy(alpha = .3f), RoundedCornerShape(10.dp))
                            .padding(6.dp),
                    ) {
                        Image(
                            painterResource(R.drawable.menu_card_v1), contentDescription = null,
                            modifier = Modifier.matchParentSize(), contentScale = androidx.compose.ui.layout.ContentScale.FillBounds,
                        )
                        Column(Modifier.fillMaxSize().padding(bottom = 88.dp).verticalScroll(rememberScrollState()).testTag("hardware-panel"), verticalArrangement = Arrangement.spacedBy(4.dp)) {
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
                                        highlighted = false,
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
                            Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(84.dp).background(Panel.copy(alpha = .9f)),
                        ) {
                            Column(
                                Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp),
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    SmallButton(
                                        stringResource(R.string.rotate), actions::rotate,
                                        Modifier.weight(1f).height(40.dp).testTag("rotate"),
                                        minHeight = 40.dp,
                                    )
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    MenuArtworkButton(
                                        label = stringResource(R.string.end_turn),
                                        onClick = actions::endTurn,
                                        primary = true,
                                        compact = true,
                                        enabled = game.tilePlacedThisTurn && game.result == null,
                                        minHeight = 40.dp,
                                        modifier = Modifier.weight(1f).height(40.dp).testTag("end-turn")
                                            .then(if (state.endTurnHint) Modifier.border(2.dp, Warning) else Modifier),
                                    )
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
            SpoofDialog(tile, state.spoofHalf, state.spoofValue, state.message?.let { rejectionText(it) },
                actions::setSpoofHalf, actions::setSpoofValue, { actions.spoof(state.spoofValue) }, actions::cancelScript)
        }
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
    val boardArtwork = boardSkinResource(skin, previewOnly)
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
        modifier.background(androidx.compose.ui.graphics.SolidColor(Void), RoundedCornerShape(10.dp))
            .border(1.dp, Cyan.copy(alpha = .4f), RoundedCornerShape(10.dp)).padding(4.dp),
        contentAlignment = Alignment.Center,
    ) {
        if (boardArtwork != null) {
            Image(
                painterResource(boardArtwork), contentDescription = null,
                modifier = Modifier.matchParentSize(),
                contentScale = androidx.compose.ui.layout.ContentScale.FillBounds,
            )
        } else Canvas(Modifier.fillMaxSize()) {
            // Generic fallback only; every built-in skin uses a pre-rendered asset.
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
        val margin = if (boardArtwork != null) maxOf(12.dp, minOf(maxWidth, maxHeight) * .1f)
            else if (maxHeight < 250.dp) 12.dp else 20.dp
        val cell = minOf((maxWidth - margin * 2) / board.width, (maxHeight - margin * 2) / board.height).coerceAtLeast(0.dp)
            Box(Modifier.width(cell * board.width).height(cell * board.height)
                .background(Color.Transparent)
                .border(1.dp, Muted.copy(alpha = .4f))) {
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
                    previewOnly = previewOnly,
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
                                if (bridge.horizontal) "═${bridge.value}" else "║${bridge.value}",
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
        bridge?.let { stringResource(R.string.bridge_label, stringResource(if (it.horizontal) R.string.horizontal else R.string.vertical)) + " ${it.value}" },
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
                if (bridge.horizontal) "═${bridge.value}" else "║${bridge.value}",
                modifier = Modifier.background(Void.copy(alpha = .85f)),
                color = Warning,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Black,
            )
        } else Text(
            when {
                bridge != null -> if (bridge.horizontal) "═${bridge.value}" else "║${bridge.value}"
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

@Composable
private fun ScriptCardView(card: ScriptCard, selected: Boolean, enabled: Boolean, highlighted: Boolean = false, compact: Boolean = false, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Box(
        modifier.then((if (compact) Modifier.width(88.dp) else Modifier.fillMaxWidth())
            .heightIn(min = 48.dp).clip(RoundedCornerShape(8.dp)).testTag("script-${card.id}")
            .semantics { this.selected = selected }.alpha(if (enabled) 1f else .4f)
            .clickable(enabled = enabled, onClick = onClick)
            .border(1.dp, if (highlighted) Warning else if (selected) Cyan else Muted, RoundedCornerShape(8.dp))),
    ) {
        Image(
            painter = painterResource(R.drawable.menu_card_v1),
            contentDescription = null,
            modifier = Modifier.matchParentSize(),
            contentScale = androidx.compose.ui.layout.ContentScale.FillBounds,
        )
        if (selected) Box(Modifier.matchParentSize().background(Cyan.copy(alpha = .2f)))
        Row(Modifier.padding(4.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            scriptArtwork(card.type)?.let { artwork ->
                Image(
                    painter = painterResource(artwork),
                    contentDescription = null,
                    modifier = Modifier.size(if (compact) 26.dp else 34.dp),
                )
            }
            Column {
                Text(if (card.type == ScriptType.KILL_PROCESS) "KILL" else card.type.name, color = Cyan, fontSize = 10.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                Text("${card.type.ramCost}R · +${card.type.traceNoise}", color = Terminal, fontSize = 10.sp)
            }
        }
    }
}

private fun scriptArtwork(type: ScriptType): Int? = when (type) {
    ScriptType.PING -> R.drawable.script_card_ping
    ScriptType.SPOOF -> R.drawable.script_card_spoof
    ScriptType.KILL_PROCESS -> R.drawable.script_card_kill
    ScriptType.BRIDGE -> R.drawable.script_card_bridge
    else -> null
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
    val transition = LocalWindowTransition.current
    val previews by produceState<Map<Int, BoardState>>(emptyMap()) {
        ChallengeCatalog.levels.forEach { challenge ->
            val board = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Default) {
                com.aela.mainboardoverride.domain.LevelGenerator.generateChallenge(challenge.number, challenge.seed).board
            }
            value = value + (challenge.number to board)
        }
    }
    CircuitBackground {
        Column(Modifier.fillMaxSize().padding(horizontal = 10.dp, vertical = 4.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            ProgressionHeader(
                title = stringResource(R.string.challenge),
                onBack = onBack,
                counter = "${state.preferences.challengeBest.size}/${ChallengeCatalog.COUNT}",
            )
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
                    val preview = previews[level]
                    val ruleText = when {
                        maxTurns != null && maxTrace != null -> stringResource(R.string.challenge_rules_both, maxTurns, maxTrace)
                        maxTurns != null -> stringResource(R.string.challenge_rules_turns, maxTurns)
                        else -> stringResource(R.string.challenge_rules_trace, maxTrace ?: 0)
                    }
                    Card(
                        modifier = Modifier.fillMaxWidth().height(ProgressionCardHeight),
                        colors = CardDefaults.cardColors(containerColor = if (unlocked) Panel.copy(alpha = .94f) else Void.copy(alpha = .72f)),
                        border = BorderStroke(1.dp, if (record != null) Terminal.copy(alpha = .8f) else if (unlocked) Cyan.copy(alpha = .35f) else Muted.copy(alpha = .35f)),
                        shape = RoundedCornerShape(8.dp),
                    ) {
                        Column(Modifier.fillMaxSize().padding(12.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                            if (preview != null) {
                                Board(preview, state.preferences.boardSkin, state.preferences.dominoSkin, emptySet(), modifier = Modifier.fillMaxWidth().height(110.dp), onCell = {}, previewOnly = true)
                            } else {
                                Box(Modifier.fillMaxWidth().height(110.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator(Modifier.size(24.dp)) }
                            }
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
                            Spacer(Modifier.weight(1f))
                            Button(
                                enabled = unlocked,
                                onClick = { transition { actions.startChallenge(level); onStart() } },
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
private fun StatusBox(label: String, value: String, color: Color, wide: Boolean = false) {
    Column(
        Modifier.width(if (wide) 88.dp else 64.dp).height(42.dp)
            .background(Void.copy(alpha = .65f), RoundedCornerShape(4.dp))
            .border(1.dp, color.copy(alpha = .38f), RoundedCornerShape(4.dp))
            .padding(horizontal = 5.dp, vertical = 3.dp),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, color = Muted, fontSize = 8.sp, fontFamily = FontFamily.Monospace, maxLines = 1)
        Text(value, color = color, fontSize = if (wide) 9.sp else 13.sp, fontFamily = FontFamily.Monospace, maxLines = 1)
    }
}

@Composable
private fun TerminalButton(label: String, onClick: () -> Unit, modifier: Modifier = Modifier, primary: Boolean = true, enabled: Boolean = true) {
    MenuArtworkButton(label, onClick, modifier = modifier, primary = primary, enabled = enabled)
}

@Composable
private fun LevelActionButton(label: String, icon: Int, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(modifier.size(49.dp).clip(RoundedCornerShape(10.dp)).semantics { contentDescription = label }) {
        Image(painterResource(R.drawable.menu_button_compact_v2), contentDescription = null, modifier = Modifier.matchParentSize(), contentScale = androidx.compose.ui.layout.ContentScale.FillBounds)
        androidx.compose.material3.IconButton(onClick = onClick, modifier = Modifier.matchParentSize()) {
            androidx.compose.material3.Icon(painterResource(icon), contentDescription = null, tint = Cyan, modifier = Modifier.size(24.dp))
        }
    }
}

@Composable
private fun SmallButton(label: String, onClick: () -> Unit, modifier: Modifier = Modifier, minHeight: Dp? = null) {
    MenuArtworkButton(label, onClick, modifier = modifier, compact = true, minHeight = minHeight)
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
        transition.animateFloat(0f, 1f, infiniteRepeatable(tween(6000, easing = LinearEasing)), label = "pulse")
    } else null
    Box(Modifier.fillMaxSize().background(Brush.linearGradient(listOf(Void, Panel, Void)))) {
        Image(
            painterResource(R.drawable.menu_background_v2),
            contentDescription = null,
            modifier = Modifier.matchParentSize().alpha(.72f),
            contentScale = androidx.compose.ui.layout.ContentScale.Crop,
        )
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
                    // Read the animation in the draw phase, without recomposing menu content each frame.
                    val distance = (((progress?.value ?: 0f) + index * .13f) % 1f) * (size.height + step / 2)
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

@Composable
internal fun PingPreview(tiles: List<Domino>) {
    Column {
        Text(stringResource(R.string.ping_preview), color = Cyan)
        tiles.forEach { DominoImage(it, Modifier.width(96.dp).padding(vertical = 3.dp)) }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun SpoofDialog(tile: Domino, half: Int, value: Int, error: Int?,
    onHalf: (Int) -> Unit, onValue: (Int) -> Unit, onApply: () -> Unit, onCancel: () -> Unit) {
    GameDialog(
        title = stringResource(R.string.spoof_value),
        onDismiss = onCancel,
        actions = {
            TextButton(onClick = onCancel, modifier = Modifier.heightIn(min = 48.dp)) { Text(stringResource(R.string.cancel)) }
            TextButton(onClick = onApply, modifier = Modifier.heightIn(min = 48.dp)) { Text(stringResource(R.string.apply)) }
        },
    ) {
            Column {
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
            }
    }
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
