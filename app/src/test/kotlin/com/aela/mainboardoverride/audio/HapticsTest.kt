package com.aela.mainboardoverride.audio

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class HapticsTest {
    @Test fun informationalCuesStayQuiet() {
        for (cue in listOf(SoundCue.Ping, SoundCue.Turn, SoundCue.Boot, SoundCue.Coin)) {
            assertNull(hapticPattern(cue), "$cue must not vibrate")
        }
    }

    @Test fun tactileCuesUseValidWaveforms() {
        val quiet = setOf(SoundCue.Ping, SoundCue.Turn, SoundCue.Boot, SoundCue.Coin)
        for (cue in SoundCue.entries - quiet) {
            val pattern = hapticPattern(cue) ?: error("$cue must vibrate")
            assertEquals(pattern.timings.size, pattern.amplitudes.size, "$cue spans mismatch")
            assertTrue(pattern.timings.size >= 2, "$cue needs at least one span")
            assertEquals(0L, pattern.timings[0], "$cue must start immediately")
            assertTrue(pattern.timings.all { it >= 0 }, "$cue negative timing")
            assertTrue(pattern.amplitudes.all { it in 0..255 }, "$cue amplitude out of range")
            assertTrue(pattern.amplitudes.drop(1).any { it > 0 }, "$cue never buzzes")
        }
    }

    @Test fun defeatsHitHarderThanTicks() {
        val tick = hapticPattern(SoundCue.Tick) ?: error("tick")
        val defeat = hapticPattern(SoundCue.Defeat) ?: error("defeat")
        assertTrue(defeat.timings.sum() > tick.timings.sum())
        assertTrue(defeat.amplitudes.max() > tick.amplitudes.max())
    }
}
