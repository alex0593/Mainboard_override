package com.aela.mainboardoverride.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.aela.mainboardoverride.R
import com.aela.mainboardoverride.domain.Domino
import com.aela.mainboardoverride.domain.Orientation

private val dominoResources = arrayOf(
    intArrayOf(R.drawable.domino_0_0, R.drawable.domino_0_1, R.drawable.domino_0_2, R.drawable.domino_0_3, R.drawable.domino_0_4, R.drawable.domino_0_5, R.drawable.domino_0_6),
    intArrayOf(R.drawable.domino_1_1, R.drawable.domino_1_2, R.drawable.domino_1_3, R.drawable.domino_1_4, R.drawable.domino_1_5, R.drawable.domino_1_6),
    intArrayOf(R.drawable.domino_2_2, R.drawable.domino_2_3, R.drawable.domino_2_4, R.drawable.domino_2_5, R.drawable.domino_2_6),
    intArrayOf(R.drawable.domino_3_3, R.drawable.domino_3_4, R.drawable.domino_3_5, R.drawable.domino_3_6),
    intArrayOf(R.drawable.domino_4_4, R.drawable.domino_4_5, R.drawable.domino_4_6),
    intArrayOf(R.drawable.domino_5_5, R.drawable.domino_5_6),
    intArrayOf(R.drawable.domino_6_6),
)

internal fun dominoResource(first: Int, second: Int): Int {
    val low = minOf(first, second)
    return dominoResources[low][maxOf(first, second) - low]
}

/** Kenney sprites put the smaller value on top. Preserve the engine's port order. */
internal fun dominoAngle(first: Int, second: Int, orientation: Orientation): Float =
    (if (orientation == Orientation.HORIZONTAL) -90f else 0f) +
        (if (first > second) 180f else 0f)

@Composable
internal fun DominoImage(
    tile: Domino,
    modifier: Modifier = Modifier,
    orientation: Orientation = Orientation.HORIZONTAL,
    describe: Boolean = true,
) {
    val painter = painterResource(dominoResource(tile.first, tile.second))
    val label = stringResource(R.string.domino_description, tile.first, tile.second)
    val horizontal = orientation == Orientation.HORIZONTAL
    Canvas(modifier.aspectRatio(if (horizontal) 2f else .5f)
        .then(if (describe) Modifier.semantics { contentDescription = label } else Modifier)) {
        val shortSide = if (horizontal) minOf(size.height, size.width / 2) else minOf(size.width, size.height / 2)
        val spriteSize = Size(shortSide, shortSide * 2)
        withTransform({
            translate(size.width / 2, size.height / 2)
            rotate(dominoAngle(tile.first, tile.second, orientation), pivot = androidx.compose.ui.geometry.Offset.Zero)
            translate(-spriteSize.width / 2, -spriteSize.height / 2)
        }) {
            with(painter) { draw(spriteSize) }
        }
    }
}
