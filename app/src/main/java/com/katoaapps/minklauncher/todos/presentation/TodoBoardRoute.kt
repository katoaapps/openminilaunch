package com.katoaapps.minklauncher.todos.presentation

import com.katoaapps.minklauncher.todos.home.TodoCardController
import com.katoaapps.minklauncher.todos.runtime.TodoFeatureRuntime

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.view.WindowManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.katoaapps.minkboard.model.Board
import com.katoaapps.minkboard.core.MinkBoardCapability
import com.katoaapps.minkboard.model.BoardId
import com.katoaapps.minkboard.model.BoardUpdate
import com.katoaapps.minkboard.model.CardId
import com.katoaapps.minkboard.model.CardPlacement
import com.katoaapps.minkboard.model.CardUpdate
import com.katoaapps.minkboard.model.FocusTimerId
import com.katoaapps.minkboard.model.StatusId
import com.katoaapps.minkboard.ui.MinkBoardActions
import com.katoaapps.minkboard.ui.MinkBoardApp
import com.katoaapps.minkboard.ui.MinkBoardEditorState
import com.katoaapps.minkboard.ui.MinkBoardTheme
import com.katoaapps.minklauncher.todos.access.AndroidBoardAuthenticator
import com.katoaapps.minklauncher.todos.access.BoardAuthenticationAvailability
import com.katoaapps.minklauncher.todos.access.BoardAuthenticationResult
import com.katoaapps.minkboard.focus.android.MinkBoardFocusSchedulePrecision
import com.katoaapps.minkboard.focus.android.StartMinkBoardFocusResult
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch

private data class PendingFocusRequest(
    val boardId: BoardId,
    val cardId: CardId,
    val durationMillis: Long,
    val replace: Boolean,
)

