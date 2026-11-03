package com.katoaapps.openminilaunch.ui.settings

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.katoaapps.openminilaunch.R
import com.katoaapps.openminilaunch.data.LauncherStore
import com.katoaapps.openminilaunch.model.MAX_DRAWER_APPS
import com.katoaapps.openminilaunch.platform.DeviceActions
import com.katoaapps.openminilaunch.ui.components.SectionLabel
import com.katoaapps.openminilaunch.ui.components.SettingsRow
import com.katoaapps.openminilaunch.ui.components.SettingsSwitchRow
import com.katoaapps.openminilaunch.ui.theme.Dimens
import com.katoaapps.openminilaunch.ui.theme.Muted

@Composable
internal fun QuickDrawerSettingsPage(
    store: LauncherStore,
    actions: DeviceActions,
    onPickDrawer: () -> Unit,
    onNavigate: (SettingsDestination) -> Unit,
    goBack: () -> Unit,
) {
    val privateSpaceAvailable = store.demoSearchDataEnabled ||
        actions.privateContainerEntryPointAvailable(store.privateContainerGatewaySelectionKey)
    val workProfileAvailable = store.demoSearchDataEnabled || actions.workProfileAvailable()

    SettingsPage(stringResource(R.string.quick_drawer), goBack) {
        SectionLabel(stringResource(R.string.apps))
        SettingsRow(
            stringResource(R.string.selected_apps),
            stringResource(R.string.count_of_max, store.drawerTargets.size, MAX_DRAWER_APPS),
            Icons.Default.GridView,
            onClick = onPickDrawer,
        )
        Text(
            stringResource(R.string.quick_drawer_apps_description),
            color = Muted,
            fontSize = Dimens.sp13,
        )

        SectionLabel(stringResource(R.string.quick_drawer_controls))
        SettingsSwitchRow(
            title = stringResource(R.string.show_minkpad_button),
            subtitle = stringResource(R.string.show_minkpad_button_description),
            checked = store.showMinkPadInQuickDrawer,
            onCheckedChange = store::updateShowMinkPadInQuickDrawer,
        )
        SettingsRow(
            title = stringResource(R.string.privacy_apps),
            subtitle = stringResource(R.string.privacy_apps_settings_summary),
            icon = Icons.Default.Shield,
        ) { onNavigate(SettingsDestination.PRIVACY_APPS) }
        if (privateSpaceAvailable) {
            SettingsSwitchRow(
                title = stringResource(R.string.show_private_space_button),
                subtitle = stringResource(R.string.show_private_space_button_description),
                checked = store.showPrivateSpaceInQuickDrawer,
                onCheckedChange = store::updateShowPrivateSpaceInQuickDrawer,
            )
        }
        if (workProfileAvailable) {
            SettingsSwitchRow(
                title = stringResource(R.string.show_work_profile_button),
                subtitle = stringResource(R.string.show_work_profile_button_description),
                checked = store.showWorkProfileInQuickDrawer,
                onCheckedChange = store::updateShowWorkProfileInQuickDrawer,
            )
        }
    }
}
