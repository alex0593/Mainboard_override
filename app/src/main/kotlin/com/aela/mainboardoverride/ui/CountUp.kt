package com.aela.mainboardoverride.ui

import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable

/**
 * Counts [target] up from its previous value so balances and tallies tick instead of jumping;
 * reduced motion swaps them instantly. A fresh composition starts at [target], so first paints
 * and tests read the true value.
 */
@Composable
internal fun animatedCount(target: Int, durationMillis: Int = 500): Int {
    val reducedMotion = LocalReducedMotion.current
    val spec: AnimationSpec<Int> = if (reducedMotion) snap() else tween(durationMillis, easing = FastOutSlowInEasing)
    return animateIntAsState(target, spec, label = "count-up").value
}
