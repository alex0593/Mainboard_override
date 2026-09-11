package com.aela.mainboardoverride.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.currentStateAsState
import com.aela.mainboardoverride.R
import kotlin.math.ceil

internal val LocalReducedMotion = compositionLocalOf { false }
internal val LocalWindowTransition = staticCompositionLocalOf<((() -> Unit) -> Unit)> { { it() } }

/** Owns one command until the doors reopen. Clear before invocation, including lifecycle restarts. */
internal class WindowTransitionState {
    var busy by mutableStateOf(false)
        private set
    private var command: (() -> Unit)? = null

    fun request(action: () -> Unit) {
        if (busy) return
        command = action
        busy = true
    }

    fun commit() {
        val action = command
        command = null
        action?.invoke()
    }

    fun finish() { busy = false }
}

@Composable
internal fun WindowTransitionHost(reducedMotion: Boolean, content: @Composable () -> Unit) {
    val state = remember { WindowTransitionState() }
    val closure = remember { Animatable(0f) }
    val lifecycle by LocalLifecycleOwner.current.lifecycle.currentStateAsState()
    val resumed = lifecycle == Lifecycle.State.RESUMED
    LaunchedEffect(state.busy, reducedMotion, resumed) {
        if (!state.busy) return@LaunchedEffect
        if (reducedMotion || !resumed) {
            state.commit()
            closure.snapTo(0f)
            state.finish()
        } else {
            closure.animateTo(1f, tween(220, easing = FastOutSlowInEasing))
            state.commit()
            // Let collected session state and the destination compose while fully covered.
            withFrameNanos { }
            withFrameNanos { }
            closure.animateTo(0f, tween(260, easing = FastOutSlowInEasing))
            state.finish()
        }
    }
    CompositionLocalProvider(LocalReducedMotion provides reducedMotion,
        LocalWindowTransition provides state::request) {
        Box(Modifier.fillMaxSize().clipToBounds()) {
            Box(Modifier.fillMaxSize().then(if (state.busy) Modifier.clearAndSetSemantics { } else Modifier)) {
                content()
            }
            if (state.busy) {
                val shutters = ImageBitmap.imageResource(R.drawable.menu_shutters_v1)
                Canvas(Modifier.fillMaxSize().testTag("window-transition").pointerInput(Unit) {
                    awaitPointerEventScope {
                        while (true) awaitPointerEvent(PointerEventPass.Initial).changes.forEach { it.consume() }
                    }
                }) {
                    val half = ceil(size.width / 2).toInt()
                    val distance = half * (1f - closure.value)
                    for (side in 0..1) {
                        translate(left = if (side == 0) -distance else size.width - half + distance) {
                            drawImage(shutters,
                                srcOffset = IntOffset(side * (shutters.width / 2), 0),
                                srcSize = IntSize(shutters.width / 2, shutters.height),
                                dstSize = IntSize(half, size.height.toInt()))
                        }
                    }
                }
            }
        }
        BackHandler(enabled = state.busy) { /* Wait for the accepted action. */ }
    }
}
