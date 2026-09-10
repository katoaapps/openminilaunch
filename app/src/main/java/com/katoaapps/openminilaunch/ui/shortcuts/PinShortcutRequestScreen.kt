@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.katoaapps.openminilaunch.ui.shortcuts

import android.graphics.drawable.Drawable
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Work
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.core.graphics.drawable.toBitmap
import com.katoaapps.openminilaunch.R
import com.katoaapps.openminilaunch.data.LauncherStore
import com.katoaapps.openminilaunch.features.apps.PendingPinShortcut
import com.katoaapps.openminilaunch.model.MAX_DRAWER_APPS
import com.katoaapps.openminilaunch.model.PinShortcutDestination
import com.katoaapps.openminilaunch.model.PinShortcutRequestPresentation
import com.katoaapps.openminilaunch.model.configurableShortcuts
import com.katoaapps.openminilaunch.platform.DeviceActions
import com.katoaapps.openminilaunch.ui.launcher.displayLabel
import com.katoaapps.openminilaunch.ui.launcher.displaySlotLabel
import com.katoaapps.openminilaunch.ui.theme.Dimens
import com.katoaapps.openminilaunch.ui.theme.Muted
import com.katoaapps.openminilaunch.ui.theme.Rust

@Composable
internal fun PinShortcutRequestHost(
    pending: PendingPinShortcut?,
    presentation: PinShortcutRequestPresentation,
    store: LauncherStore,
    actions: DeviceActions,
    onConfirm: (PinShortcutDestination) -> Boolean,
    onCancel: () -> Unit,
) {
    BackHandler(onBack = onCancel)
    if (pending == null) {
        InvalidShortcutRequestPage(onCancel)
        return
    }
    var selectedKey by rememberSaveable { mutableStateOf<String?>(null) }
    var showError by rememberSaveable { mutableStateOf(false) }
    val selected = PinShortcutDestination.fromKey(selectedKey)
    val confirm = {
        if (selected != null && !onConfirm(selected)) showError = true
    }

    when (presentation) {
        PinShortcutRequestPresentation.FULL_PAGE -> PinShortcutFullPage(
            pending = pending,
            store = store,
            actions = actions,
            selectedKey = selectedKey,
            showError = showError,
            onSelect = {
                selectedKey = it
                showError = false
            },
            onConfirm = confirm,
            onCancel = onCancel,
        )
        PinShortcutRequestPresentation.CONFIRMATION_SHEET -> PinShortcutConfirmationSheet(
            pending = pending,
            store = store,
            actions = actions,
            selectedKey = selectedKey,
            showError = showError,
            onSelect = {
                selectedKey = it
                showError = false
            },
            onConfirm = confirm,
            onCancel = onCancel,
        )
    }
}

@Composable
private fun PinShortcutFullPage(
    pending: PendingPinShortcut,
    store: LauncherStore,
    actions: DeviceActions,
    selectedKey: String?,
    showError: Boolean,
    onSelect: (String) -> Unit,
    onConfirm: () -> Unit,
    onCancel: () -> Unit,
) {
    Scaffold(
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            Row(
                Modifier.fillMaxWidth().statusBarsPadding()
                    .padding(horizontal = Dimens.dp10, vertical = Dimens.dp8),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onCancel) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.cancel))
                }
                Text(
                    stringResource(R.string.add_shortcut),
                    Modifier.weight(1f),
                    fontSize = Dimens.sp26,
                    fontWeight = FontWeight.Black,
                )
            }
        },
        bottomBar = {
            ConfirmationBar(
                selected = PinShortcutDestination.fromKey(selectedKey),
                showError = showError,
                onConfirm = onConfirm,
                modifier = Modifier.navigationBarsPadding(),
            )
        },
    ) { insets ->
        PinShortcutPlacementList(
            pending = pending,
            store = store,
            actions = actions,
            selectedKey = selectedKey,
            onSelect = onSelect,
            contentPadding = PaddingValues(
                start = Dimens.dp22,
                end = Dimens.dp22,
                top = insets.calculateTopPadding() + Dimens.dp8,
                bottom = insets.calculateBottomPadding() + Dimens.dp16,
            ),
        )
    }
}

