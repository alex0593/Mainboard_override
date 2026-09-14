package com.aela.mainboardoverride.domain

sealed interface TutorialInput {
    data object Next : TutorialInput
    data class SelectTile(val id: String) : TutorialInput
    data object Rotate : TutorialInput
    data class SelectScript(val id: String) : TutorialInput
    data class Cell(val position: Position) : TutorialInput
    data class Half(val half: Int) : TutorialInput
    data class Value(val value: Int) : TutorialInput
    data object ApplySpoof : TutorialInput
    data object Cancel : TutorialInput
    data object ToggleBridge : TutorialInput
    data object EndTurn : TutorialInput
}

data class TutorialStep(
    val instruction: Int,
    val input: TutorialInput,
    /** Keeps the instruction visible before a key teaching action. */
    val pauseBeforeAction: Boolean = false,
)
data class TutorialLesson(
    val initial: GameState,
    val steps: List<TutorialStep>,
    val initialHorizontalBridge: Boolean = true,
)
data class TutorialState(
    val lesson: Int = 0,
    val step: Int = 0,
    val game: GameState = TutorialCatalog.lessons[0].initial,
    val tile: String? = null,
    val script: String? = null,
    val rotation: Int = 0,
    val half: Int = 0,
    val value: Int = 0,
    val horizontalBridge: Boolean = true,
    val incorrect: Boolean = false,
) {
    val definition get() = TutorialCatalog.lessons[lesson]
    val finished get() = step >= definition.steps.size
    val expected get() = definition.steps.getOrNull(step)?.input
}

/** Fixtures use real engine commands; no rewards, persistence or production session access. */
object TutorialCatalog {
    private fun base() = GameState(
        seed = 42,
        board = BoardState(width = 8, height = 5, start = Position(-1, 2), extraction = Position(8, 2)),
        dominoHand = listOf(Domino("a", 0, 2), Domino("b", 2, 4), Domino("c", 4, 6)),
        dominoBag = listOf(Domino("next", 2, 3)),
        scriptHand = ScriptType.entries.map { ScriptCard(it.name, it) },
        scriptDeck = emptyList(),
    )
    private val first = PlacedDomino(Domino("a", 0, 2), Position(0, 2), Orientation.HORIZONTAL)
    private fun step(id: Int, input: TutorialInput, pauseBeforeAction: Boolean = false) =
        TutorialStep(id, input, pauseBeforeAction)
    val lessons: List<TutorialLesson> = listOf(
        TutorialLesson(base(), listOf(step(0, TutorialInput.Next, pauseBeforeAction = true))),
        TutorialLesson(base(), listOf(
            step(1, TutorialInput.SelectTile("a"), pauseBeforeAction = true),
            step(2, TutorialInput.Cell(Position(0, 2))), step(3, TutorialInput.Next, pauseBeforeAction = true))),
        TutorialLesson(base().copy(dominoHand = listOf(Domino("a", 2, 0))), listOf(
            step(4, TutorialInput.SelectTile("a"), pauseBeforeAction = true), step(5, TutorialInput.Rotate),
            step(6, TutorialInput.Rotate), step(7, TutorialInput.Cell(Position(0, 2))))),
        TutorialLesson(base().copy(board = base().board.copy(placed = listOf(first)),
            dominoHand = base().dominoHand.drop(1), tilePlacedThisTurn = true, ram = 1, pendingNoise = 5), listOf(
            step(8, TutorialInput.EndTurn, pauseBeforeAction = true), step(9, TutorialInput.Next, pauseBeforeAction = true))),
        TutorialLesson(base().copy(ram = 1, trace = 20, board = base().board.copy(
            firewalls = setOf(Position(4, 1)), honeypots = setOf(Position(2, 2)),
            daemon = Daemon(Position(6, 4)),
            buffs = mapOf(Position(1, 2) to BoardBuff.RAM_RESERVE, Position(2, 2) to BoardBuff.TRACE_COOLER))), listOf(
            step(10, TutorialInput.Next, pauseBeforeAction = true), step(11, TutorialInput.SelectTile("a")),
            step(12, TutorialInput.Cell(Position(0, 2))), step(13, TutorialInput.EndTurn), step(14, TutorialInput.Next))),
        TutorialLesson(base().copy(board = base().board.copy(
            honeypots = setOf(Position(4, 3)),
            buffs = mapOf(Position(0, 2) to BoardBuff.RAM_RESERVE))), listOf(
            step(15, TutorialInput.SelectScript("PING"), pauseBeforeAction = true),
            step(44, TutorialInput.SelectTile("a"), pauseBeforeAction = true), step(45, TutorialInput.Cell(Position(0, 2))),
            step(46, TutorialInput.SelectScript("KILL_PROCESS"), pauseBeforeAction = true),
            step(47, TutorialInput.Cell(Position(4, 3))),
            step(48, TutorialInput.EndTurn))),
        TutorialLesson(base().copy(dominoHand = listOf(Domino("a", 1, 2))), listOf(
            step(17, TutorialInput.SelectScript("SPOOF"), pauseBeforeAction = true), step(18, TutorialInput.SelectTile("a")),
            step(19, TutorialInput.Half(1)), step(20, TutorialInput.Value(3)), step(21, TutorialInput.Cancel),
            step(22, TutorialInput.SelectScript("SPOOF")), step(23, TutorialInput.SelectTile("a")),
            step(24, TutorialInput.Half(0)), step(25, TutorialInput.Value(0)),
            step(26, TutorialInput.ApplySpoof), step(27, TutorialInput.SelectTile("a")),
            step(28, TutorialInput.Cell(Position(0, 2))))),
        TutorialLesson(base().copy(board = base().board.copy(firewalls = setOf(Position(0, 2)))), listOf(
            step(29, TutorialInput.SelectScript("KILL_PROCESS"), pauseBeforeAction = true), step(30, TutorialInput.Cell(Position(0, 2))),
            step(31, TutorialInput.SelectTile("a")), step(32, TutorialInput.Cell(Position(0, 2))))),
        TutorialLesson(base().copy(board = base().board.copy(placed = listOf(first), firewalls = setOf(Position(2, 2))),
            dominoHand = listOf(Domino("b", 2, 6))), listOf(
            step(33, TutorialInput.SelectScript("BRIDGE"), pauseBeforeAction = true), step(34, TutorialInput.ToggleBridge),
            step(36, TutorialInput.Cell(Position(2, 2))), step(37, TutorialInput.SelectTile("b")),
            step(38, TutorialInput.Cell(Position(3, 2)))), initialHorizontalBridge = false),
        TutorialLesson(base().copy(board = base().board.copy(placed = listOf(first,
            PlacedDomino(Domino("b", 2, 4), Position(2, 2), Orientation.HORIZONTAL))),
            dominoHand = listOf(Domino("c", 4, 6))), listOf(
            step(39, TutorialInput.SelectTile("c"), pauseBeforeAction = true), step(40, TutorialInput.Cell(Position(4, 2))),
            step(41, TutorialInput.EndTurn))),
    )

