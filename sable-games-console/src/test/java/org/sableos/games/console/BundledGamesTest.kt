package org.sableos.games.console

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import kotlin.random.Random

/** The games that come with the console parse, start and survive a long random session. */
class BundledGamesTest {
    private val root = File("src/main/assets/console")

    private fun load(
        id: String,
        store: GameStore = MemoryGameStore(),
    ): Pair<GameManifest, LuaGame> {
        val dir = File(root, id)
        val m = GameManifest.parse(File(dir, GameManifest.FILE).readText()).getOrThrow()
        assertEquals(id, m.id)
        val pictures = dir.list().orEmpty().filter { GamePictures.isPicture(it) }.toSet()
        return m to LuaGame(File(dir, m.entry).readText(), m.entry, store, pictures).also { it.start() }
    }

    @Test
    fun theEightMotionGamesAreThere() {
        val ids =
            root
                .listFiles()
                .orEmpty()
                .map { it.name }
                .toSet()
        assertEquals(
            setOf(
                "tilt_maze",
                "motion_2048",
                "sokoban_tilt",
                "tilt_racer",
                "tilt_circuit",
                "ball_maze",
                "sky_roll",
                "ramp_rush",
            ),
            ids,
        )
        ids.forEach { assertTrue(it, load(it).first.usesTilt) }
    }

    @Test
    fun everyGameSurvivesRandomPlay() {
        val random = Random(25)
        for (id in root.list().orEmpty().sorted()) {
            val (_, g) = load(id)
            repeat(3_000) { i ->
                when (random.nextInt(4)) {
                    0 -> {
                        g.tilt(TiltStep.entries[random.nextInt(4)])
                    }

                    1 -> {
                        g.key(listOf("space", "u", "enter", "n")[random.nextInt(4)])
                    }

                    else -> {
                        g.tiltX = random.nextDouble(-1.0, 1.0)
                        g.tiltY = random.nextDouble(-1.0, 1.0)
                        g.update(1.0 / 60)
                    }
                }
                if (g.over != null && i % 7 == 0) g.restart()
                assertTrue(id, g.frame().isNotEmpty())
            }
            assertTrue(id, g.status.isNotEmpty())
        }
    }

    @Test
    fun tiltRacerEndsInACrashWhenYouDoNotSteer() {
        val (_, g) = load("tilt_racer")
        repeat(60 * 120) { if (g.over == null) g.update(1.0 / 60) }
        assertNotNull(g.over)
        assertTrue((g.score ?: 0) > 0)
    }

    @Test
    fun tiltRacerDrawsTheF1CarsAndCKeepsTheChosenColour() {
        val dir = File(root, "tilt_racer")
        val pictures = dir.list().orEmpty().filter { GamePictures.isPicture(it) }.toSet()
        assertEquals(
            setOf("blue", "red", "yellow", "green", "purple", "violet").map { "f1_$it.png" }.toSet(),
            pictures,
        )
        pictures.forEach { assertTrue(it, File(dir, it).length() <= GamePictures.MAX_BYTES) }
        val store = MemoryGameStore()
        val (_, g) = load("tilt_racer", store)
        fun player() = g.frame().filterIsInstance<DrawOp.Image>().last().name
        assertEquals("f1_red.png", player())
        g.key("c")
        assertEquals("f1_blue.png", player())
        assertEquals("2", store.get("color"))
        // A new run (and a new start of the app) keeps the colour.
        val (_, again) = load("tilt_racer", store)
        assertEquals("f1_blue.png", again.frame().filterIsInstance<DrawOp.Image>().last().name)
        // Six presses go round every colour and back.
        repeat(6) { g.key("c") }
        assertEquals("f1_blue.png", player())
    }

    @Test
    fun tiltCircuitIsRaceableAndCountsOnlyWholeLaps() {
        val dir = File(root, "tilt_circuit")
        val pictures = dir.list().orEmpty().filter { GamePictures.isPicture(it) }.toSet()
        assertTrue(pictures.size <= GamePictures.MAX)
        pictures.forEach { assertTrue(it, File(dir, it).length() <= GamePictures.MAX_BYTES) }
        // A driver that follows the road: the script's car and track helpers made reachable.
        val source =
            File(dir, "main.lua")
                .readText()
                .replace("local car, rivals,", "car, rivals,")
                .replace("lap_start, best_lap, wrong, finished\n", "lap_start, best_lap, wrong, finished = nil\n")
                .replace("local function nearest", "function nearest")
                .replace("local function at_distance", "function at_distance") +
                """

                function bot()
                  local _, where = nearest(car.x, car.y)
                  local tx, ty = at_distance(where + 3.5)
                  local diff = (math.atan2(ty - car.y, tx - car.x) - car.a + math.pi) % (2 * math.pi) - math.pi
                  return math.max(-1, math.min(1, diff * 2))
                end
                sable.tilt = function() return bot(), 0 end
                """.trimIndent()
        // Both circuits can be raced: the menu picks one (Down for the second) and Space starts it.
        for (circuit in 1..2) {
            val store = MemoryGameStore()
            val g = LuaGame(source, "main.lua", store, pictures).also { it.start() }
            assertTrue(g.status, g.status.startsWith("Choose a circuit"))
            if (circuit == 2) g.tilt(TiltStep.Down)
            g.key("space")
            assertEquals("$circuit", store.get("circuit"))
            var frames = 0
            while (g.over == null && frames < 60 * 120) {
                g.update(1.0 / 60)
                assertTrue(g.frame().size < 200)
                frames++
            }
            val over = g.over.orEmpty()
            assertTrue(over, over.contains("best lap"))
            assertTrue("$circuit ${g.score}", (g.score ?: 0) in (if (circuit == 1) 15L..40L else 30L..90L))
            assertTrue(store.get("best_$circuit") != null)
        }

        // Without steering the car leaves the road and never finishes a lap.
        val (_, idle) = load("tilt_circuit")
        idle.key("space")
        repeat(60 * 60) { idle.update(1.0 / 60) }
        assertEquals(null, idle.over)
        assertTrue(idle.status, idle.status.startsWith("Lap 1/3"))
    }

