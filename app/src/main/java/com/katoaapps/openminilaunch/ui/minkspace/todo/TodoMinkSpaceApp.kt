package com.katoaapps.openminilaunch.ui.minkspace.todo

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.katoaapps.minkboard.model.BoardId
import com.katoaapps.minkboard.model.CardId
import com.katoaapps.openminilaunch.R
import com.katoaapps.openminilaunch.data.LauncherStore
import com.katoaapps.openminilaunch.features.minkspace.MinkSpaceMiniApp
import com.katoaapps.openminilaunch.openMinkMinkBoardRuntime
import com.katoaapps.openminilaunch.ui.launcher.home.HomeTodoTextMetrics
import com.katoaapps.openminilaunch.ui.minkspace.core.MinkSpaceApp
import com.katoaapps.openminilaunch.ui.minkspace.core.MinkSpaceMiniAppManifest
import com.katoaapps.openminilaunch.ui.minkspace.core.MinkSpaceMiniAppSettingsTarget
import com.katoaapps.openminilaunch.ui.minkspace.core.MinkSpaceScope

/** Todo's complete MinkSpace adapter. The feature only receives platform primitives through [MinkSpaceScope]. */
internal class TodoMinkSpaceApp(
    private val store: LauncherStore,
    private val openTodos: () -> Unit,
    private val jumpToken: Int,
    private val itemsPerPage: Int,
    private val textMetrics: HomeTodoTextMetrics,
) : MinkSpaceApp {
    override val manifest: MinkSpaceMiniAppManifest = Manifest

    @Composable
    override fun Content(scope: MinkSpaceScope, modifier: Modifier) {
        val runtime = LocalContext.current.openMinkMinkBoardRuntime
        val boardAccessRevision by runtime.boardAccessRevision.collectAsState()
        scope.Surface(modifier = modifier) {
            if (store.demoModeEnabled) {
                TodoPager(
                    items = store.demoTodos,
                    onToggle = store::toggleDemoTodo,
                    openTodos = openTodos,
                    jumpToken = jumpToken,
                    itemsPerPage = itemsPerPage,
                    textMetrics = textMetrics,
                    compact = scope.compact,
                    contentColor = scope.contentColor,
                    mutedContentColor = scope.mutedContentColor,
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                MinkBoardTodoPager(
                    controller = runtime.launcherController,
                    openBoard = openTodos,
                    openCard = { boardId: BoardId, cardId: CardId ->
                        runtime.requestNavigation(boardId, cardId)
                    },
                    boardAccessRevision = boardAccessRevision,
                    isBoardAccessGranted = runtime.boardAccessSessions::isGranted,
                    jumpToken = jumpToken,
                    itemsPerPage = itemsPerPage,
                    textMetrics = textMetrics,
                    compact = scope.compact,
                    contentColor = scope.contentColor,
                    mutedContentColor = scope.mutedContentColor,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
    }

    companion object {
        val Manifest = MinkSpaceMiniAppManifest(
            miniApp = MinkSpaceMiniApp.TODO,
            labelRes = R.string.todo_page_title,
            icon = Icons.Default.Checklist,
            settingsTarget = MinkSpaceMiniAppSettingsTarget.TODOS,
        )
    }
}
