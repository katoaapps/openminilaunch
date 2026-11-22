package org.sableos.games.console

import org.luaj.vm2.Globals
import org.luaj.vm2.LoadState
import org.luaj.vm2.LuaError
import org.luaj.vm2.LuaString
import org.luaj.vm2.LuaTable
import org.luaj.vm2.LuaValue
import org.luaj.vm2.Varargs
import org.luaj.vm2.compiler.LuaC
import org.luaj.vm2.lib.BaseLib
import org.luaj.vm2.lib.Bit32Lib
import org.luaj.vm2.lib.DebugLib
import org.luaj.vm2.lib.MeteredPatterns
import org.luaj.vm2.lib.PackageLib
import org.luaj.vm2.lib.StringLib
import org.luaj.vm2.lib.TableLib
import org.luaj.vm2.lib.VarArgFunction
import org.luaj.vm2.lib.jse.JseMathLib
import java.io.OutputStream
import java.io.PrintStream
import java.util.Locale

/** One shape a game drew this frame, in grid cells ([LuaGame.cols] by [LuaGame.rows]). */
sealed interface DrawOp {
    val color: String

    data class Rect(
        val x: Double,
        val y: Double,
        val w: Double,
        val h: Double,
        override val color: String,
    ) : DrawOp

    data class Circle(
        val x: Double,
        val y: Double,
        val r: Double,
        override val color: String,
    ) : DrawOp

    data class Text(
        val x: Double,
        val y: Double,
        val text: String,
        val size: Double,
        override val color: String,
    ) : DrawOp

    /** A filled polygon through the points ([xs] and [ys] in step, 3 to [LuaGame.MAX_POINTS]). */
    data class Poly(
        val xs: List<Double>,
        val ys: List<Double>,
        override val color: String,
    ) : DrawOp

    /**
     * One of the game's own pictures ([name], a PNG in its folder) stretched over the box, turned
     * [angle] degrees clockwise about the box's centre.
     */
    data class Image(
        val x: Double,
        val y: Double,
        val w: Double,
        val h: Double,
        val name: String,
        val angle: Double = 0.0,
    ) : DrawOp {
        override val color: String get() = ""
    }
}

/** A game's small saved values (`sable.save` / `sable.load`): levels reached, settings. */
interface GameStore {
    fun get(key: String): String?

    fun put(
        key: String,
        value: String?,
    )
}

class MemoryGameStore : GameStore {
    private val values = linkedMapOf<String, String>()

    override fun get(key: String): String? = values[key]

    override fun put(
        key: String,
        value: String?,
    ) {
        if (value == null) values.remove(key) else values[key] = value
    }
}

/** A Lua error, with the script's line when Lua gave one; shown instead of the game. */
class GameScriptError(
    message: String,
) : Exception(message)

/**
 * Runs one game's `main.lua` in a sandbox and keeps what it reports.
 *
 * The script sees Lua's base, string, table, math and bit32 libraries and one table, `sable`, and
 * nothing else: no files, no `require`, no `load`, no Java, no network, no other apps. Every call
 * into it has an instruction budget, so a loop that never ends stops the game instead of the phone;
 * the pattern functions (find, match, gmatch, gsub) are metered too ([MeteredPatterns]), and a game
 * that grows its memory past a share of the app's heap is stopped ([HEAP_SHARE]).
 *
 * A game defines any of these global functions; the console calls them:
 *
 * - `init()` once, when the game starts or starts over
 * - `update(dt)` every frame while it runs, `dt` in seconds
 * - `draw()` every frame, drawing with `sable.rect`, `sable.circle` and `sable.text`
 * - `tilt(step)` for each tilt step ("left", "right", "up", "down"), also sent by the arrow keys,
 *   the trackpad and W A S D
 * - `key(name)` for other keys: "space", "enter", "u", "n" and the other letters, lower case
 *
 * and calls back into `sable`:
 *
 * - `sable.grid(cols, rows)` the drawing grid (default 10 by 10), scaled to fill the square
 * - `sable.rect(x, y, w, h, color)`, `sable.circle(x, y, r, color)`, and `sable.text(x, y, text, color, size)`
 *   centred on x, y, `size` its height in cells (default 1)
 * - `sable.poly({x1, y1, x2, y2, ...}, color)` a filled polygon of 3 to 64 corners
 * - `sable.image(x, y, w, h, name, angle)` one of the game's PNG pictures ([pictures]) over that box,
 *   turned `angle` degrees clockwise about its centre (default 0)
 * - `sable.status(text)` the line above the game, `sable.score(n)` the score the library keeps
 *   the best of, `sable.over(text)` ends the game with that message (`sable.over()` clears it)
 * - `sable.tilt()` the current lean, x and y from -1 to 1 (0, 0 when playing with keys)
 * - `sable.save(key, value)` and `sable.load(key)` small saved text
 *
 * Colours are Sable theme names ("bg", "wall", "floor", "player", "goal", "box", "accent", "warn",
 * "good", "text", "dim"), "#rrggbb", or "#rrggbbaa" where aa is how opaque it is (00 clear, ff solid).
 */
