@file:OptIn(
    androidx.compose.material3.ExperimentalMaterial3Api::class,
    androidx.compose.foundation.layout.ExperimentalLayoutApi::class,
)

package com.katoaapps.openminilaunch.ui.launcher

import android.app.Activity
import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.katoaapps.openminilaunch.R
import com.katoaapps.openminilaunch.data.LauncherStore
import com.katoaapps.openminilaunch.features.demo.DemoCaptureMoment
import com.katoaapps.openminilaunch.model.ContactAppRoute
import com.katoaapps.openminilaunch.model.Screen
import com.katoaapps.openminilaunch.platform.DeviceActions
import com.katoaapps.openminilaunch.ui.conversations.ConversationCenterScreen
import com.katoaapps.openminilaunch.ui.launcher.home.HomeEntranceTransition
import com.katoaapps.openminilaunch.ui.launcher.home.launcherBackgroundColor
import com.katoaapps.openminilaunch.ui.launcher.layout.HOME_PAGE
import com.katoaapps.openminilaunch.ui.launcher.layout.LauncherHomePager
import com.katoaapps.openminilaunch.ui.launcher.layout.WIDGET_PAGE
import com.katoaapps.openminilaunch.ui.magic.rememberMagicBoxSessionState
import com.katoaapps.openminilaunch.ui.magic.AssistantActivity
import com.katoaapps.openminilaunch.ui.minkspace.core.rememberMinkSpacePresentationState
import com.katoaapps.openminilaunch.ui.minkspace.todo.TodosScreen
import com.katoaapps.openminilaunch.ui.onboarding.FeatureUpdateDialog
import com.katoaapps.openminilaunch.ui.onboarding.GooglePlayBetaDialog
import com.katoaapps.openminilaunch.ui.onboarding.HomeFeatureTour
import com.katoaapps.openminilaunch.ui.onboarding.LocalHomeTourTargetRegistry
import com.katoaapps.openminilaunch.ui.onboarding.OnboardingScreen
import com.katoaapps.openminilaunch.ui.onboarding.shouldShowHomeFeatureTour
import com.katoaapps.openminilaunch.ui.onboarding.rememberHomeTourTargetRegistry
import com.katoaapps.openminilaunch.ui.settings.SettingsDestination
import com.katoaapps.openminilaunch.ui.settings.SettingsScreen
import com.katoaapps.openminilaunch.ui.theme.FontAppearanceTheme
import com.katoaapps.openminilaunch.ui.theme.MinkLauncherTheme
import com.katoaapps.openminilaunch.ui.theme.MinkTransparent
import com.katoaapps.openminilaunch.ui.theme.shouldUseDarkForeground
import com.katoaapps.openminilaunch.ui.profile.openVirtualContactCard
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect

private const val FEATURE_UPDATE_ID = "v320_whats_new_v1"
private const val HOME_FEATURE_TOUR_ID = "home_spotlight_tour_v2"
private const val GOOGLE_PLAY_BETA_NOTICE_ID = "google_play_beta_v1"
private const val PACKAGE_REMOVAL_CONFIRMATION_MS = 750L

