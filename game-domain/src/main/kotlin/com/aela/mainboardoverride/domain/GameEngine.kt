package com.aela.mainboardoverride.domain

import java.util.ArrayDeque

/**
 * Pure reducer and graph rules for a match.
 *
 * The object owns no mutable state and has no Android dependencies, allowing exact
 * replay and inexpensive JVM tests.
 */
object GameEngine {
    /** Applies one command. Invalid commands return the original state plus a rejection event. */
    fun reduce(state: GameState, action: GameAction): Transition {
        if (state.result != null) return rejected(state, RejectReason.GAME_FINISHED)
        if (state.phase != TurnPhase.ACTION) return rejected(state, RejectReason.WRONG_PHASE)
        return when (action) {
            is GameAction.PlaceDomino -> placeDomino(state, action)
            is GameAction.PlayPing -> playCard(state, action.cardId, ScriptType.PING) { current ->
                current.copy(
                    board = current.board.copy(revealedHoneypots = current.board.honeypots),
                    pingPreview = current.dominoBag.take(3),
                )
            }
            is GameAction.PlaySpoof -> playSpoof(state, action)
            is GameAction.PlayKillProcess -> playKill(state, action)
            is GameAction.PlayBridge -> playBridge(state, action)
            GameAction.EndTurn -> endTurn(state)
        }
    }

    /** Checks bounds, occupancy, all external contacts and connection to the active network. */
    fun canPlace(board: BoardState, domino: Domino, origin: Position, orientation: Orientation): Boolean {
        val placed = PlacedDomino(domino, origin, orientation)
        val (firstPosition, secondPosition) = placed.positions
        if (!inBounds(board, firstPosition) || !inBounds(board, secondPosition)) return false
        if (board.isOccupied(firstPosition) || board.isOccupied(secondPosition)) return false

        val newPositions = setOf(firstPosition, secondPosition)
        for (position in newPositions) {
            val value = placed.valueAt(position) ?: continue
            for (neighbor in neighbors(board, position).filterNot { it in newPositions }) {
                val neighborValue = board.valueAt(neighbor) ?: continue
                if (neighborValue != value) return false
            }
        }

        val reachable = reachableFromStart(board)
        return newPositions.any { position ->
            val value = placed.valueAt(position)
            neighbors(board, position).any { it in reachable && board.valueAt(it) == value }
        }
    }

    /** Returns true when extraction belongs to the component rooted at the start node. */
    fun isExtractionConnected(board: BoardState): Boolean = board.extraction in reachableFromStart(board)

    /** Enumerates every legal origin, orientation and port order for the current hand. */
    fun legalPlacements(state: GameState): List<GameAction.PlaceDomino> = buildList {
        state.dominoHand.forEach { tile ->
            Orientation.entries.forEach { orientation ->
                for (y in 0 until state.board.height) for (x in 0 until state.board.width) {
                    val position = Position(x, y)
                    if (canPlace(state.board, tile, position, orientation)) {
                        add(GameAction.PlaceDomino(tile.id, position, orientation))
                    }
                    val rotated = tile.rotated()
                    if (tile.first != tile.second && canPlace(state.board, rotated, position, orientation)) {
                        add(GameAction.PlaceDomino(tile.id, position, orientation, rotated = true))
                    }
                }
            }
        }
    }

    private fun placeDomino(state: GameState, action: GameAction.PlaceDomino): Transition {
        if (state.tilePlacedThisTurn) return rejected(state, RejectReason.DOMINO_ALREADY_PLACED)
        val handTile = state.dominoHand.find { it.id == action.dominoId }
            ?: return rejected(state, RejectReason.DOMINO_NOT_FOUND)
        val tile = if (action.rotated) handTile.rotated() else handTile
        val placed = PlacedDomino(tile, action.origin, action.orientation)
        val (first, second) = placed.positions
        if (!inBounds(state.board, first) || !inBounds(state.board, second)) return rejected(state, RejectReason.OUT_OF_BOUNDS)
        if (state.board.isOccupied(first) || state.board.isOccupied(second)) {
            return rejected(state, RejectReason.CELL_OCCUPIED)
        }
        if (!canPlace(state.board, tile, action.origin, action.orientation)) {
            val touchesNetwork = setOf(first, second).any { p -> neighbors(state.board, p).any { state.board.valueAt(it) != null } }
            return rejected(state, if (touchesNetwork) RejectReason.CONTACT_MISMATCH else RejectReason.NOT_CONNECTED)
        }

        val triggered = setOf(first, second).intersect(state.board.honeypots) - state.board.triggeredHoneypots
        val newBoard = state.board.copy(
            placed = state.board.placed + placed,
            triggeredHoneypots = state.board.triggeredHoneypots + triggered,
            revealedHoneypots = state.board.revealedHoneypots + triggered,
        )
        val next = state.copy(
            board = newBoard,
            dominoHand = state.dominoHand.filterNot { it.id == handTile.id },
            tilePlacedThisTurn = true,
            pendingNoise = state.pendingNoise + if (triggered.isEmpty()) 0 else 20,
            scriptHand = if (triggered.isEmpty()) state.scriptHand else state.scriptHand.drop(1),
        )
        return Transition(
            next,
            buildList {
                add(GameEvent.DominoPlaced)
                if (triggered.isNotEmpty()) add(GameEvent.HoneypotTriggered)
            },
        )
    }

