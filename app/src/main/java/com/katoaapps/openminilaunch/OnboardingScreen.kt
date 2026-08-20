@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class, androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package com.katoaapps.openminilaunch

import android.Manifest
import android.app.Activity
import android.app.role.RoleManager
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.net.Uri
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Message
import androidx.compose.material.icons.automirrored.filled.NoteAdd
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.zIndex
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import androidx.core.app.ActivityCompat
import androidx.core.graphics.drawable.toBitmap
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlin.math.ceil
import kotlin.math.roundToInt

@Composable
internal fun FeatureUpdateDialog(
    onOpenSettings: () -> Unit,
    onReviewTutorial: () -> Unit,
    onNotNow: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onNotNow,
        icon = { Icon(Icons.Default.Pets, null, tint = Rust) },
        title = { Text("What’s new in Open 1.0") },
        text = {
            Column(
                Modifier.heightIn(max = 560.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Text("More focused by design", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                UpdatePoint(Icons.Default.Pets, "Social apps only", "Mink’s Day now measures only the social apps you want to limit. Other apps never enter the trail or totals.")
                UpdatePoint(Icons.Default.Tune, "Defaults you can see", "Android’s Social category is preselected until you create a custom list. Your current apps stay pinned at the top of the picker.")
                UpdatePoint(Icons.Default.Email, "Email conversations", "Conversations now includes active email threads when the originating app marks them with Android’s email category.")
                UpdatePoint(Icons.Default.Folder, "Folders you choose", "Document search now works only inside folders you explicitly select. All files access has been removed.")
                UpdatePoint(Icons.Default.Security, "Still private", "Usage and conversation analysis stays on your device. No new permission is required for this update.")
                TextButton(onClick = onReviewTutorial, contentPadding = PaddingValues(0.dp)) {
                    Text("Review the updated tutorial")
                }
            }
        },
        confirmButton = { Button(onClick = onOpenSettings) { Text("Open settings") } },
        dismissButton = { TextButton(onClick = onNotNow) { Text("Not now") } },
    )
}

@Composable
internal fun FileSearchScopeDialog(
    onChooseFolder: () -> Unit,
    onSkip: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onSkip,
        icon = { Icon(Icons.Default.FolderOpen, null, tint = Rust) },
        title = { Text("Where should MinkLauncher OpenSource search?") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text("Choose a folder for document search. You can add or remove folders later in Settings.")
                Surface(
                    onClick = onChooseFolder,
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                ) {
                    Box(Modifier.padding(14.dp)) {
                        UpdatePoint(
                            Icons.Default.Folder,
                            "Choose a folder",
                            "MinkLauncher OpenSource searches only folders you explicitly approve through Android.",
                        )
                    }
                }
                Text(
                    "Search queries and filenames stay on your device and are never sent to us.",
                    color = Muted,
                    fontSize = 12.sp,
                )
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onSkip) { Text("Skip for now") } },
    )
}

@Composable
internal fun UpdatePoint(icon: ImageVector, title: String, description: String) {
    Row(verticalAlignment = Alignment.Top) {
        Icon(icon, null, Modifier.size(22.dp), tint = Rust)
        Column(Modifier.padding(start = 10.dp)) {
            Text(title, fontWeight = FontWeight.SemiBold)
            Text(description, color = Muted, fontSize = 13.sp)
        }
    }
}

@Composable
internal fun UsageAccessDisclosureDialog(onContinue: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Default.Pets, null, tint = Rust) },
        title = { Text("Let Mink reflect your day?") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Android’s Usage Access lets MinkLauncher OpenSource measure when your tracked social apps are in the foreground and for how long.")
                Text("MinkLauncher OpenSource uses this only to calculate today’s Mink state and insights on your device. It does not upload your activity, sell it, or keep a separate usage-history database.")
                Text("This is optional. Mink’s page still works as a time-of-day companion if you skip it, and you can revoke access whenever you want.")
            }
        },
        confirmButton = { Button(onClick = onContinue) { Text("Continue") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Not now") } },
    )
}

