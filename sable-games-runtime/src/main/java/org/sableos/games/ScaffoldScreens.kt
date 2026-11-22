package org.sableos.games

import android.content.Context
import android.content.ContextWrapper
import android.view.KeyEvent
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import org.sableos.design.SableActionButton

/** The words Sable Scaffold shows. English only for now; the shared strings files hold the other games' lines. */
internal object ScaffoldText {
    const val NAME = "Sable Scaffold"
    const val TAGLINE = "Climb the site. Dodge what rolls down. Rescue Juno."
    const val PLAY = "Play"
    const val LEVELS = "Levels"
    const val CONTROLS = "Controls"
    const val HOW_TO_PLAY = "How to play"
    const val RESUME = "Resume"
    const val RESTART = "Restart level"
    const val MENU = "Menu"
    const val NEXT = "Next level"
    const val RETRY = "Try again"
    const val PLAY_AGAIN = "Play again"
    const val PAUSE = "Pause"
    const val PAUSED = "Paused"
    const val RESET = "Reset to defaults"
    const val LOCKED = "Locked"
    const val CLEARED = "Cleared"
    const val NEXT_UP = "Next"
    const val OUT_OF_HEARTS = "Out of hearts"
    const val RESCUED = "Juno is safe"
    const val SELECT_LEVEL = "Select level"
    const val PRESS_A_KEY = "Press a key"
    const val KEY_TAKEN = "That key is kept for menus. Pick another."

    fun hint(
        keys: ScaffoldKeys,
        profile: DeviceProfile,
    ): String {
        val k = ScaffoldKeys.Companion::label
        val base =
            "${k(keys.keyFor(ScaffoldAction.Left))} left, ${k(keys.keyFor(ScaffoldAction.Right))} right, " +
                "${k(keys.keyFor(ScaffoldAction.Up))} up a ladder, ${k(keys.keyFor(ScaffoldAction.Down))} down, " +
                "${k(keys.keyFor(ScaffoldAction.Jump))} jumps, P pauses."
        return if (profile.letterMoves) base else "$base Arrows and the trackpad work too."
    }

    fun levelTitle(level: ScaffoldLevel): String = "${level.number} · ${level.name.uppercase()}"
}

/** The router [MainActivity]'s key host talks to: the screen showing sets what a key-down and key-up do. */
internal class ScaffoldKeyRouter {
    var down: ((GameKeyPress) -> Boolean)? = null
    var up: ((Int) -> Unit)? = null
}

/** The keys held now, turned into [ScaffoldInput] each frame. */
internal class ScaffoldController {
    val held = mutableSetOf<Int>()
    var jumpQueued = false
    var touch = ScaffoldInput()
    private var touchJumpBefore = false

    fun clear() {
        held.clear()
        jumpQueued = false
        touch = ScaffoldInput()
        touchJumpBefore = false
    }

    fun input(
        keys: ScaffoldKeys,
        profile: DeviceProfile,
    ): ScaffoldInput {
        var left = touch.left
        var right = touch.right
        var up = touch.up
        var down = touch.down
        for (code in held) {
            when (keys.actionFor(code, profile)) {
                ScaffoldAction.Left -> left = true
                ScaffoldAction.Right -> right = true
                ScaffoldAction.Up -> up = true
                ScaffoldAction.Down -> down = true
                else -> Unit
            }
        }
        val jump = jumpQueued || (touch.jump && !touchJumpBefore)
        touchJumpBefore = touch.jump
        jumpQueued = false
        return ScaffoldInput(left, right, up, down, jump)
    }
}

private sealed interface ScaffoldScreen {
    data object Title : ScaffoldScreen

    data object Levels : ScaffoldScreen

    data object Controls : ScaffoldScreen

    data object HowTo : ScaffoldScreen

    data class Play(
        val level: Int,
        val run: ScaffoldRun,
        val serial: Int,
    ) : ScaffoldScreen
}