@Composable
internal fun MiniLaunchApp(
    store: LauncherStore,
    actions: DeviceActions,
    requestHomeRole: () -> Unit,
    homeRequestToken: Int,
    setSettingsVisibleInRecents: (Boolean) -> Unit,
) {
    var screen by rememberSaveable { mutableStateOf(Screen.HOME) }
    var settingsDestination by rememberSaveable { mutableStateOf(SettingsDestination.OVERVIEW) }
    var showTutorial by rememberSaveable { mutableStateOf(!store.onboardingComplete) }
    var showUpdateNotice by rememberSaveable {
        mutableStateOf(store.onboardingComplete && !store.hasSeenUpdate(FEATURE_UPDATE_ID))
    }
    var showHomeFeatureTour by rememberSaveable {
        mutableStateOf(
            shouldShowHomeFeatureTour(
                onboardingComplete = store.onboardingComplete,
                currentTourSeen = store.hasSeenUpdate(HOME_FEATURE_TOUR_ID),
            ),
        )
    }
    var showGooglePlayBetaNotice by rememberSaveable {
        mutableStateOf(store.onboardingComplete && !store.hasSeenUpdate(GOOGLE_PLAY_BETA_NOTICE_ID))
    }
    var tutorialRun by rememberSaveable { mutableIntStateOf(0) }
    val homeMagicBoxSessionState = rememberMagicBoxSessionState()
    val homeTourTargetRegistry = rememberHomeTourTargetRegistry()
    val minkSpacePresentation = rememberMinkSpacePresentationState()
    var animateHomeEntrance by remember { mutableStateOf(false) }
    var focusedLauncherPage by rememberSaveable { mutableIntStateOf(HOME_PAGE) }
    var twoPanelActive by remember { mutableStateOf(false) }
    var homePageRequestToken by remember { mutableIntStateOf(0) }
    val launcherPagerState = rememberPagerState(initialPage = HOME_PAGE, pageCount = { WIDGET_PAGE + 1 })
    val twoPanelState = rememberLazyListState(initialFirstVisibleItemIndex = 1)
    LaunchedEffect(screen) {
        setSettingsVisibleInRecents(screen == Screen.SETTINGS)
    }
    LaunchedEffect(homeRequestToken) {
        if (homeRequestToken > 0) {
            screen = Screen.HOME
            focusedLauncherPage = HOME_PAGE
            homePageRequestToken++
        }
    }
    LaunchedEffect(actions, store) {
        actions.launcherPackageRemovals.collect { removal ->
            // Package replacement can briefly resemble removal on some OEMs. Confirm the package
            // is truly gone before destroying a user's saved launcher placement.
            delay(PACKAGE_REMOVAL_CONFIRMATION_MS)
            if (!actions.isLauncherPackageInstalled(removal)) {
                val changed = store.pruneLauncherTargets { selectionKey ->
                    actions.resolveLauncherSelection(selectionKey).let { target ->
                        target.packageName == removal.packageName &&
                            target.userSerial == removal.userSerial
                    }
                }
                if (changed) {
                    actions.syncPinnedLauncherShortcuts(store.pinnedLauncherSelectionKeys)
                }
            }
        }
    }
    val context = LocalContext.current
    val view = LocalView.current

    val transparent = MinkTransparent
    val hideHomeStatusBar = store.hideStatusBar && screen == Screen.HOME && !showTutorial

    MinkLauncherTheme(store) {
        val homeVisible = screen == Screen.HOME && !showTutorial
        val statusBarBackground = if (homeVisible) {
            launcherBackgroundColor(store, header = true)
        } else {
            MaterialTheme.colorScheme.background
        }
        val navigationBarBackground = if (homeVisible) {
            launcherBackgroundColor(store)
        } else {
            MaterialTheme.colorScheme.background
        }
        SideEffect {
            val window = (context as Activity).window
            window.statusBarColor = transparent.toArgb()
            window.navigationBarColor = transparent.toArgb()
            WindowInsetsControllerCompat(window, view).apply {
                isAppearanceLightStatusBars = shouldUseDarkForeground(statusBarBackground)
                isAppearanceLightNavigationBars = shouldUseDarkForeground(navigationBarBackground)
                systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                if (hideHomeStatusBar) hide(WindowInsetsCompat.Type.statusBars())
                else show(WindowInsetsCompat.Type.statusBars())
            }
        }
        BackHandler(enabled = !showTutorial && screen != Screen.HOME) { screen = Screen.HOME }
        BackHandler(
            enabled = !showTutorial && screen == Screen.HOME &&
                (if (twoPanelActive) twoPanelState.firstVisibleItemIndex == 0
                    else launcherPagerState.currentPage != HOME_PAGE),
        ) {
            focusedLauncherPage = HOME_PAGE
            homePageRequestToken++
        }
        val imeVisible = WindowInsets.isImeVisible
        BackHandler(
            enabled = !showTutorial && screen == Screen.HOME &&
                (twoPanelActive || launcherPagerState.currentPage == HOME_PAGE) && !imeVisible,
        ) { }

        val revealSystemWallpaper = !showTutorial && screen == Screen.HOME && store.useSystemHomeWallpaper
        Surface(
            Modifier.fillMaxSize(),
            color = if (revealSystemWallpaper) MinkTransparent else MaterialTheme.colorScheme.background,
        ) {
            if (showTutorial) {
                key(tutorialRun) {
                    OnboardingScreen(
                        store = store,
                        actions = actions,
                        requestHomeRole = requestHomeRole,
                        onFinish = {
                            store.completeOnboarding()
                            store.markUpdateSeen(FEATURE_UPDATE_ID)
                            showTutorial = false
                            screen = Screen.HOME
                            focusedLauncherPage = HOME_PAGE
                            homePageRequestToken++
                            showHomeFeatureTour = true
                            if (!actions.isHomeRoleHeld()) requestHomeRole()
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
                        FontAppearanceTheme(store) {
                            CompositionLocalProvider(
                                LocalHomeTourTargetRegistry provides homeTourTargetRegistry,
                            ) {
                                LauncherHomePager(
                                    store = store,
                                    actions = actions,
                                    pagerState = launcherPagerState,
                                    twoPanelState = twoPanelState,
                                    focusedPage = focusedLauncherPage,
                                    onFocusedPageChange = { focusedLauncherPage = it },
                                    onTwoPanelActiveChange = { twoPanelActive = it },
                                    homePageRequestToken = homePageRequestToken,
                                    keyboardInputEnabled =
                                        (twoPanelActive || launcherPagerState.currentPage == HOME_PAGE) &&
                                            !showTutorial && !showUpdateNotice &&
                                            !showHomeFeatureTour &&
                                            !showGooglePlayBetaNotice,
                                    homeRequestToken = homeRequestToken,
                                    magicBoxSessionState = homeMagicBoxSessionState,
                                    minkSpacePresentation = minkSpacePresentation,
                                    openSettings = { destination ->
                                        settingsDestination = destination
                                        screen = Screen.SETTINGS
                                    },
                                    openTodos = { screen = Screen.TODOS },
                                    openVCardSettings = {
                                        openVirtualContactCard(context)
                                    },
                                    openConversations = { screen = Screen.CONVERSATIONS },
                                    modifier = Modifier.fillMaxSize(),
                                )
                            }
                        }
                    }
                    Screen.SETTINGS -> FontAppearanceTheme(store, applyFontColor = true) {
                        SettingsScreen(
                            store = store,
                            actions = actions,
                            requestHomeRole = requestHomeRole,
                            onRepeatTutorial = { tutorialRun++; showTutorial = true },
                            onReplayHomeTour = {
                                settingsDestination = SettingsDestination.OVERVIEW
                                focusedLauncherPage = HOME_PAGE
                                homePageRequestToken++
                                screen = Screen.HOME
                                showHomeFeatureTour = true
                            },
                            initialDestination = settingsDestination,
                            onIconStyleApplied = {
                                settingsDestination = SettingsDestination.OVERVIEW
                                animateHomeEntrance = true
                                screen = Screen.HOME
                            },
                            openTodos = { screen = Screen.TODOS },
                            onDemoCaptureMoment = { moment ->
                                settingsDestination = SettingsDestination.OVERVIEW
                                when (moment) {
                                    DemoCaptureMoment.HOME -> {
                                        homeMagicBoxSessionState.resetForDemoHome()
                                        focusedLauncherPage = HOME_PAGE
                                        homePageRequestToken++
                                        screen = Screen.HOME
                                    }
                                    DemoCaptureMoment.QUICK_APPS -> {
                                        context.startActivity(Intent(context, AssistantActivity::class.java))
                                    }
                                    DemoCaptureMoment.CONVERSATIONS -> {
                                        screen = Screen.CONVERSATIONS
                                    }
                                    DemoCaptureMoment.MAGIC_BOX_SEARCH -> {
                                        homeMagicBoxSessionState.prepareDemoSearch("invoice")
                                        focusedLauncherPage = HOME_PAGE
                                        homePageRequestToken++
                                        screen = Screen.HOME
                                    }
                                }
                            },
                            goBack = {
                                settingsDestination = SettingsDestination.OVERVIEW
                                screen = Screen.HOME
                            },
                        )
                    }
                    Screen.TODOS -> TodosScreen(store, actions) { screen = Screen.HOME }
                    Screen.CONVERSATIONS -> FontAppearanceTheme(store) {
                        ConversationCenterScreen(
                            store = store,
                            actions = actions,
                            onComposeMessage = { recipient, route ->
                                minkSpacePresentation.collapse()
                                val providerPackage = (route as? ContactAppRoute.Package)?.packageName
                                homeMagicBoxSessionState.prepareMessage(
                                    recipient = recipient,
                                    providerPackage = providerPackage,
                                    providerRouteOverridden = route != ContactAppRoute.Inherit,
                                )
                                focusedLauncherPage = HOME_PAGE
                                homePageRequestToken++
                                screen = Screen.HOME
                            },
                            onComposeConversationMessage = { conversation ->
                                minkSpacePresentation.collapse()
                                homeMagicBoxSessionState.prepareConversationMessage(conversation)
                                focusedLauncherPage = HOME_PAGE
                                homePageRequestToken++
                                screen = Screen.HOME
                            },
                            goBack = { screen = Screen.HOME },
                        )
                    }
                }
            }
        }

        if (showUpdateNotice && !showTutorial) {
            FeatureUpdateDialog(
                onOpenSettings = {
                    store.markUpdateSeen(FEATURE_UPDATE_ID)
                    showUpdateNotice = false
                    screen = Screen.SETTINGS
                    settingsDestination = SettingsDestination.OVERVIEW
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
        if (showHomeFeatureTour && !showTutorial && !showUpdateNotice) {
            HomeFeatureTour(
                registry = homeTourTargetRegistry,
                onComplete = {
                    store.markUpdateSeen(HOME_FEATURE_TOUR_ID)
                    showHomeFeatureTour = false
                },
            )
        }
        if (
            showGooglePlayBetaNotice && !showTutorial && !showUpdateNotice &&
            !showHomeFeatureTour
        ) {
            GooglePlayBetaDialog(
                onOpenBeta = {
                    if (actions.openGooglePlayBetaPage()) {
                        store.markUpdateSeen(GOOGLE_PLAY_BETA_NOTICE_ID)
                        showGooglePlayBetaNotice = false
                    } else {
                        Toast.makeText(context, R.string.no_browser_available, Toast.LENGTH_SHORT).show()
                    }
                },
                onDismiss = {
                    store.markUpdateSeen(GOOGLE_PLAY_BETA_NOTICE_ID)
                    showGooglePlayBetaNotice = false
                },
            )
        }
    }
}
