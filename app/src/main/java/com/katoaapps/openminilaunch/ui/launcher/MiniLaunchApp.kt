@file:OptIn(
    androidx.compose.material3.ExperimentalMaterial3Api::class,
    androidx.compose.foundation.layout.ExperimentalLayoutApi::class,
)

package com.katoaapps.openminilaunch.ui.launcher

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.content.ContextCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.katoaapps.openminilaunch.data.LauncherStore
import com.katoaapps.openminilaunch.features.conversations.NotificationHub
import com.katoaapps.openminilaunch.features.wellbeing.UsageInsightsRepository
import com.katoaapps.openminilaunch.model.Screen
import com.katoaapps.openminilaunch.model.ThemePreference
import com.katoaapps.openminilaunch.platform.DeviceActions
import com.katoaapps.openminilaunch.ui.conversations.NotificationHubScreen
import com.katoaapps.openminilaunch.ui.onboarding.FeatureUpdateDialog
import com.katoaapps.openminilaunch.ui.onboarding.OnboardingScreen
import com.katoaapps.openminilaunch.ui.onboarding.ShortcutSetupDialog
import com.katoaapps.openminilaunch.ui.onboarding.UsageAccessDisclosureDialog
import com.katoaapps.openminilaunch.ui.settings.NotificationAccessDisclosureDialog
import com.katoaapps.openminilaunch.ui.settings.SettingsDestination
import com.katoaapps.openminilaunch.ui.settings.SettingsScreen
import com.katoaapps.openminilaunch.ui.theme.DarkBackground
import com.katoaapps.openminilaunch.ui.theme.DarkOnPrimary
import com.katoaapps.openminilaunch.ui.theme.DarkOnSurface
import com.katoaapps.openminilaunch.ui.theme.DarkPrimary
import com.katoaapps.openminilaunch.ui.theme.DarkSurface
import com.katoaapps.openminilaunch.ui.theme.DarkSurfaceContainerLow
import com.katoaapps.openminilaunch.ui.theme.LightInk
import com.katoaapps.openminilaunch.ui.theme.LightPaper
import com.katoaapps.openminilaunch.ui.theme.MinkTransparent
import com.katoaapps.openminilaunch.ui.theme.MinkWhite
import com.katoaapps.openminilaunch.ui.theme.Rust
import com.katoaapps.openminilaunch.ui.theme.withAppBackground
import com.katoaapps.openminilaunch.ui.todos.TodosScreen
import com.katoaapps.openminilaunch.ui.wellbeing.MinkDayScreen
import com.katoaapps.openminilaunch.ui.widgets.WidgetPage
import kotlinx.coroutines.launch

private const val FEATURE_UPDATE_ID = "portable_backup_and_ai_catalog_v1"
private const val MINK_DAY_PAGE = 0
private const val HOME_PAGE = 1
private const val WIDGET_PAGE = 2