@Composable
internal fun ScaffoldApp() {
    val context = LocalContext.current
    val store = remember { PrefsScaffoldStore(context.applicationContext) }
    val profile = LocalDeviceProfile.current
    val host = LocalGameKeyHost.current
    val router = remember { ScaffoldKeyRouter() }
    DisposableEffect(host, router) {
        host.handler = { press -> router.down?.invoke(press) ?: false }
        host.upHandler = { press -> router.up?.invoke(press.keyCode) }
        onDispose {
            host.handler = null
            host.upHandler = null
        }
    }
    var screen by remember { mutableStateOf<ScaffoldScreen>(ScaffoldScreen.Title) }
    var keys by remember { mutableStateOf(store.keys) }
    var serial by remember { mutableIntStateOf(0) }
    when (val current = screen) {
        ScaffoldScreen.Title -> {
            TitleScreen(
                keys = keys,
                profile = profile,
                router = router,
                onPlay = { screen = ScaffoldScreen.Play(1, ScaffoldRun(), serial++) },
                onLevels = { screen = ScaffoldScreen.Levels },
                onControls = { screen = ScaffoldScreen.Controls },
                onHowTo = { screen = ScaffoldScreen.HowTo },
            )
        }

        ScaffoldScreen.HowTo -> {
            MenuBackdrop {
                ScaffoldHowTo(
                    keys = keys,
                    profile = profile,
                    router = router,
                    onBack = { screen = ScaffoldScreen.Title },
                )
            }
        }

        ScaffoldScreen.Levels -> {
            LevelsScreen(
                store = store,
                keys = keys,
                profile = profile,
                router = router,
                onStart = { level -> screen = ScaffoldScreen.Play(level, ScaffoldRun(), serial++) },
                onBack = { screen = ScaffoldScreen.Title },
            )
        }

        ScaffoldScreen.Controls -> {
            ControlsScreen(
                keys = keys,
                profile = profile,
                router = router,
                onChange = {
                    keys = it
                    store.keys = it
                },
                onBack = { screen = ScaffoldScreen.Title },
            )
        }

        is ScaffoldScreen.Play -> {
            PlayScreen(
                level = current.level,
                startRun = current.run,
                store = store,
                keys = keys,
                profile = profile,
                router = router,
                onMenu = { screen = ScaffoldScreen.Title },
                onNext = { level, run -> screen = ScaffoldScreen.Play(level, run, serial++) },
            )
        }
    }
}

/** Installs this screen's key handling in [router] while it is shown. */
@Composable
private fun RouteKeys(
    router: ScaffoldKeyRouter,
    onUp: (Int) -> Unit = {},
    onDown: (GameKeyPress) -> Boolean,
) {
    val down by rememberUpdatedState(onDown)
    val up by rememberUpdatedState(onUp)
    DisposableEffect(router) {
        router.down = { down(it) }
        router.up = { up(it) }
        onDispose {
            router.down = null
            router.up = null
        }
    }
}

private enum class MenuKey { Back, Confirm, Left, Right, Up, Down }

/** What a key means in a menu: the same keys that play, plus Enter and Escape. */
private fun menuKey(
    press: GameKeyPress,
    keys: ScaffoldKeys,
    profile: DeviceProfile,
): MenuKey? =
    when (press.keyCode) {
        KeyEvent.KEYCODE_ENTER, KeyEvent.KEYCODE_DPAD_CENTER -> {
            MenuKey.Confirm
        }

        KeyEvent.KEYCODE_ESCAPE, KeyEvent.KEYCODE_BACK -> {
            MenuKey.Back
        }

        else -> {
            when (keys.actionFor(press.keyCode, profile)) {
                ScaffoldAction.Left -> MenuKey.Left
                ScaffoldAction.Right -> MenuKey.Right
                ScaffoldAction.Up -> MenuKey.Up
                ScaffoldAction.Down -> MenuKey.Down
                ScaffoldAction.Jump -> MenuKey.Confirm
                null -> null
            }
        }
    }

private val MenuInk = Color(0xFFFFFFFF)
private val MenuMuted = Color(0xFFC9D4E2)

