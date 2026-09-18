package com.katoaapps.openminilaunch.ui.launcher

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import com.katoaapps.openminilaunch.data.LauncherStore
import com.katoaapps.openminilaunch.platform.DeviceActions
import com.katoaapps.openminilaunch.ui.settings.SettingsDestination
import com.katoaapps.openminilaunch.ui.theme.Dimens
import com.katoaapps.openminilaunch.ui.wellbeing.MinkDayScreen
import com.katoaapps.openminilaunch.ui.widgets.WidgetPage
import kotlinx.coroutines.launch

internal const val MINK_DAY_PAGE = 0
internal const val HOME_PAGE = 1
internal const val WIDGET_PAGE = 2

/** The three launcher pages over one stationary home background. */
@Composable
internal fun LauncherHomePager(
    store: LauncherStore,
    actions: DeviceActions,
    pagerState: PagerState,
    magicBoxExpanded: Boolean,
    keyboardInputEnabled: Boolean,
    homeRequestToken: Int,
    openSettings: (SettingsDestination) -> Unit,
    openTodos: () -> Unit,
    openHub: () -> Unit,
    onMagicBoxExpandedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val scope = rememberCoroutineScope()
    val goHome: () -> Unit = {
        scope.launch { pagerState.animateScrollToPage(HOME_PAGE) }
    }

    Box(modifier) {
        HomeBackground(
            store = store,
            modifier = Modifier.blur(
                if (magicBoxExpanded && pagerState.currentPage == HOME_PAGE) {
                    Dimens.dp10
                } else {
                    Dimens.dp0
                },
            ),
        )
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
                HOME_PAGE -> HomeScreen(
                    store = store,
                    actions = actions,
                    openSettings = openSettings,
                    openTodos = openTodos,
                    openHub = openHub,
                    openMinkDay = {
                        scope.launch { pagerState.animateScrollToPage(MINK_DAY_PAGE) }
                    },
                    minkStatusActive = pagerState.currentPage == HOME_PAGE,
                    onMagicExpandedChange = onMagicBoxExpandedChange,
                    keyboardInputEnabled = keyboardInputEnabled,
                    homeRequestToken = homeRequestToken,
                )
                else -> WidgetPage(store = store, actions = actions, goHome = goHome)
            }
        }
    }
}