    private fun playSpoof(state: GameState, action: GameAction.PlaySpoof): Transition {
        if (action.half !in 0..1 || action.value !in 0..6) return rejected(state, RejectReason.INVALID_TARGET)
        if (state.dominoHand.none { it.id == action.dominoId }) return rejected(state, RejectReason.INVALID_TARGET)
        return playCard(state, action.cardId, ScriptType.SPOOF) { current ->
            current.copy(dominoHand = current.dominoHand.map { tile ->
                if (tile.id != action.dominoId) tile
                else if (action.half == 0) tile.copy(first = action.value) else tile.copy(second = action.value)
            })
        }
    }

    private fun playKill(state: GameState, action: GameAction.PlayKillProcess): Transition {
        if (action.target !in state.board.firewalls) return rejected(state, RejectReason.INVALID_TARGET)
        return playCard(state, action.cardId, ScriptType.KILL_PROCESS) { current ->
            current.copy(board = current.board.copy(firewalls = current.board.firewalls - action.target))
        }
    }

    private fun playBridge(state: GameState, action: GameAction.PlayBridge): Transition {
        val center = action.center
        if (center !in state.board.firewalls || state.board.bridges.any { it.center == center }) {
            return rejected(state, RejectReason.INVALID_TARGET)
        }
        val ends = if (action.horizontal) {
            Position(center.x - 1, center.y) to Position(center.x + 1, center.y)
        } else {
            Position(center.x, center.y - 1) to Position(center.x, center.y + 1)
        }
        val values = state.board.valueAt(ends.first) to state.board.valueAt(ends.second)
        if (!inBounds(state.board, ends.first) || !inBounds(state.board, ends.second) || values.first == null || values.first != values.second) {
            return rejected(state, RejectReason.INVALID_TARGET)
        }
        return playCard(state, action.cardId, ScriptType.BRIDGE) { current ->
            current.copy(board = current.board.copy(
                bridges = current.board.bridges + Bridge("bridge-${current.turn}-${center.x}-${center.y}", center, action.horizontal),
            ))
        }
    }

    private inline fun playCard(
        state: GameState,
        cardId: String,
        expectedType: ScriptType,
        effect: (GameState) -> GameState,
    ): Transition {
        val card = state.scriptHand.find { it.id == cardId && it.type == expectedType }
            ?: return rejected(state, RejectReason.CARD_NOT_FOUND)
        if (state.ram < card.type.ramCost) return rejected(state, RejectReason.INSUFFICIENT_RAM)
        val paid = state.copy(
            scriptHand = state.scriptHand.filterNot { it.id == card.id },
            ram = state.ram - card.type.ramCost,
            pendingNoise = state.pendingNoise + card.type.traceNoise,
        )
        return Transition(effect(paid), listOf(GameEvent.ScriptExecuted(card.type)))
    }

