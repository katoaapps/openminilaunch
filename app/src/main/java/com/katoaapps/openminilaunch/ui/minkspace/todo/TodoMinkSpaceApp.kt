package com.katoaapps.openminilaunch.ui.minkspace.todo

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.katoaapps.minkboard.model.BoardId
import com.katoaapps.minkboard.model.CardId
import com.katoaapps.minkboard.model.CardPlacement
import com.katoaapps.minkboard.model.FocusSessionState
import com.katoaapps.minkboard.model.StatusId
import com.katoaapps.openminilaunch.R
import com.katoaapps.openminilaunch.data.LauncherStore
import com.katoaapps.openminilaunch.features.minkspace.MinkSpaceMiniApp
import com.katoaapps.openminilaunch.todoFeatureRuntime
import com.katoaapps.openminilaunch.ui.launcher.home.HomeTodoTextMetrics
import com.katoaapps.openminilaunch.ui.minkspace.core.MinkSpaceApp
import com.katoaapps.openminilaunch.ui.minkspace.core.MinkSpaceMiniAppManifest
import com.katoaapps.openminilaunch.ui.minkspace.core.MinkSpaceMiniAppSettingsTarget
import com.katoaapps.openminilaunch.ui.minkspace.core.MinkSpaceScope
import com.katoaapps.minklauncher.todos.presentation.TodoCardDialogRoute
import kotlinx.coroutines.flow.catch

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
        val runtime = LocalContext.current.todoFeatureRuntime
        val boardAccessRevision by runtime.boardAccessRevision.collectAsState()
        val activeFocusSession by remember(runtime) {
            runtime.minkBoard.focus.observeFocusSession().catch { emit(null) }
        }.collectAsState(initial = null)
        var openCardIdValue by remember { mutableStateOf<String?>(null) }
        var cardRevealSequence by remember { mutableStateOf(0) }
        var cardRevealRequest by remember { mutableStateOf<CompactCardRevealRequest?>(null) }
        val openHomeBoard = {
            runtime.requestHomeBoardNavigation()
            openTodos()
        }
        scope.Surface(modifier = modifier) {
            if (store.demoModeEnabled) {
                TodoPager(
                    items = store.demoTodos,
                    onToggle = store::toggleDemoTodo,
                    openTodos = openHomeBoard,
                    jumpToken = jumpToken,
                    itemsPerPage = itemsPerPage,
                    textMetrics = textMetrics,
                    compact = scope.compact,
                    contentColor = scope.contentColor,
                    mutedContentColor = scope.mutedContentColor,
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                BoardBackedTodoPager(
                    controller = runtime.launcherController,
                    openBoard = openHomeBoard,
                    openStatus = { boardId: BoardId, statusId: StatusId ->
                        runtime.requestNavigation(boardId, statusId = statusId)
                        openTodos()
                    },
                    openCard = { _: BoardId, cardId: CardId ->
                        openCardIdValue = cardId.value
                    },
                    cardRevealRequest = cardRevealRequest,
                    initialPosition = runtime.readCompactHomePosition(),
                    onPositionChanged = runtime::rememberCompactHomePosition,
                    focusedCardId = activeFocusSession
                        ?.takeIf { it.state == FocusSessionState.Running }
                        ?.cardId,
                    onCardRevealFinished = { sequence ->
                        if (cardRevealRequest?.sequence == sequence) {
                            cardRevealRequest = null
                        }
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
        openCardIdValue?.let { cardIdValue ->
            TodoCardDialogRoute(
                runtime = runtime,
                controller = runtime.launcherController,
                cardId = CardId(cardIdValue),
                onDismiss = { openCardIdValue = null },
                onCardPlacementChanged = { placement: CardPlacement ->
                    cardRevealSequence += 1
                    cardRevealRequest = CompactCardRevealRequest(
                        sequence = cardRevealSequence,
                        placement = placement,
                    )
                },
            )
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
