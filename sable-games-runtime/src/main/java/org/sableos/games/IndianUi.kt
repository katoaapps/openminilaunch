package org.sableos.games

import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import kotlin.random.Random

private val TableFelt = Color(0xFF1F6B4F)
private val TableBrass = Color(0xFFC9A14A)
private val FaceColor = Color(0xFFF7F9FC)
private val BlueSuit = Color(0xFF1F6E96)
private val InkSuit = Color(0xFF12171C)

/** How a card is written and coloured: the plain deck, or the original filmi-themed one. */
internal interface CardStyle {
    fun label(card: PlayCard): String

    fun color(card: PlayCard): Color

    /** The long name shown for the card under the cursor. */
    fun describe(card: PlayCard): String
}

internal object ClassicStyle : CardStyle {
    override fun label(card: PlayCard): String =
        if (card.joker) {
            "JKR"
        } else {
            IndianCards.rankName(card.rank) +
                IndianCards.suitSymbol(card.suit)
        }

    override fun color(card: PlayCard): Color = if (card.joker || card.red) BlueSuit else InkSuit

    override fun describe(card: PlayCard): String =
        if (card.joker) "Joker" else "${IndianCards.rankName(card.rank)} of ${IndianCards.suitName(card.suit)}"
}

/** Filmi Rummy: four invented film roles in place of the suits. No real people, characters or titles. */
internal object FilmiStyle : CardStyle {
    private val ROLES = listOf("Veer (hero)", "Anaya (heroine)", "Kaal (villain)", "Taal Haveli (place)")
    private val MARKS = listOf("★", "♛", "☠", "⌂")
    private val COLORS = listOf(Color(0xFF1F6E96), Color(0xFF8A3FA0), Color(0xFF12171C), Color(0xFF2E7D57))

    override fun label(card: PlayCard): String =
        if (card.joker) {
            "TWIST"
        } else {
            IndianCards.rankName(card.rank) +
                MARKS[card.suit % 4]
        }

    override fun color(card: PlayCard): Color = if (card.joker) Color(0xFFB3541E) else COLORS[card.suit % 4]

    override fun describe(card: PlayCard): String =
        if (card.joker) "Plot twist (joker)" else "${ROLES[card.suit % 4]} · scene ${card.rank}"
}

@Composable
internal fun PlayingCard(
    card: PlayCard?,
    width: Dp,
    height: Dp,
    modifier: Modifier = Modifier,
    style: CardStyle = ClassicStyle,
    cursor: Boolean = false,
    picked: Boolean = false,
    faceDown: Boolean = false,
    onClick: (() -> Unit)? = null,
) {
    val colors = MaterialTheme.colorScheme
    val edge = if (cursor || picked) colors.primary else colors.outlineVariant
    val base = modifier.size(width, height)
    val tap = if (onClick != null) base.clickable(onClick = onClick) else base
    Surface(
        modifier = tap,
        shape = RoundedCornerShape(4.dp),
        color =
            when {
                card == null -> colors.surfaceVariant
                faceDown -> colors.primary.copy(alpha = 0.55f)
                picked -> Color(0xFFD9E8F2)
                else -> FaceColor
            },
        border = BorderStroke(if (cursor || picked) 2.dp else 1.dp, edge),
    ) {
        Box(Modifier.padding(horizontal = 2.dp, vertical = 1.dp)) {
            if (card != null && !faceDown) {
                Text(
                    style.label(card),
                    color = style.color(card),
                    fontSize = if (width < 30.dp) 10.sp else 12.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                )
            }
        }
    }
}

