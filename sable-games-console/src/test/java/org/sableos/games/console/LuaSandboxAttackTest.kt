package org.sableos.games.console

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

/**
 * Scripts a hostile game might ship. Each must either find nothing to reach or be stopped with a
 * message, quickly, without taking the app down: no way out of the sandbox, no freezing the phone,
 * no filling its memory.
 */
class LuaSandboxAttackTest {
    /** Runs [source] through start and two frames; the message it was stopped with, or null. */
    private fun run(source: String): String? =
        try {
            val g = LuaGame(source, MAIN, MemoryGameStore(), setOf("ok.png"))
            g.start()
            g.update(0.016)
            g.update(0.016)
            g.frame()
            null
        } catch (e: GameScriptError) {
            e.message.orEmpty()
        }

    private fun stopped(
        source: String,
        why: String,
    ) {
        val message = run(source) ?: fail("expected the game to be stopped: $source")
        assertTrue("$message", message.toString().contains(why))
    }

    private fun status(source: String): String {
        val g = LuaGame(source, MAIN, MemoryGameStore())
        g.start()
        g.update(0.016)
        return g.status
    }

    /** What [source]'s `update` saved under "out" (saves hold more than the status line). */
    private fun saved(source: String): String? {
        val store = MemoryGameStore()
        val g = LuaGame(source, MAIN, store)
        g.start()
        g.update(0.016)
        return store.get("out")
    }

    @Test(timeout = 20_000)
    fun theOnlyGlobalsAreLuasSafeOnesAndSable() {
        val globals =
            saved(
                """
                function update()
                  local seen = {}
                  for k in pairs(_G) do seen[#seen + 1] = k end
                  table.sort(seen)
                  sable.save("out", table.concat(seen, ","))
                end
                """.trimIndent(),
            )
        assertEquals(
            "_G,_VERSION,assert,bit32,error,getmetatable,ipairs,math,next,pairs,pcall,rawequal,rawget," +
                "rawlen,rawset,sable,select,setmetatable,string,table,tonumber,tostring,type,update,xpcall",
            globals,
        )
        val strings =
            status(
                "function update() local s = {} for k in pairs(string) do s[#s + 1] = k end " +
                    "table.sort(s) sable.status(table.concat(s, ',')) end",
            )
        assertEquals("byte,char,find,format,gmatch,gsub,len,lower,match,rep,reverse,sub,upper", strings)
    }

    @Test(timeout = 20_000)
    fun oneGameCantChangeAnothersStrings() {
        // The shared string metatable is hidden, so a game can't reroute string methods.
        stopped("function update() getmetatable('').__index = function() return print end end", "index expected, got string")
        val a =
            LuaGame(
                "function update() string.upper = function() return 'stolen' end end",
                MAIN,
                MemoryGameStore(),
            )
        val b = LuaGame("function update() sable.status(('quiet'):upper()) end", MAIN, MemoryGameStore())
        a.start()
        b.start()
        a.update(0.016)
        b.update(0.016)
        assertEquals("QUIET", b.status)
        // Its own string table is its own business.
        assertEquals("stolen", status("function update() string.upper = function() return 'stolen' end sable.status(('x'):upper()) end"))
    }

    @Test(timeout = 20_000)
    fun compiledCodeAndHiddenLibrariesCantBeReached() {
        // A chunk of compiled Lua is refused: the source is always compiled from text.
        assertTrue(run("\u001bLuaR\u0000") != null)
        assertEquals("nil nil nil", status("function update() sable.status(tostring(rawget(_G, 'debug')) .. ' ' .. tostring(string.dump) .. ' ' .. tostring(rawget(_G, 'package'))) end"))
        stopped("function update() local c = sable.rect.getClass end", "attempt to index")
        // Pictures are looked up by name among the game's own; a path gets nowhere.
        stopped("function draw() sable.image(0, 0, 1, 1, '../../shared_prefs/console.xml') end", "no picture")
    }

    @Test(timeout = 20_000)
    fun loopsAreStoppedWhereverTheyHide() {
        stopped("function update() while true do end end", TOO_LONG)
        stopped("function update() xpcall(function() while true do end end, function(e) return e end) end", TOO_LONG)
        stopped("function update() while true do xpcall(error, function() while true do end end) end end", TOO_LONG)
        stopped("function update() tostring(setmetatable({}, {__tostring = function() while true do end end})) end", TOO_LONG)
        stopped("function update() local t = {} for i = 1, 200 do t[i] = i end table.sort(t, function() while true do end end) end", TOO_LONG)
        stopped("function update() local function f() return 1 + f() end pcall(f) end", "too deeply")
        stopped("function update() for i = 1, 1e6 do sable.save('k' .. i, 'v') end end", TOO_LONG)
    }

    @Test(timeout = 20_000)
    fun patternsThatBacktrackForeverAreStopped() {
        // Each of these would keep LuaJ's own matcher busy for hours.
        stopped(
            "function update() local s = ('a'):rep(100) s:find(('a?'):rep(100) .. ('a'):rep(100)) end",
            TOO_LONG,
        )
        stopped(
            "function update() for x in ('a'):rep(200000):gmatch('a*a*a*a*b') do end end",
            TOO_LONG,
        )
        stopped("function update() ('('):rep(200000):find('%b()') end", TOO_LONG)
        stopped("function update() ('a'):rep(200000):gsub('a-a-a-b', '') end", TOO_LONG)
        stopped("function update() ('a'):find(('x'):rep(600)) end", "pattern longer")
        // Ordinary patterns still work.
        assertEquals(
            "Hello, world 42|42|Hello,world|10,14|(a(b)c)",
            status(
                """
                function update()
                  local s = "  Hello, world 42  "
                  local t = s:gsub("^%s*(.-)%s*$", "%1")
                  local n = tonumber(s:match("(%d+)"))
                  local w = {}
                  for x in s:gmatch("%a+") do w[#w + 1] = x end
                  local a, b = s:find("world", 1, true)
                  sable.status(t .. "|" .. n .. "|" .. table.concat(w, ",") .. "|" .. a .. "," .. b .. "|" .. ("f(a(b)c)"):match("%b()"))
                end
                """.trimIndent(),
            ),
        )
    }

    @Test(timeout = 60_000)
    fun aGameCantFillTheMemory() {
        stopped("function update() local s = ('x'):rep(2^31 - 2) end", "longer than")
        stopped("function update() local s = string.format('%999999999.1f', 1) end", TOO_LONG)
        stopped("function update() local n = select('#', table.unpack({}, 1, 2^31 - 1)) end", "memory")
        // Doubling a string a frame at a time, and growing it with gsub, run into the heap limit.
        stopped("local s = 'x' function update() for i = 1, 40 do s = s .. s end end", "memory")
        stopped(
            "local t = {} function update() local s = ('y'):rep(1e6) for i = 1, 3000 do t[#t + 1] = s .. i end end",
            "memory",
        )
    }

    private companion object {
        const val MAIN = "main.lua"
        const val TOO_LONG = "too long"
    }
}
