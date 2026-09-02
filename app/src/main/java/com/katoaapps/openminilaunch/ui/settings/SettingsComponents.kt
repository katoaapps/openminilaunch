@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class, androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package com.katoaapps.openminilaunch.ui.settings

import com.katoaapps.openminilaunch.R
import com.katoaapps.openminilaunch.data.*
import com.katoaapps.openminilaunch.model.*
import com.katoaapps.openminilaunch.platform.*
import com.katoaapps.openminilaunch.features.files.*
import com.katoaapps.openminilaunch.features.wellbeing.*
import com.katoaapps.openminilaunch.ui.components.*
import com.katoaapps.openminilaunch.ui.theme.*

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items as lazyRowItems
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.window.Dialog

@Composable
internal fun AppPickerDialog(
    title: String,
    apps: List<LaunchableApp>,
    selected: Set<String>,
    appIcon: @Composable (LaunchableApp, Dp) -> Unit = { app, size ->
        AppIcon(app.packageName, actions = null, size = size)
    },
    onApp: (LaunchableApp) -> Unit,
    onReset: (() -> Unit)? = null,
    resetLabel: String? = null,
    onDismiss: () -> Unit,
    multiSelect: Boolean = false,
    selectionLimit: Int = 5,
    loading: Boolean = true,
    emptyMessage: String? = null,
    onSelectionLimit: () -> Unit = {},
    extraActionLabel: String? = null,
    onExtraAction: () -> Unit = {},
    supportingText: String? = null,
    supportingActionLabel: String? = null,
    onSupportingAction: () -> Unit = {},
) {
    AppPickerDialogContent(
        title = title,
        apps = apps,
        selected = selected,
        appKey = LaunchableApp::packageName,
        appLabel = LaunchableApp::label,
        appIcon = appIcon,
        onApp = onApp,
        onReset = onReset,
        resetLabel = resetLabel,
        onDismiss = onDismiss,
        multiSelect = multiSelect,
        selectionLimit = selectionLimit,
        loading = loading,
        emptyMessage = emptyMessage,
        onSelectionLimit = { onSelectionLimit() },
        extraActionLabel = extraActionLabel,
        onExtraAction = onExtraAction,
        supportingText = supportingText,
        supportingActionLabel = supportingActionLabel,
        onSupportingAction = onSupportingAction,
    )
}

