@file:OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)

package com.katoaapps.openminilaunch.ui.minkspace.core

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.katoaapps.openminilaunch.data.LauncherStore
import com.katoaapps.openminilaunch.features.minkspace.MinkSpaceMiniApp
import com.katoaapps.openminilaunch.ui.launcher.home.HomeTodoTextMetrics
import com.katoaapps.openminilaunch.ui.minkspace.calculator.CalculatorMiniAppPage
import com.katoaapps.openminilaunch.ui.minkspace.media.MediaMiniAppPage
import com.katoaapps.openminilaunch.ui.minkspace.music.MusicMiniAppPage
import com.katoaapps.openminilaunch.ui.minkspace.todo.TodoMiniAppPage

/** Hosts enabled mini-apps and owns only the vertical switching axis inside MinkSpace. */
@Composable
internal fun MinkSpaceHost(
    store: LauncherStore,
    openTodos: () -> Unit,
    todoJumpToken: Int,
    todoItemsPerPage: Int,
    todoTextMetrics: HomeTodoTextMetrics,
    compact: Boolean,
    contentColor: Color,
    mutedContentColor: Color,
    insetColor: Color,
    openMediaLab: () -> Unit,
    presentationState: MinkSpacePresentationState,
    modifier: Modifier = Modifier,
) {
    val activeMiniApps = store.minkSpaceMiniAppOrder.filter { miniApp ->
        miniApp.readyForHome && miniApp in store.enabledMinkSpaceMiniApps
    }
    val pagerState = rememberPagerState(pageCount = { activeMiniApps.size })

    LaunchedEffect(activeMiniApps) {
        if (pagerState.currentPage > activeMiniApps.lastIndex) {
            pagerState.scrollToPage(activeMiniApps.lastIndex.coerceAtLeast(0))
        }
    }
    LaunchedEffect(todoJumpToken, activeMiniApps) {
        if (todoJumpToken > 0) {
            val todoPage = activeMiniApps.indexOf(MinkSpaceMiniApp.TODO)
            if (todoPage >= 0 && pagerState.currentPage != todoPage) {
                pagerState.animateScrollToPage(todoPage)
            }
        }
    }
    LaunchedEffect(pagerState.currentPage, activeMiniApps, presentationState.expandedMiniApp) {
        val currentMiniApp = activeMiniApps.getOrNull(pagerState.currentPage)
        presentationState.retainOnly(currentMiniApp)
    }

    VerticalPager(
        state = pagerState,
        modifier = modifier,
        userScrollEnabled = activeMiniApps.size > 1 && !presentationState.isExpanded,
        beyondViewportPageCount = 1,
    ) { page ->
        when (activeMiniApps[page]) {
            MinkSpaceMiniApp.TODO -> TodoMiniAppPage(
                store = store,
                openTodos = openTodos,
                jumpToken = todoJumpToken,
                itemsPerPage = todoItemsPerPage,
                textMetrics = todoTextMetrics,
                compact = compact,
                contentColor = contentColor,
                mutedContentColor = mutedContentColor,
                containerColor = insetColor,
                modifier = Modifier.fillMaxSize(),
            )
            MinkSpaceMiniApp.CALCULATOR -> CalculatorMiniAppPage(
                compact = compact,
                contentColor = contentColor,
                mutedContentColor = mutedContentColor,
                containerColor = insetColor,
                modifier = Modifier.fillMaxSize(),
            )
            MinkSpaceMiniApp.MEDIA -> MediaMiniAppPage(
                compact = compact,
                contentColor = contentColor,
                mutedContentColor = mutedContentColor,
                containerColor = insetColor,
                onManageMedia = openMediaLab,
                modifier = Modifier.fillMaxSize(),
            )
            MinkSpaceMiniApp.MUSIC -> MusicMiniAppPage(
                compact = compact,
                containerColor = insetColor,
                presentationState = presentationState,
                modifier = Modifier.fillMaxSize(),
            )
            MinkSpaceMiniApp.RECORDER -> Unit
        }
    }
}
