package com.katoaapps.openminilaunch.features.minkspace.media

import android.content.Context
import android.graphics.ImageDecoder
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.media.MediaMetadataRetriever
import android.os.Build
import kotlin.math.max

internal object MediaPreviewLoader {
    fun load(context: Context, item: MediaItem, targetPixels: Int = 900): Drawable? =
        runCatching {
            if (item.isVideo) {
                loadVideoFrame(context, item, targetPixels)
            } else {
                loadImage(context, item, targetPixels)
            }
        }.getOrNull()

    private fun loadVideoFrame(
        context: Context,
        item: MediaItem,
        targetPixels: Int,
    ): Drawable? {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(context, item.uri)
            val bitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
                retriever.getScaledFrameAtTime(
                    0,
                    MediaMetadataRetriever.OPTION_CLOSEST_SYNC,
                    targetPixels,
                    targetPixels,
                )
            } else {
                retriever.getFrameAtTime(0, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
            }
            bitmap
                ?.let { BitmapDrawable(context.resources, it) }
        } finally {
            retriever.release()
        }
    }

    private fun loadImage(context: Context, item: MediaItem, targetPixels: Int): Drawable? {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val source = ImageDecoder.createSource(context.contentResolver, item.uri)
            return ImageDecoder.decodeDrawable(source) { decoder, info, _ ->
                val longestSide = max(info.size.width, info.size.height)
                decoder.setTargetSampleSize((longestSide / targetPixels).coerceAtLeast(1))
            }
        }
        return context.contentResolver.openInputStream(item.uri)?.use { stream ->
            Drawable.createFromStream(stream, item.displayName)
        }
    }
}
