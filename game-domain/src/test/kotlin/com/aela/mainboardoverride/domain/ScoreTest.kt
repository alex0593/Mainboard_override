package com.aela.mainboardoverride.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** E03 scoring: fewer turns and less trace score higher. */
class ScoreTest {
    @Test fun `reference victories score above half`() {
        assertEquals(1000 - 6 * 40 - 48, Rewards.victoryScore(6, 48))
        assertEquals(1000 - 9 * 40 - 72, Rewards.victoryScore(9, 72))
    }

    @Test fun `score never drops below zero`() {
        assertEquals(0, Rewards.victoryScore(30, 100))
        assertEquals(0, Rewards.victoryScore(99, 99))
    }

    @Test fun `better runs outscore worse ones`() {
        val good = Rewards.victoryScore(5, 40)
        val sloppy = Rewards.victoryScore(9, 80)
        assertTrue(good > sloppy)
        // Same turns: less trace wins; same trace: fewer turns win.
        assertTrue(Rewards.victoryScore(7, 50) > Rewards.victoryScore(7, 60))
        assertTrue(Rewards.victoryScore(6, 60) > Rewards.victoryScore(7, 60))
    }
}