    @Test
    fun ballMazeKeepsItsBallsOnEveryBoard() {
        val random = Random(13)
        for (shape in 0..1) {
            for (level in 0..3) {
                val store = MemoryGameStore()
                val (_, g) = load("ball_maze", store)
                assertTrue(g.status.contains("Space starts"))
                repeat(shape) { g.tilt(TiltStep.Right) }
                repeat(2) { g.tilt(TiltStep.Down) }
                repeat(level) { g.tilt(TiltStep.Right) }
                g.key("space")
                assertEquals("${shape + 1}", store.get("shape"))
                assertEquals("${level + 1}", store.get("level"))
                assertTrue(g.status, g.status.contains("home"))
                repeat(60 * 20) { i ->
                    if (i % 40 == 0) {
                        g.tiltX = random.nextDouble(-1.0, 1.0)
                        g.tiltY = random.nextDouble(-1.0, 1.0)
                    }
                    g.update(1.0 / 60)
                }
                val frame = g.frame()
                assertTrue(frame.size < LuaGame.MAX_OPS)
                // Each ball's body is one grey circle; every one is still on the board.
                val bodies = frame.filterIsInstance<DrawOp.Circle>().filter { it.color == "#7E878E" }
                assertEquals(level + 2, bodies.size)
                for (b in bodies) {
                    if (shape == 0) {
                        assertTrue("$level $b", (b.x - 6) * (b.x - 6) + (b.y - 6) * (b.y - 6) < 5.4 * 5.4)
                    } else {
                        assertTrue("$level $b", b.x in 0.5..11.5 && b.y in 0.5..11.5)
                    }
                }
            }
        }
    }

    /** A game's own pictures stay within the console's limits. */
    private fun pictures(id: String): Set<String> {
        val dir = File(root, id)
        val pictures = dir.list().orEmpty().filter { GamePictures.isPicture(it) }.toSet()
        assertTrue(pictures.size <= GamePictures.MAX)
        pictures.forEach { assertTrue(it, File(dir, it).length() <= GamePictures.MAX_BYTES) }
        return pictures
    }

    @Test
    fun skyRollCanBeRolledThroughAllEightLevels() {
        // A driver that follows each level's route and waits for the lifts.
        val source =
            File(root, "sky_roll/main.lua")
                .readText()
                .replace(
                    "local level, lv, ball, clock, balls, score, got, falling, cleared, finished, t_all\n",
                    "level, lv, ball, clock, balls, score, got, falling, cleared, finished, t_all = nil\n",
                ).replace("local function mover_place", "function mover_place") +
                """

                local LIFTS = { { "go", 0, -3 }, { "ride", 1 }, { "go", 2, -17 }, { "ride", 2 }, { "go", 16, -21 } }
                local wp_key, wp = nil, 1
                local function clamp(v) return math.max(-1, math.min(1, v)) end
                sable.tilt = function()
                  local b = ball
                  local route = LIFTS
                  if lv.route then
                    route = {}
                    for _, p in ipairs(lv.route) do route[#route + 1] = { "go", p[1], p[2] } end
                  end
                  if wp_key ~= level * 10 + balls then wp_key, wp = level * 10 + balls, 1 end
                  local step = route[math.min(wp, #route)]
                  local tx, ty, top
                  if step[1] == "go" then
                    tx, ty, top = step[2], step[3], 3.5
                    if (b.x - tx) ^ 2 + (b.y - ty) ^ 2 < 0.25 then wp = wp + 1 end
                  else
                    local m = lv.movers[step[2]]
                    local mx, my = mover_place(m)
                    tx, ty, top = mx + m[3] / 2, my + m[4] / 2, 3
                    local k = math.max(0, math.min(1, 0.5 - 0.65 * math.cos(2 * math.pi * t_all / m[7])))
                    local ph = (t_all / m[7]) % 1
                    local on = b.x >= mx + 0.3 and b.x <= mx + m[3] - 0.3 and b.y >= my + 0.3 and b.y <= my + m[4] - 0.3
                    if on and k >= 1 then wp = wp + 1 end
                    if not on and not (k <= 0 and ph > 0.85 or ph < 0.05) then tx, ty = b.x, b.y end
                  end
                  local dx, dy = tx - b.x, ty - b.y
                  local d = math.sqrt(dx * dx + dy * dy)
                  if d == 0 then d = 1 end
                  local sp = math.min(top, d * 2.5)
                  return clamp((dx / d * sp - b.vx) * 0.5), clamp((dy / d * sp - b.vy) * 0.5)
                end
                """.trimIndent()
        val g = LuaGame(source, "main.lua", MemoryGameStore(), pictures("sky_roll")).also { it.start() }
        var frames = 0
        while (g.over == null && frames < 60 * 150) {
            g.update(1.0 / 60)
            assertTrue(g.frame().size < LuaGame.MAX_OPS / 4)
            frames++
        }
        val over = g.over.orEmpty()
        assertTrue(over, over.contains("All 8 levels"))
        assertTrue((g.score ?: 0) > 600)

        // Leaning one way the whole time rolls the ball off the edge and loses it.
        val (_, idle) = load("sky_roll")
        idle.tiltX = 1.0
        repeat(60 * 2) { idle.update(1.0 / 60) }
        assertTrue(idle.status, idle.status.contains("balls ●●●") && !idle.status.contains("●●●●"))
    }

