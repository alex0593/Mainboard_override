package com.aela.mainboardoverride.ui

import com.aela.mainboardoverride.domain.Rewards
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** Only the 200-credit skins ship an FX WebP; free, 80-credit, nebula and unknown ids stay static. */
class FxWebpResourcesTest {
    @Test fun everyPremiumSkinHasAnFxWebp() {
        for (id in Rewards.premiumSkins) {
            assertNotNull("fx webp for $id", fxResource(id))
            assertNotEquals(0, fxResource(id))
        }
    }

    @Test fun nonPremiumSkinsGetNoFxWebp() {
        val nonPremium = (Rewards.boardSkinIds - Rewards.premiumSkins) + (Rewards.dominoSkinIds - Rewards.premiumSkins)
        for (id in nonPremium) {
            assertNull("non-premium skin $id must not have an fx webp", fxResource(id))
        }
        assertNull(fxResource("nebula"))
        assertNull(fxResource("unknown"))
    }

    @Test fun fxResourceMirrorsThePremiumFxCatalog() {
        assertEquals(Rewards.premiumSkins, Rewards.premiumSkins.filter { fxResource(it) != null }.toSet())
    }

    @Test fun ambientMotesWebpExists() {
        assertNotEquals(0, ambientMotesResource())
    }
}
