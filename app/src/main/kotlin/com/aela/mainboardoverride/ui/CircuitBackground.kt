package com.aela.mainboardoverride.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.painterResource
import androidx.compose.foundation.Image
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.dp
import com.aela.mainboardoverride.R
import androidx.compose.animation.core.*

@Composable
internal fun CircuitBackground(
    animated: Boolean = false,
    @androidx.annotation.DrawableRes backgroundResource: Int = R.drawable.menu_background_v2,
    content: @Composable () -> Unit,
) {
    val progress = if (animated) {
        val transition = rememberInfiniteTransition(label = "menu circuits")
        transition.animateFloat(0f, 1f, infiniteRepeatable(tween(6000, easing = LinearEasing)), label = "pulse")
    } else null
    Box(Modifier.fillMaxSize().background(Brush.linearGradient(listOf(Void, Panel, Void)))) {
        Image(
            painterResource(backgroundResource),
            contentDescription = null,
            modifier = Modifier.matchParentSize().alpha(.72f),
            contentScale = androidx.compose.ui.layout.ContentScale.Crop,
        )
        Canvas(Modifier.fillMaxSize()) {
            val step = 48.dp.toPx()
            var x = -step
            var index = 0
            while (x < size.width) {
                val bend = size.height * (.2f + (index % 5) * .13f)
                val tint = if (index % 3 == 0) Cyan.copy(alpha = .09f) else Terminal.copy(alpha = .07f)
                drawLine(tint, Offset(x, 0f), Offset(x, bend), 1.dp.toPx())
                drawLine(tint, Offset(x, bend), Offset(x + step / 2, bend + step / 2), 1.dp.toPx())
                drawLine(tint, Offset(x + step / 2, bend + step / 2), Offset(x + step / 2, size.height), 1.dp.toPx())
                drawCircle(tint, 3.dp.toPx(), Offset(x, bend), style = Stroke(1.dp.toPx()))
                if (animated) {
                    // Read the animation in the draw phase, without recomposing menu content each frame.
                    val distance = (((progress?.value ?: 0f) + index * .13f) % 1f) * (size.height + step / 2)
                    val point = when {
                        distance < bend -> Offset(x, distance)
                        distance < bend + step / 2 -> Offset(x + distance - bend, distance)
                        else -> Offset(x + step / 2, distance)
                    }
                    drawCircle(Cyan.copy(alpha = .12f), 8.dp.toPx(), point)
                    drawCircle(Cyan.copy(alpha = .6f), 2.dp.toPx(), point)
                }
                x += step
                index++
            }
        }
        content()
    }
}
