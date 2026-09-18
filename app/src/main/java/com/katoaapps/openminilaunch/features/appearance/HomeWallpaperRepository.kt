package com.katoaapps.openminilaunch.features.appearance

import android.app.WallpaperManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.katoaapps.openminilaunch.model.HomeWallpaperAppearance
import java.io.File

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

    fun readStoredAppearance(): HomeWallpaperAppearance? {
        val thumbnail = loadThumbnail(maximumSide = 256) ?: return null
        return try {
            HomeWallpaperAppearanceAnalyzer.analyze(thumbnail)
        } finally {
            thumbnail.recycle()
        }
    }

    fun apply(bitmap: Bitmap): Result<HomeWallpaperAppearance> = runCatching {
        val appearance = HomeWallpaperAppearanceAnalyzer.analyze(bitmap)
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
}
