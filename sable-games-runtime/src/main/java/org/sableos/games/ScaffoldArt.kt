package org.sableos.games

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import kotlin.math.sin

/** The colours of Sable Scaffold: a building site at dusk. */
internal object ScaffoldColors {
    val Night = Color(0xFF121A38)
    val Dusk = Color(0xFF2D2B5A)
    val Glow = Color(0xFFC8603C)
    val Skyline = Color(0xFF1C2346)
    val Lattice = Color(0xFF3B4672)
    val Hazard = Color(0xFFFFC72C)
    val HazardDark = Color(0xFF141A2E)
    val Vest = Color(0xFFFF6B2C)
    val Teal = Color(0xFF2FD0B5)
    val Steel = Color(0xFF9FB0C4)
    val Plank = Color(0xFFC98F4E)
    val PlankTop = Color(0xFFE0AA6A)
    val PlankEdge = Color(0xFF8A5A2B)
    val Concrete = Color(0xFF8D8F9C)
    val ConcreteTop = Color(0xFFB5B8C6)
    val ConcreteBlock = Color(0xFF9A9CAB)
    val Column = Color(0xFF5D6178)
    val Ground = Color(0xFF44475A)
    val Hud = Color(0xFF0C1020)
    val Heart = Color(0xFFFF5A4D)
    val White = Color(0xFFFFFFFF)
}

/** Shapes that never change for a level, built once. */
internal class ScaffoldArt(
    val level: ScaffoldLevel,
) {
    val skyline: Path =
        PathParser()
            .parsePathString(
                "M0 672V430h40v-70h46v90h34v-130h56v150h40v-80h54v100h40v-170h50v170h56v-110h50v120h44v-70h60h150V672z",
            ).toPath()
    val heart: Path =
        PathParser()
            .parsePathString("M12 21s-7-4.4-9.5-9A5.5 5.5 0 0 1 12 6a5.5 5.5 0 0 1 9.5 6c-2.5 4.6-9.5 9-9.5 9z")
            .toPath()
    val lattice: Path = Path()

    /** How far down each surface's support goes: to the next surface below it, or the ground. */
    val supportBottom: List<Float> =
        level.surfaces.map { s ->
            val mid = (s.x0 + s.x1) / 2
            level.surfaces
                .filter { it !== s && mid >= it.x0 && mid <= it.x1 && it.y > s.y + 20f }
                .minOfOrNull { it.y } ?: ScaffoldField.GROUND_Y
        }

    init {
        if (level.style == BuildStyle.Timber) {
            level.surfaces.forEachIndexed { i, s ->
                if (!s.solid) addLattice(s.x0, s.y + DECK, s.x1, supportBottom[i])
            }
        }
        if (level.pendulum != null) {
            addLattice(MAST_X0, MAST_Y0, MAST_X1, ScaffoldField.GROUND_Y)
            addLattice(JIB_X0, JIB_Y0, JIB_X1, JIB_Y1)
        }
    }

    private fun addLattice(
        x0: Float,
        y0: Float,
        x1: Float,
        y1: Float,
    ) {
        var y = y0
        while (y + CELL <= y1 + 0.5f) {
            var x = x0
            while (x + CELL <= x1 + 0.5f) {
                lattice.moveTo(x, y)
                lattice.lineTo(x + CELL, y + CELL)
                lattice.moveTo(x + CELL, y)
                lattice.lineTo(x, y + CELL)
                x += CELL
            }
            y += CELL
        }
    }

    companion object {
        const val DECK = 14f
        const val CELL = 30f
        const val MAST_X0 = 460f
        const val MAST_X1 = 510f
        const val MAST_Y0 = 64f
        const val JIB_X0 = 60f
        const val JIB_X1 = 700f
        const val JIB_Y0 = 40f
        const val JIB_Y1 = 64f
    }
}

/** What the status bar and the banner need besides the world. */
internal data class ScaffoldHud(
    val title: String,
    val bonusLabel: String,
    val scoreLabel: String,
    val help: String,
)

