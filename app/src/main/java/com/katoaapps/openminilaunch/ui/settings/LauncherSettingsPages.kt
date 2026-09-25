package com.katoaapps.openminilaunch.ui.settings

import com.katoaapps.openminilaunch.R
import com.katoaapps.openminilaunch.data.LauncherStore
import com.katoaapps.openminilaunch.features.appearance.HomeWallpaperRepository
import com.katoaapps.openminilaunch.model.MAX_DRAWER_APPS
import com.katoaapps.openminilaunch.model.PinShortcutRequestPresentation
import com.katoaapps.openminilaunch.model.Shortcut
import com.katoaapps.openminilaunch.model.configurableShortcuts
import com.katoaapps.openminilaunch.platform.DeviceActions
import com.katoaapps.openminilaunch.ui.components.SectionLabel
import com.katoaapps.openminilaunch.ui.components.SettingsRow
import com.katoaapps.openminilaunch.ui.components.SettingsSwitchRow
import com.katoaapps.openminilaunch.ui.launcher.ShortcutAssignmentRow
import com.katoaapps.openminilaunch.ui.launcher.displayLabel
import com.katoaapps.openminilaunch.ui.theme.Dimens
import com.katoaapps.openminilaunch.ui.theme.Muted
import com.katoaapps.openminilaunch.ui.theme.Sage

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.AddToHomeScreen
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import com.katoaapps.openminilaunch.features.localization.SupportedAppLanguages

@Composable
internal fun LauncherSettingsPage(
    store: LauncherStore,
    requestHomeRole: () -> Unit,
    onNavigate: (SettingsDestination) -> Unit,
    goBack: () -> Unit,
) {
    val appName = stringResource(R.string.app_name)
    val context = LocalContext.current
    val languageSummary = if (SupportedAppLanguages.isSupportedByDevice()) {
        SupportedAppLanguages.selectedLanguageTag(context)?.let { selected ->
            SupportedAppLanguages.all.firstOrNull {
                it.languageTag.equals(selected, ignoreCase = true)
            }?.nativeDisplayName() ?: selected
        } ?: stringResource(R.string.system_default)
    } else {
        stringResource(R.string.app_language_requires_android_13_short)
    }
    SettingsPage(stringResource(R.string.launcher), goBack) {
        SectionLabel(stringResource(R.string.system_roles))
        SettingsRow(
            stringResource(R.string.default_home_app),
            stringResource(R.string.choose_as_launcher, appName),
            Icons.Default.Home,
            onClick = requestHomeRole,
        )
        HorizontalDivider(color = Sage)
        SectionLabel(stringResource(R.string.customize))
        SettingsRow(
            stringResource(R.string.language),
            languageSummary,
            Icons.Default.Translate,
        ) { onNavigate(SettingsDestination.LANGUAGE) }
        SettingsRow(
            stringResource(R.string.appearance),
            stringResource(R.string.appearance_summary, stringResource(store.themePreference.labelRes)),
            Icons.Default.Palette,
        ) { onNavigate(SettingsDestination.APPEARANCE) }
        SettingsRow(
            stringResource(R.string.shortcuts_and_drawer),
            pluralStringResource(
                R.plurals.shortcuts_drawer_summary,
                store.drawerTargets.size,
                store.drawerTargets.size,
                MAX_DRAWER_APPS,
            ),
            Icons.Default.GridView,
        ) { onNavigate(SettingsDestination.SHORTCUTS) }
    }
}

