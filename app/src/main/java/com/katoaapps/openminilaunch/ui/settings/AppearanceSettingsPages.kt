package com.katoaapps.openminilaunch.ui.settings

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Palette
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
            title = stringResource(R.string.appearance_theme_colors_title),
            subtitle = stringResource(R.string.appearance_theme_colors_summary),
            icon = Icons.Default.Palette,
        ) { onNavigate(SettingsDestination.APPEARANCE_THEME_COLORS) }
        SettingsRow(
            title = stringResource(R.string.icons),
            subtitle = stringResource(R.string.appearance_icons_summary),
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
    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        wallpaperToCrop = uri
    }

    SettingsPage(stringResource(R.string.appearance_wallpaper_title), goBack) {
        AppBackgroundSetting(
            selectedArgb = store.effectiveAppBackgroundColorArgb,
            imageSelected = store.appBackgroundImageEnabled,
            savedImageAvailable = store.hasSavedAppBackgroundImage,
            imageRevision = store.appBackgroundImageRevision,
            onColorSelected = store::setAppBackgroundColor,
            onChooseImage = { imagePicker.launch(arrayOf("image/*")) },
            onUseSavedImage = {
                scope.launch {
                    val result = withContext(Dispatchers.IO) {
                        val appearance = wallpaperRepository.readStoredAppearance()
                            ?: return@withContext Result.failure(
                                IllegalStateException("Saved wallpaper is unavailable"),
                            )
                        wallpaperRepository.applyToSystem().map { appearance }
                    }
                    result.fold(
                        onSuccess = { appearance ->
                            store.useAppBackgroundImage(appearance)
                        },
                        onFailure = {
                            Toast.makeText(
                                context,
                                wallpaperApplyFailedMessage,
                                Toast.LENGTH_LONG,
                            ).show()
                        },
                    )
                }
            },
            onUseThemeDefault = { store.setAppBackgroundColor(null) },
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
internal fun ThemeAndColorsSettingsPage(store: LauncherStore, goBack: () -> Unit) {
    SettingsPage(stringResource(R.string.appearance_theme_colors_title), goBack) {
        ThemeChooser(store.themePreference, store::setTheme)
        HomePanelColorSetting(
            selectedArgb = store.effectiveHomePanelColorArgb,
            transparency = store.homePanelTransparency,
            onColorSelected = store::setHomePanelColor,
            onWatermelonSelected = store::activateWatermelonMode,
            onTransparencyChanged = store::setHomePanelTransparency,
        )
    }
}

@Composable
internal fun IconSettingsPage(
    store: LauncherStore,
    onIconStyleApplied: () -> Unit,
    goBack: () -> Unit,
) {
    SettingsPage(stringResource(R.string.icons), goBack) {
        IconPackAppearanceSettings(store, onApplied = onIconStyleApplied)
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
