package com.katoaapps.openminilaunch.ui.settings

import android.content.Context
import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import com.katoaapps.openminilaunch.R
import com.katoaapps.openminilaunch.data.LauncherStore
import com.katoaapps.openminilaunch.features.messaging.MessagingProviderCatalog
import com.katoaapps.openminilaunch.features.wellbeing.UsageInsightsRepository
import com.katoaapps.openminilaunch.model.AppGridAssignment
import com.katoaapps.openminilaunch.model.MAX_DRAWER_APPS
import com.katoaapps.openminilaunch.model.LauncherTarget
import com.katoaapps.openminilaunch.model.LaunchableApp
import com.katoaapps.openminilaunch.platform.DeviceActions
import com.katoaapps.openminilaunch.ui.appgrid.AppGridSelectionDialog
import com.katoaapps.openminilaunch.ui.components.moveItem
import com.katoaapps.openminilaunch.ui.wellbeing.SocialAppsDialog

@Composable
internal fun SettingsPickerDialogs(
    picker: SettingsPicker?,
    store: LauncherStore,
    actions: DeviceActions,
    context: Context,
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
            initialFolderName = (store.appGridAssignment(picker.slot) as? AppGridAssignment.Folder)
                ?.folderId
                ?.let(store::appFolder)
                ?.customName,
            actions = actions,
            apps = installedApps.apps,
            shortcuts = installedShortcuts.shortcuts,
            appsLoading = !installedApps.loaded,
            shortcutsLoading = !installedShortcuts.loaded,
            onSave = { selection, folderName ->
                store.saveAppGridSelection(picker.slot, selection, folderName)
                actions.syncPinnedSelectionsFrom(store)
            },
            onDismiss = { onPickerChange(null) },
        )
        SettingsPicker.DrawerApps -> DrawerAppsPicker(
            store = store,
            actions = actions,
            context = context,
            installedApps = installedApps,
            installedShortcuts = installedShortcuts,
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

@Composable
private fun DrawerAppsPicker(
    store: LauncherStore,
    actions: DeviceActions,
    context: Context,
    installedApps: LauncherAppListLoadState,
    installedShortcuts: LauncherShortcutListLoadState,
    onDismiss: () -> Unit,
) {
    val draftSelection = remember {
        mutableStateListOf<String>().apply { addAll(store.drawerTargets) }
    }
    var pendingReplacement by remember { mutableStateOf<LauncherTarget?>(null) }
    LauncherTargetPickerDialog(
        title = pluralStringResource(
            R.plurals.drawer_apps_title,
            draftSelection.size,
            draftSelection.size,
            MAX_DRAWER_APPS,
        ),
        apps = installedApps.apps,
        shortcuts = installedShortcuts.shortcuts,
        selected = draftSelection.toSet(),
        selectedOrder = draftSelection.toList(),
        actions = actions,
        appsLoading = !installedApps.loaded,
        shortcutsLoading = !installedShortcuts.loaded,
        onTarget = { target ->
            if (target.selectionKey in draftSelection) {
                draftSelection.remove(target.selectionKey)
            } else if (draftSelection.size < MAX_DRAWER_APPS) {
                draftSelection += target.selectionKey
                if (draftSelection.size == MAX_DRAWER_APPS) {
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
            }
        },
        onSelectionLimit = { pendingReplacement = it },
        onMoveSelected = { fromIndex, toIndex ->
            draftSelection.moveItem(fromIndex, toIndex)
        },
        onDone = {
            if (store.setDrawerTargets(draftSelection)) {
                actions.syncPinnedSelectionsFrom(store)
            }
            onDismiss()
        },
        onDismiss = onDismiss,
        multiSelect = true,
        selectionLimit = MAX_DRAWER_APPS,
        showSearch = false,
    )
    pendingReplacement?.let { replacement ->
        DrawerReplacementDialog(
            replacement = replacement,
            currentSelectionKeys = draftSelection.toList(),
            actions = actions,
            onReplace = { index ->
                if (index in draftSelection.indices) {
                    draftSelection[index] = replacement.selectionKey
                }
                pendingReplacement = null
            },
            onDismiss = { pendingReplacement = null },
        )
    }
}

private fun DeviceActions.syncPinnedSelectionsFrom(store: LauncherStore) {
    syncPinnedLauncherShortcuts(store.pinnedLauncherSelectionKeys)
}