    fun start(lesson: Int) = lesson.coerceIn(0, lessons.lastIndex).let {
        TutorialState(lesson = it, game = lessons[it].initial,
            horizontalBridge = lessons[it].initialHorizontalBridge)
    }

    fun reduce(state: TutorialState, input: TutorialInput): TutorialState {
        if (state.finished || input != state.expected) return state.copy(incorrect = true)
        var next = state
        var action: GameAction? = null
        when (input) {
            TutorialInput.Next -> Unit
            is TutorialInput.SelectTile -> next = state.copy(tile = input.id)
            TutorialInput.Rotate -> next = state.copy(rotation = (state.rotation + 1) % 4)
            is TutorialInput.SelectScript -> {
                if (input.id == "PING") action = GameAction.PlayPing(input.id)
                else next = state.copy(script = input.id, tile = null, half = 0, value = 0)
            }
            is TutorialInput.Cell -> action = when (state.script) {
                "KILL_PROCESS" -> GameAction.PlayKillProcess("KILL_PROCESS", input.position)
                "BRIDGE" -> GameAction.PlayBridge("BRIDGE", input.position, state.horizontalBridge)
                else -> GameAction.PlaceDomino(checkNotNull(state.tile), input.position,
                    if (state.rotation % 2 == 0) Orientation.HORIZONTAL else Orientation.VERTICAL, state.rotation >= 2)
            }
            is TutorialInput.Half -> next = state.copy(half = input.half)
            is TutorialInput.Value -> next = state.copy(value = input.value)
            TutorialInput.ApplySpoof -> action = GameAction.PlaySpoof("SPOOF", checkNotNull(state.tile), state.half, state.value)
            TutorialInput.Cancel -> next = state.copy(script = null, tile = null)
            TutorialInput.ToggleBridge -> next = state.copy(horizontalBridge = !state.horizontalBridge)
            TutorialInput.EndTurn -> action = GameAction.EndTurn
        }
        action?.let {
            val transition = GameEngine.reduce(state.game, it)
            if (transition.events.any { event -> event is GameEvent.Rejected }) return state.copy(incorrect = true)
            next = next.copy(game = transition.state, tile = null, script = null, rotation = 0)
        }
        return next.copy(step = state.step + 1, incorrect = false)
    }
}
