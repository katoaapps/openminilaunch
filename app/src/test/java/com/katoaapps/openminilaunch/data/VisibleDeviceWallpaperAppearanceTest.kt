package com.katoaapps.openminilaunch.data

import com.katoaapps.openminilaunch.model.HomeWallpaperAppearance
import org.junit.Assert.assertEquals
import org.junit.Test

class VisibleDeviceWallpaperAppearanceTest {
    private val darkRetainedPhoto = HomeWallpaperAppearance(
        averageColorArgb = 0xFF090B0A.toInt(),
        headerColorArgb = 0xFF050706.toInt(),
    )
    private val misleadingSystemPalette = HomeWallpaperAppearance(
        averageColorArgb = 0xFFF4F1E8.toInt(),
        headerColorArgb = 0xFFF4F1E8.toInt(),
    )

    @Test
    fun retainedMinkPhotoWinsOverSystemPalette() {
        assertEquals(
            darkRetainedPhoto,
            selectVisibleDeviceWallpaperAppearance(
                retainedAppearance = darkRetainedPhoto,
                retainedWallpaperAvailable = true,
                systemAppearance = misleadingSystemPalette,
            ),
        )
    }

    @Test
    fun systemPaletteRemainsFallbackWithoutRetainedPhoto() {
        assertEquals(
            misleadingSystemPalette,
            selectVisibleDeviceWallpaperAppearance(
                retainedAppearance = darkRetainedPhoto,
                retainedWallpaperAvailable = false,
                systemAppearance = misleadingSystemPalette,
            ),
        )
    }
}
