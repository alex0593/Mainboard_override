package com.aela.mainboardoverride.ui

import android.graphics.ImageDecoder
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.graphics.drawable.AnimatedImageDrawable
import android.os.Build
import android.widget.ImageView
import androidx.annotation.DrawableRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.currentStateAsState
import com.aela.mainboardoverride.R

/** Animated-WebP resource per premium skin id; free, 80-credit, nebula and unknown ids stay static. */
internal fun fxResource(id: String): Int? = when (id) {
    "copper" -> R.drawable.fx_copper
    "aurora" -> R.drawable.fx_aurora
    "titanium" -> R.drawable.fx_titanium
    "jade" -> R.drawable.fx_jade
    "ruby" -> R.drawable.fx_ruby
    "sapphire" -> R.drawable.fx_sapphire
    "amber" -> R.drawable.fx_amber
    "amethyst" -> R.drawable.fx_amethyst
    "biolum" -> R.drawable.fx_biolum
    "prisma" -> R.drawable.fx_prisma
    "quantum" -> R.drawable.fx_quantum
    else -> null
}

/** Generic neutral motes loop that CircuitBackground tints per scenario accent. */
internal fun ambientMotesResource(): Int = R.drawable.ambient_motes

/**
 * Plays the premium skin's particle WebP over the static artwork. API 26-27 and reduced motion
 * fall back to the Canvas glow in [PremiumGlowCanvas], which is static under reduced motion.
 */
@Composable
internal fun AnimatedSkinFxWebp(skin: String?, modifier: Modifier = Modifier) {
    val reducedMotion = LocalReducedMotion.current
    val res = skin?.let(::fxResource)
    if (res == null || reducedMotion || Build.VERSION.SDK_INT < 28) {
        PremiumGlowCanvas(skin, modifier)
        return
    }
    AnimatedWebpView(res, modifier, opacity = 0.55f)
}

/**
 * Ambient motes over the free-mode background, tinted with the scenario accent. Skipped entirely
 * under reduced motion and on API 26-27; the static background already covers those.
 */
@Composable
internal fun AnimatedAmbientWebp(tint: Color, modifier: Modifier = Modifier) {
    val reducedMotion = LocalReducedMotion.current
    if (reducedMotion || Build.VERSION.SDK_INT < 28) return
    AnimatedWebpView(ambientMotesResource(), modifier, tint = tint)
}

/** Hosts one looping AnimatedImageDrawable; starts and stops with the lifecycle. */
@androidx.annotation.RequiresApi(28)
@Composable
private fun AnimatedWebpView(@DrawableRes source: Int, modifier: Modifier = Modifier, tint: Color? = null, opacity: Float = 1f) {
    val lifecycleState by LocalLifecycleOwner.current.lifecycle.currentStateAsState()
    val running = lifecycleState == Lifecycle.State.RESUMED
    AndroidView(
        modifier = modifier,
        factory = { context ->
            ImageView(context).apply {
                isClickable = false
                isFocusable = false
                scaleType = ImageView.ScaleType.FIT_XY
                this.alpha = opacity
                val decoded = ImageDecoder.decodeDrawable(
                    ImageDecoder.createSource(context.resources, source)
                )
                if (tint != null) decoded.colorFilter = PorterDuffColorFilter(tint.toArgb(), PorterDuff.Mode.SRC_IN)
                setImageDrawable(decoded)
                if (decoded is AnimatedImageDrawable) decoded.repeatCount = AnimatedImageDrawable.REPEAT_INFINITE
            }
        },
        update = { view ->
            val drawable = view.drawable as? AnimatedImageDrawable ?: return@AndroidView
            if (running) drawable.start() else drawable.stop()
        },
    )
}
