package org.sableos.games.console

import android.net.Uri
import android.os.SystemClock
import android.view.KeyEvent
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import org.sableos.games.GamesText

/** Where the console is. */
sealed interface ConsoleRoute {
    data object Library : ConsoleRoute

    data class Details(
        val id: String,
    ) : ConsoleRoute

    /** "Hold it how you'll play", before a tilt game. */
    data class Level(
        val id: String,
    ) : ConsoleRoute

    data class Play(
        val id: String,
        val keysOnly: Boolean,
    ) : ConsoleRoute

    data object Add : ConsoleRoute

    data object Settings : ConsoleRoute
}

/** The library's tabs, in order. */
enum class ConsoleTab(
    val labelKey: String,
) {
    All("console_tab_all"),
    Motion("console_tab_motion"),
    Puzzle("console_tab_puzzle"),
    Keyboard("console_tab_keyboard"),
    Recent("console_tab_recent"),
    ;

    /** The games this tab shows, most recently played first on Recent. */
    fun pick(
        games: List<GameManifest>,
        lastPlayed: (String) -> Long,
    ): List<GameManifest> =
        when (this) {
            All -> games
            Motion -> games.filter { it.shelf == GameShelf.Motion }
            Puzzle -> games.filter { it.shelf == GameShelf.Puzzle }
            Keyboard -> games.filter { it.shelf == GameShelf.Keyboard }
            Recent -> games.filter { lastPlayed(it.id) > 0 }.sortedByDescending { lastPlayed(it.id) }
        }
}

/** Shared state and services for the screens. */
class ConsoleState(
    val library: ConsoleLibrary,
    val progress: ConsoleProgress,
    val tilt: ConsoleTilt,
    val allowExternalGames: Boolean,
    val pickFolder: () -> Unit,
) {
    var route: ConsoleRoute by mutableStateOf(ConsoleRoute.Library)
    var tab by mutableIntStateOf(0)
    var selected by mutableIntStateOf(0)
    var games by mutableStateOf(library.games())
        private set

    /** Changes when progress is saved, so rows showing it redraw. */
    var revision by mutableIntStateOf(0)

    /** Counts trips to the background; a running game pauses on each. */
    var backgrounded by mutableIntStateOf(0)

    /** The folder the owner just picked in Add games. */
    var picked: Uri? by mutableStateOf(null)

    fun refresh() {
        games = library.games()
        revision++
    }

    fun game(id: String): ConsoleGame? = games.firstOrNull { it.manifest.id == id }

    /** Opens a game: tilt games level first, unless the owner plays with keys or there is no sensor. */
    fun play(game: GameManifest) {
        val keys = progress.keysOnly || !tilt.available
        route =
            if (game.usesTilt &&
                !keys
            ) {
                ConsoleRoute.Level(game.id)
            } else {
                ConsoleRoute.Play(game.id, keys || !game.usesTilt)
            }
    }
}

@Composable
fun ConsoleApp(state: ConsoleState) {
    // The app targets API 36, so the system's Back comes through the back dispatcher, not as a key
    // event: hand it to the screen's own Back key (pause, close the menu, back to the library).
    // In the library Back is left to the system, which leaves the app.
    val keys = LocalConsoleKeys.current
    BackHandler(enabled = state.route != ConsoleRoute.Library) {
        if (!consoleBack(keys)) back(state)
    }
    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(modifier = Modifier.fillMaxSize().safeDrawingPadding()) {
            when (val route = state.route) {
                ConsoleRoute.Library -> {
                    LibraryScreen(state)
                }

                is ConsoleRoute.Details -> {
                    state.game(route.id)?.let { DetailsScreen(state, it) } ?: back(state)
                }

                is ConsoleRoute.Level -> {
                    state.game(route.id)?.let { LevelScreen(state, it) } ?: back(state)
                }

                is ConsoleRoute.Play -> {
                    state.game(route.id)?.let { PlayScreen(state, it, route.keysOnly) }
                        ?: back(state)
                }

                ConsoleRoute.Add -> {
                    if (state.allowExternalGames) AddScreen(state) else back(state)
                }

                ConsoleRoute.Settings -> {
                    SettingsScreen(state)
                }
            }
        }
    }
}

private fun back(state: ConsoleState) {
    state.route = ConsoleRoute.Library
}

