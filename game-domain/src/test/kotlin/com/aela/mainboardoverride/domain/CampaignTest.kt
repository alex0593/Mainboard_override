package com.aela.mainboardoverride.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** E05 campaign phases and achievement evaluation. */
class CampaignTest {
    @Test fun `thirty levels split into three phases`() {
        assertEquals(0, challengePhase(1))
        assertEquals(0, challengePhase(10))
        assertEquals(1, challengePhase(11))
        assertEquals(1, challengePhase(20))
        assertEquals(2, challengePhase(21))
        assertEquals(2, challengePhase(30))
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
