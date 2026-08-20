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
internal fun SettingsScreen(
    store: LauncherStore,
    actions: DeviceActions,
    requestHomeRole: () -> Unit,
    onRepeatTutorial: () -> Unit,
    goBack: () -> Unit,
) {
    val context = LocalContext.current
    val fileSearchRepository = remember { FileSearchRepository(context.applicationContext) }
    var appListRefresh by remember { mutableIntStateOf(0) }
    val apps by produceState<List<LaunchableApp>>(initialValue = emptyList(), appListRefresh) {
        value = withContext(Dispatchers.IO) { actions.installedApps() }
    }
    var aiAppsLoaded by remember { mutableStateOf(false) }
    val curatedAiApps by produceState<List<LaunchableApp>>(initialValue = emptyList()) {
        value = withContext(Dispatchers.IO) { actions.curatedAiApps() }
        aiAppsLoaded = true
    }
    var allAiAppsLoaded by remember { mutableStateOf(false) }
    val allAiApps by produceState<List<LaunchableApp>>(initialValue = emptyList()) {
        value = withContext(Dispatchers.IO) { actions.textShareApps() }
        allAiAppsLoaded = true
    }
    var webAppsLoaded by remember { mutableStateOf(false) }
    val webApps by produceState<List<LaunchableApp>>(initialValue = emptyList()) {
        value = withContext(Dispatchers.IO) { actions.webSearchApps() }
        webAppsLoaded = true
    }
    var contactsGranted by remember {
        mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED)
    }
    var mediaGranted by remember { mutableStateOf(hasMediaReadAccess(context)) }
    var callsGranted by remember {
        mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.CALL_PHONE) == PackageManager.PERMISSION_GRANTED)
    }
    var smsGranted by remember {
        mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.SEND_SMS) == PackageManager.PERMISSION_GRANTED)
    }
    var lockServiceEnabled by remember { mutableStateOf(actions.isLockServiceEnabled()) }
    var showLockDisclosure by remember { mutableStateOf(false) }
    var assistantRoleHeld by remember { mutableStateOf(actions.isAssistantRoleHeld()) }
    var showAssistantDisclosure by remember { mutableStateOf(false) }
    var notificationAccessGranted by remember { mutableStateOf(NotificationHub.hasAccess(context)) }
    var showNotificationDisclosure by remember { mutableStateOf(false) }
    val usageInsights = remember { UsageInsightsRepository(context.applicationContext) }
    var usageAccessGranted by remember { mutableStateOf(usageInsights.hasAccess()) }
    var showUsageDisclosure by remember { mutableStateOf(false) }
    val directCallsSupported = remember(context) { supportsDirectCalls(context) }
    var showFileScopeChoice by remember { mutableStateOf(false) }
    DisposableEffect(context) {
        val lifecycle = (context as? ComponentActivity)?.lifecycle
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                actions.invalidateInstalledApps()
                appListRefresh++
                contactsGranted = ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED
                mediaGranted = hasMediaReadAccess(context)
                callsGranted = ContextCompat.checkSelfPermission(context, Manifest.permission.CALL_PHONE) == PackageManager.PERMISSION_GRANTED
                smsGranted = ContextCompat.checkSelfPermission(context, Manifest.permission.SEND_SMS) == PackageManager.PERMISSION_GRANTED
                lockServiceEnabled = actions.isLockServiceEnabled()
                assistantRoleHeld = actions.isAssistantRoleHeld()
                notificationAccessGranted = NotificationHub.hasAccess(context)
                usageAccessGranted = usageInsights.hasAccess()
            }
        }
        lifecycle?.addObserver(observer)
        onDispose { lifecycle?.removeObserver(observer) }
    }
    val contactsPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        contactsGranted = granted
        if (!granted && isPermanentlyDenied(context, Manifest.permission.READ_CONTACTS)) actions.openAppSettings()
    }
    val callPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        callsGranted = granted
        if (!granted && isPermanentlyDenied(context, Manifest.permission.CALL_PHONE)) actions.openAppSettings()
    }
    val smsPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        smsGranted = granted
        if (!granted && isPermanentlyDenied(context, Manifest.permission.SEND_SMS)) actions.openAppSettings()
    }
    val mediaPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        mediaGranted = hasMediaReadAccess(context)
        if (!mediaGranted && mediaPermissionPermanentlyDenied(context)) actions.openAppSettings()
        showFileScopeChoice = true
    }
    val lockServiceSettings = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        lockServiceEnabled = actions.isLockServiceEnabled()
    }
    val assistantRoleSettings = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        assistantRoleHeld = actions.isAssistantRoleHeld()
    }
    val notificationAccessSettings = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        notificationAccessGranted = NotificationHub.hasAccess(context)
        if (notificationAccessGranted) NotificationHub.requestReconnect(context)
    }
    val usageAccessSettings = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        usageAccessGranted = usageInsights.hasAccess()
    }
    val folderPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        uri?.let {
            runCatching {
                context.contentResolver.takePersistableUriPermission(it, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            store.addSearchFolder(it.toString(), fileSearchRepository.folderLabel(it))
            fileSearchRepository.invalidateFolders()
        }
    }
    var pickingShortcut by remember { mutableStateOf<Shortcut?>(null) }
    var pickingDrawer by remember { mutableStateOf(false) }
    var pickingAi by remember { mutableStateOf(false) }
    var pickingAllAi by remember { mutableStateOf(false) }
    var pickingWeb by remember { mutableStateOf(false) }
    var pickingSocialApps by remember { mutableStateOf(false) }
    LaunchedEffect(allAiAppsLoaded, allAiApps, store.preferredAiPackage) {
        if (allAiAppsLoaded && store.preferredAiPackage != null && allAiApps.none { it.packageName == store.preferredAiPackage }) {
            store.resetPreferredAiApp()
        }
    }
    LaunchedEffect(webAppsLoaded, webApps, store.preferredWebPackage) {
        if (webAppsLoaded && store.preferredWebPackage != null && webApps.none { it.packageName == store.preferredWebPackage }) {
            store.resetPreferredWebApp()
        }
    }
    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        PageHeader("Settings", goBack)
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 22.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            SectionLabel("LAUNCHER")
            SettingsRow("Default home app", "Choose MinkLauncher OpenSource as your launcher", Icons.Default.Home) { requestHomeRole() }
            SettingsRow(
                "Mink Assistant",
                if (assistantRoleHeld) "Active · Magic Box from anywhere" else "Optional · use the system assistant gesture",
                Icons.Default.Assistant,
            ) { showAssistantDisclosure = true }
            HorizontalDivider(color = Sage)
            SectionLabel("APPEARANCE")
            ThemeChooser(store.themePreference, store::setTheme)
            HomePanelColorSetting(store.homePanelColorArgb, store::setHomePanelColor)
            HorizontalDivider(color = Sage)
            SectionLabel("MINK’S DAY")
            Text("Mink measures time and opens only for the social apps you choose, then keeps those insights on this device.", color = Muted, fontSize = 13.sp)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                SOCIAL_GOAL_OPTIONS.forEach { minutes ->
                    FilterChip(
                        selected = store.socialGoalMinutes == minutes,
                        onClick = { store.updateSocialGoalMinutes(minutes) },
                        label = { Text(socialGoalLabel(minutes)) },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
            SettingsRow(
                "Apps you want to limit",
                if (store.socialPackages.isEmpty()) "Automatic Android categories" else "${store.socialPackages.size} selected",
                Icons.Default.Apps,
            ) { pickingSocialApps = true }
            HorizontalDivider(color = Sage)
            SectionLabel("SHORTCUT APPS")
            Text("Choose the app each shortcut opens. To-do and Drawer stay built in.", color = Muted, fontSize = 13.sp)
            configurableShortcuts.forEach { shortcut ->
                SettingsRow(
                    shortcut.label,
                    store.shortcutPackages[shortcut]?.let(actions::appLabel) ?: "System default",
                    Icons.Default.ChevronRight,
                ) { pickingShortcut = shortcut }
            }
            SettingsRow(
                "Reset Home grid order",
                "Return shortcuts to the original 4 × 2 order",
                Icons.Default.Restore,
            ) {
                store.resetShortcutOrder()
                Toast.makeText(context, "Shortcut order reset", Toast.LENGTH_SHORT).show()
            }
            HorizontalDivider(color = Sage)
            SectionLabel("APP DRAWER")
            SettingsRow("Selected apps", "${store.drawerPackages.size} of $MAX_DRAWER_APPS", Icons.Default.Apps) { pickingDrawer = true }
            Text("Use ? in the Magic Box to find any other installed app.", color = Muted, fontSize = 13.sp)
            HorizontalDivider(color = Sage)
            SectionLabel("SEARCH")
            SettingsRow(
                "Web app",
                store.preferredWebPackage?.let(actions::appLabel) ?: "System browser",
                Icons.Default.Public,
            ) { pickingWeb = true }
            SettingsRow(
                "AI app",
                store.preferredAiPackage?.let(actions::appLabel) ?: "Choose on first use",
                Icons.Default.AutoAwesome,
            ) { pickingAi = true }
            Text(
                "Known AI assistants are shown first. Other text-sharing apps remain available as a fallback. Your query is handled under the selected app’s privacy terms.",
                color = Muted,
                fontSize = 13.sp,
            )
            HorizontalDivider(color = Sage)
            SectionLabel("MESSAGING")
            MessageSendModeChooser(store.messageSendMode, store::updateMessageSendMode)
            Text(
                "Always ask offers Send SMS now and Choose messaging app each time. Direct SMS uses your carrier and may incur charges; a chosen provider controls its own final send and delivery method.",
                color = Muted,
                fontSize = 13.sp,
            )
            HorizontalDivider(color = Sage)
            SectionLabel("PERMISSIONS")
            PermissionCard(
                title = "Mink’s Day usage",
                description = "Reads foreground activity for your tracked social apps only to calculate private, on-device daily insights.",
                granted = usageAccessGranted,
                icon = Icons.Default.Pets,
                onGrant = { showUsageDisclosure = true },
                onManage = { usageAccessSettings.launch(usageInsights.accessSettingsIntent()) },
            )
            PermissionCard(
                title = "Conversations",
                description = "Shows active message and email conversations locally and uses reply actions supplied by their apps.",
                granted = notificationAccessGranted,
                icon = Icons.Default.Forum,
                onGrant = { showNotificationDisclosure = true },
                onManage = { notificationAccessSettings.launch(NotificationHub.accessSettingsIntent()) },
            )
            PermissionCard(
                title = "Contacts",
                description = "Used only for @ messages and # calls.",
                granted = contactsGranted,
                icon = Icons.Default.Contacts,
                onGrant = { contactsPermission.launch(Manifest.permission.READ_CONTACTS) },
                onManage = { actions.openAppSettings() },
            )
            if (directCallsSupported) {
                PermissionCard(
                    title = "Direct calls",
                    description = "Used only after you confirm a # call in MinkLauncher OpenSource.",
                    granted = callsGranted,
                    icon = Icons.Default.Phone,
                    onGrant = { callPermission.launch(Manifest.permission.CALL_PHONE) },
                    onManage = { actions.openAppSettings() },
                )
            } else {
                SettingsRow("Direct calls", "Not supported on this device; # uses the dialer", Icons.Default.Phone, enabled = false) { }
            }
            if (supportsDirectSms(context)) {
                PermissionCard(
                    title = "Direct SMS",
                    description = if (assistantRoleHeld) {
                        "Used when you press @ Send with direct SMS selected. Uses carrier SMS, not RCS; messaging rates may apply."
                    } else {
                        "Choose Mink Assistant first. Google Play restricts direct SMS access to eligible default handlers."
                    },
                    granted = smsGranted,
                    icon = Icons.AutoMirrored.Filled.Send,
                    onGrant = {
                        if (assistantRoleHeld) smsPermission.launch(Manifest.permission.SEND_SMS)
                        else showAssistantDisclosure = true
                    },
                    onManage = { actions.openAppSettings() },
                )
            } else {
                SettingsRow("Direct SMS", "Not supported on this device; @ opens Messages", Icons.AutoMirrored.Filled.Send, enabled = false) { }
            }
            PermissionCard(
                title = "Photos, videos & audio",
                description = "Searches media filenames locally. MinkLauncher OpenSource never uploads your library.",
                granted = mediaGranted,
                icon = Icons.Default.PhotoLibrary,
                onGrant = { mediaPermission.launch(mediaReadPermissions()) },
                onManage = { actions.openAppSettings() },
            )
            if (actions.supportsLockScreenAction()) {
                PermissionCard(
                    title = "Double-tap screen lock",
                    description = "Uses Android's Lock screen action only after you double-tap. It cannot read screen content.",
                    granted = lockServiceEnabled,
                    icon = Icons.Default.Lock,
                    onGrant = { showLockDisclosure = true },
                    onManage = { actions.openLockAccessibilitySettings() },
                )
            } else {
                SettingsRow("Double-tap screen lock", "Requires Android 9 or newer", Icons.Default.Lock, enabled = false) { }
            }
            HorizontalDivider(color = Sage)
            SectionLabel("FILE SEARCH")
            Text(
                "Choose the document folders MinkLauncher OpenSource may search. Only filenames are indexed, and everything remains on this device.",
                color = Muted,
                fontSize = 13.sp,
            )
            OutlinedButton(onClick = { folderPicker.launch(null) }, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Default.CreateNewFolder, null)
                Text("Add search folder", Modifier.padding(start = 8.dp))
            }
            if (store.searchFolders.isEmpty()) {
                Text("No document folders selected.", color = Muted, fontSize = 12.sp)
            } else {
                store.searchFolders.forEach { folder ->
                    Row(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp))
                            .background(MaterialTheme.colorScheme.surfaceContainerLow).padding(start = 14.dp, end = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(Icons.Default.Folder, null, tint = Rust)
                        Text(folder.label, Modifier.weight(1f).padding(horizontal = 10.dp), maxLines = 1)
                        IconButton(onClick = {
                            runCatching {
                                context.contentResolver.releasePersistableUriPermission(
                                    android.net.Uri.parse(folder.uri),
                                    Intent.FLAG_GRANT_READ_URI_PERMISSION,
                                )
                            }
                            store.removeSearchFolder(folder.uri)
                            fileSearchRepository.invalidateFolders()
                        }) { Icon(Icons.Default.Close, "Remove ${folder.label}") }
                    }
                }
            }
            HorizontalDivider(color = Sage)
            SectionLabel("INFO")
            SettingsRow("Email us", "contact@katoaapps.com", Icons.Default.Email) { actions.emailSupport() }
            SettingsRow("Privacy policy", "How MinkLauncher OpenSource handles device data", Icons.Default.PrivacyTip) { actions.openPrivacyPolicy() }
            SettingsRow("Terms of use", "Responsibilities, warranties, and data-loss limitations", Icons.Default.Gavel) { actions.openTermsOfUse() }
            SettingsRow("Repeat tutorial", "Review setup, Magic Box, Mink’s Day, Conversations, widgets, and permissions", Icons.Default.School) { onRepeatTutorial() }
            Row(Modifier.fillMaxWidth().padding(vertical = 12.dp)) {
                Text("Version", Modifier.weight(1f), fontWeight = FontWeight.Medium)
                Text(BuildConfig.VERSION_NAME, color = Muted)
            }
            Spacer(Modifier.height(24.dp))
        }
    }

    if (showLockDisclosure) {
        LockAccessibilityDisclosureDialog(
            onContinue = {
                showLockDisclosure = false
                lockServiceSettings.launch(actions.lockAccessibilitySettingsIntent())
            },
            onDismiss = { showLockDisclosure = false },
        )
    }

    if (showAssistantDisclosure) {
        AssistantDisclosureDialog(
            active = assistantRoleHeld,
            onContinue = {
                showAssistantDisclosure = false
                assistantRoleSettings.launch(actions.assistantRoleSelectionIntent())
            },
            onDismiss = { showAssistantDisclosure = false },
        )
    }

    if (showNotificationDisclosure) {
        NotificationAccessDisclosureDialog(
            onContinue = {
                showNotificationDisclosure = false
                notificationAccessSettings.launch(NotificationHub.accessSettingsIntent())
            },
            onDismiss = { showNotificationDisclosure = false },
        )
    }
    if (showUsageDisclosure) {
        UsageAccessDisclosureDialog(
            onContinue = {
                showUsageDisclosure = false
                usageAccessSettings.launch(usageInsights.accessSettingsIntent())
            },
            onDismiss = { showUsageDisclosure = false },
        )
    }

    pickingShortcut?.let { shortcut ->
        val showSamsungWeatherGuide = shortcut == Shortcut.WEATHER && Build.MANUFACTURER.equals("samsung", ignoreCase = true)
        AppPickerDialog(
            title = "Choose ${shortcut.label} app",
            apps = apps,
            selected = setOfNotNull(store.shortcutPackages[shortcut]),
            supportingText = if (showSamsungWeatherGuide) SAMSUNG_WEATHER_GUIDE else null,
            supportingActionLabel = if (showSamsungWeatherGuide) "Open Apps settings" else null,
            onSupportingAction = actions::openInstalledAppsSettings,
            onApp = { store.assignShortcut(shortcut, it.packageName); pickingShortcut = null },
            onReset = { store.resetShortcut(shortcut); pickingShortcut = null },
            onDismiss = { pickingShortcut = null },
        )
    }
    if (pickingDrawer) {
        AppPickerDialog(
            title = "Drawer apps · ${store.drawerPackages.size}/$MAX_DRAWER_APPS",
            apps = apps,
            selected = store.drawerPackages.toSet(),
            onApp = { app ->
                val fillingDrawer = app.packageName !in store.drawerPackages && store.drawerPackages.size == MAX_DRAWER_APPS - 1
                store.toggleDrawerApp(app.packageName)
                if (fillingDrawer) Toast.makeText(context, "$MAX_DRAWER_APPS apps selected", Toast.LENGTH_SHORT).show()
            },
            onSelectionLimit = {
                Toast.makeText(context, "Maximum of $MAX_DRAWER_APPS apps selected", Toast.LENGTH_SHORT).show()
            },
            onDismiss = { pickingDrawer = false },
            multiSelect = true,
            selectionLimit = MAX_DRAWER_APPS,
        )
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
            resetLabel = "Reset to chooser",
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
            onReset = { store.resetPreferredAiApp(); pickingAllAi = false },
            resetLabel = "Reset to chooser",
            onDismiss = { pickingAllAi = false },
        )
    }
    if (pickingWeb) {
        AppPickerDialog(
            title = "Choose web search app",
            apps = webApps,
            selected = setOfNotNull(store.preferredWebPackage),
            loading = !webAppsLoaded,
            emptyMessage = "No web search apps found.",
            onApp = { store.setPreferredWebApp(it.packageName); pickingWeb = false },
            onReset = { store.resetPreferredWebApp(); pickingWeb = false },
            resetLabel = "Use system browser",
            onDismiss = { pickingWeb = false },
        )
    }
    if (pickingSocialApps) {
        SocialAppsDialog(store, usageInsights) { pickingSocialApps = false }
    }
    if (showFileScopeChoice) {
        FileSearchScopeDialog(
            onChooseFolder = {
                showFileScopeChoice = false
                folderPicker.launch(null)
            },
            onSkip = { showFileScopeChoice = false },
        )
    }
}

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
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Default.Lock, null, tint = Rust) },
        title = { Text("Enable double-tap to lock?") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("To make double-tap behave like the power button, MinkLauncher OpenSource uses Android's accessibility Lock screen action.")
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
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Default.Assistant, null, tint = Rust) },
        title = { Text(if (active) "Mink Assistant is active" else "Use Mink Assistant?") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Your phone's assistant gesture will open the keyboard-first Magic Box over your current app.")
                Text("Choosing MinkLauncher OpenSource replaces your current default digital assistant. You can switch back at any time in system settings.")
                Text("Mink Assistant does not request microphone, call-log, screen-reading, or screen-context access. Android may grant Send SMS as part of the assistant role; MinkLauncher OpenSource uses it only after you choose a recipient, write the message, and press the @ action.")
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
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Default.Forum, null, tint = Rust) },
        title = { Text("Enable Conversations?") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Android notification access lets MinkLauncher OpenSource read active message and email notifications, group them into conversations, and offer inline reply when the originating app provides a reply action.")
                Text("Notifications outside messages and email are ignored. Conversation contents and replies are kept in memory only. They are not stored by MinkLauncher OpenSource, uploaded, or sent to Katoa Apps.")
                Text("Replies are handed directly to the app that created the notification. You can revoke access at any time in Android settings.")
            }
        },
        confirmButton = { Button(onClick = onContinue) { Text("Continue") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Not now") } },
    )
}
