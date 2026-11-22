package org.sableos.games.console

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class LuaGameTest {
    private fun game(
        source: String,
        store: GameStore = MemoryGameStore(),
    ) = LuaGame(source, "main.lua", store).also { it.start() }

    private fun error(block: () -> Unit): String =
        try {
            block()
            fail("expected the game to stop")
            ""
        } catch (e: GameScriptError) {
            e.message.orEmpty()
        }

    @Test
    fun aGameDrawsReportsAndSaves() {
        val store = MemoryGameStore()
        val g =
            game(
                """
                local n = 0
                function init() sable.grid(4, 3); n = tonumber(sable.load("n") or "0") end
                function tilt(step) if step == "right" then n = n + 1 end; sable.save("n", tostring(n)) end
                function update(dt) local x = sable.tilt(); sable.status("n=" .. n .. " x=" .. x); sable.score(n) end
                function key(k) if k == "space" then sable.over("done") end end
                function draw() sable.rect(0, 0, 1, 1, "wall"); sable.circle(1, 1, 0.5, "#FF0000"); sable.text(2, 2, "hi") end
                """.trimIndent(),
                store,
            )
        assertEquals(4, g.cols)
        assertEquals(3, g.rows)
        g.tilt(TiltStep.Right)
        g.tiltX = 0.5
        g.update(0.016)
        assertEquals("n=1 x=0.5", g.status)
        assertEquals(1L, g.score)
        assertEquals("1", store.get("n"))
        val ops = g.frame()
        assertEquals(DrawOp.Rect(0.0, 0.0, 1.0, 1.0, "wall"), ops[0])
        assertEquals(DrawOp.Circle(1.0, 1.0, 0.5, "#FF0000"), ops[1])
        assertEquals(DrawOp.Text(2.0, 2.0, "hi", 1.0, "text"), ops[2])
        assertTrue(g.key("space"))
        assertEquals("done", g.over)
        g.restart()
        assertNull(g.over)
    }

    @Test
    fun theSandboxHasNoFilesJavaOrLoading() {
        for (name in listOf(
            "io",
            "os",
            "luajava",
            "require",
            "load",
            "loadstring",
            "dofile",
            "loadfile",
            "debug",
            "package",
            "coroutine",
            "print",
        )) {
            val probe = game("function update() if $name ~= nil then sable.status('open') end end")
            probe.update(0.0)
            assertEquals(name, "", probe.status)
        }
    }

    @Test
    fun aLoopThatNeverEndsIsStoppedEvenUnderPcall() {
        val g = game("function update() while true do end end")
        assertTrue(error { g.update(0.0) }.contains("too long"))
        val p = game("function update() while true do pcall(function() while true do end end) end end")
        assertTrue(error { p.update(0.0) }.contains("too long"))
    }

    @Test
    fun scriptErrorsNameTheLine() {
        assertTrue(error { LuaGame("x = = 1", "main.lua", MemoryGameStore()) }.contains("main.lua:1"))
        val g = game("function update()\n  local t = nil\n  return t.x\nend")
        assertTrue(error { g.update(0.0) }.contains("main.lua:3"))
    }

    @Test
    fun drawingAndTextAreBounded() {
        val g =
            game(
                "function draw() for i = 1, 10000 do sable.rect(0, 0, 1, 1) end end function init() sable.grid(9999, 0) end",
            )
        assertEquals(LuaGame.MAX_OPS, g.frame().size)
        assertEquals(LuaGame.MAX_GRID, g.cols)
        assertEquals(1, g.rows)
    }

    /** The example in docs/GAMES_CONSOLE.md; keep the two the same. */
    @Test
    fun theAuthorGuideExamplePlays() {
        val g =
            game(
                """
                local x, y, moves

                function init()
                  sable.grid(8, 8)
                  x, y, moves = 1, 1, 0
                  sable.status("Reach the green square")
                end

                function tilt(step)
                  local dx = (step == "right" and 1) or (step == "left" and -1) or 0
                  local dy = (step == "down" and 1) or (step == "up" and -1) or 0
                  x = math.max(0, math.min(7, x + dx))
                  y = math.max(0, math.min(7, y + dy))
                  moves = moves + 1
                  sable.score(moves)
                  if x == 6 and y == 6 then sable.over("Done in " .. moves .. " moves") end
                end

                function draw()
                  sable.rect(0, 0, 8, 8, "floor")
                  sable.rect(6, 6, 1, 1, "goal")
                  sable.circle(x + 0.5, y + 0.5, 0.4, "player")
                end
                """.trimIndent(),
            )
        assertEquals(8, g.cols)
        assertEquals(3, g.frame().size)
        repeat(5) { g.tilt(TiltStep.Right) }
        repeat(5) { g.tilt(TiltStep.Down) }
        assertEquals(10L, g.score)
        assertEquals("Done in 10 moves", g.over)
    }

    /** LuaJ ignores "%.2f"; the sandbox's string.format keeps it (Tilt Racer showed 0.20044544069364575 km). */
    @Test
    fun formatKeepsDecimalPlaces() {
        val g =
            game(
                """
                function init()
                  sable.status(string.format("%.2f km|%5.1f|%d:%02d|%s %%|%x", 0.20044544069364575, 3.14159, 1, 5, "ok", 255))
                  sable.over(("%.1f"):format(2.25))
                end
                """.trimIndent(),
            )
        assertEquals("0.20 km|  3.1|1:05|ok %|ff", g.status)
        assertEquals("2.3", g.over)
    }

    @Test
    fun imagesDrawTheGamesOwnPicturesOnly() {
        val g =
            LuaGame(
                """
                function draw() sable.image(1, 2, 3, 4, "car.png") end
                function key(k) if k == "x" then sable.image(0, 0, 1, 1, "other.png") end end
                """.trimIndent(),
                "main.lua",
                MemoryGameStore(),
                setOf("car.png"),
            ).also { it.start() }
        assertEquals(listOf(DrawOp.Image(1.0, 2.0, 3.0, 4.0, "car.png")), g.frame())
        val turned =
            LuaGame("function draw() sable.image(0, 0, 1, 1, \"car.png\", 450) end", "main.lua", MemoryGameStore(), setOf("car.png"))
                .also { it.start() }
        assertEquals(90.0, (turned.frame().single() as DrawOp.Image).angle, 0.0)
        assertTrue(error { g.key("x") }.contains("no picture other.png"))
        assertTrue(GamePictures.isPicture("f1_red.png"))
        assertTrue(!GamePictures.isPicture("Car.PNG") && !GamePictures.isPicture("../x.png"))
    }

    @Test
    fun polygonsKeepTheirCornersAndSeeThroughColours() {
        val g =
            game(
                """
                function draw() sable.poly({ 0, 0, 4, 0, 2, 3 }, "#FF000080") end
                function key(k) sable.poly({ 0, 0, 1, 1 }) end
                """.trimIndent(),
            )
        assertEquals(listOf(DrawOp.Poly(listOf(0.0, 4.0, 2.0), listOf(0.0, 0.0, 3.0), "#FF000080")), g.frame())
        assertTrue(error { g.key("x") }.contains("at least 3 corners"))
        val big = game("function draw() local p = {} for i = 1, 400 do p[i] = i end sable.poly(p) end")
        assertEquals(LuaGame.MAX_POINTS, (big.frame().single() as DrawOp.Poly).xs.size)
    }
}
