package org.sableos.games

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import org.sableos.games.GamesText.t

private const val MINE_FIELD_SIZE = 9

private val MineHidden = Color(0xFF3A4A80)
private val MineEdge = Color(0xFF5266A8)
private val MineOpen = Color(0xFF0F1528)
private val MineFlag = Color(0xFFFF8A3D)
private val MineCursor = Color(0xFFFFC72C)
private val MineOne = Color(0xFF7AA2FF)
private val MineTwo = Color(0xFF8BE04E)
private val MineThree = Color(0xFFFF7A6B)
private val MineFour = Color(0xFFC58BFF)
private val MineFive = Color(0xFFFFC72C)

@Composable
internal fun MinesweeperScreen() {
    var state by remember {
        mutableStateOf(
            requireOk(
                GamesCore.minesweeperStart(
                    9,
                    9,
                    10,
                    1803L,
                ),
            ),
        )
    }
    var view by remember {
        mutableStateOf(requireOk(GamesCore.minesweeperView(state)))
    }
    var flagMode by remember { mutableStateOf(false) }
    var cursorX by remember { mutableIntStateOf(MINE_FIELD_SIZE / 2) }
    var cursorY by remember { mutableIntStateOf(MINE_FIELD_SIZE / 2) }
    var status by remember { mutableStateOf(t("clear_the_field")) }

    fun reset() {
        state =
            requireOk(
                GamesCore.minesweeperStart(
                    9,
                    9,
                    10,
                    1803L,
                ),
            )
        view = requireOk(GamesCore.minesweeperView(state))
        flagMode = false
        status = t("new_field")
    }

    fun applyResponse(response: String) {
        val fields = response.split('|')

        if (fields.size >= 3 && fields[0] == "OK") {
            state = fields[1]
            view = requireOk(GamesCore.minesweeperView(state))
            status = fields.drop(2).joinToString(" ")
        } else {
            status = humanize(response)
        }
    }

    fun selectCell(
        x: Int,
        y: Int,
    ) {
        applyResponse(
            if (flagMode) {
                GamesCore.minesweeperToggleFlag(
                    state,
                    x,
                    y,
                )
            } else {
                GamesCore.minesweeperReveal(
                    state,
                    x,
                    y,
                )
            },
        )
    }

    GameKeyHandler(GameKind.Minesweeper) { command ->
        when (command) {
            is GameCommand.Move -> {
                val (y, x) = GameKeymap.moveCursor(cursorY, cursorX, command, MINE_FIELD_SIZE)
                cursorX = x
                cursorY = y
            }

            GameCommand.Primary -> {
                applyResponse(GamesCore.minesweeperReveal(state, cursorX, cursorY))
            }

            GameCommand.Flag -> {
                applyResponse(GamesCore.minesweeperToggleFlag(state, cursorX, cursorY))
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
        title = "minesweeper",
        subtitle = t("clear_the_field_without_touching"),
        statusLabel = if (flagMode) t("flag_mode") else t("reveal_mode"),
        status = status,
        hint = t("n9_9_field_10_mines"),
        actions =
            listOf(
                GameAction(label = t("reveal"), key = "Space", active = !flagMode, onClick = { flagMode = false }),
                GameAction(label = t("flag"), key = "F", active = flagMode, onClick = { flagMode = true }),
                GameAction(tab = true, label = t("new_label"), key = "N", onClick = ::reset),
            ),
    ) { side ->
        Surface(
            modifier = Modifier.size(side),
            shape = MaterialTheme.shapes.medium,
            color = MaterialTheme.colorScheme.surface,
            border =
                BorderStroke(
                    1.dp,
                    MaterialTheme.colorScheme.outline,
                ),
            tonalElevation = 0.dp,
        ) {
            Column {
                view.chunked(9).forEachIndexed { y, row ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        row.forEachIndexed { x, cell ->
                            MinesweeperCell(
                                cell = cell,
                                cursor = x == cursorX && y == cursorY,
                                modifier = Modifier.weight(1f),
                                onClick = {
                                    cursorX = x
                                    cursorY = y
                                    selectCell(x, y)
                                },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MinesweeperCell(
    cell: Char,
    cursor: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val hidden = cell == 'H' || cell == '#'
    val flagged = cell == 'F'
    val mine = cell == '*' || cell == 'X'

    val container =
        when {
            // Hidden cells are raised blue tiles; opened ones sink to the dark field. Flags are orange.
            flagged -> MineFlag
            mine -> MineThree
            hidden -> MineHidden
            else -> MineOpen
        }

    val content =
        when {
            flagged -> MineOpen
            mine -> Color.White
            else -> minesweeperNumberColor(cell)
        }

    Surface(
        onClick = onClick,
        modifier =
            modifier
                .aspectRatio(1f),
        shape = MaterialTheme.shapes.extraSmall,
        color = container,
        contentColor = content,
        border =
            BorderStroke(
                when {
                    cursor -> 3.dp
                    hidden || flagged -> 1.dp
                    else -> 0.5.dp
                },
                when {
                    cursor -> MineCursor
                    hidden || flagged -> MineEdge
                    else -> MineHidden
                },
            ),
        tonalElevation = 0.dp,
    ) {
        Box(
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = cellLabel(cell),
                style = MaterialTheme.typography.titleMedium,
                color = content,
            )
        }
    }
}

@Composable
private fun minesweeperNumberColor(cell: Char): Color =
    when (cell) {
        '1' -> MineOne
        '2' -> MineTwo
        '3' -> MineThree
        '4' -> MineFour
        '5', '6', '7', '8' -> MineFive
        else -> Color.White
    }

private fun cellLabel(cell: Char): String =
    when (cell) {
        '#', 'H', '0' -> ""
        'F' -> "⚑"
        '*', 'X' -> "✦"
        else -> cell.toString()
    }