class LuaGame(
    source: String,
    chunkName: String,
    private val store: GameStore,
    /** The names of the pictures in the game's folder ("car_red.png"), for `sable.image`. */
    private val pictures: Set<String> = emptySet(),
) {
    var cols = DEFAULT_GRID
        private set
    var rows = DEFAULT_GRID
        private set
    var status = ""
        private set
    var score: Long? = null
        private set
    var over: String? = null
        private set

    /** The lean the game reads with `sable.tilt()`; set by the console each frame. */
    var tiltX = 0.0
    var tiltY = 0.0

    private val ops = ArrayList<DrawOp>()
    private var ticks = 0
    private var work = 0L

    /** This game's own `string` table, a copy of Lua's; `("x"):rep(3)` reaches it ([StringIndex]). */
    private val strings = LuaTable().also { t -> forEachKey(STRING_LIB) { k, v -> t.set(k, v) } }
    private val globals = sandbox()
    private val chunk: LuaValue

    init {
        chunk =
            try {
                // "=" makes Lua name the chunk as given: errors read "main.lua:12: ...".
                globals.load(source, "=$chunkName")
            } catch (e: LuaError) {
                throw GameScriptError(e.message?.trim() ?: "Lua error")
            }
    }

    /** Runs the script's top level, then `init()`. */
    fun start() {
        guarded { chunk.call() }
        restart()
    }

    /** Clears the game's report and calls `init()` again. */
    fun restart() {
        over = null
        score = null
        status = ""
        call("init")
    }

    fun update(dtSeconds: Double) {
        call("update", LuaValue.valueOf(dtSeconds))
    }

    fun tilt(step: TiltStep) {
        call("tilt", LuaValue.valueOf(step.key))
    }

    /** True when the game has a `key` function to take it. */
    fun key(name: String): Boolean {
        if (!globals.get("key").isfunction()) return false
        call("key", LuaValue.valueOf(name))
        return true
    }

    /** This frame's shapes, from `draw()`. */
    fun frame(): List<DrawOp> {
        ops.clear()
        call("draw")
        return ops.toList()
    }

    private fun call(
        name: String,
        arg: LuaValue = LuaValue.NONE,
    ) {
        val fn = globals.get(name)
        if (!fn.isfunction()) return
        guarded { if (arg === LuaValue.NONE) fn.call() else fn.call(arg) }
    }

    private fun guarded(block: () -> Unit) {
        ticks = 0
        work = 0
        val outer = running.get()
        running.set(strings)
        LuaString.s_metatable = STRING_META
        try {
            block()
        } catch (e: LuaError) {
            throw GameScriptError(e.message?.trim() ?: "Lua error")
        } catch (_: Runaway) {
            throw GameScriptError("The game took too long and was stopped.")
        } catch (_: StackOverflowError) {
            throw GameScriptError("The game called itself too deeply and was stopped.")
        } catch (_: OutOfMemoryError) {
            throw GameScriptError(TOO_MUCH_MEMORY)
        } finally {
            running.set(outer)
        }
        if (heapTooFull()) {
            // Most of the heap may be garbage; only stop the game if it is still full once collected.
            System.gc()
            if (heapTooFull()) throw GameScriptError(TOO_MUCH_MEMORY)
        }
    }

    private fun heapTooFull(): Boolean {
        val rt = Runtime.getRuntime()
        return rt.totalMemory() - rt.freeMemory() > rt.maxMemory() * HEAP_SHARE
    }

    /** Pattern matching's share of the budget; see [MeteredPatterns]. */
    private fun spend(units: Int) {
        work += units
        if (work > BUDGET_WORK) throw Runaway()
    }

    /** `string.rep` that refuses to make a string longer than [MAX_STRING] instead of filling the heap. */
    private class BoundedRep(
        private val luaj: LuaValue,
    ) : VarArgFunction() {
        override fun invoke(args: Varargs): Varargs {
            val one = args.checkstring(1).length().toLong()
            val n = args.checklong(2)
            val sep = if (args.isnoneornil(3)) 0L else args.checkstring(3).length().toLong()
            if (n > 0 && one * n + sep * (n - 1) > MAX_STRING) {
                throw LuaError("string.rep would make a string longer than $MAX_STRING characters")
            }
            return luaj.invoke(args)
        }
    }

    /**
     * `string.format` that keeps the precision of `%.2f` and the other float conversions, which
     * LuaJ 3.0.1 ignores ("%.2f" of 0.2004 gave "0.2004"). Each other conversion goes to LuaJ's own.
     */
    private class PreciseFormat(
        private val luaj: LuaValue,
    ) : VarArgFunction() {
        override fun invoke(args: Varargs): Varargs {
            val format = args.checkjstring(1)
            val out = StringBuilder()
            var next = 2
            var at = 0
            for (m in SPEC.findAll(format)) {
                out.append(format, at, m.range.first)
                at = m.range.last + 1
                val spec = m.value
                when (spec.last()) {
                    '%' -> {
                        out.append('%')
                    }

                    'f', 'F', 'e', 'E', 'g', 'G' -> {
                        // Lua's own rule: at most two digits of width and of precision.
                        if (WIDE.containsMatchIn(spec)) throw LuaError("invalid format (width or precision too long)")
                        out.append(String.format(Locale.ROOT, spec.replace('F', 'f'), args.checkdouble(next++)))
                    }

                    else -> {
                        out.append(luaj.call(LuaValue.valueOf(spec), args.arg(next++)).tojstring())
                    }
                }
            }
            out.append(format, at, format.length)
            return LuaValue.valueOf(out.toString())
        }

        private companion object {
            val SPEC = Regex("%[-+ #0]*[0-9]*(?:\\.[0-9]+)?[a-zA-Z%]")
            val WIDE = Regex("[0-9]{3,}")
        }
    }

    /** Thrown from the instruction hook; an Error, so a script's `pcall` can't catch it. */
    private class Runaway : Error("instruction budget spent")

    private fun sandbox(): Globals {
        val g = Globals()
        g.load(BaseLib())
        // The other libraries register in package.loaded; package and require are removed below.
        g.load(PackageLib())
        g.load(TableLib())
        // Not g.load(StringLib()): that would point every string's methods at this game's table.
        g.set("string", strings)
        g.load(JseMathLib())
        g.load(Bit32Lib())
        g.load(DebugLib())
        LoadState.install(g)
        LuaC.install(g)
        g.STDOUT = PrintStream(
                object : OutputStream() {
                    override fun write(b: Int) = Unit
                },
            )
        g.STDERR = g.STDOUT
        val hook =
            object : VarArgFunction() {
                override fun invoke(args: Varargs): Varargs {
                    ticks++
                    if (ticks > BUDGET_TICKS) throw Runaway()
                    return LuaValue.NONE
                }
            }
        g.get("debug").get("sethook").call(hook, LuaValue.valueOf(""), LuaValue.valueOf(TICK_INSTRUCTIONS))
        for (name in BLOCKED) g.set(name, LuaValue.NIL)
        strings.set("format", PreciseFormat(strings.get("format")))
        strings.set("rep", BoundedRep(strings.get("rep")))
        // Compiled code is no use without load, but there is no reason to hand it out.
        strings.set("dump", LuaValue.NIL)
        MeteredPatterns.install(strings) { spend(it) }
        g.set("sable", sableTable())
        return g
    }

    private fun sableTable(): LuaTable {
        val t = LuaTable()
        t.set("grid", fn { a -> grid(a) })
        t.set("rect", fn { a -> draw(DrawOp.Rect(a.n(1), a.n(2), a.n(3), a.n(4), a.color(5))) })
        t.set("circle", fn { a -> draw(DrawOp.Circle(a.n(1), a.n(2), a.n(3), a.color(4))) })
        t.set(
            "text",
            fn { a ->
                draw(DrawOp.Text(a.n(1), a.n(2), a.tojstring(3).take(MAX_TEXT), a.optdouble(5, 1.0), a.color(4)))
            },
        )
        t.set("poly", fn { a -> poly(a) })
        t.set(
            "image",
            fn { a ->
                draw(DrawOp.Image(a.n(1), a.n(2), a.n(3), a.n(4), picture(a.checkjstring(5)), a.optdouble(6, 0.0) % FULL_TURN))
            },
        )
        t.set("status", fn { a -> status = a.optjstring(1, "").take(MAX_TEXT) })
        t.set("score", fn { a -> score = a.checklong(1) })
        t.set("over", fn { a -> over = if (a.isnil(1)) null else a.tojstring(1).take(MAX_TEXT) })
        t.set("save", fn { a -> save(a) })
        t.set(
            "tilt",
            object : VarArgFunction() {
                override fun invoke(args: Varargs): Varargs =
                    LuaValue.varargsOf(LuaValue.valueOf(tiltX), LuaValue.valueOf(tiltY))
            },
        )
        t.set(
            "load",
            object : VarArgFunction() {
                override fun invoke(args: Varargs): Varargs =
                    store.get(args.checkjstring(1))?.let { LuaValue.valueOf(it) } ?: LuaValue.NIL
            },
        )
        return t
    }

    private fun grid(a: Varargs) {
        cols = a.checkint(1).coerceIn(1, MAX_GRID)
        rows = a.optint(2, cols).coerceIn(1, MAX_GRID)
    }

    private fun poly(a: Varargs) {
        val points = a.checktable(1)
        val n = points.length()
        if (n < MIN_POINTS * 2 || n % 2 != 0) throw LuaError("sable.poly needs at least 3 corners as x, y pairs")
        val count = minOf(n / 2, MAX_POINTS)
        val xs = ArrayList<Double>(count)
        val ys = ArrayList<Double>(count)
        for (k in 0 until count) {
            xs += points.get(2 * k + 1).checkdouble()
            ys += points.get(2 * k + 2).checkdouble()
        }
        draw(DrawOp.Poly(xs, ys, a.color(2)))
    }

    private fun picture(name: String): String {
        if (name !in pictures) throw LuaError("no picture $name in the game's folder")
        return name
    }

    private fun draw(op: DrawOp) {
        if (ops.size < MAX_OPS) ops += op
    }

    private fun save(a: Varargs) {
        val key = a.checkjstring(1).take(MAX_KEY)
        store.put(key, if (a.isnil(2)) null else a.tojstring(2).take(MAX_VALUE))
    }

    private fun fn(body: (Varargs) -> Unit): VarArgFunction =
        object : VarArgFunction() {
            override fun invoke(args: Varargs): Varargs {
                body(args)
                return LuaValue.NONE
            }
        }

    private fun Varargs.n(i: Int): Double = checkdouble(i)

    private fun Varargs.color(i: Int): String = optjstring(i, "text").take(MAX_COLOR)

    companion object {
        const val DEFAULT_GRID = 10
        const val MAX_GRID = 200
        const val MAX_OPS = 4_000
        const val MAX_POINTS = 64
        private const val MIN_POINTS = 3
        const val MAX_TEXT = 120
        const val MAX_KEY = 40
        const val MAX_VALUE = 2_000
        const val MAX_COLOR = 16
        const val TICK_INSTRUCTIONS = 1_000
        private const val FULL_TURN = 360.0

        /** About 300 000 Lua instructions per call: far more than a frame needs. */
        const val BUDGET_TICKS = 300

        /** Pattern-matching steps per call: a few hundredths of a second at most. */
        const val BUDGET_WORK = 3_000_000L

        /** The longest string `string.rep` makes (a game's strings are short; this is 1 MB). */
        const val MAX_STRING = 1_000_000L

        /** A game is stopped when, after a call, the app's heap is fuller than this share. */
        const val HEAP_SHARE = 0.6

        private const val TOO_MUCH_MEMORY = "The game used too much memory and was stopped."

        /** The string table of the game whose code is running on this thread, for `s:method()`. */
        private val running = ThreadLocal<LuaTable?>()

        /**
         * Lua's string functions, made once. LuaJ keeps one metatable for all strings, shared by every
         * game, so it is set here once and for all: its `__index` looks the method up in the running
         * game's own `string` table, and `__metatable` hides it, so no game can change how another
         * game's strings behave or slip its own code into them.
         */
        private val STRING_LIB: LuaTable =
            Globals()
                .also {
                    it.load(PackageLib())
                    it.load(StringLib())
                }.get("string")
                .checktable()

        private val STRING_META: LuaTable =
            LuaValue.tableOf(arrayOf(LuaValue.INDEX, StringIndex, LuaValue.METATABLE, LuaValue.valueOf("strings")))

        private object StringIndex : VarArgFunction() {
            override fun invoke(args: Varargs): Varargs = running.get()?.get(args.arg(2)) ?: LuaValue.NIL
        }

        private fun forEachKey(
            t: LuaTable,
            body: (LuaValue, LuaValue) -> Unit,
        ) {
            var k = LuaValue.NIL
            while (true) {
                val next = t.next(k)
                k = next.arg1()
                if (k.isnil()) return
                body(k, next.arg(2))
            }
        }

        private val BLOCKED =
            listOf("debug", "dofile", "loadfile", "load", "loadstring", "collectgarbage", "print", "require", "package")
    }
}
