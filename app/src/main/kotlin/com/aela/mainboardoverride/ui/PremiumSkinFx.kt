package com.aela.mainboardoverride.ui

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.dp
import com.aela.mainboardoverride.domain.Rewards
import kotlin.math.max
import kotlin.math.sin

/** Motion character of a premium skin's glow, layered over its static artwork. */
enum class SkinFxStyle {
    /** A soft sheen drifting diagonally across the surface. */
    SWEEP,
    /** A breathing halo of the material accent. */
    PULSE,
    /** Slow aurora-like bands moving laterally. */
    DRIFT,
    /** Small twinkles firing at fixed points. */
    TWINKLE,
}

/** Premium fx configuration; [accent] always mirrors [dominoPipColor] for the same id. */
data class SkinFx(val accent: Color, val style: SkinFxStyle, val periodMillis: Int)

private val premiumFx = mapOf(
    "copper" to SkinFxStyle.SWEEP,
    "aurora" to SkinFxStyle.DRIFT,
    "titanium" to SkinFxStyle.SWEEP,
    "jade" to SkinFxStyle.PULSE,
    "ruby" to SkinFxStyle.PULSE,
    "sapphire" to SkinFxStyle.TWINKLE,
    "amber" to SkinFxStyle.SWEEP,
    "amethyst" to SkinFxStyle.TWINKLE,
    "biolum" to SkinFxStyle.PULSE,
    "prisma" to SkinFxStyle.TWINKLE,
    "quantum" to SkinFxStyle.DRIFT,
)

/** Fx only for the eight 200-credit skins; free, 80-credit, nebula and unknown ids return null. */
internal fun premiumSkinFx(id: String): SkinFx? {
    val style = premiumFx[id] ?: return null
    if (id !in Rewards.premiumSkins) return null
    val period = when (style) {
        SkinFxStyle.SWEEP -> 3400
        SkinFxStyle.PULSE -> 3200
        SkinFxStyle.DRIFT -> 4600
        SkinFxStyle.TWINKLE -> 2400
    }
    return SkinFx(dominoPipColor(id), style, period)
}

/** Draws nothing for non-premium skins; the caller can unconditionally add the overlay. */
@Composable
internal fun PremiumGlowCanvas(skin: String?, modifier: Modifier = Modifier) {
    val fx = skin?.let(::premiumSkinFx) ?: return
    PremiumGlowLayer(fx, modifier)
}

@Composable
private fun PremiumGlowLayer(fx: SkinFx, modifier: Modifier = Modifier) {
    val reducedMotion = LocalReducedMotion.current
    val transition = rememberInfiniteTransition(label = "premium-glow")
    // Reduced motion keeps the layer but freezes it at the midpoint of each cycle.
    val phase by transition.animateFloat(
        0f, 1f,
        infiniteRepeatable(tween(fx.periodMillis, easing = LinearEasing)),
        label = "premium-glow-phase",
    )
    val frozen = 0.5f
    Canvas(modifier) {
        val t = if (reducedMotion) frozen else phase
        when (fx.style) {
            SkinFxStyle.SWEEP -> drawSweep(fx.accent, t)
            SkinFxStyle.PULSE -> drawPulse(fx.accent, t)
            SkinFxStyle.DRIFT -> drawDrift(fx.accent, t)
            SkinFxStyle.TWINKLE -> drawTwinkle(fx.accent, t)
        }
    }
}

/** A soft diagonal sheen drifting across the field; frozen mid-crossing under reduced motion. */
private fun DrawScope.drawSweep(accent: Color, t: Float) {
    val band = size.width * .24f
    val x0 = -band + (size.width + band * 2f) * t
    rotate(10f, Offset(size.width / 2f, size.height / 2f)) {
        drawRect(
            Brush.linearGradient(
                0f to accent.copy(alpha = 0f),
                .5f to accent.copy(alpha = .10f),
                1f to accent.copy(alpha = 0f),
                start = Offset(x0, 0f),
                end = Offset(x0 + band, 0f),
            ),
        )
    }
}

/** A soft accent vignette breathing in and out. */
private fun DrawScope.drawPulse(accent: Color, t: Float) {
    val a = .08f + .16f * (0.5f + 0.5f * sin(t * 2f * Math.PI.toFloat()))
    drawRect(
        Brush.radialGradient(
            listOf(accent.copy(alpha = a), accent.copy(alpha = 0f)),
            center = Offset(size.width / 2f, size.height / 2f),
            radius = max(size.width, size.height) * .72f,
        ),
    )
}

/** Two slow translucent bands drifting downward, like an aurora. */
private fun DrawScope.drawDrift(accent: Color, t: Float) {
    repeat(2) { i ->
        val yy = ((t + i * .5f) % 1f) * (size.height * 1.5f) - size.height * .25f
        drawRect(
            Brush.verticalGradient(
                0f to accent.copy(alpha = 0f),
                .5f to accent.copy(alpha = .16f),
                1f to accent.copy(alpha = 0f),
            ),
            topLeft = Offset(0f, yy),
            size = Size(size.width, size.height * .3f),
        )
    }
}

/** Small sparkles at fixed points, each on its own phase. */
private fun DrawScope.drawTwinkle(accent: Color, t: Float) {
    val points = listOf(.2f to .3f, .75f to .2f, .6f to .62f, .3f to .8f, .85f to .75f, .52f to .45f)
    points.forEachIndexed { i, (px, py) ->
        val local = (t + i * .37f) % 1f
        val a = if (local < .25f) local / .25f else (1f - (local - .25f) / .75f).coerceAtLeast(0f)
        val center = Offset(size.width * px, size.height * py)
        val r = (1.5f + 4.5f * a) * (size.minDimension / 200f).coerceAtLeast(1f)
        drawCircle(accent.copy(alpha = a * .85f), r, center)
        drawLine(accent.copy(alpha = a * .5f), center + Offset(-r * 2.2f, 0f), center + Offset(r * 2.2f, 0f), 1.dp.toPx())
        drawLine(accent.copy(alpha = a * .5f), center + Offset(0f, -r * 2.2f), center + Offset(0f, r * 2.2f), 1.dp.toPx())
    }
}

/** Breathing accent halo painted behind a placed premium domino. */
fun DrawScope.premiumHalo(accent: Color, t: Float) {
    drawRoundRect(
        accent.copy(alpha = (.10f + .24f * t).coerceAtMost(.34f)),
        cornerRadius = CornerRadius(8.dp.toPx()),
    )
}

/** Animated premium border for store cards; plain (static accent) border under reduced motion. */
@Composable
internal fun Modifier.premiumCardBorder(skin: String?, cornerDp: Float = 14f): Modifier {
    val fx = skin?.let(::premiumSkinFx) ?: return this
    val reducedMotion = LocalReducedMotion.current
    val transition = rememberInfiniteTransition(label = "premium-card-border")
    val pulse by transition.animateFloat(
        .55f, 1f,
        infiniteRepeatable(tween(1900, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "premium-card-border-alpha",
    )
    return drawBehind {
        drawRoundRect(
            fx.accent.copy(alpha = (if (reducedMotion) .7f else pulse) * .65f),
            cornerRadius = CornerRadius(cornerDp.dp.toPx()),
            style = Stroke(1.6.dp.toPx()),
        )
    }
}
