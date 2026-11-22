package org.sableos.games

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlin.random.Random

private fun cardSize(side: Dp): Pair<Dp, Dp> {
    val w = (side / 8).coerceAtLeast(28.dp)
    return w to w * 1.4f
}

private fun others(
    random: Random,
    count: Int,
): List<String> = PlayerNames.pick(random, count)

// ---------------------------------------------------------------- Teen Patti

@Composable
internal fun TeenPattiScreen(
    random: Random,
    bank: Int,
    onChips: (Int) -> Unit,
) {
    val names = remember { listOf("You") + others(random, 3) }
    val lookKey = if (LocalDeviceProfile.current.letterMoves) "S" else "L"
    var game by remember { mutableStateOf(TeenPattiGame.deal(random, names, listOf(bank, 1000, 1000, 1000))) }
    var choice by remember { mutableIntStateOf(0) }
    LaunchedEffect(game.over) { if (game.over) onChips(game.chips[0]) }

    fun enabled(i: Int): Boolean =
        !game.over && game.turn == 0 &&
            when (i) {
                0 -> game.canChaal(0)
                1 -> game.raiseAllowed(0)
                2 -> game.canShow(0)
                else -> true
            }

    fun run(i: Int) {
        if (!enabled(i)) return
        val action =
            listOf(
                TeenPattiGame.Action.Chaal,
                TeenPattiGame.Action.Raise,
                TeenPattiGame.Action.Show,
                TeenPattiGame.Action.Pack,
            )[i]
        game = game.act(action, random)
    }

    fun next() {
        game = if (game.over) TeenPattiGame.deal(random, names, game.chips) else game
    }

    GameKeyHandler(GameKind.Indian, CardRules.teenPatti) { command ->
        when (command) {
            is GameCommand.Move -> {
                var i = choice
                repeat(4) {
                    i = (i + if (command.dx != 0) command.dx else command.dy + 4) % 4
                    if (i < 0) i += 4
                    if (enabled(i)) {
                        choice = i
                        return@repeat
                    }
                }
            }

            GameCommand.Primary, GameCommand.Select -> {
                if (game.over) next() else run(choice)
            }

            GameCommand.NewGame -> {
                next()
            }

            is GameCommand.Letter -> {
                when (command.value) {
                    'C' -> run(0)

                    'R' -> run(1)

                    'T' -> run(2)

                    'P' -> run(3)

                    // S too: on a phone without arrows L moves the cursor (DeviceProfile.letterMoves).
                    'L', 'S' -> game = game.see()

                    else -> return@GameKeyHandler false
                }
            }

            else -> {
                return@GameKeyHandler false
            }
        }
        true
    }

    GameFrame(
        title = "Teen Patti",
        subtitle = "Practice chips only: they have no value",
        statusLabel = "chips",
        status = game.chips[0].toString(),
        hint = "Arrows choose · Enter confirms · C chaal · R raise · T show · P pack · $lookKey look · N next round",
        actions =
            listOf(
                GameAction("Look", lookKey, active = !game.seen[0]) { game = game.see() },
                GameAction(
                    if (game.over) "Next" else "Pack",
                    if (game.over) "N" else "P",
                ) { if (game.over) next() else run(3) },
            ),
    ) { side ->
        val (cw, ch) = cardSize(side)
        TableSurface(side) {
            Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                NoteLine("Practice chips only: no real value, nothing to buy or win", bold = true)
                RoundTable(
                    players = 4,
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    seatHeight = 38.dp + ch * 0.6f,
                    seat = { p, width ->
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            SeatLabel(
                                names[p],
                                "${game.chips[p]} · " +
                                    if (game.packed[p]) {
                                        "packed"
                                    } else if (game.seen[p]) {
                                        "seen"
                                    } else {
                                        "blind"
                                    },
                                game.turn == p && !game.over,
                                Modifier.width(width),
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy((-cw / 2))) {
                                game.hands[p].forEach {
                                    PlayingCard(
                                        it,
                                        cw * 0.6f,
                                        ch * 0.6f,
                                        faceDown = !(game.revealed && !game.packed[p]),
                                    )
                                }
                            }
                        }
                    },
                ) {
                    Text(
                        "Pot ${game.pot}",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
                NoteLine("Stake ${game.stake} · you pay ${game.cost(0)}", bold = true)
                NoteLine(game.note)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                    game.hands[0].forEach {
                        PlayingCard(
                            it,
                            cw * 1.2f,
                            ch * 1.2f,
                            faceDown = !(game.seen[0] || game.over),
                        )
                    }
                }
                if (game.over) {
                    NoteLine(
                        if (game.winner ==
                            0
                        ) {
                            "You win the pot"
                        } else {
                            "${names[game.winner!!]} wins · Enter for the next round"
                        },
                        bold = true,
                    )
                } else {
                    ChoiceRow(
                        labels = listOf("Chaal ${game.cost(0)}", "Raise", "Show", "Pack"),
                        selected = choice,
                        enabled = List(4) { enabled(it) },
                    ) {
                        choice = it
                        run(it)
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------- Rummy and Filmi Rummy

@Composable
internal fun RummyScreen(
    random: Random,
    style: CardStyle,
    title: String,
) {
    val names = remember { listOf("You") + others(random, 2) }
    var game by remember { mutableStateOf(RummyGame.deal(random, names)) }
    var cursor by remember { mutableIntStateOf(0) }
    var picked by remember { mutableStateOf<Int?>(null) }
    var gaps by remember { mutableStateOf(emptySet<Int>()) }
    val hand = game.hands[0]
    val status =
        remember(hand, game.wild) {
            val a = RummyMelds.analyse(hand, game.wild)
            val ready = hand.size > RummyGame.HAND && hand.indices.any { a.declarable(it) }
            a.dead((1 shl hand.size) - 1) to ready
        }

    fun setHand(h: List<PlayCard>) {
        game = game.copy(hands = listOf(h) + game.hands.drop(1))
        gaps = emptySet()
    }

    fun clampCursor() {
        cursor = cursor.coerceIn(0, (game.hands[0].size - 1).coerceAtLeast(0))
    }

    fun newDeal() {
        game = RummyGame.deal(random, names)
        cursor = 0
        picked = null
        gaps = emptySet()
    }

    GameKeyHandler(GameKind.Indian, CardRules.rummy) { command ->
        when (command) {
            is GameCommand.Move -> {
                val p = picked
                if (command.dx != 0) {
                    val to = (cursor + command.dx).coerceIn(0, hand.lastIndex)
                    if (p != null && to != cursor) {
                        val h = hand.toMutableList()
                        h.add(to, h.removeAt(cursor))
                        setHand(h)
                        picked = to
                    }
                    cursor = to
                }
            }

            GameCommand.Select -> {
                picked = if (picked == null) cursor else null
            }

            GameCommand.Primary -> {
                game = game.discard(cursor, random)
                picked = null
                gaps = emptySet()
                clampCursor()
            }

            GameCommand.NewGame -> {
                newDeal()
            }

            is GameCommand.Letter -> {
                when (command.value) {
                    'Z' -> {
                        game = game.drawFromStock(random).also { cursor = it.hands[0].lastIndex }
                    }

                    'X' -> {
                        game = game.drawFromOpen().also { cursor = it.hands[0].lastIndex }
                    }

                    'M' -> {
                        game = game.declare(cursor)
                        picked = null
                    }

                    'G' -> {
                        RummyMelds.arrange(hand, game.wild).let { (h, g) ->
                            game =
                                game.copy(hands = listOf(h) + game.hands.drop(1))
                            ; gaps = g
                        }
                    }

                    'O' -> {
                        setHand(IndianCards.sorted(hand))
                    }

                    else -> {
                        return@GameKeyHandler false
                    }
                }
            }

            else -> {
                return@GameKeyHandler false
            }
        }
        true
    }

    GameFrame(
        title = title,
        subtitle = "Make two sequences, one pure, and sets with the rest",
        statusLabel = "points left",
        status = status.first.toString(),
        hint = "Z stock · X open pile · Space picks a card up, then move it · Enter throws · M declares · G groups · O sorts · N new",
        actions =
            listOf(
                GameAction("Stock", "Z") { game = game.drawFromStock(random).also { cursor = it.hands[0].lastIndex } },
                GameAction("Open", "X") { game = game.drawFromOpen().also { cursor = it.hands[0].lastIndex } },
                GameAction("Throw", "Enter") {
                    game = game.discard(cursor, random)
                    clampCursor()
                },
                GameAction("Declare", "M", active = status.second) { game = game.declare(cursor) },
            ),
    ) { side ->
        val (cw, ch) = cardSize(side)
        TableSurface(side) {
            Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                RoundTable(
                    players = 3,
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    seat = { p, width ->
                        SeatLabel(
                            names[p],
                            "${game.hands[p].size} cards",
                            game.turn == p && !game.over,
                            Modifier.width(width),
                        )
                    },
                ) {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.Bottom,
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            PlayingCard(PlayCard(1, 0), cw * 1.1f, ch * 1.1f, faceDown = true)
                            NoteLine("Stock ${game.stock.size}")
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            PlayingCard(game.top, cw * 1.1f, ch * 1.1f, style = style)
                            NoteLine("Open")
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            PlayingCard(PlayCard(game.wild, 0), cw * 1.1f, ch * 1.1f, style = style)
                            NoteLine("Wild rank")
                        }
                    }
                }
                NoteLine(
                    when {
                        game.over -> {
                            game.note + " · points " +
                                game.points.indices.joinToString(" ") { "${names[it]} ${game.points[it]}" } +
                                " · N deals again"
                        }

                        game.note.isNotEmpty() -> {
                            game.note
                        }

                        game.turn != 0 -> {
                            "Waiting"
                        }

                        !game.drawn -> {
                            "Your turn: draw from the stock (Z) or the open pile (X)"
                        }

                        status.second -> {
                            "You can declare: M with the card to throw"
                        }

                        else -> {
                            "Throw one card (Enter)"
                        }
                    },
                    bold = true,
                )
                if (hand.isNotEmpty()) NoteLine(style.describe(hand[cursor.coerceIn(0, hand.lastIndex)]))
                HandRow(
                    cards = hand,
                    cursor = cursor,
                    picked = setOfNotNull(picked),
                    cardWidth = cw,
                    cardHeight = ch,
                    style = style,
                    gaps = gaps,
                    onClick = { cursor = it },
                )
            }
        }
    }
}

// ---------------------------------------------------------------- the trick-taking games

@Composable
internal fun TrickScreen(
    random: Random,
    variant: TrickVariant,
    title: String,
) {
    val names = remember { listOf("You") + others(random, variant.players - 1) }
    var game by remember { mutableStateOf(TrickGame.newGame(variant, names, random)) }
    var cursor by remember { mutableIntStateOf(0) }
    var bid by remember { mutableIntStateOf(1) }
    var suit by remember { mutableIntStateOf(0) }
    var warn by remember { mutableStateOf("") }
    val n = variant.players

    LaunchedEffect(game) {
        if (game.needsStep) {
            delay(
                when {
                    game.trick.size == n -> IndianPace.TRICK_HOLD_MS
                    game.phase == TrickPhase.Play -> IndianPace.CARD_MS
                    else -> IndianPace.BID_MS
                },
            )
            game = game.step(random)
        }
    }
    LaunchedEffect(game.phase, game.round) {
        if (game.phase == TrickPhase.Bid) bid = game.bidRange().first { game.bidAllowed(0, it) }
        cursor = cursor.coerceIn(0, (game.hands[0].size - 1).coerceAtLeast(0))
    }

    fun confirm() {
        warn = ""
        when (game.phase) {
            TrickPhase.Bid -> {
                if (game.turn == 0) {
                    if (game.bidAllowed(0, bid)) game = game.bid(bid) else warn = "That bid is not allowed"
                }
            }

            TrickPhase.PickTrump -> {
                if (game.turn == 0) game = game.pickTrump(suit)
            }

            TrickPhase.Play -> {
                if (game.turn == 0 && game.trick.size < n) {
                    val card = game.hands[0].getOrNull(cursor) ?: return
                    if (card in game.legal()) {
                        game = game.play(card)
                        cursor = cursor.coerceIn(0, (game.hands[0].size - 1).coerceAtLeast(0))
                    } else {
                        warn = "You must follow suit"
                    }
                }
            }

            TrickPhase.RoundOver -> {
                game = game.nextRound(random)
                cursor = 0
            }

            TrickPhase.GameOver -> {
                game = TrickGame.newGame(variant, names, random)
                cursor = 0
            }
        }
    }

    GameKeyHandler(GameKind.Indian, CardRules.trick) { command ->
        when (command) {
            is GameCommand.Move -> {
                when (game.phase) {
                    TrickPhase.Bid -> {
                        val range = game.bidRange()
                        if (command.dy != 0) bid = (bid - command.dy).coerceIn(range.first, range.last)
                        if (command.dx != 0) bid = (bid + command.dx).coerceIn(range.first, range.last)
                    }

                    TrickPhase.PickTrump -> {
                        if (command.dx != 0) suit = (suit + command.dx + 4) % 4
                    }

                    else -> {
                        if (command.dx !=
                            0
                        ) {
                            cursor = (cursor + command.dx).coerceIn(0, (game.hands[0].size - 1).coerceAtLeast(0))
                        }
                    }
                }
            }

            GameCommand.Primary, GameCommand.Select -> {
                confirm()
            }

            GameCommand.NewGame -> {
                game = TrickGame.newGame(variant, names, random)
                cursor = 0
            }

            else -> {
                return@GameKeyHandler false
            }
        }
        true
    }

    GameFrame(
        title = title,
        subtitle = "Round ${game.round + 1} of ${variant.rounds}",
        statusLabel = "you",
        status = "%.1f".format(game.scores[0]),
        hint = "Arrows choose a card · Enter plays · Up and Down set a bid · N starts a new game",
        actions =
            listOf(
                GameAction(if (game.roundOver) "Next" else "Play", "Enter") { confirm() },
                GameAction("New", "N", tab = true) { game = TrickGame.newGame(variant, names, random) },
            ),
    ) { side ->
        val (cw, ch) = cardSize(side)
        TableSurface(side) {
            Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                NoteLine(
                    buildString {
                        append(
                            when (variant) {
                                TrickVariant.Mendicot -> {
                                    if (game.trump ==
                                        null
                                    ) {
                                        "Trump is hidden until someone cannot follow"
                                    } else {
                                        "Trump: ${IndianCards.suitName(game.trump!!)}"
                                    }
                                }

                                else -> {
                                    game.trump?.let { "Trump: ${IndianCards.suitName(it)}" }
                                        ?: "No trump this round"
                                }
                            },
                        )
                        if (variant ==
                            TrickVariant.TwoThreeFive
                        ) {
                            append(" · your aim ${game.target(0)}, won ${game.tricks[0]}")
                        }
                        if (variant ==
                            TrickVariant.Mendicot
                        ) {
                            append(" · tens ${game.tens[0] + game.tens[2]}-${game.tens[1] + game.tens[3]}")
                        }
                        if (variant == TrickVariant.Callbreak || variant == TrickVariant.Judgement) {
                            if (game.bids[0] >= 0) append(" · your bid ${game.bids[0]}, won ${game.tricks[0]}")
                        }
                    },
                    bold = true,
                )
                RoundTable(
                    players = n,
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    seat = { p, width ->
                        val detail =
                            when (variant) {
                                TrickVariant.Mendicot -> {
                                    "${if (p % 2 == 0) "partner" else "rival"} · ${game.tricks[p]} won"
                                }

                                TrickVariant.TwoThreeFive -> {
                                    "aims ${game.target(p)} · won ${game.tricks[p]}"
                                }

                                else -> {
                                    if (game.bids[p] >=
                                        0
                                    ) {
                                        "bid ${game.bids[p]} · won ${game.tricks[p]}"
                                    } else {
                                        "bidding"
                                    }
                                }
                            }
                        SeatLabel(names[p], detail, game.turn == p && !game.roundOver, Modifier.width(width))
                    },
                ) {
                    when (game.phase) {
                        TrickPhase.Bid -> {
                            Text(
                                if (game.turn ==
                                    0
                                ) {
                                    "Your bid: $bid   (Up, Down, Enter)"
                                } else {
                                    "${names[game.turn]} is bidding…"
                                },
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                        }

                        TrickPhase.PickTrump -> {
                            if (game.turn == 0) {
                                ChoiceRow(listOf("♠", "♥", "♦", "♣"), suit) {
                                    suit = it
                                    confirm()
                                }
                            } else {
                                Text(
                                    "${names[game.turn]} is choosing trump…",
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                            }
                        }

                        TrickPhase.RoundOver, TrickPhase.GameOver -> {
                            Text(
                                game.summary +
                                    if (game.gameOver) " · game over, Enter plays again" else " · Enter for the next round",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                        }

                        TrickPhase.Play -> {
                            if (game.trick.isEmpty() && game.lastTrick.isNotEmpty()) {
                                // The trick just won stays on view, dimmed, until the next card is played.
                                Box(Modifier.fillMaxSize().alpha(0.55f)) {
                                    TableCards(game.lastTrick, n, names, cw, ch, keyBase = -100)
                                }
                                NoteLine("Last trick", Modifier.align(Alignment.TopCenter))
                            } else {
                                TableCards(
                                    game.trick,
                                    n,
                                    names,
                                    cw,
                                    ch,
                                    keyBase =
                                        game.round * 1000 + game.tricks.sum() * 10,
                                )
                            }
                        }
                    }
                }
                NoteLine(warn.ifEmpty { game.note })
                HandRow(
                    cards = game.hands[0],
                    cursor = cursor,
                    cardWidth = cw,
                    cardHeight = ch,
                    showCursor = game.phase == TrickPhase.Play,
                    onClick = {
                        cursor = it
                        confirm()
                    },
                )
            }
        }
    }
}

// ---------------------------------------------------------------- Bluff

@Composable
internal fun BluffScreen(random: Random) {
    val names = remember { listOf("You") + others(random, 3) }
    var game by remember { mutableStateOf(BluffGame.deal(random, names)) }
    var cursor by remember { mutableIntStateOf(0) }
    var picked by remember { mutableStateOf(emptySet<Int>()) }
    LaunchedEffect(game) {
        if (game.needsStep) {
            delay(IndianPace.CARD_MS)
            game = game.step(random)
        }
    }

    fun play() {
        val hand = game.hands[0]
        val chosen = (if (picked.isEmpty()) setOf(cursor) else picked).filter { it in hand.indices }.map { hand[it] }
        if (chosen.isEmpty() || chosen.size > 4) return
        game = game.play(chosen)
        picked = emptySet()
        cursor = cursor.coerceIn(0, (game.hands[0].size - 1).coerceAtLeast(0))
    }

    fun call() {
        game = game.call()
        cursor = cursor.coerceIn(0, (game.hands[0].size - 1).coerceAtLeast(0))
    }

    GameKeyHandler(GameKind.Indian, CardRules.bluff) { command ->
        when (command) {
            is GameCommand.Move -> {
                if (command.dx !=
                    0
                ) {
                    cursor = (cursor + command.dx).coerceIn(0, (game.hands[0].size - 1).coerceAtLeast(0))
                }
            }

            GameCommand.Select -> {
                if (game.hands[0].isNotEmpty()) {
                    picked =
                        if (cursor in picked) {
                            picked - cursor
                        } else if (picked.size < 4) {
                            picked + cursor
                        } else {
                            picked
                        }
                }
            }

            GameCommand.Primary -> {
                play()
            }

            GameCommand.Erase -> {
                picked = emptySet()
            }

            GameCommand.NewGame -> {
                game = BluffGame.deal(random, names)
                picked = emptySet()
                cursor = 0
            }

            is GameCommand.Letter -> {
                when (command.value) {
                    'B' -> call()
                    else -> return@GameKeyHandler false
                }
            }

            else -> {
                return@GameKeyHandler false
            }
        }
        true
    }

    val claimName = IndianCards.rankName(game.claim)
    GameFrame(
        title = "Bluff",
        subtitle = "Get rid of your cards, lie if you must",
        statusLabel = "claim",
        status = claimName,
        hint = "Space picks cards (up to 4) · Enter puts them down as $claimName · B calls a bluff · Del clears · N new",
        actions =
            listOf(
                GameAction("Put down", "Enter") { play() },
                GameAction("Call", "B", active = game.canCall()) { call() },
                GameAction("New", "N", tab = true) { game = BluffGame.deal(random, names) },
            ),
    ) { side ->
        val (cw, ch) = cardSize(side)
        TableSurface(side) {
            Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                RoundTable(
                    players = 4,
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    seat = { p, width ->
                        SeatLabel(
                            names[p],
                            "${game.hands[p].size} cards",
                            game.turn == p && !game.over,
                            Modifier.width(width),
                        )
                    },
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        PlayingCard(PlayCard(1, 0), cw * 1.2f, ch * 1.2f, faceDown = game.pile.isNotEmpty())
                        Text("Pile: ${game.pile.size}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface)
                        if (game.reveal.isNotEmpty()) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(2.dp),
                            ) { game.reveal.forEach { PlayingCard(it, cw, ch) } }
                        }
                    }
                }
                NoteLine(
                    when {
                        game.over -> {
                            "${names[game.winner!!]} wins · N plays again"
                        }

                        game.last != null && game.turn == 0 -> {
                            "${names[game.last!!.first]} says ${game.last!!.second} × ${IndianCards.rankName(
                                game.last!!.third,
                            )}. Call bluff (B) or put down ${claimName}s (Enter)"
                        }

                        game.turn == 0 -> {
                            "Your turn: put down cards as $claimName"
                        }

                        else -> {
                            game.note
                        }
                    },
                    bold = true,
                )
                NoteLine(game.note)
                HandRow(game.hands[0], cursor, picked, cw, ch, onClick = {
                    cursor = it
                    picked =
                        if (it in picked) picked - it else picked + it
                })
            }
        }
    }
}

