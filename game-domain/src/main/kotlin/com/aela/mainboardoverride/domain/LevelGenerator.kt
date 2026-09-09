package com.aela.mainboardoverride.domain

import kotlin.random.Random

/** Reproducible networks, with a reducer-verified witness kept outside the game state. */
object LevelGenerator {
    const val TUTORIAL_SEED = 0x4D41494EL
    internal data class GeneratedLevel(val state: GameState, val solution: List<GameAction.PlaceDomino>)

    fun generate(seed: Long): GameState = generateVerified(seed).state
    fun generateScenario(seed: Long, scenarioId: String): GameState = generateVerified(seed, scenarioId = scenarioId).state
    fun generateChallenge(level: Int): GameState = generateVerified(
        ChallengeCatalog.level(level)?.seed ?: error("Unknown challenge level $level"), level
    ).state

    internal fun generateVerified(seed: Long, challengeLevel: Int? = null, scenarioId: String = "classic"): GeneratedLevel {
        val scenario = ScenarioCatalog.get(scenarioId).takeIf { it.id != "classic" }
        val random = Random(seed)
        if (scenario != null) {
            repeat(512) {
                val start = Position(0, random.nextInt(1, scenario.height))
                val extraction = Position(scenario.width - 1, random.nextInt(1, scenario.height))
                val path = findPath(random, scenario.route, start, extraction, scenario.width, scenario.height)
                if (path != null) buildLevel(seed, random, path, start, extraction, scenario = scenario)?.let { return it }
            }
            error("Unable to generate scenario ${scenario.id} for $seed")
        }
        val startY = random.nextInt(1, 6)
        val exitY = (startY + random.nextInt(1, 6)) % 6 + 1
        val start = Position(0, startY)
        // Some seeds use an eight-column board, others nine columns.
        val extraction = Position(random.nextInt(7, MAX_GENERATED_BOARD_WIDTH), exitY)
        repeat(24) {
            val count = challengeLevel?.let { (4 + it / 2).coerceIn(5, 9) } ?: random.nextInt(5, 8)
            val path = findPath(random, count, start, extraction)
            if (path != null) buildLevel(seed, random, path, start, extraction, challengeLevel)?.let { return it }
        }
        // A five-tile detour; reflections retain valid board geometry.
        val fallback = listOf(1 to 3, 1 to 2, 2 to 2, 3 to 2, 4 to 2, 5 to 2, 6 to 2, 7 to 2, 7 to 3, 7 to 4)
        val reflect = random.nextBoolean()
        return checkNotNull(buildLevel(seed, random, fallback.map { (x, y) -> Position(x, if (reflect) 6 - y else y) }, start, extraction, challengeLevel))
    }

    private fun findPath(random: Random, count: Int, start: Position, extraction: Position, width: Int = MAX_GENERATED_BOARD_WIDTH, height: Int = BOARD_HEIGHT): List<Position>? {
        val board = BoardState(width = width, height = height, start = start, extraction = extraction)
        val path = mutableListOf<Position>()
        var budget = 12000
        fun visit(current: Position): Boolean {
            if (--budget <= 0) return false
            if (path.size == count * 2) return path.takeLast(2).any { board.extraction in it.neighbors(board.width, board.height) }
            val remaining = count * 2 - path.size
            if (kotlin.math.abs(current.x - board.extraction.x) + kotlin.math.abs(current.y - board.extraction.y) > remaining + 1) return false
            for (next in current.neighbors(board.width, board.height).shuffled(random)) {
                if (next == board.start || next == board.extraction || next in path) continue
                // Only the final domino may touch extraction.
                if (path.size < count * 2 - 2 && board.extraction in next.neighbors(width, height)) continue
                path.add(next)
                if (visit(next)) return true
                path.removeAt(path.lastIndex)
            }
            return false
        }
        return if (visit(board.start)) path.toList() else null
    }

