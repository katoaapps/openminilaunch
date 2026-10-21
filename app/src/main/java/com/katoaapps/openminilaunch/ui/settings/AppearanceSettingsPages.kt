package com.katoaapps.openminilaunch.ui.settings

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Wallpaper
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.katoaapps.openminilaunch.R
import com.katoaapps.openminilaunch.data.LauncherStore
import com.katoaapps.openminilaunch.features.appearance.HomeWallpaperRepository
import com.katoaapps.openminilaunch.ui.components.SectionLabel
import com.katoaapps.openminilaunch.ui.components.SettingsRow
import com.katoaapps.openminilaunch.ui.components.SettingsSwitchRow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
internal fun AppearanceSettingsPage(
    store: LauncherStore,
    onNavigate: (SettingsDestination) -> Unit,
    goBack: () -> Unit,
) {
    SettingsPage(stringResource(R.string.appearance), goBack) {
        SettingsRow(
            title = stringResource(R.string.appearance_wallpaper_title),
            subtitle = stringResource(R.string.appearance_wallpaper_summary),
            icon = Icons.Default.Wallpaper,
        ) { onNavigate(SettingsDestination.APPEARANCE_WALLPAPER) }
        SettingsRow(
            title = stringResource(R.string.appearance_icons_fonts_title),
            subtitle = stringResource(R.string.appearance_icons_fonts_summary),
            icon = Icons.Default.Apps,
        ) { onNavigate(SettingsDestination.APPEARANCE_ICONS) }
        SettingsRow(
            title = stringResource(R.string.appearance_layout_clock_title),
            subtitle = stringResource(R.string.appearance_layout_clock_summary),
            icon = Icons.Default.AccessTime,
        ) { onNavigate(SettingsDestination.APPEARANCE_LAYOUT_CLOCK) }
    }
}

