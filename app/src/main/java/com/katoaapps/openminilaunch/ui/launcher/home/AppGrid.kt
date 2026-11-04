@file:OptIn(
    androidx.compose.foundation.ExperimentalFoundationApi::class,
    androidx.compose.foundation.layout.ExperimentalLayoutApi::class,
    androidx.compose.material3.ExperimentalMaterial3Api::class,
)

package com.katoaapps.openminilaunch.ui.launcher.home

import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.katoaapps.openminilaunch.data.LauncherStore
import com.katoaapps.openminilaunch.features.wellbeing.MinkAppAccessState
import com.katoaapps.openminilaunch.model.AppGridAssignment
import com.katoaapps.openminilaunch.model.AppGridSlot
import com.katoaapps.openminilaunch.model.LauncherTarget
import com.katoaapps.openminilaunch.model.configurableAppGridSlots
import com.katoaapps.openminilaunch.platform.DeviceActions
import com.katoaapps.openminilaunch.ui.appgrid.AppFolderContentsDialog
import com.katoaapps.openminilaunch.ui.appgrid.AppFolderIcon
import com.katoaapps.openminilaunch.ui.appgrid.AppGridSelectionDialog
import com.katoaapps.openminilaunch.ui.appgrid.UnavailableLauncherTargetDialog
import com.katoaapps.openminilaunch.ui.appgrid.backgroundColor
import com.katoaapps.openminilaunch.ui.appgrid.defaultIcon
import com.katoaapps.openminilaunch.ui.appgrid.displayLabel
import com.katoaapps.openminilaunch.ui.components.LauncherTargetIcon
import com.katoaapps.openminilaunch.ui.components.minkBuiltInIconTint
import com.katoaapps.openminilaunch.ui.theme.Dimens
import com.katoaapps.openminilaunch.ui.theme.MinkTransparent
import com.katoaapps.openminilaunch.ui.wellbeing.MinkPausedBadge

