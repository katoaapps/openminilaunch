package com.katoaapps.openminilaunch.features.appearance

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class BundledHomeThemeCatalogTest {
    @Test fun everyWallpaperHasOneCompleteHomeTheme() {
        val wallpapers = BundledWallpaperCatalog.wallpapers
        val themes = BundledHomeThemeCatalog.themes

        assertEquals(16, themes.size)
        assertEquals(wallpapers.map { it.id }, themes.map { it.id })
        assertEquals(themes.size, themes.map { it.backgroundColorArgb }.distinct().size)
        assertEquals(themes.size, themes.map { it.panelColorArgb }.distinct().size)
        themes.forEach { theme -> assertNotNull(BundledHomeThemeCatalog.find(theme.id)) }
    }
}
