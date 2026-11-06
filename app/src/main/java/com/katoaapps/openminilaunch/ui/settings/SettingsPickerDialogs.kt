package com.katoaapps.openminilaunch.ui.settings

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.katoaapps.openminilaunch.R
import com.katoaapps.openminilaunch.data.LauncherStore
import com.katoaapps.openminilaunch.features.messaging.MessagingProviderCatalog
import com.katoaapps.openminilaunch.features.wellbeing.UsageInsightsRepository
import com.katoaapps.openminilaunch.model.LaunchableApp
import com.katoaapps.openminilaunch.platform.DeviceActions
import com.katoaapps.openminilaunch.ui.appgrid.AppGridSelectionDialog
import com.katoaapps.openminilaunch.ui.quickdrawer.QuickAppsEditorDialog
import com.katoaapps.openminilaunch.ui.wellbeing.SocialAppsDialog

@Composable
internal fun SettingsPickerDialogs(
    picker: SettingsPicker?,
    store: LauncherStore,
    actions: DeviceActions,
    installedApps: LauncherAppListLoadState,
    installedShortcuts: LauncherShortcutListLoadState,
    aiProviders: AiProviderLoadState,
    allAiApps: AppListLoadState,
    webApps: AppListLoadState,
    messagingProviders: MessagingProviderLoadState,
    callingApps: CallingAppLoadState,
    usageInsights: UsageInsightsRepository,
    onPickerChange: (SettingsPicker?) -> Unit,
) {
    when (picker) {
        is SettingsPicker.AppGridSlotPicker -> AppGridSelectionDialog(
            slot = picker.slot,
            initialSelectionKeys = store.appGridSelectionKeys(picker.slot),
            actions = actions,
            apps = installedApps.apps,
            shortcuts = installedShortcuts.shortcuts,
            appsLoading = !installedApps.loaded,
            shortcutsLoading = !installedShortcuts.loaded,
            onSave = { selection ->
                store.saveAppGridSelection(picker.slot, selection)
                actions.syncPinnedSelectionsFrom(store)
            },
            onDismiss = { onPickerChange(null) },
        )
        SettingsPicker.DrawerApps -> QuickAppsEditorDialog(
            store = store,
            actions = actions,
            onDismiss = { onPickerChange(null) },
        )
        SettingsPicker.PrivateContainerGateway -> LauncherTargetPickerDialog(
            title = stringResource(R.string.choose_private_container_gateway),
            apps = installedApps.apps.filterNot { it.isWorkProfile },
            shortcuts = installedShortcuts.shortcuts.filterNot { it.isWorkProfile },
            selected = setOfNotNull(store.privateContainerGatewaySelectionKey),
            actions = actions,
            appsLoading = !installedApps.loaded,
            shortcutsLoading = !installedShortcuts.loaded,
            supportingText = stringResource(R.string.private_container_gateway_picker_description),
            onTarget = { target ->
                store.setPrivateContainerGateway(target.selectionKey)
                actions.syncPinnedSelectionsFrom(store)
                onPickerChange(null)
            },
            onReset = {
                store.clearPrivateContainerGateway()
                actions.syncPinnedSelectionsFrom(store)
                onPickerChange(null)
            },
            resetLabel = stringResource(R.string.remove_private_container_gateway),
            onDismiss = { onPickerChange(null) },
        )
        SettingsPicker.CuratedAiApp -> AiProviderPickerDialog(
            title = stringResource(R.string.choose_ai_app_title),
            options = aiProviders.options,
            selectedPackage = store.preferredAiPackage,
            loading = !aiProviders.loaded,
            showUnavailable = true,
            onProvider = { option ->
                option.installedPackageName?.let(store::setPreferredAiApp)
                onPickerChange(null)
            },
            onInstall = { actions.openAiProviderInstallPage(it) },
            onSeeAllApps = { onPickerChange(SettingsPicker.CompatibleAiApp) },
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
            onInstall = { option ->
                option.preferencePackageName?.let(actions::openMessagingProviderInstallPage)
            },
            onDismiss = { onPickerChange(null) },
        )
        SettingsPicker.CallingApp -> AppPickerDialog(
            title = stringResource(R.string.choose_integrated_calling_app),
            apps = callingApps.options.map { option ->
                LaunchableApp(
                    label = option.label,
                    packageName = option.packageName,
                )
            },
            selected = setOfNotNull(store.preferredCallingPackage),
            loading = !callingApps.loaded,
            emptyMessage = stringResource(R.string.no_compatible_calling_app),
            onApp = { store.setPreferredCallingApp(it.packageName); onPickerChange(null) },
            onReset = { store.resetPreferredCallingApp(); onPickerChange(null) },
            resetLabel = stringResource(R.string.use_system_calling_app),
            onDismiss = { onPickerChange(null) },
        )
        SettingsPicker.MinkDayApps -> SocialAppsDialog(store, usageInsights) {
            onPickerChange(null)
        }
        null -> Unit
    }
}

private fun DeviceActions.syncPinnedSelectionsFrom(store: LauncherStore) {
    syncPinnedLauncherShortcuts(store.pinnedLauncherSelectionKeys)
}
