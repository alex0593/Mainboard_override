package com.aela.mainboardoverride.domain

const val BOARD_WIDTH = 9
const val BOARD_HEIGHT = 7
const val MIN_GENERATED_BOARD_WIDTH = 8
const val MAX_GENERATED_BOARD_WIDTH = 10
const val MAX_RAM = 3
const val MAX_TRACE = 100
const val DOMINO_HAND_SIZE = 3
const val SCRIPT_HAND_SIZE = 5

data class Position(val x: Int, val y: Int) {
    fun neighbors(width: Int = BOARD_WIDTH, height: Int = BOARD_HEIGHT): List<Position> = listOf(
        Position(x - 1, y), Position(x + 1, y),
        Position(x, y - 1), Position(x, y + 1),
    ).filter { it.x in 0 until width && it.y in 0 until height }
}

enum class Orientation { HORIZONTAL, VERTICAL }

data class Domino(val id: String, val first: Int, val second: Int) {
    init {
        require(first in 0..6 && second in 0..6)
    }

    fun rotated() = copy(first = second, second = first)
}

/** A domino committed to two board cells. The two halves always form an internal edge. */
data class PlacedDomino(
    val domino: Domino,
    val origin: Position,
    val orientation: Orientation,
) {
    val positions: Pair<Position, Position>
        get() = origin to if (orientation == Orientation.HORIZONTAL) {
            Position(origin.x + 1, origin.y)
        } else {
            Position(origin.x, origin.y + 1)
        }

    fun valueAt(position: Position): Int? = when (position) {
        positions.first -> domino.first
        positions.second -> domino.second
        else -> null
    }
}

enum class ScriptType(
    val ramCost: Int,
    val traceNoise: Int,
) {
    PING(1, 0),
    SPOOF(2, 5),
    KILL_PROCESS(3, 15),
    BRIDGE(2, 10),
}

data class ScriptCard(val id: String, val type: ScriptType)

data class Bridge(val id: String, val center: Position, val horizontal: Boolean)

data class Daemon(val position: Position)

enum class TurnPhase { ACTION, SYSTEM, FINISHED }

enum class GameResult {
    VICTORY,
    TRACE_INTERCEPTED,
    DAEMON_BREACH,
    KERNEL_PANIC,
    MEMORY_EXHAUSTED,
    CHALLENGE_LIMIT,
}

/**
 * Immutable snapshot of every spatial element in a level.
 *
 * Start and extraction are virtual single-cell nodes. Honeypots do not occupy a
 * cell: they are hidden coordinates activated when hardware is placed over them.
 */
data class BoardState(
    val width: Int = BOARD_WIDTH,
    val height: Int = BOARD_HEIGHT,
    val start: Position = Position(0, height / 2),
    val extraction: Position = Position(width - 1, height / 2),
    val placed: List<PlacedDomino> = emptyList(),
    val firewalls: Set<Position> = emptySet(),
    val honeypots: Set<Position> = emptySet(),
    val revealedHoneypots: Set<Position> = emptySet(),
    val triggeredHoneypots: Set<Position> = emptySet(),
    val bridges: List<Bridge> = emptyList(),
    val daemon: Daemon? = null,
    val buffs: Map<Position, BoardBuff> = emptyMap(),
    val collectedBuffs: Set<Position> = emptySet(),
) {
    fun valueAt(position: Position): Int? {
        if (position == start) return 0
        if (position == extraction) return 6
        return placed.firstNotNullOfOrNull { it.valueAt(position) }
    }

    fun isOccupied(position: Position): Boolean =
        position == start || position == extraction ||
            position in firewalls || placed.any { it.valueAt(position) != null }
}

enum class BoardBuff(val traceDelta: Int = 0, val ramDelta: Int = 0) {
    TRACE_COOLER(traceDelta = -8),
    RAM_RESERVE(ramDelta = 1),
}

/** Complete serializable-in-principle snapshot consumed and produced by [GameEngine]. */
data class GameState(
    val seed: Long,
    val board: BoardState,
    val dominoHand: List<Domino>,
    val dominoBag: List<Domino>,
    val scriptHand: List<ScriptCard>,
    val scriptDeck: List<ScriptCard>,
    val ram: Int = MAX_RAM,
    val trace: Int = 0,
    val pendingNoise: Int = 0,
    val turn: Int = 1,
    val tilePlacedThisTurn: Boolean = false,
    val pingPreview: List<Domino> = emptyList(),
    val challengeRules: ChallengeRules? = null,
    val phase: TurnPhase = TurnPhase.ACTION,
    val result: GameResult? = null,
)

/** Commands accepted by [GameEngine]; UI code must never mutate [GameState] directly. */
sealed interface GameAction {
    data class PlaceDomino(
        val dominoId: String,
        val origin: Position,
        val orientation: Orientation,
        val rotated: Boolean = false,
    ) : GameAction

    data class PlayPing(val cardId: String) : GameAction
    data class PlaySpoof(val cardId: String, val dominoId: String, val half: Int, val value: Int) : GameAction
    data class PlayKillProcess(val cardId: String, val target: Position) : GameAction
    data class PlayBridge(val cardId: String, val center: Position, val horizontal: Boolean) : GameAction
    data object EndTurn : GameAction
}

sealed interface GameEvent {
    data object DominoPlaced : GameEvent
    data class ScriptExecuted(val type: ScriptType) : GameEvent
    data object HoneypotTriggered : GameEvent
    data class BuffCollected(val buff: BoardBuff) : GameEvent
    data class Rejected(val reason: RejectReason) : GameEvent
    data class Finished(val result: GameResult) : GameEvent
}

enum class RejectReason {
    GAME_FINISHED,
    WRONG_PHASE,
    DOMINO_ALREADY_PLACED,
    DOMINO_NOT_FOUND,
    OUT_OF_BOUNDS,
    CELL_OCCUPIED,
    CONTACT_MISMATCH,
    NOT_CONNECTED,
    CARD_NOT_FOUND,
    INSUFFICIENT_RAM,
    INVALID_TARGET,
    MUST_PLACE_DOMINO,
}

/** Reducer output: durable [state] plus one-shot presentation [events]. */
data class Transition(val state: GameState, val events: List<GameEvent> = emptyList())
