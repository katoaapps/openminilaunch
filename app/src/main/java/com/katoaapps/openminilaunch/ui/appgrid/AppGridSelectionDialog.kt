package com.katoaapps.openminilaunch.ui.appgrid

import android.os.Build
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.katoaapps.openminilaunch.R
import com.katoaapps.openminilaunch.model.AppGridSlot
import com.katoaapps.openminilaunch.model.LauncherAppTarget
import com.katoaapps.openminilaunch.model.LauncherShortcutTarget
import com.katoaapps.openminilaunch.model.MAX_APP_FOLDER_NAME_LENGTH
import com.katoaapps.openminilaunch.platform.DeviceActions
import com.katoaapps.openminilaunch.ui.components.moveItem
import com.katoaapps.openminilaunch.ui.settings.LauncherTargetPickerDialog

/** Shared transactional editor for a direct App Grid target or adaptive folder contents. */
@Composable
internal fun AppGridSelectionDialog(
    slot: AppGridSlot,
    initialSelectionKeys: List<String>,
    initialFolderName: String?,
    actions: DeviceActions,
    apps: List<LauncherAppTarget>,
    shortcuts: List<LauncherShortcutTarget>,
    appsLoading: Boolean = false,
    shortcutsLoading: Boolean = false,
    onSave: (selectedTargetKeys: List<String>, folderName: String?) -> Unit,
    onDismiss: () -> Unit,
) {
    val draftSelection = remember(slot) {
        mutableStateListOf<String>().apply { addAll(initialSelectionKeys) }
    }
    var draftFolderName by remember(slot) { mutableStateOf(initialFolderName.orEmpty()) }
    val showSamsungWeatherGuide = Build.MANUFACTURER.equals("samsung", ignoreCase = true)
    LauncherTargetPickerDialog(
        title = stringResource(R.string.home_shortcut_and_folder),
        apps = apps,
        shortcuts = shortcuts,
        selected = draftSelection.toSet(),
        selectedOrder = draftSelection.toList(),
        actions = actions,
        appsLoading = appsLoading,
        shortcutsLoading = shortcutsLoading,
        supportingText = if (showSamsungWeatherGuide) {
            stringResource(R.string.samsung_weather_guide)
        } else {
            null
        },
        supportingActionLabel = if (showSamsungWeatherGuide) {
            stringResource(R.string.open_apps_settings)
        } else {
            null
        },
        onSupportingAction = actions::openInstalledAppsSettings,
        onTarget = { target ->
            if (target.selectionKey in draftSelection) draftSelection.remove(target.selectionKey)
            else draftSelection += target.selectionKey
        },
        onMoveSelected = { fromIndex, toIndex ->
            draftSelection.moveItem(fromIndex, toIndex)
        },
        configurationContent = {
            if (draftSelection.size > 1) {
                OutlinedTextField(
                    value = draftFolderName,
                    onValueChange = { draftFolderName = it.take(MAX_APP_FOLDER_NAME_LENGTH) },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.folder_name)) },
                    placeholder = { Text(stringResource(R.string.apps)) },
                    singleLine = true,
                )
            }
        },
        onDone = {
            onSave(draftSelection.toList(), draftFolderName)
            onDismiss()
        },
        onDismiss = onDismiss,
        multiSelect = true,
        selectionLimit = Int.MAX_VALUE,
        showSearch = false,
    )
}