/**
 * Draws the whole 720 x 720 board, scaled to the canvas: the status bar on top and the field below.
 * Called every frame, so it allocates as little as it can.
 */
internal fun DrawScope.drawScaffold(
    art: ScaffoldArt,
    world: ScaffoldWorld,
    hud: ScaffoldHud,
    measurer: TextMeasurer,
) {
    val s = size.width / ScaffoldField.WIDTH
    drawRect(ScaffoldColors.Night)
    withTransform({ scale(s, s, Offset.Zero) }) {
        drawHud(art, world, hud, measurer)
        withTransform({ translate(0f, ScaffoldField.HUD) }) {
            drawField(art, world, hud, measurer)
        }
    }
}

private fun DrawScope.drawHud(
    art: ScaffoldArt,
    world: ScaffoldWorld,
    hud: ScaffoldHud,
    measurer: TextMeasurer,
) {
    drawRect(ScaffoldColors.Hud, size = Size(ScaffoldField.WIDTH, ScaffoldField.HUD))
    var x = 14f
    repeat(ScaffoldWorld.START_HEARTS) { i ->
        withTransform({ translate(x, 12f) }) {
            if (i < world.hearts) {
                drawPath(art.heart, ScaffoldColors.Heart)
            } else {
                drawPath(art.heart, ScaffoldColors.Heart, style = Stroke(2f))
            }
        }
        x += 26f
    }
    x += 6f
    repeat(world.hats) {
        drawHat(x + 11f, 30f, 1f)
        x += 26f
    }
    val bold = TextStyle(color = ScaffoldColors.White, fontSize = textUnits(14f), fontWeight = FontWeight.Bold)
    val title = measurer.measure(hud.title, bold)
    drawText(title, topLeft = Offset(ScaffoldField.WIDTH / 2 - title.size.width / 2f, 14f))
    val bonus = measurer.measure(hud.bonusLabel, bold.copy(color = ScaffoldColors.Hazard))
    drawText(bonus, topLeft = Offset(ScaffoldField.WIDTH - 232f, 14f))
    val score = measurer.measure(hud.scoreLabel, bold)
    drawText(score, topLeft = Offset(ScaffoldField.WIDTH - 14f - score.size.width, 14f))
}

private const val PIP_SCALE = 1.1f
private const val JUNO_SCALE = 1.4f
private const val CLUNK_SCALE = 1.3f
private const val HAZARD_SCALE = 1.5f
private const val BOLT_SCALE = 2.6f

/** [size] is in field units, which are pixels before the board's scale is applied. */
private fun DrawScope.textUnits(size: Float) = size.toSp()

private fun DrawScope.drawField(
    art: ScaffoldArt,
    world: ScaffoldWorld,
    hud: ScaffoldHud,
    measurer: TextMeasurer,
) {
    val level = art.level
    drawRect(
        Brush.verticalGradient(
            listOf(ScaffoldColors.Night, ScaffoldColors.Dusk, ScaffoldColors.Glow),
            startY = 0f,
            endY = ScaffoldField.HEIGHT,
        ),
        size = Size(ScaffoldField.WIDTH, ScaffoldField.HEIGHT),
    )
    drawPath(art.skyline, ScaffoldColors.Skyline)
    drawPath(art.lattice, ScaffoldColors.Lattice, alpha = 0.8f, style = Stroke(2f))
    if (level.pendulum != null) drawCrane(level.pendulum, world)
    drawSurfaces(art)
    level.lifts.forEachIndexed { i, lift -> drawLift(lift, world.liftY[i]) }
    level.ladders.forEach { drawLadder(it) }
    level.bolts.forEachIndexed { i, b -> if (!world.boltTaken[i]) drawBolt(b.x, b.y) }
    level.hats.forEachIndexed { i, h -> if (!world.hatTaken[i]) drawHat(h.x, h.y, 1.9f) }
    drawJuno(level, world, hud, measurer)
    drawClunk(level, world)
    world.hazards.forEach { h ->
        if (h.kind == HazardKind.Brick) {
            drawHazard(h)
        } else {
            withTransform({ scale(HAZARD_SCALE, HAZARD_SCALE, Offset(h.x, h.y)) }) { drawHazard(h) }
        }
    }
    level.pendulum?.let { drawBall(it, world) }
    val flicker = world.invulnerable > 0f && (world.time * 14f).toInt() % 2 == 0
    if (!flicker) drawPip(world)
    if (world.time < BANNER_SECONDS) drawBanner(level, world.time, measurer)
}

