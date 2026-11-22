package org.sableos.games

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.hypot
import kotlin.math.sin
import kotlin.random.Random

private val SEATS_HEIGHT = 34.dp
private val LINES_HEIGHT = 44.dp

/** Ludo is drawn on a classic light board in both themes, so the track always shows. */
private val LUDO_BOARD = Color(0xFFFBF7EC)
private val LUDO_LINE = Color(0xFFCBBFA6)
private val LUDO_INK = Color(0xFF3A3326)

/**
 * The board column: [side] wide, with [extra] height for the seats and move lines. On a square phone
 * (the Q25's 720 x 720, the Titan 2) the frame gives the board a width-limited square, so the column uses
 * the height left beside the rail and the board keeps nearly the full width; in a scrolling tall layout
 * the board is [side] less [extra].
 */
@Composable
private fun BoardRoom(
    side: Dp,
    extra: Dp,
    content: @Composable (boardSide: Dp) -> Unit,
) {
    BoxWithConstraints(Modifier.width(side)) {
        val boardSide = boardSideFor(side, if (constraints.hasBoundedHeight) maxHeight else null, extra)
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            content(boardSide)
        }
    }
}

/** The board's side in a column [side] wide with [room] height (null: as tall as needed) and [extra] for the rest. */
internal fun boardSideFor(
    side: Dp,
    room: Dp?,
    extra: Dp,
): Dp = minOf(side, (room ?: side) - extra).coerceAtLeast(side / 2)

/** A token part way between two places, [t] from 0 to 1. */
private data class Glide<T>(
    val from: T,
    val to: T,
    val t: Float,
)

private fun lerp(
    a: Offset,
    b: Offset,
    t: Float,
): Offset = Offset(a.x + (b.x - a.x) * t, a.y + (b.y - a.y) * t)

// ---------------------------------------------------------------- Snakes & Ladders

/** The classic board's colours: warm paper and sand by day, walnut by night. */
private class SnakesPalette(
    dark: Boolean,
) {
    val board = if (dark) Color(0xFF2B2219) else Color(0xFFE9DCC0)
    val frame = if (dark) Color(0xFF4A3826) else Color(0xFF9C7A4E)
    val light = if (dark) Color(0xFF3A2F24) else Color(0xFFF7EFDD)
    val sand = if (dark) Color(0xFF30271E) else Color(0xFFE2CCA0)
    val number = if (dark) Color(0xFFCDB894) else Color(0xFF7A6142)
    val wood = Color(0xFFA9773F)
    val woodDark = Color(0xFF6E4A22)
    val snakes = listOf(Color(0xFF6E8B3D), Color(0xFFB5583F), Color(0xFF3F7F7B), Color(0xFF8A5A9E), Color(0xFFC08A2E))
    val finish = Color(0xFFD9A62E)
}

/** The centre of square [n] (1 to 100) on a board of cells [cell] wide; square 0 sits just off the first square. */
private fun snakeCentre(
    n: Int,
    cell: Float,
): Offset {
    // Before the first roll the tokens wait on the START square.
    if (n <= 0) return snakeCentre(1, cell)
    val row = (n - 1) / SnakesGame.SIDE
    val col = if (row % 2 == 0) (n - 1) % SnakesGame.SIDE else SnakesGame.SIDE - 1 - (n - 1) % SnakesGame.SIDE
    return Offset((col + 0.5f) * cell, (SnakesGame.SIDE - 1 - row + 0.5f) * cell)
}

/** Where player [i]'s token sits in its square, so up to four share one without hiding each other. */
private fun snakeSpot(
    i: Int,
    base: Offset,
    cell: Float,
): Offset = Offset(base.x + (i % 2 - 0.5f) * cell * 0.42f, base.y + (i / 2 - 0.5f) * cell * 0.42f)

