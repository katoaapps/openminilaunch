package org.sableos.games

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

/** What the player holds down this frame. [jump] is true for the one frame after the key went down. */
internal data class ScaffoldInput(
    val left: Boolean = false,
    val right: Boolean = false,
    val up: Boolean = false,
    val down: Boolean = false,
    val jump: Boolean = false,
)

/** What carries from one level to the next. */
internal data class ScaffoldRun(
    val hearts: Int = ScaffoldWorld.START_HEARTS,
    val hats: Int = 0,
    val bolts: Int = 0,
    val score: Int = 0,
)

internal enum class WorldState { Playing, Cleared, Dead }

/** One thing in flight or rolling. [warn] holds a brick in the air above its landing marker. */
internal class Hazard(
    val kind: HazardKind,
    var x: Float,
    var y: Float,
    var dir: Int,
    val throwDir: Int,
) {
    var vy = 0f
    var airborne = true
    var angle = 0f
    var warn = 0f
    var targetY = 0f
    var jumped = false
    var landedOnce = false
}

/**
 * The rules of one level, with no Android in it. [step] moves everything by a small time step.
 * Pip runs at [RUN_SPEED], climbs at [CLIMB_SPEED], and jumps 62 high and 110 long with no steering in
 * the air. Hazards pass in front of a climber. A hit takes a hard hat if there is one, else a heart.
 */