private const val BANNER_SECONDS = 2.2f

private fun DrawScope.drawBanner(
    level: ScaffoldLevel,
    time: Float,
    measurer: TextMeasurer,
) {
    val alpha = (1f - (time - 1.4f).coerceAtLeast(0f) / (BANNER_SECONDS - 1.4f)).coerceIn(0f, 1f)
    drawRoundRect(
        ScaffoldColors.Hud.copy(alpha = 0.85f * alpha),
        Offset(120f, 300f),
        Size(480f, 70f),
        CornerRadius(14f),
    )
    val text =
        measurer.measure(
            "LEVEL ${level.number} · ${level.name.uppercase()}",
            TextStyle(
                color = ScaffoldColors.Hazard.copy(alpha = alpha),
                fontSize = textUnits(26f),
                fontWeight = FontWeight.Bold,
            ),
        )
    drawText(text, topLeft = Offset(360f - text.size.width / 2f, 335f - text.size.height / 2f))
}

private fun DrawScope.drawSurfaces(art: ScaffoldArt) {
    val concrete = art.level.style == BuildStyle.Concrete
    art.level.surfaces.forEachIndexed { i, s ->
        val bottom = art.supportBottom[i]
        when {
            s.y == ScaffoldField.GROUND_Y -> {
                drawGround(s)
            }

            s.solid -> {
                drawRect(ScaffoldColors.ConcreteBlock, Offset(s.x0, s.y), Size(s.x1 - s.x0, bottom - s.y))
                drawRect(ScaffoldColors.ConcreteTop, Offset(s.x0, s.y), Size(s.x1 - s.x0, 3f))
            }

            concrete -> {
                var cx = s.x0 + 40f
                while (cx < s.x1 - 8f) {
                    drawRect(ScaffoldColors.Column, Offset(cx, s.y + 16f), Size(16f, bottom - s.y - 16f))
                    cx += 240f
                }
                drawRect(ScaffoldColors.Concrete, Offset(s.x0, s.y), Size(s.x1 - s.x0, 16f))
                drawRect(ScaffoldColors.ConcreteTop, Offset(s.x0, s.y), Size(s.x1 - s.x0, 3f))
            }

            else -> {
                drawPlank(s)
            }
        }
    }
}

private fun DrawScope.drawGround(s: Surface) {
    drawRect(ScaffoldColors.Ground, Offset(s.x0, s.y), Size(s.x1 - s.x0, ScaffoldField.HEIGHT - s.y))
    stripes(s.x0, s.y, s.x1, 6f)
}

private fun DrawScope.stripes(
    x0: Float,
    y: Float,
    x1: Float,
    height: Float,
) {
    var x = x0
    var yellow = true
    while (x < x1) {
        val w = minOf(14f, x1 - x)
        drawRect(if (yellow) ScaffoldColors.Hazard else ScaffoldColors.HazardDark, Offset(x, y), Size(w, height))
        x += w
        yellow = !yellow
    }
}

private fun DrawScope.drawPlank(s: Surface) {
    val w = s.x1 - s.x0
    drawRect(ScaffoldColors.Plank, Offset(s.x0, s.y), Size(w, ScaffoldArt.DECK))
    drawRect(ScaffoldColors.PlankTop, Offset(s.x0, s.y), Size(w, 3f))
    drawRect(ScaffoldColors.PlankEdge, Offset(s.x0, s.y + 11f), Size(w, 3f))
    var x = s.x0 + 24f
    while (x < s.x1) {
        drawRect(ScaffoldColors.PlankEdge, Offset(x, s.y), Size(1.5f, ScaffoldArt.DECK))
        x += 24f
    }
}

