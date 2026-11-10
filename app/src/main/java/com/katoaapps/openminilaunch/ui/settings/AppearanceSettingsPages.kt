package com.katoaapps.openminilaunch.ui.settings

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Wallpaper
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.core.content.ContextCompat
import com.katoaapps.openminilaunch.R
import com.katoaapps.openminilaunch.data.LauncherStore
import com.katoaapps.openminilaunch.features.appearance.HomeWallpaperRepository
import com.katoaapps.openminilaunch.features.appearance.WATERMELON_HOTPHRASE
import com.katoaapps.openminilaunch.features.appearance.BundledHomeTheme
import com.katoaapps.openminilaunch.features.appearance.applyBundledHomeTheme
import com.katoaapps.openminilaunch.features.appearance.applyHomeThemeWallpaper
import com.katoaapps.openminilaunch.features.appearance.restoreDeviceWallpaper
import com.katoaapps.openminilaunch.ui.components.SectionLabel
import com.katoaapps.openminilaunch.ui.components.SettingsRow
import com.katoaapps.openminilaunch.ui.components.SettingsSwitchRow
import kotlinx.coroutines.launch

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
    var applyingSelectionId by remember { mutableStateOf<String?>(null) }
    var themeAwaitingWallpaperChoice by remember { mutableStateOf<BundledHomeTheme?>(null) }
    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        wallpaperToCrop = uri
    }

    SettingsPage(stringResource(R.string.appearance_wallpaper_title), goBack) {
        val customBackgroundArgb = store.appBackgroundColorArgb
            ?: ContextCompat.getColor(context, R.color.mink_forest)
        HomeThemeSetting(
            selectedThemeId = store.selectedHomeTheme?.id,
            applyingThemeId = applyingSelectionId,
            themeBackgroundSelected = !store.appBackgroundImageEnabled &&
                !store.useSystemHomeWallpaper,
            deviceWallpaperSelected = store.useSystemHomeWallpaper ||
                store.appBackgroundImageEnabled,
            savedDeviceWallpaperAvailable = store.hasSavedAppBackgroundImage,
            savedDeviceWallpaperRevision = store.appBackgroundImageRevision,
            applyingWallpaperSourceId = applyingSelectionId?.takeIf {
                it == DEVICE_WALLPAPER_SELECTION_ID ||
                    it == THEME_BACKGROUND_SELECTION_ID
            },
            customBackgroundArgb = customBackgroundArgb,
            customPanelArgb = store.homePanelColorArgb,
            panelTransparency = store.homePanelTransparency,
            onUseThemeBackground = {
                val selectedTheme = store.selectedHomeTheme
                if (selectedTheme == null) {
                    store.useThemeHomeBackground()
                } else {
                    applyingSelectionId = THEME_BACKGROUND_SELECTION_ID
                    scope.launch {
                        val result = applyHomeThemeWallpaper(
                            store,
                            wallpaperRepository,
                            selectedTheme,
                        )
                        applyingSelectionId = null
                        result.onFailure {
                            Toast.makeText(
                                context,
                                wallpaperApplyFailedMessage,
                                Toast.LENGTH_LONG,
                            ).show()
                        }
                    }
                }
            },
            onUseDeviceWallpaper = {
                applyingSelectionId = DEVICE_WALLPAPER_SELECTION_ID
                scope.launch {
                    val result = restoreDeviceWallpaper(store, wallpaperRepository)
                    applyingSelectionId = null
                    result.onFailure {
                        Toast.makeText(
                            context,
                            wallpaperApplyFailedMessage,
                            Toast.LENGTH_LONG,
                        ).show()
                    }
                }
            },
            onChoosePhoto = { imagePicker.launch(arrayOf("image/*")) },
            onSelectTheme = { theme ->
                if (
                    shouldConfirmThemeWallpaperReplacement(
                        appPhotoSelected = store.appBackgroundImageEnabled,
                        deviceWallpaperSelected = store.useSystemHomeWallpaper,
                    )
                ) {
                    themeAwaitingWallpaperChoice = theme
                } else {
                    applyingSelectionId = theme.id
                    scope.launch {
                        val result = applyBundledHomeTheme(store, wallpaperRepository, theme)
                        applyingSelectionId = null
                        result.onFailure {
                            Toast.makeText(
                                context,
                                wallpaperApplyFailedMessage,
                                Toast.LENGTH_LONG,
                            ).show()
                        }
                    }
                }
            },
            onCustomBackgroundSelected = store::setCustomThemeBackgroundColor,
            onCustomPanelSelected = { input, argb ->
                if (input.trim().equals(WATERMELON_HOTPHRASE, ignoreCase = true)) {
                    store.activateWatermelonMode()
                } else {
                    store.setHomePanelColor(argb)
                }
            },
            onTransparencyChanged = store::setHomePanelTransparency,
        )
        PaletteChooser(store.palettePreference, store::setPalette)
        ThemeChooser(store.themePreference, store::setTheme)
    }

    wallpaperToCrop?.let { uri ->
        WallpaperCropDialog(
            imageUri = uri,
            saveWallpaper = wallpaperRepository::save,
            applySystemWallpaper = wallpaperRepository::applyToSystem,
            onWallpaperSaved = store::retainAndUseSystemHomeWallpaper,
            onApplied = {
                wallpaperToCrop = null
                onIconStyleApplied()
            },
            onDismiss = { wallpaperToCrop = null },
        )
    }

    themeAwaitingWallpaperChoice?.let { theme ->
        AlertDialog(
            onDismissRequest = { themeAwaitingWallpaperChoice = null },
            title = { Text(stringResource(R.string.theme_wallpaper_choice_title)) },
            text = { Text(stringResource(R.string.theme_wallpaper_choice_description)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        themeAwaitingWallpaperChoice = null
                        applyingSelectionId = theme.id
                        scope.launch {
                            val result = applyBundledHomeTheme(store, wallpaperRepository, theme)
                            applyingSelectionId = null
                            result.onFailure {
                                Toast.makeText(
                                    context,
                                    wallpaperApplyFailedMessage,
                                    Toast.LENGTH_LONG,
                                ).show()
                            }
                        }
                    },
                ) { Text(stringResource(R.string.use_theme_wallpaper)) }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        store.applyHomeTheme(theme)
                        themeAwaitingWallpaperChoice = null
                    },
                ) { Text(stringResource(R.string.keep_my_wallpaper)) }
            },
        )
    }
}

internal fun shouldConfirmThemeWallpaperReplacement(
    appPhotoSelected: Boolean,
    deviceWallpaperSelected: Boolean,
): Boolean = appPhotoSelected || deviceWallpaperSelected

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