@Composable
internal fun OnboardingDialog(store: LauncherStore, actions: DeviceActions, onFinish: () -> Unit) {
    var page by rememberSaveable { mutableIntStateOf(0) }
    var pickingAi by remember { mutableStateOf(false) }
    var pickingAllAi by remember { mutableStateOf(false) }
    var aiAppsLoaded by remember { mutableStateOf(false) }
    val curatedAiApps by produceState<List<LaunchableApp>>(initialValue = emptyList()) {
        value = withContext(Dispatchers.IO) { actions.curatedAiApps() }
        aiAppsLoaded = true
    }
    var allAiAppsLoaded by remember { mutableStateOf(false) }
    var assistantRoleHeld by remember { mutableStateOf(actions.isAssistantRoleHeld()) }
    val assistantRoleSettings = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        assistantRoleHeld = actions.isAssistantRoleHeld()
    }
    val allAiApps by produceState<List<LaunchableApp>>(initialValue = emptyList()) {
        value = withContext(Dispatchers.IO) { actions.textShareApps() }
        allAiAppsLoaded = true
    }
    val titles = listOf(
        "A quieter home screen",
        "Meet the Magic Box",
        "To-dos, kept close",
        "Search locally or beyond",
        "Magic Box, from anywhere",
        "Meet Mink’s Day",
        "Conversations and widgets",
        "Permissions, on your terms",
    )
    val icons = listOf(
        Icons.Default.Keyboard,
        Icons.Default.AutoAwesome,
        Icons.Default.Checklist,
        Icons.Default.ManageSearch,
        Icons.Default.Assistant,
        Icons.Default.Pets,
        Icons.Default.Widgets,
        Icons.Default.Security,
    )
    Dialog(
        onDismissRequest = {},
        properties = DialogProperties(usePlatformDefaultWidth = false, dismissOnBackPress = false, dismissOnClickOutside = false),
    ) {
        Surface(
            Modifier.fillMaxWidth(.92f).fillMaxHeight(.82f),
            shape = RoundedCornerShape(30.dp),
            color = MaterialTheme.colorScheme.background,
        ) {
            Column(Modifier.fillMaxSize().padding(26.dp)) {
                Icon(icons[page], null, Modifier.size(46.dp), tint = Rust)
                Text(titles[page], fontSize = 28.sp, fontWeight = FontWeight.Black, modifier = Modifier.padding(top = 16.dp))
                Spacer(Modifier.height(18.dp))
                Box(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState())) {
                    when (page) {
                        0 -> Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
                            Text("MinkLauncher OpenSource is a minimal keyboard launcher designed around fast, keyboard-based input.", fontSize = 18.sp)
                            OnboardingPoint(Icons.Default.FilterAlt, "Less visual noise", "One focused home page, eight shortcuts, and only eight drawer apps.")
                            OnboardingPoint(Icons.Default.Palette, "Choose your panel color", "Pick from five presets in Settings, or swipe through a custom color and enter its hex value.")
                            OnboardingPoint(Icons.Default.Keyboard, "Just start typing", "On a physical-keyboard phone, press any text key from home. The Magic Box appears with that first character already entered.")
                            OnboardingPoint(Icons.Default.Search, "Everything is still reachable", "Use ? to find any installed app.")
                        }
                        1 -> Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("The Magic Box is ready from the moment home opens. No tap is required on a physical keyboard.", color = Muted, fontSize = 12.sp)
                            listOf(
                                "@" to "Text a contact",
                                "#" to "Call a contact",
                                "-" to "Create a to-do",
                                "$" to "Send text to a notes app",
                                "+" to "Create an event, e.g. next Friday or in 4 weeks",
                                "?" to "Find and launch an app",
                            ).forEach { (key, description) -> MagicKeyRow(key, description) }
                            Text("Message behavior defaults to Always ask: send carrier SMS now or choose a compatible messaging app for the final send. Calls offer the same choice between calling now and choosing a calling app. Direct SMS requires Mink Assistant; Android may grant Send SMS access as part of that role.", color = Muted, fontSize = 12.sp)
                        }
                        2 -> Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
                            Text("Type - followed by your task to send it straight to the home widget.", fontSize = 18.sp)
                            OnboardingPoint(Icons.Default.Swipe, "Swipe the widget", "Each page shows three items at a time.")
                            OnboardingPoint(Icons.Default.TouchApp, "Tap the widget", "Open the full list to check, edit, delete, add, or rearrange items.")
                            OnboardingPoint(Icons.Default.IosShare, "Take the list with you", "Export the current checklist to a notes app or save it as a PDF.")
                            OnboardingPoint(Icons.Default.CheckCircle, "Keep the context", "New Magic Box tasks animate into the newest to-do page.")
                        }
                        3 -> Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
                            Text("Type normally in the Magic Box to find local files, search the web, or hand the query to an AI app you choose.", fontSize = 18.sp)
                            OnboardingPoint(Icons.Default.Language, "Links stay links", "Type a web address to open it directly; other plain text remains a web or AI query.")
                            OnboardingPoint(Icons.Default.History, "Recent activity", "Your last five successful searches and ? app launches stay on this device. Reuse one, delete one, or clear them all from the empty Magic Box.")
                            OnboardingPoint(Icons.Default.AutoAwesome, "AI is optional", "MinkLauncher OpenSource shares your query only after you tap AI. The selected app then handles it under its own privacy terms.")
                            OutlinedButton(onClick = { pickingAi = true }, modifier = Modifier.fillMaxWidth()) {
                                Icon(Icons.Default.AutoAwesome, null)
                                Text(
                                    store.preferredAiPackage?.let { "AI app: ${actions.appLabel(it)}" } ?: "Choose an AI app",
                                    Modifier.padding(start = 8.dp),
                                )
                            }
                            Text("Optional. Skip this and MinkLauncher OpenSource will ask the first time you tap AI.", color = Muted, fontSize = 12.sp)
                            OnboardingPoint(Icons.Default.PhotoLibrary, "Media filenames", "Optional access finds photos, videos, and audio through Android’s media index.")
                            OnboardingPoint(Icons.Default.FolderOpen, "Choose your scope", "Select specific folders, or optionally grant full-device file access for broader filename search.")
                            OnboardingPoint(Icons.Default.Security, "Never sent to us", "MinkLauncher OpenSource does not receive your queries, filenames, or selected folder locations.")
                        }
                        4 -> Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
                            Text("MinkLauncher OpenSource is also a keyboard-first digital assistant, putting the Magic Box behind your phone’s assistant gesture.", fontSize = 18.sp)
                            OnboardingPoint(Icons.Default.Keyboard, "Keyboard first", "Invoke it over any app and start typing immediately. The same hotkeys, local file search, Web, and AI handoffs remain available.")
                            OnboardingPoint(Icons.Default.TouchApp, "A deliberate choice", "This replaces Gemini, Bixby, or your current default assistant until you change it back in Android settings.")
                            OnboardingPoint(Icons.Default.PrivacyTip, "No screen inspection", "Mink Assistant ignores assist context and does not request microphone, call-log, screen-reading, or screen-context access.")
                            OnboardingPoint(Icons.Default.Sms, "Direct SMS", "Android may grant Send SMS access with the Assistant role. MinkLauncher OpenSource uses it only after you choose a contact, write the message, and press the @ action.")
                            Button(
                                onClick = { assistantRoleSettings.launch(actions.assistantRoleSelectionIntent()) },
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Icon(Icons.Default.Assistant, null)
                                Text(
                                    if (assistantRoleHeld) "Manage Mink Assistant" else "Choose Mink Assistant",
                                    Modifier.padding(start = 8.dp),
                                )
                            }
                            Text(
                                if (assistantRoleHeld) "Mink Assistant is active. Direct SMS can be enabled during the permission setup that follows."
                                else "You can skip this, but direct SMS stays unavailable until Mink Assistant is active.",
                                color = Muted,
                                fontSize = 13.sp,
                            )
                            Text("Choose message behavior", fontWeight = FontWeight.Bold)
                            MessageSendModeChooser(store.messageSendMode, store::updateMessageSendMode)
                        }
                        5 -> Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
                            Text("Swipe right from Home to visit Mink’s Day: a private view of time spent in the social apps you want to limit.", fontSize = 18.sp)
                            OnboardingPoint(Icons.Default.Pets, "Six gentle states", "Mink walks, moves steadily, gets distracted, checks a phone, rests, or goes to sleep based on today’s local activity and the time of day.")
                            OnboardingPoint(Icons.Default.Tune, "Your tracked apps and goal", "Use Android’s Social category or choose the apps you want to limit, then set a daily tracked-time goal.")
                            OnboardingPoint(Icons.Default.Security, "Calculated on your device", "Other apps are excluded from the trail. MinkLauncher OpenSource never sends tracked activity, app choices, or insights to Katoa Apps.")
                            OnboardingPoint(Icons.Default.VisibilityOff, "Completely optional", "Without Usage Access, Mink’s page remains available as a quiet time-of-day companion.")
                        }
                        6 -> Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
                            Text("The main page stays focused while optional spaces remain close by.", fontSize = 18.sp)
                            OnboardingPoint(Icons.Default.Forum, "Conversations", "Tap the chat icon beside Settings to read active message and email threads. Inline reply appears only when the originating app supplies Android's reply action.")
                            OnboardingPoint(Icons.Default.Widgets, "Widget page", "Swipe left from Home to add up to four Android widgets. Swipe right or use Back to return Home.")
                            OnboardingPoint(Icons.Default.Pets, "Mink’s Day", "Swipe right from Home for local daily insights. Swipe left or use Back to return Home.")
                            OnboardingPoint(Icons.Default.DragIndicator, "Arrange your grid", "Long-press any Home shortcut to enter edit mode, then drag shortcuts into your preferred order.")
                            OnboardingPoint(Icons.Default.PrivacyTip, "Active and local", "MinkLauncher OpenSource does not keep conversation history. Message text and replies remain in memory and are never sent to Katoa Apps.")
                        }
                        else -> Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
                            Text("MinkLauncher OpenSource asks only for access tied to features you use.", fontSize = 18.sp)
                            Row(
                                Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp))
                                    .background(MaterialTheme.colorScheme.surfaceContainerLow).padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Icon(Icons.Default.Contacts, null, tint = Rust)
                                Column(Modifier.padding(start = 12.dp)) {
                                    Text("Contacts", fontWeight = FontWeight.Bold)
                                    Text("Only read when you search for a person.", color = Muted, fontSize = 13.sp)
                                }
                            }
                            OnboardingPoint(Icons.Default.Phone, "Calls", "Call access is used only when you choose Call now. Choose calling app hands the number to an Android-compatible dialer instead.")
                            OnboardingPoint(Icons.Default.Sms, "Direct SMS", "Send SMS access is used only after you choose a contact, write a message, and press the @ action. Always ask adds a separate confirmation; Always send treats that action as approval.")
                            OnboardingPoint(Icons.Default.Lock, "Double-tap lock", "Optional accessibility access performs only Android's Lock screen action after your double-tap. It cannot read screen content or observe what you do.")
                            OnboardingPoint(Icons.Default.PhotoLibrary, "Media", "Optional access searches photo, video, and audio filenames locally.")
                            OnboardingPoint(Icons.Default.Forum, "Conversation access", "Optional notification access powers message and email conversations. Other notification types are ignored, and you can revoke access whenever you want.")
                            OnboardingPoint(Icons.Default.Pets, "Mink’s Day usage", "Optional Usage Access measures foreground time and opens for your tracked social apps only, entirely on this device.")
                            Text("For documents, Android lets you choose specific folders. MinkLauncher OpenSource cannot search outside folders you approve.", color = Muted, fontSize = 13.sp)
                            Text("Android will show Contacts and Call prompts next. If Mink Assistant is active and your messaging choice includes direct SMS, Android may grant or request Send SMS access before the optional Conversations and Mink’s Day explanations. Media access appears only when you enable file search.", color = Muted)
                        }
                    }
                }
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        repeat(titles.size) { index ->
                            Box(Modifier.size(if (index == page) 22.dp else 8.dp, 8.dp).clip(CircleShape).background(if (index == page) Rust else Sage))
                        }
                    }
                    if (page > 0) TextButton(onClick = { page-- }) { Text("Back") }
                    Button(
                        onClick = { if (page < titles.lastIndex) page++ else onFinish() },
                    ) {
                        Text(if (page < titles.lastIndex) "Next" else "Finish setup")
                    }
                }
            }
        }
    }
    if (pickingAi) {
        AppPickerDialog(
            title = "Choose AI app",
            apps = curatedAiApps,
            selected = setOfNotNull(store.preferredAiPackage),
            loading = !aiAppsLoaded,
            emptyMessage = "No curated AI apps were found.",
            extraActionLabel = "Other compatible app",
            onExtraAction = { pickingAi = false; pickingAllAi = true },
            onApp = { store.setPreferredAiApp(it.packageName); pickingAi = false },
            onReset = { store.resetPreferredAiApp(); pickingAi = false },
            resetLabel = "Choose on first use",
            onDismiss = { pickingAi = false },
        )
    }
    if (pickingAllAi) {
        AppPickerDialog(
            title = "Other compatible apps",
            apps = allAiApps,
            selected = setOfNotNull(store.preferredAiPackage),
            loading = !allAiAppsLoaded,
            onApp = { store.setPreferredAiApp(it.packageName); pickingAllAi = false },
            onDismiss = { pickingAllAi = false },
        )
    }
}