private fun DrawScope.drawLadder(l: Ladder) {
    val rail = 3f
    drawRect(ScaffoldColors.Steel, Offset(l.x - 10f, l.top), Size(rail, l.bottom - l.top))
    drawRect(ScaffoldColors.Steel, Offset(l.x + 7f, l.top), Size(rail, l.bottom - l.top))
    var y = l.top + 5f
    while (y < l.bottom - 3f) {
        drawRect(Color(0xFFC9D4E2), Offset(l.x - 7f, y), Size(14f, 3f))
        y += 14f
    }
}

private fun DrawScope.drawLift(
    lift: Lift,
    y: Float,
) {
    val top = lift.stops.min()
    val bottom = lift.stops.max()
    drawRect(Color(0xFF0F1530), Offset(lift.x0, top), Size(lift.x1 - lift.x0, bottom - top), alpha = 0.6f)
    drawRect(ScaffoldColors.Steel, Offset(lift.x0 - 1f, top), Size(3f, bottom - top))
    drawRect(ScaffoldColors.Steel, Offset(lift.x1 - 2f, top), Size(3f, bottom - top))
    stripes(lift.x0, y, lift.x1, 10f)
    val mid = (lift.x0 + lift.x1) / 2
    val arrow = Path()
    arrow.moveTo(mid, y - 34f)
    arrow.lineTo(mid - 8f, y - 22f)
    arrow.lineTo(mid + 8f, y - 22f)
    arrow.close()
    arrow.moveTo(mid, y + 34f)
    arrow.lineTo(mid - 8f, y + 22f)
    arrow.lineTo(mid + 8f, y + 22f)
    arrow.close()
    drawPath(arrow, ScaffoldColors.Hazard)
}

private fun DrawScope.drawCrane(
    p: Pendulum,
    world: ScaffoldWorld,
) {
    drawRect(
        ScaffoldColors.Steel,
        Offset(ScaffoldArt.JIB_X0, ScaffoldArt.JIB_Y1 - 2f),
        Size(ScaffoldArt.JIB_X1 - ScaffoldArt.JIB_X0, 4f),
    )
    drawRoundRect(ScaffoldColors.Hazard, Offset(p.pivot.x - 12f, ScaffoldArt.JIB_Y1), Size(24f, 14f), CornerRadius(2f))
    val ghost = ScaffoldWorld.ballAt(p, world.time + p.periodSeconds / 2)
    drawLine(ScaffoldColors.Steel, Offset(p.pivot.x, p.pivot.y), Offset(ghost.x, ghost.y), 2f, alpha = 0.25f)
    drawCircle(Color(0xFF4B5370), p.radius, Offset(ghost.x, ghost.y), alpha = 0.3f)
}

private fun DrawScope.drawBall(
    p: Pendulum,
    world: ScaffoldWorld,
) {
    val c = world.ballPosition ?: return
    drawLine(Color(0xFFC9D4E2), Offset(p.pivot.x, p.pivot.y), Offset(c.x, c.y), 3f)
    drawCircle(Color(0xFF2B3150), p.radius, Offset(c.x, c.y))
    drawCircle(ScaffoldColors.Steel, p.radius, Offset(c.x, c.y), style = Stroke(3f))
}

private fun DrawScope.drawBolt(
    x: Float,
    y: Float,
) {
    withTransform({ scale(BOLT_SCALE, BOLT_SCALE, Offset(x, y)) }) { drawBoltShape(x, y) }
}

