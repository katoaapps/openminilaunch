package com.katoaapps.openminilaunch.ui.settings

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Translate
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.katoaapps.openminilaunch.R
import com.katoaapps.openminilaunch.data.LauncherStore
import com.katoaapps.openminilaunch.features.localization.SupportedAppLanguages
import com.katoaapps.openminilaunch.model.AppGridSlot
import com.katoaapps.openminilaunch.platform.DeviceActions
import com.katoaapps.openminilaunch.ui.appgrid.AppGridOrderEditor
import com.katoaapps.openminilaunch.ui.components.SectionLabel
import com.katoaapps.openminilaunch.ui.components.SettingsRow

@Composable
internal fun LauncherSettingsPage(
    store: LauncherStore,
    onNavigate: (SettingsDestination) -> Unit,
    goBack: () -> Unit,
) {
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
    SettingsPage(stringResource(R.string.settings_section_home), goBack) {
        SectionLabel(stringResource(R.string.customize))
        SettingsRow(
            stringResource(R.string.appearance),
            stringResource(R.string.appearance_summary, stringResource(store.themePreference.labelRes)),
            Icons.Default.Palette,
        ) { onNavigate(SettingsDestination.APPEARANCE) }
        SettingsRow(
            stringResource(R.string.language),
            languageSummary,
            Icons.Default.Translate,
        ) { onNavigate(SettingsDestination.LANGUAGE) }
    }
}

@Composable
internal fun AppGridSettingsPage(
    store: LauncherStore,
    actions: DeviceActions,
    onPickAppGridSlot: (AppGridSlot) -> Unit,
    onNavigate: (SettingsDestination) -> Unit,
    goBack: () -> Unit,
) {
    SettingsPage(stringResource(R.string.app_grid), goBack) {
        SectionLabel(stringResource(R.string.home_shortcuts))
        AppGridOrderEditor(
            store = store,
            actions = actions,
            onSlotClick = { slot ->
                if (slot == AppGridSlot.DRAWER) {
                    onNavigate(SettingsDestination.QUICK_DRAWER)
                } else {
                    onPickAppGridSlot(slot)
                }
            },
        )
    }
}
