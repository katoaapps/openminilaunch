package org.sableos.games

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

/**
 * How fast the phone players move in the card games: slow enough to follow each card as it lands
 * (owner request 2026-11-14: the earlier 650 ms per card and 1 s per trick were too quick to read).
 */
internal object IndianPace {
    /** A phone player's card, or move in Bluff and Gulam Choor. */
    const val CARD_MS = 1200L

    /** A phone player's bid or trump choice. */
    const val BID_MS = 900L

    /** A full trick stays on the table this long before it is collected. */
    const val TRICK_HOLD_MS = 2400L

    /** A played card slides onto the table over this time. */
    const val SLIDE_MS = 450
}

/**
 * Slides [content] in from the [from] side (a unit vector, the player's seat) and fades it in, once for
 * each [key].
 */
@Composable
internal fun ArrivingCard(
    key: Int,
    from: Pair<Float, Float> = 0f to -1f,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val progress = remember(key) { Animatable(0f) }
    LaunchedEffect(key) {
        progress.animateTo(1f, tween(IndianPace.SLIDE_MS, easing = FastOutSlowInEasing))
    }
    val travel = with(LocalDensity.current) { 48.dp.toPx() }
    Box(
        modifier.graphicsLayer {
            alpha = progress.value
            translationX = (1f - progress.value) * from.first * travel
            translationY = (1f - progress.value) * from.second * travel
        },
    ) { content() }
}

/** Which way play goes round the table; the card games read it to place the seats. */
internal val LocalTableClockwise = staticCompositionLocalOf { true }

/** The direction of play the card games share, kept on the phone. Clockwise unless changed. */
internal object TableDirection {
    private const val FILE = "indian_cards"
    private const val KEY = "clockwise"

    fun load(context: Context): Boolean = context.getSharedPreferences(FILE, Context.MODE_PRIVATE).getBoolean(KEY, true)

    fun save(
        context: Context,
        clockwise: Boolean,
    ) {
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE).edit().putBoolean(KEY, clockwise).apply()
    }
}

/**
 * Where seat [player] sits round a table of [players], as a unit vector from the centre (x right, y
 * down). You (player 0) sit at the bottom; play goes 0, 1, 2, ... so with [clockwise] player 1 sits on
 * your left, and otherwise on your right.
 */
internal fun seatDirection(
    player: Int,
    players: Int,
    clockwise: Boolean,
): Pair<Float, Float> {
    val step = 2 * PI / players * (if (clockwise) 1 else -1)
    val angle = PI / 2 + step * player
    return cos(angle).toFloat().snap() to sin(angle).toFloat().snap()
}

private fun Float.snap(): Float = if (abs(this) < 1e-4f) 0f else this

private val SEAT_WIDTH = 92.dp
private val SEAT_HEIGHT = 36.dp

/**
 * The phone players round a table, as people sit: you at the bottom (your hand is under the table),
 * the others round the edge in the order they play ([LocalTableClockwise]). [center] fills the
 * middle, clear of the seats.
 */
@Composable
internal fun RoundTable(
    players: Int,
    modifier: Modifier = Modifier,
    seatHeight: Dp = SEAT_HEIGHT,
    seat: @Composable (player: Int, width: Dp) -> Unit,
    center: @Composable BoxScope.() -> Unit,
) {
    val clockwise = LocalTableClockwise.current
    BoxWithConstraints(modifier) {
        val seatWidth = minOf(SEAT_WIDTH, maxWidth * 0.3f)
        val rx = (maxWidth - seatWidth) / 2
        val ry = (maxHeight - seatHeight) / 2
        Box(
            Modifier
                .fillMaxSize()
                .padding(horizontal = seatWidth / 2, vertical = seatHeight / 2)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.06f), RoundedCornerShape(50)),
        )
        Box(
            Modifier.fillMaxSize().padding(start = seatWidth + 4.dp, end = seatWidth + 4.dp, top = seatHeight + 4.dp),
            contentAlignment = Alignment.Center,
            content = center,
        )
        for (p in 1 until players) {
            val (x, y) = seatDirection(p, players, clockwise)
            Box(Modifier.align(Alignment.Center).offset(rx * x, ry * y)) { seat(p, seatWidth) }
        }
    }
}

/** The cards on the table, each in front of the player who played it, sliding in from that seat. */
@Composable
internal fun TableCards(
    cards: List<Pair<Int, PlayCard>>,
    players: Int,
    names: List<String>,
    cardWidth: Dp,
    cardHeight: Dp,
    keyBase: Int,
) {
    val clockwise = LocalTableClockwise.current
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val rx = ((maxWidth - cardWidth) / 2).coerceAtLeast(0.dp) * 0.75f
        val ry = ((maxHeight - cardHeight) / 2).coerceAtLeast(0.dp) * 0.6f
        cards.forEachIndexed { i, (who, card) ->
            val from = seatDirection(who, players, clockwise)
            ArrivingCard(
                key = keyBase + i,
                from = from,
                modifier = Modifier.align(Alignment.Center).offset(rx * from.first, ry * from.second),
            ) {
                PlayedCard(card, names[who], cardWidth, cardHeight)
            }
        }
    }
}

/**
 * A card on the table with its player's name under it. The name is only as wide as the card: a
 * full-width line here would take the whole row and squeeze the other players' cards out of sight.
 */
@Composable
internal fun PlayedCard(
    card: PlayCard,
    name: String,
    cardWidth: Dp,
    cardHeight: Dp,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        PlayingCard(card, cardWidth, cardHeight)
        Text(
            name,
            modifier = Modifier.width(cardWidth + 16.dp),
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