@Composable
private fun PinShortcutConfirmationSheet(
    pending: PendingPinShortcut,
    store: LauncherStore,
    actions: DeviceActions,
    selectedKey: String?,
    showError: Boolean,
    onSelect: (String) -> Unit,
    onConfirm: () -> Unit,
    onCancel: () -> Unit,
) {
    Box(
        Modifier.fillMaxSize().background(MaterialTheme.colorScheme.scrim.copy(alpha = .5f)),
    ) {
        ModalBottomSheet(
            onDismissRequest = onCancel,
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = MaterialTheme.colorScheme.surface,
        ) {
            Column(Modifier.fillMaxHeight(.9f).navigationBarsPadding()) {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = Dimens.dp18),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        stringResource(R.string.add_shortcut),
                        Modifier.weight(1f),
                        fontSize = Dimens.sp24,
                        fontWeight = FontWeight.Black,
                    )
                    IconButton(onClick = onCancel) {
                        Icon(Icons.Default.Close, stringResource(R.string.cancel))
                    }
                }
                PinShortcutPlacementList(
                    pending = pending,
                    store = store,
                    actions = actions,
                    selectedKey = selectedKey,
                    onSelect = onSelect,
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = Dimens.dp18, vertical = Dimens.dp8),
                )
                ConfirmationBar(
                    selected = PinShortcutDestination.fromKey(selectedKey),
                    showError = showError,
                    onConfirm = onConfirm,
                )
            }
        }
    }
}

@Composable
private fun PinShortcutPlacementList(
    pending: PendingPinShortcut,
    store: LauncherStore,
    actions: DeviceActions,
    selectedKey: String?,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues,
) {
    val homeDestinations = configurableShortcuts.map(PinShortcutDestination::HomeSlot)
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = contentPadding,
        verticalArrangement = Arrangement.spacedBy(Dimens.dp10),
    ) {
        item {
            RequestedShortcutCard(pending)
            Spacer(Modifier.height(Dimens.dp8))
            Text(
                stringResource(R.string.choose_shortcut_destination),
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                stringResource(R.string.choose_shortcut_destination_description),
                color = Muted,
                fontSize = Dimens.sp12,
            )
        }
        item(PinShortcutDestination.AddToLibrary.key) {
            DestinationRow(
                title = stringResource(R.string.add_to_search_and_all_apps),
                subtitle = stringResource(R.string.add_to_search_and_all_apps_description),
                selected = selectedKey == PinShortcutDestination.AddToLibrary.key,
                onClick = { onSelect(PinShortcutDestination.AddToLibrary.key) },
                leading = { Icon(Icons.Default.Search, null) },
            )
        }
        item { DestinationSectionLabel(stringResource(R.string.home_shortcuts)) }
        items(homeDestinations, key = PinShortcutDestination::key) { destination ->
            val currentKey = store.shortcutTargets[destination.shortcut]
            DestinationRow(
                title = destination.shortcut.displaySlotLabel(),
                subtitle = currentKey?.let(actions::launcherTargetLabel)
                    ?: stringResource(
                        R.string.shortcut_default,
                        destination.shortcut.displayLabel(),
                    ),
                selected = selectedKey == destination.key,
                onClick = { onSelect(destination.key) },
            )
        }
        item { DestinationSectionLabel(stringResource(R.string.shortcut_top_eight)) }
        if (store.drawerTargets.size < MAX_DRAWER_APPS) {
            item(PinShortcutDestination.AddToDrawer.key) {
                DestinationRow(
                    title = stringResource(R.string.add_to_top_eight),
                    subtitle = stringResource(
                        R.string.count_of_max,
                        store.drawerTargets.size,
                        MAX_DRAWER_APPS,
                    ),
                    selected = selectedKey == PinShortcutDestination.AddToDrawer.key,
                    onClick = { onSelect(PinShortcutDestination.AddToDrawer.key) },
                    leading = { Icon(Icons.Default.Add, null) },
                )
            }
        }
        items(store.drawerTargets.size, key = { "drawer-slot-$it" }) { index ->
            val destination = PinShortcutDestination.ReplaceDrawerSlot(index)
            DestinationRow(
                title = stringResource(R.string.replace_top_eight_item, index + 1),
                subtitle = actions.launcherTargetLabel(store.drawerTargets[index]),
                selected = selectedKey == destination.key,
                onClick = { onSelect(destination.key) },
                leading = { Icon(Icons.Default.GridView, null) },
            )
        }
    }
}

