package com.katoaapps.openminilaunch.features.appearance

/** A curated complete Home look. Wallpaper can be overridden later without losing its colors. */
internal data class BundledHomeTheme(
    val id: String,
    val wallpaper: BundledWallpaper?,
    val backgroundColorArgb: Int,
    val panelColorArgb: Int,
)

internal object BundledHomeThemeCatalog {
    const val MINK_THEME_ID = "mink-theme"

    val minkTheme = BundledHomeTheme(
        id = MINK_THEME_ID,
        wallpaper = null,
        backgroundColorArgb = 0xFF173529.toInt(),
        panelColorArgb = 0xFF602C00.toInt(),
    )

    val themes = listOf(
        minkTheme,
        theme("mink-01", background = 0xFFF4EFE3, panel = 0xFFB7D530),
        theme("mink-02", background = 0xFF111A20, panel = 0xFF9B6B35),
        theme("mink-03", background = 0xFFE9E5DE, panel = 0xFF7A5137),
        theme("mink-04", background = 0xFFFFF3D8, panel = 0xFFE74B63),
        theme("mink-05", background = 0xFFF1D7B2, panel = 0xFFA45C32),
        theme("mink-06", background = 0xFFE0EEFF, panel = 0xFFB386C5),
        theme("mink-07", background = 0xFFF0EDE5, panel = 0xFFE84A3C),
        theme("mink-08", background = 0xFF120B35, panel = 0xFFE83ADB),
        theme("mink-09", background = 0xFFCCB7F2, panel = 0xFF54C6E8),
        theme("mink-10", background = 0xFFF9D915, panel = 0xFFD82946),
        theme("mink-11", background = 0xFF151A19, panel = 0xFFB7E100),
        theme("mink-12", background = 0xFFD7DEDC, panel = 0xFFE86B2C),
        theme("mink-13", background = 0xFFF1E2D2, panel = 0xFFA45A63),
        theme("mink-14", background = 0xFFCFF6FA, panel = 0xFF3F9FB7),
        theme("mink-15", background = 0xFFF2EDE4, panel = 0xFF244A72),
        theme("mink-16", background = 0xFFDCE8FF, panel = 0xFF775FD3),
    )

    fun find(id: String?): BundledHomeTheme? = themes.firstOrNull { it.id == id }

    fun findByColors(backgroundColorArgb: Int, panelColorArgb: Int): BundledHomeTheme? =
        themes.firstOrNull { theme ->
            theme.backgroundColorArgb == backgroundColorArgb &&
                theme.panelColorArgb == panelColorArgb
        }

    private fun theme(id: String, background: Long, panel: Long): BundledHomeTheme =
        BundledHomeTheme(
            id = id,
            wallpaper = checkNotNull(BundledWallpaperCatalog.find(id)),
            backgroundColorArgb = background.toInt(),
            panelColorArgb = panel.toInt(),
        )
}
