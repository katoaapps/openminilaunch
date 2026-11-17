package com.katoaapps.openminilaunch.ui.minkboard

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
import com.katoaapps.minkboard.core.MinkBoardHostAccess
import com.katoaapps.minkboard.core.MinkBoardRules
import com.katoaapps.minkboard.core.OpenMinkFullPolicy
import com.katoaapps.minkboard.model.BoardId
import com.katoaapps.minkboard.model.BoardUpdate
import com.katoaapps.minkboard.model.CardId
import com.katoaapps.minkboard.model.CardUpdate
import com.katoaapps.minkboard.model.FocusTimerId
import com.katoaapps.minkboard.model.MinkBoardWorkspace
import com.katoaapps.minkboard.ui.MinkBoardActions
import com.katoaapps.minkboard.ui.MinkBoardApp
import com.katoaapps.minkboard.ui.MinkBoardEditorState
import com.katoaapps.minkboard.ui.MinkBoardTheme
import com.katoaapps.openminilaunch.R
import com.katoaapps.openminilaunch.features.minkboard.access.AndroidBoardAuthenticator
import com.katoaapps.openminilaunch.features.minkboard.access.BoardAuthenticationAvailability
import com.katoaapps.openminilaunch.features.minkboard.access.BoardAuthenticationResult
import com.katoaapps.openminilaunch.features.minkboard.focus.FocusTimerSchedulePrecision
import com.katoaapps.openminilaunch.features.minkboard.focus.StartFocusTimerResult
import com.katoaapps.openminilaunch.openMinkMinkBoardRuntime
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

private data class PendingFocusRequest(
    val boardId: BoardId,
    val cardId: CardId,
    val durationMillis: Long,
    val replace: Boolean,
)

