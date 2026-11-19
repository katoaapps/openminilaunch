package com.katoaapps.openminilaunch.ui.launcher.layout

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import com.katoaapps.openminilaunch.data.LauncherStore
import com.katoaapps.openminilaunch.model.CommunicationRecipient
import com.katoaapps.openminilaunch.model.ContactAppRoute
import com.katoaapps.openminilaunch.model.HomeLayoutStyle
import com.katoaapps.openminilaunch.model.LauncherShortcutTarget
import com.katoaapps.openminilaunch.platform.DeviceActions
import com.katoaapps.openminilaunch.ui.launcher.home.HomeScreen
import com.katoaapps.openminilaunch.ui.launcher.home.HomeBackground
import com.katoaapps.openminilaunch.ui.magic.MagicBoxSessionState
import com.katoaapps.openminilaunch.ui.minkspace.core.MinkSpaceMiniAppCatalog
import com.katoaapps.openminilaunch.ui.minkspace.core.MinkSpacePresentationState
import com.katoaapps.openminilaunch.ui.minkspace.core.MinkSpaceTotalScreenDependencies
import com.katoaapps.openminilaunch.ui.minkspace.core.MinkSpaceTotalScreenHost
import com.katoaapps.openminilaunch.ui.settings.SettingsDestination
import com.katoaapps.openminilaunch.ui.theme.Dimens
import com.katoaapps.openminilaunch.ui.wellbeing.MinkDayScreen
import com.katoaapps.openminilaunch.ui.widgets.WidgetPage
import com.katoaapps.openminilaunch.ui.widgets.rememberWidgetPagerDragHandler
import kotlinx.coroutines.launch

internal const val MINK_DAY_PAGE = 0
internal const val HOME_PAGE = 1
internal const val WIDGET_PAGE = 2

