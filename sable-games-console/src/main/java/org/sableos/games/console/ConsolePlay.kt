package org.sableos.games.console

import android.view.KeyEvent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.isActive
import org.sableos.games.GamesText
import java.io.IOException

/** One run of a game: the Lua game, what it last reported, and the console's own state around it. */
class PlaySession(
    private val manifest: GameManifest,
    lua: LuaGame?,
    startError: String?,
    private val progress: ConsoleProgress,
    keysOnly: Boolean,
    /** The game's pictures for `sable.image`, by file name. */
    val pictures: Map<String, ImageBitmap> = emptyMap(),
) {
    /** The running game; let go when it stops with an error, so the memory it held is freed. */
    private var lua: LuaGame? = lua

    var ops by mutableStateOf(emptyList<DrawOp>())
        private set
    var status by mutableStateOf("")
        private set
    var over by mutableStateOf<String?>(null)
        private set
    var error by mutableStateOf(startError)
        private set
    var paused by mutableStateOf(false)
    var keysOnly by mutableStateOf(keysOnly)
    val cols: Int get() = lua?.cols ?: LuaGame.DEFAULT_GRID
    val rows: Int get() = lua?.rows ?: LuaGame.DEFAULT_GRID

    /** The best score of this run, saved when the run pauses, ends or closes (not every frame). */
    private var unsaved: Long? = null

    val running: Boolean get() = !paused && over == null && error == null

    fun tick(
        dt: Double,
        lean: Pair<Double, Double>,
    ) = safely {
        it.tiltX = lean.first
        it.tiltY = lean.second
        it.update(dt)
    }

    fun step(step: TiltStep) = safely { it.tilt(step) }

    fun key(name: String) = safely { it.key(name) }

    fun restart() {
        save()
        safely { it.restart() }
    }

    fun pause() {
        paused = true
        save()
    }

    /** Keeps the run's score if it beat the saved best. */
    fun save() {
        unsaved?.let { progress.offer(manifest, it) }
        unsaved = null
    }

    private fun safely(block: (LuaGame) -> Unit) {
        val game = lua ?: return
        if (error != null) return
        try {
            block(game)
            ops = game.frame()
            status = game.status
            game.score?.let { unsaved = it }
            if (over == null && game.over != null) {
                over = game.over
                save()
            } else {
                over = game.over
            }
        } catch (e: GameScriptError) {
            error = e.message
            ops = emptyList()
            lua = null
            save()
        }
    }

    companion object {
        fun open(
            state: ConsoleState,
            game: ConsoleGame,
            keysOnly: Boolean,
        ): PlaySession {
            val m = game.manifest
            val store = state.progress.store(m.id)
            return try {
                val pictures = state.library.pictures(game)
                val lua = LuaGame(state.library.script(game), m.entry, store, pictures.keys)
                PlaySession(m, lua, null, state.progress, keysOnly, pictures).also { session ->
                    session.safely { it.start() }
                }
            } catch (e: GameScriptError) {
                PlaySession(m, null, e.message, state.progress, keysOnly)
            } catch (e: IOException) {
                PlaySession(m, null, e.message ?: "can't read ${m.entry}", state.progress, keysOnly)
            }
        }
    }
}