    private fun buildLevel(seed: Long, random: Random, path: List<Position>, start: Position = Position(0, BOARD_HEIGHT / 2), extraction: Position = Position(BOARD_WIDTH - 1, BOARD_HEIGHT / 2), challengeLevel: Int? = null, scenario: Scenario? = null): GeneratedLevel? {
        // Fit the generated witness while varying the playable footprint per seed.
        val width = scenario?.width ?: when {
            path.all { it.x < MIN_GENERATED_BOARD_WIDTH } && extraction.x < MIN_GENERATED_BOARD_WIDTH -> MIN_GENERATED_BOARD_WIDTH
            path.all { it.x < 9 } && extraction.x < 9 -> 9
            else -> MAX_GENERATED_BOARD_WIDTH
        }
        val height = scenario?.height ?: if (path.all { it.y < 6 } && extraction.y < 6) 6 else 7
        if (start.x !in 0 until width || extraction.x !in 0 until width || start.y !in 0 until height || extraction.y !in 0 until height) return null
        val base = BoardState(width = width, height = height, start = start, extraction = extraction)
        val positions = path + base.start + base.extraction
        val parent = IntArray(positions.size) { it }
        fun root(index: Int): Int {
            var current = index
            while (parent[current] != current) current = parent[current]
            return current
        }
        // External contacts must match; the two ports inside a domino need not.
        for (i in positions.indices) for (j in 0 until i) {
            if (i < path.size && j < path.size && i / 2 == j / 2) continue
            if (positions[j] in positions[i].neighbors(width, height)) parent[root(i)] = root(j)
        }
        val startRoot = root(path.size)
        val exitRoot = root(path.size + 1)
        if (startRoot == exitRoot) return null
        val values = positions.indices.map(::root).distinct().associateWith {
            when (it) { startRoot -> 0; exitRoot -> 6; else -> random.nextInt(0, 7) }
        }
        val route = path.chunked(2).mapIndexed { index, pair ->
            val horizontal = pair[0].y == pair[1].y
            val ordered = if (horizontal) pair.sortedBy { it.x } else pair.sortedBy { it.y }
            val domino = Domino("hardware-$index", values.getValue(root(path.indexOf(ordered[0]))), values.getValue(root(path.indexOf(ordered[1]))))
            PlacedDomino(domino, ordered[0], if (horizontal) Orientation.HORIZONTAL else Orientation.VERTICAL)
        }
        val available = (0 until height).flatMap { y -> (0 until width).map { x -> Position(x, y) } }
            .filter { it !in positions }.shuffled(random)
        val firewallCount = scenario?.firewalls ?: random.nextInt(4 + (challengeLevel ?: 1) / 3, 8 + (challengeLevel ?: 1) / 2).coerceAtMost(available.size)
        val board = base.copy(
            firewalls = available.take(firewallCount).toSet(),
            honeypots = available.drop(firewallCount).take(scenario?.traps ?: random.nextInt(2, 4 + (challengeLevel ?: 0) / 3)).toSet(),
            daemon = Daemon(base.extraction),
            buffs = if (scenario != null) {
                available.drop(firewallCount + scenario.traps).take(2).mapIndexed { index, position ->
                    position to if (index == 0) BoardBuff.TRACE_COOLER else BoardBuff.RAM_RESERVE
                }.toMap()
            } else emptyMap(),
        )
        val decoys = (0..6).flatMap { a -> (a..6).map { b -> a to b } }.shuffled(random).take(12)
            .mapIndexed { index, (a, b) -> Domino("hardware-${route.size + index}", a, b) }
        val flipped = route.map { if (random.nextBoolean()) it.domino.rotated() else it.domino }
        val bag = (listOf(flipped.first()) + decoys.take(2)).shuffled(random) + flipped.drop(1) + decoys.drop(2)
        val scripts = (0..2).flatMap { cycle -> ScriptType.entries.map { ScriptCard("$cycle-${it.name}", it) } }.shuffled(random)
        val state = GameState(
            seed = seed,
            board = board,
            dominoHand = bag.take(DOMINO_HAND_SIZE),
            dominoBag = bag.drop(DOMINO_HAND_SIZE),
            scriptHand = scripts.take(2),
            scriptDeck = scripts.drop(2),
            challengeRules = challengeLevel?.let { ChallengeCatalog.level(it)?.rules },
        )
        val actions = route.mapIndexed { index, tile -> GameAction.PlaceDomino(tile.domino.id, tile.origin, tile.orientation, flipped[index] != tile.domino) }
        var replay = state
        actions.forEach { action ->
            val placed = GameEngine.reduce(replay, action)
            if (placed.events.any { it is GameEvent.Rejected }) return null
            replay = GameEngine.reduce(placed.state, GameAction.EndTurn).state
        }
        return if (replay.result == GameResult.VICTORY) GeneratedLevel(state, actions) else null
    }
}
