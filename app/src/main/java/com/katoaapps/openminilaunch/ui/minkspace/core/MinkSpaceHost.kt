@file:OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)

package com.katoaapps.openminilaunch.ui.minkspace.core

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.katoaapps.openminilaunch.data.LauncherStore
import com.katoaapps.openminilaunch.features.minkspace.MinkSpaceMiniApp
import com.katoaapps.openminilaunch.platform.DeviceActions
import com.katoaapps.openminilaunch.ui.launcher.home.HomeTodoTextMetrics

/** Hosts visible mini-apps, the settings shortcut, and MinkSpace's vertical switching axis. */
@Composable
internal fun MinkSpaceHost(
    store: LauncherStore,
    actions: DeviceActions,
    openTodos: () -> Unit,
    todoJumpToken: Int,
    todoItemsPerPage: Int,
    todoTextMetrics: HomeTodoTextMetrics,
    compact: Boolean,
    contentColor: Color,
    mutedContentColor: Color,
    insetColor: Color,
    openMediaLab: () -> Unit,
    openVCardSettings: () -> Unit,
    openMinkSpaceSettings: () -> Unit,
    presentationState: MinkSpacePresentationState,
    expandedPresentation: Boolean,
    modifier: Modifier = Modifier,
) {
    val dependencies = remember(
        store,
        actions,
        openTodos,
        todoJumpToken,
        todoItemsPerPage,
        todoTextMetrics,
        openMediaLab,
        openVCardSettings,
    ) {
        MinkSpaceHostDependencies(
            store = store,
            actions = actions,
            openTodos = openTodos,
            todoJumpToken = todoJumpToken,
            todoItemsPerPage = todoItemsPerPage,
            todoTextMetrics = todoTextMetrics,
            openMediaLab = openMediaLab,
            openVCardSettings = openVCardSettings,
        )
    }
    val registry = remember(dependencies) {
        MinkSpaceMiniAppCatalog.createRegistry(dependencies)
    }
    val activeMiniApps = store.minkSpaceMiniAppOrder.filter { miniApp ->
        miniApp.readyForHome && miniApp in store.enabledMinkSpaceMiniApps
    }
    val settingsPage = activeMiniApps.size
    val pagerState = rememberPagerState(pageCount = { activeMiniApps.size + 1 })

    BackHandler(enabled = presentationState.isExpanded) {
        presentationState.collapse()
    }

    LaunchedEffect(activeMiniApps) {
        if (pagerState.currentPage > settingsPage) {
            pagerState.scrollToPage(settingsPage)
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
        userScrollEnabled = !presentationState.isExpanded,
        beyondViewportPageCount = 1,
    ) { page ->
        val miniApp = activeMiniApps.getOrNull(page)
        if (miniApp == null) {
            MinkSpaceSettingsShortcutPage(
                compact = compact,
                contentColor = contentColor,
                mutedContentColor = mutedContentColor,
                containerColor = insetColor,
                onOpenSettings = openMinkSpaceSettings,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            key(miniApp.stableId) {
                val app = registry.get(miniApp)
                val expansion = remember(presentationState, app.manifest) {
                    presentationState.expansionController(app.manifest)
                }
                MinkSpaceAppFrame(
                    app = app,
                    scope = MinkSpaceScope(
                        compact = compact,
                        contentColor = contentColor,
                        mutedContentColor = mutedContentColor,
                        containerColor = insetColor,
                        expandedPresentation = expandedPresentation,
                        expansion = expansion,
                    ),
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
    }
}