@Composable
internal fun ColumnScope.PlayScreen(
    state: ConsoleState,
    game: ConsoleGame,
    keysOnly: Boolean,
) {
    val m = game.manifest
    val tiltGame = m.usesTilt
    val session = remember(m.id) { PlaySession.open(state, game, keysOnly) }
    val held = remember { mutableSetOf<Int>() }
    val tilt = state.tilt

    fun quit() {
        session.save()
        state.progress.markPlayed(m.id)
        state.refresh()
        state.route = ConsoleRoute.Library
    }
    DisposableEffect(m.id) {
        state.progress.markPlayed(m.id)
        onDispose { session.save() }
    }
    DisposableEffect(session.keysOnly) {
        if (tiltGame && !session.keysOnly) {
            tilt.filter.sensitivity = state.progress.sensitivity
            tilt.onStep = { if (session.running) session.step(it) }
            tilt.start()
        } else {
            tilt.stop()
        }
        onDispose {
            tilt.onStep = null
            tilt.stop()
        }
    }
    LaunchedEffect(session, session.running) {
        if (!session.running) return@LaunchedEffect
        var last = 0L
        while (isActive && session.running) {
            withFrameNanos { now ->
                val dt = if (last == 0L) 0.0 else (now - last) / NANOS
                last = now
                val lean =
                    ConsoleKeys.heldLean(held, tiltGame)
                        ?: if (tiltGame && !session.keysOnly) tilt.x to tilt.y else 0.0 to 0.0
                session.tick(dt, lean)
            }
        }
    }
    val leftAt = remember(session) { state.backgrounded }
    LaunchedEffect(state.backgrounded) { if (state.backgrounded != leftAt && session.running) session.pause() }
    val menu = remember(session) { PauseMenu(state, session, tiltGame) { quit() } }
    ConsoleKeyHandler { event ->
        if (event.action == KeyEvent.ACTION_UP) return@ConsoleKeyHandler held.remove(event.keyCode)
        if (event.action != KeyEvent.ACTION_DOWN) return@ConsoleKeyHandler false
        when {
            session.paused -> {
                menu.key(event.keyCode)
            }

            session.error != null -> {
                quit()
                true
            }

            session.over != null -> {
                overKey(event.keyCode, session) { quit() }
            }

            else -> {
                playKey(event, tiltGame, session, held)
            }
        }
    }
    Row(
        modifier = Modifier.fillMaxWidth().height(32.dp).padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(m.name, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
        Text(
            session.status,
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.End,
            modifier = Modifier.weight(1f),
        )
    }
    Box(modifier = Modifier.weight(1f).fillMaxWidth().padding(4.dp), contentAlignment = Alignment.Center) {
        val error = session.error
        when {
            error != null -> {
                Text(
                    GamesText.t("console_stopped", error),
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(16.dp),
                )
            }

            session.paused -> {
                menu.Show()
            }

            else -> {
                GameCanvas(session.ops, session.cols, session.rows, Modifier.fillMaxSize(), session.pictures)
                session.over?.let { OverBanner(it) }
            }
        }
    }
    ConsoleHint(
        when {
            session.error != null -> GamesText.t("console_back_library")
            session.paused -> GamesText.t("console_pause_hint")
            session.over != null -> GamesText.t("console_over_hint")
            else -> GamesText.t("console_play_hint")
        },
    )
}

private const val NANOS = 1_000_000_000.0

private fun overKey(
    keyCode: Int,
    session: PlaySession,
    quit: () -> Unit,
): Boolean {
    when (keyCode) {
        KeyEvent.KEYCODE_SPACE, in ConsoleKeys.ENTER_KEYS -> session.restart()
        KeyEvent.KEYCODE_BACK, KeyEvent.KEYCODE_ESCAPE -> quit()
        else -> return false
    }
    return true
}

private fun playKey(
    event: KeyEvent,
    tiltGame: Boolean,
    session: PlaySession,
    held: MutableSet<Int>,
): Boolean {
    val char = event.unicodeChar.takeIf { it > 0 }?.toChar()
    when (val key = ConsoleKeys.inGame(event.keyCode, char, tiltGame)) {
        is ConsoleKey.Step -> {
            held += event.keyCode
            if (event.repeatCount == 0) session.step(key.step)
        }

        ConsoleKey.Pause -> {
            session.pause()
        }

        is ConsoleKey.Named -> {
            session.key(key.name)
        }

        null -> {
            return false
        }
    }
    return true
}

@Composable
private fun OverBanner(text: String) {
    Box(
        modifier =
            Modifier
                .background(MaterialTheme.colorScheme.surface.copy(alpha = BANNER_ALPHA), RoundedCornerShape(6.dp))
                .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        Text(text, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center)
    }
}

private const val BANNER_ALPHA = 0.92f

/** The pause menu: each line has a letter key, and the trackpad moves between them. */
private class PauseMenu(
    private val state: ConsoleState,
    private val session: PlaySession,
    private val tiltGame: Boolean,
    private val quit: () -> Unit,
) {
    private var selected by mutableIntStateOf(0)

    /** Bumped when a setting shown in the menu changes, so the menu redraws. */
    private var version by mutableIntStateOf(0)

    private data class Item(
        val letter: Char,
        val label: String,
        val value: String = "",
        val act: () -> Unit,
    )

    private fun items(): List<Item> {
        val list =
            mutableListOf(
                Item('R', GamesText.t("console_resume")) { session.paused = false },
                Item('N', GamesText.t("console_new_game")) {
                    session.restart()
                    session.paused = false
                },
            )
        if (tiltGame) {
            if (!session.keysOnly) {
                // The phone's angle when L is pressed becomes level, and play goes on.
                list +=
                    Item('L', GamesText.t("console_relevel"), GamesText.t("console_relevel_how")) {
                        state.tilt.level()
                        session.paused = false
                    }
                list +=
                    Item(
                        'T',
                        GamesText.t("console_sensitivity"),
                        GamesText.t("console_sens_" + state.progress.sensitivity.key),
                    ) {
                        val next = state.progress.sensitivity.next()
                        state.progress.sensitivity = next
                        state.tilt.filter.sensitivity = next
                        version++
                    }
            }
            list +=
                Item(
                    'K',
                    GamesText.t("console_play_with_keys"),
                    GamesText.t(if (session.keysOnly) "console_on" else "console_off"),
                ) {
                    session.keysOnly = !session.keysOnly
                }
        }
        list += Item('Q', GamesText.t("console_quit")) { quit() }
        return list
    }

    fun key(keyCode: Int): Boolean {
        val list = items()
        when (keyCode) {
            KeyEvent.KEYCODE_DPAD_UP -> {
                selected = (selected - 1).coerceAtLeast(0)
            }

            KeyEvent.KEYCODE_DPAD_DOWN -> {
                selected = (selected + 1).coerceAtMost(list.size - 1)
            }

            KeyEvent.KEYCODE_SPACE, in ConsoleKeys.ENTER_KEYS -> {
                list.getOrNull(selected)?.act?.invoke()
            }

            KeyEvent.KEYCODE_BACK, KeyEvent.KEYCODE_ESCAPE -> {
                quit()
            }

            else -> {
                val letter = KeyEvent.keyCodeToString(keyCode).removePrefix("KEYCODE_").singleOrNull()
                list.firstOrNull { it.letter == letter }?.act?.invoke() ?: return false
            }
        }
        return true
    }

    @Composable
    fun Show() {
        @Suppress("UNUSED_VARIABLE")
        val redraw = version
        val list = items()
        Column(modifier = Modifier.fillMaxSize()) {
            Text(
                GamesText.t("console_paused"),
                fontSize = 22.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            )
            list.forEachIndexed { i, item ->
                ConsoleRow(
                    title = item.label,
                    selected = i == selected,
                    onClick = item.act,
                    lead = {
                        Text(
                            item.letter.toString(),
                            color = MaterialTheme.colorScheme.primary,
                            fontSize = 13.sp,
                            modifier = Modifier.width(16.dp),
                        )
                    },
                ) {
                    if (item.value.isNotEmpty()) {
                        Text(
                            item.value,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}