@Composable
internal fun SnakesScreen(random: Random) {
    // Players on the title menu: 2, 3 or 4.
    val startPlayers = (LocalGameShell.current?.option ?: 1) + 2
    var players by remember { mutableIntStateOf(startPlayers) }
    val names = remember { listOf("You") + PlayerNames.pick(random, 3) }
    var game by remember { mutableStateOf(SnakesGame.start(names.take(startPlayers))) }
    var glide by remember { mutableStateOf<Pair<Int, Glide<Int>>?>(null) }
    var shake by remember { mutableStateOf<Pair<Int, Float>?>(null) }
    var trail by remember { mutableStateOf<Pair<Int, List<Int>>?>(null) }
    var dice by remember { mutableStateOf<Int?>(null) }
    var diceBy by remember { mutableIntStateOf(0) }
    var lines by remember { mutableStateOf(listOf<MoveLine>()) }
    var busy by remember { mutableStateOf(false) }
    var job by remember { mutableStateOf<Job?>(null) }
    val scope = rememberCoroutineScope()

    /** Replays the move that took the game to [next]: the dice, the walk, any ladder or snake. */
    suspend fun play(next: SnakesGame) {
        val m = next.last ?: return
        busy = true
        diceBy = m.player
        BoardPace.tumble(m.dice) { dice = it }
        var at = m.from
        for (square in m.walk) {
            val from = at
            BoardPace.glide(BoardPace.STEP_MS) { glide = m.player to Glide(from, square, it) }
            at = square
        }
        if (m.overshot) BoardPace.glide(BoardPace.JUMP_MS) { shake = m.player to sin(it * 6 * PI).toFloat() }
        shake = null
        if (m.to != m.land) BoardPace.glide(BoardPace.JUMP_MS) { glide = m.player to Glide(m.land, m.to, it) }
        game = next
        glide = null
        trail = m.player to listOf(m.from) + m.walk
        lines = lines.plusLine(MoveLine(next.note, m.player))
        if (next.over) {
            val others = next.standings.drop(1).joinToString(", ") { next.names[it] }
            lines = lines.plusLine(MoveLine("Then $others", next.winner!!))
        }
        busy = false
    }

    fun restart(count: Int) {
        job?.cancel()
        players = count.coerceIn(2, 4)
        game = SnakesGame.start(names.take(players))
        glide = null
        shake = null
        trail = null
        dice = null
        lines = emptyList()
        busy = false
    }

    fun roll() {
        if (game.over) {
            restart(players)
        } else if (game.turn == SnakesGame.HUMAN && !busy) {
            val next = game.roll(random)
            job = scope.launch { play(next) }
        }
    }

    LaunchedEffect(game) {
        if (game.needsStep) {
            delay(BoardPace.THINK_MS)
            play(game.roll(random))
        }
    }

    GameKeyHandler(GameKind.Indian, GameCatalog.snakesLadders.controls.first()) { command ->
        when (command) {
            GameCommand.Primary, GameCommand.Select -> roll()
            GameCommand.NewGame -> restart(players)
            is GameCommand.Digit -> if (command.value in 2..4) restart(command.value) else return@GameKeyHandler false
            else -> return@GameKeyHandler false
        }
        true
    }

    val yourTurn = game.turn == SnakesGame.HUMAN && !game.over && !busy
    GameFrame(
        title = "Snakes & Ladders",
        subtitle = "Climb ladders, dodge snakes, land exactly on 100",
        statusLabel = if (game.over) "winner" else "turn",
        status =
            when {
                game.over -> game.names[game.winner!!]
                busy -> game.names[diceBy]
                game.turn == SnakesGame.HUMAN -> "you"
                else -> game.names[game.turn]
            },
        hint = "Space or Enter rolls · your tokens carry a star · 2 3 4 set the number of players · N starts over",
        actions =
            listOf(
                GameAction(if (game.over) "Again" else "Roll", "Space", active = yourTurn || game.over) { roll() },
                GameAction("Players $players", "2 3 4") { restart(if (players == 4) 2 else players + 1) },
            ),
    ) { side ->
        BoardRoom(side, SEATS_HEIGHT + LINES_HEIGHT) { boardSide ->
            Row(
                Modifier.fillMaxWidth().height(SEATS_HEIGHT),
                horizontalArrangement = Arrangement.spacedBy(3.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                game.names.indices.forEach { p ->
                    val place = if (game.over) game.standings.indexOf(p) + 1 else 0
                    val detail =
                        when {
                            game.over && place == 1 -> "Winner"
                            game.over -> ordinal(place)
                            p == game.turn && game.rollsAgain && !busy -> "Roll again"
                            else -> "${game.pos[p]}"
                        }
                    val active = if (game.over) place == 1 else (if (busy) diceBy else game.turn) == p
                    BoardSeat(p, game.names[p], detail, active, Modifier.weight(1f))
                }
                DiceFace(dice, TOKEN_COLORS[diceBy], 30.dp)
            }
            SnakesBoard(game, glide, shake, trail, boardSide)
            MoveLines(lines)
        }
    }
}

@Composable
private fun SnakesBoard(
    game: SnakesGame,
    glide: Pair<Int, Glide<Int>>?,
    shake: Pair<Int, Float>?,
    trail: Pair<Int, List<Int>>?,
    side: Dp,
) {
    // The paper board in both themes, as approved (owner, 2026-11-14): the walnut one was too dark on the Titan 2.
    val palette = remember { SnakesPalette(dark = false) }
    val measurer = rememberTextMeasurer()
    Canvas(Modifier.size(side)) {
        val pad = size.width * 0.03f
        drawRoundRect(palette.frame, Offset.Zero, size, CornerRadius(pad * 1.6f))
        val inner = size.width - pad * 2
        val cell = inner / SnakesGame.SIDE
        translate(pad, pad) {
            drawRect(palette.board, Offset.Zero, Size(inner, inner))
            for (n in 1..SnakesGame.LAST) {
                val c = snakeCentre(n, cell)
                val col = (c.x / cell).toInt()
                val row = (n - 1) / SnakesGame.SIDE
                val fill =
                    when (n) {
                        1 -> TOKEN_COLORS[1].copy(alpha = 0.28f)
                        SnakesGame.LAST -> palette.finish.copy(alpha = 0.45f)
                        else -> if ((row + col) % 2 == 0) palette.light else palette.sand
                    }
                drawRoundRect(
                    fill,
                    Offset(c.x - cell / 2 + 1f, c.y - cell / 2 + 1f),
                    Size(cell - 2f, cell - 2f),
                    CornerRadius(cell * 0.12f),
                )
                drawText(
                    measurer,
                    if (n == 1) "1 START" else n.toString(),
                    Offset(c.x - cell / 2 + cell * 0.07f, c.y - cell / 2 + cell * 0.03f),
                    TextStyle(
                        fontSize = (cell / 4.4f / density).sp,
                        color = palette.number,
                        fontWeight = FontWeight.Medium,
                    ),
                )
            }
            drawStar(snakeCentre(SnakesGame.LAST, cell), cell * 0.3f, palette.finish)
            SnakesGame.LADDERS.forEach { (from, to) ->
                drawLadder(snakeCentre(from, cell), snakeCentre(to, cell), cell, palette)
            }
            SnakesGame.SNAKES.entries.forEachIndexed { i, (from, to) ->
                drawSnake(snakeCentre(from, cell), snakeCentre(to, cell), cell, palette.snakes[i % palette.snakes.size])
            }
            trail?.let { (p, squares) ->
                squares.forEach { drawCircle(TOKEN_COLORS[p].copy(alpha = 0.3f), cell * 0.08f, snakeCentre(it, cell)) }
            }
            game.pos.forEachIndexed { i, square ->
                val g = glide?.takeIf { it.first == i }?.second
                val base =
                    if (g != null) {
                        lerp(snakeCentre(g.from, cell), snakeCentre(g.to, cell), g.t)
                    } else {
                        snakeCentre(square, cell)
                    }
                var at = if (g != null) base else snakeSpot(i, base, cell)
                shake?.takeIf { it.first == i }?.let { at = Offset(at.x + it.second * cell * 0.12f, at.y) }
                val last = trail?.first == i && glide == null
                if (last) drawCircle(TOKEN_COLORS[i].copy(alpha = 0.3f), cell * 0.33f, at)
                drawToken(at, cell * 0.22f, TOKEN_COLORS[i], you = i == SnakesGame.HUMAN)
            }
        }
    }
}

/** A wooden ladder: two shaded rails and rungs, with a soft shadow. */
private fun DrawScope.drawLadder(
    from: Offset,
    to: Offset,
    cell: Float,
    palette: SnakesPalette,
) {
    val dx = to.y - from.y
    val dy = from.x - to.x
    val len = hypot(dx, dy).coerceAtLeast(1f)
    val ox = dx / len * cell * 0.16f
    val oy = dy / len * cell * 0.16f
    val shadow = Offset(cell * 0.04f, cell * 0.06f)
    val rail = cell * 0.075f
    for ((sx, sy) in listOf(-ox to -oy, ox to oy)) {
        val a = Offset(from.x + sx, from.y + sy)
        val b = Offset(to.x + sx, to.y + sy)
        drawLine(Color.Black.copy(alpha = 0.2f), a + shadow, b + shadow, strokeWidth = rail)
        drawLine(palette.woodDark, a, b, strokeWidth = rail)
        drawLine(palette.wood, a, b, strokeWidth = rail * 0.55f)
    }
    val rungs = (len / (cell * 0.4f)).toInt().coerceAtLeast(2)
    for (i in 1 until rungs) {
        val t = i / rungs.toFloat()
        val c = lerp(from, to, t)
        drawLine(palette.woodDark, Offset(c.x - ox, c.y - oy), Offset(c.x + ox, c.y + oy), strokeWidth = cell * 0.06f)
        drawLine(palette.wood, Offset(c.x - ox, c.y - oy), Offset(c.x + ox, c.y + oy), strokeWidth = cell * 0.03f)
    }
}

/** A snake from [head] down to [tail]: a tapering, banded body that winds, and a head with eyes and a tongue. */
private fun DrawScope.drawSnake(
    head: Offset,
    tail: Offset,
    cell: Float,
    color: Color,
) {
    val dx = tail.y - head.y
    val dy = head.x - tail.x
    val len = hypot(dx, dy).coerceAtLeast(1f)
    val steps = (len / (cell * 0.06f)).toInt().coerceIn(30, 140)
    val points =
        (0..steps).map { i ->
            val t = i / steps.toFloat()
            val wave = sin(t * PI * 3).toFloat() * cell * 0.24f * (1 - t * 0.4f)
            Offset(head.x + (tail.x - head.x) * t + dx / len * wave, head.y + (tail.y - head.y) * t + dy / len * wave)
        }
    val dark = darken(color, 0.2f)
    points.forEachIndexed { i, p ->
        val t = i / steps.toFloat()
        val r = cell * (0.13f - 0.1f * t)
        drawCircle(Color.Black.copy(alpha = 0.12f), r, Offset(p.x + cell * 0.04f, p.y + cell * 0.06f))
    }
    points.forEachIndexed { i, p ->
        val t = i / steps.toFloat()
        val r = cell * (0.13f - 0.1f * t)
        val band = (i / (steps / 6 + 1)) % 2 == 0
        drawCircle(if (band) color else dark, r, p)
        drawCircle(lighten(color, 0.35f).copy(alpha = 0.5f), r * 0.35f, Offset(p.x - r * 0.3f, p.y - r * 0.3f))
    }
    val hr = cell * 0.19f
    drawOval(color, Offset(head.x - hr, head.y - hr * 0.85f), Size(hr * 2, hr * 1.7f))
    val tongue = Path()
    tongue.moveTo(head.x, head.y - hr * 0.8f)
    tongue.lineTo(head.x, head.y - hr * 1.35f)
    tongue.moveTo(head.x, head.y - hr * 1.35f)
    tongue.lineTo(head.x - hr * 0.2f, head.y - hr * 1.6f)
    tongue.moveTo(head.x, head.y - hr * 1.35f)
    tongue.lineTo(head.x + hr * 0.2f, head.y - hr * 1.6f)
    drawPath(tongue, Color(0xFFC0392B), style = Stroke(cell * 0.025f))
    for (side in listOf(-1, 1)) {
        val eye = Offset(head.x + side * hr * 0.45f, head.y - hr * 0.2f)
        drawCircle(Color.White, hr * 0.28f, eye)
        drawCircle(Color.Black, hr * 0.14f, eye)
    }
}

// ---------------------------------------------------------------- Ludo

/** Where things go on Ludo's 15 x 15 grid: (row, column). */
internal object LudoGeometry {
    val PATH: List<Pair<Int, Int>> =
        buildList {
            for (c in 1..5) add(6 to c)
            for (r in 5 downTo 0) add(r to 6)
            add(0 to 7)
            add(0 to 8)
            for (r in 1..5) add(r to 8)
            for (c in 9..14) add(6 to c)
            add(7 to 14)
            add(8 to 14)
            for (c in 13 downTo 9) add(8 to c)
            for (r in 9..14) add(r to 8)
            add(14 to 7)
            add(14 to 6)
            for (r in 13 downTo 9) add(r to 6)
            for (c in 5 downTo 0) add(8 to c)
            add(7 to 0)
            add(6 to 0)
        }

    /** Each colour's yard, a 6 x 6 corner: (top row, left column). */
    val YARDS = listOf(0 to 0, 0 to 9, 9 to 9, 9 to 0)

    /** The centre (x, y) in cell units of token [token] of [color] at [progress]. */
    fun centre(
        color: Int,
        token: Int,
        progress: Int,
    ): Pair<Float, Float> =
        when {
            progress < 0 -> {
                val (r, c) = YARDS[color]
                (c + 2 + (token % 2) * 2) to (r + 2 + (token / 2) * 2)
            }

            progress <= LudoGame.LAST_TRACK -> {
                val (r, c) = PATH[LudoGame.square(color, progress)]
                (c + 0.5f) to (r + 0.5f)
            }

            progress < LudoGame.HOME -> {
                val k = progress - 51
                val (r, c) =
                    when (color) {
                        0 -> 7 to 1 + k
                        1 -> 1 + k to 7
                        2 -> 7 to 13 - k
                        else -> 13 - k to 7
                    }
                (c + 0.5f) to (r + 0.5f)
            }

            else -> {
                (7.5f + (token % 2 - 0.5f) * 0.7f) to (7.5f + (token / 2 - 0.5f) * 0.7f)
            }
        }.let { (x, y) -> x.toFloat() to y.toFloat() }
}

/** A Ludo token's place on the board in cell units, as an offset. */
private fun ludoAt(
    color: Int,
    token: Int,
    progress: Int,
): Offset = LudoGeometry.centre(color, token, progress).let { (x, y) -> Offset(x, y) }

@Composable
internal fun LudoScreen(random: Random) {
    val names = remember { listOf("You") + PlayerNames.pick(random, 3) }
    var game by remember { mutableStateOf(LudoGame.start(names)) }
    var choice by remember { mutableIntStateOf(0) }
    var glides by remember { mutableStateOf<Map<Pair<Int, Int>, Glide<Int>>>(emptyMap()) }
    var trail by remember { mutableStateOf<LudoMove?>(null) }
    var flash by remember { mutableStateOf(emptySet<Pair<Int, Int>>()) }
    var dice by remember { mutableStateOf<Int?>(null) }
    var diceBy by remember { mutableIntStateOf(0) }
    var lines by remember { mutableStateOf(listOf<MoveLine>()) }
    var busy by remember { mutableStateOf(false) }
    var job by remember { mutableStateOf<Job?>(null) }
    val scope = rememberCoroutineScope()

    suspend fun rollFor(next: LudoGame) {
        busy = true
        diceBy = game.turn
        val roller = game.turn
        BoardPace.tumble(next.rolled ?: 1) { dice = it }
        game = next
        lines = lines.plusLine(MoveLine(next.note, roller))
        busy = false
    }

    /** Replays the move that took the game to [next]: the walk, then any token sent back to its yard. */
    suspend fun moveTo(next: LudoGame) {
        val m = next.last ?: return
        busy = true
        var at = m.from
        for (step in m.walk) {
            val from = at
            BoardPace.glide(if (from < 0) BoardPace.JUMP_MS else BoardPace.STEP_MS) {
                glides = mapOf((m.player to m.token) to Glide(from, step, it))
            }
            at = step
        }
        if (m.captured.isNotEmpty()) {
            flash = m.captured.map { it.first to it.second }.toSet()
            delay(BoardPace.STEP_MS * 2L)
            flash = emptySet()
            BoardPace.glide(BoardPace.JUMP_MS) { t ->
                glides = mapOf((m.player to m.token) to Glide(m.to, m.to, 1f)) +
                    m.captured.associate { (c, tok, from) -> (c to tok) to Glide(from, -1, t) }
            }
        }
        game = next
        glides = emptyMap()
        trail = m
        lines = lines.plusLine(MoveLine(next.note, m.player))
        busy = false
    }

    fun newGame() {
        job?.cancel()
        game = LudoGame.start(names)
        glides = emptyMap()
        trail = null
        flash = emptySet()
        dice = null
        lines = emptyList()
        busy = false
    }

    LaunchedEffect(game) {
        if (game.needsStep) {
            if (game.roll == null) {
                delay(BoardPace.THINK_MS)
                rollFor(game.rollDice(random))
            } else {
                delay(BoardPace.PICK_MS)
                moveTo(game.step(random))
            }
        } else if (!game.over && game.turn == LudoGame.HUMAN && game.roll != null) {
            val options = game.movable()
            if (options.size == 1) {
                delay(BoardPace.PICK_MS)
                moveTo(game.move(options.first()))
            } else if (choice !in options) {
                choice = options.firstOrNull() ?: 0
            }
        }
    }

    fun act() {
        if (game.over) {
            newGame()
        } else if (game.turn == LudoGame.HUMAN && !busy) {
            if (game.roll == null) {
                val next = game.rollDice(random)
                job = scope.launch { rollFor(next) }
            } else if (choice in game.movable()) {
                val next = game.move(choice)
                job = scope.launch { moveTo(next) }
            }
        }
    }

    GameKeyHandler(GameKind.Indian, BoardRules.ludoControls) { command ->
        when (command) {
            GameCommand.Primary, GameCommand.Select -> {
                act()
            }

            is GameCommand.Move -> {
                if (game.turn == LudoGame.HUMAN && game.roll != null && !busy) {
                    val options = game.movable()
                    if (options.isNotEmpty()) {
                        val at = options.indexOf(choice).coerceAtLeast(0)
                        val step = if (command.dx != 0) command.dx else command.dy
                        choice = options[(at + step + options.size) % options.size]
                    }
                }
            }

            GameCommand.NewGame -> {
                newGame()
            }

            else -> {
                return@GameKeyHandler false
            }
        }
        true
    }

    val yourTurn = game.turn == LudoGame.HUMAN && !game.over && !busy
    GameFrame(
        title = "Ludo",
        subtitle = "You are red, with a star on your tokens. Roll a six to bring a token out",
        statusLabel = if (game.over) "winner" else "turn",
        status =
            when {
                game.over -> game.names[game.winner!!]
                game.turn == LudoGame.HUMAN -> if (game.roll == null) "your roll" else "pick a token"
                else -> game.names[game.turn]
            },
        hint = "Space or Enter rolls and moves · arrows choose a token, its ghost shows where it lands · N starts over",
        actions =
            listOf(
                GameAction(
                    if (game.over) {
                        "Again"
                    } else if (game.roll == null) {
                        "Roll"
                    } else {
                        "Move"
                    },
                    "Space",
                    active = yourTurn || game.over,
                ) { act() },
                GameAction("New", "N", tab = true) { newGame() },
            ),
    ) { side ->
        BoardRoom(side, LINES_HEIGHT) { boardSide ->
            Box(Modifier.size(boardSide), contentAlignment = Alignment.Center) {
                LudoBoard(
                    game = game,
                    choice = choice,
                    glides = glides,
                    trail = trail,
                    flash = flash,
                    dice = dice,
                    diceBy = diceBy,
                    busy = busy,
                    side = boardSide,
                )
            }
            MoveLines(lines)
        }
    }
}

@Composable
private fun LudoBoard(
    game: LudoGame,
    choice: Int,
    glides: Map<Pair<Int, Int>, Glide<Int>>,
    trail: LudoMove?,
    flash: Set<Pair<Int, Int>>,
    dice: Int?,
    diceBy: Int,
    busy: Boolean,
    side: Dp,
) {
    val boardLight = LUDO_BOARD
    val line = LUDO_LINE
    val ink = LUDO_INK
    val measurer = rememberTextMeasurer()
    Canvas(Modifier.size(side)) {
        val cell = size.width / 15f
        drawRect(boardLight, Offset.Zero, size)

        fun square(
            r: Int,
            c: Int,
            color: Color,
        ) {
            drawRect(color, Offset(c * cell, r * cell), Size(cell, cell))
            drawRect(line, Offset(c * cell, r * cell), Size(cell, cell), style = Stroke(1f))
        }
        val active = if (busy) diceBy else game.turn
        LudoGeometry.YARDS.forEachIndexed { i, (r0, c0) ->
            drawRect(TOKEN_COLORS[i].copy(alpha = 0.85f), Offset(c0 * cell, r0 * cell), Size(cell * 6, cell * 6))
            drawRoundRect(
                boardLight,
                Offset((c0 + 1) * cell, (r0 + 1) * cell),
                Size(cell * 4, cell * 4),
                CornerRadius(cell * 0.4f),
            )
            // The seat: name and tokens home on the yard's band, outlined while it is this player's turn.
            val home = game.homeCount(i)
            val label =
                when {
                    game.over && game.winner == i -> "${game.names[i]} wins"
                    game.threeSixes == i -> "${game.names[i]} · three 6s"
                    else -> "${game.names[i]} · $home/4"
                }
            drawText(
                measurer,
                label,
                Offset(c0 * cell + cell * 0.25f, r0 * cell + cell * 0.12f),
                TextStyle(
                    fontSize = (cell * 0.5f / density).sp,
                    color = Color.White,
                    fontWeight = if (i == active && !game.over) FontWeight.Bold else FontWeight.Normal,
                ),
                maxLines = 1,
                size = Size(cell * 5.6f, cell),
            )
            if (i == active && !game.over || game.over && game.winner == i) {
                drawRect(ink, Offset(c0 * cell, r0 * cell), Size(cell * 6, cell * 6), style = Stroke(cell * 0.14f))
            } else {
                drawRect(Color.Black.copy(alpha = 0.18f), Offset(c0 * cell, r0 * cell), Size(cell * 6, cell * 6))
            }
        }
        LudoGeometry.PATH.forEach { (r, c) -> square(r, c, boardLight) }
        for (k in 0 until 5) {
            square(7, 1 + k, TOKEN_COLORS[0])
            square(1 + k, 7, TOKEN_COLORS[1])
            square(7, 13 - k, TOKEN_COLORS[2])
            square(13 - k, 7, TOKEN_COLORS[3])
        }
        for (i in 0 until 4) {
            val (r, c) = LudoGeometry.PATH[i * 13]
            square(r, c, TOKEN_COLORS[i].copy(alpha = 0.7f))
            val (sr, sc) = LudoGeometry.PATH[i * 13 + 8]
            drawStar(Offset((sc + 0.5f) * cell, (sr + 0.5f) * cell), cell * 0.36f, ink.copy(alpha = 0.55f))
        }
        val c = Offset(7.5f * cell, 7.5f * cell)
        listOf(
            listOf(Offset(6f, 6f), Offset(9f, 6f)),
            listOf(Offset(9f, 6f), Offset(9f, 9f)),
            listOf(Offset(9f, 9f), Offset(6f, 9f)),
            listOf(Offset(6f, 9f), Offset(6f, 6f)),
        ).forEachIndexed { i, pts ->
            val p = Path()
            p.moveTo(pts[0].x * cell, pts[0].y * cell)
            p.lineTo(pts[1].x * cell, pts[1].y * cell)
            p.lineTo(c.x, c.y)
            p.close()
            drawPath(p, TOKEN_COLORS[i])
        }
        // The dice sits in the middle, in the colour of the player rolling.
        if (dice != null) {
            val ds = cell * 1.5f
            drawDice(Offset(c.x - ds / 2, c.y - ds / 2), ds, dice, TOKEN_COLORS[diceBy])
        }
        // The last move's path, faint, in the mover's colour.
        trail?.let { m ->
            (listOf(m.from) + m.walk).filter { it >= 0 }.forEach { progress ->
                drawCircle(
                    TOKEN_COLORS[m.player].copy(alpha = 0.3f),
                    cell * 0.12f,
                    ludoAt(m.player, m.token, progress) * cell,
                )
            }
        }
        val movable = if (game.turn == LudoGame.HUMAN && game.roll != null && !busy) game.movable() else emptyList()
        // Ghosts: where each of your movable tokens would land; the chosen one plainer.
        movable.forEach { t ->
            val to = game.landing(t) ?: return@forEach
            drawToken(
                ludoAt(0, t, to) * cell,
                cell * 0.34f,
                TOKEN_COLORS[0],
                you = true,
                alpha =
                    if (t ==
                        choice
                    ) {
                        0.5f
                    } else {
                        0.22f
                    },
            )
        }
        // Tokens that share a square sit side by side, smaller, with how many are there.
        val placed =
            game.tokens.flatMapIndexed { color, list ->
                list.mapIndexed { token, progress -> Triple(color, token, progress) }
            }
        val stacks =
            placed
                .filter { (color, token, progress) -> progress in 0 until LudoGame.HOME && (color to token) !in glides }
                .groupBy { (color, token, progress) -> ludoAt(color, token, progress) }
        placed.forEach { (color, token, progress) ->
            val g = glides[color to token]
            var at: Offset
            var radius = cell * 0.36f
            if (g != null) {
                at = lerp(ludoAt(color, token, g.from), ludoAt(color, token, g.to), g.t) * cell
            } else {
                at = ludoAt(color, token, progress) * cell
                val stack = stacks[ludoAt(color, token, progress)]
                if (stack != null && stack.size > 1) {
                    val k = stack.indexOfFirst { it.first == color && it.second == token }
                    radius = cell * 0.24f
                    at = Offset(at.x + (k % 2 - 0.5f) * cell * 0.46f, at.y + (k / 2 - 0.5f) * cell * 0.46f)
                }
            }
            if (trail?.player == color && trail.token == token && glides.isEmpty()) {
                drawCircle(TOKEN_COLORS[color].copy(alpha = 0.3f), radius * 1.4f, at)
            }
            if ((color to token) in flash) drawCircle(Color.White, radius * 1.5f, at)
            drawToken(at, radius, TOKEN_COLORS[color], you = color == LudoGame.HUMAN)
            if (color == LudoGame.HUMAN && token in movable) {
                drawCircle(ink, radius * 1.3f, at, style = Stroke(if (token == choice) cell * 0.12f else cell * 0.04f))
            }
        }
        stacks.forEach { (spot, stack) ->
            if (stack.size > 1) {
                val at = spot * cell
                drawCircle(ink, cell * 0.2f, Offset(at.x + cell * 0.45f, at.y - cell * 0.45f))
                drawText(
                    measurer,
                    stack.size.toString(),
                    Offset(at.x + cell * 0.33f, at.y - cell * 0.66f),
                    TextStyle(fontSize = (cell * 0.32f / density).sp, color = boardLight, fontWeight = FontWeight.Bold),
                )
            }
        }
    }
}
