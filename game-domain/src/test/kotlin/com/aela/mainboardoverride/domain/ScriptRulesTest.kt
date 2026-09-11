package com.aela.mainboardoverride.domain

import kotlin.test.*

class ScriptRulesTest {
    private fun state(board: BoardState) = GameState(
        seed = 42, board = board, dominoHand = listOf(Domino("next", 3, 6)),
        dominoBag = emptyList(), scriptHand = listOf(ScriptCard("bridge", ScriptType.BRIDGE)),
        scriptDeck = emptyList(),
    )

    @Test fun pingRevealsOneNewHiddenTrapAndOneTileWithoutDrawing() {
        val traps = setOf(Position(2, 2), Position(3, 2), Position(4, 2), Position(5, 2))
        val initial = state(BoardState(honeypots = traps, revealedHoneypots = setOf(Position(2, 2)),
            triggeredHoneypots = setOf(Position(3, 2)))).copy(
            dominoBag = listOf(Domino("a", 1, 2), Domino("b", 2, 3)),
            scriptHand = (1..3).map { ScriptCard("ping-$it", ScriptType.PING) },
        )
        val first = GameEngine.reduce(initial, GameAction.PlayPing("ping-1")).state
        assertEquals(first, GameEngine.reduce(initial, GameAction.PlayPing("ping-1")).state)
        assertEquals(2, first.board.revealedHoneypots.size)
        assertFalse(Position(3, 2) in first.board.revealedHoneypots)
        assertEquals(initial.dominoBag, first.dominoBag)
        assertEquals(initial.dominoBag.take(1), first.pingPreview)
        val second = GameEngine.reduce(first, GameAction.PlayPing("ping-2")).state
        assertEquals(3, second.board.revealedHoneypots.size)
        val third = GameEngine.reduce(second.copy(dominoBag = emptyList()), GameAction.PlayPing("ping-3")).state
        assertEquals(second.board, third.board)
        assertTrue(third.pingPreview.isEmpty())
        assertEquals(0, third.ram)
    }

    @Test fun bridgeCopiesOnePortAndAllowsLaterPlacementFromEitherSideAndAxis() {
        for (horizontal in listOf(true, false)) for (reverse in listOf(true, false)) {
            fun p(x: Int, y: Int) = if (horizontal) Position(x, y) else Position(y, x)
            val sourceX = if (reverse) 4 else 1
            val source = PlacedDomino(Domino("source", if (reverse) 3 else 0, if (reverse) 0 else 3),
                p(sourceX, 3), if (horizontal) Orientation.HORIZONTAL else Orientation.VERTICAL)
            val board = BoardState(width = 8, height = 8, start = p(if (reverse) 6 else 0, 3),
                extraction = p(if (reverse) 0 else 6, 3), placed = listOf(source), firewalls = setOf(p(3, 3)))
            val initial = state(board)
            val bridged = GameEngine.reduce(initial, GameAction.PlayBridge("bridge", p(3, 3), horizontal)).state
            assertEquals(3, bridged.board.bridges.single().value)
            assertEquals(1, bridged.ram)
            assertEquals(10, bridged.pendingNoise)
            assertFalse(GameEngine.isExtractionConnected(bridged.board))
            val origin = p(if (reverse) 1 else 4, 3)
            val orientation = if (horizontal) Orientation.HORIZONTAL else Orientation.VERTICAL
            val tile = if (reverse) Domino("next", 6, 3) else Domino("next", 3, 6)
            assertTrue(GameEngine.canPlace(bridged.board, tile, origin, orientation))
            assertFalse(GameEngine.canPlace(bridged.board, Domino("bad", 2, 2), origin, orientation))
            val placed = GameEngine.reduce(bridged.copy(dominoHand = listOf(tile)), GameAction.PlaceDomino("next", origin, orientation)).state
            assertTrue(GameEngine.isExtractionConnected(placed.board))
            assertEquals(GameResult.VICTORY, GameEngine.reduce(placed, GameAction.EndTurn).state.result)
            assertEquals(bridged, GameEngine.reduce(bridged, GameAction.PlayBridge("bridge", p(3, 3), horizontal)).state)
        }
    }

    @Test fun invalidBridgeTargetsNeverSpendResources() {
        val source = PlacedDomino(Domino("source", 0, 3), Position(1, 3), Orientation.HORIZONTAL)
        val board = BoardState(placed = listOf(source), firewalls = setOf(Position(3, 3)))
        val variants = listOf(
            board.copy(placed = emptyList()),
            board.copy(firewalls = board.firewalls + Position(4, 3)),
            board.copy(placed = board.placed + PlacedDomino(Domino("wrong", 2, 6), Position(4, 3), Orientation.HORIZONTAL)),
            board.copy(width = 4),
        )
        for (variant in variants) {
            val initial = state(variant)
            assertEquals(initial, GameEngine.reduce(initial, GameAction.PlayBridge("bridge", Position(3, 3), true)).state)
        }
    }

    @Test fun bridgeCanPreventPrematureKernelPanic() {
        val board = BoardState(width = 7, height = 1, start = Position(0, 0), extraction = Position(6, 0),
            placed = listOf(PlacedDomino(Domino("source", 0, 3), Position(1, 0), Orientation.HORIZONTAL)),
            firewalls = setOf(Position(3, 0)))
        val initial = state(board)
        assertTrue(GameEngine.legalPlacements(initial).isEmpty())
        assertNull(GameEngine.resolveForcedResult(initial).state.result)
        assertEquals(GameResult.KERNEL_PANIC, GameEngine.resolveForcedResult(initial.copy(ram = 1)).state.result)
    }

    @Test fun bridgeJoinsExistingMatchingPortsAndKillRemovesItsStoredPort() {
        val board = BoardState(width = 7, height = 1, start = Position(0, 0), extraction = Position(6, 0),
            placed = listOf(
                PlacedDomino(Domino("source", 0, 3), Position(1, 0), Orientation.HORIZONTAL),
                PlacedDomino(Domino("destination", 3, 6), Position(4, 0), Orientation.HORIZONTAL),
            ), firewalls = setOf(Position(3, 0)))
        assertFalse(GameEngine.isExtractionConnected(board))
        val bridged = GameEngine.reduce(state(board), GameAction.PlayBridge("bridge", Position(3, 0), true)).state
        assertTrue(GameEngine.isExtractionConnected(bridged.board))
        val killed = GameEngine.reduce(bridged.copy(ram = 3, scriptHand = listOf(ScriptCard("kill", ScriptType.KILL_PROCESS))),
            GameAction.PlayKillProcess("kill", Position(3, 0))).state
        assertTrue(killed.board.bridges.isEmpty())
        assertFalse(killed.board.isOccupied(Position(3, 0)))
        assertFalse(GameEngine.isExtractionConnected(killed.board))
    }
}
