package com.aela.mainboardoverride.ui

import com.aela.mainboardoverride.domain.Rewards
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

/** Only the eleven 200-credit skins carry premium fx; free, 80-credit, nebula and unknown ids stay static. */
class PremiumSkinFxTest {
    @Test fun everyPremiumSkinResolvesAnFx() {
        for (id in Rewards.premiumSkins) {
            assertNotNull("premium skin $id must have fx", premiumSkinFx(id))
        }
    }

    @Test fun nonPremiumSkinsGetNoFx() {
        val nonPremium = (Rewards.boardSkinIds - Rewards.premiumSkins) + (Rewards.dominoSkinIds - Rewards.premiumSkins)
        for (id in nonPremium) {
            assertNull("non-premium skin $id must not have premium fx", premiumSkinFx(id))
        }
        assertNull(premiumSkinFx("nebula"))
        assertNull(premiumSkinFx("unknown"))
    }

    @Test fun fxAccentMirrorsThePipAccent() {
        for (id in Rewards.premiumSkins) {
            assertEquals(dominoPipColor(id), premiumSkinFx(id)?.accent)
        }
    }

    @Test fun allFourStylesAreUsed() {
        val styles = Rewards.premiumSkins.mapNotNull { premiumSkinFx(it)?.style }.toSet()
        assertEquals(SkinFxStyle.entries.toSet(), styles)
    }

    @Test fun distinctiveSkinsKeepTheirCharacter() {
        assertEquals(SkinFxStyle.DRIFT, premiumSkinFx("aurora")?.style)
        assertEquals(SkinFxStyle.TWINKLE, premiumSkinFx("amethyst")?.style)
        assertEquals(SkinFxStyle.PULSE, premiumSkinFx("ruby")?.style)
        assertEquals(SkinFxStyle.SWEEP, premiumSkinFx("copper")?.style)
    }
}
