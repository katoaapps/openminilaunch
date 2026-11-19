package com.katoaapps.openminilaunch.ui.minkboard

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
import com.katoaapps.openminilaunch.R
import com.katoaapps.openminilaunch.features.minkboard.OpenMinkLauncherBoardController
import com.katoaapps.openminilaunch.features.minkboard.OpenMinkCardActionController
import com.katoaapps.openminilaunch.features.minkboard.focus.FocusTimerSchedulePrecision
import com.katoaapps.openminilaunch.features.minkboard.focus.StartFocusTimerResult
import com.katoaapps.openminilaunch.openMinkMinkBoardRuntime
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
internal fun OpenMinkHomeCardDialog(
    controller: OpenMinkLauncherBoardController,
    cardId: CardId,
    onDismiss: () -> Unit,
    onCardPlacementChanged: (CardPlacement) -> Unit,
) {
    val context = LocalContext.current
    val runtime = remember(context) { context.openMinkMinkBoardRuntime }
    val cardActionController = remember(runtime) {
        OpenMinkCardActionController(runtime.repository, runtime.focusTimerCoordinator)
    }
    val scope = rememberCoroutineScope()
    val boardState by controller.boardState.collectAsState()
    val activeFocusSession by remember(runtime) {
        runtime.repository.observeFocusSession().catch { emit(null) }
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
                    actionError = context.openMinkBoardErrorMessage(it)
                }
        }
    }

    fun <T> mutateResult(onSuccess: (T) -> Unit, block: suspend () -> T) {
        scope.launch {
            runCatching { block() }
                .onSuccess(onSuccess)
                .onFailure {
                    actionError = context.openMinkBoardErrorMessage(it)
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
                is StartFocusTimerResult.ActiveTimerExists -> {
                    actionError = context.getString(R.string.minkboard_focus_timer_already_running)
                }
                is StartFocusTimerResult.Started -> {
                    if (result.precision == FocusTimerSchedulePrecision.Inexact) {
                        actionError = context.getString(R.string.minkboard_focus_timer_inexact)
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
            actionError = context.getString(R.string.minkboard_notification_permission_required)
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
                    ?: error(context.getString(R.string.minkboard_card_unavailable))
                cardActionController.cloneCard(current, sourceCardId, duplicateSuffix)
            }
        },
        updateCardAndMove = { targetCardId, update, destination, onSuccess ->
            mutateResult(onSuccess) {
                val current = (controller.boardState.value as? LauncherBoardState.Ready)?.board
                    ?: error(context.getString(R.string.minkboard_card_unavailable))
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
