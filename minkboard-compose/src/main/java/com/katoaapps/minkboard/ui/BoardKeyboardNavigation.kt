package com.katoaapps.minkboard.ui

import android.view.KeyEvent
import androidx.compose.foundation.focusable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.onPreviewKeyEvent
import com.katoaapps.minkboard.model.BoardSnapshot
import com.katoaapps.minkboard.model.BoardStatus
import com.katoaapps.minkboard.model.CardId
import com.katoaapps.minkboard.model.StatusId

internal enum class BoardKeyboardCommand {
    NewCard,
    MoveUp,
    MoveLeft,
    MoveDown,
    MoveRight,
    OpenCard,
}

internal data class BoardKeyboardSelection(
    val statusId: StatusId,
    val cardId: CardId,
)

internal class BoardKeyboardNavigationBinding(
    val modifier: Modifier,
    val selection: BoardKeyboardSelection?,
    private val selectCard: (StatusId, CardId) -> Unit,
) {
    fun highlightedCardId(statusId: StatusId): CardId? = selection
        ?.takeIf { it.statusId == statusId }
        ?.cardId

    fun select(statusId: StatusId, cardId: CardId) = selectCard(statusId, cardId)
}

@Composable
internal fun rememberBoardKeyboardNavigation(
    board: BoardSnapshot?,
    enabled: Boolean,
    fallbackStatusId: StatusId?,
    onNewCard: (StatusId) -> Unit,
    onOpenCard: (CardId) -> Unit,
    onStatusHighlighted: (StatusId) -> Unit,
): BoardKeyboardNavigationBinding {
    val focusRequester = remember { FocusRequester() }
    var selection by remember(board?.board?.id?.value) {
        mutableStateOf<BoardKeyboardSelection?>(null)
    }
    val latestBoard by rememberUpdatedState(board)
    val latestEnabled by rememberUpdatedState(enabled)
    val latestFallbackStatusId by rememberUpdatedState(fallbackStatusId)
    val latestOnNewCard by rememberUpdatedState(onNewCard)
    val latestOnOpenCard by rememberUpdatedState(onOpenCard)
    val latestOnStatusHighlighted by rememberUpdatedState(onStatusHighlighted)

    LaunchedEffect(enabled) {
        if (enabled) focusRequester.requestFocus()
    }
    LaunchedEffect(board?.cards, board?.statuses) {
        val selected = selection ?: return@LaunchedEffect
        val selectedCardStillExists = board
            ?.cards
            ?.any { it.id == selected.cardId && it.statusId == selected.statusId }
            ?: false
        if (!selectedCardStillExists) selection = null
    }

    val keyModifier = Modifier
        .focusRequester(focusRequester)
        .onPreviewKeyEvent { composeEvent ->
            val command = boardKeyboardCommand(composeEvent.nativeKeyEvent)
                ?: return@onPreviewKeyEvent false
            val currentBoard = latestBoard
            if (!latestEnabled || currentBoard == null) return@onPreviewKeyEvent false

            val orderedStatuses = currentBoard.statuses.sortedBy { it.position }
            when (command) {
                BoardKeyboardCommand.NewCard -> {
                    val destinationStatusId = selection?.statusId
                        ?.takeIf { selected -> orderedStatuses.any { it.id == selected } }
                        ?: latestFallbackStatusId
                        ?: orderedStatuses.firstOrNull()?.id
                    destinationStatusId?.let(latestOnNewCard)
                }

                BoardKeyboardCommand.OpenCard -> selection?.cardId?.let(latestOnOpenCard)

                else -> {
                    selection = moveBoardKeyboardSelection(
                        board = currentBoard,
                        current = selection,
                        command = command,
                        fallbackStatusId = latestFallbackStatusId,
                    )
                    selection?.statusId?.let(latestOnStatusHighlighted)
                }
            }
            true
        }
        .focusable()

    return BoardKeyboardNavigationBinding(
        modifier = keyModifier,
        selection = selection,
        selectCard = { statusId, cardId ->
            selection = BoardKeyboardSelection(statusId, cardId)
        },
    )
}

internal fun boardKeyboardCommand(event: KeyEvent): BoardKeyboardCommand? = boardKeyboardCommand(
    action = event.action,
    shiftPressed = event.isShiftPressed,
    keyCode = event.keyCode,
    scanCode = event.scanCode,
    unicodeChar = event.unicodeChar,
    repeatCount = event.repeatCount,
)

/**
 * Maps physical-board shortcuts without assuming that the keyboard prints US-QWERTY letters.
 * Linux scan codes 17/30/31/32 identify the physical WASD cluster used by Android keyboards;
 * logical WASD, AZERTY ZQSD, and arrow keys remain fallbacks for devices that omit scan codes.
 */
