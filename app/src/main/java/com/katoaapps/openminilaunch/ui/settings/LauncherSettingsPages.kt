package com.katoaapps.openminilaunch.ui.settings

import com.katoaapps.openminilaunch.R
import com.katoaapps.openminilaunch.data.LauncherStore
import com.katoaapps.openminilaunch.model.MAX_DRAWER_APPS
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

import android.widget.Toast
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource

@Composable
internal fun LauncherSettingsPage(
    store: LauncherStore,
    requestHomeRole: () -> Unit,
    onNavigate: (SettingsDestination) -> Unit,
    goBack: () -> Unit,
) {
    val appName = stringResource(R.string.app_name)
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
internal fun AppearanceSettingsPage(store: LauncherStore, goBack: () -> Unit) {
    SettingsPage(stringResource(R.string.appearance), goBack) {
        ThemeChooser(store.themePreference, store::setTheme)
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
        HomePanelColorSetting(store.effectiveHomePanelColorArgb, store::setHomePanelColor)
        AppBackgroundColorSetting(
            selectedArgb = store.effectiveAppBackgroundColorArgb,
            onColorSelected = store::setAppBackgroundColor,
            onUseThemeDefault = { store.setAppBackgroundColor(null) },
        )
    }
}

@Composable
internal fun ShortcutsSettingsPage(
    store: LauncherStore,
    actions: DeviceActions,
    onPickShortcut: (Shortcut) -> Unit,
    onPickDrawer: () -> Unit,
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
    }
}
