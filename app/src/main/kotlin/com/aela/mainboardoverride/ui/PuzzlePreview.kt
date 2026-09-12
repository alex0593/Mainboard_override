package com.aela.mainboardoverride.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.util.LruCache
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.aela.mainboardoverride.R
import com.aela.mainboardoverride.domain.*
import java.io.File
import java.security.MessageDigest
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** Increment when generator geometry or preview rendering changes. */
internal object PuzzlePreviewCache {
    private const val REVISION = 1
    private val mutex = Mutex()
    private val memory = object : LruCache<String, Bitmap>(8 * 1024 * 1024) {
        override fun sizeOf(key: String, value: Bitmap) = value.byteCount
    }
    internal fun clearMemory() = memory.evictAll()
    internal fun key(id: String, boardSkin: String, dominoSkin: String): String =
        MessageDigest.getInstance("SHA-256")
            .digest("$REVISION:$id:$boardSkin:$dominoSkin".toByteArray())
            .joinToString("") { "%02x".format(it) }

    suspend fun load(context: Context, id: String, boardSkin: String, dominoSkin: String,
        generate: () -> BoardState): Bitmap = withContext(Dispatchers.IO) {
        mutex.withLock {
            val key = key(id, boardSkin, dominoSkin)
            memory.get(key)?.let { return@withLock it }
            val directory = File(context.cacheDir, "puzzle-previews").apply { mkdirs() }
            val file = File(directory, "$key.png")
            val cached = if (file.isFile) BitmapFactory.decodeFile(file.path) else null
            if (cached != null && cached.width == 640 && cached.height == 400) {
                file.setLastModified(System.currentTimeMillis())
                memory.put(key, cached)
                return@withLock cached
            }
            val board = withContext(Dispatchers.Default) { generate() }
            val bitmap = render(context, board, boardSkin)
            // Cache failure must not prevent playing; the in-memory image is still usable.
            runCatching {
                val temporary = File.createTempFile("preview-", ".tmp", directory)
                try {
                    temporary.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
                    check(temporary.renameTo(file))
                } finally { temporary.delete() }
                var bytes = 0L
                directory.listFiles().orEmpty().filter { it.extension == "png" }
                    .sortedByDescending { it.lastModified() }.forEach {
                        bytes += it.length()
                        if (bytes > 24 * 1024 * 1024) it.delete()
                    }
            }
            memory.put(key, bitmap)
            bitmap
        }
    }

    private fun render(context: Context, board: BoardState, skin: String): Bitmap {
        val bitmap = Bitmap.createBitmap(640, 400, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { isFilterBitmap = true }
        canvas.drawColor(Color.rgb(9, 20, 25))
        boardSkinResource(skin, true)?.let {
            val background = BitmapFactory.decodeResource(context.resources, it)
            canvas.drawBitmap(background, null, Rect(0, 0, 640, 400), paint)
            background.recycle()
        }
        val cell = minOf(540f / board.width, 300f / board.height)
        val left = (640 - cell * board.width) / 2
        val top = (400 - cell * board.height) / 2
        fun bounds(p: Position) = RectF(left + p.x * cell, top + p.y * cell,
            left + (p.x + 1) * cell, top + (p.y + 1) * cell)
        paint.color = Color.argb(70, 100, 175, 185)
        paint.style = Paint.Style.STROKE
        for (y in 0 until board.height) for (x in 0 until board.width)
            canvas.drawRect(bounds(Position(x, y)), paint)
        paint.style = Paint.Style.FILL
        fun node(p: Position, text: String, color: Int) {
            val rect = bounds(p).apply { inset(3f, 3f) }
            paint.color = color
            canvas.drawRoundRect(rect, 5f, 5f, paint)
            paint.color = Color.BLACK
            paint.textAlign = Paint.Align.CENTER
            paint.textSize = cell * .55f
            canvas.drawText(text, rect.centerX(), rect.centerY() - (paint.ascent() + paint.descent()) / 2, paint)
        }
        node(board.start, "0", Color.rgb(87, 230, 183))
        node(board.extraction, "6", Color.rgb(90, 205, 240))
        val firewall = threatBitmap(context.resources, R.drawable.board_firewall)
        for (position in board.firewalls) canvas.drawBitmap(firewall, null, bounds(position), paint)
        // Hidden honeypots are deliberately absent; never serialize a solution or reveal traps.
        board.buffs.filterKeys { it !in board.collectedBuffs }.forEach { (p, buff) ->
            node(p, if (buff == BoardBuff.RAM_RESERVE) "+" else "-", Color.rgb(240, 190, 80))
        }
        board.daemon?.let { node(it.position, "D", Color.rgb(240, 90, 90)) }
        return bitmap
    }
}

@Composable
internal fun PuzzlePreview(
    id: String,
    boardSkin: String,
    dominoSkin: String,
    modifier: Modifier = Modifier.fillMaxWidth().height(180.dp),
    generate: () -> BoardState,
) {
    val context = LocalContext.current.applicationContext
    var retry by remember(id) { mutableIntStateOf(0) }
    val result by produceState<Result<Bitmap>?>(null, id, boardSkin, dominoSkin, retry) {
        value = null
        value = try { Result.success(PuzzlePreviewCache.load(context, id, boardSkin, dominoSkin, generate)) }
        catch (cancelled: CancellationException) { throw cancelled }
        catch (error: Exception) { Result.failure(error) }
    }
    Box(modifier, contentAlignment = Alignment.Center) {
        val bitmap = result?.getOrNull()
        when {
            bitmap != null -> Image(bitmap.asImageBitmap(), stringResource(R.string.puzzle_preview), Modifier.fillMaxSize())
            result?.isFailure == true -> Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(stringResource(R.string.preview_failed))
                MenuArtworkButton(stringResource(R.string.preview_retry), { retry++ }, compact = true, fillWidth = false)
            }
            else -> CircularProgressIndicator(Modifier.size(24.dp))
        }
    }
}
