package org.sableos.games

import android.content.Context
import android.view.KeyEvent
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** The keys the player set for a game's controls; the shell provides it, defaults elsewhere. */
internal val LocalKeyBindings =
    staticCompositionLocalOf<(GameControls) -> KeyBindings> { { it.defaults(DeviceProfile.Other) } }

/** What the shell tells the game it hosts; null for a screen with no shell (Scaffold, tests). */
internal val LocalGameShell = compositionLocalOf<ShellPlay?> { null }

/**
 * The shell's side of a running game: whether a menu covers it ([suspended], so a timed game stops),
 * the title-menu option, and the top-bar tabs. A game's own extra actions (Undo, Hint, Flag...) go in
 * the pause menu through [setActions].
 */
internal class ShellPlay(
    val spec: GameSpec,
    val suspended: Boolean,
    val option: Int,
    val pauseKey: String?,
    val newKey: String?,
    val onPause: () -> Unit,
    val onNew: () -> Unit,
    val onMenu: () -> Unit,
    val setActions: (List<GameAction>) -> Unit,
)

/** What a key does in a menu. */
internal enum class ShellMenuKey { Back, Confirm, Left, Right, Up, Down }

internal object ShellKeys {
    /** Arrows or the trackpad, I J K L where the phone has no arrows, Enter or Space to choose, Back or Esc to leave. */
    fun menu(
        press: GameKeyPress,
        profile: DeviceProfile,
    ): ShellMenuKey? {
        if (press.alt || press.ctrl) return null
        return when (press.keyCode) {
            KeyEvent.KEYCODE_ENTER, KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_SPACE -> ShellMenuKey.Confirm
            KeyEvent.KEYCODE_ESCAPE, KeyEvent.KEYCODE_BACK -> ShellMenuKey.Back
            KeyEvent.KEYCODE_DPAD_UP -> ShellMenuKey.Up
            KeyEvent.KEYCODE_DPAD_DOWN -> ShellMenuKey.Down
            KeyEvent.KEYCODE_DPAD_LEFT -> ShellMenuKey.Left
            KeyEvent.KEYCODE_DPAD_RIGHT -> ShellMenuKey.Right
            KeyEvent.KEYCODE_I -> ShellMenuKey.Up.takeIf { profile.letterMoves }
            KeyEvent.KEYCODE_K -> ShellMenuKey.Down.takeIf { profile.letterMoves }
            KeyEvent.KEYCODE_J -> ShellMenuKey.Left.takeIf { profile.letterMoves }
            KeyEvent.KEYCODE_L -> ShellMenuKey.Right.takeIf { profile.letterMoves }
            else -> null
        }
    }

    /** The keys bound to the first action of [controls] that gives [command], as shown on a tab. */
    fun keyLabel(
        controls: GameControls?,
        bindings: KeyBindings?,
        command: GameCommand,
    ): String? {
        val action = controls?.actions?.firstOrNull { it.command == command } ?: return null
        return bindings?.keysFor(action.id)?.firstOrNull()?.let { KeyBindings.label(it) }
    }
}

private enum class ShellScreen { Title, HowTo, Controls, Play }

/** The title-menu option, kept between runs. */
private class ShellPrefs(
    context: Context,
) {
    private val prefs = context.getSharedPreferences("sable_shell", Context.MODE_PRIVATE)

    fun option(spec: GameSpec): Int =
        spec.option?.let { prefs.getInt(spec.id + "_option", it.default).coerceIn(0, it.values.lastIndex) } ?: 0

    fun setOption(
        spec: GameSpec,
        value: Int,
    ) = prefs.edit().putInt(spec.id + "_option", value).apply()
}

/**
 * Every game's frame around its play screen, after Scaffold's: a title menu (Play, Continue, How to
 * play, Controls and the game's option), How to play with this phone's keys, Controls to rebind
 * them, and a pause menu. The game stays composed under the menus, so Continue picks up where it was.
 */