internal fun boardKeyboardCommand(
    action: Int,
    shiftPressed: Boolean,
    keyCode: Int,
    scanCode: Int,
    unicodeChar: Int,
    repeatCount: Int,
): BoardKeyboardCommand? {
    if (action != KeyEvent.ACTION_DOWN || !shiftPressed) return null

    if (keyCode == KeyEvent.KEYCODE_N && repeatCount == 0) {
        return BoardKeyboardCommand.NewCard
    }
    if (
        (keyCode == KeyEvent.KEYCODE_ENTER || keyCode == KeyEvent.KEYCODE_NUMPAD_ENTER) &&
        repeatCount == 0
    ) {
        return BoardKeyboardCommand.OpenCard
    }

    return when {
        keyCode == KeyEvent.KEYCODE_DPAD_UP || scanCode == SCAN_CODE_W_POSITION ->
            BoardKeyboardCommand.MoveUp
        keyCode == KeyEvent.KEYCODE_DPAD_LEFT || scanCode == SCAN_CODE_A_POSITION ->
            BoardKeyboardCommand.MoveLeft
        keyCode == KeyEvent.KEYCODE_DPAD_DOWN || scanCode == SCAN_CODE_S_POSITION ->
            BoardKeyboardCommand.MoveDown
        keyCode == KeyEvent.KEYCODE_DPAD_RIGHT || scanCode == SCAN_CODE_D_POSITION ->
            BoardKeyboardCommand.MoveRight
        keyCode == KeyEvent.KEYCODE_W -> BoardKeyboardCommand.MoveUp
        keyCode == KeyEvent.KEYCODE_A -> BoardKeyboardCommand.MoveLeft
        keyCode == KeyEvent.KEYCODE_S -> BoardKeyboardCommand.MoveDown
        keyCode == KeyEvent.KEYCODE_D -> BoardKeyboardCommand.MoveRight
        scanCode == 0 && unicodeChar.toKeyboardChar() == 'z' -> BoardKeyboardCommand.MoveUp
        scanCode == 0 && unicodeChar.toKeyboardChar() == 'q' -> BoardKeyboardCommand.MoveLeft
        else -> null
    }
}

internal fun moveBoardKeyboardSelection(
    board: BoardSnapshot,
    current: BoardKeyboardSelection?,
    command: BoardKeyboardCommand,
    fallbackStatusId: StatusId?,
): BoardKeyboardSelection? {
    if (command !in NAVIGATION_COMMANDS) return current
    val statuses = board.statuses.sortedBy { it.position }
    if (statuses.isEmpty()) return null

    val currentStatusIndex = current
        ?.let { selected -> statuses.indexOfFirst { it.id == selected.statusId } }
        ?.takeIf { it >= 0 }
    val fallbackStatusIndex = statuses.indexOfFirst { it.id == fallbackStatusId }
        .takeIf { it >= 0 }
        ?: 0
    val baseStatusIndex = currentStatusIndex ?: fallbackStatusIndex
    val baseCards = board.cardsIn(statuses[baseStatusIndex].id)
    val baseCardIndex = current
        ?.let { selected -> baseCards.indexOfFirst { it.id == selected.cardId } }
        ?.takeIf { it >= 0 }
        ?: 0

    if (current == null || baseCards.isEmpty()) {
        return initialSelection(statuses, board, baseStatusIndex)
    }

    return when (command) {
        BoardKeyboardCommand.MoveUp,
        BoardKeyboardCommand.MoveDown,
        -> {
            val delta = if (command == BoardKeyboardCommand.MoveUp) -1 else 1
            val destinationIndex = (baseCardIndex + delta).coerceIn(0, baseCards.lastIndex)
            BoardKeyboardSelection(statuses[baseStatusIndex].id, baseCards[destinationIndex].id)
        }

        BoardKeyboardCommand.MoveLeft,
        BoardKeyboardCommand.MoveRight,
        -> {
            val delta = if (command == BoardKeyboardCommand.MoveLeft) -1 else 1
            val candidateIndices = generateSequence(baseStatusIndex + delta) { it + delta }
                .takeWhile { it in statuses.indices }
            candidateIndices.firstNotNullOfOrNull { statusIndex ->
                val destinationCards = board.cardsIn(statuses[statusIndex].id)
                destinationCards.takeIf { it.isNotEmpty() }?.let { cards ->
                    val destinationIndex = baseCardIndex.coerceAtMost(cards.lastIndex)
                    BoardKeyboardSelection(statuses[statusIndex].id, cards[destinationIndex].id)
                }
            } ?: current
        }

        else -> current
    }
}

private fun initialSelection(
    statuses: List<BoardStatus>,
    board: BoardSnapshot,
    preferredStatusIndex: Int,
): BoardKeyboardSelection? {
    val searchOrder = buildList {
        add(preferredStatusIndex)
        statuses.indices.forEach { if (it != preferredStatusIndex) add(it) }
    }
    return searchOrder.firstNotNullOfOrNull { statusIndex ->
        board.cardsIn(statuses[statusIndex].id).firstOrNull()?.let { card ->
            BoardKeyboardSelection(statuses[statusIndex].id, card.id)
        }
    }
}

private fun Int.toKeyboardChar(): Char? = takeIf { it != 0 }?.toChar()?.lowercaseChar()

private val NAVIGATION_COMMANDS = setOf(
    BoardKeyboardCommand.MoveUp,
    BoardKeyboardCommand.MoveLeft,
    BoardKeyboardCommand.MoveDown,
    BoardKeyboardCommand.MoveRight,
)

private const val SCAN_CODE_W_POSITION = 17
private const val SCAN_CODE_A_POSITION = 30
private const val SCAN_CODE_S_POSITION = 31
private const val SCAN_CODE_D_POSITION = 32
