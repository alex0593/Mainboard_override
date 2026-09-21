package com.aela.mainboardoverride.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** F01 skin catalog: titanium, jade and ruby PCBs are premium; entry skins stay cheap. */
class SkinCatalogTest {
    @Test fun `titanium jade and ruby pcbs cost the premium price`() {
        for (skin in listOf("titanium", "jade", "ruby")) {
            assertTrue(skin in Rewards.premiumSkins)
            assertTrue(skin in Rewards.purchasableSkins)
            assertEquals(Rewards.SKIN_PRICE, Rewards.skinPrice(skin))
        }
    }

    @Test fun `entry pcbs stay cheap and unknown ids are not purchasable`() {
        for (skin in Rewards.affordableSkins) {
            assertEquals(Rewards.ENTRY_SKIN_PRICE, Rewards.skinPrice(skin))
        }
        assertFalse("unknown" in Rewards.purchasableSkins)
        // Purchasable ids gate PCBs only; domino shells never enter the set.
        assertFalse("obsidian" in Rewards.purchasableSkins)
        assertFalse("titanium" !in Rewards.purchasableSkins && "titanium" in Rewards.affordableSkins)
    }
}
