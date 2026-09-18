package com.katoaapps.openminilaunch.features.appearance

import android.app.Activity
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.ImageDecoder
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.net.Uri
import android.os.Build
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.IntSize
import kotlin.math.max
import kotlin.math.roundToInt

internal object WallpaperImageProcessor {
    fun deviceDisplaySize(context: Context): IntSize {
        val activity = context as? Activity
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && activity != null) {
            val bounds = activity.windowManager.currentWindowMetrics.bounds
            if (bounds.width() > 0 && bounds.height() > 0) return IntSize(bounds.width(), bounds.height())
        }
        val metrics = context.resources.displayMetrics
        return IntSize(metrics.widthPixels.coerceAtLeast(1), metrics.heightPixels.coerceAtLeast(1))
    }

    fun decodeForDisplay(context: Context, uri: Uri, target: IntSize): Bitmap {
        require(target.width > 0 && target.height > 0)
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val source = ImageDecoder.createSource(context.contentResolver, uri)
            ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
                decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                decoder.setTargetSampleSize(
                    sampleSizeFor(
                        sourceWidth = info.size.width,
                        sourceHeight = info.size.height,
                        targetWidth = target.width,
                        targetHeight = target.height,
                    ),
                )
            }
        } else {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
            require(bounds.outWidth > 0 && bounds.outHeight > 0) { "Unable to read image" }
            val options = BitmapFactory.Options().apply {
                inSampleSize = sampleSizeFor(bounds.outWidth, bounds.outHeight, target.width, target.height)
            }
            context.contentResolver.openInputStream(uri)?.use {
                BitmapFactory.decodeStream(it, null, options)
            } ?: error("Unable to read image")
        }
    }

    fun constrainOffset(
        source: IntSize,
        viewport: IntSize,
        zoom: Float,
        requested: Offset,
    ): Offset {
        if (source.width <= 0 || source.height <= 0 || viewport.width <= 0 || viewport.height <= 0) {
            return Offset.Zero
        }
        val scale = baseScale(source, viewport) * zoom.coerceAtLeast(1f)
        val horizontalRoom = ((source.width * scale - viewport.width) / 2f).coerceAtLeast(0f)
        val verticalRoom = ((source.height * scale - viewport.height) / 2f).coerceAtLeast(0f)
        return Offset(
            x = requested.x.coerceIn(-horizontalRoom, horizontalRoom),
            y = requested.y.coerceIn(-verticalRoom, verticalRoom),
        )
    }

    fun renderCrop(
        source: Bitmap,
        viewport: IntSize,
        target: IntSize,
        zoom: Float,
        offset: Offset,
    ): Bitmap {
        require(viewport.width > 0 && viewport.height > 0)
        require(target.width > 0 && target.height > 0)
        val sourceSize = IntSize(source.width, source.height)
        val safeZoom = zoom.coerceAtLeast(1f)
        val safeOffset = constrainOffset(sourceSize, viewport, safeZoom, offset)
        val scale = baseScale(sourceSize, viewport) * safeZoom
        val drawnWidth = source.width * scale
        val drawnHeight = source.height * scale
        val drawnLeft = (viewport.width - drawnWidth) / 2f + safeOffset.x
        val drawnTop = (viewport.height - drawnHeight) / 2f + safeOffset.y
        val crop = RectF(
            (-drawnLeft / scale).coerceIn(0f, source.width.toFloat()),
            (-drawnTop / scale).coerceIn(0f, source.height.toFloat()),
            ((viewport.width - drawnLeft) / scale).coerceIn(0f, source.width.toFloat()),
            ((viewport.height - drawnTop) / scale).coerceIn(0f, source.height.toFloat()),
        )
        val output = Bitmap.createBitmap(target.width, target.height, Bitmap.Config.ARGB_8888)
        Canvas(output).drawBitmap(
            source,
            Rect(
                crop.left.roundToInt(),
                crop.top.roundToInt(),
                crop.right.roundToInt(),
                crop.bottom.roundToInt(),
            ),
            Rect(0, 0, target.width, target.height),
            Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG),
        )
        return output
    }

    private fun baseScale(source: IntSize, viewport: IntSize): Float = max(
        viewport.width.toFloat() / source.width,
        viewport.height.toFloat() / source.height,
    )

    private fun sampleSizeFor(
        sourceWidth: Int,
        sourceHeight: Int,
        targetWidth: Int,
        targetHeight: Int,
    ): Int {
        val fittingRatio = minOf(
            sourceWidth.toFloat() / targetWidth.coerceAtLeast(1),
            sourceHeight.toFloat() / targetHeight.coerceAtLeast(1),
        )
        if (fittingRatio < 2f) return 1
        var sampleSize = 1
        while (sampleSize < 256 && sampleSize * 2 <= fittingRatio) sampleSize *= 2
        return sampleSize
    }
}
