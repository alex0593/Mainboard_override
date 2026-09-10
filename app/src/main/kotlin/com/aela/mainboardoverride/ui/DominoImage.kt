package com.aela.mainboardoverride.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.aela.mainboardoverride.R
import com.aela.mainboardoverride.domain.Domino
import com.aela.mainboardoverride.domain.Orientation
import androidx.compose.ui.unit.dp

private val kenneyDominoResources = arrayOf(
    intArrayOf(R.drawable.domino_0_0, R.drawable.domino_0_1, R.drawable.domino_0_2, R.drawable.domino_0_3, R.drawable.domino_0_4, R.drawable.domino_0_5, R.drawable.domino_0_6),
    intArrayOf(R.drawable.domino_1_1, R.drawable.domino_1_2, R.drawable.domino_1_3, R.drawable.domino_1_4, R.drawable.domino_1_5, R.drawable.domino_1_6),
    intArrayOf(R.drawable.domino_2_2, R.drawable.domino_2_3, R.drawable.domino_2_4, R.drawable.domino_2_5, R.drawable.domino_2_6),
    intArrayOf(R.drawable.domino_3_3, R.drawable.domino_3_4, R.drawable.domino_3_5, R.drawable.domino_3_6),
    intArrayOf(R.drawable.domino_4_4, R.drawable.domino_4_5, R.drawable.domino_4_6),
    intArrayOf(R.drawable.domino_5_5, R.drawable.domino_5_6),
    intArrayOf(R.drawable.domino_6_6),
)

private val themedDominoResources = mapOf(
    // The original bundled Kenney images are the ZIP's Light family.
    "light" to kenneyDominoResources,
    "dark" to arrayOf(
        intArrayOf(R.drawable.domino_dark_0_0, R.drawable.domino_dark_0_1, R.drawable.domino_dark_0_2, R.drawable.domino_dark_0_3, R.drawable.domino_dark_0_4, R.drawable.domino_dark_0_5, R.drawable.domino_dark_0_6),
        intArrayOf(R.drawable.domino_dark_1_1, R.drawable.domino_dark_1_2, R.drawable.domino_dark_1_3, R.drawable.domino_dark_1_4, R.drawable.domino_dark_1_5, R.drawable.domino_dark_1_6),
        intArrayOf(R.drawable.domino_dark_2_2, R.drawable.domino_dark_2_3, R.drawable.domino_dark_2_4, R.drawable.domino_dark_2_5, R.drawable.domino_dark_2_6),
        intArrayOf(R.drawable.domino_dark_3_3, R.drawable.domino_dark_3_4, R.drawable.domino_dark_3_5, R.drawable.domino_dark_3_6),
        intArrayOf(R.drawable.domino_dark_4_4, R.drawable.domino_dark_4_5, R.drawable.domino_dark_4_6),
        intArrayOf(R.drawable.domino_dark_5_5, R.drawable.domino_dark_5_6),
        intArrayOf(R.drawable.domino_dark_6_6),
    ),
    "gingerbread" to arrayOf(
        intArrayOf(R.drawable.domino_gingerbread_0_0, R.drawable.domino_gingerbread_0_1, R.drawable.domino_gingerbread_0_2, R.drawable.domino_gingerbread_0_3, R.drawable.domino_gingerbread_0_4, R.drawable.domino_gingerbread_0_5, R.drawable.domino_gingerbread_0_6),
        intArrayOf(R.drawable.domino_gingerbread_1_1, R.drawable.domino_gingerbread_1_2, R.drawable.domino_gingerbread_1_3, R.drawable.domino_gingerbread_1_4, R.drawable.domino_gingerbread_1_5, R.drawable.domino_gingerbread_1_6),
        intArrayOf(R.drawable.domino_gingerbread_2_2, R.drawable.domino_gingerbread_2_3, R.drawable.domino_gingerbread_2_4, R.drawable.domino_gingerbread_2_5, R.drawable.domino_gingerbread_2_6),
        intArrayOf(R.drawable.domino_gingerbread_3_3, R.drawable.domino_gingerbread_3_4, R.drawable.domino_gingerbread_3_5, R.drawable.domino_gingerbread_3_6),
        intArrayOf(R.drawable.domino_gingerbread_4_4, R.drawable.domino_gingerbread_4_5, R.drawable.domino_gingerbread_4_6),
        intArrayOf(R.drawable.domino_gingerbread_5_5, R.drawable.domino_gingerbread_5_6),
        intArrayOf(R.drawable.domino_gingerbread_6_6),
    ),
    "hearts" to arrayOf(
        intArrayOf(R.drawable.domino_hearts_0_0, R.drawable.domino_hearts_0_1, R.drawable.domino_hearts_0_2, R.drawable.domino_hearts_0_3, R.drawable.domino_hearts_0_4, R.drawable.domino_hearts_0_5, R.drawable.domino_hearts_0_6),
        intArrayOf(R.drawable.domino_hearts_1_1, R.drawable.domino_hearts_1_2, R.drawable.domino_hearts_1_3, R.drawable.domino_hearts_1_4, R.drawable.domino_hearts_1_5, R.drawable.domino_hearts_1_6),
        intArrayOf(R.drawable.domino_hearts_2_2, R.drawable.domino_hearts_2_3, R.drawable.domino_hearts_2_4, R.drawable.domino_hearts_2_5, R.drawable.domino_hearts_2_6),
        intArrayOf(R.drawable.domino_hearts_3_3, R.drawable.domino_hearts_3_4, R.drawable.domino_hearts_3_5, R.drawable.domino_hearts_3_6),
        intArrayOf(R.drawable.domino_hearts_4_4, R.drawable.domino_hearts_4_5, R.drawable.domino_hearts_4_6),
        intArrayOf(R.drawable.domino_hearts_5_5, R.drawable.domino_hearts_5_6),
        intArrayOf(R.drawable.domino_hearts_6_6),
    ),
    "stars" to arrayOf(
        intArrayOf(R.drawable.domino_stars_0_0, R.drawable.domino_stars_0_1, R.drawable.domino_stars_0_2, R.drawable.domino_stars_0_3, R.drawable.domino_stars_0_4, R.drawable.domino_stars_0_5, R.drawable.domino_stars_0_6),
        intArrayOf(R.drawable.domino_stars_1_1, R.drawable.domino_stars_1_2, R.drawable.domino_stars_1_3, R.drawable.domino_stars_1_4, R.drawable.domino_stars_1_5, R.drawable.domino_stars_1_6),
        intArrayOf(R.drawable.domino_stars_2_2, R.drawable.domino_stars_2_3, R.drawable.domino_stars_2_4, R.drawable.domino_stars_2_5, R.drawable.domino_stars_2_6),
        intArrayOf(R.drawable.domino_stars_3_3, R.drawable.domino_stars_3_4, R.drawable.domino_stars_3_5, R.drawable.domino_stars_3_6),
        intArrayOf(R.drawable.domino_stars_4_4, R.drawable.domino_stars_4_5, R.drawable.domino_stars_4_6),
        intArrayOf(R.drawable.domino_stars_5_5, R.drawable.domino_stars_5_6),
        intArrayOf(R.drawable.domino_stars_6_6),
    ),
)

