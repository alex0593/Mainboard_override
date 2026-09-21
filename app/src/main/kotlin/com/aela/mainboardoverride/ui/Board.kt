package com.aela.mainboardoverride.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.platform.testTag
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.zIndex
import androidx.compose.runtime.remember
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.painterResource
import androidx.compose.foundation.Image
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aela.mainboardoverride.R
import com.aela.mainboardoverride.domain.BoardState
import com.aela.mainboardoverride.domain.Orientation
import com.aela.mainboardoverride.domain.Position
import com.aela.mainboardoverride.domain.BoardBuff
import androidx.compose.animation.core.*
import kotlinx.coroutines.delay

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
    // Restarting, repeating a lesson or opening another match replaces the board: the key tells
    // destruction feedback that nothing was killed.
    sessionKey: Any? = null,
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
    // Only KILL removes firewalls, revealed honeypots and whole dominoes from the state, so a diff
    // against the previous snapshot is exactly the destruction signal.
    val aliveEntities = remember(board.firewalls, board.revealedHoneypots, board.placed) {
        buildMap<String, List<Position>> {
            board.firewalls.forEach { put("firewall:${it.x}:${it.y}", listOf(it)) }
            board.revealedHoneypots.forEach { put("honeypot:${it.x}:${it.y}", listOf(it)) }
            board.placed.forEach { put("domino:${it.domino.id}", it.positions.toList()) }
        }
    }
    var session by remember { mutableStateOf(sessionKey) }
    var knownEntities by remember { mutableStateOf<Map<String, List<Position>>?>(null) }
    var bursts by remember { mutableStateOf(emptyList<DestructionBurst>()) }
    var burstIds by remember { mutableStateOf(0) }
    LaunchedEffect(sessionKey, aliveEntities) {
        val freshSession = session != sessionKey
        session = sessionKey
        val previous = knownEntities
        knownEntities = aliveEntities
        if (freshSession || previous == null) return@LaunchedEffect
        val destroyed = previous.filterKeys { it !in aliveEntities }
        if (destroyed.isEmpty()) return@LaunchedEffect
        val created = destroyed.values.map { positions -> DestructionBurst(++burstIds, positions) }
        bursts = bursts + created
        delay(BurstDurationMillis)
        bursts = bursts - created.toSet()
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
        val cell = minOf((maxWidth - margin * 2) / (board.width + 2), (maxHeight - margin * 2) / board.height).coerceAtLeast(0.dp)
        Box(Modifier.width(cell * (board.width + 2)).height(cell * board.height)) {
            PortSprite(R.drawable.board_port_s0_v1, "S0", board.start.y, cell, start = true,
                modifier = Modifier.offset(x = cell * .18f).testTag("board-port-s0"))
            PortSprite(R.drawable.board_port_x6_v1, "X6", board.extraction.y, cell, start = false,
                modifier = Modifier.offset(x = cell * (board.width + .82f)).testTag("board-port-x6"))
            Box(Modifier.offset(x = cell).width(cell * board.width).height(cell * board.height)
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
                            p in board.buffs -> null
                            else -> null
                        }
                        if (label != null) drawContext.canvas.nativeCanvas.drawText(label, (x + .5f) * unit, (y + .65f) * unit, ink)
                    }
                }
                (board.buffs.keys - board.collectedBuffs).forEach { position ->
                    Box(Modifier.offset(cell * position.x, cell * position.y).size(cell).padding(2.dp)) {
                        BoardBuffSprite(board.buffs.getValue(position))
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
                            BoardBridgeSprite(bridge.horizontal, bridge.value)
                        }
                    }
                }
            } else for (y in 0 until board.height) for (x in 0 until board.width) {
                val position = Position(x, y)
                BoardCell(board, position, cell, position in legalOrigins, position == target, onCell)
            }
            // Bursts cover the destroyed footprint, which is two cells for a removed domino.
            for (burst in bursts) {
                val minX = burst.positions.minOf { it.x }
                val minY = burst.positions.minOf { it.y }
                DestructionBurst(
                    burstId = burst.id,
                    modifier = Modifier
                        .offset(x = cell * minX, y = cell * minY)
                        .size(
                            width = cell * (burst.positions.maxOf { it.x } - minX + 1),
                            height = cell * (burst.positions.maxOf { it.y } - minY + 1),
                        ),
                )
            }
            }
        }
    }
}