@Composable
internal fun GameShell(
    spec: GameSpec,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val profile = LocalDeviceProfile.current
    val host = LocalGameKeyHost.current
    val store = remember { PrefsControlsStore(context.applicationContext) }
    val prefs = remember { ShellPrefs(context.applicationContext) }
    val bindings = remember { mutableStateMapOf<String, KeyBindings>() }
    spec.controls.forEach { c -> if (c.id !in bindings) bindings[c.id] = store.load(c, profile) }
    val bindingsFor: (GameControls) -> KeyBindings = { c -> bindings[c.id] ?: store.load(c, profile) }

    var screen by remember { mutableStateOf(ShellScreen.Title) }
    var returnTo by remember { mutableStateOf(ShellScreen.Title) }
    var started by remember { mutableStateOf(false) }
    var paused by remember { mutableStateOf(false) }
    var serial by remember { mutableIntStateOf(0) }
    var option by remember { mutableIntStateOf(prefs.option(spec)) }
    var gameActions by remember { mutableStateOf(emptyList<GameAction>()) }

    val main = spec.controls.firstOrNull()
    val mainBindings = main?.let { bindingsFor(it) }
    val pauseKeys =
        main
            ?.actions
            ?.filter { it.command == GameCommand.Pause }
            ?.flatMap {
                mainBindings
                    ?.keysFor(
                        it.id,
                    ).orEmpty()
            }.orEmpty()

    fun play() {
        serial++
        started = true
        paused = false
        screen = ShellScreen.Play
    }

    fun newGame() {
        paused = false
        if (host.commandHandler?.invoke(GameCommand.NewGame) != true) serial++
    }

    fun open(next: ShellScreen) {
        returnTo = if (screen == ShellScreen.Play) ShellScreen.Play else ShellScreen.Title
        screen = next
    }

    fun close() {
        screen = returnTo
    }

    val menuHandler = remember { MenuHandlerSlot() }
    val playing = screen == ShellScreen.Play && !paused
    DisposableEffect(host) {
        onDispose { host.gate = null }
    }
    val latestPlaying by rememberUpdatedState(playing)
    val latestPauseKeys by rememberUpdatedState(pauseKeys)
    val gate: (GameKeyPress) -> Boolean? = { press ->
        if (latestPlaying) {
            when {
                press.keyCode == KeyEvent.KEYCODE_BACK || press.keyCode == KeyEvent.KEYCODE_ESCAPE -> {
                    screen = ShellScreen.Title
                    true
                }

                press.keyCode == KeyEvent.KEYCODE_MENU ||
                    (!press.alt && press.repeatCount == 0 && press.keyCode in latestPauseKeys) -> {
                    paused = true
                    true
                }

                else -> {
                    null
                }
            }
        } else {
            // A menu takes the keys it uses; the rest (volume...) go on to the system, not the game.
            menuHandler.handler?.invoke(press) ?: false
        }
    }
    SideEffect { host.gate = gate }
    BackHandler(enabled = screen == ShellScreen.Play || paused || screen != ShellScreen.Title) {
        when {
            paused -> paused = false
            screen == ShellScreen.Play -> screen = ShellScreen.Title
            else -> close()
        }
    }

    val shell =
        ShellPlay(
            spec = spec,
            suspended = !playing,
            option = option,
            pauseKey = pauseKeys.firstOrNull()?.let { KeyBindings.label(it) },
            newKey = ShellKeys.keyLabel(main, mainBindings, GameCommand.NewGame),
            onPause = { paused = true },
            onNew = ::newGame,
            onMenu = { screen = ShellScreen.Title },
            setActions = { if (it != gameActions) gameActions = it },
        )
    val palette = spec.palette
    Box(Modifier.fillMaxSize().background(palette.field)) {
        if (started) {
            // The game draws in its own palette: a dark scheme in its colours, whatever the phone's theme.
            MaterialTheme(colorScheme = palette.scheme()) {
                CompositionLocalProvider(LocalGameShell provides shell, LocalKeyBindings provides bindingsFor) {
                    key(serial) { content() }
                }
            }
        }
        when (screen) {
            ShellScreen.Title -> {
                TitleMenu(spec, started, option, menuHandler, profile, onPlay = ::play, onContinue = {
                    screen = ShellScreen.Play
                }, onHowTo = { open(ShellScreen.HowTo) }, onControls = { open(ShellScreen.Controls) }, onOption = {
                    option = it
                    prefs.setOption(spec, it)
                })
            }

            ShellScreen.HowTo -> {
                HowToScreen(spec, profile, bindingsFor, menuHandler, onBack = ::close)
            }

            ShellScreen.Controls -> {
                ControlsScreen(spec, profile, bindingsFor, menuHandler, onChange = { c, b ->
                    bindings[c.id] = b
                    store.save(c, b)
                }, onBack = ::close)
            }

            ShellScreen.Play -> {
                if (paused) {
                    PauseMenu(spec, gameActions, menuHandler, profile, onResume = { paused = false }, onNew = {
                        newGame()
                    }, onHowTo = { open(ShellScreen.HowTo) }, onControls = { open(ShellScreen.Controls) }, onQuit = {
                        paused = false
                        screen = ShellScreen.Title
                    })
                }
            }
        }
    }
}