@Composable
private fun <T> AppPickerDialogContent(
    title: String,
    apps: List<T>,
    selected: Set<String>,
    appKey: (T) -> String,
    appLabel: (T) -> String,
    appIcon: @Composable (T, Dp) -> Unit,
    onApp: (T) -> Unit,
    onReset: (() -> Unit)? = null,
    resetLabel: String? = null,
    onDismiss: () -> Unit,
    multiSelect: Boolean = false,
    selectionLimit: Int = 5,
    loading: Boolean = true,
    emptyMessage: String? = null,
    onSelectionLimit: (T) -> Unit = {},
    extraActionLabel: String? = null,
    onExtraAction: () -> Unit = {},
    supportingText: String? = null,
    supportingActionLabel: String? = null,
    onSupportingAction: () -> Unit = {},
    selectedItems: List<T> = apps,
    categoryContent: (@Composable () -> Unit)? = null,
    customListContent: (@Composable () -> Unit)? = null,
) {
    val gridState = rememberLazyGridState()
    var requestedLetter by remember { mutableStateOf('A') }
    LaunchedEffect(requestedLetter, apps) {
        if (apps.isNotEmpty()) {
            val index = apps.indexOfFirst {
                (appLabel(it).firstOrNull()?.uppercaseChar() ?: 'Z') >= requestedLetter
            }
                .let { if (it < 0) apps.lastIndex else it }
            if (index >= 0) gridState.scrollToItem(index)
        }
    }
    Dialog(
        onDismissRequest = onDismiss,
        properties = MinkDialogDefaults.properties,
    ) {
        Surface(
            Modifier.minkDialogWidth().fillMaxHeight(.96f),
            shape = RoundedCornerShape(Dimens.dp24),
            color = MaterialTheme.colorScheme.background,
        ) {
            Column(Modifier.padding(Dimens.dp14)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(title, Modifier.weight(1f), fontWeight = FontWeight.Black, fontSize = Dimens.sp18)
                    IconButton(onClick = onDismiss) { Icon(Icons.Default.Close, stringResource(R.string.close)) }
                }
                categoryContent?.invoke()
                supportingText?.let { guide ->
                    Surface(
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        shape = RoundedCornerShape(Dimens.dp14),
                        modifier = Modifier.fillMaxWidth().padding(bottom = Dimens.dp6),
                    ) {
                        Column(Modifier.padding(horizontal = Dimens.dp12, vertical = Dimens.dp10)) {
                            Row(verticalAlignment = Alignment.Top) {
                                Icon(Icons.Default.Info, null, Modifier.size(Dimens.dp18), tint = MaterialTheme.colorScheme.onSecondaryContainer)
                                Text(
                                    guide,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                                    fontSize = Dimens.sp12,
                                    modifier = Modifier.padding(start = Dimens.dp8).weight(1f),
                                )
                            }
                            supportingActionLabel?.let { label ->
                                TextButton(
                                    onClick = onSupportingAction,
                                    modifier = Modifier.align(Alignment.End),
                                    contentPadding = PaddingValues(horizontal = Dimens.dp6),
                                ) { Text(label) }
                            }
                        }
                    }
                }
                onReset?.let {
                    TextButton(onClick = it, contentPadding = PaddingValues(horizontal = Dimens.dp4)) {
                        Icon(Icons.Default.Restore, null, Modifier.size(Dimens.dp18))
                        Text(resetLabel ?: stringResource(R.string.reset_system_default), Modifier.padding(start = Dimens.dp6))
                    }
                }
                extraActionLabel?.let { label ->
                    TextButton(onClick = onExtraAction, contentPadding = PaddingValues(horizontal = Dimens.dp4)) {
                        Icon(Icons.Default.MoreHoriz, null, Modifier.size(Dimens.dp18))
                        Text(label, Modifier.padding(start = Dimens.dp6))
                    }
                }
                if (multiSelect && selected.isNotEmpty()) {
                    Text(
                        stringResource(R.string.selected_tap_to_remove),
                        color = Muted,
                        fontSize = Dimens.sp10,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = Dimens.sp1,
                        modifier = Modifier.padding(top = Dimens.dp4, bottom = Dimens.dp6),
                    )
                    val selectedApps = selectedItems.filter { appKey(it) in selected }.take(selectionLimit)
                    LazyRow(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(Dimens.dp6),
                    ) {
                        lazyRowItems(selectedApps, key = appKey) { app ->
                            Column(
                                Modifier.width(Dimens.dp64).clip(RoundedCornerShape(Dimens.dp12))
                                    .background(MaterialTheme.colorScheme.surfaceContainerLow)
                                    .clickable { onApp(app) }.padding(horizontal = Dimens.dp3, vertical = Dimens.dp6),
                                horizontalAlignment = Alignment.CenterHorizontally,
                            ) {
                                Box {
                                    appIcon(app, Dimens.dp30)
                                    Surface(
                                        modifier = Modifier.align(Alignment.TopEnd).offset(x = Dimens.dp5, y = -Dimens.dp5),
                                        shape = CircleShape,
                                        color = MaterialTheme.colorScheme.error,
                                    ) {
                                        Icon(Icons.Default.Close, stringResource(R.string.remove_app, appLabel(app)), Modifier.size(Dimens.dp14), tint = MaterialTheme.colorScheme.onError)
                                    }
                                }
                                Text(appLabel(app), fontSize = Dimens.sp9, maxLines = 1, textAlign = TextAlign.Center, modifier = Modifier.padding(top = Dimens.dp3))
                            }
                        }
                    }
                    HorizontalDivider(Modifier.padding(top = Dimens.dp8), color = Sage)
                }
                Box(Modifier.weight(1f).fillMaxWidth()) {
                    if (apps.isEmpty() && loading) {
                        CircularProgressIndicator(Modifier.align(Alignment.Center), color = MaterialTheme.colorScheme.onSurface)
                    } else if (apps.isEmpty()) {
                        Text(emptyMessage ?: stringResource(R.string.no_apps_found), Modifier.align(Alignment.Center).padding(Dimens.dp24), textAlign = TextAlign.Center, color = Muted)
                    } else if (customListContent != null) {
                        customListContent()
                    } else {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(3),
                            state = gridState,
                            contentPadding = PaddingValues(top = Dimens.dp8, bottom = Dimens.dp8, end = Dimens.dp34),
                            modifier = Modifier.fillMaxSize(),
                        ) {
                            items(apps, key = appKey) { app ->
                                val isSelected = appKey(app) in selected
                                Column(
                                    Modifier.padding(Dimens.dp4).clip(RoundedCornerShape(Dimens.dp16))
                                        .background(if (isSelected) Sage else MaterialTheme.colorScheme.surfaceContainerLow)
                                        .clickable {
                                            if (multiSelect && !isSelected && selected.size >= selectionLimit) {
                                                onSelectionLimit(app)
                                            } else {
                                                onApp(app)
                                            }
                                        }
                                        .padding(Dimens.dp8),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                ) {
                                    appIcon(app, Dimens.dp48)
                                    Text(appLabel(app), textAlign = TextAlign.Center, fontSize = Dimens.sp11, maxLines = 2, modifier = Modifier.padding(top = Dimens.dp6))
                                }
                            }
                        }
                        AlphabetRail(
                            onLetterSelected = { requestedLetter = it },
                            modifier = Modifier.align(Alignment.CenterEnd),
                        )
                    }
                }
            }
        }
    }
}