// ---------------------------------------------------------------- Donkey

@Composable
internal fun DonkeyScreen(random: Random) {
    val names = remember { listOf("You") + others(random, 3) }
    var game by remember { mutableStateOf(DonkeyGame.deal(random, names)) }
    var cursor by remember { mutableIntStateOf(0) }

    LaunchedEffect(game.phase, game.round) {
        if (game.phase == DonkeyGame.Phase.Grabbing) {
            val times = game.reactionTimes(random)
            var t = 0
            while (game.phase == DonkeyGame.Phase.Grabbing) {
                delay(100)
                t += 100
                for ((p, at) in times) {
                    if (p !in game.grabbed && t >= at &&
                        (game.hasFour(p) || game.grabbed.isNotEmpty() || t >= 4000)
                    ) {
                        game = game.grab(p)
                    }
                }
            }
        }
    }

    fun act() {
        when (game.phase) {
            DonkeyGame.Phase.Passing -> game = game.pass(cursor, random)
            DonkeyGame.Phase.RoundDone -> game = game.nextRound(random)
            DonkeyGame.Phase.Over -> game = DonkeyGame.deal(random, names)
            DonkeyGame.Phase.Grabbing -> game = game.grab(0)
        }
        cursor = cursor.coerceIn(0, 3)
    }

    GameKeyHandler(GameKind.Indian, CardRules.donkey) { command ->
        when (command) {
            is GameCommand.Move -> {
                if (command.dx != 0) cursor = (cursor + command.dx).coerceIn(0, 3)
            }

            GameCommand.Primary -> {
                if (game.phase != DonkeyGame.Phase.Grabbing) act()
            }

            GameCommand.Select -> {
                if (game.phase ==
                    DonkeyGame.Phase.Grabbing
                ) {
                    game = game.grab(0)
                } else if (game.phase != DonkeyGame.Phase.Passing) {
                    act()
                }
            }

            GameCommand.NewGame -> {
                game = DonkeyGame.deal(random, names)
            }

            else -> {
                return@GameKeyHandler false
            }
        }
        true
    }

    GameFrame(
        title = "Donkey",
        subtitle = "Pass cards, get four of a kind, grab a spoon",
        statusLabel = "letters",
        status = game.lettersOf(0).ifEmpty { "none" },
        hint = "Left and Right choose a card · Enter passes it to ${names[1]} · Space grabs a spoon · N new game",
        actions =
            listOf(
                GameAction("Pass", "Enter") { if (game.phase != DonkeyGame.Phase.Grabbing) act() },
                GameAction("Grab", "Space", active = game.phase == DonkeyGame.Phase.Grabbing) {
                    if (game.phase ==
                        DonkeyGame.Phase.Grabbing
                    ) {
                        game = game.grab(0)
                    }
                },
            ),
    ) { side ->
        val (cw, ch) = cardSize(side)
        TableSurface(side) {
            Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                RoundTable(
                    players = 4,
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    seat = { p, width ->
                        SeatLabel(
                            names[p],
                            game.lettersOf(p).ifEmpty { "no letters" } +
                                if (p in
                                    game.grabbed
                                ) {
                                    " · spoon"
                                } else {
                                    ""
                                },
                            false,
                            Modifier.width(width),
                        )
                    },
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        repeat(3) { i ->
                            val taken = i < game.grabbed.size
                            Text(
                                if (taken) "● taken" else "○ spoon",
                                fontSize = 16.sp,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                        }
                    }
                }
                NoteLine(
                    when (game.phase) {
                        DonkeyGame.Phase.Passing -> "Pick a card to pass to ${names[1]}. Four of a kind wins a spoon."
                        DonkeyGame.Phase.Grabbing -> "Grab a spoon now! (Space)"
                        DonkeyGame.Phase.RoundDone -> "${game.note} · Enter for the next round"
                        DonkeyGame.Phase.Over -> "${game.note} · Enter plays again"
                    },
                    bold = true,
                )
                if (game.phase == DonkeyGame.Phase.Grabbing ||
                    game.phase == DonkeyGame.Phase.Passing
                ) {
                    NoteLine(game.note)
                }
                HandRow(game.hands[0], cursor, emptySet(), cw * 1.5f, ch * 1.5f, onClick = {
                    cursor = it
                    if (game.phase ==
                        DonkeyGame.Phase.Passing
                    ) {
                        act()
                    }
                })
            }
        }
    }
}

