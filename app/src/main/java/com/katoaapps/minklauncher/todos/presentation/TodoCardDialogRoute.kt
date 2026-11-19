package com.katoaapps.minklauncher.todos.presentation

import com.katoaapps.minklauncher.todos.home.TodoCardController
import com.katoaapps.minklauncher.todos.home.TodoController
import com.katoaapps.minklauncher.todos.runtime.TodoFeatureRuntime

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import com.katoaapps.minkboard.core.LauncherBoardState
import com.katoaapps.minkboard.model.BoardId
import com.katoaapps.minkboard.model.CardId
import com.katoaapps.minkboard.model.CardPlacement
import com.katoaapps.minkboard.model.FocusTimerId
import com.katoaapps.minkboard.ui.MinkBoardCardActions
import com.katoaapps.minkboard.ui.MinkBoardCardDialogHost
import com.katoaapps.minkboard.ui.MinkBoardEditorState
import com.katoaapps.minkboard.ui.MinkBoardTheme
import com.katoaapps.minkboard.focus.android.MinkBoardFocusSchedulePrecision
import com.katoaapps.minkboard.focus.android.StartMinkBoardFocusResult
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

private data class CompactPendingFocusRequest(
    val boardId: BoardId,
    val cardId: CardId,
    val durationMillis: Long,
    val replace: Boolean,
)

/** Opens the canonical MinkBoard card workflow without navigating Home into the board surface. */
@Composable
internal fun TodoCardDialogRoute(
    runtime: TodoFeatureRuntime,
    controller: TodoController,
    cardId: CardId,
    onDismiss: () -> Unit,
    onCardPlacementChanged: (CardPlacement) -> Unit,
) {
    val context = LocalContext.current
    val cardActionController = remember(runtime) {
        TodoCardController(
            cards = runtime.minkBoard.cards,
            subtasks = runtime.minkBoard.subtasks,
            focusTimerCoordinator = runtime.focusTimerCoordinator,
        )
    }
    val scope = rememberCoroutineScope()
    val boardState by controller.boardState.collectAsState()
    val activeFocusSession by remember(runtime) {
        runtime.minkBoard.focus.observeFocusSession().catch { emit(null) }
    }.collectAsState(initial = null)
    var editorState by remember(cardId.value) {
        mutableStateOf<MinkBoardEditorState>(MinkBoardEditorState.None)
    }
    var actionError by remember { mutableStateOf<String?>(null) }
    var pendingFocusRequest by remember { mutableStateOf<CompactPendingFocusRequest?>(null) }

    val ready = boardState as? LauncherBoardState.Ready
    val board = ready?.board
    val cardExists = board?.cards?.any { it.id == cardId } == true

    LaunchedEffect(cardExists) {
        if (board != null && !cardExists) onDismiss()
    }
    LaunchedEffect(actionError) {
        actionError?.let { message ->
            Toast.makeText(context, message, Toast.LENGTH_LONG).show()
            actionError = null
        }
    }

    fun mutate(onSuccess: () -> Unit = {}, block: suspend () -> Unit) {
        scope.launch {
            runCatching { block() }
                .onSuccess { onSuccess() }
                .onFailure {
                    actionError = context.todoBoardErrorMessage(it, runtime.resources)
                }
        }
    }

    fun <T> mutateResult(onSuccess: (T) -> Unit, block: suspend () -> T) {
        scope.launch {
            runCatching { block() }
                .onSuccess(onSuccess)
                .onFailure {
                    actionError = context.todoBoardErrorMessage(it, runtime.resources)
                }
        }
    }

    fun startFocus(request: CompactPendingFocusRequest) {
        mutate {
            when (
                val result = cardActionController.startFocus(
                    request.boardId,
                    request.cardId,
                    request.durationMillis,
                    request.replace,
                )
            ) {
                is StartMinkBoardFocusResult.ActiveTimerExists -> {
                    actionError = context.getString(runtime.resources.focusTimerAlreadyRunning)
                }
                is StartMinkBoardFocusResult.Started -> {
                    if (result.precision == MinkBoardFocusSchedulePrecision.Inexact) {
                        actionError = context.getString(runtime.resources.focusTimerInexact)
                    }
                }
            }
        }
    }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        val request = pendingFocusRequest.also { pendingFocusRequest = null }
        if (granted && request != null) {
            startFocus(request)
        } else if (!granted) {
            actionError = context.getString(runtime.resources.notificationPermissionRequired)
        }
    }

    if (board == null || !cardExists) return

    val actions = MinkBoardCardActions(
        closeCard = {
            editorState = MinkBoardEditorState.None
            onDismiss()
        },
        cloneCard = { sourceCardId, duplicateSuffix, onSuccess ->
            mutateResult(onSuccess) {
                val current = (controller.boardState.value as? LauncherBoardState.Ready)?.board
                    ?: error(context.getString(runtime.resources.cardUnavailable))
                cardActionController.cloneCard(current, sourceCardId, duplicateSuffix)
            }
        },
        updateCardAndMove = { targetCardId, update, destination, onSuccess ->
            mutateResult(onSuccess) {
                val current = (controller.boardState.value as? LauncherBoardState.Ready)?.board
                    ?: error(context.getString(runtime.resources.cardUnavailable))
                cardActionController.updateCardAndMove(
                    current,
                    targetCardId,
                    update,
                    destination,
                )
            }
        },
        moveCard = { targetCardId, statusId, position, onSuccess ->
            mutateResult(onSuccess) {
                cardActionController.moveCard(targetCardId, statusId, position)
            }
        },
        deleteCard = { targetCardId, onSuccess ->
            mutate(onSuccess) { cardActionController.deleteCard(targetCardId) }
        },
        addSubtask = { targetCardId, text, onSuccess ->
            mutate(onSuccess) { cardActionController.addSubtask(targetCardId, text) }
        },
        updateSubtask = { subtaskId, text, onSuccess ->
            mutate(onSuccess) { cardActionController.updateSubtask(subtaskId, text) }
        },
        setSubtaskCompleted = { subtaskId, completed ->
            mutate { cardActionController.setSubtaskCompleted(subtaskId, completed) }
        },
        deleteSubtask = { subtaskId ->
            mutate { cardActionController.deleteSubtask(subtaskId) }
        },
        startFocus = { boardId, targetCardId, durationMillis, replace ->
            val request = CompactPendingFocusRequest(
                boardId,
                targetCardId,
                durationMillis,
                replace,
            )
            if (
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
                PackageManager.PERMISSION_GRANTED
            ) {
                pendingFocusRequest = request
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            } else {
                startFocus(request)
            }
        },
        stopFocus = { timerId: FocusTimerId ->
            mutate { cardActionController.stopFocus(timerId) }
        },
    )

    MinkBoardTheme {
        MinkBoardCardDialogHost(
            board = board,
            openCardId = cardId,
            activeFocusSession = activeFocusSession,
            editorState = editorState,
            onEditorStateChange = { editorState = it },
            actions = actions,
            // Compact Home intentionally does not navigate or scroll the full board after a move.
            onCardPlacementChanged = onCardPlacementChanged,
        )
    }
}