/** Launcher-owned app shell for the reusable MinkBoard Compose surface. */
@Composable
internal fun TodoBoardRoute(
    runtime: TodoFeatureRuntime,
    modifier: Modifier = Modifier,
    onOpenSettings: () -> Unit,
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
    var selectedBoardIdValue by rememberSaveable { mutableStateOf<String?>(null) }
    var openCardIdValue by rememberSaveable { mutableStateOf<String?>(null) }
    var openStatusIdValue by rememberSaveable { mutableStateOf<String?>(null) }
    var editorState by remember { mutableStateOf<MinkBoardEditorState>(MinkBoardEditorState.None) }
    var actionError by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(true) }
    var navigationLoading by remember { mutableStateOf(false) }
    var pendingFocusRequest by remember { mutableStateOf<PendingFocusRequest?>(null) }

    // The interactive shell observes cheap board summaries plus only the active board. Whole-
    // workspace hydration is reserved for archive operations inside the canonical MinkBoard core.
    val boardsFlow = remember(runtime) {
        runtime.minkBoard.boards.observeBoardSummaries().catch { emit(emptyList()) }
    }
    val boards by boardsFlow.collectAsState(initial = emptyList())
    val selectedBoardId = selectedBoardIdValue?.let(::BoardId)
    val selectedBoardFlow = remember(runtime, selectedBoardId) {
        selectedBoardId?.let(runtime.minkBoard.boards::observeBoard) ?: flowOf(null)
    }
    val selectedBoard by selectedBoardFlow
        .catch { failure ->
            actionError = context.todoBoardErrorMessage(failure, runtime.resources)
            emit(null)
        }
        .collectAsState(initial = null)
    val launcherBoardState by runtime.launcherController.boardState.collectAsState()
    val activeFocusSession by runtime.minkBoard.focus.observeFocusSession()
        .catch { emit(null) }
        .collectAsState(initial = null)
    val boardAccessRevision by runtime.boardAccessRevision.collectAsState()
    val navigationRequest by runtime.navigationRequest.collectAsState()

    val activity = context as? FragmentActivity
    val boardAuthenticator = remember(activity) {
        activity?.let {
            AndroidBoardAuthenticator(
                it,
                context.getString(runtime.resources.authenticationPromptTitle),
                context.getString(runtime.resources.authenticationPromptDescription),
            )
        }
    }
    val canUseBoardLock = boardAuthenticator?.availability() ==
        BoardAuthenticationAvailability.Available

    fun mutate(onSuccess: () -> Unit = {}, block: suspend () -> Unit) {
        scope.launch {
            actionError = null
            runCatching { block() }
                .onSuccess { onSuccess() }
                .onFailure { failure ->
                    actionError = context.todoBoardErrorMessage(failure, runtime.resources)
                }
        }
    }

    fun <T> mutateResult(onSuccess: (T) -> Unit, block: suspend () -> T) {
        scope.launch {
            actionError = null
            runCatching { block() }
                .onSuccess(onSuccess)
                .onFailure { failure ->
                    actionError = context.todoBoardErrorMessage(failure, runtime.resources)
                }
        }
    }

    fun authenticate(onGranted: () -> Unit) {
        val authenticator = boardAuthenticator
        if (
            authenticator == null ||
            authenticator.availability() != BoardAuthenticationAvailability.Available
        ) {
            actionError = context.getString(runtime.resources.authenticationUnavailable)
            return
        }
        authenticator.authenticate { result ->
            when (result) {
                BoardAuthenticationResult.Succeeded -> onGranted()
                BoardAuthenticationResult.Canceled -> Unit
                is BoardAuthenticationResult.Failed -> {
                    actionError = context.getString(runtime.resources.authenticationFailed)
                }
            }
        }
    }

    fun requestBoardAccess(
        board: Board,
        forcePrompt: Boolean = false,
        onGranted: () -> Unit,
    ) {
        if (
            !board.isLocked ||
            (!forcePrompt && runtime.boardAccessSessions.isGranted(board.id))
        ) {
            onGranted()
            return
        }
        authenticate {
            runtime.boardAccessSessions.grant(board.id)
            runtime.markBoardAccessChanged()
            onGranted()
        }
    }

    fun startFocus(request: PendingFocusRequest) {
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

    LaunchedEffect(runtime) {
        runtime.launcherController.initialize()
        loading = false
    }
    LaunchedEffect(launcherBoardState, boards) {
        val designation = (launcherBoardState as?
            com.katoaapps.minkboard.core.LauncherBoardState.Ready)?.designation
        val selectedExists = selectedBoardIdValue?.let(::BoardId)?.let { selectedId ->
            boards.any { it.board.id == selectedId }
        } == true
        if (!selectedExists) {
            val lastOpenBoardId = runtime.uiPreferences.readLastOpenBoardId()
                ?.takeIf { candidate -> boards.any { it.board.id == candidate } }
            selectedBoardIdValue = lastOpenBoardId?.value
                ?: designation?.boardId?.value
                ?: boards.minByOrNull { it.board.position }?.board?.id?.value
            openCardIdValue = null
            openStatusIdValue = null
        }
    }
    LaunchedEffect(selectedBoardIdValue, boards) {
        selectedBoardIdValue
            ?.let(::BoardId)
            ?.takeIf { candidate -> boards.any { it.board.id == candidate } }
            ?.let(runtime.uiPreferences::writeLastOpenBoardId)
    }

    LaunchedEffect(navigationRequest?.token) {
        val request = navigationRequest ?: return@LaunchedEffect
        navigationLoading = true
        val targetBoard = try {
            val board = runtime.minkBoard.boards.board(request.boardId)
            val requestedCard = request.cardId?.let { requestedId ->
                board?.cards?.firstOrNull { it.id == requestedId }
            }
            val requestedStatusId = request.statusId ?: requestedCard?.statusId
            val targetExists = board != null &&
                (request.cardId == null || requestedCard != null) &&
                (requestedStatusId == null || board.statuses.any { it.id == requestedStatusId })
            if (!targetExists) {
                runtime.consumeNavigationRequest(request.token)
                actionError = context.getString(
                    if (request.cardId != null) {
                        runtime.resources.focusCardUnavailable
                    } else {
                        runtime.resources.changeFailed
                    },
                )
                return@LaunchedEffect
            }
            board
        } finally {
            navigationLoading = false
        }
        selectedBoardIdValue = request.boardId.value
        openCardIdValue = null
        openStatusIdValue = null
        editorState = MinkBoardEditorState.None
        requestBoardAccess(targetBoard.board) {
            openStatusIdValue = request.statusId?.value
                ?: request.cardId?.let { requestedId ->
                    targetBoard.cards.firstOrNull { it.id == requestedId }?.statusId?.value
                }
            openCardIdValue = request.cardId?.value
            runtime.consumeNavigationRequest(request.token)
        }
    }

    val selectedBoardIsLocked = selectedBoardIdValue
        ?.let(::BoardId)
        ?.let { selectedId -> boards.firstOrNull { it.board.id == selectedId } }
        ?.board
        ?.isLocked == true
    DisposableEffect(selectedBoardIsLocked, activity) {
        if (selectedBoardIsLocked) {
            activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        } else {
            activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
        }
        onDispose {
            activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
        }
    }
    DisposableEffect(boardAuthenticator) {
        onDispose { boardAuthenticator?.cancel() }
    }

    val openCardId = openCardIdValue?.let(::CardId)
    val actions = MinkBoardActions(
        selectBoard = { boardId ->
            boards.firstOrNull { it.board.id == boardId }?.let { summary ->
                requestBoardAccess(
                    board = summary.board,
                    forcePrompt = summary.board.isLocked &&
                        selectedBoardIdValue != boardId.value,
                ) {
                    selectedBoardIdValue = boardId.value
                    openCardIdValue = null
                    openStatusIdValue = null
                    editorState = MinkBoardEditorState.None
                }
            }
        },
        createBoard = { draft, onCreated ->
            val create = {
                mutate {
                    val id = runtime.minkBoard.boards.createBoard(draft)
                    if (draft.isLocked) {
                        runtime.boardAccessSessions.grant(id)
                        runtime.markBoardAccessChanged()
                    }
                    selectedBoardIdValue = id.value
                    onCreated(id)
                }
            }
            if (draft.isLocked) authenticate(create) else create()
        },
        createBoardFromTemplate = { draft, template, onCreated ->
            val create = {
                mutate {
                    val id = runtime.minkBoard.boards.createBoardFromTemplate(draft, template)
                    if (draft.isLocked) {
                        runtime.boardAccessSessions.grant(id)
                        runtime.markBoardAccessChanged()
                    }
                    selectedBoardIdValue = id.value
                    onCreated(id)
                }
            }
            if (draft.isLocked) authenticate(create) else create()
        },
        renameBoard = { boardId, name, onSuccess ->
            val board = boards.firstOrNull { it.board.id == boardId }?.board
            if (board != null) {
                val rename = {
                    mutate({
                        if (board.isLocked) {
                            runtime.boardAccessSessions.grant(boardId)
                            runtime.markBoardAccessChanged()
                        }
                        onSuccess()
                    }) {
                        runtime.minkBoard.boards.updateBoard(
                            boardId,
                            BoardUpdate(name = name, isLocked = board.isLocked),
                        )
                    }
                }
                if (board.isLocked) authenticate(rename) else rename()
            }
        },
        updateBoard = { boardId, update, onSuccess ->
            val board = boards.firstOrNull { it.board.id == boardId }?.board
            if (board != null) {
                val updateBoard = {
                    mutate({
                        if (update.isLocked) runtime.boardAccessSessions.grant(boardId)
                        else runtime.boardAccessSessions.revoke(boardId)
                        runtime.markBoardAccessChanged()
                        onSuccess()
                    }) { runtime.minkBoard.boards.updateBoard(boardId, update) }
                }
                if (board.isLocked || board.isLocked != update.isLocked) {
                    authenticate(updateBoard)
                } else {
                    updateBoard()
                }
            }
        },
        reorderBoards = { ids -> mutate { runtime.minkBoard.boards.reorderBoards(ids) } },
        deleteBoard = { id, onSuccess ->
            if (boards.size <= 1) {
                actionError = context.getString(runtime.resources.keepOneBoard)
            } else {
                val board = boards.firstOrNull { it.board.id == id }?.board
                if (board != null) {
                    val delete = {
                        mutate({
                            if (selectedBoardIdValue == id.value) selectedBoardIdValue = null
                            runtime.boardAccessSessions.revoke(id)
                            runtime.markBoardAccessChanged()
                            scope.launch { runtime.focusTimerCoordinator.reconcile() }
                            onSuccess()
                        }) { runtime.minkBoard.boards.deleteBoard(id) }
                    }
                    if (board.isLocked) authenticate(delete) else delete()
                }
            }
        },
        deleteBoards = { ids, onSuccess ->
            if (ids.isEmpty() || ids.size >= boards.size) {
                actionError = context.getString(runtime.resources.chooseBoardsKeepOne)
            } else {
                val selectedBoards = boards.filter { it.board.id in ids }
                if (selectedBoards.size == ids.size) {
                    val delete = {
                        mutate({
                            if (ids.any { it.value == selectedBoardIdValue }) {
                                selectedBoardIdValue = null
                            }
                            ids.forEach(runtime.boardAccessSessions::revoke)
                            runtime.markBoardAccessChanged()
                            scope.launch { runtime.focusTimerCoordinator.reconcile() }
                            onSuccess()
                        }) { runtime.minkBoard.boards.deleteBoards(ids) }
                    }
                    if (selectedBoards.any { it.board.isLocked }) authenticate(delete) else delete()
                }
            }
        },
        addStatus = { boardId, draft, onSuccess ->
            mutate(onSuccess) { runtime.minkBoard.statuses.addStatus(boardId, draft) }
        },
        updateStatus = { id, update, onSuccess ->
            mutate(onSuccess) { runtime.minkBoard.statuses.updateStatus(id, update) }
        },
        reorderStatuses = { boardId, ids ->
            mutate { runtime.minkBoard.statuses.reorderStatuses(boardId, ids) }
        },
        deleteStatus = { id, policy, onSuccess ->
            mutate(onSuccess) { runtime.minkBoard.statuses.deleteStatus(id, policy) }
        },
        addCard = { statusId, draft, onSuccess ->
            mutateResult(onSuccess) {
                CardPlacement(
                    cardId = runtime.minkBoard.cards.addCard(statusId, draft),
                    statusId = statusId,
                    position = 0,
                )
            }
        },
        cloneCard = { cardId, duplicateSuffix, onSuccess ->
            mutateResult(onSuccess) {
                val activeBoard = selectedBoard
                    ?: error(context.getString(runtime.resources.cardUnavailable))
                cardActionController.cloneCard(activeBoard, cardId, duplicateSuffix)
            }
        },
        updateCardAndMove = { cardId, update, destination, onSuccess ->
            mutateResult(onSuccess) {
                val activeBoard = selectedBoard
                    ?: error(context.getString(runtime.resources.cardUnavailable))
                cardActionController.updateCardAndMove(
                    activeBoard,
                    cardId,
                    update,
                    destination,
                )
            }
        },
        moveCard = { cardId, statusId, position ->
            mutate { runtime.minkBoard.cards.moveCard(cardId, statusId, position) }
        },
        moveCardAndReport = { cardId, statusId, position, onSuccess ->
            mutateResult(onSuccess) {
                cardActionController.moveCard(cardId, statusId, position)
            }
        },
        deleteCard = { cardId, onSuccess ->
            mutate({
                openCardIdValue = null
                onSuccess()
            }) {
                cardActionController.deleteCard(cardId)
            }
        },
        addSubtask = { cardId, text, onSuccess ->
            mutate(onSuccess) { cardActionController.addSubtask(cardId, text) }
        },
        updateSubtask = { id, text, onSuccess ->
            mutate(onSuccess) { cardActionController.updateSubtask(id, text) }
        },
        setSubtaskCompleted = { id, completed ->
            mutate { cardActionController.setSubtaskCompleted(id, completed) }
        },
        deleteSubtask = { id -> mutate { cardActionController.deleteSubtask(id) } },
        openCard = { boardId, cardId ->
            boards.firstOrNull { it.board.id == boardId }?.let { summary ->
                requestBoardAccess(summary.board) {
                    selectedBoardIdValue = boardId.value
                    openCardIdValue = cardId.value
                }
            }
        },
        closeCard = { openCardIdValue = null; editorState = MinkBoardEditorState.None },
        unlockBoard = { boardId ->
            boards.firstOrNull { it.board.id == boardId }?.board?.let { board ->
                requestBoardAccess(board, forcePrompt = true) {
                    selectedBoardIdValue = boardId.value
                }
            }
        },
        startFocus = { boardId, cardId, durationMillis, replace ->
            val request = PendingFocusRequest(boardId, cardId, durationMillis, replace)
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
        clearActionError = { actionError = null },
    )

    MinkBoardTheme {
        MinkBoardApp(
            boards = boards,
            selectedBoard = selectedBoard,
            hostAccess = runtime.hostAccess,
            openCardId = openCardId,
            openStatusId = openStatusIdValue?.let(::StatusId),
            isBoardAccessible = { boardId ->
                // Capturing the revision makes access grants observable to this stateless shell.
                boardAccessRevision.let {
                    boards.firstOrNull { it.board.id == boardId }?.board?.let { board ->
                        !board.isLocked || runtime.boardAccessSessions.isGranted(boardId)
                    } == true
                }
            },
            activeFocusSession = activeFocusSession,
            canUseBoardLock = canUseBoardLock,
            isLoading = loading || navigationLoading ||
                (selectedBoardId != null && selectedBoard?.board?.id != selectedBoardId),
            fatalError = null,
            actionErrorMessage = actionError,
            editorState = editorState,
            onEditorStateChange = { editorState = it },
            actions = actions,
            onCapabilityUnavailable = { capability ->
                actionError = when (capability) {
                    MinkBoardCapability.CreateAdditionalBoard ->
                        context.getString(runtime.resources.additionalBoardsUnavailable)
                    MinkBoardCapability.UseBoardTemplates ->
                        context.getString(runtime.resources.templatesUnavailable)
                }
            },
            onOpenSettings = onOpenSettings,
            modifier = modifier,
        )
    }
}
