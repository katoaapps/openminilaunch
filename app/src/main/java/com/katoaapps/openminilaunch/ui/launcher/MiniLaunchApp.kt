@file:OptIn(
    androidx.compose.material3.ExperimentalMaterial3Api::class,
    androidx.compose.foundation.layout.ExperimentalLayoutApi::class,
)

package com.katoaapps.openminilaunch.ui.launcher

import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
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
import com.katoaapps.openminilaunch.data.LauncherStore
import com.katoaapps.openminilaunch.model.ContactAppRoute
import com.katoaapps.openminilaunch.model.Screen
import com.katoaapps.openminilaunch.platform.DeviceActions
import com.katoaapps.openminilaunch.ui.conversations.ConversationCenterScreen
import com.katoaapps.openminilaunch.ui.launcher.home.HomeEntranceTransition
import com.katoaapps.openminilaunch.ui.launcher.layout.HOME_PAGE
import com.katoaapps.openminilaunch.ui.launcher.layout.LauncherHomePager
import com.katoaapps.openminilaunch.ui.launcher.layout.WIDGET_PAGE
import com.katoaapps.openminilaunch.ui.magic.rememberMagicBoxSessionState
import com.katoaapps.openminilaunch.ui.onboarding.FeatureUpdateDialog
import com.katoaapps.openminilaunch.ui.onboarding.HomeFeatureTour
import com.katoaapps.openminilaunch.ui.onboarding.OnboardingScreen
import com.katoaapps.openminilaunch.ui.settings.SettingsDestination
import com.katoaapps.openminilaunch.ui.settings.SettingsScreen
import com.katoaapps.openminilaunch.ui.theme.FontAppearanceTheme
import com.katoaapps.openminilaunch.ui.theme.MinkLauncherTheme
import com.katoaapps.openminilaunch.ui.theme.MinkTransparent
import com.katoaapps.openminilaunch.ui.theme.minkDarkTheme
import com.katoaapps.openminilaunch.ui.minkspace.todo.TodosScreen
import com.katoaapps.openminilaunch.ui.profile.openVirtualContactCard

private const val FEATURE_UPDATE_ID = "v310_whats_new_v1"
private const val HOME_FEATURE_TOUR_ID = "home_feature_tour_v2"

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
        mutableStateOf(store.onboardingComplete && !store.hasSeenUpdate(HOME_FEATURE_TOUR_ID))
    }
    var tutorialRun by rememberSaveable { mutableIntStateOf(0) }
    val homeMagicBoxSessionState = rememberMagicBoxSessionState()
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
    val context = LocalContext.current
    val darkTheme = minkDarkTheme(store)
    val view = LocalView.current

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

    MinkLauncherTheme(store) {
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
                                        !showTutorial && !showUpdateNotice && !showHomeFeatureTour,
                                homeRequestToken = homeRequestToken,
                                magicBoxSessionState = homeMagicBoxSessionState,
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
                    Screen.SETTINGS -> FontAppearanceTheme(store, applyFontColor = true) {
                        SettingsScreen(
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
                            openTodos = { screen = Screen.TODOS },
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
                onOpenMinkSpaceSettings = {
                    store.markUpdateSeen(FEATURE_UPDATE_ID)
                    showUpdateNotice = false
                    screen = Screen.SETTINGS
                    settingsDestination = SettingsDestination.MINK_SPACE
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
                onDismiss = {
                    store.markUpdateSeen(HOME_FEATURE_TOUR_ID)
                    showHomeFeatureTour = false
                },
            )
        }
    }
}
