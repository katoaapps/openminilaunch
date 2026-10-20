package com.katoaapps.openminilaunch.features.appearance

import androidx.annotation.DrawableRes
import com.katoaapps.openminilaunch.R

internal data class BundledWallpaper(
    val id: String,
    @param:DrawableRes val drawableRes: Int,
)

internal object BundledWallpaperCatalog {
    val wallpapers = listOf(
        BundledWallpaper("mink-01", R.drawable.mink_wallpaper_01),
        BundledWallpaper("mink-02", R.drawable.mink_wallpaper_02),
        BundledWallpaper("mink-03", R.drawable.mink_wallpaper_03),
        BundledWallpaper("mink-04", R.drawable.mink_wallpaper_04),
        BundledWallpaper("mink-05", R.drawable.mink_wallpaper_05),
        BundledWallpaper("mink-06", R.drawable.mink_wallpaper_06),
        BundledWallpaper("mink-07", R.drawable.mink_wallpaper_07),
        BundledWallpaper("mink-08", R.drawable.mink_wallpaper_08),
        BundledWallpaper("mink-09", R.drawable.mink_wallpaper_09),
        BundledWallpaper("mink-10", R.drawable.mink_wallpaper_10),
        BundledWallpaper("mink-11", R.drawable.mink_wallpaper_11),
        BundledWallpaper("mink-12", R.drawable.mink_wallpaper_12),
        BundledWallpaper("mink-13", R.drawable.mink_wallpaper_13),
        BundledWallpaper("mink-14", R.drawable.mink_wallpaper_14),
        BundledWallpaper("mink-15", R.drawable.mink_wallpaper_15),
        BundledWallpaper("mink-16", R.drawable.mink_wallpaper_16),
    )

    fun find(id: String?): BundledWallpaper? = wallpapers.firstOrNull { it.id == id }
}
