@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class, androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package com.katoaapps.openminilaunch

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ScrollState
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.pluralStringResource
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private data class AppListLoadState(
    val apps: List<LaunchableApp> = emptyList(),
    val loaded: Boolean = false,
)

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
    var pickingShortcut by remember { mutableStateOf<Shortcut?>(null) }
    var pickingDrawer by remember { mutableStateOf(false) }
    var pickingAi by remember { mutableStateOf(false) }
    var pickingAllAi by remember { mutableStateOf(false) }
    var pickingWeb by remember { mutableStateOf(false) }
    var pickingSocialApps by remember { mutableStateOf(false) }
    var appListRefresh by remember { mutableIntStateOf(0) }
    val loadInstalledApps = pickingShortcut != null || pickingDrawer
    val installedApps by produceState(AppListLoadState(), loadInstalledApps, appListRefresh) {
        if (loadInstalledApps) {
            value = AppListLoadState(
                apps = withContext(Dispatchers.IO) { actions.installedApps() },
                loaded = true,
            )
        }
    }
    val curatedAiApps by produceState(AppListLoadState(), pickingAi, appListRefresh) {
        if (pickingAi) {
            value = AppListLoadState(
                apps = withContext(Dispatchers.IO) { actions.curatedAiApps() },
                loaded = true,
            )
        }
    }
    val allAiApps by produceState(AppListLoadState(), pickingAllAi, appListRefresh) {
        if (pickingAllAi) {
            value = AppListLoadState(
                apps = withContext(Dispatchers.IO) { actions.textShareApps() },
                loaded = true,
            )
        }
    }
    val webApps by produceState(AppListLoadState(), pickingWeb, appListRefresh) {
        if (pickingWeb) {
            value = AppListLoadState(
                apps = withContext(Dispatchers.IO) { actions.webSearchApps() },
                loaded = true,
            )
        }
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
    LaunchedEffect(allAiApps.loaded, allAiApps.apps, store.preferredAiPackage) {
        if (allAiApps.loaded && store.preferredAiPackage != null && allAiApps.apps.none { it.packageName == store.preferredAiPackage }) {
            store.resetPreferredAiApp()
        }
    }
    LaunchedEffect(webApps.loaded, webApps.apps, store.preferredWebPackage) {
        if (webApps.loaded && store.preferredWebPackage != null && webApps.apps.none { it.packageName == store.preferredWebPackage }) {
            store.resetPreferredWebApp()
        }
    }
    val permissionState = SettingsPermissionState(
        usageAccessGranted = usageAccessGranted,
        notificationAccessGranted = notificationAccessGranted,
        contactsGranted = contactsGranted,
        directCallsSupported = directCallsSupported,
        callsGranted = callsGranted,
        directSmsSupported = supportsDirectSms(context),
        assistantRoleHeld = assistantRoleHeld,
        smsGranted = smsGranted,
        mediaGranted = mediaGranted,
        lockSupported = actions.supportsLockScreenAction(),
        lockServiceEnabled = lockServiceEnabled,
    )
    val permissionActions = SettingsPermissionActions(
        requestUsageAccess = { showUsageDisclosure = true },
        manageUsageAccess = { usageAccessSettings.launch(usageInsights.accessSettingsIntent()) },
        requestNotificationAccess = { showNotificationDisclosure = true },
        manageNotificationAccess = { notificationAccessSettings.launch(NotificationHub.accessSettingsIntent()) },
        requestContacts = { contactsPermission.launch(Manifest.permission.READ_CONTACTS) },
        requestCalls = { callPermission.launch(Manifest.permission.CALL_PHONE) },
        requestSms = { smsPermission.launch(Manifest.permission.SEND_SMS) },
        requestMedia = { mediaPermission.launch(mediaReadPermissions()) },
        manageAppPermissions = actions::openAppSettings,
        requestLockService = { showLockDisclosure = true },
        manageLockService = actions::openLockAccessibilitySettings,
        showAssistantSetup = { showAssistantDisclosure = true },
    )
    var settingsStack by remember { mutableStateOf(listOf(SettingsDestination.OVERVIEW)) }
    var navigatingBack by remember { mutableStateOf(false) }
    val settingsScrollStates = remember {
        SettingsDestination.entries.associateWith { ScrollState(0) }
    }
    val destination = settingsStack.last()

    fun navigateTo(next: SettingsDestination) {
        navigatingBack = false
        settingsStack = pushSettingsDestination(settingsStack, next)
    }

    fun navigateBack() {
        if (settingsStack.size > 1) {
            navigatingBack = true
            settingsStack = popSettingsDestination(settingsStack)
        } else {
            goBack()
        }
    }

    BackHandler(enabled = settingsStack.size > 1) { navigateBack() }

    AnimatedContent(
        targetState = destination,
        transitionSpec = {
            val direction = if (navigatingBack) -1 else 1
            (slideInHorizontally(tween(180)) { width -> direction * width / 6 } + fadeIn(tween(150)))
                .togetherWith(
                    slideOutHorizontally(tween(180)) { width -> -direction * width / 6 } + fadeOut(tween(120)),
                )
        },
        label = "settings-page",
    ) { page ->
        CompositionLocalProvider(LocalSettingsScrollState provides settingsScrollStates.getValue(page)) {
            when (page) {
            SettingsDestination.OVERVIEW -> SettingsOverviewPage(
                store = store,
                permissionState = permissionState,
                onNavigate = ::navigateTo,
                goBack = goBack,
            )
            SettingsDestination.LAUNCHER -> LauncherSettingsPage(
                store = store,
                assistantRoleHeld = assistantRoleHeld,
                requestHomeRole = requestHomeRole,
                showAssistantDisclosure = { showAssistantDisclosure = true },
                onNavigate = ::navigateTo,
                goBack = ::navigateBack,
            )
            SettingsDestination.APPEARANCE -> AppearanceSettingsPage(store, ::navigateBack)
            SettingsDestination.SHORTCUTS -> ShortcutsSettingsPage(
                store = store,
                actions = actions,
                onPickShortcut = { pickingShortcut = it },
                onPickDrawer = { pickingDrawer = true },
                goBack = ::navigateBack,
            )
            SettingsDestination.MAGIC_BOX -> MagicBoxSettingsPage(
                store = store,
                actions = actions,
                mediaGranted = mediaGranted,
                onPickWeb = { pickingWeb = true },
                onPickAi = { pickingAi = true },
                onOpenFileSearch = { navigateTo(SettingsDestination.FILE_SEARCH) },
                goBack = ::navigateBack,
            )
            SettingsDestination.FILE_SEARCH -> FileSearchSettingsPage(
                store = store,
                mediaGranted = mediaGranted,
                onOpenPermissions = { navigateTo(SettingsDestination.PERMISSIONS) },
                onAddFolder = { folderPicker.launch(null) },
                onRemoveFolder = { folder ->
                    runCatching {
                        context.contentResolver.releasePersistableUriPermission(
                            android.net.Uri.parse(folder.uri),
                            Intent.FLAG_GRANT_READ_URI_PERMISSION,
                        )
                    }
                    store.removeSearchFolder(folder.uri)
                    fileSearchRepository.invalidateFolders()
                },
                goBack = ::navigateBack,
            )
            SettingsDestination.MINK_DAY -> MinkDaySettingsPage(
                store = store,
                usageAccessGranted = usageAccessGranted,
                onPickSocialApps = { pickingSocialApps = true },
                onOpenPermissions = { navigateTo(SettingsDestination.PERMISSIONS) },
                goBack = ::navigateBack,
            )
            SettingsDestination.PERMISSIONS -> PermissionsSettingsPage(
                state = permissionState,
                actions = permissionActions,
                goBack = ::navigateBack,
            )
            SettingsDestination.ABOUT -> AboutSettingsPage(actions, onRepeatTutorial, ::navigateBack)
            }
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
        val showSamsungWeatherGuide = Build.MANUFACTURER.equals("samsung", ignoreCase = true)
        AppPickerDialog(
            title = stringResource(R.string.choose_app_for_shortcut, shortcut.displaySlotLabel()),
            apps = installedApps.apps,
            selected = setOfNotNull(store.shortcutPackages[shortcut]),
            loading = !installedApps.loaded,
            supportingText = if (showSamsungWeatherGuide) stringResource(R.string.samsung_weather_guide) else null,
            supportingActionLabel = if (showSamsungWeatherGuide) stringResource(R.string.open_apps_settings) else null,
            onSupportingAction = actions::openInstalledAppsSettings,
            onApp = { store.assignShortcut(shortcut, it.packageName); pickingShortcut = null },
            onReset = { store.resetShortcut(shortcut); pickingShortcut = null },
            resetLabel = stringResource(R.string.restore_shortcut_default, shortcut.displayLabel()),
            onDismiss = { pickingShortcut = null },
        )
    }
    if (pickingDrawer) {
        AppPickerDialog(
            title = pluralStringResource(
                R.plurals.drawer_apps_title,
                store.drawerPackages.size,
                store.drawerPackages.size,
                MAX_DRAWER_APPS,
            ),
            apps = installedApps.apps,
            selected = store.drawerPackages.toSet(),
            loading = !installedApps.loaded,
            onApp = { app ->
                val fillingDrawer = app.packageName !in store.drawerPackages && store.drawerPackages.size == MAX_DRAWER_APPS - 1
                store.toggleDrawerApp(app.packageName)
                if (fillingDrawer) {
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
            },
            onSelectionLimit = {
                Toast.makeText(
                    context,
                    context.resources.getQuantityString(
                        R.plurals.maximum_apps_selected,
                        MAX_DRAWER_APPS,
                        MAX_DRAWER_APPS,
                    ),
                    Toast.LENGTH_SHORT,
                ).show()
            },
            onDismiss = { pickingDrawer = false },
            multiSelect = true,
            selectionLimit = MAX_DRAWER_APPS,
        )
    }
    if (pickingAi) {
        AppPickerDialog(
            title = stringResource(R.string.choose_ai_app_title),
            apps = curatedAiApps.apps,
            selected = setOfNotNull(store.preferredAiPackage),
            loading = !curatedAiApps.loaded,
            emptyMessage = stringResource(R.string.no_curated_ai_apps_short),
            extraActionLabel = stringResource(R.string.other_compatible_app),
            onExtraAction = { pickingAi = false; pickingAllAi = true },
            onApp = { store.setPreferredAiApp(it.packageName); pickingAi = false },
            onReset = { store.resetPreferredAiApp(); pickingAi = false },
            resetLabel = stringResource(R.string.reset_to_chooser),
            onDismiss = { pickingAi = false },
        )
    }
    if (pickingAllAi) {
        AppPickerDialog(
            title = stringResource(R.string.other_compatible_apps),
            apps = allAiApps.apps,
            selected = setOfNotNull(store.preferredAiPackage),
            loading = !allAiApps.loaded,
            onApp = { store.setPreferredAiApp(it.packageName); pickingAllAi = false },
            onReset = { store.resetPreferredAiApp(); pickingAllAi = false },
            resetLabel = stringResource(R.string.reset_to_chooser),
            onDismiss = { pickingAllAi = false },
        )
    }
    if (pickingWeb) {
        AppPickerDialog(
            title = stringResource(R.string.choose_web_search_app),
            apps = webApps.apps,
            selected = setOfNotNull(store.preferredWebPackage),
            loading = !webApps.loaded,
            emptyMessage = stringResource(R.string.no_web_search_apps),
            onApp = { store.setPreferredWebApp(it.packageName); pickingWeb = false },
            onReset = { store.resetPreferredWebApp(); pickingWeb = false },
            resetLabel = stringResource(R.string.use_system_browser),
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