/** The dusk backdrop of the menus, with [content] over it. */
@Composable
private fun MenuBackdrop(content: @Composable () -> Unit) {
    val art = remember { ScaffoldArt(ScaffoldLevels.level(1)) }
    Box(Modifier.fillMaxSize().background(ScaffoldColors.Night)) {
        Canvas(Modifier.fillMaxSize()) {
            drawRect(
                Brush.verticalGradient(
                    listOf(ScaffoldColors.Night, ScaffoldColors.Dusk, ScaffoldColors.Glow),
                    startY = 0f,
                    endY = size.height,
                ),
            )
            val s = size.width / ScaffoldField.WIDTH
            val top = size.height - ScaffoldField.HEIGHT * s
            drawContext.canvas.save()
            drawContext.canvas.translate(0f, top)
            drawContext.canvas.scale(s, s)
            drawPath(art.skyline, ScaffoldColors.Skyline)
            drawContext.canvas.restore()
        }
        Box(Modifier.fillMaxSize().safeDrawingPadding(), content = { content() })
    }
}

@Composable
private fun MenuButton(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    SableActionButton(text = text, modifier = Modifier.fillMaxWidth(), primary = selected, onClick = onClick)
}

@Composable
private fun TitleScreen(
    keys: ScaffoldKeys,
    profile: DeviceProfile,
    router: ScaffoldKeyRouter,
    onPlay: () -> Unit,
    onLevels: () -> Unit,
    onControls: () -> Unit,
    onHowTo: () -> Unit,
) {
    val items =
        listOf(
            ScaffoldText.PLAY to onPlay,
            ScaffoldText.LEVELS to onLevels,
            ScaffoldText.HOW_TO_PLAY to onHowTo,
            ScaffoldText.CONTROLS to onControls,
        )
    var selected by remember { mutableIntStateOf(0) }
    RouteKeys(router) { press ->
        when (menuKey(press, keys, profile)) {
            MenuKey.Up, MenuKey.Left -> selected = (selected - 1).coerceAtLeast(0)
            MenuKey.Down, MenuKey.Right -> selected = (selected + 1).coerceAtMost(items.lastIndex)
            MenuKey.Confirm -> items[selected].second()
            else -> return@RouteKeys false
        }
        true
    }
    MenuBackdrop {
        BoxWithConstraints(Modifier.fillMaxSize()) {
            val wide = maxWidth > maxHeight * 1.2f
            Column(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .verticalScroll(
                            rememberScrollState(),
                        ).padding(horizontal = 24.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterVertically),
                horizontalAlignment = Alignment.Start,
            ) {
                Text(
                    "SABLE GAMES",
                    color = ScaffoldColors.Hazard,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    letterSpacing = 2.sp,
                )
                Text(
                    "SABLE\nSCAFFOLD",
                    color = MenuInk,
                    fontWeight = FontWeight.Bold,
                    fontSize = if (wide) 40.sp else 44.sp,
                    lineHeight = if (wide) 40.sp else 44.sp,
                )
                Text(ScaffoldText.TAGLINE, color = MenuInk, fontSize = 14.sp)
                Column(
                    Modifier.fillMaxWidth(if (wide) 0.5f else 0.8f),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items.forEachIndexed { i, (label, action) ->
                        MenuButton(label, selected == i) {
                            selected = i
                            action()
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LevelThumb(
    level: ScaffoldLevel,
    locked: Boolean,
    modifier: Modifier,
) {
    Canvas(modifier) {
        val s = size.width / ScaffoldField.WIDTH
        drawRoundRect(ScaffoldColors.Skyline, cornerRadius = CornerRadius(6f))
        level.surfaces.forEach { surface ->
            val color =
                when {
                    locked -> ScaffoldColors.Lattice
                    surface.solid -> ScaffoldColors.ConcreteBlock
                    level.style == BuildStyle.Concrete -> ScaffoldColors.Concrete
                    surface.y == ScaffoldField.GROUND_Y -> ScaffoldColors.Ground
                    else -> ScaffoldColors.Plank
                }
            drawRect(
                color,
                Offset(surface.x0 * s, surface.y * s * 0.62f),
                Size((surface.x1 - surface.x0) * s, if (surface.solid) 4f else 3f),
            )
        }
        if (!locked) {
            level.ladders.forEach {
                drawRect(
                    ScaffoldColors.Steel,
                    Offset((it.x - 5f) * s, it.top * s * 0.62f),
                    Size(
                        3f,
                        (it.bottom - it.top) * s * 0.62f,
                    ),
                )
            }
        }
    }
}

@Composable
private fun LevelsScreen(
    store: ScaffoldStore,
    keys: ScaffoldKeys,
    profile: DeviceProfile,
    router: ScaffoldKeyRouter,
    onStart: (Int) -> Unit,
    onBack: () -> Unit,
) {
    val unlocked = store.unlocked
    var selected by remember { mutableIntStateOf((unlocked - 1).coerceIn(0, ScaffoldLevels.COUNT - 1)) }
    BackHandler(onBack = onBack)
    RouteKeys(router) { press ->
        when (menuKey(press, keys, profile)) {
            MenuKey.Left -> selected = (selected - 1).coerceAtLeast(0)
            MenuKey.Right -> selected = (selected + 1).coerceAtMost(ScaffoldLevels.COUNT - 1)
            MenuKey.Up -> if (selected >= 2) selected -= 2
            MenuKey.Down -> if (selected + 2 < ScaffoldLevels.COUNT) selected += 2
            MenuKey.Confirm -> if (selected + 1 <= unlocked) onStart(selected + 1)
            MenuKey.Back -> onBack()
            null -> return@RouteKeys false
        }
        true
    }
    MenuBackdrop {
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically),
        ) {
            Text(
                ScaffoldText.SELECT_LEVEL.uppercase(),
                color = MenuInk,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                letterSpacing = 1.sp,
            )
            for (row in 0 until 2) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    for (col in 0 until 2) {
                        val index = row * 2 + col
                        val level = ScaffoldLevels.level(index + 1)
                        val locked = level.number > unlocked
                        LevelCard(
                            level = level,
                            locked = locked,
                            selected = selected == index,
                            best = store.best(level.number),
                            bolts = store.bolts(level.number),
                            modifier = Modifier.weight(1f),
                        ) {
                            selected = index
                            if (!locked) onStart(level.number)
                        }
                    }
                }
            }
            Text(
                "${keyPair(keys, ScaffoldAction.Left, ScaffoldAction.Right)} choose · Enter starts · Esc back",
                color = MenuMuted,
                fontSize = 12.sp,
            )
        }
    }
}

private fun keyPair(
    keys: ScaffoldKeys,
    a: ScaffoldAction,
    b: ScaffoldAction,
): String = "${ScaffoldKeys.label(keys.keyFor(a))} / ${ScaffoldKeys.label(keys.keyFor(b))}"

@Composable
private fun LevelCard(
    level: ScaffoldLevel,
    locked: Boolean,
    selected: Boolean,
    best: Int,
    bolts: Int,
    modifier: Modifier,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = Color(0xFF10152A),
        border = BorderStroke(if (selected) 2.dp else 1.dp, if (selected) ScaffoldColors.Hazard else Color(0xFF2A3358)),
    ) {
        Column(Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            LevelThumb(level, locked, Modifier.fillMaxWidth().height(56.dp))
            Text(
                ScaffoldText.levelTitle(level),
                color = MenuInk,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                maxLines = 1,
            )
            Text(
                if (locked) ScaffoldText.LOCKED else "Best %06d · bolts %d/%d".format(best, bolts, level.boltCount),
                color = MenuMuted,
                fontSize = 11.sp,
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun ControlsScreen(
    keys: ScaffoldKeys,
    profile: DeviceProfile,
    router: ScaffoldKeyRouter,
    onChange: (ScaffoldKeys) -> Unit,
    onBack: () -> Unit,
) {
    val actions = ScaffoldAction.entries
    var selected by remember { mutableIntStateOf(0) }
    var listening by remember { mutableStateOf(false) }
    var refused by remember { mutableStateOf(false) }
    BackHandler(onBack = { if (listening) listening = false else onBack() })
    RouteKeys(router) { press ->
        if (listening) {
            refused = false
            when {
                press.keyCode == KeyEvent.KEYCODE_ESCAPE -> {
                    listening = false
                }

                press.keyCode in ScaffoldKeys.RESERVED -> {
                    refused = true
                }

                else -> {
                    onChange(keys.bind(actions[selected], press.keyCode))
                    listening = false
                }
            }
            return@RouteKeys true
        }
        when (menuKey(press, keys, profile)) {
            MenuKey.Up, MenuKey.Left -> selected = (selected - 1).coerceAtLeast(0)
            MenuKey.Down, MenuKey.Right -> selected = (selected + 1).coerceAtMost(actions.size)
            MenuKey.Confirm -> if (selected < actions.size) listening = true else onChange(ScaffoldKeys())
            MenuKey.Back -> onBack()
            null -> return@RouteKeys false
        }
        true
    }
    MenuBackdrop {
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterVertically),
        ) {
            Text(
                ScaffoldText.CONTROLS.uppercase(),
                color = MenuInk,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                letterSpacing = 1.sp,
            )
            actions.forEachIndexed { i, action ->
                val value =
                    if (listening &&
                        selected == i
                    ) {
                        ScaffoldText.PRESS_A_KEY
                    } else {
                        ScaffoldKeys.label(keys.keyFor(action))
                    }
                MenuButton("${actionName(action)} · $value", selected == i) {
                    selected = i
                    listening = true
                }
            }
            MenuButton(ScaffoldText.RESET, selected == actions.size) {
                selected = actions.size
                onChange(ScaffoldKeys())
            }
            val note =
                when {
                    refused -> ScaffoldText.KEY_TAKEN
                    profile.letterMoves -> "Choose a row, press Enter, then the key you want."
                    else -> "Choose a row, press Enter, then the key you want. Arrows and the trackpad always work too."
                }
            Text(note, color = MenuMuted, fontSize = 12.sp)
        }
    }
}

private fun actionName(action: ScaffoldAction): String =
    when (action) {
        ScaffoldAction.Left -> "Run left"
        ScaffoldAction.Right -> "Run right"
        ScaffoldAction.Up -> "Climb up"
        ScaffoldAction.Down -> "Climb down"
        ScaffoldAction.Jump -> "Jump"
    }

private enum class Phase { Playing, Cleared, Dead, Rescued }

private const val NANOS = 1_000_000_000f
private const val MAX_FRAME = 0.05f

@Composable
private fun PlayScreen(
    level: Int,
    startRun: ScaffoldRun,
    store: ScaffoldStore,
    keys: ScaffoldKeys,
    profile: DeviceProfile,
    router: ScaffoldKeyRouter,
    onMenu: () -> Unit,
    onNext: (Int, ScaffoldRun) -> Unit,
) {
    val data = ScaffoldLevels.level(level)
    val art = remember(level) { ScaffoldArt(data) }
    val controller = remember { ScaffoldController() }
    var attempt by remember { mutableIntStateOf(0) }
    val world =
        remember(level, attempt) {
            ScaffoldWorld(data, if (attempt == 0) startRun else startRun.copy(hearts = ScaffoldWorld.START_HEARTS))
        }
    var tick by remember { mutableIntStateOf(0) }
    var paused by remember { mutableStateOf(false) }
    var phase by remember(level, attempt) { mutableStateOf(Phase.Playing) }
    var newBest by remember(level, attempt) { mutableStateOf(false) }
    var touchMode by remember { mutableStateOf(false) }
    var howTo by remember { mutableStateOf(false) }
    var shownScore by remember(level, attempt) { mutableIntStateOf(startRun.score) }

    fun pause(on: Boolean) {
        paused = on && phase == Phase.Playing
        controller.clear()
    }

    fun restart() {
        controller.clear()
        paused = false
        attempt++
    }

    // The activity's own pause (a call, the app switcher) pauses the game and lets go of every key.
    val activity = LocalContext.current.findComponentActivity()
    DisposableEffect(activity) {
        val observer = LifecycleEventObserver { _, event -> if (event == Lifecycle.Event.ON_PAUSE) pause(true) }
        activity?.lifecycle?.addObserver(observer)
        onDispose { activity?.lifecycle?.removeObserver(observer) }
    }
    BackHandler { if (phase == Phase.Playing && !paused) pause(true) else onMenu() }

    LaunchedEffect(world, paused) {
        if (paused) return@LaunchedEffect
        var last = 0L
        while (world.state == WorldState.Playing) {
            withFrameNanos { now ->
                if (last != 0L) {
                    world.step(((now - last) / NANOS).coerceAtMost(MAX_FRAME), controller.input(keys, profile))
                    tick++
                    if (world.score != shownScore) shownScore = world.score
                }
                last = now
            }
        }
        newBest = store.record(level, world.score)
        store.recordBolts(level, world.levelBolts)
        phase =
            when {
                world.state == WorldState.Dead -> Phase.Dead
                level == ScaffoldLevels.COUNT -> Phase.Rescued
                else -> Phase.Cleared
            }
        if (world.state == WorldState.Cleared &&
            level < ScaffoldLevels.COUNT
        ) {
            store.unlocked = maxOf(store.unlocked, level + 1)
        }
        controller.clear()
    }

    fun primary() {
        when {
            paused -> pause(false)
            phase == Phase.Cleared -> onNext(level + 1, world.runState())
            phase == Phase.Dead -> restart()
            phase == Phase.Rescued -> onMenu()
        }
    }

    RouteKeys(
        router,
        onUp = { controller.held.remove(it) },
    ) { press ->
        val code = press.keyCode
        val overlay = paused || phase != Phase.Playing
        when {
            howTo -> {
                return@RouteKeys false
            }

            paused && code == KeyEvent.KEYCODE_H -> {
                howTo = true
                true
            }

            overlay -> {
                when {
                    code == KeyEvent.KEYCODE_ENTER || code == KeyEvent.KEYCODE_DPAD_CENTER ||
                        code == keys.keyFor(ScaffoldAction.Jump) -> primary()

                    code == KeyEvent.KEYCODE_P && paused -> pause(false)

                    code == KeyEvent.KEYCODE_N -> if (phase == Phase.Rescued) onNext(1, ScaffoldRun()) else restart()

                    code == KeyEvent.KEYCODE_M || code == KeyEvent.KEYCODE_ESCAPE -> onMenu()

                    else -> return@RouteKeys false
                }
                true
            }

            code == KeyEvent.KEYCODE_P || code == KeyEvent.KEYCODE_ESCAPE -> {
                pause(true)
                true
            }

            else -> {
                val action = keys.actionFor(code, profile)
                if (action == null) {
                    false
                } else {
                    touchMode = false
                    if (controller.held.add(code) && action == ScaffoldAction.Jump) controller.jumpQueued = true
                    true
                }
            }
        }
    }

    val status =
        when {
            phase == Phase.Dead -> ScaffoldText.OUT_OF_HEARTS
            phase == Phase.Rescued -> ScaffoldText.RESCUED
            phase == Phase.Cleared -> ScaffoldText.CLEARED
            paused -> ScaffoldText.PAUSED
            else -> "%06d".format(shownScore)
        }
    GameFrame(
        title = ScaffoldText.NAME,
        subtitle = ScaffoldText.TAGLINE,
        statusLabel = "level $level of ${ScaffoldLevels.COUNT}",
        status = status,
        hint = ScaffoldText.hint(keys, profile),
        actions =
            listOf(
                GameAction(
                    label = if (paused) ScaffoldText.RESUME else ScaffoldText.PAUSE,
                    key = "P",
                    active = paused,
                ) {
                    pause(!paused)
                },
                GameAction(label = "Restart", key = "N", onClick = ::restart),
            ),
    ) { side ->
        ScaffoldBoard(
            side = side,
            art = art,
            world = world,
            tick = { tick },
            touchMode = touchMode,
            paused = paused,
            phase = phase,
            newBest = newBest,
            best = store.best(level),
            onTouch = { points, isPauseTap, sidePx ->
                touchMode = true
                controller.touch = ScaffoldTouch.input(points, sidePx)
                if (isPauseTap) pause(!paused)
            },
            onPrimary = ::primary,
            onRestart = {
                if (phase == Phase.Rescued) onNext(1, ScaffoldRun()) else restart()
            },
            onMenu = onMenu,
            onResume = { pause(false) },
            onHowTo = { howTo = true },
        )
        if (howTo) {
            Box(Modifier.size(side).background(Color(0xF00B0F1F))) {
                ScaffoldHowTo(keys = keys, profile = profile, router = router, onBack = { howTo = false })
            }
        }
    }
}

private tailrec fun Context.findComponentActivity(): ComponentActivity? =
    when (this) {
        is ComponentActivity -> this
        is ContextWrapper -> baseContext.findComponentActivity()
        else -> null
    }

@Composable
private fun ScaffoldBoard(
    side: Dp,
    art: ScaffoldArt,
    world: ScaffoldWorld,
    tick: () -> Int,
    touchMode: Boolean,
    paused: Boolean,
    phase: Phase,
    newBest: Boolean,
    best: Int,
    onTouch: (List<Pair<Float, Float>>, Boolean, Float) -> Unit,
    onPrimary: () -> Unit,
    onRestart: () -> Unit,
    onMenu: () -> Unit,
    onResume: () -> Unit,
    onHowTo: () -> Unit,
) {
    val measurer = rememberTextMeasurer()
    val colors = MaterialTheme.colorScheme
    val help = if (art.level.number == ScaffoldLevels.COUNT) "Pip!" else "Help!"
    val title = ScaffoldText.levelTitle(art.level)
    val currentTouchMode by rememberUpdatedState(touchMode)
    val updatedOnTouch by rememberUpdatedState(onTouch)
    Box(
        Modifier
            .size(side)
            .clip(RoundedCornerShape(12.dp))
            .border(1.dp, colors.outline, RoundedCornerShape(12.dp))
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent()
                        val pressed = event.changes.filter { it.pressed }.map { it.position.x to it.position.y }
                        val tap =
                            event.type == PointerEventType.Press &&
                                event.changes.any {
                                    ScaffoldTouch.isPause(
                                        it.position.x,
                                        it.position.y,
                                        size.width.toFloat(),
                                    )
                                }
                        updatedOnTouch(pressed, tap, size.width.toFloat())
                        event.changes.forEach { it.consume() }
                    }
                }
            },
    ) {
        Canvas(Modifier.size(side)) {
            tick()
            val hud =
                ScaffoldHud(
                    title = title,
                    bonusLabel = "BONUS ${world.bonusLeft}",
                    scoreLabel = "%06d".format(world.score),
                    help = help,
                )
            drawScaffold(art, world, hud, measurer)
            if (currentTouchMode) drawTouchControls()
        }
        if (paused || phase != Phase.Playing) {
            Overlay(
                side = side,
                world = world,
                phase = phase,
                paused = paused,
                newBest = newBest,
                best = best,
                onPrimary = onPrimary,
                onRestart = onRestart,
                onMenu = onMenu,
                onResume = onResume,
                onHowTo = onHowTo,
            )
        }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawTouchControls() {
    val side = size.width
    val pad = Offset(ScaffoldTouch.PAD_X * side, ScaffoldTouch.PAD_Y * side)
    val radius = ScaffoldTouch.PAD_RADIUS * side
    val ink = Color(0xFFFFFFFF)
    drawCircle(ink, radius, pad, alpha = 0.12f)
    drawCircle(ink, radius, pad, alpha = 0.45f, style = Stroke(2f))
    val arm = radius * 0.62f
    val tip = radius * 0.18f
    listOf(Offset(0f, -arm), Offset(0f, arm), Offset(-arm, 0f), Offset(arm, 0f)).forEach {
        drawCircle(ink, tip, pad + it, alpha = 0.55f)
    }
    val jump = Offset(ScaffoldTouch.JUMP_X * side, ScaffoldTouch.JUMP_Y * side)
    val jumpRadius = ScaffoldTouch.JUMP_RADIUS * side
    drawCircle(ScaffoldColors.Hazard, jumpRadius, jump, alpha = 0.25f)
    drawCircle(ScaffoldColors.Hazard, jumpRadius, jump, alpha = 0.8f, style = Stroke(3f))
    // Pause corner: two bars in the status bar.
    val bar = side * 0.008f
    val px = side * 0.955f
    drawRect(ink, Offset(px - bar * 1.5f, side * 0.016f), Size(bar, side * 0.035f), alpha = 0.8f)
    drawRect(ink, Offset(px + bar * 0.5f, side * 0.016f), Size(bar, side * 0.035f), alpha = 0.8f)
}

@Composable
private fun Overlay(
    side: Dp,
    world: ScaffoldWorld,
    phase: Phase,
    paused: Boolean,
    newBest: Boolean,
    best: Int,
    onPrimary: () -> Unit,
    onRestart: () -> Unit,
    onMenu: () -> Unit,
    onResume: () -> Unit,
    onHowTo: () -> Unit,
) {
    val u = side.value / 720f
    val title =
        when {
            phase == Phase.Dead -> ScaffoldText.OUT_OF_HEARTS
            phase == Phase.Rescued -> ScaffoldText.RESCUED
            phase == Phase.Cleared -> "${ScaffoldText.CLEARED}: ${world.level.name}"
            else -> ScaffoldText.PAUSED
        }
    val detail =
        when {
            phase == Phase.Dead -> {
                "Score %06d · best %06d".format(world.score, best)
            }

            phase == Phase.Cleared || phase == Phase.Rescued -> {
                "Bonus +%d · score %06d%s".format(world.clearBonus, world.score, if (newBest) " · new best" else "")
            }

            else -> {
                ScaffoldText.levelTitle(world.level)
            }
        }
    Box(Modifier.fillMaxSize().background(Color(0xCC0B0F1F)), contentAlignment = Alignment.Center) {
        Column(
            Modifier.fillMaxWidth(0.82f),
            verticalArrangement = Arrangement.spacedBy(maxOf(4f, 10f * u).dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                title.uppercase(),
                color = MenuInk,
                fontWeight = FontWeight.Bold,
                fontSize = maxOf(14f, 34f * u).sp,
                textAlign = TextAlign.Center,
            )
            Text(detail, color = MenuMuted, fontSize = maxOf(11f, 18f * u).sp, textAlign = TextAlign.Center)
            val primaryLabel =
                when {
                    paused -> ScaffoldText.RESUME
                    phase == Phase.Cleared -> ScaffoldText.NEXT
                    phase == Phase.Dead -> ScaffoldText.RETRY
                    else -> ScaffoldText.MENU
                }
            MenuButton("$primaryLabel · Enter", true, if (paused) onResume else onPrimary)
            MenuButton(
                (if (phase == Phase.Rescued) ScaffoldText.PLAY_AGAIN else ScaffoldText.RESTART) + " · N",
                false,
                onRestart,
            )
            if (paused) MenuButton("${ScaffoldText.HOW_TO_PLAY} · H", false, onHowTo)
            if (phase != Phase.Rescued) MenuButton("${ScaffoldText.MENU} · M", false, onMenu)
        }
    }
}

/**
 * How to play: the rules from [GameCatalog.scaffold] and the keys as the player set them. It takes the
 * keys while shown (the play screen hands them over while it is open), and Back or Enter closes it.
 */
@Composable
private fun ScaffoldHowTo(
    keys: ScaffoldKeys,
    profile: DeviceProfile,
    router: ScaffoldKeyRouter,
    onBack: () -> Unit,
) {
    val upstream = router.down
    DisposableEffect(router) {
        router.down = { press ->
            when (menuKey(press, keys, profile)) {
                MenuKey.Back, MenuKey.Confirm -> onBack()
                else -> Unit
            }
            true
        }
        onDispose { router.down = upstream }
    }
    BackHandler(onBack = onBack)
    val page = GameCatalog.scaffold.howTo.first()
    val k = ScaffoldKeys.Companion::label
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically),
    ) {
        Text(
            ScaffoldText.HOW_TO_PLAY.uppercase(),
            color = ScaffoldColors.Hazard,
            fontWeight = FontWeight.Bold,
            fontSize = 12.sp,
            letterSpacing = 2.sp,
        )
        Text(page.title, color = MenuInk, fontWeight = FontWeight.Bold, fontSize = 20.sp)
        page.rules.forEach { Text(it, color = MenuInk, fontSize = 14.sp) }
        Text(
            "KEYS (FROM CONTROLS)",
            color = ScaffoldColors.Hazard,
            fontWeight = FontWeight.Bold,
            fontSize = 12.sp,
            letterSpacing = 2.sp,
        )
        Text(
            ScaffoldAction.entries.joinToString(" · ") { "${actionName(it)} ${k(keys.keyFor(it))}" } + " · Pause P",
            color = MenuInk,
            fontSize = 13.sp,
        )
        Text(
            if (profile.letterMoves) "Enter or Back closes" else "Arrows and the trackpad work too · Enter or Back closes",
            color = MenuMuted,
            fontSize = 12.sp,
        )
    }
}