/** The key handling of the menu on screen; the shell's gate hands it every key while a menu shows. */
private class MenuHandlerSlot {
    var handler: ((GameKeyPress) -> Boolean)? = null
}

@Composable
private fun MenuKeys(
    slot: MenuHandlerSlot,
    onKey: (GameKeyPress) -> Boolean,
) {
    val latest by rememberUpdatedState(onKey)
    DisposableEffect(slot) {
        slot.handler = { latest(it) }
        onDispose { slot.handler = null }
    }
}

/** A full-screen menu page in the game's colours; it covers the game and takes its touches. */
@Composable
private fun MenuPage(
    palette: GamePalette,
    dim: Boolean = false,
    content: @Composable () -> Unit,
) {
    Surface(
        Modifier.fillMaxSize(),
        color = if (dim) palette.field.copy(alpha = 0.88f) else palette.field,
    ) {
        Box(Modifier.fillMaxSize().safeDrawingPadding()) { content() }
    }
}

@Composable
private fun MenuRow(
    text: String,
    selected: Boolean,
    palette: GamePalette,
    value: String? = null,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().heightIn(min = 40.dp),
        shape = RoundedCornerShape(10.dp),
        color = if (selected) palette.accent else palette.bar,
        contentColor = if (selected) palette.bar else palette.ink,
        border = BorderStroke(1.dp, if (selected) palette.accent else palette.accent.copy(alpha = 0.35f)),
    ) {
        Row(
            Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text.uppercase(),
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                letterSpacing = 1.sp,
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (value != null) Text(value, fontWeight = FontWeight.Bold, fontSize = 14.sp, maxLines = 1)
        }
    }
}

@Composable
private fun Eyebrow(
    text: String,
    palette: GamePalette,
) {
    Text(text.uppercase(), color = palette.accent, fontWeight = FontWeight.Bold, fontSize = 12.sp, letterSpacing = 2.sp)
}