@Composable
internal fun WallpaperAndBackgroundSettingsPage(
    store: LauncherStore,
    onIconStyleApplied: () -> Unit,
    goBack: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val wallpaperRepository = remember(context) { HomeWallpaperRepository(context) }
    val wallpaperApplyFailedMessage = stringResource(R.string.wallpaper_apply_failed)
    var wallpaperToCrop by remember { mutableStateOf<Uri?>(null) }
    var applyingWallpaperId by remember { mutableStateOf<String?>(null) }
    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        wallpaperToCrop = uri
    }

    SettingsPage(stringResource(R.string.appearance_wallpaper_title), goBack) {
        val activeWallpaperLabel = when {
            store.appBackgroundImageEnabled -> stringResource(R.string.your_photo)
            store.appBackgroundPreset != null -> stringResource(R.string.mink_wallpaper)
            else -> null
        }
        ThemeChooser(store.themePreference, store::setTheme)
        HomePanelColorSetting(
            selectedArgb = store.effectiveHomePanelColorArgb,
            transparency = store.homePanelTransparency,
            onColorSelected = store::setHomePanelColor,
            onWatermelonSelected = store::activateWatermelonMode,
            onTransparencyChanged = store::setHomePanelTransparency,
        )
        AppBackgroundSetting(
            selectedArgb = store.effectiveAppBackgroundColorArgb,
            activeWallpaperLabel = activeWallpaperLabel,
            onColorSelected = store::setAppBackgroundColor,
            onUseThemeDefault = { store.setAppBackgroundColor(null) },
        )
        WallpaperLibrarySetting(
            selectedPresetId = store.appBackgroundPreset?.id,
            customPhotoSelected = store.appBackgroundImageEnabled,
            savedPhotoAvailable = store.hasSavedAppBackgroundImage,
            imageRevision = store.appBackgroundImageRevision,
            applyingSelectionId = applyingWallpaperId,
            onChoosePhoto = { imagePicker.launch(arrayOf("image/*")) },
            onUseSavedPhoto = {
                applyingWallpaperId = CUSTOM_PHOTO_SELECTION_ID
                scope.launch {
                    val appearanceResult = withContext(Dispatchers.IO) {
                        runCatching {
                            checkNotNull(wallpaperRepository.readStoredAppearance()) {
                                "Saved wallpaper is unavailable"
                            }
                        }
                    }
                    val result = appearanceResult.fold(
                        onSuccess = { appearance ->
                            // Match the crop flow: save the source before WallpaperManager can
                            // recreate the Activity after a dynamic color update.
                            store.useAppBackgroundImage(appearance)
                            withContext(Dispatchers.IO) { wallpaperRepository.applyToSystem() }
                        },
                        onFailure = { Result.failure(it) },
                    )
                    applyingWallpaperId = null
                    result.onFailure {
                        Toast.makeText(
                            context,
                            wallpaperApplyFailedMessage,
                            Toast.LENGTH_LONG,
                        ).show()
                    }
                }
            },
            onSelectPreset = { wallpaper ->
                applyingWallpaperId = wallpaper.id
                scope.launch {
                    val appearanceResult = withContext(Dispatchers.IO) {
                        wallpaperRepository.readBundledAppearance(wallpaper.drawableRes)
                    }
                    val result = appearanceResult.fold(
                        onSuccess = { appearance ->
                            // Save the source before WallpaperManager can trigger an Activity
                            // recreation through dynamic color updates on some devices.
                            store.useAppBackgroundPreset(wallpaper.id, appearance)
                            withContext(Dispatchers.IO) {
                                wallpaperRepository.applyBundledToSystem(wallpaper.drawableRes)
                            }
                        },
                        onFailure = { Result.failure(it) },
                    )
                    applyingWallpaperId = null
                    result.onFailure {
                        Toast.makeText(
                            context,
                            wallpaperApplyFailedMessage,
                            Toast.LENGTH_LONG,
                        ).show()
                    }
                }
            },
        )
    }

    wallpaperToCrop?.let { uri ->
        WallpaperCropDialog(
            imageUri = uri,
            saveWallpaper = wallpaperRepository::save,
            applySystemWallpaper = wallpaperRepository::applyToSystem,
            onWallpaperSaved = store::useAppBackgroundImage,
            onApplied = {
                wallpaperToCrop = null
                onIconStyleApplied()
            },
            onDismiss = { wallpaperToCrop = null },
        )
    }
}

@Composable
internal fun IconsAndFontsSettingsPage(
    store: LauncherStore,
    onIconStyleApplied: () -> Unit,
    goBack: () -> Unit,
) {
    SettingsPage(stringResource(R.string.appearance_icons_fonts_title), goBack) {
        SectionLabel(stringResource(R.string.icons))
        IconPackAppearanceSettings(store, onApplied = onIconStyleApplied)
        SectionLabel(stringResource(R.string.fonts))
        FontAppearanceSettings(store)
    }
}

@Composable
internal fun LayoutAndClockSettingsPage(store: LauncherStore, goBack: () -> Unit) {
    SettingsPage(stringResource(R.string.appearance_layout_clock_title), goBack) {
        ClockAppearanceSettings(store)
        SectionLabel(stringResource(R.string.appearance_layout_section))
        SettingsSwitchRow(
            title = stringResource(R.string.hide_status_bar),
            subtitle = stringResource(R.string.hide_status_bar_description),
            checked = store.hideStatusBar,
            onCheckedChange = store::updateHideStatusBar,
        )
        SettingsSwitchRow(
            title = stringResource(R.string.align_pill_to_bottom),
            subtitle = stringResource(R.string.align_pill_to_bottom_description),
            checked = store.alignHomePanelBottom,
            onCheckedChange = store::updateAlignHomePanelBottom,
        )
        SettingsSwitchRow(
            title = stringResource(R.string.two_panel_mode_for_large_displays),
            subtitle = stringResource(R.string.two_panel_mode_for_large_displays_description),
            checked = store.twoPanelModeForLargeDisplays,
            onCheckedChange = store::updateTwoPanelModeForLargeDisplays,
        )
    }
}
