package com.katoaapps.openminilaunch.features.appearance

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertSame
import org.junit.Test

class BundledHomeThemeCatalogTest {
    @Test fun catalogKeepsMinkDefaultAndOneColorThemePerWallpaper() {
        val wallpapers = BundledWallpaperCatalog.wallpapers
        val themes = BundledHomeThemeCatalog.themes

        assertEquals(17, themes.size)
        assertSame(BundledHomeThemeCatalog.minkTheme, themes.first())
        assertEquals(
            wallpapers.map { it.id },
            themes.drop(1).map { it.wallpaper?.id },
        )
        assertEquals(themes.size, themes.map { it.backgroundColorArgb }.distinct().size)
        assertEquals(themes.size, themes.map { it.panelColorArgb }.distinct().size)
        themes.forEach { theme -> assertNotNull(BundledHomeThemeCatalog.find(theme.id)) }
    }

    @Test fun themeCanBeRecoveredFromItsPortableColors() {
        BundledHomeThemeCatalog.themes.forEach { theme ->
            assertSame(
                theme,
                BundledHomeThemeCatalog.findByColors(
                    theme.backgroundColorArgb,
                    theme.panelColorArgb,
                ),
            )
        }
    }
}