@Composable
private fun TitleMenu(
    spec: GameSpec,
    started: Boolean,
    option: Int,
    slot: MenuHandlerSlot,
    profile: DeviceProfile,
    onPlay: () -> Unit,
    onContinue: () -> Unit,
    onHowTo: () -> Unit,
    onControls: () -> Unit,
    onOption: (Int) -> Unit,
) {
    val opt = spec.option
    val items =
        buildList<Pair<String, () -> Unit>> {
            add((if (spec.id == "cards") "Pick a game" else "Play") to onPlay)
            if (started) add("Continue" to onContinue)
            add("How to play" to onHowTo)
            add("Controls" to onControls)
            if (opt != null) add(opt.name to { onOption((option + 1) % opt.values.size) })
        }
    var selected by remember(started) { mutableIntStateOf(if (started) 1 else 0) }
    MenuKeys(slot) { press ->
        when (ShellKeys.menu(press, profile)) {
            ShellMenuKey.Up -> {
                selected = (selected - 1).coerceAtLeast(0)
            }

            ShellMenuKey.Down -> {
                selected = (selected + 1).coerceAtMost(items.lastIndex)
            }

            ShellMenuKey.Left, ShellMenuKey.Right -> {
                if (opt != null && selected == items.lastIndex) {
                    val step = if (ShellKeys.menu(press, profile) == ShellMenuKey.Left) -1 else 1
                    onOption((option + step + opt.values.size) % opt.values.size)
                }
            }

            ShellMenuKey.Confirm -> {
                items[selected].second()
            }

            // Back on the title leaves the app.
            ShellMenuKey.Back -> {
                return@MenuKeys false
            }

            null -> {
                return@MenuKeys false
            }
        }
        true
    }
    val palette = spec.palette
    MenuPage(palette) {
        BoxWithConstraints(Modifier.fillMaxSize()) {
            val wide = maxWidth > maxHeight * 1.2f
            Column(
                Modifier
                    .fillMaxSize()
                    .verticalScroll(
                        rememberScrollState(),
                    ).padding(horizontal = 24.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically),
            ) {
                Eyebrow("Sable Games", palette)
                Text(
                    spec.title.uppercase(),
                    color = palette.ink,
                    fontWeight = FontWeight.Bold,
                    fontSize = 36.sp,
                    lineHeight = 38.sp,
                )
                Text(spec.tagline, color = palette.muted, fontSize = 14.sp)
                Column(
                    Modifier.fillMaxWidth(if (wide) 0.55f else 0.85f),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    items.forEachIndexed { i, (label, action) ->
                        val value = if (opt != null && i == items.lastIndex) opt.values[option] else null
                        MenuRow(label, selected == i, palette, value) {
                            selected = i
                            action()
                        }
                    }
                }
                val move = if (profile.letterMoves) "I K choose" else "Trackpad or arrows choose"
                Text("$move · Enter selects · Back leaves", color = palette.muted, fontSize = 12.sp)
            }
        }
    }
}

@Composable
private fun PauseMenu(
    spec: GameSpec,
    gameActions: List<GameAction>,
    slot: MenuHandlerSlot,
    profile: DeviceProfile,
    onResume: () -> Unit,
    onNew: () -> Unit,
    onHowTo: () -> Unit,
    onControls: () -> Unit,
    onQuit: () -> Unit,
) {
    val items =
        buildList<Pair<String, () -> Unit>> {
            add("Resume" to onResume)
            gameActions.forEach { a ->
                add(
                    a.label to {
                        a.onClick()
                        onResume()
                    },
                )
            }
            add("New game" to onNew)
            add("How to play" to onHowTo)
            add("Controls" to onControls)
            add("Quit to title" to onQuit)
        }
    var selected by remember { mutableIntStateOf(0) }
    MenuKeys(slot) { press ->
        when (ShellKeys.menu(press, profile)) {
            ShellMenuKey.Up, ShellMenuKey.Left -> selected = (selected - 1).coerceAtLeast(0)
            ShellMenuKey.Down, ShellMenuKey.Right -> selected = (selected + 1).coerceAtMost(items.lastIndex)
            ShellMenuKey.Confirm -> items[selected].second()
            ShellMenuKey.Back -> onResume()
            null -> {
                if (press.keyCode != KeyEvent.KEYCODE_P && press.keyCode != KeyEvent.KEYCODE_MENU) return@MenuKeys false
                onResume()
            }
        }
        true
    }
    val palette = spec.palette
    MenuPage(palette, dim = true) {
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterVertically),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text("PAUSED", color = palette.ink, fontWeight = FontWeight.Bold, fontSize = 26.sp, letterSpacing = 2.sp)
            Column(Modifier.widthIn(max = 280.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                items.forEachIndexed { i, (label, action) ->
                    MenuRow(label, selected == i, palette) {
                        selected = i
                        action()
                    }
                }
            }
            Text("Back again resumes", color = palette.muted, fontSize = 12.sp)
        }
    }
}

