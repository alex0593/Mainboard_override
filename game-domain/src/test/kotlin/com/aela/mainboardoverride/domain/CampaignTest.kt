package com.aela.mainboardoverride.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** E05 campaign phases and achievement evaluation. */
class CampaignTest {
    @Test fun `hundred levels split into five phases of twenty`() {
        assertEquals(0, challengePhase(1))
        assertEquals(0, challengePhase(20))
        assertEquals(1, challengePhase(21))
        assertEquals(1, challengePhase(40))
        assertEquals(2, challengePhase(41))
        assertEquals(2, challengePhase(60))
        assertEquals(3, challengePhase(61))
        assertEquals(3, challengePhase(80))
        assertEquals(4, challengePhase(81))
        assertEquals(4, challengePhase(100))
    }

    @Test fun `achievements unlock by the agreed conditions`() {
        assertEquals(
            setOf("first_victory", "clean_run"),
            earnedAchievements(challenges = 1, scenarios = 0, trace = 40, firstVictory = true),
        )
        assertEquals(
            setOf("five_challenges"),
            earnedAchievements(challenges = 5, scenarios = 0, trace = 72, firstVictory = false),
        )
        assertEquals(
            setOf("explorer"),
            earnedAchievements(challenges = 0, scenarios = 1, trace = 64, firstVictory = false),
        )
        assertTrue(
            earnedAchievements(challenges = 0, scenarios = 0, trace = 41, firstVictory = false).isEmpty(),
        )
    }

    @Test fun `daily goal pays once per calendar day`() {
        assertTrue(dailyGoalEarns(lastClaim = null, today = "2026-09-21"))
        assertTrue(dailyGoalEarns(lastClaim = "2026-09-20", today = "2026-09-21"))
        assertTrue(!dailyGoalEarns(lastClaim = "2026-09-21", today = "2026-09-21"))
    }
}
