package org.sableos.games

import android.annotation.SuppressLint
import android.app.ActivityManager
import android.os.Bundle
import android.view.KeyEvent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalConfiguration
import org.sableos.design.SableGlobalTheme

internal val LocalGameKeyHost = staticCompositionLocalOf { GameKeyHost() }

/** Reusable host activity for one standard Sable game or collection. */
open class SableGameActivity : ComponentActivity() {
    protected open val gameRoute: String
        get() = requireNotNull(intent.getStringExtra(SableGames.EXTRA_GAME_ID)) {
            "SableGameActivity requires ${SableGames.EXTRA_GAME_ID}"
        }

    private val keyHost = GameKeyHost()
    private val consumedKeyCodes = mutableSetOf<Int>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        SableGames.initialize(applicationContext)
        val route = gameRoute
        SableGames.entry(route)?.let { entry ->
            @Suppress("DEPRECATION")
            setTaskDescription(ActivityManager.TaskDescription(entry.title))
        }
        setContent {
            val configuration = LocalConfiguration.current
            SableGlobalTheme(window = window) {
                CompositionLocalProvider(
                    LocalGameKeyHost provides keyHost,
                    LocalDeviceProfile provides DeviceProfile.current(configuration),
                ) {
                    SableGameContent(route)
                }
            }
        }
    }

    @SuppressLint("RestrictedApi")
    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        val taken =
            when (event.action) {
                KeyEvent.ACTION_DOWN -> {
                    val press = event.toGameKeyPress()
                    (keyHost.gate?.invoke(press) ?: (keyHost.handler?.invoke(press) == true)).also { consumed ->
                        if (consumed) consumedKeyCodes += event.keyCode
                    }
                }
                KeyEvent.ACTION_UP -> {
                    keyHost.upHandler?.invoke(event.toGameKeyPress())
                    consumedKeyCodes.remove(event.keyCode)
                }
                else -> false
            }
        return taken || super.dispatchKeyEvent(event)
    }
}

@androidx.compose.runtime.Composable
private fun SableGameContent(route: String) {
    when (route) {
        "sudoku" -> GameShell(GameCatalog.sudoku) { SudokuScreen() }
        "minesweeper" -> GameShell(GameCatalog.minesweeper) { MinesweeperScreen() }
        "2048" -> GameShell(GameCatalog.game2048) { Game2048Screen() }
        "bricks" -> GameShell(GameCatalog.bricks) { BricksScreen() }
        "snake" -> GameShell(GameCatalog.snake) { SnakeScreen() }
        "blocks" -> GameShell(GameCatalog.blocks) { BlocksScreen() }
        "reversi" -> GameShell(GameCatalog.reversi) { ReversiScreen() }
        "solitaire" -> GameShell(GameCatalog.solitaire) { SolitaireScreen() }
        "mahjong" -> GameShell(GameCatalog.mahjong) { MahjongScreen() }
        "words" -> GameShell(GameCatalog.words) { WordsScreen() }
        "cards" -> GameShell(GameCatalog.cards) { IndianGamesApp("Sable Card Games", CARD_GAMES, emptyList()) }
        "snakesladders" -> GameShell(GameCatalog.snakesLadders) { BoardGameApp("snakes") }
        "ludo" -> GameShell(GameCatalog.ludo) { BoardGameApp("ludo") }
        "scaffold" -> ScaffoldApp()
        else -> error("Unsupported Sable game route: $route")
    }
}
