package org.sableos.games

import androidx.compose.animation.core.animate
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/** The board games' pace, matched to the card games (owner, 2026-11-14: 1.2 s a phone player's turn). */
internal object BoardPace {
    /** From the end of one turn to a phone player's dice showing its number. */
    const val TURN_MS = IndianPace.CARD_MS

    /** The dice tumbles this long before it shows the roll. */
    const val TUMBLE_MS = 500L

    /** One square of a token's walk. */
    const val STEP_MS = 180

    /** A ladder, a snake, or a captured token going back to its yard. */
    const val JUMP_MS = 650

    /** A phone player in Ludo picks a token this long after its dice shows. */
    const val PICK_MS = 600L

    /** The wait before a phone player's dice starts to tumble. */
    const val THINK_MS = TURN_MS - TUMBLE_MS

    private const val FACE_MS = 70L

    /** Shows random faces through [show] for [TUMBLE_MS], then the roll. */
    suspend fun tumble(
        roll: Int,
        show: (Int) -> Unit,
    ) {
        repeat((TUMBLE_MS / FACE_MS).toInt()) {
            show(Random.nextInt(1, 7))
            delay(FACE_MS)
        }
        show(roll)
    }

    /** Runs [frame] from 0 to 1 over [ms]. */
    suspend fun glide(
        ms: Int,
        frame: (Float) -> Unit,
    ) {
        animate(0f, 1f, animationSpec = tween(ms)) { value, _ -> frame(value) }
        frame(1f)
    }
}

/** Ruby, emerald, amber and sapphire: player 0 (you) is always the first. */
internal val TOKEN_COLORS = listOf(Color(0xFFD93A3A), Color(0xFF2E9A55), Color(0xFFE8A923), Color(0xFF2A6FD6))

/** What happened, newest last, in the colour of the player who did it. */
internal data class MoveLine(
    val text: String,
    val player: Int,
)

/** Keeps the last two lines. */
internal fun List<MoveLine>.plusLine(line: MoveLine): List<MoveLine> = (this + line).takeLast(2)

/**
 * A token: a shaded disc in [color] with a white rim. [you] marks your own tokens with a white star, so
 * they stand apart from the phone players' tokens of every colour.
 */
internal fun DrawScope.drawToken(
    at: Offset,
    radius: Float,
    color: Color,
    you: Boolean,
    alpha: Float = 1f,
) {
    drawCircle(Color.Black.copy(alpha = 0.25f * alpha), radius, Offset(at.x + radius * 0.12f, at.y + radius * 0.18f))
    drawCircle(
        Brush.radialGradient(
            listOf(lighten(color, 0.45f), color, darken(color, 0.25f)),
            center = Offset(at.x - radius * 0.35f, at.y - radius * 0.35f),
            radius = radius * 1.6f,
        ),
        radius,
        at,
        alpha = alpha,
    )
    drawCircle(Color.White.copy(alpha = alpha), radius, at, style = Stroke(radius * 0.16f))
    if (you) drawStar(at, radius * 0.62f, Color.White.copy(alpha = alpha))
}

/** A five-pointed star centred on [at]. */
internal fun DrawScope.drawStar(
    at: Offset,
    radius: Float,
    color: Color,
) {
    val path = Path()
    for (i in 0 until 10) {
        val r = if (i % 2 == 0) radius else radius * 0.45f
        val a = -PI / 2 + i * PI / 5
        val x = at.x + (r * cos(a)).toFloat()
        val y = at.y + (r * sin(a)).toFloat()
        if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
    }
    path.close()
    drawPath(path, color)
}

