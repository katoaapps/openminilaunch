package org.sableos.games

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import org.sableos.games.GamesText.t

private const val SUDOKU_SIZE = 9

private const val DEFAULT_SUDOKU =
    "530070000600195000098000060800060003400803001700020006060000280000419005000080079"

@Composable
internal fun SudokuScreen() {
    var state by remember {
        mutableStateOf(requireOk(GamesCore.sudokuStart(DEFAULT_SUDOKU)))
    }
    var board by remember {
        mutableStateOf(requireOk(GamesCore.sudokuBoard(state)))
    }
    var selectedRow by remember { mutableIntStateOf(0) }
    var selectedCol by remember { mutableIntStateOf(2) }
    var status by remember { mutableStateOf(t("choose_a_cell")) }
    val history = remember { mutableStateListOf<String>() }

    fun reset() {
        state = requireOk(GamesCore.sudokuStart(DEFAULT_SUDOKU))
        board = requireOk(GamesCore.sudokuBoard(state))
        selectedRow = 0
        selectedCol = 2
        history.clear()
        status = t("new_puzzle")
    }

    fun setValue(value: Int) {
        val response =
            GamesCore.sudokuSet(
                state,
                selectedRow,
                selectedCol,
                value,
            )

        if (response.startsWith("OK:")) {
            history.add(state)
            state = response.removePrefix("OK:")
            board = requireOk(GamesCore.sudokuBoard(state))
            status =
                if (GamesCore.sudokuSolved(state)) {
                    t("solved")
                } else if (value == 0) {
                    t("cell_erased")
                } else {
                    t("move_accepted")
                }
        } else {
            status = humanize(response)
        }
    }

    fun undo() {
        if (history.isEmpty()) {
            status = t("nothing_to_undo")
            return
        }

        state = history.removeAt(history.lastIndex)
        board = requireOk(GamesCore.sudokuBoard(state))
        status = t("last_move_undone")
    }

    fun erase() {
        setValue(0)
    }

    fun select(
        row: Int,
        col: Int,
    ) {
        selectedRow = row
        selectedCol = col
        status =
            if (isGiven(row, col)) {
                t("fixed_clue")
            } else {
                t("row_column", row + 1, col + 1)
            }
    }

    GameKeyHandler(GameKind.Sudoku) { command ->
        when (command) {
            is GameCommand.Move -> {
                val (row, col) = GameKeymap.moveCursor(selectedRow, selectedCol, command, SUDOKU_SIZE)
                select(row, col)
            }

            is GameCommand.Digit -> {
                setValue(command.value)
            }

            GameCommand.Erase -> {
                erase()
            }

            GameCommand.Undo -> {
                undo()
            }

            GameCommand.NewGame -> {
                reset()
            }

            else -> {
                return@GameKeyHandler false
            }
        }
        true
    }

    GameFrame(
        title = "sudoku",
        subtitle = t("a_focused_number_puzzle"),
        statusLabel = "puzzle",
        status = status,
        hint = t("arrows_move_1_9_or"),
        actions =
            listOf(
                GameAction(label = t("undo"), key = "U", onClick = ::undo),
                GameAction(label = t("erase"), key = "Del", onClick = ::erase),
                GameAction(tab = true, label = t("new_label"), key = "N", onClick = ::reset),
            ),
        tallExtras = {
            Row(
                modifier = Modifier.fillMaxWidth(),
            ) {
                (1..9).forEach { value ->
                    SudokuNumberKey(
                        value = value,
                        modifier = Modifier.weight(1f),
                        onClick = { setValue(value) },
                    )
                }
            }
        },
    ) { side ->
        SudokuBoard(
            side = side,
            board = board,
            selectedRow = selectedRow,
            selectedCol = selectedCol,
            onSelect = ::select,
        )
    }
}

