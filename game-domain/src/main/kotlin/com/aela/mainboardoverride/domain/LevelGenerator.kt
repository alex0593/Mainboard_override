package com.aela.mainboardoverride.domain

import kotlin.random.Random

/** Reproducible networks, with a reducer-verified witness kept outside the game state. */
object LevelGenerator {
    internal data class GeneratedLevel(val state: GameState, val solution: List<GameAction.PlaceDomino>)

    fun generate(seed: Long): GameState = generateVerified(seed).state
    fun generateScenario(seed: Long, scenarioId: String): GameState = generateVerified(seed, scenarioId = scenarioId).state
    fun generateChallenge(level: Int, seed: Long = ChallengeCatalog.level(level)?.seed ?: error("Unknown challenge level $level")): GameState {
        val fallback = ChallengeCatalog.level(level)?.seed ?: error("Unknown challenge level $level")
        var candidate = seed
        // A challenge keeps its rules and level, while a fresh seed may need a few
        // deterministic retries to find a verified witness in the generated geometry.
        repeat(64) {
            runCatching { generateVerified(candidate, level) }.getOrNull()?.let { return it.state }
            candidate += 7919L
        }
        return generateVerified(fallback, level).state
    }

    internal fun generateVerified(seed: Long, challengeLevel: Int? = null, scenarioId: String = "classic"): GeneratedLevel {
        val scenario = ScenarioCatalog.get(scenarioId).takeIf { challengeLevel == null }
        val random = Random(seed)
        if (scenario != null) {
            repeat(512) {
                val start = Position(-1, random.nextInt(1, scenario.height))
                val extraction = Position(scenario.width, random.nextInt(1, scenario.height))
                val path = findPath(random, scenario.route, start, extraction, scenario.width, scenario.height)
                if (path != null) buildLevel(seed, random, path, start, extraction, boardWidth = scenario.width, scenario = scenario)?.let { return it }
            }
            error("Unable to generate scenario ${scenario.id} for $seed")
        }
        val startY = random.nextInt(1, 6)
        val exitY = (startY + random.nextInt(1, 6)) % 6 + 1
        val start = Position(-1, startY)
        // Some seeds use an eight-column board, others nine or ten columns.
        val boardWidth = random.nextInt(MIN_GENERATED_BOARD_WIDTH, MAX_GENERATED_BOARD_WIDTH + 1)
        val extraction = Position(boardWidth, exitY)
        // Challenge routes need enough dominoes to span from start to extraction:
        // the walk must cover (0, startY) through (width - 1, exitY) at minimum.
        val minChallengeCount = if (challengeLevel != null)
            (boardWidth + kotlin.math.abs(extraction.y - start.y) + 1) / 2 else 0
        repeat(24) {
            val base = challengeLevel?.let { (4 + it / 2).coerceIn(5, 9) } ?: random.nextInt(5, 8)
            val count = maxOf(base, minChallengeCount)
            val path = findPath(random, count, start, extraction, boardWidth)
            if (path != null) buildLevel(seed, random, path, start, extraction, challengeLevel, boardWidth = boardWidth)?.let { return it }
        }
        // Deterministic detour built from the actual endpoints, so the first cell
        // always neighbors start and the last always neighbors extraction.
        val fallbackCount = maxOf((4 + (challengeLevel ?: 4) / 2).coerceIn(5, 9), minChallengeCount)
        val fallback = fallbackPath(random, fallbackCount, start, extraction, boardWidth)
        return checkNotNull(fallback?.let { buildLevel(seed, random, it, start, extraction, challengeLevel, boardWidth = boardWidth) })
    }

    private fun findPath(random: Random, count: Int, start: Position, extraction: Position, width: Int = MAX_GENERATED_BOARD_WIDTH, height: Int = BOARD_HEIGHT): List<Position>? {
        val board = BoardState(width = width, height = height, start = start, extraction = extraction)
        val path = mutableListOf<Position>()
        var budget = 12000
        fun visit(current: Position): Boolean {
            if (--budget <= 0) return false
            if (path.size == count * 2) return path.takeLast(2).any { adjacent(it, board.extraction) }
            val remaining = count * 2 - path.size
            if (kotlin.math.abs(current.x - board.extraction.x) + kotlin.math.abs(current.y - board.extraction.y) > remaining + 1) return false
            for (next in current.neighbors(board.width, board.height).shuffled(random)) {
                if (next == board.start || next == board.extraction || next in path) continue
                // Only the final domino may touch extraction.
                if (path.size < count * 2 - 2 && adjacent(next, board.extraction)) continue
                path.add(next)
                if (visit(next)) return true
                path.removeAt(path.lastIndex)
            }
            return false
        }
        return if (visit(board.start)) path.toList() else null
    }

