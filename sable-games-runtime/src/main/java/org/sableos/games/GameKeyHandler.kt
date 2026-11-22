package org.sableos.games

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState

/**
 * Routes key-downs to [onCommand] while this game screen is shown; false leaves the key to the focused
 * view. The keys the player set in Controls for [controls] (by default the game's own, from
 * [GameCatalog]) come first.
 */
@Composable
internal fun GameKeyHandler(
    game: GameKind,
    controls: GameControls? = GameCatalog.controlsFor(game),
    onCommand: (GameCommand) -> Boolean,
) {
    val host = LocalGameKeyHost.current
    val profile = LocalDeviceProfile.current
    val keys = LocalKeyBindings.current
    val bound = controls?.let { BoundKeys(it, keys(it)) }
    val latest by rememberUpdatedState(onCommand)
    DisposableEffect(host, game, profile, bound) {
        host.handler = { press -> GameKeymap.command(game, press, profile, bound)?.let { latest(it) } ?: false }
        host.commandHandler = { latest(it) }
        onDispose {
            host.handler = null
            host.commandHandler = null
        }
    }
}