private fun DrawScope.drawBoltShape(
    x: Float,
    y: Float,
) {
    val hex = Path()
    hex.moveTo(x, y - 7f)
    hex.lineTo(x + 6f, y - 3.5f)
    hex.lineTo(x + 6f, y + 3.5f)
    hex.lineTo(x, y + 7f)
    hex.lineTo(x - 6f, y + 3.5f)
    hex.lineTo(x - 6f, y - 3.5f)
    hex.close()
    drawPath(hex, ScaffoldColors.Hazard)
    drawPath(hex, Color(0xFF8A5A00), style = Stroke(1.6f))
    drawCircle(Color(0xFF8A5A00), 2.6f, Offset(x, y))
    drawCircle(Color(0xFFFFE9A0), 1.2f, Offset(x - 1.6f, y - 3.4f))
}

/** A teal hard hat with its brim at [y]; [size] 1 is the HUD size. */
private fun DrawScope.drawHat(
    x: Float,
    y: Float,
    size: Float,
) {
    withTransform({
        translate(x, y)
        scale(size, size, Offset.Zero)
    }) {
        drawArc(
            ScaffoldColors.Teal,
            startAngle = 180f,
            sweepAngle = 180f,
            useCenter = true,
            topLeft = Offset(-10f, -10f),
            size = Size(20f, 20f),
        )
        drawRoundRect(Color(0xFF1E9C86), Offset(-12f, -1f), Size(24f, 3f), CornerRadius(1.5f))
    }
}

private fun DrawScope.drawPip(world: ScaffoldWorld) {
    val steps = (world.time * 10f).toInt() % 2
    val moving = world.moving
    val climbing = world.ladder != null
    withTransform({
        translate(world.heroX, world.heroY)
        scale(PIP_SCALE * world.facing, PIP_SCALE, Offset.Zero)
    }) {
        val legShift = if (moving && !climbing) (if (steps == 0) 2f else -2f) else 0f
        val navy = Color(0xFF2A3B7A)
        val boot = Color(0xFF3B2A20)
        drawRoundRect(
            navy,
            Offset(-8f, -14f + legShift.coerceAtLeast(0f)),
            Size(7f, 14f - legShift.coerceAtLeast(0f)),
            CornerRadius(2f),
        )
        drawRoundRect(
            navy,
            Offset(1f, -14f - legShift.coerceAtMost(0f)),
            Size(7f, 14f + legShift.coerceAtMost(0f)),
            CornerRadius(2f),
        )
        drawRoundRect(boot, Offset(-10f, -3f), Size(9f, 4f), CornerRadius(2f))
        drawRoundRect(boot, Offset(1f, -3f), Size(9f, 4f), CornerRadius(2f))
        drawRoundRect(ScaffoldColors.Vest, Offset(-10f, -30f), Size(20f, 18f), CornerRadius(5f))
        drawRect(Color(0xFFFFE27A), Offset(-10f, -23f), Size(20f, 3f))
        val glove = ScaffoldColors.Teal
        if (climbing) {
            val up = if (steps == 0) -42f else -36f
            drawCircle(glove, 4f, Offset(-11f, up))
            drawCircle(glove, 4f, Offset(11f, if (steps == 0) -36f else -42f))
        } else {
            drawCircle(glove, 4f, Offset(-11f, -20f))
            drawCircle(glove, 4f, Offset(11f, -20f))
        }
        drawCircle(Color(0xFFF2B88C), 9f, Offset(0f, -38f))
        drawArc(
            ScaffoldColors.Hazard,
            startAngle = 180f,
            sweepAngle = 180f,
            useCenter = true,
            topLeft = Offset(-10f, -50f),
            size = Size(20f, 20f),
        )
        drawRoundRect(Color(0xFFE0A800), Offset(-12f, -41f), Size(24f, 3f), CornerRadius(1.5f))
        if (!climbing) drawCircle(ScaffoldColors.HazardDark, 1.5f, Offset(3f, -37f))
        if (world.hats > 0) drawHatRing()
    }
}

/** A faint teal outline round Pip while a hard hat is in reserve. */
private fun DrawScope.drawHatRing() {
    drawRoundRect(
        ScaffoldColors.Teal,
        Offset(-14f, -52f),
        Size(28f, 54f),
        CornerRadius(12f),
        alpha = 0.35f,
        style = Stroke(1.5f),
    )
}

