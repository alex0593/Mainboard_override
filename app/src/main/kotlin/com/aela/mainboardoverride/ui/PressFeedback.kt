package com.aela.mainboardoverride.ui

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role

/** How far a compact surface sinks before springing back. */
private const val PressedScale = .93f

/**
 * Makes a press visible: the surface sinks and dims while the finger stays down and springs back
 * when it lifts. The values are read inside the layer, so a press only re-draws the surface.
 * With reduced motion the sink is dropped and only the dim remains, following [LocalReducedMotion].
 */
@Composable
internal fun Modifier.pressFeedback(
    interactionSource: InteractionSource,
    label: String = "press",
    pressedScale: Float = PressedScale,
): Modifier {
    val pressed by interactionSource.collectIsPressedAsState()
    val reducedMotion = LocalReducedMotion.current
    val scale = animateFloatAsState(
        targetValue = if (pressed && !reducedMotion) pressedScale else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "$label scale",
    )
    val dim = animateFloatAsState(
        targetValue = if (pressed) .72f else 1f,
        animationSpec = tween(90),
        label = "$label dim",
    )
    return graphicsLayer {
        scaleX = scale.value
        scaleY = scale.value
        alpha = dim.value
    }
}

/** Clickable surface that reports its presses to [pressFeedback]. */
@Composable
internal fun Modifier.pressable(
    enabled: Boolean = true,
    role: Role? = null,
    label: String = "press",
    pressedScale: Float = PressedScale,
    onClick: () -> Unit,
): Modifier {
    val interaction = remember { MutableInteractionSource() }
    return clickable(
        interactionSource = interaction,
        indication = LocalIndication.current,
        enabled = enabled,
        role = role,
        onClick = onClick,
    ).pressFeedback(interaction, label = label, pressedScale = pressedScale)
}
