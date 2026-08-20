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
    resetLabel: String = "Reset to system default",
    onDismiss: () -> Unit,
    multiSelect: Boolean = false,
    selectionLimit: Int = 5,
    loading: Boolean = true,
    emptyMessage: String = "No apps found.",
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
        Surface(Modifier.fillMaxWidth().fillMaxHeight(.78f), shape = RoundedCornerShape(24.dp), color = MaterialTheme.colorScheme.background) {
            Column(Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(title, Modifier.weight(1f), fontWeight = FontWeight.Black, fontSize = 18.sp)
                    IconButton(onClick = onDismiss) { Icon(Icons.Default.Close, "Close") }
                }
                supportingText?.let { guide ->
                    Surface(
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp),
                    ) {
                        Column(Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
                            Row(verticalAlignment = Alignment.Top) {
                                Icon(Icons.Default.Info, null, Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onSecondaryContainer)
                                Text(
                                    guide,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                                    fontSize = 12.sp,
                                    modifier = Modifier.padding(start = 8.dp).weight(1f),
                                )
                            }
                            supportingActionLabel?.let { label ->
                                TextButton(
                                    onClick = onSupportingAction,
                                    modifier = Modifier.align(Alignment.End),
                                    contentPadding = PaddingValues(horizontal = 6.dp),
                                ) { Text(label) }
                            }
                        }
                    }
                }
                onReset?.let {
                    TextButton(onClick = it, contentPadding = PaddingValues(horizontal = 4.dp)) {
                        Icon(Icons.Default.Restore, null, Modifier.size(18.dp))
                        Text(resetLabel, Modifier.padding(start = 6.dp))
                    }
                }
                extraActionLabel?.let { label ->
                    TextButton(onClick = onExtraAction, contentPadding = PaddingValues(horizontal = 4.dp)) {
                        Icon(Icons.Default.MoreHoriz, null, Modifier.size(18.dp))
                        Text(label, Modifier.padding(start = 6.dp))
                    }
                }
                if (multiSelect && selected.isNotEmpty()) {
                    Text(
                        "SELECTED · TAP TO REMOVE",
                        color = Muted,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        modifier = Modifier.padding(top = 4.dp, bottom = 6.dp),
                    )
                    val selectedApps = apps.filter { it.packageName in selected }.take(selectionLimit)
                    selectedApps.chunked(4).forEach { rowApps ->
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            rowApps.forEach { app ->
                                Column(
                                    Modifier.weight(1f).clip(RoundedCornerShape(12.dp))
                                        .background(MaterialTheme.colorScheme.surfaceContainerLow)
                                        .clickable { onApp(app) }.padding(horizontal = 3.dp, vertical = 7.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                ) {
                                    Box {
                                        AppIcon(app.packageName, actions = null, size = 31.dp)
                                        Surface(
                                            modifier = Modifier.align(Alignment.TopEnd).offset(x = 5.dp, y = (-5).dp),
                                            shape = CircleShape,
                                            color = MaterialTheme.colorScheme.error,
                                        ) {
                                            Icon(Icons.Default.Close, "Remove ${app.label}", Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onError)
                                        }
                                    }
                                    Text(app.label, fontSize = 9.sp, maxLines = 1, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 4.dp))
                                }
                            }
                            repeat(4 - rowApps.size) { Spacer(Modifier.weight(1f)) }
                        }
                        Spacer(Modifier.height(6.dp))
                    }
                    HorizontalDivider(Modifier.padding(top = 10.dp), color = Sage)
                }
                Box(Modifier.weight(1f).fillMaxWidth()) {
                    if (apps.isEmpty() && loading) {
                        CircularProgressIndicator(Modifier.align(Alignment.Center), color = MaterialTheme.colorScheme.onSurface)
                    } else if (apps.isEmpty()) {
                        Text(emptyMessage, Modifier.align(Alignment.Center).padding(24.dp), textAlign = TextAlign.Center, color = Muted)
                    } else {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(3),
                            state = gridState,
                            contentPadding = PaddingValues(top = 8.dp, bottom = 8.dp, end = 34.dp),
                            modifier = Modifier.fillMaxSize(),
                        ) {
                            items(apps, key = { it.packageName }) { app ->
                                val isSelected = app.packageName in selected
                                Column(
                                    Modifier.padding(5.dp).clip(RoundedCornerShape(16.dp))
                                        .background(if (isSelected) Sage else MaterialTheme.colorScheme.surfaceContainerLow)
                                        .clickable {
                                            if (multiSelect && !isSelected && selected.size >= selectionLimit) onSelectionLimit()
                                            else onApp(app)
                                        }
                                        .padding(10.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                ) {
                                    AppIcon(app.packageName, actions = null, size = 42.dp)
                                    Text(app.label, textAlign = TextAlign.Center, fontSize = 11.sp, maxLines = 2, modifier = Modifier.padding(top = 7.dp))
                                }
                            }
                        }
                        Column(
                            Modifier.align(Alignment.CenterEnd).fillMaxHeight().width(28.dp)
                                .onSizeChanged { railHeight = it.height }
                                .clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.background.copy(alpha = .94f))
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
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isFocused) MaterialTheme.colorScheme.onSurface else Rust.copy(alpha = .72f),
                                    modifier = Modifier.clickable {
                                        railLetterIndex = letters.indexOf(letter)
                                    }.graphicsLayer { scaleX = scale; scaleY = scale }
                                        .padding(horizontal = 6.dp),
                                )
                            }
                        }
                        if (railDragging) {
                            Surface(
                                modifier = Modifier.align(Alignment.CenterEnd).padding(end = 38.dp),
                                shape = CircleShape,
                                color = Rust,
                                shadowElevation = 8.dp,
                            ) {
                                Box(Modifier.size(48.dp), contentAlignment = Alignment.Center) {
                                    Text(letters[railLetterIndex].toString(), color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Black)
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
        Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(MaterialTheme.colorScheme.surfaceContainerLow).padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text("Light Mode", fontWeight = FontWeight.SemiBold)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
            ThemePreference.entries.forEach { option ->
                FilterChip(
                    selected = selected == option,
                    onClick = { onSelect(option) },
                    label = { Text(option.label) },
                    leadingIcon = if (selected == option) ({ Icon(Icons.Default.Check, null, Modifier.size(15.dp)) }) else null,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
internal fun MessageSendModeChooser(selected: MessageSendMode, onSelect: (MessageSendMode) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        MessageSendMode.entries.forEach { option ->
            Surface(
                onClick = { onSelect(option) },
                shape = RoundedCornerShape(14.dp),
                color = if (selected == option) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerLow,
            ) {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    RadioButton(selected = selected == option, onClick = null)
                    Text(option.label, Modifier.padding(start = 8.dp), fontWeight = FontWeight.Medium)
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
        Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(MaterialTheme.colorScheme.surfaceContainerLow).padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(9.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = MaterialTheme.colorScheme.onSurface)
            Column(Modifier.weight(1f).padding(start = 10.dp)) {
                Text(title, fontWeight = FontWeight.SemiBold)
                Text(description, color = Muted, fontSize = 12.sp)
            }
            if (granted) Icon(Icons.Default.CheckCircle, "Granted", tint = Color(0xFF198754))
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            TextButton(onClick = onManage) { Text("Manage") }
            if (!granted) FilledTonalButton(onClick = onGrant) { Text("Allow") }
        }
    }
}

@Composable
internal fun LockAccessibilityDisclosureDialog(onContinue: () -> Unit, onDismiss: () -> Unit) {
    val appName = stringResource(R.string.app_name)
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Default.Lock, null, tint = Rust) },
        title = { Text("Enable double-tap to lock?") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("To make double-tap behave like the power button, $appName uses Android's accessibility Lock screen action.")
                Text("The service runs only when you double-tap empty Home space. It does not observe accessibility events, read screen content, perform gestures, or collect data.")
                Text("Android will ask you to enable Double-tap screen lock. You can disable it at any time in Accessibility settings.")
            }
        },
        confirmButton = { Button(onClick = onContinue) { Text("Continue") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Not now") } },
    )
}

@Composable
internal fun AssistantDisclosureDialog(active: Boolean, onContinue: () -> Unit, onDismiss: () -> Unit) {
    val appName = stringResource(R.string.app_name)
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Default.Assistant, null, tint = Rust) },
        title = { Text(if (active) "Mink Assistant is active" else "Use Mink Assistant?") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Your phone's assistant gesture will open the keyboard-first Magic Box over your current app.")
                Text("Choosing $appName replaces your current default digital assistant. You can switch back at any time in system settings.")
                Text("Mink Assistant does not request microphone, call-log, screen-reading, or screen-context access. Android may grant Send SMS as part of the assistant role; $appName uses it only after you choose a recipient, write the message, and press the @ action.")
            }
        },
        confirmButton = {
            Button(onClick = onContinue) { Text(if (active) "Manage assistant" else "Choose assistant") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
internal fun NotificationAccessDisclosureDialog(onContinue: () -> Unit, onDismiss: () -> Unit) {
    val appName = stringResource(R.string.app_name)
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Default.Forum, null, tint = Rust) },
        title = { Text("Enable Conversations?") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Android notification access lets $appName read active message and email notifications, group them into conversations, and offer inline reply when the originating app provides a reply action.")
                Text("Notifications outside messages and email are ignored. Conversation contents and replies are kept in memory only. They are not stored by $appName, uploaded, or sent to Katoa Apps.")
                Text("Replies are handed directly to the app that created the notification. You can revoke access at any time in Android settings.")
            }
        },
        confirmButton = { Button(onClick = onContinue) { Text("Continue") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Not now") } },
    )
}
