package org.sableos.games

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import org.sableos.games.GamesText.t
import kotlin.random.Random

private val CardRed = Color(0xFFD6304A)
private val CardBlack = Color(0xFF1B1F2E)
private val CardFace = Color(0xFFFFFFFF)
private val TileFace = Color(0xFFFBF6EA)
private val SuitColors = listOf(Color(0xFF2F6FE0), Color(0xFF1F9A5C), Color(0xFFD6304A))
private val Gold = Color(0xFFFFC72C)
private val Teal = Color(0xFF2FD0B5)
private val ReversiBaize = Color(0xFF1F7A5C)
private val ReversiLines = Color(0xFF155A44)
private val ReversiDark = Color(0xFF1B1F2E)
private val ReversiLight = Color(0xFFF4F0E6)
private val ReversiEdge = Color(0xFFC9C2B0)
private val Felt = Color(0xFF17654A)
private val FeltLine = Color(0xFF3F8F6E)
private val CardBack = Color(0xFF2D4A8A)
private val MahjongField = Color(0xFF3A1E30)
private val TileEdge = Color(0xFFD8CCB2)
private val TileShadow = Color(0xFFB89A70)
private val WordsField = Color(0xFF141428)
private val WordsTile = Color(0xFF262B4A)
private val WordsEdge = Color(0xFF33407A)
private val MarkAbsent = Color(0xFF3A4560)
private val SuitMarks = listOf("●", "▮", "✦")
private val MarkRight = Color(0xFF2FD0B5)
private val MarkElsewhere = Color(0xFFFF8A3D)
private const val PHONE_THINK_MS = 450L
private const val BLOCKED_ALPHA = 0.55f

/** The square board surface the four board games draw into. */
@Composable
private fun BoardSurface(
    side: Dp,
    color: Color,
    content: @Composable BoxScope.() -> Unit,
) {
    Surface(
        modifier = Modifier.size(side),
        shape = MaterialTheme.shapes.large,
        color = color,
        border = BorderStroke(1.dp, color.copy(alpha = 0.6f)),
        tonalElevation = 0.dp,
    ) {
        Box(Modifier.fillMaxSize().padding(4.dp)) { content() }
    }
}

// ---------------------------------------------------------------- Reversi