/** A hand of cards, fanned in one row, or two rows when one row would squash the cards. */
@Composable
internal fun HandRow(
    cards: List<PlayCard>,
    cursor: Int,
    picked: Set<Int> = emptySet(),
    cardWidth: Dp,
    cardHeight: Dp,
    style: CardStyle = ClassicStyle,
    gaps: Set<Int> = emptySet(),
    showCursor: Boolean = true,
    faceDown: Boolean = false,
    onClick: (Int) -> Unit = {},
) {
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val avail = maxWidth
        val gapWidth = 6.dp

        fun rowFits(count: Int): Boolean = cardWidth * 0.55f * (count - 1) + cardWidth <= avail
        val rows =
            if (cards.size <= 1 || rowFits(cards.size)) {
                listOf(cards.indices.toList())
            } else {
                val half = (cards.size + 1) / 2
                listOf((0 until half).toList(), (half until cards.size).toList())
            }
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            rows.forEach { indices ->
                val extra = indices.count { it - 1 in gaps } * gapWidth.value
                val step =
                    if (indices.size > 1) {
                        minOf(cardWidth + 3.dp, (avail - cardWidth - extra.dp) / (indices.size - 1))
                    } else {
                        0.dp
                    }
                Box(Modifier.fillMaxWidth().height(cardHeight + 10.dp)) {
                    var x = 0.dp
                    indices.forEachIndexed { pos, i ->
                        if (pos > 0 && i - 1 in gaps) x += gapWidth
                        val isCursor = showCursor && i == cursor
                        PlayingCard(
                            card = cards[i],
                            width = cardWidth,
                            height = cardHeight,
                            style = style,
                            cursor = isCursor,
                            picked = i in picked,
                            faceDown = faceDown,
                            modifier =
                                Modifier
                                    .offset(
                                        x,
                                        if (isCursor ||
                                            i in picked
                                        ) {
                                            0.dp
                                        } else {
                                            8.dp
                                        },
                                    ).zIndex(if (isCursor) 2f else pos / 100f),
                            onClick = { onClick(i) },
                        )
                        x += step
                    }
                }
            }
        }
    }
}

/** The table or board background. */
@Composable
internal fun TableSurface(
    side: Dp,
    content: @Composable BoxScope.() -> Unit,
) {
    // In the games shell the card table is green felt with a brass rim; elsewhere it follows the theme.
    val felt = LocalGameShell.current != null
    Surface(
        modifier = Modifier.size(side),
        shape = MaterialTheme.shapes.large,
        color = if (felt) TableFelt else MaterialTheme.colorScheme.surface,
        border = if (felt) BorderStroke(4.dp, TableBrass) else BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        tonalElevation = 0.dp,
    ) {
        Box(Modifier.fillMaxSize().padding(6.dp)) { content() }
    }
}

/** One player's name and a short line under it; the player to act has a blue edge. */
@Composable
internal fun SeatLabel(
    name: String,
    detail: String,
    active: Boolean,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        color = colors.background,
        border = BorderStroke(if (active) 2.dp else 1.dp, if (active) colors.primary else colors.outlineVariant),
    ) {
        Column(
            Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                name,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = colors.onBackground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                detail,
                fontSize = 10.sp,
                color = colors.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/** A line of small text, such as the latest event or a hint. */
@Composable
internal fun NoteLine(
    text: String,
    modifier: Modifier = Modifier,
    bold: Boolean = false,
) {
    Text(
        text,
        modifier = modifier.fillMaxWidth(),
        fontSize = 12.sp,
        fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal,
        color = MaterialTheme.colorScheme.onSurface,
        textAlign = TextAlign.Center,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
    )
}

/** A row of choice buttons for the on-screen version of the game's keys; [selected] shows the key cursor. */
@Composable
internal fun ChoiceRow(
    labels: List<String>,
    selected: Int,
    enabled: List<Boolean> = labels.map { true },
    onPick: (Int) -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        labels.forEachIndexed { i, label ->
            val on = i == selected
            Surface(
                onClick = { if (enabled[i]) onPick(i) },
                modifier = Modifier.weight(1f).heightIn48(),
                shape = RoundedCornerShape(8.dp),
                color = if (on) colors.primary else colors.surface,
                contentColor = if (on) colors.onPrimary else colors.onSurface,
                border = BorderStroke(1.dp, if (on) colors.primary else colors.outlineVariant),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        label,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        color = if (enabled[i]) Color.Unspecified else colors.onSurfaceVariant.copy(alpha = 0.5f),
                    )
                }
            }
        }
    }
}

private fun Modifier.heightIn48(): Modifier = this.height(40.dp)

// ---------------------------------------------------------------- chips and menu

/** The practice chips the card games share. They are play money only and are never bought or sold. */
internal object ChipBank {
    const val START = 10_000
    private const val FILE = "indian_cards"
    private const val KEY = "chips"