@Composable
internal fun AppearanceSettingsPage(
    store: LauncherStore,
    goBack: () -> Unit,
    onIconStyleApplied: () -> Unit,
) {
    val context = LocalContext.current
    val wallpaperRepository = remember(context) { HomeWallpaperRepository(context) }
    var wallpaperToCrop by remember { mutableStateOf<Uri?>(null) }
    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        wallpaperToCrop = uri
    }
    SettingsPage(stringResource(R.string.appearance), goBack) {
        ThemeChooser(store.themePreference, store::setTheme)
        ClockAppearanceSettings(store)
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
        SectionLabel(stringResource(R.string.visual_style))
        IconPackAppearanceSettings(store, onApplied = onIconStyleApplied)
        HomePanelColorSetting(
            selectedArgb = store.effectiveHomePanelColorArgb,
            transparency = store.homePanelTransparency,
            onColorSelected = store::setHomePanelColor,
            onTransparencyChanged = store::setHomePanelTransparency,
        )
        AppBackgroundSetting(
            selectedArgb = store.effectiveAppBackgroundColorArgb,
            imageSelected = store.appBackgroundImageEnabled,
            imageRevision = store.appBackgroundImageRevision,
            onColorSelected = store::setAppBackgroundColor,
            onChooseImage = { imagePicker.launch(arrayOf("image/*")) },
            onUseThemeDefault = { store.setAppBackgroundColor(null) },
        )
    }
    wallpaperToCrop?.let { uri ->
        WallpaperCropDialog(
            imageUri = uri,
            applyWallpaper = wallpaperRepository::apply,
            onApplied = { wallpaper ->
                store.useAppBackgroundImage(wallpaper)
                wallpaperToCrop = null
                onIconStyleApplied()
            },
            onDismiss = { wallpaperToCrop = null },
        )
    }
}

@Composable
internal fun ShortcutsSettingsPage(
    store: LauncherStore,
    actions: DeviceActions,
    onPickShortcut: (Shortcut) -> Unit,
    onPickDrawer: () -> Unit,
    onNavigate: (SettingsDestination) -> Unit,
    goBack: () -> Unit,
) {
    val context = LocalContext.current
    SettingsPage(stringResource(R.string.shortcuts_and_drawer), goBack) {
        SectionLabel(stringResource(R.string.home_shortcuts))
        Text(
            stringResource(R.string.home_shortcuts_description),
            color = Muted,
            fontSize = Dimens.sp13,
        )
        configurableShortcuts.forEach { shortcut ->
            val targetKey = store.shortcutTargets[shortcut]
            ShortcutAssignmentRow(
                shortcut = shortcut,
                targetKey = targetKey,
                actions = actions,
                subtitle = targetKey?.let(actions::launcherTargetLabel)
                    ?: stringResource(R.string.shortcut_default, shortcut.displayLabel()),
            ) { onPickShortcut(shortcut) }
        }
        SettingsRow(
            stringResource(R.string.reset_home_grid_order),
            stringResource(R.string.reset_home_grid_order_description),
            Icons.Default.Restore,
        ) {
            store.resetShortcutOrder()
            Toast.makeText(
                context,
                context.getString(R.string.shortcut_order_reset),
                Toast.LENGTH_SHORT,
            ).show()
        }
        HorizontalDivider(color = Sage)
        SectionLabel(stringResource(R.string.app_drawer))
        SettingsRow(
            stringResource(R.string.selected_apps),
            stringResource(R.string.count_of_max, store.drawerTargets.size, MAX_DRAWER_APPS),
            Icons.Default.Apps,
            onClick = onPickDrawer,
        )
        Text(stringResource(R.string.magic_box_find_other_apps), color = Muted, fontSize = Dimens.sp13)
        HorizontalDivider(color = Sage)
        SectionLabel(stringResource(R.string.add_to_home_requests))
        SettingsRow(
            stringResource(R.string.add_to_home_confirmation),
            stringResource(
                when (store.pinShortcutRequestPresentation) {
                    PinShortcutRequestPresentation.FULL_PAGE -> R.string.pin_shortcut_full_page
                    PinShortcutRequestPresentation.CONFIRMATION_SHEET -> R.string.pin_shortcut_confirmation_sheet
                },
            ),
            Icons.AutoMirrored.Filled.AddToHomeScreen,
        ) { onNavigate(SettingsDestination.PIN_SHORTCUT_REQUESTS) }
    }
}
