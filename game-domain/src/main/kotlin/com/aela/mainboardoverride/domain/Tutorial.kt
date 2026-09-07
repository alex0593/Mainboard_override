package com.aela.mainboardoverride.domain

import kotlin.random.Random

/** Ordered lessons. Explanations never consume game resources. */
enum class TutorialStep {
    INTRO, SELECT, ROTATE, PLACE, END_TURN, SYSTEM,
    PING, PING_RESULT, SPOOF, SPOOF_RESULT, KILL, KILL_RESULT,
    BRIDGE, BRIDGE_RESULT, FINAL, COMPLETE;

    val explanation: Boolean get() = this in setOf(INTRO, SYSTEM, PING_RESULT, SPOOF_RESULT, KILL_RESULT, BRIDGE_RESULT)
    val guided: Boolean get() = this < FINAL
}

/** Deterministic teaching fixtures use the same reducer as ordinary matches. */
object Tutorial {
    fun fixture(step: TutorialStep): GameState {
        val base = GameState(
            seed = LevelGenerator.TUTORIAL_SEED,
            board = BoardState(),
            dominoHand = listOf(Domino("lesson", 0, 2)),
            dominoBag = listOf(Domino("next", 2, 6), Domino("preview", 6, 6), Domino("third", 1, 4)),
            scriptHand = emptyList(), scriptDeck = emptyList(),
        )
        return when (step) {
            TutorialStep.PING -> base.copy(board = base.board.copy(honeypots = setOf(Position(2, 1))), scriptHand = listOf(card(ScriptType.PING)))
            TutorialStep.SPOOF -> base.copy(dominoHand = listOf(Domino("lesson", 3, 2)), scriptHand = listOf(card(ScriptType.SPOOF)))
            TutorialStep.KILL -> base.copy(board = base.board.copy(firewalls = setOf(Position(1, 3))), scriptHand = listOf(card(ScriptType.KILL_PROCESS)))
            TutorialStep.BRIDGE -> base.copy(board = base.board.copy(
                placed = listOf(PlacedDomino(Domino("left", 0, 2), Position(1, 3), Orientation.HORIZONTAL), PlacedDomino(Domino("right", 2, 6), Position(4, 3), Orientation.HORIZONTAL)),
                firewalls = setOf(Position(3, 3)),
            ), scriptHand = listOf(card(ScriptType.BRIDGE)))
            TutorialStep.FINAL -> finalPractice()
            else -> base
        }
    }

    private fun finalPractice(): GameState {
        val seed = LevelGenerator.TUTORIAL_SEED
        val random = Random(seed)
        val joints = List(3) { random.nextInt(0, 7) }
        val solution = listOf(
            Domino("route-1", 0, joints[0]),
            Domino("route-2", joints[0], joints[1]),
            Domino("route-3", joints[1], 6),
            Domino("route-4", random.nextInt(0, 7), 6),
        )
        val decoys = standardSet()
            .filterNot { candidate -> solution.any { it.first == candidate.first && it.second == candidate.second } }
            .shuffled(random)
            .take(12)
            .mapIndexed { index, tile -> tile.copy(id = "decoy-$index") }

        // A route tile becomes available after every successful placement while
        // retaining two decoys in hand, so every seed remains completable.
        val bag = buildList {
            add(solution[0])
            add(decoys[0])
            add(decoys[1])
            add(solution[1])
            add(solution[2])
            add(solution[3])
            addAll(decoys.drop(2))
        }
        val scripts = buildList {
            repeat(3) { cycle ->
                ScriptType.entries.forEach { type -> add(ScriptCard("$cycle-${type.name}", type)) }
            }
        }.shuffled(random)

        val initialBoard = BoardState(
            firewalls = setOf(Position(4, 1), Position(4, 5)),
            honeypots = setOf(Position(2, 1), Position(6, 5)),
            daemon = Daemon(Position(BOARD_WIDTH - 1, BOARD_HEIGHT / 2)),
        )
        return GameState(
            seed = seed,
            board = initialBoard,
            dominoHand = bag.take(DOMINO_HAND_SIZE),
            dominoBag = bag.drop(DOMINO_HAND_SIZE),
            scriptHand = scripts.take(2),
            scriptDeck = scripts.drop(2),
        )
    }

    private fun standardSet(): List<Domino> = buildList {
        for (first in 0..6) for (second in first..6) add(Domino("$first-$second", first, second))
    }

    private fun card(type: ScriptType) = ScriptCard("lesson-${type.name}", type)

    fun target(step: TutorialStep): Position? = when (step) {
        TutorialStep.PLACE, TutorialStep.KILL -> Position(1, 3)
        TutorialStep.BRIDGE -> Position(3, 3)
        else -> null
    }

    fun allows(step: TutorialStep, action: GameAction): Boolean = when (step) {
        TutorialStep.PLACE -> action == GameAction.PlaceDomino("lesson", Position(1, 3), Orientation.VERTICAL)
        TutorialStep.END_TURN -> action == GameAction.EndTurn
        TutorialStep.PING -> action is GameAction.PlayPing
        TutorialStep.SPOOF -> action is GameAction.PlaySpoof && action.dominoId == "lesson" && action.half == 0 && action.value == 0
        TutorialStep.KILL -> action is GameAction.PlayKillProcess && action.target == target(step)
        TutorialStep.BRIDGE -> action is GameAction.PlayBridge && action.center == target(step) && action.horizontal
        TutorialStep.FINAL -> true
        else -> false
    }

    /** Rejections and unrelated actions cannot advance the lesson. */
    fun after(step: TutorialStep, action: GameAction, transition: Transition): TutorialStep {
        if (!allows(step, action) || transition.events.any { it is GameEvent.Rejected }) return step
        return when (step) {
            TutorialStep.PLACE -> TutorialStep.END_TURN
            TutorialStep.END_TURN -> TutorialStep.SYSTEM
            TutorialStep.PING -> TutorialStep.PING_RESULT
            TutorialStep.SPOOF -> TutorialStep.SPOOF_RESULT
            TutorialStep.KILL -> TutorialStep.KILL_RESULT
            TutorialStep.BRIDGE -> TutorialStep.BRIDGE_RESULT
            TutorialStep.FINAL -> if (transition.state.result == GameResult.VICTORY) TutorialStep.COMPLETE else step
            else -> step
        }
    }

    fun continueFrom(step: TutorialStep): TutorialStep = when (step) {
        TutorialStep.INTRO -> TutorialStep.SELECT
        TutorialStep.SYSTEM -> TutorialStep.PING
        TutorialStep.PING_RESULT -> TutorialStep.SPOOF
        TutorialStep.SPOOF_RESULT -> TutorialStep.KILL
        TutorialStep.KILL_RESULT -> TutorialStep.BRIDGE
        TutorialStep.BRIDGE_RESULT -> TutorialStep.FINAL
        else -> step
    }

    fun lessonStart(step: TutorialStep): TutorialStep = when (step) {
        TutorialStep.PING, TutorialStep.PING_RESULT -> TutorialStep.PING
        TutorialStep.SPOOF, TutorialStep.SPOOF_RESULT -> TutorialStep.SPOOF
        TutorialStep.KILL, TutorialStep.KILL_RESULT -> TutorialStep.KILL
        TutorialStep.BRIDGE, TutorialStep.BRIDGE_RESULT -> TutorialStep.BRIDGE
        TutorialStep.FINAL, TutorialStep.COMPLETE -> TutorialStep.FINAL
        else -> TutorialStep.INTRO
    }
}