@Composable
internal fun MiniLaunchApp(
    store: LauncherStore,
    actions: DeviceActions,
    requestHomeRole: () -> Unit,
    homeRequestToken: Int,
) {
    var screen by remember { mutableStateOf(Screen.HOME) }
    var settingsDestination by remember { mutableStateOf(SettingsDestination.OVERVIEW) }
    var showTutorial by rememberSaveable { mutableStateOf(!store.onboardingComplete) }
    var showUpdateNotice by rememberSaveable {
        mutableStateOf(store.onboardingComplete && !store.hasSeenUpdate(FEATURE_UPDATE_ID))
    }
    var showShortcutSetup by rememberSaveable { mutableStateOf(false) }
    var showNotificationAccessPrompt by rememberSaveable { mutableStateOf(false) }
    var showUsageAccessPrompt by rememberSaveable { mutableStateOf(false) }
    var tutorialRun by rememberSaveable { mutableIntStateOf(0) }
    var homeMagicExpanded by remember { mutableStateOf(false) }
    var animateHomeEntrance by remember { mutableStateOf(false) }
    val launcherPagerState = rememberPagerState(initialPage = HOME_PAGE, pageCount = { WIDGET_PAGE + 1 })
    val launcherScope = rememberCoroutineScope()
    LaunchedEffect(homeRequestToken) {
        if (homeRequestToken > 0) {
            screen = Screen.HOME
            launcherPagerState.animateScrollToPage(HOME_PAGE)
        }
    }
    val context = LocalContext.current
    val usageInsightsRepository = remember(context) {
        UsageInsightsRepository(context.applicationContext)
    }
    val systemDark = isSystemInDarkTheme()
    val darkTheme = when (store.themePreference) {
        ThemePreference.SYSTEM -> systemDark
        ThemePreference.LIGHT -> false
        ThemePreference.DARK -> true
    }
    val fallbackColors = if (darkTheme) {
        darkColorScheme(
            primary = DarkPrimary,
            onPrimary = DarkOnPrimary,
            background = DarkBackground,
            surface = DarkSurface,
            surfaceContainerLow = DarkSurfaceContainerLow,
            onSurface = DarkOnSurface,
            secondary = Rust,
        )
    } else {
        lightColorScheme(
            primary = LightInk,
            onPrimary = LightPaper,
            background = LightPaper,
            surface = LightPaper,
            surfaceContainerLow = MinkWhite,
            onSurface = LightInk,
            secondary = Rust,
        )
    }
    val baseColors = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
    } else {
        fallbackColors
    }
    val colors = baseColors.withAppBackground(store.effectiveAppBackgroundColorArgb)
    val view = LocalView.current

    val finishOnboardingSetup = {
        if (store.hasConfirmedAllShortcutChoices()) requestHomeRole()
        else showShortcutSetup = true
    }
    val finishSpecialAccessSetup = {
        if (usageInsightsRepository.hasAccess()) finishOnboardingSetup()
        else showUsageAccessPrompt = true
    }
    val finishPermissionSetup = {
        if (NotificationHub.hasAccess(context)) finishSpecialAccessSetup()
        else showNotificationAccessPrompt = true
    }
    val onboardingNotificationAccess = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) {
        if (NotificationHub.hasAccess(context)) {
            showNotificationAccessPrompt = false
            NotificationHub.requestReconnect(context)
            finishSpecialAccessSetup()
        }
    }
    val onboardingUsageAccess = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) {
        finishOnboardingSetup()
    }
    val onboardingSmsPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (!granted && isPermanentlyDenied(context, Manifest.permission.SEND_SMS)) {
            actions.openAppSettings()
        }
        finishPermissionSetup()
    }
    val continueAfterCallPermission = {
        val directSmsCanBeUsed = supportsDirectSms(context) &&
            actions.isAssistantRoleHeld() &&
            store.sendMessagesAutomatically &&
            store.preferredMessagingPackage == null
        if (directSmsCanBeUsed &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED
        ) {
            onboardingSmsPermission.launch(Manifest.permission.SEND_SMS)
        } else {
            finishPermissionSetup()
        }
    }
    val onboardingCallPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (!granted && isPermanentlyDenied(context, Manifest.permission.CALL_PHONE)) {
            actions.openAppSettings()
        }
        continueAfterCallPermission()
    }
    val onboardingPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (!granted && isPermanentlyDenied(context, Manifest.permission.READ_CONTACTS)) {
            actions.openAppSettings()
            continueAfterCallPermission()
        } else if (!supportsDirectCalls(context) ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.CALL_PHONE) == PackageManager.PERMISSION_GRANTED
        ) {
            continueAfterCallPermission()
        } else {
            onboardingCallPermission.launch(Manifest.permission.CALL_PHONE)
        }
    }

    val transparent = MinkTransparent
    val hideHomeStatusBar = store.hideStatusBar && screen == Screen.HOME && !showTutorial
    SideEffect {
        val window = (context as Activity).window
        window.statusBarColor = transparent.toArgb()
        window.navigationBarColor = transparent.toArgb()
        WindowInsetsControllerCompat(window, view).apply {
            isAppearanceLightStatusBars = !darkTheme
            isAppearanceLightNavigationBars = !darkTheme
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            if (hideHomeStatusBar) hide(WindowInsetsCompat.Type.statusBars())
            else show(WindowInsetsCompat.Type.statusBars())
        }
    }

    MaterialTheme(colorScheme = colors, typography = Typography()) {
        BackHandler(enabled = !showTutorial && screen != Screen.HOME) { screen = Screen.HOME }
        BackHandler(
            enabled = !showTutorial && screen == Screen.HOME && launcherPagerState.currentPage != HOME_PAGE,
        ) {
            launcherScope.launch { launcherPagerState.animateScrollToPage(HOME_PAGE) }
        }
        val imeVisible = WindowInsets.isImeVisible
        BackHandler(
            enabled = !showTutorial && screen == Screen.HOME &&
                launcherPagerState.currentPage == HOME_PAGE && !imeVisible,
        ) { }

        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            if (showTutorial) {
                key(tutorialRun) {
                    OnboardingScreen(
                        store = store,
                        actions = actions,
                        onFinish = {
                            store.completeOnboarding()
                            store.markUpdateSeen(FEATURE_UPDATE_ID)
                            showTutorial = false
                            if (ContextCompat.checkSelfPermission(
                                    context,
                                    Manifest.permission.READ_CONTACTS,
                                ) == PackageManager.PERMISSION_GRANTED
                            ) {
                                if (!supportsDirectCalls(context) ||
                                    ContextCompat.checkSelfPermission(
                                        context,
                                        Manifest.permission.CALL_PHONE,
                                    ) == PackageManager.PERMISSION_GRANTED
                                ) {
                                    continueAfterCallPermission()
                                } else {
                                    onboardingCallPermission.launch(Manifest.permission.CALL_PHONE)
                                }
                            } else {
                                onboardingPermission.launch(Manifest.permission.READ_CONTACTS)
                            }
                        },
                    )
                }
            } else {
                when (screen) {
                    Screen.HOME -> HomeEntranceTransition(
                        animate = animateHomeEntrance,
                        onAnimationFinished = { animateHomeEntrance = false },
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        HorizontalPager(
                            state = launcherPagerState,
                            modifier = Modifier.fillMaxSize(),
                            userScrollEnabled = !homeMagicExpanded,
                        ) { page ->
                            when (page) {
                                MINK_DAY_PAGE -> MinkDayScreen(
                                    store = store,
                                    isActive = launcherPagerState.currentPage == MINK_DAY_PAGE,
                                    goHome = {
                                        launcherScope.launch { launcherPagerState.animateScrollToPage(HOME_PAGE) }
                                    },
                                )
                                HOME_PAGE -> HomeScreen(
                                    store = store,
                                    actions = actions,
                                    openSettings = { destination ->
                                        settingsDestination = destination
                                        screen = Screen.SETTINGS
                                    },
                                    openTodos = { screen = Screen.TODOS },
                                    openHub = { screen = Screen.HUB },
                                    openMinkDay = {
                                        launcherScope.launch { launcherPagerState.animateScrollToPage(MINK_DAY_PAGE) }
                                    },
                                    minkStatusActive = launcherPagerState.currentPage == HOME_PAGE,
                                    onMagicExpandedChange = { homeMagicExpanded = it },
                                    keyboardInputEnabled = launcherPagerState.currentPage == HOME_PAGE &&
                                        !showTutorial && !showUpdateNotice && !showShortcutSetup,
                                    homeRequestToken = homeRequestToken,
                                )
                                else -> WidgetPage(
                                    store = store,
                                    actions = actions,
                                    goHome = {
                                        launcherScope.launch { launcherPagerState.animateScrollToPage(HOME_PAGE) }
                                    },
                                )
                            }
                        }
                    }
                    Screen.SETTINGS -> SettingsScreen(
                        store = store,
                        actions = actions,
                        requestHomeRole = requestHomeRole,
                        onRepeatTutorial = { tutorialRun++; showTutorial = true },
                        initialDestination = settingsDestination,
                        onIconStyleApplied = {
                            settingsDestination = SettingsDestination.OVERVIEW
                            animateHomeEntrance = true
                            screen = Screen.HOME
                        },
                        goBack = {
                            settingsDestination = SettingsDestination.OVERVIEW
                            screen = Screen.HOME
                        },
                    )
                    Screen.TODOS -> TodosScreen(store, actions) { screen = Screen.HOME }
                    Screen.HUB -> NotificationHubScreen(store, actions) { screen = Screen.HOME }
                }
            }
        }

        if (showShortcutSetup && !showTutorial && !showUpdateNotice) {
            ShortcutSetupDialog(
                store = store,
                actions = actions,
                onFinish = {
                    showShortcutSetup = false
                    requestHomeRole()
                },
            )
        }
        if (showNotificationAccessPrompt && !showTutorial && !showUpdateNotice) {
            NotificationAccessDisclosureDialog(
                onOpenAppInfo = actions::openAppSettings,
                onOpenNotificationAccess = {
                    onboardingNotificationAccess.launch(NotificationHub.accessSettingsIntent())
                },
                onDismiss = {
                    showNotificationAccessPrompt = false
                    finishSpecialAccessSetup()
                },
            )
        }
        if (showUsageAccessPrompt && !showTutorial && !showUpdateNotice) {
            UsageAccessDisclosureDialog(
                onContinue = {
                    showUsageAccessPrompt = false
                    onboardingUsageAccess.launch(usageInsightsRepository.accessSettingsIntent())
                },
                onDismiss = {
                    showUsageAccessPrompt = false
                    finishOnboardingSetup()
                },
            )
        }
        if (showUpdateNotice && !showTutorial) {
            FeatureUpdateDialog(
                onOpenBackup = {
                    store.markUpdateSeen(FEATURE_UPDATE_ID)
                    showUpdateNotice = false
                    settingsDestination = SettingsDestination.BACKUP_RESTORE
                    screen = Screen.SETTINGS
                },
                onReviewTutorial = {
                    store.markUpdateSeen(FEATURE_UPDATE_ID)
                    showUpdateNotice = false
                    tutorialRun++
                    showTutorial = true
                },
                onNotNow = {
                    store.markUpdateSeen(FEATURE_UPDATE_ID)
                    showUpdateNotice = false
                },
            )
        }
    }
}