@Composable
private fun PortSprite(resource: Int, label: String, row: Int, size: Dp, start: Boolean, modifier: Modifier = Modifier) {
    Box(
        modifier.offset(y = size * row).width(size).height(size).zIndex(2f),
        contentAlignment = Alignment.BottomCenter,
    ) {
        Image(
            painterResource(resource), contentDescription = label,
            modifier = Modifier.fillMaxSize(),
            contentScale = androidx.compose.ui.layout.ContentScale.Fit,
        )
        Text(
            label,
            color = Cyan,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Black,
            fontSize = (size.value * .18f).coerceAtLeast(7f).sp,
            modifier = Modifier.align(if (start) Alignment.CenterStart else Alignment.CenterEnd)
                .offset(x = if (start) -size * .24f else size * .24f),
        )
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
            .testTag("board-cell-${position.x}-${position.y}")
            .padding(2.dp)
            .background(if (isPlaced) Color.Transparent else placedColor.copy(alpha = if (value != null || firewall || trap || buff != null) .22f else placedColor.alpha))
            .then(if (target) Modifier.border(3.dp, Warning) else if (legal) Modifier.border(1.dp, Terminal) else Modifier)
            .clickable(role = Role.Button) { onCell(position) }
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
            if (bridge != null) BoardBridgeSprite(bridge.horizontal, bridge.value)
        } else if (buff != null && !daemon && !isStart && !isExit) {
            BoardBuffSprite(buff)
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

/** One KILL burst. The id keeps repeated destructions animating independently. */
private data class DestructionBurst(val id: Int, val positions: List<Position>)

/** How long a KILL burst stays on the board. */
private const val BurstDurationMillis = 460L

/**
 * Reads as a digital impact: the destroyed cells flash, glitch bars sweep across them, two rings
 * expand and sparks fly out. With [LocalReducedMotion] only the flash remains, so nothing moves.
 */
@Composable
private fun DestructionBurst(burstId: Int, modifier: Modifier = Modifier) {
    val reducedMotion = LocalReducedMotion.current
    val progress = remember(burstId) { Animatable(0f) }
    LaunchedEffect(burstId) { progress.animateTo(1f, tween(BurstDurationMillis.toInt(), easing = LinearEasing)) }
    Canvas(modifier.testTag("kill-burst")) {
        val elapsed = progress.value
        val center = Offset(size.width / 2f, size.height / 2f)
        val reach = size.minDimension
        drawRect(Danger.copy(alpha = .45f * (1f - elapsed)))
        if (reducedMotion) return@Canvas
        val glitch = (.34f - elapsed).coerceAtLeast(0f) / .34f
        if (glitch > 0f) repeat(3) { index ->
            val y = size.height * (index + 1f) / 4f
            val side = if (index % 2 == 0) 1f else -1f
            drawLine(Danger.copy(alpha = .85f * glitch),
                Offset(center.x + side * reach * .38f, y), Offset(center.x + side * reach * .12f, y),
                strokeWidth = 1.5.dp.toPx())
        }
        repeat(2) { ring ->
            val expansion = ((elapsed - ring * .18f) / .82f).coerceIn(0f, 1f)
            if (expansion > 0f) drawCircle(
                (if (ring == 0) Danger else Warning).copy(alpha = .9f * (1f - expansion)),
                radius = reach * (.22f + .78f * expansion),
                center = center,
                style = Stroke(width = (2.dp.toPx() * (1f - expansion)).coerceAtLeast(.5f)),
            )
        }
        repeat(8) { index ->
            val angle = index * (PI.toFloat() / 4f)
            val outward = Offset(cos(angle), sin(angle))
            val start = reach * (.2f + .5f * elapsed)
            drawLine(Warning.copy(alpha = .9f * (1f - elapsed)),
                center + outward * start, center + outward * (start + reach * .26f),
                strokeWidth = 1.5.dp.toPx())
        }
    }
}

@Composable
private fun BoardBridgeSprite(horizontal: Boolean, value: Int) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.BottomEnd) {
        Image(painterResource(R.drawable.board_bridge_v1), contentDescription = null,
            modifier = Modifier.fillMaxSize().rotate(if (horizontal) 0f else 90f))
        Text("$value", color = Warning, fontWeight = FontWeight.Black, fontSize = 10.sp,
            modifier = Modifier.background(Void.copy(alpha = .85f)).padding(horizontal = 2.dp))
    }
}

@Composable
private fun BoardBuffSprite(buff: BoardBuff) {
    // The label is a small corner chip so the pickup artwork underneath stays visible.
    Box(Modifier.fillMaxSize()) {
        Image(painterResource(if (buff == BoardBuff.TRACE_COOLER) R.drawable.board_trace_cooler_v1
            else R.drawable.board_ram_reserve_v1), contentDescription = null, modifier = Modifier.fillMaxSize().padding(1.dp))
        Text(if (buff == BoardBuff.TRACE_COOLER) "−8" else "+1R", color = Cyan,
            fontSize = 9.sp, fontWeight = FontWeight.Black, maxLines = 1,
            modifier = Modifier.align(Alignment.TopEnd)
                .background(Void.copy(alpha = .65f), RoundedCornerShape(2.dp))
                .padding(horizontal = 3.dp, vertical = 1.dp))
    }
}
