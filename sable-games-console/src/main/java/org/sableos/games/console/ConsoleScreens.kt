package org.sableos.games.console

import android.view.KeyEvent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.sableos.games.GamesText
import kotlin.math.roundToInt

@Composable
internal fun ColumnScope.DetailsScreen(
    state: ConsoleState,
    game: ConsoleGame,
) {
    val m = game.manifest
    var best by remember(m.id) { mutableStateOf(state.progress.best(m.id)) }

    fun reset() {
        state.progress.reset(m.id)
        best = null
        state.refresh()
    }

    fun remove() {
        state.library.remove(game)
        state.progress.reset(m.id)
        state.refresh()
        state.route = ConsoleRoute.Library
    }
    ConsoleKeyHandler { event ->
        if (event.action != KeyEvent.ACTION_DOWN) return@ConsoleKeyHandler false
        when (event.keyCode) {
            KeyEvent.KEYCODE_BACK, KeyEvent.KEYCODE_ESCAPE -> state.route = ConsoleRoute.Library
            KeyEvent.KEYCODE_SPACE, in ConsoleKeys.ENTER_KEYS -> state.play(m)
            else -> return@ConsoleKeyHandler false
        }
        true
    }
    ConsoleTitle(m.name, back = { state.route = ConsoleRoute.Library })
    Row(
        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        GameTile(m, side = 64)
        Column {
            Text(
                GamesText.t(if (game.builtIn) "console_from_sable" else "console_from_folder") + " · " + m.version,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
    Column(modifier = Modifier.weight(1f).verticalScroll(rememberScrollState())) {
        // How to play comes from the game's own game.toml: plain text, never styled or run.
        ConsoleSection(GamesText.t("console_how_to_play"))
        howToPlay(m.summary, m.controls)?.let {
            Text(
                it.ifEmpty { GamesText.t("console_no_instructions") },
                fontSize = 14.sp,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
            )
        }
        if (m.controls.isNotBlank()) {
            ConsoleSection(GamesText.t("console_controls"))
            Text(
                m.controls,
                fontSize = 14.sp,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
            )
        }
        ConsoleSection(GamesText.t("console_progress"))
        ConsoleRow(GamesText.t("console_progress"), trail = {
            Text(
                best?.let { GamesText.t("console_best", it) } ?: GamesText.t("console_not_played"),
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        })
    }
    ConsoleActionBar(
        listOfNotNull(
            ConsoleAction(GamesText.t("console_play"), primary = true) { state.play(m) },
            ConsoleAction(GamesText.t("console_reset_progress")) { reset() },
            if (game.builtIn) null else ConsoleAction(GamesText.t("console_remove")) { remove() },
        ),
    )
}

/** "Hold it how you'll play": the angle the owner holds now becomes level. */
@Composable
internal fun ColumnScope.LevelScreen(
    state: ConsoleState,
    game: ConsoleGame,
) {
    val tilt = state.tilt
    DisposableEffect(Unit) {
        tilt.filter.sensitivity = state.progress.sensitivity
        tilt.start()
        onDispose { }
    }

    fun start() {
        tilt.level()
        state.route = ConsoleRoute.Play(game.manifest.id, keysOnly = false)
    }

    fun keys() {
        tilt.stop()
        state.route = ConsoleRoute.Play(game.manifest.id, keysOnly = true)
    }
    ConsoleKeyHandler { event ->
        if (event.action != KeyEvent.ACTION_DOWN) return@ConsoleKeyHandler false
        when (event.keyCode) {
            KeyEvent.KEYCODE_SPACE, in ConsoleKeys.ENTER_KEYS -> {
                start()
            }

            KeyEvent.KEYCODE_K -> {
                keys()
            }

            KeyEvent.KEYCODE_BACK, KeyEvent.KEYCODE_ESCAPE -> {
                tilt.stop()
                state.route = ConsoleRoute.Library
            }

            else -> {
                return@ConsoleKeyHandler false
            }
        }
        true
    }
    Column(
        modifier = Modifier.weight(1f).fillMaxWidth().padding(horizontal = 24.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(GamesText.t("console_level_title"), fontSize = 22.sp, fontWeight = FontWeight.SemiBold)
        Text(
            GamesText.t("console_level_body"),
            fontSize = 14.sp,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        val ring = 140
        Box(
            modifier = Modifier.size(ring.dp).border(2.dp, MaterialTheme.colorScheme.surfaceVariant, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Box(Modifier.size(40.dp).border(2.dp, MaterialTheme.colorScheme.primary, CircleShape))
            // The dot shows the lean against the current level; Space makes this angle level.
            val reach = ring / 2 - DOT / 2
            Box(
                Modifier
                    .offset((tilt.x * reach).roundToInt().dp, (tilt.y * reach).roundToInt().dp)
                    .size(DOT.dp)
                    .background(ConsolePalette.Tilt, CircleShape),
            )
        }
        Spacer(Modifier.weight(1f))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            WideButton(GamesText.t("console_start"), primary = true, modifier = Modifier.weight(1f)) { start() }
            WideButton(GamesText.t("console_use_keys"), primary = false, modifier = Modifier.weight(1f)) { keys() }
        }
    }
}

private const val DOT = 16

@Composable
private fun WideButton(
    label: String,
    primary: Boolean,
    modifier: Modifier,
    onClick: () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    Box(
        modifier =
            modifier
                .height(44.dp)
                .then(
                    if (primary) {
                        Modifier.background(scheme.primary, RoundedCornerShape(6.dp))
                    } else {
                        Modifier.border(1.dp, scheme.outline, RoundedCornerShape(6.dp))
                    },
                ).clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            label,
            fontSize = 16.sp,
            fontWeight = if (primary) FontWeight.SemiBold else FontWeight.Normal,
            color = if (primary) scheme.onPrimary else scheme.onSurface,
        )
    }
}

/**
 * A game's How to play text: its summary, cut at [MAX_HOW_TO] characters; empty when its game.toml has
 * neither summary nor controls (the screen then says so); null when only the controls are given.
 */
internal fun howToPlay(
    summary: String,
    controls: String,
): String? =
    when {
        summary.isNotBlank() -> summary.take(MAX_HOW_TO)
        controls.isNotBlank() -> null
        else -> ""
    }

private const val MAX_HOW_TO = 600
