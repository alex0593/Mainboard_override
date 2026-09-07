package com.aela.mainboardoverride.domain

import kotlin.random.Random

/** Reproducible networks, with a reducer-verified witness kept outside the game state. */
object LevelGenerator {
    const val TUTORIAL_SEED = 0x4D41494EL
    internal data class GeneratedLevel(val state: GameState, val solution: List<GameAction.PlaceDomino>)

    fun generate(seed: Long): GameState = generateVerified(seed).state

    internal fun generateVerified(seed: Long): GeneratedLevel {
        val random = Random(seed)
        repeat(24) {
            val path = findPath(random, random.nextInt(5, 8))
            if (path != null) buildLevel(seed, random, path)?.let { return it }
        }
        // A five-tile detour; reflections retain valid board geometry.
        val fallback = listOf(1 to 3, 1 to 2, 2 to 2, 3 to 2, 4 to 2, 5 to 2, 6 to 2, 7 to 2, 7 to 3, 7 to 4)
        val reflect = random.nextBoolean()
        return checkNotNull(buildLevel(seed, random, fallback.map { (x, y) -> Position(x, if (reflect) 6 - y else y) }))
    }

    private fun findPath(random: Random, count: Int): List<Position>? {
        val board = BoardState()
        val path = mutableListOf<Position>()
        var budget = 12000
        fun visit(current: Position): Boolean {
            if (--budget <= 0) return false
            if (path.size == count * 2) return path.takeLast(2).any { board.extraction in it.neighbors() }
            val remaining = count * 2 - path.size
            if (kotlin.math.abs(current.x - board.extraction.x) + kotlin.math.abs(current.y - board.extraction.y) > remaining + 1) return false
            for (next in current.neighbors().shuffled(random)) {
                if (next == board.start || next == board.extraction || next in path) continue
                // Only the final domino may touch extraction.
                if (path.size < count * 2 - 2 && board.extraction in next.neighbors()) continue
                path.add(next)
                if (visit(next)) return true
                path.removeAt(path.lastIndex)
            }
            return false
        }
        return if (visit(board.start)) path.toList() else null
    }

    private fun buildLevel(seed: Long, random: Random, path: List<Position>): GeneratedLevel? {
        val base = BoardState()
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
            if (positions[j] in positions[i].neighbors()) parent[root(i)] = root(j)
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
        val available = (0 until BOARD_HEIGHT).flatMap { y -> (0 until BOARD_WIDTH).map { x -> Position(x, y) } }
            .filter { it !in positions }.shuffled(random)
        val firewallCount = random.nextInt(4, 8)
        val board = base.copy(
            firewalls = available.take(firewallCount).toSet(),
            honeypots = available.drop(firewallCount).take(random.nextInt(2, 5)).toSet(),
            daemon = Daemon(base.extraction),
        )
        val decoys = (0..6).flatMap { a -> (a..6).map { b -> a to b } }.shuffled(random).take(12)
            .mapIndexed { index, (a, b) -> Domino("hardware-${route.size + index}", a, b) }
        val flipped = route.map { if (random.nextBoolean()) it.domino.rotated() else it.domino }
        val bag = (listOf(flipped.first()) + decoys.take(2)).shuffled(random) + flipped.drop(1) + decoys.drop(2)
        val scripts = (0..2).flatMap { cycle -> ScriptType.entries.map { ScriptCard("$cycle-${it.name}", it) } }.shuffled(random)
        val state = GameState(seed, board, bag.take(DOMINO_HAND_SIZE), bag.drop(DOMINO_HAND_SIZE), scripts.take(2), scripts.drop(2))
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
