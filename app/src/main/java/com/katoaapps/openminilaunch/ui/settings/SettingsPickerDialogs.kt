package com.katoaapps.openminilaunch.ui.settings

import android.content.Context
import android.os.Build
import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import com.katoaapps.openminilaunch.R
import com.katoaapps.openminilaunch.data.LauncherStore
import com.katoaapps.openminilaunch.features.messaging.MessagingProviderCatalog
import com.katoaapps.openminilaunch.features.wellbeing.UsageInsightsRepository
import com.katoaapps.openminilaunch.model.MAX_DRAWER_APPS
import com.katoaapps.openminilaunch.platform.DeviceActions
import com.katoaapps.openminilaunch.ui.launcher.displayLabel
import com.katoaapps.openminilaunch.ui.launcher.displaySlotLabel
import com.katoaapps.openminilaunch.ui.wellbeing.SocialAppsDialog

@Composable
internal fun SettingsPickerDialogs(
    picker: SettingsPicker?,
    store: LauncherStore,
    actions: DeviceActions,
    context: Context,
    installedApps: LauncherAppListLoadState,
    curatedAiApps: AppListLoadState,
    allAiApps: AppListLoadState,
    webApps: AppListLoadState,
    messagingProviders: MessagingProviderLoadState,
    usageInsights: UsageInsightsRepository,
    onPickerChange: (SettingsPicker?) -> Unit,
) {
    when (picker) {
        is SettingsPicker.ShortcutApp -> ShortcutAppPicker(
            picker = picker,
            store = store,
            actions = actions,
            installedApps = installedApps,
            onDismiss = { onPickerChange(null) },
        )
        SettingsPicker.DrawerApps -> DrawerAppsPicker(
            store = store,
            actions = actions,
            context = context,
            installedApps = installedApps,
            onDismiss = { onPickerChange(null) },
        )
        SettingsPicker.CuratedAiApp -> AppPickerDialog(
            title = stringResource(R.string.choose_ai_app_title),
            apps = curatedAiApps.apps,
            selected = setOfNotNull(store.preferredAiPackage),
            loading = !curatedAiApps.loaded,
            emptyMessage = stringResource(R.string.no_curated_ai_apps_short),
            extraActionLabel = stringResource(R.string.other_compatible_app),
            onExtraAction = { onPickerChange(SettingsPicker.CompatibleAiApp) },
            onApp = { store.setPreferredAiApp(it.packageName); onPickerChange(null) },
            onReset = { store.resetPreferredAiApp(); onPickerChange(null) },
            resetLabel = stringResource(R.string.reset_to_chooser),
            onDismiss = { onPickerChange(null) },
        )
        SettingsPicker.CompatibleAiApp -> AppPickerDialog(
            title = stringResource(R.string.other_compatible_apps),
            apps = allAiApps.apps,
            selected = setOfNotNull(store.preferredAiPackage),
            loading = !allAiApps.loaded,
            onApp = { store.setPreferredAiApp(it.packageName); onPickerChange(null) },
            onReset = { store.resetPreferredAiApp(); onPickerChange(null) },
            resetLabel = stringResource(R.string.reset_to_chooser),
            onDismiss = { onPickerChange(null) },
        )
        SettingsPicker.WebApp -> AppPickerDialog(
            title = stringResource(R.string.choose_web_search_app),
            apps = webApps.apps,
            selected = setOfNotNull(store.preferredWebPackage),
            loading = !webApps.loaded,
            emptyMessage = stringResource(R.string.no_web_search_apps),
            onApp = { store.setPreferredWebApp(it.packageName); onPickerChange(null) },
            onReset = { store.resetPreferredWebApp(); onPickerChange(null) },
            resetLabel = stringResource(R.string.use_system_browser),
            onDismiss = { onPickerChange(null) },
        )
        SettingsPicker.MessagingApp -> MessagingProviderPickerDialog(
            title = stringResource(R.string.choose_preferred_messaging_app),
            options = messagingProviders.options,
            selectedProviderId = MessagingProviderCatalog.providerForPackage(
                store.preferredMessagingPackage,
            )?.id ?: MessagingProviderCatalog.SYSTEM_DEFAULT_PROVIDER_ID,
            loading = !messagingProviders.loaded,
            showUnavailable = true,
            onProvider = { option ->
                option.preferencePackageName?.let(store::setPreferredMessagingApp)
                    ?: store.resetPreferredMessagingApp()
                onPickerChange(null)
            },
            onDismiss = { onPickerChange(null) },
        )
        SettingsPicker.MinkDayApps -> SocialAppsDialog(store, usageInsights) {
            onPickerChange(null)
        }
        null -> Unit
    }
}

@Composable
private fun ShortcutAppPicker(
    picker: SettingsPicker.ShortcutApp,
    store: LauncherStore,
    actions: DeviceActions,
    installedApps: LauncherAppListLoadState,
    onDismiss: () -> Unit,
) {
    val shortcut = picker.shortcut
    val showSamsungWeatherGuide = Build.MANUFACTURER.equals("samsung", ignoreCase = true)
    LauncherAppPickerDialog(
        title = stringResource(R.string.choose_app_for_shortcut, shortcut.displaySlotLabel()),
        apps = installedApps.apps,
        selected = setOfNotNull(store.shortcutTargets[shortcut]),
        actions = actions,
        loading = !installedApps.loaded,
        supportingText = if (showSamsungWeatherGuide) stringResource(R.string.samsung_weather_guide) else null,
        supportingActionLabel = if (showSamsungWeatherGuide) stringResource(R.string.open_apps_settings) else null,
        onSupportingAction = actions::openInstalledAppsSettings,
        onApp = { store.assignShortcut(shortcut, it.selectionKey); onDismiss() },
        onReset = { store.resetShortcut(shortcut); onDismiss() },
        resetLabel = stringResource(R.string.restore_shortcut_default, shortcut.displayLabel()),
        onDismiss = onDismiss,
    )
}

@Composable
private fun DrawerAppsPicker(
    store: LauncherStore,
    actions: DeviceActions,
    context: Context,
    installedApps: LauncherAppListLoadState,
    onDismiss: () -> Unit,
) {
    LauncherAppPickerDialog(
        title = pluralStringResource(
            R.plurals.drawer_apps_title,
            store.drawerTargets.size,
            store.drawerTargets.size,
            MAX_DRAWER_APPS,
        ),
        apps = installedApps.apps,
        selected = store.drawerTargets.toSet(),
        actions = actions,
        loading = !installedApps.loaded,
        onApp = { app ->
            val fillingDrawer = app.selectionKey !in store.drawerTargets &&
                store.drawerTargets.size == MAX_DRAWER_APPS - 1
            store.toggleDrawerApp(app.selectionKey)
            if (fillingDrawer) {
                Toast.makeText(
                    context,
                    context.resources.getQuantityString(
                        R.plurals.apps_selected,
                        MAX_DRAWER_APPS,
                        MAX_DRAWER_APPS,
                    ),
                    Toast.LENGTH_SHORT,
                ).show()
            }
        },
        onSelectionLimit = {
            Toast.makeText(
                context,
                context.resources.getQuantityString(
                    R.plurals.maximum_apps_selected,
                    MAX_DRAWER_APPS,
                    MAX_DRAWER_APPS,
                ),
                Toast.LENGTH_SHORT,
            ).show()
        },
        onDismiss = onDismiss,
        multiSelect = true,
        selectionLimit = MAX_DRAWER_APPS,
    )
}