    private fun endTurn(state: GameState): Transition {
        if (!state.tilePlacedThisTurn) return rejected(state, RejectReason.MUST_PLACE_DOMINO)
        val traced = (state.trace + 8 + state.pendingNoise).coerceAtMost(MAX_TRACE)
        val movedBoard = moveDaemon(state.board)
        val result = when {
            traced >= MAX_TRACE -> GameResult.TRACE_INTERCEPTED
            movedBoard.daemon?.position == movedBoard.start -> GameResult.DAEMON_BREACH
            isExtractionConnected(movedBoard) -> GameResult.VICTORY
            else -> null
        }
        if (result != null) {
            val finished = state.copy(board = movedBoard, trace = traced, phase = TurnPhase.FINISHED, result = result)
            return Transition(finished, listOf(GameEvent.Finished(result)))
        }

        val replenishedHand = (state.dominoHand + state.dominoBag.take(1)).take(DOMINO_HAND_SIZE)
        val remainingBag = state.dominoBag.drop(1)
        val drawnScripts = if (state.scriptHand.size < SCRIPT_HAND_SIZE) {
            state.scriptHand + state.scriptDeck.take(1)
        } else state.scriptHand
        val nextDeck = if (state.scriptHand.size < SCRIPT_HAND_SIZE) state.scriptDeck.drop(1) else state.scriptDeck
        var next = state.copy(
            board = movedBoard,
            dominoHand = replenishedHand,
            dominoBag = remainingBag,
            scriptHand = drawnScripts,
            scriptDeck = nextDeck,
            ram = MAX_RAM,
            trace = traced,
            pendingNoise = 0,
            turn = state.turn + 1,
            tilePlacedThisTurn = false,
            pingPreview = emptyList(),
        )
        val noPlacement = legalPlacements(next).isEmpty()
        val canSpoof = next.ram >= ScriptType.SPOOF.ramCost && next.scriptHand.any { it.type == ScriptType.SPOOF }
        val forcedResult = when {
            next.dominoHand.isEmpty() && next.dominoBag.isEmpty() -> GameResult.MEMORY_EXHAUSTED
            noPlacement && !canSpoof -> GameResult.KERNEL_PANIC
            else -> null
        }
        if (forcedResult != null) next = next.copy(phase = TurnPhase.FINISHED, result = forcedResult)
        return Transition(next, forcedResult?.let { listOf(GameEvent.Finished(it)) }.orEmpty())
    }

    private fun moveDaemon(board: BoardState): BoardState {
        val daemon = board.daemon ?: return board
        if (daemon.position == board.start) return board
        val previous = mutableMapOf<Position, Position?>()
        val queue = ArrayDeque<Position>()
        queue.add(board.start)
        previous[board.start] = null
        while (queue.isNotEmpty()) {
            val current = queue.removeFirst()
            connectedNeighbors(board, current).forEach { neighbor ->
                if (neighbor !in previous) {
                    previous[neighbor] = current
                    queue.add(neighbor)
                }
            }
        }
        if (daemon.position !in previous) return board
        return board.copy(daemon = daemon.copy(position = previous[daemon.position] ?: board.start))
    }

    private fun reachableFromStart(board: BoardState): Set<Position> {
        val visited = mutableSetOf(board.start)
        val queue = ArrayDeque<Position>().apply { add(board.start) }
        while (queue.isNotEmpty()) {
            connectedNeighbors(board, queue.removeFirst()).forEach { neighbor ->
                if (visited.add(neighbor)) queue.add(neighbor)
            }
        }
        return visited
    }

    private fun connectedNeighbors(board: BoardState, position: Position): List<Position> {
        val result = mutableSetOf<Position>()
        val containing = board.placed.find { it.valueAt(position) != null }
        if (containing != null) {
            val pair = containing.positions
            result += if (pair.first == position) pair.second else pair.first
        }
        val value = board.valueAt(position)
        if (value != null) {
            neighbors(board, position).filterTo(result) { board.valueAt(it) == value }
        }
        board.bridges.forEach { bridge ->
            val ends = if (bridge.horizontal) {
                Position(bridge.center.x - 1, bridge.center.y) to Position(bridge.center.x + 1, bridge.center.y)
            } else {
                Position(bridge.center.x, bridge.center.y - 1) to Position(bridge.center.x, bridge.center.y + 1)
            }
            if (position == ends.first) result += ends.second
            if (position == ends.second) result += ends.first
        }
        return result.filter { inBounds(board, it) }
    }

    private fun inBounds(board: BoardState, position: Position): Boolean =
        position.x in 0 until board.width && position.y in 0 until board.height

    private fun neighbors(board: BoardState, position: Position): List<Position> = listOf(
        Position(position.x - 1, position.y), Position(position.x + 1, position.y),
        Position(position.x, position.y - 1), Position(position.x, position.y + 1),
    ).filter { inBounds(board, it) }

    private fun rejected(state: GameState, reason: RejectReason) =
        Transition(state, listOf(GameEvent.Rejected(reason)))
}
