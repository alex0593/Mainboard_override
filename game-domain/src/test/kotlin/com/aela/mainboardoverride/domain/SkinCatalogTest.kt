package com.aela.mainboardoverride.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** K01 store contract: three free skins per category, per-category payment state, two price tiers. */
class SkinCatalogTest {
    @Test fun `free skins are exactly the first three of each catalog`() {
        assertEquals(setOf("pcb", "blueprint", "industrial"), Rewards.freeBoardSkins)
        assertEquals(setOf("kenney", "dark", "gingerbread"), Rewards.freeDominoSkins)
        assertTrue(Rewards.freeBoardSkins.all { it in Rewards.boardSkinIds })
        assertTrue(Rewards.freeDominoSkins.all { it in Rewards.dominoSkinIds })
    }

    @Test fun `payment state is per category and unknown ids are never paid`() {
        assertFalse(Rewards.isPaid(true, "blueprint"))
        assertTrue(Rewards.isPaid(false, "blueprint"))
        assertFalse(Rewards.isPaid(true, "pcb"))
        assertFalse(Rewards.isPaid(false, "kenney"))
        assertTrue(Rewards.isPaid(true, "rust"))
        assertTrue(Rewards.isPaid(false, "hearts"))
        assertFalse(Rewards.isPaid(true, "unknown"))
        assertFalse(Rewards.isPaid(false, "unknown"))
    }

    @Test fun `premium skins cost 200 and the rest of the paid catalog costs 80`() {
        for (skin in listOf("copper", "aurora", "titanium", "jade", "ruby", "sapphire", "amber", "amethyst", "biolum", "prisma", "quantum")) {
            assertTrue(skin in Rewards.premiumSkins)
            assertEquals(Rewards.SKIN_PRICE, Rewards.skinPrice(skin))
        }
        for (skin in listOf("graphite", "signal", "rust", "ice", "obsidian", "ceramic", "hearts", "stars", "circuit", "blueprint")) {
            assertEquals(Rewards.ENTRY_SKIN_PRICE, Rewards.skinPrice(skin))
        }
    }

    @Test fun `ownership keys are namespaced per category`() {
        assertEquals("board:copper", Rewards.ownedKey(true, "copper"))
        assertEquals("domino:hearts", Rewards.ownedKey(false, "hearts"))
    }
}
