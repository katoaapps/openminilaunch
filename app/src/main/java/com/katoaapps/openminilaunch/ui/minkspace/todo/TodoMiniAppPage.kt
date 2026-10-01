package com.katoaapps.openminilaunch.ui.minkspace.todo

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.katoaapps.openminilaunch.data.LauncherStore
import com.katoaapps.openminilaunch.ui.launcher.home.HomeTodoTextMetrics
import com.katoaapps.openminilaunch.ui.minkspace.core.MinkSpaceMiniAppSurface

/** Home-facing entry point for the required Todo mini-app. */
@Composable
internal fun TodoMiniAppPage(
    store: LauncherStore,
    openTodos: () -> Unit,
    jumpToken: Int,
    itemsPerPage: Int,
    textMetrics: HomeTodoTextMetrics,
    compact: Boolean,
    contentColor: Color,
    mutedContentColor: Color,
    containerColor: Color,
    modifier: Modifier = Modifier,
) {
    MinkSpaceMiniAppSurface(
        compact = compact,
        containerColor = containerColor,
        contentColor = contentColor,
        modifier = modifier,
    ) {
        TodoPager(
            store = store,
            openTodos = openTodos,
            jumpToken = jumpToken,
            itemsPerPage = itemsPerPage,
            textMetrics = textMetrics,
            compact = compact,
            contentColor = contentColor,
            mutedContentColor = mutedContentColor,
            modifier = Modifier.fillMaxSize(),
        )
    }
}
