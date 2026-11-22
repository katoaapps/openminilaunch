package org.sableos.games.console

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap

/**
 * A game's pictures for `sable.image`: PNG files in its folder, next to `game.toml`. Each name is
 * 1-32 of a-z, 0-9 and _ then `.png`; a game may have up to [MAX] of them, each at most
 * [MAX_SIDE] pixels a side and [MAX_BYTES] long.
 */
object GamePictures {
    const val MAX = 32
    const val MAX_SIDE = 512
    const val MAX_BYTES = 256 * 1024
    private val NAME = Regex("[a-z0-9_]{1,32}\\.png")

    fun isPicture(name: String): Boolean = NAME.matches(name)

    /** Why these picture bytes can't be used, or null when they can. */
    fun problem(
        name: String,
        bytes: ByteArray?,
    ): String? {
        if (bytes == null || bytes.size > MAX_BYTES) return "$name is bigger than 256 KB or can't be read"
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
        return when {
            bounds.outWidth <= 0 || bounds.outHeight <= 0 -> "$name is not a PNG picture"
            bounds.outWidth > MAX_SIDE || bounds.outHeight > MAX_SIDE -> "$name is bigger than $MAX_SIDE by $MAX_SIDE"
            else -> null
        }
    }

    /** The picture, or null when it breaks the limits above. */
    fun decode(
        name: String,
        bytes: ByteArray,
    ): ImageBitmap? {
        if (problem(name, bytes) != null) return null
        val options = BitmapFactory.Options().apply { inPreferredConfig = Bitmap.Config.ARGB_8888 }
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)?.asImageBitmap()
    }
}
