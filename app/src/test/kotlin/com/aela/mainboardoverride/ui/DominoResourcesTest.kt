package com.aela.mainboardoverride.ui

import com.aela.mainboardoverride.domain.Orientation
import androidx.compose.ui.graphics.Color
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class DominoResourcesTest {
    @Test fun everyOrderedPairUsesOneOfThe28UniqueSprites() {
        val resources = (0..6).flatMap { first ->
            (first..6).map { second ->
                dominoResource(first, second).also {
                    assertTrue(it != 0)
                    assertEquals(it, dominoResource(second, first))
                }
            }
        }
        assertEquals(28, resources.toSet().size)
    }

    @Test fun rotationPreservesPortOrderInBothOrientations() {
        assertEquals(-90f, dominoAngle(0, 6, Orientation.HORIZONTAL))
        assertEquals(0f, dominoAngle(0, 6, Orientation.VERTICAL))
        assertEquals(90f, dominoAngle(6, 0, Orientation.HORIZONTAL))
        assertEquals(180f, dominoAngle(6, 0, Orientation.VERTICAL))
        for (value in 0..6) {
            assertEquals(-90f, dominoAngle(value, value, Orientation.HORIZONTAL))
            assertEquals(0f, dominoAngle(value, value, Orientation.VERTICAL))
        }
    }

    @Test fun crystalPackShellsResolveSpritesAndDistinctPipColors() {
        val pack = listOf("sapphire", "amber", "amethyst")
        for (skin in pack) {
            assertNotEquals(null, dominoShellResource(skin))
            assertNotEquals(null, dominoShellResource(skin, previewOnly = true))
            assertNotEquals(null, boardSkinResource(skin))
            assertNotEquals(null, boardSkinResource(skin, previewOnly = true))
        }
        // Light pips over each shell's very dark faces keep contrast AA-readable.
        assertEquals(Color(0xFFBFE7FF), dominoPipColor("sapphire"))
        assertEquals(Color(0xFFFFAB3D), dominoPipColor("amber"))
        assertEquals(Color(0xFFD9B3FF), dominoPipColor("amethyst"))
        val pips = pack.map(::dominoPipColor)
        assertEquals(3, pips.toSet().size)
        val materials = listOf("titanium", "jade", "ruby", "ice", "aurora").map(::dominoPipColor)
        assertTrue(pips.none { it in materials })
    }

    @Test fun lightThemedSkinsUseDistinctReadablePipColors() {
        assertEquals(Color(0xFF075985), dominoPipColor("ice"))
        assertEquals(Color(0xFFFFC107), dominoPipColor("aurora"))
        assertTrue(dominoPipColor("ice") != dominoPipColor("aurora"))
    }

    @Test fun materialShellsResolveSpritesAndDistinctPipColors() {
        for (skin in listOf("titanium", "jade", "ruby")) {
            assertNotEquals(null, dominoShellResource(skin))
            assertNotEquals(null, dominoShellResource(skin, previewOnly = true))
        }
        // Light pips over each shell's very dark faces keep contrast AA-readable.
        assertEquals(Color(0xFFE8F1FF), dominoPipColor("titanium"))
        assertEquals(Color(0xFFFFD43B), dominoPipColor("jade"))
        assertEquals(Color(0xFFFFB3C1), dominoPipColor("ruby"))
        val pips = listOf("titanium", "jade", "ruby").map(::dominoPipColor)
        assertEquals(3, pips.toSet().size)
        assertTrue(pips.none { it == dominoPipColor("ice") || it == dominoPipColor("aurora") })
    }
}