@Composable
internal fun ShortcutSetupDialog(store: LauncherStore, actions: DeviceActions, onFinish: () -> Unit) {
    val context = LocalContext.current
    var pickingShortcut by remember { mutableStateOf<Shortcut?>(null) }
    var installedAppsLoaded by remember { mutableStateOf(false) }
    var appListRefresh by remember { mutableIntStateOf(0) }
    val installedApps by produceState<List<LaunchableApp>>(initialValue = emptyList(), appListRefresh) {
        value = withContext(Dispatchers.IO) { actions.installedApps() }
        installedAppsLoaded = true
    }
    DisposableEffect(context) {
        val lifecycle = (context as? ComponentActivity)?.lifecycle
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                actions.invalidateInstalledApps()
                appListRefresh++
            }
        }
        lifecycle?.addObserver(observer)
        onDispose { lifecycle?.removeObserver(observer) }
    }

    Dialog(
        onDismissRequest = {},
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = false,
            dismissOnClickOutside = false,
        ),
    ) {
        Surface(
            Modifier.fillMaxWidth(.92f).fillMaxHeight(.82f),
            shape = RoundedCornerShape(30.dp),
            color = MaterialTheme.colorScheme.background,
        ) {
            Column(Modifier.fillMaxSize().padding(26.dp)) {
                Icon(Icons.Default.Apps, null, Modifier.size(46.dp), tint = Rust)
                Text(
                    "Choose your shortcut apps",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Black,
                    modifier = Modifier.padding(top = 16.dp),
                )
                Text(
                    "Setup is almost done. Choose what each external shortcut opens, or explicitly use Android's system default.",
                    color = Muted,
                    modifier = Modifier.padding(top = 10.dp, bottom = 18.dp),
                )
                Column(
                    Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    configurableShortcuts.forEach { shortcut ->
                        SettingsRow(
                            title = shortcut.label,
                            subtitle = when {
                                shortcut in store.shortcutPackages -> actions.appLabel(store.shortcutPackages.getValue(shortcut))
                                shortcut in store.confirmedShortcutChoices -> "System default"
                                else -> "Choose an app"
                            },
                            icon = shortcut.setupIcon(),
                        ) { pickingShortcut = shortcut }
                    }
                    if (!store.hasConfirmedAllShortcutChoices()) {
                        OutlinedButton(
                            onClick = store::confirmSystemDefaultsForUnselectedShortcuts,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Icon(Icons.Default.Restore, null)
                            Text("Use system defaults for remaining", Modifier.padding(start = 8.dp))
                        }
                    }
                }
                Button(
                    onClick = onFinish,
                    enabled = store.hasConfirmedAllShortcutChoices(),
                    modifier = Modifier.fillMaxWidth().padding(top = 18.dp),
                ) {
                    Text("Finish launcher setup")
                }
            }
        }
    }

    pickingShortcut?.let { shortcut ->
        val showSamsungWeatherGuide = shortcut == Shortcut.WEATHER && Build.MANUFACTURER.equals("samsung", ignoreCase = true)
        AppPickerDialog(
            title = "Choose ${shortcut.label} app",
            apps = installedApps,
            selected = setOfNotNull(store.shortcutPackages[shortcut]),
            loading = !installedAppsLoaded,
            supportingText = if (showSamsungWeatherGuide) SAMSUNG_WEATHER_GUIDE else null,
            supportingActionLabel = if (showSamsungWeatherGuide) "Open Apps settings" else null,
            onSupportingAction = actions::openInstalledAppsSettings,
            onApp = {
                store.assignShortcut(shortcut, it.packageName)
                pickingShortcut = null
            },
            onReset = {
                store.resetShortcut(shortcut)
                pickingShortcut = null
            },
            resetLabel = "Use system default",
            onDismiss = { pickingShortcut = null },
        )
    }
}

