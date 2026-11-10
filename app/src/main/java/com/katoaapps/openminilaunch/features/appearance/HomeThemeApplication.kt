package com.katoaapps.openminilaunch.features.appearance

import com.katoaapps.openminilaunch.data.LauncherStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Applies a complete curated theme, including its paired wallpaper or solid Mink background. */
internal suspend fun applyBundledHomeTheme(
    store: LauncherStore,
    repository: HomeWallpaperRepository,
    theme: BundledHomeTheme,
): Result<Unit> = applyThemeWallpaper(store, repository, theme) {
    store.applyHomeTheme(theme)
}
/** Restores the selected theme's own wallpaper without changing its colors. */
internal suspend fun applyHomeThemeWallpaper(
    store: LauncherStore,
    repository: HomeWallpaperRepository,
    theme: BundledHomeTheme,
): Result<Unit> = applyThemeWallpaper(store, repository, theme)

/**
 * Returns Android to the last gallery photo chosen through Mink when one is available.
 * Without a retained Mink photo, this simply reveals the device's current system wallpaper.
 */
internal suspend fun restoreDeviceWallpaper(
    store: LauncherStore,
    repository: HomeWallpaperRepository,
): Result<Unit> {
    // Persist the destination before WallpaperManager can recreate the Activity after a
    // dynamic-color update. The retained photo is never deleted by theme selection.
    store.useSystemHomeWallpaper()
    return if (repository.hasWallpaper()) {
        withContext(Dispatchers.IO) { repository.applyToSystem() }
    } else {
        Result.success(Unit)
    }
}

private suspend fun applyThemeWallpaper(
    store: LauncherStore,
    repository: HomeWallpaperRepository,
    theme: BundledHomeTheme,
    beforePersistingWallpaper: () -> Unit = {},
): Result<Unit> {
    val wallpaper = theme.wallpaper
    if (wallpaper == null) {
        beforePersistingWallpaper()
        store.useThemeHomeBackground()
        return Result.success(Unit)
    }
    val appearanceResult = withContext(Dispatchers.IO) {
        repository.readBundledAppearance(wallpaper.drawableRes)
    }
    return appearanceResult.fold(
        onSuccess = { appearance ->
            // Persist both halves before WallpaperManager can recreate the Activity.
            beforePersistingWallpaper()
            store.useAppBackgroundPreset(wallpaper.id, appearance)
            withContext(Dispatchers.IO) {
                repository.applyBundledToSystem(wallpaper.drawableRes)
            }
        },
        onFailure = { Result.failure(it) },
    )
}
