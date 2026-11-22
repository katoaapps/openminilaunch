package org.sableos.games.console

import android.view.KeyEvent
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.sableos.games.GamesText

/** Add games: pick a folder with the system picker, then add the games found in it. */
@Composable
internal fun ColumnScope.AddScreen(state: ConsoleState) {
    var found by remember { mutableStateOf<List<FoundGame>?>(null) }
    val added = remember { mutableStateOf(setOf<String>()) }
    var selected by remember { mutableIntStateOf(0) }
    val picked = state.picked
    LaunchedEffect(picked) {
        if (picked != null) {
            found = withContext(Dispatchers.IO) { state.library.scan(picked) }
            selected = 0
        }
    }

    fun add(game: FoundGame) {
        if (game.problem != null) return
        if (state.library.add(game) == null) {
            added.value = added.value + game.folderName
            state.refresh()
        }
    }
    val list = found.orEmpty()
    ConsoleKeyHandler { event ->
        if (event.action != KeyEvent.ACTION_DOWN) return@ConsoleKeyHandler false
        when (event.keyCode) {
            KeyEvent.KEYCODE_BACK, KeyEvent.KEYCODE_ESCAPE -> {
                state.picked = null
                state.route = ConsoleRoute.Library
            }

            KeyEvent.KEYCODE_C -> {
                state.pickFolder()
            }

            KeyEvent.KEYCODE_DPAD_UP -> {
                selected = (selected - 1).coerceAtLeast(0)
            }

            KeyEvent.KEYCODE_DPAD_DOWN -> {
                selected = (selected + 1).coerceAtMost((list.size - 1).coerceAtLeast(0))
            }

            KeyEvent.KEYCODE_SPACE, in ConsoleKeys.ENTER_KEYS -> {
                list.getOrNull(selected)?.let { add(it) }
                    ?: state.pickFolder()
            }

            else -> {
                return@ConsoleKeyHandler false
            }
        }
        true
    }
    ConsoleTitle(GamesText.t("console_add_title"), back = {
        state.picked = null
        state.route = ConsoleRoute.Library
    })
    Text(
        GamesText.t("console_add_help"),
        fontSize = 14.sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
    )
    LazyColumn(modifier = Modifier.weight(1f)) {
        val shown = found
        if (shown != null) {
            item {
                ConsoleSection(
                    if (shown.isEmpty()) {
                        GamesText.t(
                            "console_none_found",
                        )
                    } else {
                        GamesText.t("console_found", shown.size)
                    },
                )
            }
        }
        itemsIndexed(list) { i, game ->
            val done = game.folderName in added.value
            ConsoleRow(
                title = game.manifest?.name ?: game.folderName,
                line = game.problem?.let { GamesText.t("console_cant_add", it) } ?: game.manifest?.summary.orEmpty(),
                selected = i == selected,
                onClick = { add(game) },
                lead = { game.manifest?.let { GameTile(it) } },
            ) {
                if (game.problem == null) {
                    Row {
                        Text(
                            GamesText.t(if (done) "console_added" else "console_add"),
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
            }
        }
    }
    ConsoleHint(GamesText.t("console_sandbox_note"))
    ConsoleActionBar(listOf(ConsoleAction(GamesText.t("console_choose_folder"), primary = true) { state.pickFolder() }))
}

/** Settings: how far to tilt, and whether to play tilt games with the keys. */
@Composable
internal fun ColumnScope.SettingsScreen(state: ConsoleState) {
    var sensitivity by remember { mutableStateOf(state.progress.sensitivity) }
    var keysOnly by remember { mutableStateOf(state.progress.keysOnly) }

    fun cycle() {
        sensitivity = sensitivity.next()
        state.progress.sensitivity = sensitivity
    }

    fun toggle() {
        keysOnly = !keysOnly
        state.progress.keysOnly = keysOnly
    }
    ConsoleKeyHandler { event ->
        if (event.action != KeyEvent.ACTION_DOWN) return@ConsoleKeyHandler false
        when (event.keyCode) {
            KeyEvent.KEYCODE_BACK, KeyEvent.KEYCODE_ESCAPE -> state.route = ConsoleRoute.Library
            KeyEvent.KEYCODE_T -> cycle()
            KeyEvent.KEYCODE_K -> toggle()
            else -> return@ConsoleKeyHandler false
        }
        true
    }
    ConsoleTitle(GamesText.t("console_settings_title"), back = { state.route = ConsoleRoute.Library })
    ConsoleRow(GamesText.t("console_sensitivity"), onClick = { cycle() }, lead = { Letter('T') }) {
        Text(
            GamesText.t("console_sens_" + sensitivity.key),
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
    ConsoleRow(GamesText.t("console_play_with_keys"), onClick = { toggle() }, lead = { Letter('K') }) {
        Text(
            GamesText.t(if (keysOnly) "console_on" else "console_off"),
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
    if (!state.tilt.available) {
        Text(
            GamesText.t("console_no_sensor"),
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(12.dp),
        )
    }
}

@Composable
private fun Letter(c: Char) {
    Text(c.toString(), color = MaterialTheme.colorScheme.primary, fontSize = 13.sp)
}
