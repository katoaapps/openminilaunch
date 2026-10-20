package com.katoaapps.openminilaunch.features.appearance

import android.app.WallpaperManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import android.os.Handler
import android.os.Looper
import androidx.annotation.DrawableRes
import androidx.compose.runtime.mutableStateOf
import com.katoaapps.openminilaunch.model.HomeWallpaperAppearance
import java.io.File

internal class HomeWallpaperRepository(context: Context) {
    private val appContext = context.applicationContext
    private val wallpaperDirectory = File(appContext.filesDir, "appearance")
    private val wallpaperManager = WallpaperManager.getInstance(appContext)
    private val systemWallpaperAppearanceState = mutableStateOf(readSystemWallpaperAppearance())
    /** The user's retained gallery crop. Bundled wallpaper choices must not replace this file. */
    val wallpaperFile = File(wallpaperDirectory, "home-wallpaper.jpg")
    val systemWallpaperAppearance: HomeWallpaperAppearance?
        get() = systemWallpaperAppearanceState.value

    init {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            runCatching {
                wallpaperManager.addOnColorsChangedListener(
                    { colors, which ->
                        if (which and WallpaperManager.FLAG_SYSTEM != 0) {
                            systemWallpaperAppearanceState.value = colors?.primaryColor
                                ?.toArgb()
                                ?.let(::singleColorAppearance)
                        }
                    },
                    Handler(Looper.getMainLooper()),
                )
            }
        }
    }

    fun hasWallpaper(): Boolean = wallpaperFile.isFile && wallpaperFile.length() > 0L

    fun loadThumbnail(maximumSide: Int): Bitmap? = if (wallpaperFile.isFile) {
        decodeSampled(maximumSide) { options ->
            BitmapFactory.decodeFile(wallpaperFile.absolutePath, options)
        }
    } else null

    fun loadBundledThumbnail(@DrawableRes drawableRes: Int, maximumSide: Int): Bitmap? =
        decodeSampled(maximumSide) { options ->
            BitmapFactory.decodeResource(appContext.resources, drawableRes, options)
        }

    private fun decodeSampled(
        maximumSide: Int,
        decode: (BitmapFactory.Options) -> Bitmap?,
    ): Bitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        decode(bounds)
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
        var sampleSize = 1
        while (maxOf(bounds.outWidth, bounds.outHeight) / (sampleSize * 2) >= maximumSide) {
            sampleSize *= 2
        }
        return decode(BitmapFactory.Options().apply { inSampleSize = sampleSize })
    }

    fun readStoredAppearance(): HomeWallpaperAppearance? {
        val thumbnail = loadThumbnail(maximumSide = 256) ?: return null
        return try {
            HomeWallpaperAppearanceAnalyzer.analyze(thumbnail)
        } finally {
            thumbnail.recycle()
        }
    }

    fun readBundledAppearance(@DrawableRes drawableRes: Int): Result<HomeWallpaperAppearance> = runCatching {
        val thumbnail = checkNotNull(loadBundledThumbnail(drawableRes, maximumSide = 256)) {
            "Unable to decode bundled wallpaper"
        }
        try {
            HomeWallpaperAppearanceAnalyzer.analyze(thumbnail)
        } finally {
            thumbnail.recycle()
        }
    }

    /** Saves Mink's private copy before Android is asked to change the system wallpaper. */
    fun save(bitmap: Bitmap): Result<HomeWallpaperAppearance> = runCatching {
        val appearance = HomeWallpaperAppearanceAnalyzer.analyze(bitmap)
        wallpaperDirectory.mkdirs()
        val pendingFile = File(wallpaperDirectory, "home-wallpaper.pending.jpg")
        pendingFile.outputStream().buffered().use { output ->
            check(bitmap.compress(Bitmap.CompressFormat.JPEG, 94, output)) {
                "Unable to encode wallpaper"
            }
        }
        try {
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

    fun applyToSystem(): Result<Unit> = runCatching {
        check(hasWallpaper()) { "No saved wallpaper to apply" }
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(wallpaperFile.absolutePath, bounds)
        if (bounds.outWidth > 0 && bounds.outHeight > 0) {
            runCatching { wallpaperManager.suggestDesiredDimensions(bounds.outWidth, bounds.outHeight) }
        }
        wallpaperFile.inputStream().buffered().use { input ->
            wallpaperManager.setStream(
                input,
                null,
                false,
                WallpaperManager.FLAG_SYSTEM,
            )
        }
    }

    fun applyBundledToSystem(@DrawableRes drawableRes: Int): Result<Unit> = runCatching {
        appContext.resources.openRawResource(drawableRes).buffered().use { input ->
            wallpaperManager.setStream(
                input,
                null,
                false,
                WallpaperManager.FLAG_SYSTEM,
            )
        }
    }

    private fun readSystemWallpaperAppearance(): HomeWallpaperAppearance? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O_MR1) return null
        return runCatching {
            wallpaperManager.getWallpaperColors(WallpaperManager.FLAG_SYSTEM)
                ?.primaryColor
                ?.toArgb()
                ?.let(::singleColorAppearance)
        }.getOrNull()
    }

    private fun singleColorAppearance(argb: Int): HomeWallpaperAppearance =
        HomeWallpaperAppearance(averageColorArgb = argb, headerColorArgb = argb)
}