@Composable
internal fun LauncherTargetPickerDialog(
    title: String,
    apps: List<LauncherAppTarget>,
    shortcuts: List<LauncherShortcutTarget>,
    selected: Set<String>,
    actions: DeviceActions,
    onTarget: (LauncherTarget) -> Unit,
    onReset: (() -> Unit)? = null,
    resetLabel: String? = null,
    onDismiss: () -> Unit,
    multiSelect: Boolean = false,
    selectionLimit: Int = 5,
    appsLoading: Boolean = true,
    shortcutsLoading: Boolean = true,
    onSelectionLimit: (LauncherTarget) -> Unit = {},
    supportingText: String? = null,
    supportingActionLabel: String? = null,
    onSupportingAction: () -> Unit = {},
) {
    var selectedCategory by rememberSaveable { mutableStateOf(LauncherTargetCategory.APPS) }
    val allTargets = remember(apps, shortcuts, selected) {
        (apps + shortcuts + selected.map(actions::resolveLauncherSelection))
            .distinctBy(LauncherTarget::selectionKey)
            .sortedBy { it.label.lowercase() }
    }
    val visibleTargets = remember(allTargets, selectedCategory) {
        allTargets.filter { target ->
            when (selectedCategory) {
                LauncherTargetCategory.APPS -> target is LauncherAppTarget
                LauncherTargetCategory.SHORTCUTS -> target is LauncherShortcutTarget
            }
        }
    }
    AppPickerDialogContent(
        title = title,
        apps = visibleTargets,
        selected = selected,
        appKey = LauncherTarget::selectionKey,
        appLabel = LauncherTarget::label,
        appIcon = { target, size -> LauncherTargetIcon(target, actions, size) },
        onApp = onTarget,
        onReset = onReset,
        resetLabel = resetLabel,
        onDismiss = onDismiss,
        multiSelect = multiSelect,
        selectionLimit = selectionLimit,
        loading = when (selectedCategory) {
            LauncherTargetCategory.APPS -> appsLoading
            LauncherTargetCategory.SHORTCUTS -> shortcutsLoading
        },
        emptyMessage = when (selectedCategory) {
            LauncherTargetCategory.APPS -> stringResource(R.string.no_apps_found)
            LauncherTargetCategory.SHORTCUTS -> stringResource(R.string.no_app_shortcuts_found)
        },
        onSelectionLimit = onSelectionLimit,
        supportingText = supportingText,
        supportingActionLabel = supportingActionLabel,
        onSupportingAction = onSupportingAction,
        selectedItems = allTargets,
        categoryContent = {
            Row(
                Modifier.fillMaxWidth().padding(bottom = Dimens.dp8),
                horizontalArrangement = Arrangement.spacedBy(Dimens.dp8),
            ) {
                LauncherTargetCategory.entries.forEach { category ->
                    FilterChip(
                        selected = selectedCategory == category,
                        onClick = { selectedCategory = category },
                        label = {
                            Text(
                                stringResource(
                                    if (category == LauncherTargetCategory.APPS) R.string.apps
                                    else R.string.app_shortcuts,
                                ),
                            )
                        },
                        leadingIcon = {
                            Icon(
                                if (category == LauncherTargetCategory.APPS) Icons.Default.Apps
                                else Icons.Default.Bolt,
                                null,
                                Modifier.size(Dimens.dp18),
                            )
                        },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        },
        customListContent = if (selectedCategory == LauncherTargetCategory.SHORTCUTS) {
            {
                LauncherShortcutGroupList(
                    shortcuts = visibleTargets.filterIsInstance<LauncherShortcutTarget>(),
                    apps = apps,
                    selected = selected,
                    actions = actions,
                    multiSelect = multiSelect,
                    selectionLimit = selectionLimit,
                    onSelectionLimit = onSelectionLimit,
                    onShortcut = onTarget,
                )
            }
        } else {
            null
        },
    )
}

private enum class LauncherTargetCategory { APPS, SHORTCUTS }

@Composable
internal fun ThemeChooser(selected: ThemePreference, onSelect: (ThemePreference) -> Unit) {
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(Dimens.dp16)).background(MaterialTheme.colorScheme.surfaceContainerLow).padding(Dimens.dp14),
        verticalArrangement = Arrangement.spacedBy(Dimens.dp10),
    ) {
        Text(stringResource(R.string.light_mode), fontWeight = FontWeight.SemiBold)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Dimens.dp7)) {
            ThemePreference.entries.forEach { option ->
                FilterChip(
                    selected = selected == option,
                    onClick = { onSelect(option) },
                    label = { Text(stringResource(option.labelRes)) },
                    leadingIcon = if (selected == option) ({ Icon(Icons.Default.Check, null, Modifier.size(Dimens.dp15)) }) else null,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}
