package com.aela.mainboardoverride.ui

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.LruCache
import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalResources
import com.aela.mainboardoverride.R

// All nodpi assets are fixed across densities and locales; share their decoded pixels across cells.
private val threatImages = LruCache<Int, Bitmap>(3)

/** Cell semantics describe the threat; the artwork is decorative. */
@Composable
internal fun BoardThreatImage(firewall: Boolean, modifier: Modifier = Modifier) {
    val resources = LocalResources.current
    val resource = if (firewall) R.drawable.board_firewall else R.drawable.board_honeypot
    val image = remember(resources, resource) { threatBitmap(resources, resource).asImageBitmap() }
    Image(image, contentDescription = null, modifier = modifier, contentScale = ContentScale.Fit)
}

/** Lock sprite; the demanded value is drawn by the caller, never baked into the art. */
@Composable
internal fun BoardLockImage(modifier: Modifier = Modifier) {
    val resources = LocalResources.current
    val image = remember(resources) { threatBitmap(resources, R.drawable.board_lock).asImageBitmap() }
    Image(image, contentDescription = null, modifier = modifier, contentScale = ContentScale.Fit)
}

/** Shared cropped art for live cells and cached previews. */
internal fun threatBitmap(resources: android.content.res.Resources, resource: Int): Bitmap {
        threatImages.get(resource)?.let { return it }
        val source = BitmapFactory.decodeResource(resources, resource)
        val pixels = IntArray(source.width * source.height)
        source.getPixels(pixels, 0, source.width, 0, 0, source.width, source.height)
        var left = source.width
        var top = source.height
        var right = -1
        var bottom = -1
        pixels.forEachIndexed { index, pixel ->
            // These exports contain almost invisible background specks (alpha < 8).
            if (pixel ushr 24 >= 8) {
                val x = index % source.width
                val y = index / source.width
                left = minOf(left, x)
                top = minOf(top, y)
                right = maxOf(right, x)
                bottom = maxOf(bottom, y)
            }
        }
        // Ignore transparent export margins without resampling or altering the source art.
        return (if (right >= left && bottom >= top) {
            // Keep a small fringe for the antialiased edges of the artwork.
            left = (left - 2).coerceAtLeast(0)
            top = (top - 2).coerceAtLeast(0)
            right = (right + 2).coerceAtMost(source.width - 1)
            bottom = (bottom + 2).coerceAtMost(source.height - 1)
            Bitmap.createBitmap(source, left, top, right - left + 1, bottom - top + 1)
        } else source).also {
            if (it !== source) source.recycle()
            threatImages.put(resource, it)
        }
}