@Composable
internal fun AppGrid(
    store: LauncherStore,
    actions: DeviceActions,
    appAccessState: MinkAppAccessState,
    onPausedApp: (String) -> Unit,
    onConfigureExtraApp: () -> Unit,
    compact: Boolean = false,
    contentColor: Color = MaterialTheme.colorScheme.onSurface,
    itemContainerColor: Color = MinkTransparent,
    modifier: Modifier = Modifier,
    openDrawer: () -> Unit,
) {
    val launcherAppsRevision by actions.launcherAppsRevision.collectAsState()
    val launcherShortcutsRevision by actions.launcherShortcutsRevision.collectAsState()
    val builtInIconColor = store.iconAppearance.minkBuiltInIconTint(contentColor)
    var openFolderSlot by remember { mutableStateOf<AppGridSlot?>(null) }
    var editingSelectionSlot by remember { mutableStateOf<AppGridSlot?>(null) }
    var pendingUnavailableTarget by remember { mutableStateOf<LauncherTarget?>(null) }
    val gridState = rememberLazyGridState()

    Column(modifier, verticalArrangement = Arrangement.spacedBy(Dimens.dp4)) {
        val visibleOrder = store.effectiveAppGridOrder
        BoxWithConstraints(Modifier.fillMaxWidth().weight(1f)) {
            val slotSize = appGridSlotCellSizeDp(maxWidth.value, maxHeight.value).dp
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                state = gridState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(vertical = Dimens.dp2),
                horizontalArrangement = Arrangement.spacedBy(Dimens.dp4),
                verticalArrangement = Arrangement.SpaceEvenly,
                userScrollEnabled = false,
            ) {
                items(visibleOrder, key = AppGridSlot::name) { slot ->
                    val assignment = store.appGridAssignment(slot)
                    val folder = (assignment as? AppGridAssignment.Folder)?.let {
                        store.appFolder(it.folderId)
                    }
                    val assignedTargetKey = store.appGridTargetKey(slot)
                    val assignedTarget = remember(
                        assignedTargetKey,
                        launcherAppsRevision,
                        launcherShortcutsRevision,
                    ) {
                        assignedTargetKey?.let(actions::resolveLauncherSelection)
                    }
                    val hasAssignedTarget = slot in configurableAppGridSlots && assignedTargetKey != null
                    val hasFolder = folder != null
                    val defaultTargetPackage = actions.appGridSlotTargetPackage(slot, null)
                    val pausedTargetPackage = assignedTarget?.packageName ?: defaultTargetPackage
                    val appUnavailable = assignedTarget?.isAvailable == false
                    val appPaused = !appUnavailable && (
                        assignedTarget?.let(appAccessState::isPaused)
                            ?: (defaultTargetPackage?.let(appAccessState::isPaused) == true)
                        )
                    val interactionModifier = Modifier.combinedClickable(
                        onClick = {
                            if (folder != null) {
                                openFolderSlot = slot
                            } else if (appUnavailable) {
                                pendingUnavailableTarget = assignedTarget
                            } else if (appPaused) {
                                onPausedApp(checkNotNull(pausedTargetPackage))
                            } else if (slot == AppGridSlot.EXTRA && assignedTargetKey == null) {
                                onConfigureExtraApp()
                            } else {
                                actions.launchAppGridSlot(slot, assignedTargetKey, openDrawer)
                            }
                        },
                        onLongClick = {
                            if (!store.demoSearchDataEnabled && slot != AppGridSlot.DRAWER) {
                                editingSelectionSlot = slot
                            }
                        },
                    )
                    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Box(
                            Modifier
                                .size(slotSize)
                                .animateItem()
                                .clip(RoundedCornerShape(if (compact) Dimens.dp14 else Dimens.dp18))
                                .background(if (folder != null) Color.Transparent else itemContainerColor)
                                .then(interactionModifier)
                                .padding(if (hasAssignedTarget || hasFolder) Dimens.dp0 else Dimens.dp6),
                            contentAlignment = Alignment.Center,
                        ) {
                            if (folder != null) {
                                AppFolderIcon(
                                    targetKeys = folder.orderedTargetKeys,
                                    actions = actions,
                                    size = slotSize,
                                    backgroundColor = folder.backgroundColor(
                                        defaultColor = itemContainerColor,
                                        themeColor = MaterialTheme.colorScheme.primaryContainer,
                                    ),
                                    indicatorStyle = store.folderIndicatorStyle,
                                )
                            } else if (hasAssignedTarget) {
                                LauncherTargetIcon(
                                    target = checkNotNull(assignedTarget),
                                    actions = actions,
                                    size = (slotSize * .56f).coerceIn(
                                        if (compact) Dimens.dp28 else Dimens.dp32,
                                        if (compact) Dimens.dp42 else Dimens.dp48,
                                    ),
                                    themedTint = contentColor,
                                    contentDescription = assignedTarget.label,
                                )
                                if (appPaused) {
                                    MinkPausedBadge(Modifier.align(Alignment.Center))
                                }
                            } else {
                                Icon(
                                    slot.defaultIcon(),
                                    slot.displayLabel(),
                                    Modifier.size(if (compact) Dimens.dp24 else Dimens.dp28),
                                    tint = builtInIconColor,
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    openFolderSlot?.let { slot ->
        val assignment = store.appGridAssignment(slot) as? AppGridAssignment.Folder
        val folder = assignment?.let { store.appFolder(it.folderId) }
        if (folder == null) {
            openFolderSlot = null
        } else {
            AppFolderContentsDialog(
                folder = folder,
                animationStyle = store.folderAnimationStyle,
                actions = actions,
                isPaused = appAccessState::isPaused,
                onTarget = { target ->
                    when {
                        !target.isAvailable -> pendingUnavailableTarget = target
                        appAccessState.isPaused(target) -> onPausedApp(target.packageName)
                        else -> actions.launchLauncherTarget(target)
                    }
                    openFolderSlot = null
                },
                onAppearanceChange = { customName, colorMode, colorArgb ->
                    store.updateAppFolderAppearance(
                        folder.id,
                        customName,
                        colorMode,
                        colorArgb,
                    )
                },
                onEdit = {
                    openFolderSlot = null
                    editingSelectionSlot = slot
                },
                onDismiss = { openFolderSlot = null },
            )
        }
    }

    editingSelectionSlot?.let { slot ->
        val editorApps = remember(slot, launcherAppsRevision) { actions.installedApps() }
        val editorShortcuts = remember(slot, launcherShortcutsRevision) { actions.installedShortcuts() }
        AppGridSelectionDialog(
            slot = slot,
            initialSelectionKeys = store.appGridSelectionKeys(slot),
            actions = actions,
            apps = editorApps,
            shortcuts = editorShortcuts,
            onSave = { selection ->
                store.saveAppGridSelection(slot, selection)
                actions.syncPinnedLauncherShortcuts(store.pinnedLauncherSelectionKeys)
            },
            onDismiss = { editingSelectionSlot = null },
        )
    }

    pendingUnavailableTarget?.let { target ->
        UnavailableLauncherTargetDialog(
            target = target,
            actions = actions,
            preferredWebPackage = store.preferredWebPackage,
            onRemove = {
                if (store.pruneLauncherTargets { it == target.selectionKey }) {
                    actions.syncPinnedLauncherShortcuts(store.pinnedLauncherSelectionKeys)
                }
                pendingUnavailableTarget = null
            },
            onDismiss = { pendingUnavailableTarget = null },
        )
    }
}
