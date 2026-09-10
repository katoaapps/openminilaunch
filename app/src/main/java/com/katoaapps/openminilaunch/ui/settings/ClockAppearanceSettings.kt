package com.katoaapps.openminilaunch.ui.settings

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.katoaapps.openminilaunch.R
import com.katoaapps.openminilaunch.data.LauncherStore
import com.katoaapps.openminilaunch.ui.components.SectionLabel
import com.katoaapps.openminilaunch.ui.components.SettingsSwitchRow

@Composable
internal fun ClockAppearanceSettings(store: LauncherStore) {
    SectionLabel(stringResource(R.string.date_and_time))
    SettingsSwitchRow(
        title = stringResource(R.string.show_clock),
        subtitle = stringResource(R.string.show_clock_description),
        checked = store.showClock,
        onCheckedChange = store::updateShowClock,
    )
    if (store.showClock) {
        SettingsSwitchRow(
            title = stringResource(R.string.use_24_hour_clock),
            subtitle = stringResource(R.string.use_24_hour_clock_description),
            checked = store.use24HourClock,
            onCheckedChange = store::updateUse24HourClock,
        )
    }
}