/** Selects the single-page pager or the continuous two-panel launcher. */
@Composable
internal fun LauncherHomePager(
    store: LauncherStore,
    actions: DeviceActions,
    pagerState: PagerState,
    twoPanelState: LazyListState,
    focusedPage: Int,
    onFocusedPageChange: (Int) -> Unit,
    homePageRequestToken: Int,
    keyboardInputEnabled: Boolean,
    homeRequestToken: Int,
    magicBoxSessionState: MagicBoxSessionState,
    minkSpacePresentation: MinkSpacePresentationState,
    openSettings: (SettingsDestination) -> Unit,
    openTodos: () -> Unit,
    openVCardSettings: () -> Unit,
    openConversations: () -> Unit,
    openMinkDay: () -> Unit,
    onComposeMessage: (CommunicationRecipient, ContactAppRoute) -> Unit,
    onComposeConversationMessage: (LauncherShortcutTarget) -> Unit,
    modifier: Modifier = Modifier,
) {
    val scope = rememberCoroutineScope()
    val configuration = LocalConfiguration.current
    val density = LocalDensity.current
    val contentLayoutDirection = LocalLayoutDirection.current
    val fold = currentFoldLayoutFeature()
    val homeStateHolder = rememberSaveableStateHolder()
    var settledPair by remember { mutableIntStateOf(pairForFocusedPage(HOME_PAGE, store.showMinkDayTab)) }
    var initialized by remember { mutableStateOf(false) }
    var previousModeWasTwoPanel by remember { mutableStateOf(false) }
    var minkSpaceExpanded by remember { mutableStateOf(false) }
    val magicBoxExpanded = magicBoxSessionState.expanded

    LaunchedEffect(fold, configuration.smallestScreenWidthDp) {
        store.maybeEnableTwoPanelForUnfoldedBookDisplay(
            unfoldedBookDisplay = fold?.vertical == true &&
                configuration.smallestScreenWidthDp >= 600,
        )
    }

    BoxWithConstraints(modifier) {
        val geometry = twoPanelGeometry(
            enabled = store.twoPanelModeForLargeDisplays,
            smallestWidthDp = configuration.smallestScreenWidthDp,
            orientation = configuration.orientation,
            windowWidthPx = constraints.maxWidth,
            density = density.density,
            fold = fold,
        )
        val classicLayout = store.homeLayoutStyle == HomeLayoutStyle.CLASSIC
        val twoPanelActive = geometry != null && !classicLayout

        LaunchedEffect(classicLayout) {
            if (classicLayout) onFocusedPageChange(HOME_PAGE)
        }

        LaunchedEffect(geometry, store.showMinkDayTab) {
            if (twoPanelActive) {
                val pair = if (initialized && previousModeWasTwoPanel) settledPair
                    else pairForFocusedPage(focusedPage, store.showMinkDayTab)
                settledPair = pair
                twoPanelState.scrollToItem(pair)
            } else if (!initialized || previousModeWasTwoPanel) {
                pagerState.scrollToPage(
                    launcherPagerIndex(focusedPage, store.showMinkDayTab),
                )
            }
            previousModeWasTwoPanel = twoPanelActive
            initialized = true
        }

        LaunchedEffect(pagerState.settledPage, store.showMinkDayTab) {
            if (!twoPanelActive) {
                onFocusedPageChange(
                    launcherPageAt(pagerState.settledPage, store.showMinkDayTab),
                )
            }
        }

        LaunchedEffect(
            twoPanelActive,
            twoPanelState.isScrollInProgress,
            twoPanelState.firstVisibleItemIndex,
        ) {
            if (twoPanelActive && initialized && !twoPanelState.isScrollInProgress) {
                val pair = twoPanelState.firstVisibleItemIndex.coerceIn(0, 1)
                if (pair != settledPair) {
                    settledPair = pair
                    onFocusedPageChange(newlyRevealedPage(pair, store.showMinkDayTab))
                }
            }
        }

        LaunchedEffect(homePageRequestToken) {
            if (homePageRequestToken > 0) {
                onFocusedPageChange(HOME_PAGE)
                // A back/host request can arrive while the pager is still settling an edge swipe.
                // Jumping cancels that in-flight motion; starting a second animation makes RTL
                // layouts visibly oscillate between logical and physical directions.
                if (twoPanelActive) {
                    settledPair = pairForFocusedPage(HOME_PAGE, store.showMinkDayTab)
                    twoPanelState.scrollToItem(settledPair)
                } else {
                    pagerState.scrollToPage(launcherPagerIndex(HOME_PAGE, store.showMinkDayTab))
                }
            }
        }

        val goHome: () -> Unit = {
            onFocusedPageChange(HOME_PAGE)
            scope.launch {
                if (twoPanelActive) {
                    settledPair = pairForFocusedPage(HOME_PAGE, store.showMinkDayTab)
                    twoPanelState.animateScrollToItem(settledPair)
                } else {
                    pagerState.animateScrollToPage(
                        launcherPagerIndex(HOME_PAGE, store.showMinkDayTab),
                    )
                }
            }
        }
        val goToMinkDay: () -> Unit = {
            if (classicLayout || !store.showMinkDayTab) {
                openMinkDay()
            } else {
                onFocusedPageChange(MINK_DAY_PAGE)
                scope.launch {
                    if (twoPanelActive) {
                        settledPair = 0
                        twoPanelState.animateScrollToItem(0)
                    } else {
                        pagerState.animateScrollToPage(
                            launcherPagerIndex(MINK_DAY_PAGE, showMinkDayTab = true),
                        )
                    }
                }
            }
        }

        HomeBackground(
            store = store,
            modifier = Modifier.blur(
                if (magicBoxExpanded && focusedPage == HOME_PAGE) {
                    Dimens.dp10
                } else {
                    Dimens.dp0
                },
            ),
        )

        if (classicLayout) {
            HomeScreen(
                store = store,
                actions = actions,
                openSettings = openSettings,
                openTodos = openTodos,
                openVCardSettings = openVCardSettings,
                openConversations = openConversations,
                openMinkDay = goToMinkDay,
                onComposeMessage = onComposeMessage,
                onComposeConversationMessage = onComposeConversationMessage,
                minkStatusActive = true,
                onMagicExpandedChange = {},
                onMinkSpaceExpandedChange = { minkSpaceExpanded = it },
                keyboardInputEnabled = keyboardInputEnabled,
                homeRequestToken = homeRequestToken,
                magicBoxSessionState = magicBoxSessionState,
                minkSpacePresentation = minkSpacePresentation,
            )
        } else if (geometry != null) {
            LauncherTwoPanel(
                store = store,
                actions = actions,
                geometry = geometry,
                viewportWidthPx = constraints.maxWidth,
                listState = twoPanelState,
                homeStateHolder = homeStateHolder,
                magicBoxExpanded = magicBoxExpanded,
                minkSpaceExpanded = minkSpaceExpanded,
                homeRequestToken = homeRequestToken,
                magicBoxSessionState = magicBoxSessionState,
                minkSpacePresentation = minkSpacePresentation,
                onFocusPage = onFocusedPageChange,
                onMinkDay = goToMinkDay,
                onGoHome = goHome,
                openSettings = openSettings,
                openTodos = openTodos,
                openVCardSettings = openVCardSettings,
                openConversations = openConversations,
                showMinkDayTab = store.showMinkDayTab,
                onMinkSpaceExpandedChange = { minkSpaceExpanded = it },
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            // These pages are fixed physical surfaces rather than reading-order content. Keeping
            // pager physics LTR avoids RTL settle oscillation while each page still renders using
            // the locale's real layout direction.
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxSize(),
                    userScrollEnabled = !magicBoxExpanded && !minkSpaceExpanded,
                ) { pageIndex ->
                    val page = launcherPageAt(pageIndex, store.showMinkDayTab)
                    CompositionLocalProvider(LocalLayoutDirection provides contentLayoutDirection) {
                        when (page) {
                            MINK_DAY_PAGE -> MinkDayScreen(
                                store = store,
                                isActive = launcherPageAt(
                                    pagerState.currentPage,
                                    store.showMinkDayTab,
                                ) == MINK_DAY_PAGE,
                                goHome = goHome,
                            )
                            HOME_PAGE -> homeStateHolder.SaveableStateProvider("home") {
                                HomeScreen(
                                    store = store,
                                    actions = actions,
                                    openSettings = openSettings,
                                    openTodos = openTodos,
                                    openVCardSettings = openVCardSettings,
                                    openConversations = openConversations,
                                    openMinkDay = goToMinkDay,
                                    onComposeMessage = onComposeMessage,
                                    onComposeConversationMessage = onComposeConversationMessage,
                                    minkStatusActive = launcherPageAt(
                                        pagerState.currentPage,
                                        store.showMinkDayTab,
                                    ) == HOME_PAGE,
                                    onMagicExpandedChange = {},
                                    onMinkSpaceExpandedChange = { minkSpaceExpanded = it },
                                    keyboardInputEnabled = keyboardInputEnabled,
                                    homeRequestToken = homeRequestToken,
                                    magicBoxSessionState = magicBoxSessionState,
                                    minkSpacePresentation = minkSpacePresentation,
                                )
                            }
                            else -> WidgetPage(
                                store = store,
                                actions = actions,
                                goHome = goHome,
                                horizontalDragHandler = rememberWidgetPagerDragHandler(pagerState),
                            )
                        }
                    }
                }
            }
        }

        minkSpacePresentation.totalScreenMiniApp?.let { miniApp ->
            val dependencies = remember(actions) {
                MinkSpaceTotalScreenDependencies(openMediaLab = actions::openMediaLab)
            }
            val totalScreenApp = remember(miniApp, dependencies) {
                MinkSpaceMiniAppCatalog.createTotalScreenApp(miniApp, dependencies)
            }
            if (totalScreenApp == null) {
                LaunchedEffect(miniApp) { minkSpacePresentation.collapse(miniApp) }
            } else {
                MinkSpaceTotalScreenHost(
                    store = store,
                    app = totalScreenApp,
                    onDismiss = { minkSpacePresentation.collapse(miniApp) },
                    onOpenConversations = openConversations,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
    }
}