internal class ScaffoldWorld(
    val level: ScaffoldLevel,
    run: ScaffoldRun = ScaffoldRun(),
) {
    var heroX = level.heroStart.x
        private set
    var heroY = level.heroStart.y
        private set
    var facing = 1
        private set

    /** True while Pip is running or climbing, for the picture. */
    var moving = false
        private set
    var onGround = true
        private set
    var ladder: Ladder? = null
        private set
    var hearts = run.hearts
        private set
    var hats = run.hats
        private set
    var boltTally = run.bolts
        private set
    var score = run.score
        private set
    var levelBolts = 0
        private set
    var time = 0f
        private set
    var invulnerable = 0f
        private set
    var state = WorldState.Playing
        private set

    /** The time bonus left, in points, rounded down to ten. */
    val bonusLeft: Int get() = max(0, level.bonus - (time * BONUS_PER_SECOND).toInt()) / 10 * 10

    /** Points added when the level ended, for the tally. */
    var clearBonus = 0
        private set

    val hazards = mutableListOf<Hazard>()
    val boltTaken = BooleanArray(level.bolts.size)
    val hatTaken = BooleanArray(level.hats.size)
    val liftY = FloatArray(level.lifts.size) { level.lifts[it].stops.first() }

    /** Seconds the claw has been raised after a throw, for the picture. */
    var throwAnim = 0f
        private set

    private var heroVx = 0f
    private var heroVy = 0f
    private var supportLift = -1
    private val liftTarget = IntArray(level.lifts.size) { 1 }
    private val liftWait = FloatArray(level.lifts.size)
    private var throwTimer = FIRST_THROW
    private var throwIndex = 0
    private var combo = 0

    /** The wrecking ball's centre, or null on a level without one. */
    val ballPosition: Spot? get() = level.pendulum?.let { ballAt(it, time) }

    fun runState(): ScaffoldRun = ScaffoldRun(hearts, hats, boltTally, score)

    fun step(
        seconds: Float,
        input: ScaffoldInput,
    ) {
        if (state != WorldState.Playing) return
        var left = seconds
        while (left > 0f && state == WorldState.Playing) {
            val dt = min(left, MAX_STEP)
            tick(dt, input.copy(jump = input.jump && left == seconds))
            left -= dt
        }
    }

    private fun tick(
        dt: Float,
        input: ScaffoldInput,
    ) {
        time += dt
        invulnerable = max(0f, invulnerable - dt)
        throwAnim = max(0f, throwAnim - dt)
        moveLifts(dt)
        moveHero(dt, input)
        throwThings(dt)
        moveHazards(dt)
        collect()
        checkHits()
        checkGoal()
    }

    // --- hero ---

    private fun moveHero(
        dt: Float,
        input: ScaffoldInput,
    ) {
        val dir = (if (input.right) 1 else 0) - (if (input.left) 1 else 0)
        moving = if (ladder != null) input.up != input.down else onGround && dir != 0
        val climbing = ladder
        when {
            climbing != null -> climb(dt, input, dir, climbing)
            onGround -> walk(dt, input, dir)
            else -> fly(dt)
        }
    }

    private fun climb(
        dt: Float,
        input: ScaffoldInput,
        dir: Int,
        l: Ladder,
    ) {
        if (input.jump && dir != 0) {
            ladder = null
            facing = dir
            launch(dir)
            return
        }
        val move = (if (input.down) 1 else 0) - (if (input.up) 1 else 0)
        heroY += move * CLIMB_SPEED * dt
        if (heroY <= l.top) {
            heroY = l.top
            ladder = null
            stand()
        } else if (heroY >= l.bottom) {
            heroY = l.bottom
            ladder = null
            stand()
        }
    }

    private fun stand() {
        onGround = true
        heroVx = 0f
        heroVy = 0f
        supportLift = liftUnder(heroX, heroY)
    }

    private fun launch(dir: Int) {
        onGround = false
        heroVx = dir * RUN_SPEED
        heroVy = -JUMP_SPEED
        supportLift = -1
        combo = 0
    }

    private fun walk(
        dt: Float,
        input: ScaffoldInput,
        dir: Int,
    ) {
        if (supportLift >= 0) heroY = liftY[supportLift]
        if (input.jump) {
            if (dir != 0) facing = dir
            launch(dir)
            return
        }
        if (input.up) findLadder(up = true)?.let { grab(it) }
        if (ladder == null && input.down) findLadder(up = false)?.let { grab(it) }
        if (ladder != null) return
        if (dir != 0) {
            facing = dir
            heroX =
                blocked(heroX, heroX + dir * RUN_SPEED * dt, dir).coerceIn(HALF_WIDTH, ScaffoldField.WIDTH - HALF_WIDTH)
        }
        val lift = liftUnder(heroX, heroY)
        val floor = floorUnder(heroX, heroY)
        when {
            lift >= 0 -> {
                supportLift = lift
                heroY = liftY[lift]
            }

            floor != null -> {
                supportLift = -1
                heroY = floor
            }

            else -> {
                onGround = false
                heroVx = 0f
                heroVy = 0f
                supportLift = -1
                combo = 0
            }
        }
    }

    private fun grab(l: Ladder) {
        ladder = l
        heroX = l.x
        onGround = false
        supportLift = -1
        heroVx = 0f
        heroVy = 0f
        // Starting down from the top, step just below the deck so the climb continues.
        if (heroY <= l.top + 1f) heroY = l.top + 1f
    }

    private fun findLadder(up: Boolean): Ladder? =
        level.ladders.firstOrNull { l ->
            abs(heroX - l.x) <= LADDER_REACH &&
                if (up) heroY >= l.top + 1f && abs(heroY - l.bottom) <= STEP else abs(heroY - l.top) <= STEP
        }

    /** The x after trying to walk from [from] to [to]: stopped by the side of a stair that is higher than the feet. */
    private fun blocked(
        from: Float,
        to: Float,
        dir: Int,
    ): Float {
        var x = to
        for (s in level.surfaces) {
            if (!s.solid || s.y >= heroY - STEP) continue
            if (dir > 0 && from + HALF_WIDTH <= s.x0 + 0.01f && x + HALF_WIDTH > s.x0) x = min(x, s.x0 - HALF_WIDTH)
            if (dir < 0 && from - HALF_WIDTH >= s.x1 - 0.01f && x - HALF_WIDTH < s.x1) x = max(x, s.x1 + HALF_WIDTH)
        }
        return x
    }

    private fun fly(dt: Float) {
        val oldY = heroY
        val oldX = heroX
        heroVy += GRAVITY * dt
        heroY += heroVy * dt
        heroX += heroVx * dt
        for (s in level.surfaces) {
            if (!s.solid || heroY <= s.y + 2f) continue
            if (oldX + HALF_WIDTH <= s.x0 + 0.5f && heroX + HALF_WIDTH > s.x0) {
                heroX = s.x0 - HALF_WIDTH
                heroVx = 0f
            } else if (oldX - HALF_WIDTH >= s.x1 - 0.5f && heroX - HALF_WIDTH < s.x1) {
                heroX = s.x1 + HALF_WIDTH
                heroVx = 0f
            }
        }
        heroX = heroX.coerceIn(HALF_WIDTH, ScaffoldField.WIDTH - HALF_WIDTH)
        if (heroVy <= 0f) return
        val lift = landingLift(heroX, oldY, heroY)
        val landing = landingSurface(heroX, oldY, heroY)
        when {
            lift >= 0 && (landing == null || liftY[lift] <= landing) -> {
                heroY = liftY[lift]
                supportLift = lift
                land()
            }

            landing != null -> {
                heroY = landing
                supportLift = -1
                land()
            }
        }
    }

    private fun land() {
        onGround = true
        heroVy = 0f
        heroVx = 0f
        if (combo >= 2) score += COMBO_BONUS
        combo = 0
    }

    private fun floorUnder(
        x: Float,
        y: Float,
    ): Float? =
        level.surfaces
            .filter { x >= it.x0 && x <= it.x1 && abs(it.y - y) <= STEP }
            .minOfOrNull { it.y }

    private fun liftUnder(
        x: Float,
        y: Float,
    ): Int =
        level.lifts.indices.firstOrNull {
            x >= level.lifts[it].x0 && x <= level.lifts[it].x1 &&
                abs(liftY[it] - y) <= STEP
        }
            ?: -1

    private fun landingSurface(
        x: Float,
        fromY: Float,
        toY: Float,
    ): Float? =
        level.surfaces
            .filter { x >= it.x0 && x <= it.x1 && it.y >= fromY - 0.01f && it.y <= toY + 0.01f }
            .minOfOrNull { it.y }

    private fun landingLift(
        x: Float,
        fromY: Float,
        toY: Float,
    ): Int =
        level.lifts.indices.firstOrNull {
            x >= level.lifts[it].x0 && x <= level.lifts[it].x1 && liftY[it] >= fromY - 0.01f && liftY[it] <= toY + 0.01f
        } ?: -1

    // --- lifts ---

    private fun moveLifts(dt: Float) {
        for (i in level.lifts.indices) {
            val lift = level.lifts[i]
            if (liftWait[i] > 0f) {
                liftWait[i] = max(0f, liftWait[i] - dt)
                continue
            }
            val target = lift.stops[liftTarget[i]]
            val delta = target - liftY[i]
            val move = lift.speed * dt
            if (abs(delta) <= move) {
                liftY[i] = target
                liftWait[i] = lift.wait
                liftTarget[i] = nextStop(i, lift)
            } else {
                liftY[i] += if (delta > 0) move else -move
            }
        }
    }

    private val liftUp = BooleanArray(level.lifts.size) { true }

    private fun nextStop(
        i: Int,
        lift: Lift,
    ): Int {
        val current = liftTarget[i]
        if (lift.stops.size == 1) return 0
        if (liftUp[i] && current == lift.stops.lastIndex) liftUp[i] = false
        if (!liftUp[i] && current == 0) liftUp[i] = true
        return if (liftUp[i]) current + 1 else current - 1
    }

    // --- hazards ---

    private fun throwThings(dt: Float) {
        if (level.throws.isEmpty()) return
        throwTimer -= dt
        if (throwTimer > 0f) return
        throwTimer += level.throwInterval
        val spec = level.throws[throwIndex % level.throws.size]
        throwIndex++
        throwAnim = THROW_ANIM
        val h = Hazard(spec.kind, spec.x, spec.y, if (spec.dir == 0) 1 else spec.dir, spec.dir)
        if (spec.kind == HazardKind.Brick) {
            h.warn = BRICK_WARNING
            h.targetY = landingSurface(spec.x, spec.y, ScaffoldField.HEIGHT) ?: ScaffoldField.GROUND_Y
        }
        hazards += h
    }

    private fun moveHazards(dt: Float) {
        val it = hazards.iterator()
        while (it.hasNext()) {
            val h = it.next()
            if (h.kind == HazardKind.Brick) {
                if (!dropBrick(h, dt)) it.remove()
                continue
            }
            if (h.airborne) fall(h, dt) else roll(h, dt)
            if (h.x < -OFFSCREEN || h.x > ScaffoldField.WIDTH + OFFSCREEN ||
                h.y > ScaffoldField.HEIGHT + OFFSCREEN
            ) {
                it.remove()
            }
            scoreJump(h)
        }
    }

    /** False once the brick has landed. */
    private fun dropBrick(
        h: Hazard,
        dt: Float,
    ): Boolean {
        if (h.warn > 0f) {
            h.warn -= dt
            return true
        }
        h.vy += GRAVITY * dt
        h.y += h.vy * dt
        return h.y < h.targetY
    }

    private fun fall(
        h: Hazard,
        dt: Float,
    ) {
        val oldY = h.y
        h.vy += GRAVITY * dt
        h.y += h.vy * dt
        val landing = landingSurface(h.x, oldY, h.y) ?: return
        h.y = landing
        h.vy = 0f
        h.airborne = false
        val surface = level.surfaces.first { h.x >= it.x0 && h.x <= it.x1 && it.y == landing }
        h.dir =
            when {
                !h.landedOnce && h.throwDir != 0 -> h.throwDir
                surface.flow != 0 -> surface.flow
                else -> h.dir
            }
        h.landedOnce = true
    }

    private fun roll(
        h: Hazard,
        dt: Float,
    ) {
        val speed = level.rollSpeed
        h.x += h.dir * speed * dt
        h.angle += h.dir * speed * dt / (h.kind.height / 2)
        val supported = level.surfaces.any { h.x >= it.x0 && h.x <= it.x1 && abs(it.y - h.y) < 1f }
        if (!supported) {
            h.airborne = true
            h.vy = 0f
        }
    }

    /** +100 for the first hazard you pass over in a hop; a second in the same hop adds 100 more when you land. */
    private fun scoreJump(h: Hazard) {
        if (h.jumped || onGround || ladder != null) return
        val top = h.y - h.kind.height
        if (abs(h.x - heroX) < JUMP_SCORE_REACH && heroY <= top + 6f && heroY >= top - JUMP_SCORE_HEIGHT) {
            h.jumped = true
            combo++
            score += JUMP_POINTS
        }
    }

    // --- pickups, hits, goal ---

    private fun collect() {
        level.bolts.forEachIndexed { i, b ->
            if (!boltTaken[i] && near(b)) {
                boltTaken[i] = true
                levelBolts++
                boltTally++
                score += BOLT_POINTS
                if (boltTally % BOLTS_PER_HAT == 0) hats = min(MAX_HATS, hats + 1)
            }
        }
        level.hats.forEachIndexed { i, p ->
            if (!hatTaken[i] && abs(p.x - heroX) < PICKUP_REACH && abs(p.y - heroY) < PICKUP_REACH) {
                hatTaken[i] = true
                hats = min(MAX_HATS, hats + 1)
            }
        }
    }

    private fun near(p: Spot): Boolean =
        abs(p.x - heroX) < PICKUP_REACH && abs(p.y - (heroY - HEIGHT / 2)) < PICKUP_REACH + HEIGHT / 2 - 6f

    private fun checkHits() {
        if (invulnerable > 0f || ladder != null) return
        val hit =
            hazards.any { h ->
                (h.kind != HazardKind.Brick || h.warn <= 0f) &&
                    abs(h.x - heroX) < (h.kind.width + HALF_WIDTH * 2) / 2 - HIT_SHRINK &&
                    h.y > heroY - HEIGHT + HIT_SHRINK && h.y - h.kind.height < heroY - HIT_SHRINK
            } || level.pendulum?.let { ballHits(it) } == true
        if (!hit) return
        invulnerable = INVULNERABLE_SECONDS
        if (hats > 0) {
            hats--
        } else {
            hearts--
            if (hearts <= 0) state = WorldState.Dead
        }
    }

    private fun ballHits(p: Pendulum): Boolean {
        val c = ballAt(p, time)
        val nearestX = c.x.coerceIn(heroX - HALF_WIDTH, heroX + HALF_WIDTH)
        val nearestY = c.y.coerceIn(heroY - HEIGHT, heroY)
        val dx = c.x - nearestX
        val dy = c.y - nearestY
        return dx * dx + dy * dy < (p.radius - HIT_SHRINK) * (p.radius - HIT_SHRINK)
    }

    private fun checkGoal() {
        if (state != WorldState.Playing || !onGround || ladder != null) return
        if (abs(heroX - level.juno.x) < GOAL_REACH && abs(heroY - level.juno.y) < STEP) {
            clearBonus =
                bonusLeft + (if (level.number == ScaffoldLevels.COUNT) RESCUE_POINTS + hearts * HEART_POINTS else 0)
            score += clearBonus
            state = WorldState.Cleared
        }
    }

    companion object {
        const val START_HEARTS = 3
        const val MAX_HATS = 3
        const val BOLTS_PER_HAT = 8
        const val RUN_SPEED = 120f
        const val CLIMB_SPEED = 80f
        const val JUMP_SPEED = 270f
        const val GRAVITY = 590f
        const val HALF_WIDTH = 8f
        const val HEIGHT = 38f
        const val STEP = 4f
        const val LADDER_REACH = 12f
        const val BONUS_PER_SECOND = 10
        const val BOLT_POINTS = 50
        const val JUMP_POINTS = 100
        const val COMBO_BONUS = 100
        const val RESCUE_POINTS = 2000
        const val HEART_POINTS = 500
        const val INVULNERABLE_SECONDS = 1.5f
        const val BRICK_WARNING = 0.6f
        const val FIRST_THROW = 1.5f
        const val THROW_ANIM = 0.4f
        private const val MAX_STEP = 1f / 120f
        private const val OFFSCREEN = 30f
        private const val PICKUP_REACH = 22f
        private const val GOAL_REACH = 26f
        private const val HIT_SHRINK = 2f
        private const val JUMP_SCORE_REACH = 10f
        private const val JUMP_SCORE_HEIGHT = 70f

        /** The ball's centre at [time]. */
        fun ballAt(
            p: Pendulum,
            time: Float,
        ): Spot {
            val a = p.amplitudeDeg * PI.toFloat() / 180f * sin(2f * PI.toFloat() * time / p.periodSeconds)
            return Spot(p.pivot.x + p.length * sin(a), p.pivot.y + p.length * cos(a))
        }
    }
}
