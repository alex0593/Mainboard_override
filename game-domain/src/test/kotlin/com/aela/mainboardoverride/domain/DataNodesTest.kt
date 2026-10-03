package com.aela.mainboardoverride.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

/** J03 data nodes: extraction alone never wins while a node stays outside the network. */
class DataNodesTest {
    private fun replay(state: GameState, solution: List<GameAction.PlaceDomino>): GameState {
        var current = state
        for (action in solution) {
            current = GameEngine.reduce(GameEngine.reduce(current, action).state, GameAction.EndTurn).state
        }
        return current
    }

    @Test fun `node counts scale by band and stay off below level forty-one`() {
        assertEquals(0, dataNodeCount(1))
        assertEquals(0, dataNodeCount(40))
        assertEquals(1, dataNodeCount(41))
        assertEquals(1, dataNodeCount(60))
        assertEquals(2, dataNodeCount(61))
        assertEquals(2, dataNodeCount(80))
        assertEquals(3, dataNodeCount(81))
        assertEquals(3, dataNodeCount(100))
    }

    @Test fun `boards grow by band and only challenges from forty-one carry nodes`() {
        for (number in listOf(1, 15, 30)) {
            val generated = LevelGenerator.generateVerified(ChallengeCatalog.level(number)!!.seed, number)
            assertTrue(generated.state.board.width in 8..10, "challenge $number width ${generated.state.board.width}")
            assertTrue(generated.state.board.waypoints.isEmpty(), "challenge $number carries nodes")
        }
        for (number in listOf(31, 40)) {
            val generated = LevelGenerator.generateVerified(ChallengeCatalog.level(number)!!.seed, number)
            assertTrue(generated.state.board.width in 11..12, "challenge $number width ${generated.state.board.width}")
            assertTrue(generated.state.board.waypoints.isEmpty(), "challenge $number carries nodes")
        }
        for (number in listOf(45, 60)) {
            val generated = LevelGenerator.generateVerified(ChallengeCatalog.level(number)!!.seed, number)
            assertTrue(generated.state.board.width in 11..12, "challenge $number width ${generated.state.board.width}")
            assertEquals(dataNodeCount(number), generated.state.board.waypoints.size, "challenge $number nodes")
        }
        for (number in listOf(61, 75, 81, 100)) {
            val generated = LevelGenerator.generateVerified(ChallengeCatalog.level(number)!!.seed, number)
            assertTrue(generated.state.board.width in 13..14, "challenge $number width ${generated.state.board.width}")
            assertEquals(dataNodeCount(number), generated.state.board.waypoints.size, "challenge $number nodes")
        }
    }

    @Test fun `generated nodes sit off the traps and are swept by the reference route`() {
        for (number in listOf(41, 61, 81, 100)) {
            val generated = LevelGenerator.generateVerified(ChallengeCatalog.level(number)!!.seed, number)
            val nodes = generated.state.board.waypoints
            assertTrue(nodes.none { it in generated.state.board.firewalls }, "challenge $number node on firewall")
            assertTrue(nodes.none { it in generated.state.board.honeypots }, "challenge $number node on honeypot")
            val end = replay(generated.state, generated.solution)
            assertEquals(GameResult.VICTORY, end.result, "challenge $number")
            assertEquals(nodes, GameEngine.waypointsCovered(end.board), "challenge $number sweep")
        }
    }

    @Test fun `extraction withholds victory while a data node is unswept`() {
        val generated = LevelGenerator.generateVerified(4242L)
        // A firewall cell is never covered by the reference route, so this node stays pending.
        val pending = generated.state.board.firewalls.first()
        val hoarded = generated.state.copy(board = generated.state.board.copy(waypoints = setOf(pending)))
        assertNotEquals(GameResult.VICTORY, replay(hoarded, generated.solution).result)

        // The same reference route wins as soon as the node sits on a cell it sweeps.
        val end = replay(generated.state, generated.solution)
        assertEquals(GameResult.VICTORY, end.result)
        val swept = end.board.placed.first().positions.first
        val routed = generated.state.copy(board = generated.state.board.copy(waypoints = setOf(swept)))
        assertEquals(GameResult.VICTORY, replay(routed, generated.solution).result)
        assertEquals(setOf(swept), GameEngine.waypointsCovered(replay(routed, generated.solution).board))
    }
}
