@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class, androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package com.katoaapps.openminilaunch

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog

@Composable
internal fun AppPickerDialog(
    title: String,
    apps: List<LaunchableApp>,
    selected: Set<String>,
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
    val gridState = rememberLazyGridState()
    val letters = remember { ('A'..'Z').toList() }
    var railHeight by remember { mutableIntStateOf(1) }
    var railLetterIndex by remember { mutableIntStateOf(0) }
    var railDragging by remember { mutableStateOf(false) }
    LaunchedEffect(railLetterIndex, apps) {
        if (apps.isNotEmpty()) {
            val letter = letters[railLetterIndex]
            val index = apps.indexOfFirst { (it.label.firstOrNull()?.uppercaseChar() ?: 'Z') >= letter }
                .let { if (it < 0) apps.lastIndex else it }
            if (index >= 0) gridState.scrollToItem(index)
        }
    }
    Dialog(onDismissRequest = onDismiss) {
        Surface(Modifier.fillMaxWidth().fillMaxHeight(.78f), shape = RoundedCornerShape(Dimens.dp24), color = MaterialTheme.colorScheme.background) {
            Column(Modifier.padding(Dimens.dp16)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(title, Modifier.weight(1f), fontWeight = FontWeight.Black, fontSize = Dimens.sp18)
                    IconButton(onClick = onDismiss) { Icon(Icons.Default.Close, stringResource(R.string.close)) }
                }
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
                    val selectedApps = apps.filter { it.packageName in selected }.take(selectionLimit)
                    selectedApps.chunked(4).forEach { rowApps ->
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Dimens.dp6)) {
                            rowApps.forEach { app ->
                                Column(
                                    Modifier.weight(1f).clip(RoundedCornerShape(Dimens.dp12))
                                        .background(MaterialTheme.colorScheme.surfaceContainerLow)
                                        .clickable { onApp(app) }.padding(horizontal = Dimens.dp3, vertical = Dimens.dp7),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                ) {
                                    Box {
                                        AppIcon(app.packageName, actions = null, size = Dimens.dp31)
                                        Surface(
                                            modifier = Modifier.align(Alignment.TopEnd).offset(x = Dimens.dp5, y = -Dimens.dp5),
                                            shape = CircleShape,
                                            color = MaterialTheme.colorScheme.error,
                                        ) {
                                            Icon(Icons.Default.Close, stringResource(R.string.remove_app, app.label), Modifier.size(Dimens.dp14), tint = MaterialTheme.colorScheme.onError)
                                        }
                                    }
                                    Text(app.label, fontSize = Dimens.sp9, maxLines = 1, textAlign = TextAlign.Center, modifier = Modifier.padding(top = Dimens.dp4))
                                }
                            }
                            repeat(4 - rowApps.size) { Spacer(Modifier.weight(1f)) }
                        }
                        Spacer(Modifier.height(Dimens.dp6))
                    }
                    HorizontalDivider(Modifier.padding(top = Dimens.dp10), color = Sage)
                }
                Box(Modifier.weight(1f).fillMaxWidth()) {
                    if (apps.isEmpty() && loading) {
                        CircularProgressIndicator(Modifier.align(Alignment.Center), color = MaterialTheme.colorScheme.onSurface)
                    } else if (apps.isEmpty()) {
                        Text(emptyMessage ?: stringResource(R.string.no_apps_found), Modifier.align(Alignment.Center).padding(Dimens.dp24), textAlign = TextAlign.Center, color = Muted)
                    } else {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(3),
                            state = gridState,
                            contentPadding = PaddingValues(top = Dimens.dp8, bottom = Dimens.dp8, end = Dimens.dp34),
                            modifier = Modifier.fillMaxSize(),
                        ) {
                            items(apps, key = { it.packageName }) { app ->
                                val isSelected = app.packageName in selected
                                Column(
                                    Modifier.padding(Dimens.dp5).clip(RoundedCornerShape(Dimens.dp16))
                                        .background(if (isSelected) Sage else MaterialTheme.colorScheme.surfaceContainerLow)
                                        .clickable {
                                            if (multiSelect && !isSelected && selected.size >= selectionLimit) onSelectionLimit()
                                            else onApp(app)
                                        }
                                        .padding(Dimens.dp10),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                ) {
                                    AppIcon(app.packageName, actions = null, size = Dimens.dp42)
                                    Text(app.label, textAlign = TextAlign.Center, fontSize = Dimens.sp11, maxLines = 2, modifier = Modifier.padding(top = Dimens.dp7))
                                }
                            }
                        }
                        Column(
                            Modifier.align(Alignment.CenterEnd).fillMaxHeight().width(Dimens.dp28)
                                .onSizeChanged { railHeight = it.height }
                                .clip(RoundedCornerShape(Dimens.dp12)).background(MaterialTheme.colorScheme.background.copy(alpha = .94f))
                                .pointerInput(apps, railHeight) {
                                    fun selectAt(y: Float) {
                                        railLetterIndex = ((y / railHeight) * letters.size).toInt().coerceIn(0, letters.lastIndex)
                                    }
                                    detectVerticalDragGestures(
                                        onDragStart = { railDragging = true; selectAt(it.y) },
                                        onVerticalDrag = { change, _ -> selectAt(change.position.y) },
                                        onDragEnd = { railDragging = false },
                                        onDragCancel = { railDragging = false },
                                    )
                                },
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.SpaceEvenly,
                        ) {
                            letters.forEach { letter ->
                                val isFocused = letters.indexOf(letter) == railLetterIndex
                                val scale by animateFloatAsState(
                                    targetValue = if (isFocused) 1.7f else .9f,
                                    animationSpec = spring(dampingRatio = .7f, stiffness = 500f),
                                    label = "rail-letter-scale",
                                )
                                Text(
                                    letter.toString(),
                                    fontSize = Dimens.sp9,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isFocused) MaterialTheme.colorScheme.onSurface else Rust.copy(alpha = .72f),
                                    modifier = Modifier.clickable {
                                        railLetterIndex = letters.indexOf(letter)
                                    }.graphicsLayer { scaleX = scale; scaleY = scale }
                                        .padding(horizontal = Dimens.dp6),
                                )
                            }
                        }
                        if (railDragging) {
                            Surface(
                                modifier = Modifier.align(Alignment.CenterEnd).padding(end = Dimens.dp38),
                                shape = CircleShape,
                                color = Rust,
                                shadowElevation = Dimens.dp8,
                            ) {
                                Box(Modifier.size(Dimens.dp48), contentAlignment = Alignment.Center) {
                                    Text(letters[railLetterIndex].toString(), color = MinkWhite, fontSize = Dimens.sp22, fontWeight = FontWeight.Black)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

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

@Composable
internal fun MessageSendModeChooser(selected: MessageSendMode, onSelect: (MessageSendMode) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(Dimens.dp8)) {
        MessageSendMode.entries.forEach { option ->
            Surface(
                onClick = { onSelect(option) },
                shape = RoundedCornerShape(Dimens.dp14),
                color = if (selected == option) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerLow,
            ) {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = Dimens.dp12, vertical = Dimens.dp10),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    RadioButton(selected = selected == option, onClick = null)
                    Text(stringResource(option.labelRes), Modifier.padding(start = Dimens.dp8), fontWeight = FontWeight.Medium)
                }
            }
        }
    }
}

