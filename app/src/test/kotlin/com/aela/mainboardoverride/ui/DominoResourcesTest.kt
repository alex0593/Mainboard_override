package com.aela.mainboardoverride.ui

import com.aela.mainboardoverride.domain.Orientation
import kotlin.test.Test
import kotlin.test.assertEquals
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
}