    @Test
    fun rampRushCanBeDrivenToTheEndAndKeepsTheCarColour() {
        // A driver that keeps to the middle, boosts now and then, and rolls only on long jumps.
        val source =
            File(root, "ramp_rush/main.lua")
                .readText()
                .replace(
                    "local track, car, clock, check_z, rolls_total, boost, fuel, message, message_t, finished, color, total\n",
                    "track, car, clock, check_z, rolls_total, boost, fuel, message, message_t, finished, color, total = nil\n",
                ) +
                """

                local was_air, plan = false, false
                sable.tilt = function()
                  local lean = 0
                  if car.air then
                    if not was_air then plan = 2 * car.vh / 26 > 0.95 end
                    if plan and car.spun < 2 * math.pi then lean = 1 end
                  else
                    lean = math.max(-1, math.min(1, -car.x * 1.5))
                  end
                  was_air = car.air
                  return lean, 0
                end
                """.trimIndent()
        val g = LuaGame(source, "main.lua", MemoryGameStore(), pictures("ramp_rush")).also { it.start() }
        var frames = 0
        while (g.over == null && frames < 60 * 120) {
            if (frames % 300 == 120) g.key("space")
            g.update(1.0 / 60)
            assertTrue(g.frame().size < LuaGame.MAX_OPS / 4)
            frames++
        }
        val over = g.over.orEmpty()
        assertTrue(over, over.contains("Finished") && !over.contains(" 0 rolls"))
        assertTrue((g.score ?: 0) in 30L..60L)

        val store = MemoryGameStore()
        val (_, car) = load("ramp_rush", store)
        fun player() = car.frame().filterIsInstance<DrawOp.Image>().last().name
        assertEquals("car_blue.png", player())
        car.key("c")
        assertEquals("car_red.png", player())
        assertEquals("car_red.png", load("ramp_rush", store).second.frame().filterIsInstance<DrawOp.Image>().last().name)
    }

    @Test
    fun theConsoleSupportsResizableMultiWindowPlay() {
        val manifest = File("src/main/AndroidManifest.xml").readText()
        val activity = manifest.substringAfter("android:name=\"org.sableos.games.console.ConsoleActivity\"").substringBefore(">")
        assertTrue(activity, activity.contains("android:resizeableActivity=\"true\""))
        assertTrue(activity, activity.contains("android:exported=\"true\""))
        assertTrue(activity, activity.contains("android:icon=\"@mipmap/ic_sable_game\""))
        assertTrue(activity, activity.contains("android:label=\"@string/console_title\""))
        assertTrue(activity, activity.contains("android:excludeFromRecents=\"false\""))
        assertTrue(manifest, manifest.contains("android.intent.action.MAIN"))
        assertTrue(manifest, manifest.contains("android.intent.category.LAUNCHER"))
        assertTrue(activity, activity.contains("android:launchMode=\"standard\""))
        assertFalse(activity, activity.contains("android:screenOrientation="))
        assertTrue(
            manifest,
            manifest.contains("android.window.PROPERTY_SUPPORTS_MULTI_INSTANCE_SYSTEM_UI"),
        )
    }

    @Test
    fun motion2048SlidesAndScores() {
        val (_, g) = load("motion_2048")
        repeat(200) { g.tilt(TiltStep.entries[it % 4]) }
        assertTrue((g.score ?: 0) > 0)
    }
}