@Composable
internal fun PermissionCard(
    title: String,
    description: String,
    granted: Boolean,
    icon: ImageVector,
    onGrant: () -> Unit,
    onManage: () -> Unit,
) {
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(Dimens.dp16)).background(MaterialTheme.colorScheme.surfaceContainerLow).padding(Dimens.dp14),
        verticalArrangement = Arrangement.spacedBy(Dimens.dp9),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = MaterialTheme.colorScheme.onSurface)
            Column(Modifier.weight(1f).padding(start = Dimens.dp10)) {
                Text(title, fontWeight = FontWeight.SemiBold)
                Text(description, color = Muted, fontSize = Dimens.sp12)
            }
            if (granted) Icon(Icons.Default.CheckCircle, stringResource(R.string.granted), tint = MagicCallColor)
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            TextButton(onClick = onManage) { Text(stringResource(R.string.manage)) }
            if (!granted) FilledTonalButton(onClick = onGrant) { Text(stringResource(R.string.allow)) }
        }
    }
}

@Composable
internal fun LockAccessibilityDisclosureDialog(onContinue: () -> Unit, onDismiss: () -> Unit) {
    val appName = stringResource(R.string.app_name)
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Default.Lock, null, tint = Rust) },
        title = { Text(stringResource(R.string.enable_double_tap_lock)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(Dimens.dp10)) {
                Text(stringResource(R.string.double_tap_disclosure_one, appName))
                Text(stringResource(R.string.double_tap_disclosure_two))
                Text(stringResource(R.string.double_tap_disclosure_three))
            }
        },
        confirmButton = { Button(onClick = onContinue) { Text(stringResource(R.string.continue_action)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.not_now)) } },
    )
}

@Composable
internal fun AssistantDisclosureDialog(active: Boolean, onContinue: () -> Unit, onDismiss: () -> Unit) {
    val appName = stringResource(R.string.app_name)
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Default.Assistant, null, tint = Rust) },
        title = { Text(stringResource(if (active) R.string.assistant_active_title else R.string.assistant_enable_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(Dimens.dp10)) {
                Text(stringResource(R.string.assistant_disclosure_one))
                Text(stringResource(R.string.assistant_disclosure_two, appName))
                Text(stringResource(R.string.assistant_disclosure_three, appName))
            }
        },
        confirmButton = {
            Button(onClick = onContinue) { Text(stringResource(if (active) R.string.manage_assistant else R.string.choose_assistant)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
}

@Composable
internal fun NotificationAccessDisclosureDialog(onContinue: () -> Unit, onDismiss: () -> Unit) {
    val appName = stringResource(R.string.app_name)
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Default.Forum, null, tint = Rust) },
        title = { Text(stringResource(R.string.enable_conversations)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(Dimens.dp10)) {
                Text(stringResource(R.string.conversations_disclosure_one, appName))
                Text(stringResource(R.string.conversations_disclosure_two, appName))
                Text(stringResource(R.string.conversations_disclosure_three))
            }
        },
        confirmButton = { Button(onClick = onContinue) { Text(stringResource(R.string.continue_action)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.not_now)) } },
    )
}