    private fun buildLevel(seed: Long, random: Random, path: List<Position>, start: Position = Position(-1, BOARD_HEIGHT / 2), extraction: Position = Position(BOARD_WIDTH, BOARD_HEIGHT / 2), challengeLevel: Int? = null, boardWidth: Int? = null, scenario: Scenario? = null): GeneratedLevel? {
        // Fit the generated witness while varying the playable footprint per seed.
        val width = boardWidth ?: scenario?.width ?: MAX_GENERATED_BOARD_WIDTH
        val height = scenario?.height ?: if (path.all { it.y < 6 } && extraction.y < 6) 6 else 7
        if (start.x !in -1..width || extraction.x !in -1..width || start.y !in 0 until height || extraction.y !in 0 until height) return null
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
            if (adjacent(positions[j], positions[i])) parent[root(i)] = root(j)
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
        // Locks reinforce the reference corridor in free play: a cell beside the
        // witness path demands its neighbor's value, so the reference replay
        // satisfies it while deviations risk defeat. A side stream keeps the main
        // shuffle untouched; challenges stay lock-free.
        val pathValues = buildMap<Position, Int> {
            route.forEach { tile -> tile.positions.toList().forEach { put(it, tile.valueAt(it)!!) } }
        }
        val lockCell = if (challengeLevel == null) {
            available.filter { cell -> path.count { adjacent(it, cell) } == 1 }
                .randomOrNull(kotlin.random.Random(seed xor 0x9E3779B9L))?.let { cell ->
                    cell to pathValues.getValue(path.first { adjacent(it, cell) })
                }
        } else null
        val open = if (lockCell == null) available else available - lockCell.first
        val firewallCount = scenario?.firewalls ?: random.nextInt(4 + (challengeLevel ?: 1) / 3, 8 + (challengeLevel ?: 1) / 2).coerceAtMost(open.size)
        val board = base.copy(
            firewalls = open.take(firewallCount).toSet(),
            honeypots = open.drop(firewallCount).take(scenario?.traps ?: random.nextInt(2, 4 + (challengeLevel ?: 0) / 3)).toSet(),
            daemon = Daemon(base.extraction),
            buffs = if (scenario != null) {
                open.drop(firewallCount + scenario.traps).take(2).mapIndexed { index, position ->
                    position to if (index == 0) BoardBuff.TRACE_COOLER else BoardBuff.RAM_RESERVE
                }.toMap()
            } else emptyMap(),
            locks = if (lockCell == null) emptyMap() else mapOf(lockCell),
        )
        val decoys = (0..6).flatMap { a -> (a..6).map { b -> a to b } }.shuffled(random).take(12)
            .mapIndexed { index, (a, b) -> Domino("hardware-${route.size + index}", a, b) }
        val flipped = route.map { if (random.nextBoolean()) it.domino.rotated() else it.domino }
        val bag = (listOf(flipped.first()) + decoys.take(2)).shuffled(random) + flipped.drop(1) + decoys.drop(2)
        val banned = challengeLevel?.let { ChallengeCatalog.level(it)?.rules?.bannedScripts } ?: emptySet()
        val scripts = (0..2).flatMap { cycle ->
            ScriptType.entries.filter { it !in banned }.map { ScriptCard("$cycle-${it.name}", it) }
        }.shuffled(random)
        val state = GameState(
            seed = seed,
            board = board,
            dominoHand = bag.take(DOMINO_HAND_SIZE),
            dominoBag = bag.drop(DOMINO_HAND_SIZE),
            scriptHand = scripts.take(STARTING_SCRIPT_HAND),
            scriptDeck = scripts.drop(STARTING_SCRIPT_HAND),
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

    private fun adjacent(first: Position, second: Position): Boolean =
        kotlin.math.abs(first.x - second.x) + kotlin.math.abs(first.y - second.y) == 1

    /**
     * Deterministic fallback walk with exactly [count] dominoes from the only
     * start neighbor (0, startY) to the only extraction neighbor (width - 1,
     * exitY). An L-shaped spine covers the span; paired side-steps grow it to
     * the requested length without revisiting cells. Returns null when no
     * detour fits, letting the caller surface the failure instead of emitting
     * a disconnected board.
     */
    private fun fallbackPath(random: Random, count: Int, start: Position, extraction: Position, width: Int): List<Position>? {
        val spine = mutableListOf<Position>()
        if (random.nextBoolean()) {
            for (x in 0 until width) spine += Position(x, start.y)
            val step = if (extraction.y >= start.y) 1 else -1
            var y = start.y
            while (y != extraction.y) {
                y += step
                spine += Position(width - 1, y)
            }
        } else {
            spine += Position(0, start.y)
            val step = if (extraction.y >= start.y) 1 else -1
            var y = start.y
            while (y != extraction.y) {
                y += step
                spine += Position(0, y)
            }
            for (x in 1 until width) spine += Position(x, extraction.y)
        }
        var target = count * 2
        if (spine.size > target) target = spine.size + (spine.size % 2)
        val path = spine.toMutableList()
        var guard = 0
        while (path.size < target && guard++ < 1000) {
            var grew = false
            for (i in 0 until path.size - 1) {
                if (path.size >= target) break
                val a = path[i]
                val b = path[i + 1]
                val perps = if (b.x != a.x) listOf(Position(0, 1), Position(0, -1))
                else listOf(Position(1, 0), Position(-1, 0))
                val sides = if (random.nextBoolean()) perps else perps.reversed()
                for (side in sides) {
                    val first = Position(a.x + side.x, a.y + side.y)
                    val second = Position(b.x + side.x, b.y + side.y)
                    if (first.x !in 0 until width || first.y !in 0..BOARD_HEIGHT - 1) continue
                    if (second.x !in 0 until width || second.y !in 0..BOARD_HEIGHT - 1) continue
                    if (first in path || second in path) continue
                    if (first == start || first == extraction || second == start || second == extraction) continue
                    path.add(i + 1, second)
                    path.add(i + 1, first)
                    grew = true
                    break
                }
                if (grew) break
            }
            if (!grew) return null
        }
        return path.takeIf { it.size == target }
    }
}