@Composable
internal fun ReversiScreen() {
    var game by remember { mutableStateOf(ReversiGame()) }
    var cursor by remember { mutableIntStateOf(2 * ReversiGame.SIZE + 3) }

    LaunchedEffect(game) {
        if (game.turn == ReversiGame.LIGHT && !game.over) {
            delay(PHONE_THINK_MS)
            game.phoneMove()?.let { game = game.play(it) }
        }
    }

    fun place(index: Int) {
        if (game.turn == ReversiGame.DARK) game = game.play(index)
    }

    GameKeyHandler(GameKind.Reversi) { command ->
        when (command) {
            is GameCommand.Move -> {
                val (row, col) =
                    GameKeymap.moveCursor(
                        cursor / ReversiGame.SIZE,
                        cursor % ReversiGame.SIZE,
                        command,
                        ReversiGame.SIZE,
                    )
                cursor = row * ReversiGame.SIZE + col
            }

            GameCommand.Primary -> {
                place(cursor)
            }

            GameCommand.NewGame -> {
                game = ReversiGame()
            }

            else -> {
                return@GameKeyHandler false
            }
        }
        true
    }

    val dark = game.count(ReversiGame.DARK)
    val light = game.count(ReversiGame.LIGHT)
    GameFrame(
        title = t("reversi"),
        subtitle = t("outflank_the_phone_s_discs"),
        statusLabel = t("you_phone", dark, light),
        status =
            when {
                game.over && dark > light -> t("you_win")
                game.over && dark < light -> t("the_phone_wins")
                game.over -> t("a_draw")
                game.turn == ReversiGame.DARK -> t("your_move")
                else -> t("the_phone_is_thinking")
            },
        hint = t("arrows_move_enter_or_space"),
        actions = listOf(GameAction(tab = true, label = t("new_label"), key = "N") { game = ReversiGame() }),
    ) { side ->
        val legal = if (game.turn == ReversiGame.DARK) game.legalMoves(ReversiGame.DARK).toSet() else emptySet()
        val colors = MaterialTheme.colorScheme
        BoardSurface(side, ReversiLines) {
            Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                repeat(ReversiGame.SIZE) { row ->
                    Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                        repeat(ReversiGame.SIZE) { col ->
                            val index = row * ReversiGame.SIZE + col
                            Box(
                                modifier =
                                    Modifier
                                        .weight(1f)
                                        .fillMaxSize()
                                        .background(ReversiBaize, RoundedCornerShape(4.dp))
                                        .border(
                                            if (index ==
                                                cursor
                                            ) {
                                                2.dp
                                            } else {
                                                0.dp
                                            },
                                            if (index ==
                                                cursor
                                            ) {
                                                Gold
                                            } else {
                                                Color.Transparent
                                            },
                                            RoundedCornerShape(4.dp),
                                        ).clickable { place(index) },
                                contentAlignment = Alignment.Center,
                            ) {
                                when (game.cells[index]) {
                                    ReversiGame.DARK -> {
                                        Disc(ReversiDark, ReversiLines)
                                    }

                                    ReversiGame.LIGHT -> {
                                        Disc(ReversiLight, ReversiEdge)
                                    }

                                    else -> {
                                        if (index in
                                            legal
                                        ) {
                                            Box(Modifier.size(8.dp).background(Gold, CircleShape))
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
}

@Composable
private fun Disc(
    fill: Color,
    edge: Color,
) {
    Box(
        Modifier
            .fillMaxSize()
            .padding(3.dp)
            .background(fill, CircleShape)
            .border(1.dp, edge, CircleShape),
    )
}

// ---------------------------------------------------------------- Solitaire

/** The pile under the Solitaire cursor: row 0 is stock, waste, a gap and the foundations; row 1 the columns. */
internal fun solitairePileAt(
    row: Int,
    col: Int,
): Int =
    when {
        row == 1 -> SolitaireGame.TABLEAU_FIRST + col
        col == 0 -> SolitaireGame.STOCK
        col <= 2 -> SolitaireGame.WASTE
        else -> SolitaireGame.FOUNDATION_FIRST + col - 3
    }

@Composable
internal fun SolitaireScreen() {
    val random = remember { Random(System.nanoTime()) }
    var game by remember { mutableStateOf(SolitaireGame.deal(random)) }
    var row by remember { mutableIntStateOf(1) }
    var col by remember { mutableIntStateOf(0) }
    var picked by remember { mutableStateOf<Int?>(null) }
    var note by remember { mutableStateOf("") }

    fun reset() {
        game = SolitaireGame.deal(random)
        picked = null
        note = ""
    }

    fun choose(pile: Int) {
        val from = picked
        note = ""
        when {
            pile == SolitaireGame.STOCK -> {
                game = game.draw()
                picked = null
            }

            from == null -> {
                if (game.pile(pile).lastOrNull()?.up == true) picked = pile
            }

            from == pile -> {
                picked = null
            }

            else -> {
                val moved = game.move(from, pile)
                if (moved != null) game = moved else note = t("can_t_go_there")
                picked = null
            }
        }
    }

    GameKeyHandler(GameKind.Solitaire) { command ->
        when (command) {
            is GameCommand.Move -> {
                row = (row + command.dy).coerceIn(0, 1)
                col = (col + command.dx).coerceIn(0, 6)
            }

            GameCommand.Primary -> {
                choose(solitairePileAt(row, col))
            }

            GameCommand.Draw -> {
                choose(SolitaireGame.STOCK)
            }

            GameCommand.Auto -> {
                game.toFoundation(picked ?: solitairePileAt(row, col))?.let {
                    game = it
                    picked = null
                }
            }

            GameCommand.Erase -> {
                picked = null
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
        title = t("solitaire"),
        subtitle = t("build_each_suit_from_ace"),
        statusLabel = "moves",
        status =
            when {
                game.won -> t("you_won_in_moves", game.moves)
                note.isNotEmpty() -> note
                picked != null -> t("pick_where_it_goes", game.moves)
                else -> game.moves.toString()
            },
        hint = t("arrows_move_enter_picks_up"),
        actions =
            listOf(
                GameAction(label = t("turn"), key = "Space") { choose(SolitaireGame.STOCK) },
                GameAction(tab = true, label = t("new_label"), key = "N", onClick = ::reset),
            ),
    ) { side ->
        val colors = MaterialTheme.colorScheme
        val cardW = (side - 8.dp - 12.dp) / 7
        val cardH = cardW * 1.3f
        val cursorPile = solitairePileAt(row, col)
        BoardSurface(side, Felt) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    (0 until 7).forEach { c ->
                        if (c == 2) {
                            Spacer(Modifier.size(cardW, cardH))
                            return@forEach
                        }
                        val pile = solitairePileAt(0, c)
                        val cards = game.pile(pile)
                        CardSlot(
                            card =
                                if (pile ==
                                    SolitaireGame.STOCK
                                ) {
                                    cards.lastOrNull()?.copy(up = false)
                                } else {
                                    cards.lastOrNull()
                                },
                            width = cardW,
                            height = cardH,
                            cursor = pile == cursorPile,
                            picked = pile == picked,
                            empty = if (pile == SolitaireGame.STOCK) "↻" else "",
                        ) { choose(pile) }
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    (0 until 7).forEach { c ->
                        val pile = SolitaireGame.TABLEAU_FIRST + c
                        val cards = game.pile(pile)
                        Box(Modifier.size(cardW, side - cardH - 20.dp).clickable { choose(pile) }) {
                            if (cards.isEmpty()) {
                                CardSlot(
                                    null,
                                    cardW,
                                    cardH,
                                    cursor = pile == cursorPile,
                                    picked = false,
                                    empty = "K",
                                ) { choose(pile) }
                            }
                            var y = 0.dp
                            cards.forEachIndexed { i, card ->
                                val top = i == cards.lastIndex
                                Box(Modifier.offset(y = y)) {
                                    CardSlot(
                                        card,
                                        cardW,
                                        cardH,
                                        cursor = top && pile == cursorPile,
                                        picked =
                                            top && pile == picked,
                                        empty = "",
                                    ) { choose(pile) }
                                }
                                y += if (card.up) cardH * 0.32f else cardH * 0.14f
                            }
                        }
                    }
                }
            }
            if (game.won) {
                Text(
                    t("well_done"),
                    Modifier.align(Alignment.Center),
                    color = colors.primary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 22.sp,
                )
            }
        }
    }
}

@Composable
private fun CardSlot(
    card: PlayCard?,
    width: Dp,
    height: Dp,
    cursor: Boolean,
    picked: Boolean,
    empty: String,
    onClick: () -> Unit,
) {
    // The picked card has a gold outline, the cursor a teal one; backs are Sable blue.
    val edge =
        when {
            picked -> Gold
            cursor -> Teal
            card != null && !card.up -> Color.White
            else -> FeltLine
        }
    Surface(
        onClick = onClick,
        modifier = Modifier.size(width, height),
        shape = RoundedCornerShape(4.dp),
        color =
            when {
                card == null -> Felt
                !card.up -> CardBack
                picked -> Gold.copy(alpha = 0.25f).compositeOverFace()
                else -> CardFace
            },
        border = BorderStroke(if (cursor || picked) 2.dp else 1.dp, edge),
    ) {
        Box(Modifier.padding(horizontal = 2.dp), contentAlignment = Alignment.TopStart) {
            when {
                card == null -> {
                    Text(
                        empty,
                        Modifier.align(Alignment.Center),
                        color = FeltLine,
                        fontSize = 14.sp,
                    )
                }

                card.up -> {
                    Text(
                        card.label,
                        color = if (card.red) CardRed else CardBlack,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                    )
                }
            }
        }
    }
}

private fun Color.compositeOverFace(): Color =
    Color(
        red = red * alpha + CardFace.red * (1 - alpha),
        green = green * alpha + CardFace.green * (1 - alpha),
        blue = blue * alpha + CardFace.blue * (1 - alpha),
    )

// ---------------------------------------------------------------- Mahjong

@Composable
internal fun MahjongScreen() {
    val random = remember { Random(System.nanoTime()) }
    var game by remember { mutableStateOf(MahjongGame.deal(random)) }
    var row by remember { mutableIntStateOf(0) }
    var col by remember { mutableIntStateOf(0) }
    var picked by remember { mutableStateOf<TileSpot?>(null) }
    var hint by remember { mutableStateOf<Pair<TileSpot, TileSpot>?>(null) }

    fun reset() {
        game = MahjongGame.deal(random)
        picked = null
        hint = null
    }

    fun choose(
        c: Int,
        r: Int,
    ) {
        col = c
        row = r
        val spot = game.topAt(c, r) ?: return
        if (!game.isFree(spot)) return
        val first = picked
        picked =
            when {
                first == null -> {
                    spot
                }

                first == spot -> {
                    null
                }

                else -> {
                    val matched = game.match(first, spot)
                    if (matched != null) {
                        game = matched
                        hint = null
                        null
                    } else {
                        spot
                    }
                }
            }
    }

    GameKeyHandler(GameKind.Mahjong) { command ->
        when (command) {
            is GameCommand.Move -> {
                row = (row + command.dy).coerceIn(0, MahjongGame.ROWS - 1)
                col = (col + command.dx).coerceIn(0, MahjongGame.COLUMNS - 1)
            }

            GameCommand.Primary -> {
                choose(col, row)
            }

            GameCommand.Hint -> {
                hint = game.hint()
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

    val stuck = !game.won && game.hint() == null
    GameFrame(
        title = t("mahjong"),
        subtitle = t("clear_the_pile_two_matching"),
        statusLabel = t("tiles_left"),
        status =
            when {
                game.won -> t("cleared")
                stuck -> t("no_pairs_left_n_deals", game.tiles.size)
                else -> game.tiles.size.toString()
            },
        hint = t("a_tile_is_free_when"),
        actions =
            listOf(
                GameAction(label = t("hint"), key = "H") { hint = game.hint() },
                GameAction(tab = true, label = t("new_label"), key = "N", onClick = ::reset),
            ),
    ) { side ->
        val colors = MaterialTheme.colorScheme
        BoardSurface(side, MahjongField) {
            Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                repeat(MahjongGame.ROWS) { r ->
                    Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                        repeat(MahjongGame.COLUMNS) { c ->
                            val spot = game.topAt(c, r)
                            val face = spot?.let { game.tiles[it] }
                            val isCursor = c == col && r == row
                            val isPicked = spot != null && spot == picked
                            val isHint = spot != null && hint?.let { spot == it.first || spot == it.second } == true
                            Box(
                                Modifier
                                    .weight(1f)
                                    .fillMaxSize()
                                    .clickable { choose(c, r) },
                            ) {
                                if (spot != null && face != null) {
                                    MahjongTile(face, spot.layer, game.isFree(spot), isPicked, isHint)
                                }
                                if (isCursor) {
                                    Box(Modifier.fillMaxSize().border(3.dp, Gold, RoundedCornerShape(5.dp)))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MahjongTile(
    face: Int,
    layer: Int,
    free: Boolean,
    picked: Boolean,
    hinted: Boolean,
) {
    val colors = MaterialTheme.colorScheme
    val suit = face / 6
    Box(
        Modifier
            .fillMaxSize()
            .alpha(if (free) 1f else BLOCKED_ALPHA)
            .background(if (picked) Gold else TileFace, RoundedCornerShape(5.dp))
            .border(
                if (layer >
                    0
                ) {
                    3.dp
                } else {
                    1.dp
                },
                if (hinted) Teal else if (layer > 0) TileShadow else TileEdge,
                RoundedCornerShape(5.dp),
            ),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                (face % 6 + 1).toString(),
                color = if (picked) CardBlack else SuitColors[suit],
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
            )
            Text(SuitMarks[suit], color = if (picked) CardBlack else SuitColors[suit], fontSize = 12.sp)
        }
    }
}

// ---------------------------------------------------------------- Words

@Composable
internal fun WordsScreen() {
    val random = remember { Random(System.nanoTime()) }
    var game by remember { mutableStateOf(WordsGame.start(random)) }

    fun reset() {
        game = WordsGame.start(random)
    }

    GameKeyHandler(GameKind.Words) { command ->
        when (command) {
            is GameCommand.Letter -> game = game.type(command.value)
            GameCommand.Erase -> game = game.erase()
            GameCommand.Primary -> if (game.over) reset() else game = game.submit()
            else -> return@GameKeyHandler false
        }
        true
    }

    val marks = game.letterMarks()
    val absent =
        marks
            .filterValues { it == LetterMark.Absent }
            .keys
            .sorted()
            .joinToString(" ")
    GameFrame(
        title = t("words"),
        subtitle = t("find_the_five_letter_word"),
        statusLabel = t("guess_of", minOf(game.guesses.size + 1, WordsGame.MAX_GUESSES), WordsGame.MAX_GUESSES),
        status =
            when {
                game.won -> t("got_it_enter_plays_again")
                game.over -> t("it_was_enter_plays_again", game.answer)
                absent.isNotEmpty() -> t("not_in_it", absent)
                else -> t("type_a_word_enter_guesses")
            },
        hint = t("green_right_place_amber_in"),
        actions = listOf(GameAction(tab = true, label = t("new_label"), key = "Enter", onClick = ::reset)),
    ) { side ->
        val colors = MaterialTheme.colorScheme
        BoardSurface(side, WordsField) {
            Column(
                Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterVertically),
            ) {
                repeat(WordsGame.MAX_GUESSES) { r ->
                    val guess = game.guesses.getOrNull(r)
                    val text = guess ?: if (r == game.guesses.size) game.typing else ""
                    val rowMarks = guess?.let { WordsGame.marks(it, game.answer) }
                    // The next empty tile in the row being typed gets a gold outline.
                    val next = if (r == game.guesses.size && !game.over) game.typing.length else -1
                    Row(
                        Modifier.weight(1f),
                        horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterHorizontally),
                    ) {
                        repeat(WordsGame.LENGTH) { i ->
                            val mark = rowMarks?.get(i)
                            val fill =
                                when (mark) {
                                    LetterMark.Right -> MarkRight
                                    LetterMark.Elsewhere -> MarkElsewhere
                                    LetterMark.Absent -> MarkAbsent
                                    null -> WordsTile
                                }
                            Box(
                                Modifier
                                    .weight(1f)
                                    .fillMaxSize()
                                    .background(fill, RoundedCornerShape(4.dp))
                                    .border(
                                        if (next == i) 3.dp else 1.dp,
                                        when {
                                            next == i -> Gold
                                            mark == null -> WordsEdge
                                            else -> fill
                                        },
                                        RoundedCornerShape(4.dp),
                                    ),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    text.getOrNull(i)?.toString().orEmpty(),
                                    color =
                                        if (mark == LetterMark.Right ||
                                            mark == LetterMark.Elsewhere
                                        ) {
                                            WordsField
                                        } else {
                                            Color.White
                                        },
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 22.sp,
                                )
                            }
                        }
                    }
                }
                Spacer(Modifier.height(2.dp))
            }
        }
    }
}