/** Launcher-owned app shell for the reusable MinkBoard Compose surface. */
@Composable
internal fun OpenMinkBoardScreen(
    modifier: Modifier = Modifier,
    onOpenSettings: () -> Unit,
) {
    val context = LocalContext.current
    val runtime = remember(context) { context.openMinkMinkBoardRuntime }
    val scope = rememberCoroutineScope()
    val workspaceFlow = remember(runtime) {
        runtime.repository.observeWorkspace().catch { emit(MinkBoardWorkspace.Empty) }
    }
    val workspace by workspaceFlow.collectAsState(initial = MinkBoardWorkspace.Empty)
    val launcherBoardState by runtime.launcherController.boardState.collectAsState()
    val activeFocusSession by runtime.repository.observeFocusSession()
        .catch { emit(null) }
        .collectAsState(initial = null)
    val boardAccessRevision by runtime.boardAccessRevision.collectAsState()
    val navigationRequest by runtime.navigationRequest.collectAsState()
    var selectedBoardIdValue by rememberSaveable { mutableStateOf<String?>(null) }
    var openCardIdValue by rememberSaveable { mutableStateOf<String?>(null) }
    var editorState by remember { mutableStateOf<MinkBoardEditorState>(MinkBoardEditorState.None) }
    var actionError by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(true) }
    var pendingFocusRequest by remember { mutableStateOf<PendingFocusRequest?>(null) }

    val activity = context as? FragmentActivity
    val boardAuthenticator = remember(activity) {
        activity?.let {
            AndroidBoardAuthenticator(
                it,
                context.getString(R.string.minkboard_authentication_prompt_title),
                context.getString(R.string.minkboard_authentication_prompt_description),
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
                    actionError = failure.message
                        ?: context.getString(R.string.minkboard_change_failed)
                }
        }
    }

    fun authenticate(onGranted: () -> Unit) {
        val authenticator = boardAuthenticator
        if (
            authenticator == null ||
            authenticator.availability() != BoardAuthenticationAvailability.Available
        ) {
            actionError = context.getString(R.string.minkboard_authentication_unavailable)
            return
        }
        authenticator.authenticate { result ->
            when (result) {
                BoardAuthenticationResult.Succeeded -> onGranted()
                BoardAuthenticationResult.Canceled -> Unit
                is BoardAuthenticationResult.Failed -> {
                    actionError = context.getString(R.string.minkboard_authentication_failed)
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
                val result = runtime.focusTimerCoordinator.start(
                    boardId = request.boardId,
                    cardId = request.cardId,
                    durationMillis = request.durationMillis,
                    replaceCurrent = request.replace,
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

    LaunchedEffect(runtime) {
        runtime.launcherController.initialize()
        loading = false
    }
    LaunchedEffect(launcherBoardState, workspace) {
        val designation = (launcherBoardState as?
            com.katoaapps.minkboard.core.LauncherBoardState.Ready)?.designation
        val selectedExists = selectedBoardIdValue?.let(::BoardId)?.let(workspace::board) != null
        if (!selectedExists) {
            val lastOpenBoardId = runtime.uiPreferences.readLastOpenBoardId()
                ?.takeIf { workspace.board(it) != null }
            selectedBoardIdValue = lastOpenBoardId?.value
                ?: designation?.boardId?.value
                ?: workspace.firstBoard?.board?.id?.value
            openCardIdValue = null
        }
    }
    LaunchedEffect(selectedBoardIdValue, workspace) {
        selectedBoardIdValue
            ?.let(::BoardId)
            ?.takeIf { workspace.board(it) != null }
            ?.let(runtime.uiPreferences::writeLastOpenBoardId)
    }

    LaunchedEffect(navigationRequest?.token) {
        val request = navigationRequest ?: return@LaunchedEffect
        val currentWorkspace = runtime.repository.observeWorkspace().first()
        val targetBoard = currentWorkspace.board(request.boardId)
        val requestedCardExists = request.cardId == null ||
            targetBoard?.cards?.any { it.id == request.cardId } == true
        if (targetBoard == null || !requestedCardExists) {
            runtime.consumeNavigationRequest(request.token)
            actionError = context.getString(R.string.minkboard_focus_card_unavailable)
            return@LaunchedEffect
        }
        selectedBoardIdValue = request.boardId.value
        openCardIdValue = null
        editorState = MinkBoardEditorState.None
        requestBoardAccess(targetBoard.board) {
            openCardIdValue = request.cardId?.value
            runtime.consumeNavigationRequest(request.token)
        }
    }

    val selectedBoardIsLocked = selectedBoardIdValue
        ?.let(::BoardId)
        ?.let(workspace::board)
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

    val selectedBoardId = selectedBoardIdValue?.let(::BoardId)
    val openCardId = openCardIdValue?.let(::CardId)
    val actions = MinkBoardActions(
        selectBoard = { boardId ->
            workspace.board(boardId)?.let { snapshot ->
                requestBoardAccess(
                    board = snapshot.board,
                    forcePrompt = snapshot.board.isLocked &&
                        selectedBoardIdValue != boardId.value,
                ) {
                    selectedBoardIdValue = boardId.value
                    openCardIdValue = null
                    editorState = MinkBoardEditorState.None
                }
            }
        },
        createBoard = { draft, onCreated ->
            val create = {
                mutate {
                    val id = runtime.repository.createBoard(draft)
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
                    val id = runtime.repository.createBoardFromTemplate(draft, template)
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
            val board = workspace.board(boardId)?.board
            if (board != null) {
                val rename = {
                    mutate({
                        if (board.isLocked) {
                            runtime.boardAccessSessions.grant(boardId)
                            runtime.markBoardAccessChanged()
                        }
                        onSuccess()
                    }) {
                        runtime.repository.updateBoard(
                            boardId,
                            BoardUpdate(name = name, isLocked = board.isLocked),
                        )
                    }
                }
                if (board.isLocked) authenticate(rename) else rename()
            }
        },
        updateBoard = { boardId, update, onSuccess ->
            val board = workspace.board(boardId)?.board
            if (board != null) {
                val updateBoard = {
                    mutate({
                        if (update.isLocked) runtime.boardAccessSessions.grant(boardId)
                        else runtime.boardAccessSessions.revoke(boardId)
                        runtime.markBoardAccessChanged()
                        onSuccess()
                    }) { runtime.repository.updateBoard(boardId, update) }
                }
                if (board.isLocked || board.isLocked != update.isLocked) {
                    authenticate(updateBoard)
                } else {
                    updateBoard()
                }
            }
        },
        reorderBoards = { ids -> mutate { runtime.repository.reorderBoards(ids) } },
        deleteBoard = { id, onSuccess ->
            if (workspace.boards.size <= 1) {
                actionError = context.getString(R.string.minkboard_keep_one_board)
            } else {
                val board = workspace.board(id)?.board
                if (board != null) {
                    val delete = {
                        mutate({
                            if (selectedBoardIdValue == id.value) selectedBoardIdValue = null
                            runtime.boardAccessSessions.revoke(id)
                            runtime.markBoardAccessChanged()
                            scope.launch { runtime.focusTimerCoordinator.reconcile() }
                            onSuccess()
                        }) { runtime.repository.deleteBoard(id) }
                    }
                    if (board.isLocked) authenticate(delete) else delete()
                }
            }
        },
        deleteBoards = { ids, onSuccess ->
            if (ids.isEmpty() || ids.size >= workspace.boards.size) {
                actionError = context.getString(R.string.minkboard_choose_boards_keep_one)
            } else {
                val selectedBoards = workspace.boards.filter { it.board.id in ids }
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
                        }) { runtime.repository.deleteBoards(ids) }
                    }
                    if (selectedBoards.any { it.board.isLocked }) authenticate(delete) else delete()
                }
            }
        },
        addStatus = { boardId, draft, onSuccess ->
            mutate(onSuccess) { runtime.repository.addStatus(boardId, draft) }
        },
        updateStatus = { id, update, onSuccess ->
            mutate(onSuccess) { runtime.repository.updateStatus(id, update) }
        },
        reorderStatuses = { boardId, ids ->
            mutate { runtime.repository.reorderStatuses(boardId, ids) }
        },
        deleteStatus = { id, policy, onSuccess ->
            mutate(onSuccess) { runtime.repository.deleteStatus(id, policy) }
        },
        addCard = { statusId, draft, onSuccess ->
            mutate(onSuccess) { runtime.repository.addCard(statusId, draft) }
        },
        cloneCard = { cardId, duplicateSuffix, onSuccess ->
            mutate(onSuccess) {
                val source = workspace.boards.firstNotNullOfOrNull { board ->
                    board.cards.firstOrNull { it.id == cardId }
                } ?: error(context.getString(R.string.minkboard_card_unavailable))
                val cloneId = runtime.repository.cloneCard(cardId)
                val suffix = duplicateSuffix.trim().takeIf(String::isNotEmpty)
                    ?.let { " ($it)" }.orEmpty()
                val baseLength = (MinkBoardRules.MAX_CARD_TITLE_LENGTH - suffix.length)
                    .coerceAtLeast(0)
                runtime.repository.updateCard(
                    cloneId,
                    CardUpdate(
                        title = (source.title.take(baseLength).trimEnd() + suffix)
                            .take(MinkBoardRules.MAX_CARD_TITLE_LENGTH),
                        details = source.details,
                        dueDate = source.dueDate,
                        isMajor = source.isMajor,
                    ),
                )
            }
        },
        updateCardAndMove = { cardId, update, destination, onSuccess ->
            mutate(onSuccess) {
                val source = workspace.boards.firstNotNullOfOrNull { board ->
                    board.cards.firstOrNull { it.id == cardId }
                } ?: error(context.getString(R.string.minkboard_card_unavailable))
                runtime.repository.updateCard(cardId, update)
                if (source.statusId != destination) {
                    runtime.repository.moveCard(cardId, destination)
                }
            }
        },
        moveCard = { cardId, statusId, position ->
            mutate { runtime.repository.moveCard(cardId, statusId, position) }
        },
        deleteCard = { cardId, onSuccess ->
            mutate({
                openCardIdValue = null
                scope.launch { runtime.focusTimerCoordinator.reconcile() }
                onSuccess()
            }) {
                runtime.repository.deleteCard(cardId)
            }
        },
        addSubtask = { cardId, text, onSuccess ->
            mutate(onSuccess) { runtime.repository.addSubtask(cardId, text) }
        },
        updateSubtask = { id, text, onSuccess ->
            mutate(onSuccess) { runtime.repository.updateSubtask(id, text) }
        },
        setSubtaskCompleted = { id, completed ->
            mutate { runtime.repository.setSubtaskCompleted(id, completed) }
        },
        deleteSubtask = { id -> mutate { runtime.repository.deleteSubtask(id) } },
        openCard = { boardId, cardId ->
            workspace.board(boardId)?.let { snapshot ->
                requestBoardAccess(snapshot.board) {
                    selectedBoardIdValue = boardId.value
                    openCardIdValue = cardId.value
                }
            }
        },
        closeCard = { openCardIdValue = null; editorState = MinkBoardEditorState.None },
        unlockBoard = { boardId ->
            workspace.board(boardId)?.board?.let { board ->
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
            mutate { runtime.focusTimerCoordinator.stop(timerId) }
        },
        clearActionError = { actionError = null },
    )

    MinkBoardTheme {
        MinkBoardApp(
            workspace = workspace,
            hostAccess = MinkBoardHostAccess.full(OpenMinkFullPolicy),
            selectedBoardId = selectedBoardId,
            openCardId = openCardId,
            isBoardAccessible = { boardId ->
                // Capturing the revision makes access grants observable to this stateless shell.
                boardAccessRevision.let {
                    workspace.board(boardId)?.board?.let { board ->
                        !board.isLocked || runtime.boardAccessSessions.isGranted(boardId)
                    } == true
                }
            },
            activeFocusSession = activeFocusSession,
            canUseBoardLock = canUseBoardLock,
            isLoading = loading,
            fatalError = null,
            actionErrorMessage = actionError,
            editorState = editorState,
            onEditorStateChange = { editorState = it },
            actions = actions,
            onCapabilityUnavailable = { capability ->
                actionError = when (capability) {
                    MinkBoardCapability.CreateAdditionalBoard ->
                        context.getString(R.string.minkboard_additional_boards_unavailable)
                    MinkBoardCapability.UseBoardTemplates ->
                        context.getString(R.string.minkboard_templates_unavailable)
                }
            },
            onOpenSettings = onOpenSettings,
            modifier = modifier,
        )
    }
}