private fun pipPositions(value: Int): List<Pair<Float, Float>> = when (value.coerceIn(0, 6)) {
    1 -> listOf(.5f to .5f)
    2 -> listOf(.28f to .28f, .72f to .72f)
    3 -> listOf(.28f to .28f, .5f to .5f, .72f to .72f)
    4 -> listOf(.28f to .28f, .72f to .28f, .28f to .72f, .72f to .72f)
    5 -> listOf(.28f to .28f, .72f to .28f, .5f to .5f, .28f to .72f, .72f to .72f)
    6 -> listOf(.28f to .25f, .72f to .25f, .28f to .5f, .72f to .5f, .28f to .75f, .72f to .75f)
    else -> emptyList()
}

internal fun dominoResource(first: Int, second: Int, skin: String = "kenney"): Int {
    val low = minOf(first, second)
    val resources = themedDominoResources[skin] ?: kenneyDominoResources
    return resources[low][maxOf(first, second) - low]
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
    skin: String = "kenney",
) {
    val customCircuit = skin == "circuit"
    val painter = painterResource(if (customCircuit) R.drawable.domino_circuit else dominoResource(tile.first, tile.second, skin))
    val label = stringResource(R.string.domino_description, tile.first, tile.second)
    val horizontal = orientation == Orientation.HORIZONTAL
    Canvas(modifier.aspectRatio(if (horizontal) 2f else .5f)
        .then(if (describe) Modifier.semantics { contentDescription = label } else Modifier)) {
        drawRoundRect(Color.White, cornerRadius = androidx.compose.ui.geometry.CornerRadius(5.dp.toPx()))
        val shortSide = if (horizontal) minOf(size.height, size.width / 2) else minOf(size.width, size.height / 2)
        val spriteSize = Size(shortSide, shortSide * 2)
        withTransform({
            translate(size.width / 2, size.height / 2)
            rotate(dominoAngle(tile.first, tile.second, orientation), pivot = androidx.compose.ui.geometry.Offset.Zero)
            translate(-spriteSize.width / 2, -spriteSize.height / 2)
        }) {
            with(painter) { draw(spriteSize) }
            if (customCircuit) {
                // The generated circuit shell is reused for every value. Mask its
                // decorative pips, then paint the real engine values on top.
                val mask = Color(0xFF20292A)
                val pipRadius = spriteSize.width * .072f
                val maskRadius = spriteSize.width * .105f
                val slots = listOf(.28f, .5f, .72f)
                for (half in 0..1) {
                    for (x in slots) for (y in slots) {
                        drawCircle(mask, maskRadius, Offset(spriteSize.width * x, spriteSize.height * (half * .5f + y * .5f)))
                    }
                }
                fun drawPips(value: Int, half: Int) {
                    pipPositions(value).forEach { (x, y) ->
                        drawCircle(Cyan, pipRadius * 1.18f, Offset(spriteSize.width * x, spriteSize.height * (half * .5f + y * .5f)))
                        drawCircle(Terminal, pipRadius, Offset(spriteSize.width * x, spriteSize.height * (half * .5f + y * .5f)))
                    }
                }
                drawPips(tile.first, 0)
                drawPips(tile.second, 1)
            }
        }
    }
}