@Composable
private fun SudokuBoard(
    side: Dp,
    board: String,
    selectedRow: Int,
    selectedCol: Int,
    onSelect: (Int, Int) -> Unit,
) {
    Column(
        modifier = Modifier.size(side),
    ) {
        (0 until 3).forEach { blockRow ->
            Row(
                modifier = Modifier.fillMaxWidth(),
            ) {
                (0 until 3).forEach { blockCol ->
                    Surface(
                        modifier = Modifier.weight(1f),
                        shape = RectangleShape,
                        color = SudokuCellColor,
                        border = BorderStroke(1.5.dp, SudokuBoxLine),
                    ) {
                        Column {
                            (0 until 3).forEach { innerRow ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                ) {
                                    (0 until 3).forEach { innerCol ->
                                        val row = blockRow * 3 + innerRow
                                        val col = blockCol * 3 + innerCol
                                        SudokuCell(
                                            row = row,
                                            col = col,
                                            cell = board[row * 9 + col],
                                            highlight = cellHighlight(row, col, selectedRow, selectedCol),
                                            clash = clashes(board, row, col),
                                            onSelect = onSelect,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private enum class CellHighlight { SELECTED, PEER, NONE }

private fun cellHighlight(
    row: Int,
    col: Int,
    selectedRow: Int,
    selectedCol: Int,
): CellHighlight =
    when {
        row == selectedRow && col == selectedCol -> CellHighlight.SELECTED
        row == selectedRow || col == selectedCol || (row / 3 == selectedRow / 3 && col / 3 == selectedCol / 3) ->
            CellHighlight.PEER
        else -> CellHighlight.NONE
    }

@Composable
private fun RowScope.SudokuCell(
    row: Int,
    col: Int,
    cell: Char,
    highlight: CellHighlight,
    clash: Boolean,
    onSelect: (Int, Int) -> Unit,
) {
    val given = isGiven(row, col)
    val selected = highlight == CellHighlight.SELECTED
    // Clues white and bold, your digits light blue, a clash coral; the cursor is a thick gold outline
    // and its row, column and box are tinted.

    val container =
        when {
            clash -> SudokuClashFill
            highlight == CellHighlight.NONE -> SudokuCellColor
            else -> SudokuPeer
        }
    val content =
        when {
            clash -> SudokuClash
            given -> Color.White
            else -> SudokuYours
        }

    Surface(
        onClick = { onSelect(row, col) },
        modifier =
            Modifier
                .weight(1f)
                .aspectRatio(1f),
        shape = RectangleShape,
        color = container,
        contentColor = content,
        border = if (selected) BorderStroke(3.dp, SudokuCursor) else BorderStroke(0.5.dp, SudokuCellLine),
        tonalElevation = 0.dp,
    ) {
        Box(
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text =
                    if (cell == '0') {
                        ""
                    } else {
                        cell.toString()
                    },
                style = MaterialTheme.typography.titleMedium,
                fontWeight =
                    if (given) {
                        FontWeight.Bold
                    } else {
                        FontWeight.Medium
                    },
            )
        }
    }
}

@Composable
private fun SudokuNumberKey(
    value: Int,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        modifier = modifier.heightIn(min = 44.dp),
        shape = MaterialTheme.shapes.extraSmall,
        color = Color.Transparent,
        contentColor = MaterialTheme.colorScheme.primary,
        tonalElevation = 0.dp,
    ) {
        Box(
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = value.toString(),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium,
            )
        }
    }
}

/** True when the digit at [row], [col] is also in its row, column or box. */
internal fun clashes(
    board: String,
    row: Int,
    col: Int,
): Boolean {
    val v = board[row * 9 + col]
    if (v == '0') return false
    return (0 until 9).any { i ->
        val boxRow = row / 3 * 3 + i / 3
        val boxCol = col / 3 * 3 + i % 3
        (i != col && board[row * 9 + i] == v) ||
            (i != row && board[i * 9 + col] == v) ||
            ((boxRow != row || boxCol != col) && board[boxRow * 9 + boxCol] == v)
    }
}

private val SudokuCellColor = Color(0xFF18223F)
private val SudokuPeer = Color(0xFF22305A)
private val SudokuCellLine = Color(0xFF2C3A66)
private val SudokuBoxLine = Color(0xFF7A8BC0)
private val SudokuYours = Color(0xFF7AD7FF)
private val SudokuCursor = Color(0xFFFFC72C)
private val SudokuClash = Color(0xFFFF7A6B)
private val SudokuClashFill = Color(0xFF3A2430)

private fun isGiven(
    row: Int,
    col: Int,
): Boolean = DEFAULT_SUDOKU[row * 9 + col] != '0'