private fun DrawScope.drawJuno(
    level: ScaffoldLevel,
    world: ScaffoldWorld,
    hud: ScaffoldHud,
    measurer: TextMeasurer,
) {
    val wave = sin(world.time * 6f) * 3f
    withTransform({
        translate(level.juno.x, level.juno.y)
        scale(JUNO_SCALE, JUNO_SCALE, Offset.Zero)
    }) {
        val pants = Color(0xFF3A3F66)
        drawRect(pants, Offset(-6f, -14f), Size(5f, 14f))
        drawRect(pants, Offset(1f, -14f), Size(5f, 14f))
        val skin = Color(0xFFC98A60)
        drawLine(skin, Offset(-9f, -27f), Offset(-17f, -42f + wave), 4f)
        drawLine(skin, Offset(9f, -27f), Offset(17f, -42f - wave), 4f)
        drawRoundRect(Color(0xFFF4F0E6), Offset(-9f, -30f), Size(18f, 17f), CornerRadius(5f))
        drawRect(ScaffoldColors.Teal, Offset(-9f, -22f), Size(18f, 3f))
        drawRoundRect(Color(0xFF2A1A14), Offset(-9f, -46f), Size(18f, 17f), CornerRadius(8f))
        drawCircle(skin, 7f, Offset(0f, -37f))
        drawArc(
            ScaffoldColors.Teal,
            startAngle = 180f,
            sweepAngle = 180f,
            useCenter = true,
            topLeft = Offset(-9f, -48f),
            size = Size(18f, 18f),
        )
    }
    if (world.state == WorldState.Playing) {
        val text =
            measurer.measure(
                hud.help,
                TextStyle(color = ScaffoldColors.HazardDark, fontSize = textUnits(19f), fontWeight = FontWeight.Bold),
            )
        val w = text.size.width + 24f
        val x = (level.juno.x - w / 2).coerceIn(2f, ScaffoldField.WIDTH - w - 2f)
        drawRoundRect(ScaffoldColors.White, Offset(x, level.juno.y - 112f), Size(w, 34f), CornerRadius(12f))
        drawBubbleTip(level.juno.x, level.juno.y - 79f)
        drawText(text, topLeft = Offset(x + 12f, level.juno.y - 112f + (34f - text.size.height) / 2))
    }
}

private fun DrawScope.drawBubbleTip(
    x: Float,
    y: Float,
) {
    val tip = Path()
    tip.moveTo(x - 6f, y - 1f)
    tip.lineTo(x - 2f, y + 12f)
    tip.lineTo(x + 8f, y - 1f)
    tip.close()
    drawPath(tip, ScaffoldColors.White)
}

private fun DrawScope.drawClunk(
    level: ScaffoldLevel,
    world: ScaffoldWorld,
) {
    val flip = if (level.clunkFlip) -1f else 1f
    val raised = world.throwAnim > 0f
    withTransform({
        translate(level.clunk.x, level.clunk.y)
        scale(flip * CLUNK_SCALE, CLUNK_SCALE, Offset.Zero)
    }) {
        val dark = Color(0xFF2B3150)
        drawRoundRect(dark, Offset(-26f, -8f), Size(52f, 8f), CornerRadius(4f))
        listOf(-18f, 0f, 18f).forEach { drawCircle(Color(0xFF586088), 3f, Offset(it, -4f)) }
        drawRoundRect(ScaffoldColors.Hazard, Offset(-22f, -40f), Size(44f, 32f), CornerRadius(6f))
        drawRect(dark, Offset(-22f, -20f), Size(44f, 6f))
        drawRoundRect(ScaffoldColors.HazardDark, Offset(-14f, -36f), Size(28f, 12f), CornerRadius(3f))
        val eye = Color(0xFFFF4D3D)
        drawCircle(eye, 3f, Offset(-5f, -30f))
        drawCircle(eye, 3f, Offset(6f, -30f))
        val arm = Path()
        arm.moveTo(22f, -30f)
        arm.lineTo(38f, if (raised) -52f else -42f)
        arm.lineTo(44f, if (raised) -44f else -34f)
        drawPath(arm, Color(0xFFE09A00), style = Stroke(5f))
        drawRect(Color(0xFFE09A00), Offset(-2f, -48f), Size(4f, 8f))
        drawCircle(eye, 3f, Offset(0f, -50f))
    }
}

