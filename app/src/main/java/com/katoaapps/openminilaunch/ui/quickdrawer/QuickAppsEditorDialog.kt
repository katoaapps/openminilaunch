package com.katoaapps.openminilaunch.ui.quickdrawer

import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.katoaapps.openminilaunch.R
import com.katoaapps.openminilaunch.data.LauncherStore
import com.katoaapps.openminilaunch.model.LauncherAppTarget
import com.katoaapps.openminilaunch.model.LauncherShortcutTarget
import com.katoaapps.openminilaunch.model.LauncherTarget
import com.katoaapps.openminilaunch.model.MAX_DRAWER_APPS
import com.katoaapps.openminilaunch.platform.DeviceActions
import com.katoaapps.openminilaunch.ui.components.moveItem
import com.katoaapps.openminilaunch.ui.settings.LauncherTargetPickerDialog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Shared editor used by Quick Drawer and Settings without navigating between them. */
@Composable
internal fun QuickAppsEditorDialog(
    store: LauncherStore,
    actions: DeviceActions,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val launcherAppsRevision by actions.launcherAppsRevision.collectAsState()
    val launcherShortcutsRevision by actions.launcherShortcutsRevision.collectAsState()
    val apps by produceState<List<LauncherAppTarget>?>(null, launcherAppsRevision) {
        value = withContext(Dispatchers.IO) { actions.installedApps() }
    }
    val shortcuts by produceState<List<LauncherShortcutTarget>?>(null, launcherShortcutsRevision) {
        value = withContext(Dispatchers.IO) { actions.installedShortcuts() }
    }
    val draftSelection = remember {
        mutableStateListOf<String>().apply { addAll(store.drawerTargets) }
    }
    var pendingReplacement by remember { mutableStateOf<LauncherTarget?>(null) }

    LauncherTargetPickerDialog(
        title = stringResource(R.string.quick_apps),
        apps = apps.orEmpty(),
        shortcuts = shortcuts.orEmpty(),
        selected = draftSelection.toSet(),
        selectedOrder = draftSelection.toList(),
        actions = actions,
        appsLoading = apps == null,
        shortcutsLoading = shortcuts == null,
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
        onMoveSelected = draftSelection::moveItem,
        onDone = {
            if (store.setDrawerTargets(draftSelection)) {
                actions.syncPinnedLauncherShortcuts(store.pinnedLauncherSelectionKeys)
            }
            onDismiss()
        },
        onDismiss = onDismiss,
        multiSelect = true,
        selectionLimit = MAX_DRAWER_APPS,
        showSearch = false,
    )
    pendingReplacement?.let { replacement ->
        QuickAppReplacementDialog(
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
