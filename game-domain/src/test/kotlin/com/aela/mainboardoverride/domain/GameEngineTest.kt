package com.aela.mainboardoverride.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class GameEngineTest {
    @Test fun `same seed produces the same level`() {
        assertEquals(LevelGenerator.generate(42), LevelGenerator.generate(42))
        assertNotEquals(LevelGenerator.generate(42).dominoBag, LevelGenerator.generate(43).dominoBag)
    }

    @Test fun `first tile must connect to start with matching value`() {
        val board = BoardState()
        val route = Domino("route", 0, 3)
        assertTrue(GameEngine.canPlace(board, route, Position(1, 3), Orientation.HORIZONTAL))
        assertFalse(GameEngine.canPlace(board, route, Position(4, 3), Orientation.HORIZONTAL))
        assertFalse(GameEngine.canPlace(board, route.rotated(), Position(1, 3), Orientation.HORIZONTAL))
    }

    @Test fun `external mismatches reject placement but domino halves remain internally connected`() {
        val board = BoardState(placed = listOf(PlacedDomino(Domino("existing", 0, 2), Position(1, 3), Orientation.HORIZONTAL)))
        assertTrue(GameEngine.canPlace(board, Domino("match", 2, 5), Position(3, 3), Orientation.HORIZONTAL))
        assertFalse(GameEngine.canPlace(board, Domino("mismatch", 4, 5), Position(3, 3), Orientation.HORIZONTAL))
    }

    @Test fun `generated networks are varied reproducible and winnable across a thousand seeds`() {
        val layouts = mutableSetOf<Set<Position>>()
        val lengths = mutableSetOf<Int>()
        for (seed in 0L until 1000L) {
            val generated = LevelGenerator.generateVerified(seed)
            assertEquals(generated, LevelGenerator.generateVerified(seed))
            var state = generated.state
            layouts += state.board.firewalls
            lengths += generated.solution.size
            assertTrue(state.board.firewalls.size in 4..7)
            assertTrue(state.board.honeypots.size in 2..4)
            assertTrue(state.board.firewalls.intersect(state.board.honeypots).isEmpty())
            assertTrue((state.board.firewalls + state.board.honeypots).none {
                it == state.board.start || it == state.board.extraction || it.x !in 0 until state.board.width || it.y !in 0 until state.board.height
            })
            assertTrue(generated.solution.size in 5..7)
            for (action in generated.solution) {
                val placed = GameEngine.reduce(state, action)
                assertTrue(placed.events.none { it is GameEvent.Rejected }, "seed $seed rejected $action")
                state = GameEngine.reduce(placed.state, GameAction.EndTurn).state
            }
            assertEquals(GameResult.VICTORY, state.result, "seed $seed")
            assertEquals(generated.solution.size * 8, state.trace)
        }
        assertTrue(layouts.size > 900)
        assertEquals(setOf(5, 6, 7), lengths)
    }

    @Test fun `ping consumes ram and reveals traps`() {
        val generated = LevelGenerator.generate(7)
        val ping = ScriptCard("test-ping", ScriptType.PING)
        val result = GameEngine.reduce(generated.copy(scriptHand = listOf(ping)), GameAction.PlayPing(ping.id)).state
        assertEquals(2, result.ram)
        assertEquals(result.board.honeypots, result.board.revealedHoneypots)
        assertEquals(result.dominoBag.take(3), result.pingPreview)
    }

    @Test fun `spoof permanently changes one held port`() {
        val generated = LevelGenerator.generate(8)
        val spoof = ScriptCard("test-spoof", ScriptType.SPOOF)
        val target = generated.dominoHand.first()
        val result = GameEngine.reduce(
            generated.copy(scriptHand = listOf(spoof)),
            GameAction.PlaySpoof(spoof.id, target.id, 0, 6),
        ).state
        assertEquals(6, result.dominoHand.first { it.id == target.id }.first)
        assertEquals(1, result.ram)
        assertEquals(5, result.pendingNoise)
    }

    @Test fun `automatic resolution ends a blocked action phase`() {
        val blocked = routeFixture().copy(
            dominoHand = listOf(Domino("blocked", 1, 1)),
            dominoBag = listOf(Domino("future", 1, 1)),
            scriptHand = emptyList(),
        )
        val result = GameEngine.resolveForcedResult(blocked)
        assertEquals(GameResult.KERNEL_PANIC, result.state.result)
        assertTrue(result.events.any { it == GameEvent.Finished(GameResult.KERNEL_PANIC) })

        val spoofEscape = blocked.copy(scriptHand = listOf(ScriptCard("spoof", ScriptType.SPOOF)))
        assertEquals(null, GameEngine.resolveForcedResult(spoofEscape).state.result)
    }

    @Test fun `loss takes priority over extraction when trace reaches maximum`() {
        var state = routeFixture()
        val placements = listOf(
            Position(1, 3) to Orientation.HORIZONTAL,
            Position(3, 3) to Orientation.HORIZONTAL,
            Position(5, 3) to Orientation.HORIZONTAL,
            Position(7, 2) to Orientation.VERTICAL,
        )
        placements.forEachIndexed { index, target ->
            state = GameEngine.reduce(state, GameAction.PlaceDomino("route-${index + 1}", target.first, target.second)).state
            if (index < placements.lastIndex) state = GameEngine.reduce(state, GameAction.EndTurn).state
        }
        state = state.copy(trace = 99)
        assertEquals(GameResult.TRACE_INTERCEPTED, GameEngine.reduce(state, GameAction.EndTurn).state.result)
    }

    @Test fun `challenge rules allow the exact limit and fail when trace is exceeded`() {
        val rules = ChallengeRules(maxTrace = 48)
        val state = routeFixture().copy(challengeRules = rules, trace = 40)
        val placed = GameEngine.reduce(state, GameAction.PlaceDomino("route-1", Position(1, 3), Orientation.HORIZONTAL)).state
        val result = GameEngine.reduce(placed, GameAction.EndTurn).state
        assertNotEquals(GameResult.CHALLENGE_LIMIT, result.result)

        val overLimit = placed.copy(trace = 41)
        assertEquals(GameResult.CHALLENGE_LIMIT, GameEngine.reduce(overLimit, GameAction.EndTurn).state.result)
    }

    @Test fun `challenge turn limit fails only when extraction is not reached`() {
        val rules = ChallengeRules(maxTurns = 1)
        val state = routeFixture().copy(challengeRules = rules)
        val placed = GameEngine.reduce(state, GameAction.PlaceDomino("route-1", Position(1, 3), Orientation.HORIZONTAL)).state
        assertEquals(GameResult.CHALLENGE_LIMIT, GameEngine.reduce(placed, GameAction.EndTurn).state.result)
    }

    @Test fun `challenge catalog contains thirty constrained solvable levels`() {
        assertEquals(30, ChallengeCatalog.COUNT)
        assertTrue(ChallengeCatalog.levels.all { it.rules.maxTurns != null || it.rules.maxTrace != null })
        assertEquals(30, ChallengeCatalog.levels.distinctBy { it.seed }.size)
        for (level in ChallengeCatalog.levels) {
            val generated = LevelGenerator.generateVerified(level.seed, level.number)
            var state = generated.state
            for (action in generated.solution) {
                state = GameEngine.reduce(GameEngine.reduce(state, action).state, GameAction.EndTurn).state
            }
            assertEquals(GameResult.VICTORY, state.result, "challenge ${level.number}")
        }
    }

    @Test fun `every scenario is reproducible solvable and respects its profile`() {
        for (scenario in ScenarioCatalog.all.drop(1)) for (seed in 0L until 100L) {
            val generated = LevelGenerator.generateVerified(seed, scenarioId = scenario.id)
            assertEquals(generated, LevelGenerator.generateVerified(seed, scenarioId = scenario.id))
            assertEquals(scenario.width, generated.state.board.width)
            assertEquals(scenario.height, generated.state.board.height)
            assertEquals(scenario.firewalls, generated.state.board.firewalls.size)
            assertEquals(scenario.traps, generated.state.board.honeypots.size)
            assertEquals(2, generated.state.board.buffs.size)
            assertEquals(scenario.route, generated.solution.size)
            var state = generated.state
            for (action in generated.solution) {
                val transition = GameEngine.reduce(state, action)
                assertTrue(transition.events.none { it is GameEvent.Rejected })
                state = GameEngine.reduce(transition.state, GameAction.EndTurn).state
            }
            assertEquals(GameResult.VICTORY, state.result, "${scenario.id}: $seed")
        }
    }

    @Test fun `free mode buffs are collected once and apply bounded effects`() {
        val base = routeFixture()
        val tile = base.dominoHand.first()
        val target = base.board.start.neighbors().first()
        val buff = target
        val state = base.copy(
            trace = 4,
            ram = MAX_RAM,
            board = base.board.copy(buffs = mapOf(buff to BoardBuff.TRACE_COOLER)),
            dominoHand = listOf(tile.copy(first = 0, second = 0)),
        )
        val action = GameAction.PlaceDomino(tile.id, target, Orientation.HORIZONTAL)
        val transition = GameEngine.reduce(state, action)
        assertTrue(transition.events.any { it == GameEvent.BuffCollected(BoardBuff.TRACE_COOLER) })
        assertEquals(0, transition.state.trace)
        assertTrue(buff in transition.state.board.collectedBuffs)
        val repeated = GameEngine.reduce(transition.state.copy(tilePlacedThisTurn = false), action)
        assertTrue(repeated.events.none { it is GameEvent.BuffCollected })
    }

    private fun routeFixture(): GameState = GameState(
        seed = 42L,
        board = BoardState(
            firewalls = setOf(Position(4, 1), Position(4, 5)),
            honeypots = setOf(Position(2, 1), Position(6, 5)),
            daemon = Daemon(Position(BOARD_WIDTH - 1, BOARD_HEIGHT / 2)),
        ),
        dominoHand = listOf(
            Domino("route-1", 0, 1),
            Domino("route-2", 1, 2),
            Domino("route-3", 2, 6),
            Domino("route-4", 3, 6),
        ),
        dominoBag = emptyList(),
        scriptHand = emptyList(),
        scriptDeck = emptyList(),
    )
}