private fun DrawScope.drawHazard(h: Hazard) {
    when (h.kind) {
        HazardKind.Pipe -> {
            withTransform({
                translate(h.x, h.y - 11f)
                rotate(Math.toDegrees(h.angle.toDouble()).toFloat(), Offset.Zero)
            }) {
                drawCircle(Color(0xFF7F8DA1), 11f)
                drawCircle(Color(0xFFC9D4E2), 11f, style = Stroke(2f))
                drawCircle(Color(0xFF2A3150), 6f)
                drawRect(ScaffoldColors.Hazard, Offset(-1f, -11f), Size(2f, 6f))
            }
        }

        HazardKind.Spool -> {
            withTransform({
                translate(h.x, h.y - 12f)
                rotate(Math.toDegrees(h.angle.toDouble()).toFloat(), Offset.Zero)
            }) {
                drawCircle(Color(0xFFE07A2E), 12f)
                drawCircle(Color(0xFFFFB066), 12f, style = Stroke(2f))
                drawCircle(Color(0xFF5A2F10), 5f)
                val spoke = Color(0xFF5A2F10)
                drawLine(spoke, Offset(0f, -5f), Offset(0f, -10f), 2f)
                drawLine(spoke, Offset(5f, 0f), Offset(10f, 0f), 2f)
                drawLine(spoke, Offset(0f, 5f), Offset(0f, 10f), 2f)
                drawLine(spoke, Offset(-5f, 0f), Offset(-10f, 0f), 2f)
            }
        }

        HazardKind.Cart -> {
            withTransform({
                translate(h.x, h.y - 8f)
                scale(-h.dir.toFloat(), 1f, Offset.Zero)
            }) {
                val tub = Path()
                tub.moveTo(-16f, -12f)
                tub.lineTo(12f, -12f)
                tub.lineTo(8f, 2f)
                tub.lineTo(-10f, 2f)
                tub.close()
                drawPath(tub, Color(0xFF2FA6D0))
                drawLine(ScaffoldColors.Steel, Offset(-16f, -12f), Offset(-24f, -16f), 3f)
                drawCircle(Color(0xFF222A45), 6f, Offset(14f, -2f))
                drawCircle(ScaffoldColors.Steel, 6f, Offset(14f, -2f), style = Stroke(2f))
                drawRect(ScaffoldColors.Steel, Offset(-10f, 2f), Size(3f, 6f))
            }
        }

        HazardKind.Brick -> {
            drawBrick(h)
        }
    }
}

private fun DrawScope.drawBrick(h: Hazard) {
    if (h.warn > 0f) {
        val tri = Path()
        tri.moveTo(h.x, h.targetY - 24f)
        tri.lineTo(h.x - 12f, h.targetY - 2f)
        tri.lineTo(h.x + 12f, h.targetY - 2f)
        tri.close()
        drawPath(tri, ScaffoldColors.Hazard)
        drawRect(ScaffoldColors.HazardDark, Offset(h.x - 1.5f, h.targetY - 17f), Size(3f, 8f))
        drawRect(ScaffoldColors.HazardDark, Offset(h.x - 1.5f, h.targetY - 7f), Size(3f, 3f))
    }
    withTransform({
        translate(h.x, h.y - 6f)
        scale(HAZARD_SCALE, HAZARD_SCALE, Offset.Zero)
        rotate(-14f, Offset.Zero)
    }) {
        drawRoundRect(Color(0xFFC4553A), Offset(-10f, -5.5f), Size(20f, 11f), CornerRadius(2f))
        drawRect(Color(0xFF7D2F1E), Offset(-10f, -0.5f), Size(20f, 1f))
    }
}
