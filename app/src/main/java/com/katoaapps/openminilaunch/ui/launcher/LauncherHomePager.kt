package com.katoaapps.openminilaunch.ui.launcher

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.runtime.Composable
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
import com.katoaapps.openminilaunch.data.LauncherStore
import com.katoaapps.openminilaunch.platform.DeviceActions
import com.katoaapps.openminilaunch.ui.magic.MagicBoxSessionState
import com.katoaapps.openminilaunch.ui.settings.SettingsDestination
import com.katoaapps.openminilaunch.ui.theme.Dimens
import com.katoaapps.openminilaunch.ui.wellbeing.MinkDayScreen
import com.katoaapps.openminilaunch.ui.widgets.WidgetPage
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
    onTwoPanelActiveChange: (Boolean) -> Unit,
    homePageRequestToken: Int,
    keyboardInputEnabled: Boolean,
    homeRequestToken: Int,
    magicBoxSessionState: MagicBoxSessionState,
    openSettings: (SettingsDestination) -> Unit,
    openTodos: () -> Unit,
    openHub: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scope = rememberCoroutineScope()
    val configuration = LocalConfiguration.current
    val density = LocalDensity.current
    val fold = currentFoldLayoutFeature()
    val homeStateHolder = rememberSaveableStateHolder()
    var settledPair by remember { mutableIntStateOf(1) }
    var initialized by remember { mutableStateOf(false) }
    var previousModeWasTwoPanel by remember { mutableStateOf(false) }
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
        val twoPanelActive = geometry != null

        LaunchedEffect(geometry) {
            if (twoPanelActive) {
                val pair = if (initialized && previousModeWasTwoPanel) settledPair
                    else pairForFocusedPage(focusedPage)
                settledPair = pair
                twoPanelState.scrollToItem(pair)
            } else if (!initialized || previousModeWasTwoPanel) {
                pagerState.scrollToPage(focusedPage)
            }
            previousModeWasTwoPanel = twoPanelActive
            initialized = true
            onTwoPanelActiveChange(twoPanelActive)
        }

        LaunchedEffect(pagerState.settledPage) {
            if (!twoPanelActive) onFocusedPageChange(pagerState.settledPage)
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
                    onFocusedPageChange(newlyRevealedPage(pair))
                }
            }
        }

        LaunchedEffect(homePageRequestToken) {
            if (homePageRequestToken > 0) {
                onFocusedPageChange(HOME_PAGE)
                if (twoPanelActive) {
                    settledPair = 1
                    twoPanelState.animateScrollToItem(1)
                } else {
                    pagerState.animateScrollToPage(HOME_PAGE)
                }
            }
        }

        val goHome: () -> Unit = {
            onFocusedPageChange(HOME_PAGE)
            scope.launch {
                if (twoPanelActive) {
                    settledPair = 1
                    twoPanelState.animateScrollToItem(1)
                } else {
                    pagerState.animateScrollToPage(HOME_PAGE)
                }
            }
        }
        val goToMinkDay: () -> Unit = {
            onFocusedPageChange(MINK_DAY_PAGE)
            scope.launch {
                if (twoPanelActive) {
                    settledPair = 0
                    twoPanelState.animateScrollToItem(0)
                } else {
                    pagerState.animateScrollToPage(MINK_DAY_PAGE)
                }
            }
        }

        HomeBackground(
            store = store,
            modifier = Modifier.blur(
                if (magicBoxExpanded && (twoPanelActive || pagerState.currentPage == HOME_PAGE)) {
                    Dimens.dp10
                } else {
                    Dimens.dp0
                },
            ),
        )

        if (geometry != null) {
            LauncherTwoPanel(
                store = store,
                actions = actions,
                geometry = geometry,
                viewportWidthPx = constraints.maxWidth,
                listState = twoPanelState,
                homeStateHolder = homeStateHolder,
                magicBoxExpanded = magicBoxExpanded,
                homeRequestToken = homeRequestToken,
                magicBoxSessionState = magicBoxSessionState,
                onFocusPage = onFocusedPageChange,
                onMinkDay = goToMinkDay,
                onGoHome = goHome,
                openSettings = openSettings,
                openTodos = openTodos,
                openHub = openHub,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize(),
                userScrollEnabled = !magicBoxExpanded,
            ) { page ->
                when (page) {
                    MINK_DAY_PAGE -> MinkDayScreen(
                        store = store,
                        isActive = pagerState.currentPage == MINK_DAY_PAGE,
                        goHome = goHome,
                    )
                    HOME_PAGE -> homeStateHolder.SaveableStateProvider("home") {
                        HomeScreen(
                            store = store,
                            actions = actions,
                            openSettings = openSettings,
                            openTodos = openTodos,
                            openHub = openHub,
                            openMinkDay = goToMinkDay,
                            minkStatusActive = pagerState.currentPage == HOME_PAGE,
                            onMagicExpandedChange = {},
                            keyboardInputEnabled = keyboardInputEnabled,
                            homeRequestToken = homeRequestToken,
                            magicBoxSessionState = magicBoxSessionState,
                        )
                    }
                    else -> WidgetPage(store = store, actions = actions, goHome = goHome)
                }
            }
        }
    }
}
