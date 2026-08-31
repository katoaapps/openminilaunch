@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class, androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package com.katoaapps.openminilaunch.ui.settings

import com.katoaapps.openminilaunch.data.*
import com.katoaapps.openminilaunch.model.*
import com.katoaapps.openminilaunch.platform.*
import com.katoaapps.openminilaunch.features.files.*
import com.katoaapps.openminilaunch.features.messaging.MessagingProviderCatalog
import com.katoaapps.openminilaunch.ui.onboarding.FileSearchScopeDialog

import android.content.Intent
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
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
internal fun SettingsScreen(
    store: LauncherStore,
    actions: DeviceActions,
    requestHomeRole: () -> Unit,
    onRepeatTutorial: () -> Unit,
    initialDestination: SettingsDestination = SettingsDestination.OVERVIEW,
    goBack: () -> Unit,
) {
    val context = LocalContext.current
    val fileSearchRepository = remember { FileSearchRepository(context.applicationContext) }
    var picker by remember { mutableStateOf<SettingsPicker?>(null) }
    var appListRefresh by remember { mutableIntStateOf(0) }
    val launcherAppsRevision by actions.launcherAppsRevision.collectAsState()
    val launcherShortcutsRevision by actions.launcherShortcutsRevision.collectAsState()
    val loadLauncherTargets = picker is SettingsPicker.ShortcutApp || picker == SettingsPicker.DrawerApps
    val installedApps by produceState(
        LauncherAppListLoadState(),
        loadLauncherTargets,
        appListRefresh,
        launcherAppsRevision,
    ) {
        if (loadLauncherTargets) {
            value = LauncherAppListLoadState(
                apps = withContext(Dispatchers.IO) { actions.installedApps() },
                loaded = true,
            )
        }
    }
    val installedShortcuts by produceState(
        LauncherShortcutListLoadState(),
        loadLauncherTargets,
        appListRefresh,
        launcherShortcutsRevision,
    ) {
        if (loadLauncherTargets) {
            value = LauncherShortcutListLoadState(
                shortcuts = withContext(Dispatchers.IO) { actions.installedShortcuts() },
                loaded = true,
            )
        }
    }
    val curatedAiApps by produceState(AppListLoadState(), picker, appListRefresh) {
        if (picker == SettingsPicker.CuratedAiApp) {
            value = AppListLoadState(
                apps = withContext(Dispatchers.IO) { actions.curatedAiApps() },
                loaded = true,
            )
        }
    }
    val allAiApps by produceState(AppListLoadState(), picker, appListRefresh) {
        if (picker == SettingsPicker.CompatibleAiApp) {
            value = AppListLoadState(
                apps = withContext(Dispatchers.IO) { actions.textShareApps() },
                loaded = true,
            )
        }
    }
    val webApps by produceState(AppListLoadState(), picker, appListRefresh) {
        if (picker == SettingsPicker.WebApp) {
            value = AppListLoadState(
                apps = withContext(Dispatchers.IO) { actions.webSearchApps() },
                loaded = true,
            )
        }
    }
    val messagingProviders by produceState(
        MessagingProviderLoadState(),
        picker,
        appListRefresh,
    ) {
        if (picker == SettingsPicker.MessagingApp) {
            value = MessagingProviderLoadState(
                options = withContext(Dispatchers.IO) { actions.messagingProviderOptions() },
                loaded = true,
            )
        }
    }
    var showFileScopeChoice by remember { mutableStateOf(false) }
    val permissionHost = rememberSettingsPermissionHost(actions) {
        showFileScopeChoice = true
    }
    DisposableEffect(context, actions) {
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
    LaunchedEffect(
        messagingProviders.loaded,
        messagingProviders.options,
        store.preferredMessagingPackage,
    ) {
        val savedPackage = store.preferredMessagingPackage
        if (messagingProviders.loaded && savedPackage != null) {
            val savedProviderId = MessagingProviderCatalog.providerForPackage(savedPackage)?.id
            val installedVariant = messagingProviders.options.firstOrNull {
                it.id == savedProviderId && it.selectable
            }
            // Preferences store the concrete package so handoffs remain explicit. If an official
            // alternate build replaces it, migrate to that installed package without changing the
            // provider selected by the user.
            when {
                installedVariant == null -> store.resetPreferredMessagingApp()
                installedVariant.preferencePackageName != savedPackage -> {
                    installedVariant.preferencePackageName?.let(store::setPreferredMessagingApp)
                }
            }
        }
    }
    var settingsStack by remember(initialDestination) { mutableStateOf(settingsPathTo(initialDestination)) }
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
            SettingsDestinationContent(
                destination = page,
                store = store,
                actions = actions,
                permissionState = permissionHost.state,
                permissionActions = permissionHost.actions,
                requestHomeRole = requestHomeRole,
                onRepeatTutorial = onRepeatTutorial,
                onNavigate = ::navigateTo,
                onNavigateBack = ::navigateBack,
                onExitSettings = goBack,
                onPickShortcut = { picker = SettingsPicker.ShortcutApp(it) },
                onPickDrawer = { picker = SettingsPicker.DrawerApps },
                onPickWeb = { picker = SettingsPicker.WebApp },
                onPickAi = { picker = SettingsPicker.CuratedAiApp },
                onPickMessagingApp = { picker = SettingsPicker.MessagingApp },
                onPickSocialApps = { picker = SettingsPicker.MinkDayApps },
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
            )
        }
    }

    SettingsPickerDialogs(
        picker = picker,
        store = store,
        actions = actions,
        context = context,
        installedApps = installedApps,
        installedShortcuts = installedShortcuts,
        curatedAiApps = curatedAiApps,
        allAiApps = allAiApps,
        webApps = webApps,
        messagingProviders = messagingProviders,
        usageInsights = permissionHost.usageInsights,
        onPickerChange = { picker = it },
    )
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