internal const val SAMSUNG_WEATHER_GUIDE =
    "Samsung Weather missing? Turn on Settings > Apps > Samsung app settings > Weather settings > Show Weather on Apps screen, then return here."

private fun Shortcut.setupIcon(): ImageVector = when (this) {
    Shortcut.NOTE -> Icons.Default.EditNote
    Shortcut.EVENT -> Icons.Default.Event
    Shortcut.WEATHER -> Icons.Default.Cloud
    Shortcut.CALL -> Icons.Default.Call
    Shortcut.MESSAGE -> Icons.AutoMirrored.Filled.Message
    Shortcut.FILES -> Icons.Default.FolderOpen
    Shortcut.TODO -> Icons.Default.CheckCircle
    Shortcut.DRAWER -> Icons.Default.GridView
}

@Composable
internal fun OnboardingPoint(icon: ImageVector, title: String, description: String) {
    Row(verticalAlignment = Alignment.Top) {
        Icon(icon, null, tint = Rust, modifier = Modifier.size(24.dp))
        Column(Modifier.padding(start = 12.dp)) {
            Text(title, fontWeight = FontWeight.Bold)
            Text(description, color = Muted, fontSize = 14.sp)
        }
    }
}

@Composable
internal fun MagicKeyRow(key: String, description: String) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(MaterialTheme.colorScheme.surfaceContainerLow).padding(11.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(36.dp).clip(CircleShape).background(LightInk), contentAlignment = Alignment.Center) {
            Text(key, color = LightPaper, fontSize = 20.sp, fontWeight = FontWeight.Black)
        }
        Text(description, Modifier.padding(start = 12.dp), fontWeight = FontWeight.SemiBold)
    }
}
