package com.aela.mainboardoverride.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aela.mainboardoverride.R
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.currentStateAsState
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

/** The image is static. Only a cached raster overlay moves, independently of menu layout. */
@Composable
internal fun MenuArtworkBackground(reducedMotion: Boolean, content: @Composable () -> Unit) {
    val lifecycleState by LocalLifecycleOwner.current.lifecycle.currentStateAsState()
    val animate = !reducedMotion && lifecycleState == Lifecycle.State.RESUMED
    val phase = if (animate) {
        rememberInfiniteTransition(label = "menu pulse").animateFloat(
            0f, 1f, infiniteRepeatable(tween(10_000, easing = LinearEasing)), label = "menu pulse phase",
        )
    } else null
    Box(Modifier.fillMaxSize().clip(RoundedCornerShape(0.dp)).background(Void)) {
        Image(painterResource(R.drawable.menu_background_v2), null,
            Modifier.matchParentSize(), contentScale = ContentScale.Crop)
        if (animate) {
            Image(
                painterResource(R.drawable.menu_pulse_overlay_v1),
                contentDescription = null,
                // Edge ornament must stay visible even when the host has a different aspect ratio.
                contentScale = ContentScale.FillBounds,
                modifier = Modifier.matchParentSize().graphicsLayer {
                    // Only the lightweight overlay moves; the base artwork stays cached.
                    val angle = (phase?.value ?: 0f) * (2f * Math.PI.toFloat())
                    val sine = sin(angle.toDouble()).toFloat()
                    val cosine = cos(angle.toDouble()).toFloat()
                    translationX = sine * size.width * .035f
                    translationY = cosine * size.height * .02f
                    alpha = .28f + .28f * ((cosine + 1f) * .5f)
                },
            )
        }
        content()
    }
}

/** Nine-slice raster backing keeps the artwork's corners intact at every button width. */
@Composable
internal fun MenuArtworkButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    primary: Boolean = false,
    compact: Boolean = false,
    enabled: Boolean = true,
    fillWidth: Boolean = true,
    minHeight: androidx.compose.ui.unit.Dp? = null,
) {
    val image = ImageBitmap.imageResource(when {
        compact -> R.drawable.menu_button_compact_v2
        primary -> R.drawable.menu_button_primary_v2
        else -> R.drawable.menu_button_secondary_v2
    })
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    Button(
        onClick = onClick,
        enabled = enabled,
        interactionSource = interaction,
        modifier = (if (fillWidth) modifier.fillMaxWidth() else modifier)
            .heightIn(min = minHeight ?: if (compact) 48.dp else 54.dp)
            .graphicsLayer { alpha = if (enabled) 1f else .45f }
            .drawWithCache {
                val sourceEdge = (image.height * .22f).roundToInt().coerceAtLeast(1)
                val corner = minOf(12.dp.toPx().roundToInt(), (size.height / 2).toInt(), (size.width / 2).toInt())
                val sourceX = intArrayOf(0, sourceEdge, image.width - sourceEdge, image.width)
                val sourceY = intArrayOf(0, sourceEdge, image.height - sourceEdge, image.height)
                val targetX = intArrayOf(0, corner, size.width.roundToInt() - corner, size.width.roundToInt())
                val targetY = intArrayOf(0, corner, size.height.roundToInt() - corner, size.height.roundToInt())
                onDrawBehind {
                    for (row in 0..2) for (col in 0..2) {
                        drawImage(image,
                            srcOffset = IntOffset(sourceX[col], sourceY[row]),
                            srcSize = IntSize(sourceX[col + 1] - sourceX[col], sourceY[row + 1] - sourceY[row]),
                            dstOffset = IntOffset(targetX[col], targetY[row]),
                            dstSize = IntSize(targetX[col + 1] - targetX[col], targetY[row + 1] - targetY[row]))
                    }
                }
            },
        shape = RoundedCornerShape(8.dp),
        contentPadding = PaddingValues(horizontal = if (compact) 10.dp else 24.dp, vertical = 12.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (pressed) Cyan.copy(alpha = .12f) else Color.Transparent,
            contentColor = if (primary) Color(0xFFE0FFF9) else Color(0xFFC5E0DF),
            disabledContainerColor = Color.Transparent,
            disabledContentColor = Muted,
        ),
    ) {
        Text(label, fontFamily = FontFamily.Monospace,
            fontWeight = if (primary) FontWeight.Bold else FontWeight.Medium,
            fontSize = if (compact) 11.sp else 13.sp,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center)
    }
}
