package com.aela.mainboardoverride.ui

import com.aela.mainboardoverride.domain.Rewards
import org.junit.Assert.assertEquals
import org.junit.Test

/** The label lists are the catalog the store sells; the domain holds the same ids for its rules. */
class SkinCatalogListsTest {
    @Test fun appSkinListsMatchTheDomainCatalog() {
        assertEquals(Rewards.boardSkinIds, boardSkins.map { it.id }.toSet())
        assertEquals(Rewards.dominoSkinIds, dominoSkins.map { it.id }.toSet())
    }

    @Test fun theFirstThreeEntriesOfEachListAreTheFreeOnes() {
        assertEquals(Rewards.freeBoardSkins, boardSkins.take(3).map { it.id }.toSet())
        assertEquals(Rewards.freeDominoSkins, dominoSkins.take(3).map { it.id }.toSet())
    }
}