/** Every key of [controls] as (what it does, the keys), the player's choices first, then the fixed ones. */
internal fun keyTable(
    controls: GameControls,
    bindings: KeyBindings,
    profile: DeviceProfile,
): List<Pair<String, String>> =
    controls.actions.filter { bindings.keysFor(it.id).isNotEmpty() }.map {
        it.name to
            KeyBindings.labels(bindings.keysFor(it.id))
    } +
        controls.fixed.map { it.name to it.keys(profile) }

@Composable
private fun HowToScreen(
    spec: GameSpec,
    profile: DeviceProfile,
    bindingsFor: (GameControls) -> KeyBindings,
    slot: MenuHandlerSlot,
    onBack: () -> Unit,
) {
    val pages = spec.howTo
    var page by remember { mutableIntStateOf(0) }
    MenuKeys(slot) { press ->
        when (ShellKeys.menu(press, profile)) {
            ShellMenuKey.Left, ShellMenuKey.Up -> page = (page - 1).coerceAtLeast(0)
            ShellMenuKey.Right, ShellMenuKey.Down -> page = (page + 1).coerceAtMost(pages.lastIndex)
            ShellMenuKey.Back, ShellMenuKey.Confirm -> onBack()
            null -> return@MenuKeys false
        }
        true
    }
    val palette = spec.palette
    val current = pages[page]
    // The Cards app has one set of keys per game, on the page of the same index.
    val controls = spec.controls.getOrNull(if (spec.controls.size == pages.size) page else 0)
    MenuPage(palette) {
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Eyebrow("${spec.title} · How to play", palette)
                Spacer(Modifier.weight(1f))
                if (pages.size > 1) Text("Page ${page + 1} of ${pages.size}", color = palette.muted, fontSize = 12.sp)
            }
            Text(current.title, color = palette.ink, fontWeight = FontWeight.Bold, fontSize = 20.sp)
            current.rules.forEachIndexed { i, rule ->
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Box(
                        Modifier.size(22.dp).background(palette.accent, RoundedCornerShape(11.dp)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text("${i + 1}", color = palette.bar, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                    Text(rule, color = palette.ink, fontSize = 14.sp, lineHeight = 19.sp)
                }
            }
            if (controls != null) {
                Eyebrow("Your keys on this phone (${profile.label})", palette)
                keyTable(controls, bindingsFor(controls), profile).forEach { (what, keys) ->
                    Row {
                        Text(what, color = palette.muted, fontSize = 13.sp, modifier = Modifier.width(130.dp))
                        Text(keys, color = palette.ink, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }
                Text("Change any key in Controls.", color = palette.muted, fontSize = 12.sp)
            }
            val nav = if (pages.size > 1) "Left and Right turn the page · " else ""
            Text("${nav}Back closes", color = palette.muted, fontSize = 12.sp)
        }
    }
}

@Composable
private fun ControlsScreen(
    spec: GameSpec,
    profile: DeviceProfile,
    bindingsFor: (GameControls) -> KeyBindings,
    slot: MenuHandlerSlot,
    onChange: (GameControls, KeyBindings) -> Unit,
    onBack: () -> Unit,
) {
    var set by remember { mutableIntStateOf(0) }
    var selected by remember { mutableIntStateOf(0) }
    var listening by remember { mutableStateOf(false) }
    var refused by remember { mutableStateOf(false) }
    val sets = spec.controls
    val controls = sets.getOrNull(set)
    val actions = controls?.actions.orEmpty()
    val bindings = controls?.let(bindingsFor)
    MenuKeys(slot) { press ->
        if (listening && controls != null && bindings != null) {
            refused = false
            when (press.keyCode) {
                KeyEvent.KEYCODE_ESCAPE, KeyEvent.KEYCODE_BACK -> {
                    listening = false
                }

                in KeyBindings.SYSTEM -> {
                    return@MenuKeys false
                }

                in KeyBindings.RESERVED -> {
                    refused = true
                }

                else -> {
                    onChange(controls, bindings.bind(actions[selected].id, press.keyCode))
                    listening = false
                }
            }
            return@MenuKeys true
        }
        when (ShellKeys.menu(press, profile)) {
            ShellMenuKey.Up -> {
                selected = (selected - 1).coerceAtLeast(0)
            }

            ShellMenuKey.Down -> {
                selected = (selected + 1).coerceAtMost(actions.size)
            }

            ShellMenuKey.Left, ShellMenuKey.Right -> {
                if (sets.size > 1) {
                    val step = if (ShellKeys.menu(press, profile) == ShellMenuKey.Left) -1 else 1
                    set = (set + step + sets.size) % sets.size
                    selected = 0
                }
            }

            ShellMenuKey.Confirm -> {
                when {
                    controls == null -> onBack()
                    selected < actions.size -> listening = true
                    else -> onChange(controls, controls.defaults(profile))
                }
            }

            ShellMenuKey.Back -> {
                onBack()
            }

            null -> {
                return@MenuKeys false
            }
        }
        true
    }
    val palette = spec.palette
    MenuPage(palette) {
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Eyebrow("${spec.title} · Controls", palette)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Your keys", color = palette.ink, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                Spacer(Modifier.weight(1f))
                Text("Profile: ${profile.label}", color = palette.muted, fontSize = 12.sp)
            }
            if (sets.size > 1) {
                val name = spec.howTo.getOrNull(set)?.title ?: controls?.id.orEmpty()
                Text("◀ $name ▶", color = palette.accent, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }
            if (controls != null && bindings != null) {
                actions.forEachIndexed { i, action ->
                    val value =
                        if (listening &&
                            selected == i
                        ) {
                            "Press a key…"
                        } else {
                            KeyBindings.labels(bindings.keysFor(action.id))
                        }
                    MenuRow(action.name, selected == i, palette, value) {
                        selected = i
                        listening = true
                    }
                }
                if (actions.isNotEmpty()) {
                    MenuRow("Reset to this phone's defaults", selected == actions.size, palette) {
                        selected = actions.size
                        onChange(controls, controls.defaults(profile))
                    }
                }
                controls.fixed.forEach { fixed ->
                    Row {
                        Text(fixed.name, color = palette.muted, fontSize = 13.sp, modifier = Modifier.width(130.dp))
                        Text(fixed.keys(profile), color = palette.ink, fontSize = 13.sp)
                    }
                }
            }
            val note =
                when {
                    refused -> {
                        "That key is kept for menus and moving. Choose another."
                    }

                    actions.isEmpty() -> {
                        "These keys are part of the game and can't change."
                    }

                    else -> {
                        "Enter on a row, then press the new key. Back, Enter and the arrows stay fixed so you " +
                            "can't lock yourself out. A key already in use swaps with the old one."
                    }
                }
            Text(note, color = if (refused) palette.accent else palette.muted, fontSize = 12.sp)
        }
    }
}

/** A dark Material scheme in the game's colours, for the parts of a game drawn with theme colours. */
internal fun GamePalette.scheme() =
    darkColorScheme(
        primary = accent,
        onPrimary = bar,
        background = field,
        onBackground = ink,
        surface = bar,
        onSurface = ink,
        surfaceVariant = lerp(field, ink, 0.12f),
        onSurfaceVariant = muted,
        outline = lerp(field, accent, 0.6f),
        outlineVariant = lerp(field, ink, 0.25f),
    )