    fun load(context: Context): Int =
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE).getInt(KEY, START).let {
            if (it <
                TeenPattiGame.BOOT * 4
            ) {
                START
            } else {
                it
            }
        }

    fun save(
        context: Context,
        chips: Int,
    ) {
        context
            .getSharedPreferences(FILE, Context.MODE_PRIVATE)
            .edit()
            .putInt(KEY, chips)
            .apply()
    }
}

internal data class MenuGame(
    val id: String,
    val title: String,
    val blurb: String,
    val mark: String,
)

internal val CARD_GAMES =
    listOf(
        MenuGame("teenpatti", "Teen Patti", "3-card brag · practice chips", "♠"),
        MenuGame("rummy", "Indian Rummy", "13 cards · sets and runs", "♥"),
        MenuGame("mendicot", "Mendicot", "Partner tricks · the tens", "♣"),
        MenuGame("callbreak", "Callbreak", "Bid and win · spades trump", "♠"),
        MenuGame("judgement", "Judgement", "Exact bids · changing trump", "♦"),
        MenuGame("235", "2-3-5", "Targets of 2, 3 and 5", "5"),
        MenuGame("bluff", "Bluff", "Lie, call or believe", "?"),
        MenuGame("donkey", "Donkey", "Four of a kind · grab a spoon", "D"),
        MenuGame("gulam", "Gulam Choor", "Dodge the odd Jack", "J"),
        MenuGame("filmi", "Filmi Rummy", "Rummy with film-set cards", "★"),
    )

internal val BOARD_GAMES =
    listOf(
        MenuGame("snakes", "Snakes & Ladders", "2 to 4 players · dice race", "S"),
        MenuGame("ludo", "Ludo", "4 colours · bring tokens home", "L"),
    )