@Composable
private fun RequestedShortcutCard(pending: PendingPinShortcut) {
    Surface(
        shape = RoundedCornerShape(Dimens.dp18),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Row(
            Modifier.fillMaxWidth().padding(Dimens.dp16),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ShortcutRequestIcon(pending.icon, pending.target.label)
            Column(Modifier.weight(1f).padding(start = Dimens.dp14)) {
                Text(
                    pending.target.label,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(pending.publisherLabel, color = Muted, fontSize = Dimens.sp12)
                if (pending.target.isWorkProfile) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Work,
                            null,
                            Modifier.size(Dimens.dp14),
                            tint = Rust,
                        )
                        Text(
                            stringResource(R.string.work_profile),
                            Modifier.padding(start = Dimens.dp4),
                            color = Rust,
                            fontSize = Dimens.sp11,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ShortcutRequestIcon(drawable: Drawable?, label: String) {
    val bitmap = remember(drawable) {
        drawable?.toBitmap(width = 144, height = 144)?.asImageBitmap()
    }
    Surface(
        modifier = Modifier.size(Dimens.dp58),
        shape = RoundedCornerShape(Dimens.dp16),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
    ) {
        if (bitmap != null) {
            Image(
                bitmap,
                label,
                Modifier.fillMaxSize().padding(Dimens.dp4),
                contentScale = ContentScale.Fit,
            )
        } else {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Icon(Icons.Default.GridView, label, Modifier.size(Dimens.dp30))
            }
        }
    }
}

@Composable
private fun DestinationSectionLabel(label: String) {
    Text(
        label,
        Modifier.padding(top = Dimens.dp8),
        color = Rust,
        fontSize = Dimens.sp12,
        fontWeight = FontWeight.Black,
    )
}

@Composable
private fun DestinationRow(
    title: String,
    subtitle: String,
    selected: Boolean,
    onClick: () -> Unit,
    leading: (@Composable () -> Unit)? = null,
) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(Dimens.dp16))
            .background(
                if (selected) MaterialTheme.colorScheme.primaryContainer
                else MaterialTheme.colorScheme.surfaceContainerLow,
            )
            .clickable(onClick = onClick)
            .padding(horizontal = Dimens.dp12, vertical = Dimens.dp10),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (leading != null) {
            Box(Modifier.size(Dimens.dp34), contentAlignment = Alignment.Center) { leading() }
        }
        Column(Modifier.weight(1f).padding(horizontal = Dimens.dp8)) {
            Text(title, fontWeight = FontWeight.SemiBold)
            Text(
                subtitle,
                color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = .72f) else Muted,
                fontSize = Dimens.sp12,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        RadioButton(selected = selected, onClick = onClick)
    }
}

@Composable
private fun ConfirmationBar(
    selected: PinShortcutDestination?,
    showError: Boolean,
    onConfirm: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(modifier = modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.surface) {
        Column(Modifier.fillMaxWidth().padding(horizontal = Dimens.dp18, vertical = Dimens.dp12)) {
            if (showError) {
                Text(
                    stringResource(R.string.shortcut_request_expired),
                    color = MaterialTheme.colorScheme.error,
                    fontSize = Dimens.sp12,
                )
                Spacer(Modifier.height(Dimens.dp6))
            }
            Button(
                onClick = onConfirm,
                enabled = selected != null,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.add_shortcut))
            }
        }
    }
}

@Composable
private fun InvalidShortcutRequestPage(onClose: () -> Unit) {
    Column(
        Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)
            .statusBarsPadding().navigationBarsPadding().padding(Dimens.dp22),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            stringResource(R.string.shortcut_request_unavailable),
            fontSize = Dimens.sp22,
            fontWeight = FontWeight.Bold,
        )
        Spacer(Modifier.height(Dimens.dp8))
        Text(
            stringResource(R.string.shortcut_request_expired),
            color = Muted,
        )
        Spacer(Modifier.height(Dimens.dp18))
        Button(onClick = onClose) { Text(stringResource(R.string.close)) }
    }
}