/** A dice face of [value] (1-6) in a [side]-wide square at [topLeft], tinted with the roller's [color]. */
internal fun DrawScope.drawDice(
    topLeft: Offset,
    side: Float,
    value: Int,
    color: Color,
) {
    val corner = CornerRadius(side * 0.2f)
    drawRoundRect(
        Color.Black.copy(alpha = 0.25f),
        Offset(topLeft.x + side * 0.05f, topLeft.y + side * 0.07f),
        Size(side, side),
        corner,
    )
    drawRoundRect(Color.White, topLeft, Size(side, side), corner)
    drawRoundRect(color, topLeft, Size(side, side), corner, style = Stroke(side * 0.08f))
    val pip = side * 0.09f

    fun dot(
        fx: Float,
        fy: Float,
    ) = drawCircle(color, pip, Offset(topLeft.x + side * fx, topLeft.y + side * fy))
    val l = 0.27f
    val m = 0.5f
    val h = 0.73f
    if (value % 2 == 1) dot(m, m)
    if (value >= 2) {
        dot(l, l)
        dot(h, h)
    }
    if (value >= 4) {
        dot(h, l)
        dot(l, h)
    }
    if (value == 6) {
        dot(l, m)
        dot(h, m)
    }
}

/** A dice on its own, outside a board. */
@Composable
internal fun DiceFace(
    value: Int?,
    color: Color,
    size: Dp,
) {
    Canvas(Modifier.size(size)) {
        val side = this.size.width * 0.86f
        if (value == null) {
            drawRoundRect(
                color.copy(alpha = 0.25f),
                Offset.Zero,
                Size(side, side),
                CornerRadius(side * 0.2f),
                style = Stroke(2f),
            )
        } else {
            drawDice(Offset.Zero, side, value, color)
        }
    }
}

/**
 * A player's seat: a token in their colour (with your star), their name and [detail]. The player whose
 * turn it is gets a bright outline; the others are dimmed while they wait.
 */
@Composable
internal fun BoardSeat(
    player: Int,
    name: String,
    detail: String,
    active: Boolean,
    modifier: Modifier = Modifier,
) {
    val color = TOKEN_COLORS[player]
    val scheme = MaterialTheme.colorScheme
    // One line, so a large font size on the Titan 2 cannot cut the detail off (owner photo, 2026-11-14).
    Row(
        modifier
            .heightIn(min = 30.dp)
            .background(color.copy(alpha = if (active) 0.22f else 0.08f), RoundedCornerShape(8.dp))
            .border(
                if (active) 2.dp else 1.dp,
                if (active) color else scheme.outline.copy(alpha = 0.3f),
                RoundedCornerShape(8.dp),
            ).padding(horizontal = 4.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Canvas(Modifier.size(16.dp)) { drawToken(center, size.width * 0.42f, color, you = player == 0) }
        Text(
            "$name · $detail",
            fontSize = 12.sp,
            lineHeight = 14.sp,
            fontWeight = if (active) FontWeight.Bold else FontWeight.Normal,
            color = scheme.onSurface.copy(alpha = if (active) 1f else 0.7f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/**
 * The last two things that happened, the older one faded, each in its player's colour (lightened on a
 * dark theme so blue stays readable). Each line has its own fixed height, so neither is cut off.
 */
@Composable
internal fun MoveLines(
    lines: List<MoveLine>,
    modifier: Modifier = Modifier,
) {
    val dark = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(2.dp)) {
        List(2) { lines.getOrNull(lines.size - 2 + it) }.forEachIndexed { i, line ->
            val newest = i == 1
            val color = TOKEN_COLORS[line?.player ?: 0].let { if (dark) lighten(it, 0.3f) else darken(it, 0.1f) }
            Text(
                line?.text ?: "",
                modifier = Modifier.height(if (newest) 20.dp else 18.dp),
                fontSize = if (newest) 13.sp else 11.sp,
                lineHeight = if (newest) 16.sp else 14.sp,
                fontWeight = if (newest) FontWeight.Medium else FontWeight.Normal,
                color = color.copy(alpha = if (newest) 1f else 0.6f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

internal fun lighten(
    color: Color,
    by: Float,
): Color =
    Color(
        color.red + (1 - color.red) * by,
        color.green + (1 - color.green) * by,
        color.blue + (1 - color.blue) * by,
        color.alpha,
    )

internal fun darken(
    color: Color,
    by: Float,
): Color = Color(color.red * (1 - by), color.green * (1 - by), color.blue * (1 - by), color.alpha)

/** "1st", "2nd", "3rd", "4th". */
internal fun ordinal(n: Int): String =
    n.toString() +
        when (n) {
            1 -> "st"
            2 -> "nd"
            3 -> "rd"
            else -> "th"
        }
