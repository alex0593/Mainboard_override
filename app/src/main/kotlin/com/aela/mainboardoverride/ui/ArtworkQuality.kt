package com.aela.mainboardoverride.ui

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.LruCache
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp

// One reduced decode per resource and display box serves every cell and card on screen.
private val reductionCache = object : LruCache<String, Bitmap>(16 * 1024 * 1024) {
    override fun sizeOf(key: String, value: Bitmap): Int = value.allocationByteCount
}

/**
 * Halves [source] bilinearly while the half still covers the [maxW]×[maxH] display box, so the
 * result lands in [display, 2×display). The draw pass then minifies at most 2×, where plain
 * bilinear stops aliasing: Android's Canvas has no mipmaps (FilterQuality only toggles
 * nearest/bilinear), and nodpi art like the 1254² script icons can be 16× the target size.
 *
 * The cropped input is never recycled: callers may share it through their own caches.
 */
internal fun reduceToDisplay(source: Bitmap, key: String, maxW: Float, maxH: Float): Bitmap {
    if (maxW <= 0f || maxH <= 0f) return source
    reductionCache.get(key)?.let { return it }
    var result = source
    while (result.width / 2f >= maxW && result.height / 2f >= maxH) {
        val next = Bitmap.createScaledBitmap(result, result.width / 2, result.height / 2, true)
        if (next === result) break
        result = next
    }
    reductionCache.put(key, result)
    return result
}

/**
 * [resId] decoded once and reduced for a [maxW]×[maxH] display box; null for resources the
 * bitmap decoder cannot read (vector drawables), which need no reduction anyway.
 */
@Composable
internal fun smoothArtwork(resId: Int, maxW: Dp, maxH: Dp): ImageBitmap? {
    val resources = LocalResources.current
    val wPx = with(LocalDensity.current) { maxW.toPx() }
    val hPx = with(LocalDensity.current) { maxH.toPx() }
    return remember(resources, resId, wPx, hPx) {
        val decoded = BitmapFactory.decodeResource(resources, resId) ?: return@remember null
        reduceToDisplay(decoded, "$resId:$wPx:$hPx", wPx, hPx).asImageBitmap()
    }
}

/** Painter form of [smoothArtwork], falling back to plain [painterResource] for vectors. */
@Composable
internal fun smoothArtworkPainter(resId: Int, maxW: Dp, maxH: Dp): Painter {
    val bitmap = smoothArtwork(resId, maxW, maxH)
    return if (bitmap != null) remember(bitmap) { BitmapPainter(bitmap) } else painterResource(resId)
}
