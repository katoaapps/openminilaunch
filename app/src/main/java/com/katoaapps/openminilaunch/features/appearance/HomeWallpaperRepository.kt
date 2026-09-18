package com.katoaapps.openminilaunch.features.appearance

import android.app.WallpaperManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import java.io.File

internal data class AppliedHomeWallpaper(
    val averageColorArgb: Int,
    val headerColorArgb: Int,
)

internal class HomeWallpaperRepository(context: Context) {
    private val appContext = context.applicationContext
    private val wallpaperDirectory = File(appContext.filesDir, "appearance")
    val wallpaperFile = File(wallpaperDirectory, "home-wallpaper.jpg")

    fun hasWallpaper(): Boolean = wallpaperFile.isFile && wallpaperFile.length() > 0L

    fun loadThumbnail(maximumSide: Int): Bitmap? {
        if (!wallpaperFile.isFile) return null
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(wallpaperFile.absolutePath, bounds)
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
        var sampleSize = 1
        while (maxOf(bounds.outWidth, bounds.outHeight) / (sampleSize * 2) >= maximumSide) {
            sampleSize *= 2
        }
        return BitmapFactory.decodeFile(
            wallpaperFile.absolutePath,
            BitmapFactory.Options().apply { inSampleSize = sampleSize },
        )
    }

    fun readStoredAppearance(): AppliedHomeWallpaper? {
        val thumbnail = loadThumbnail(maximumSide = 256) ?: return null
        return try {
            thumbnail.appearance()
        } finally {
            thumbnail.recycle()
        }
    }

    fun apply(bitmap: Bitmap): Result<AppliedHomeWallpaper> = runCatching {
        val appearance = bitmap.appearance()
        wallpaperDirectory.mkdirs()
        val pendingFile = File(wallpaperDirectory, "home-wallpaper.pending.jpg")
        pendingFile.outputStream().buffered().use { output ->
            check(bitmap.compress(Bitmap.CompressFormat.JPEG, 94, output)) {
                "Unable to encode wallpaper"
            }
        }

        try {
            val wallpaperManager = WallpaperManager.getInstance(appContext)
            runCatching { wallpaperManager.suggestDesiredDimensions(bitmap.width, bitmap.height) }
            pendingFile.inputStream().buffered().use { input ->
                wallpaperManager.setStream(
                    input,
                    null,
                    false,
                    WallpaperManager.FLAG_SYSTEM,
                )
            }
            check(
                pendingFile.renameTo(wallpaperFile) ||
                    pendingFile.copyTo(wallpaperFile, overwrite = true).exists(),
            ) {
                "Unable to save wallpaper"
            }
        } finally {
            pendingFile.delete()
        }
        appearance
    }

    private fun Bitmap.appearance() = AppliedHomeWallpaper(
        averageColorArgb = averageColorBetween(0f, 1f),
        headerColorArgb = averageColorBetween(0f, HEADER_SAMPLE_END_FRACTION),
    )

    private fun Bitmap.averageColorBetween(startYFraction: Float, endYFraction: Float): Int {
        val startY = (height * startYFraction).toInt().coerceIn(0, height - 1)
        val endY = (height * endYFraction).toInt().coerceIn(startY + 1, height)
        val horizontalSamples = minOf(width, COLOR_SAMPLE_COLUMNS)
        val verticalSamples = minOf(endY - startY, COLOR_SAMPLE_ROWS)
        var red = 0L
        var green = 0L
        var blue = 0L
        var count = 0L
        repeat(verticalSamples) { row ->
            val y = startY + ((row + .5f) * (endY - startY) / verticalSamples).toInt()
                .coerceAtMost(endY - startY - 1)
            repeat(horizontalSamples) { column ->
                val x = ((column + .5f) * width / horizontalSamples).toInt()
                    .coerceAtMost(width - 1)
                val pixel = getPixel(x, y)
                red += Color.red(pixel)
                green += Color.green(pixel)
                blue += Color.blue(pixel)
                count++
            }
        }
        return Color.rgb(
            (red / count).toInt(),
            (green / count).toInt(),
            (blue / count).toInt(),
        )
    }

    private companion object {
        const val HEADER_SAMPLE_END_FRACTION = .18f
        const val COLOR_SAMPLE_COLUMNS = 48
        const val COLOR_SAMPLE_ROWS = 32
    }
}