/** Sends a Back press (down, then up) to the showing screen's key handler; false if none took it. */
internal fun consoleBack(keys: ConsoleKeyHost): Boolean {
    val handler = keys.handler ?: return false
    val now = SystemClock.uptimeMillis()
    val down = KeyEvent(now, now, KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_BACK, 0)
    val taken = handler(down)
    handler(KeyEvent.changeAction(down, KeyEvent.ACTION_UP))
    return taken
}

@Composable
private fun ColumnScope.LibraryScreen(state: ConsoleState) {
    val tabs = ConsoleTab.entries
    val shown =
        remember(state.revision, state.games, state.tab) {
            tabs[state.tab].pick(state.games.map { it.manifest }) { state.progress.lastPlayed(it) }
        }
    val bests =
        remember(state.revision, state.games) {
            state.games.associate {
                it.manifest.id to
                    state.progress.best(it.manifest.id)
            }
        }
    val list = rememberLazyListState()
    val selected = state.selected.coerceIn(0, (shown.size - 1).coerceAtLeast(0))

    fun open(i: Int) {
        shown.getOrNull(i)?.let { state.play(it) }
    }
    ConsoleKeyHandler { event ->
        if (event.action != KeyEvent.ACTION_DOWN) return@ConsoleKeyHandler false
        when (event.keyCode) {
            KeyEvent.KEYCODE_DPAD_UP -> {
                state.selected = (selected - 1).coerceAtLeast(0)
            }

            KeyEvent.KEYCODE_DPAD_DOWN -> {
                state.selected =
                    (selected + 1).coerceAtMost((shown.size - 1).coerceAtLeast(0))
            }

            KeyEvent.KEYCODE_DPAD_LEFT -> {
                state.tab = (state.tab + tabs.size - 1) % tabs.size
            }

            KeyEvent.KEYCODE_DPAD_RIGHT -> {
                state.tab = (state.tab + 1) % tabs.size
            }

            KeyEvent.KEYCODE_SPACE, in ConsoleKeys.ENTER_KEYS -> {
                open(selected)
            }

            KeyEvent.KEYCODE_I -> {
                shown.getOrNull(selected)?.let { state.route = ConsoleRoute.Details(it.id) }
            }

            KeyEvent.KEYCODE_A -> {
                if (state.allowExternalGames) {
                    state.route = ConsoleRoute.Add
                } else {
                    return@ConsoleKeyHandler false
                }
            }

            KeyEvent.KEYCODE_S -> {
                state.route = ConsoleRoute.Settings
            }

            else -> {
                return@ConsoleKeyHandler false
            }
        }
        true
    }
    ConsoleTitle(GamesText.t("console_title"))
    ConsoleTabs(tabs.map { GamesText.t(it.labelKey) }, state.tab) {
        state.tab = it
        state.selected = 0
    }
    LaunchedEffect(selected) { if (shown.isNotEmpty()) list.animateScrollToItem(selected) }
    LazyColumn(state = list, modifier = Modifier.weight(1f)) {
        if (shown.isEmpty()) item { ConsoleRow(GamesText.t("console_empty")) }
        itemsIndexed(shown, key = { _, g -> g.id }) { i, game ->
            val best = bests[game.id]
            ConsoleRow(
                title = game.name,
                line = best?.let { GamesText.t("console_best", it) } ?: GamesText.t("console_not_played"),
                selected = i == selected,
                onClick = {
                    if (i == selected) open(i) else state.selected = i
                },
                lead = { GameTile(game) },
            ) {
                ConsoleBadge(
                    GamesText.t(if (game.usesTilt) "console_badge_tilt" else "console_badge_keys"),
                    game.usesTilt,
                )
            }
        }
    }
    ConsoleActionBar(
        buildList {
            add(
                ConsoleAction(GamesText.t("console_play"), primary = true) { open(selected) },
            )
            add(
                ConsoleAction(GamesText.t("console_details")) {
                    shown.getOrNull(selected)?.let { state.route = ConsoleRoute.Details(it.id) }
                },
            )
            if (state.allowExternalGames) {
                add(ConsoleAction(GamesText.t("console_add_games")) { state.route = ConsoleRoute.Add })
            }
            add(ConsoleAction(GamesText.t("console_settings")) { state.route = ConsoleRoute.Settings })
        },
    )
}