// ---------------------------------------------------------------- Gulam Choor

@Composable
internal fun GulamChoorScreen(random: Random) {
    val names = remember { listOf("You") + others(random, 3) }
    var game by remember { mutableStateOf(GulamChoorGame.deal(random, names)) }
    var cursor by remember { mutableIntStateOf(0) }
    LaunchedEffect(game) {
        if (game.needsStep) {
            delay(IndianPace.CARD_MS)
            game = game.step(random)
        }
    }
    val victim = game.victim(0)
    val victimSize = game.hands[victim].size

    fun take() {
        if (game.turn == 0 && !game.over && victimSize > 0) {
            game = game.take(cursor.coerceIn(0, victimSize - 1), random)
            cursor = 0
        }
    }

    GameKeyHandler(GameKind.Indian, CardRules.gulam) { command ->
        when (command) {
            is GameCommand.Move -> {
                if (command.dx !=
                    0
                ) {
                    cursor = (cursor + command.dx).coerceIn(0, (victimSize - 1).coerceAtLeast(0))
                }
            }

            GameCommand.Primary, GameCommand.Select -> {
                take()
            }

            GameCommand.NewGame -> {
                game = GulamChoorGame.deal(random, names)
                cursor = 0
            }

            else -> {
                return@GameKeyHandler false
            }
        }
        true
    }

    GameFrame(
        title = "Gulam Choor",
        subtitle = "Don't be left with the odd Jack",
        statusLabel = "pairs out",
        status = (game.thrown / 2).toString(),
        hint = "Left and Right pick one of ${names[victim]}'s cards · Enter takes it · N new game",
        actions =
            listOf(
                GameAction("Take", "Enter") { take() },
                GameAction("New", "N", tab = true) {
                    game = GulamChoorGame.deal(random, names)
                    cursor = 0
                },
            ),
    ) { side ->
        val (cw, ch) = cardSize(side)
        TableSurface(side) {
            Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                RoundTable(
                    players = 4,
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    seat = { p, width ->
                        SeatLabel(
                            names[p],
                            if (game.hands[p].isEmpty()) "safe" else "${game.hands[p].size} cards",
                            game.turn == p && !game.over,
                            Modifier.width(width),
                        )
                    },
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        NoteLine(if (game.over) "" else "Take from ${names[victim]}:", bold = true)
                        HandRow(
                            cards = game.hands[victim],
                            cursor = cursor.coerceIn(0, (victimSize - 1).coerceAtLeast(0)),
                            cardWidth = cw * 0.8f,
                            cardHeight = ch * 0.8f,
                            showCursor = game.turn == 0 && !game.over,
                            faceDown = true,
                            onClick = {
                                cursor = it
                                take()
                            },
                        )
                    }
                }
                NoteLine(
                    when {
                        game.over && game.loser == 0 -> "You are the Gulam Choor! N plays again"
                        game.over -> "${names[game.loser!!]} is the Gulam Choor. N plays again"
                        else -> game.note
                    },
                    bold = true,
                )
                if (game.hands[0].isEmpty() && !game.over) NoteLine("You are out: safe!")
                HandRow(game.hands[0], 0, emptySet(), cw, ch, showCursor = false)
            }
        }
    }
}