/** The home screen of an Indian games app: pick a game with the arrows, Enter opens it. */
@Composable
internal fun GameMenu(
    title: String,
    games: List<MenuGame>,
    chips: Int?,
    tab: Int = 0,
    onSwitch: (() -> Unit)? = null,
    clockwise: Boolean? = null,
    onDirection: () -> Unit = {},
    onOpen: (String) -> Unit,
) {
    var cursor by remember(tab) { mutableIntStateOf(0) }
    GameKeyHandler(GameKind.Indian) { command ->
        when (command) {
            is GameCommand.Move -> {
                cursor = (cursor + command.dx + command.dy * 2).coerceIn(0, games.lastIndex)
            }

            GameCommand.Primary, GameCommand.Select -> {
                onOpen(games[cursor].id)
            }

            is GameCommand.Letter -> {
                when {
                    command.value == 'T' && onSwitch != null -> onSwitch()
                    command.value == 'D' && clockwise != null -> onDirection()
                    else -> return@GameKeyHandler false
                }
            }

            else -> {
                return@GameKeyHandler false
            }
        }
        true
    }
    val colors = MaterialTheme.colorScheme
    Surface(Modifier.fillMaxSize(), color = colors.background) {
        Column(Modifier.fillMaxSize().padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(title, style = MaterialTheme.typography.titleMedium, color = colors.onBackground)
                if (chips != null) Text("Practice chips: $chips", fontSize = 12.sp, color = colors.onSurfaceVariant)
            }
            if (onSwitch != null) {
                ChoiceRow(listOf("Card games", "Board games"), tab) { if (it != tab) onSwitch() }
            }
            if (clockwise != null && tab == 0) {
                Text(
                    "Play goes ${if (clockwise) "clockwise" else "anticlockwise"} round the table · D changes",
                    modifier = Modifier.fillMaxWidth().clickable(onClick = onDirection),
                    fontSize = 12.sp,
                    color = colors.onSurfaceVariant,
                )
            }
            val rows = (games.size + 1) / 2
            for (r in 0 until rows) {
                Row(Modifier.fillMaxWidth().weight(1f), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    for (c in 0..1) {
                        val i = r * 2 + c
                        if (i < games.size) {
                            MenuTile(games[i], i == cursor, Modifier.weight(1f).fillMaxSize()) {
                                cursor = i
                                onOpen(games[i].id)
                            }
                        } else {
                            Box(Modifier.weight(1f))
                        }
                    }
                }
            }
            val move = if (LocalDeviceProfile.current.letterMoves) "I J K L choose" else "Arrows choose"
            Text(
                if (onSwitch != null) {
                    "$move · Enter opens · T switches tab · Back returns"
                } else {
                    "$move · Enter opens · Back returns"
                },
                fontSize = 11.sp,
                color = colors.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun MenuTile(
    game: MenuGame,
    selected: Boolean,
    modifier: Modifier,
    onClick: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = colors.surface,
        border = BorderStroke(if (selected) 3.dp else 1.dp, if (selected) colors.primary else colors.outlineVariant),
    ) {
        Row(
            Modifier.padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Box(
                Modifier.size(40.dp).background(colors.primary, RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Text(game.mark, color = colors.onPrimary, fontWeight = FontWeight.Bold, fontSize = 20.sp)
            }
            Column(Modifier.width(1.dp).weight(1f)) {
                Text(
                    game.title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = colors.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    game.blurb,
                    fontSize = 11.sp,
                    color = colors.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

// ---------------------------------------------------------------- the two apps

/** A board game as an app of its own: no menu, the game opens at once. */
@Composable
internal fun BoardGameApp(id: String) {
    val random = remember { Random(System.nanoTime()) }
    when (id) {
        "snakes" -> SnakesScreen(random)
        "ludo" -> LudoScreen(random)
        else -> error("Unknown board game: $id")
    }
}

/**
 * An Indian games app: a menu of [cardGames], and a second tab of [boardGames] when there are any
 * (SableOS ships both in one app; Sable Games ships the card games alone).
 */
@Composable
internal fun IndianGamesApp(
    title: String = "Sable Indian Games",
    cardGames: List<MenuGame> = CARD_GAMES,
    boardGames: List<MenuGame> = BOARD_GAMES,
) {
    var open by remember { mutableStateOf<String?>(null) }
    var tab by remember { mutableIntStateOf(0) }
    val context = LocalContext.current
    var chips by remember { mutableIntStateOf(ChipBank.load(context)) }
    var clockwise by remember { mutableStateOf(TableDirection.load(context)) }
    val id = open
    // Under the games shell, Back from a game goes to the shell's title, and its menus own Back.
    BackHandler(enabled = id != null && LocalGameShell.current?.suspended != true) { open = null }
    val random = remember { Random(System.nanoTime()) }
    CompositionLocalProvider(LocalTableClockwise provides clockwise) {
        IndianGame(id, random, chips, onChips = {
            chips = it
            ChipBank.save(context, it)
        }, onClose = { open = null }) {
            GameMenu(
                title,
                if (tab == 0) cardGames else boardGames,
                chips,
                tab,
                if (boardGames.isEmpty()) null else ({ tab = 1 - tab }),
                clockwise = clockwise,
                onDirection = {
                    clockwise = !clockwise
                    TableDirection.save(context, clockwise)
                },
            ) { open = it }
        }
    }
}

/** The open game, or the [menu] when none is open. */
@Composable
private fun IndianGame(
    id: String?,
    random: Random,
    chips: Int,
    onChips: (Int) -> Unit,
    onClose: () -> Unit,
    menu: @Composable () -> Unit,
) {
    when (id) {
        null -> {
            menu()
        }

        "teenpatti" -> {
            TeenPattiScreen(random, chips, onChips)
        }

        "rummy" -> {
            RummyScreen(random, ClassicStyle, "Indian Rummy")
        }

        "filmi" -> {
            RummyScreen(random, FilmiStyle, "Filmi Rummy")
        }

        "callbreak" -> {
            TrickScreen(random, TrickVariant.Callbreak, "Callbreak")
        }

        "judgement" -> {
            TrickScreen(random, TrickVariant.Judgement, "Judgement")
        }

        "235" -> {
            TrickScreen(random, TrickVariant.TwoThreeFive, "2-3-5")
        }

        "mendicot" -> {
            TrickScreen(random, TrickVariant.Mendicot, "Mendicot")
        }

        "bluff" -> {
            BluffScreen(random)
        }

        "donkey" -> {
            DonkeyScreen(random)
        }

        "gulam" -> {
            GulamChoorScreen(random)
        }

        "snakes" -> {
            SnakesScreen(random)
        }

        "ludo" -> {
            LudoScreen(random)
        }

        else -> {
            onClose()
        }
    }
}
