package com.katoaapps.openminilaunch.features.appearance

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class BundledWallpaperCatalogTest {
    @Test fun catalogKeepsSixteenStableUniqueChoices() {
        val wallpapers = BundledWallpaperCatalog.wallpapers

        assertEquals(16, wallpapers.size)
        assertEquals(wallpapers.size, wallpapers.map { it.id }.distinct().size)
        assertEquals(wallpapers.size, wallpapers.map { it.drawableRes }.distinct().size)
        wallpapers.forEach { wallpaper -> assertNotNull(BundledWallpaperCatalog.find(wallpaper.id)) }
    }
}
