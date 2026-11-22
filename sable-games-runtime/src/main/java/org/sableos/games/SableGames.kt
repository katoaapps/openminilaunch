package org.sableos.games

import android.app.Activity
import android.content.Context
import android.content.Intent
import org.sableos.design.DesignText
import org.sableos.games.runtime.R

enum class SableGameKind { Game, Collection, Console }

data class SableGameEntry(
    val id: String,
    val title: String,
    val description: String,
    val kind: SableGameKind = SableGameKind.Game,
    val accentArgb: Long,
    val iconRes: Int,
)

/** Capabilities selected by the app embedding Sable Games. */
data class SableGamesConfiguration(
    val allowExternalConsoleGames: Boolean = false,
)

/** Public entry point used by standalone flavors and apps embedding Sable Games. */
object SableGames {
    const val EXTRA_GAME_ID = "org.sableos.games.extra.GAME_ID"

    val catalog: List<SableGameEntry> =
        listOf(
            game("sudoku", "Sudoku", "A focused number puzzle.", 0xFF7AD7FF, R.drawable.ic_game_sudoku),
            game(
                "minesweeper",
                "Minesweeper",
                "Clear the field without touching a mine.",
                0xFFFFC857,
                R.drawable.ic_game_minesweeper,
            ),
            game("2048", "2048", "Slide, merge and build the highest tile.", 0xFFFFC72C, R.drawable.ic_game_2048),
            game("bricks", "Bricks", "Keep the ball in play and break every brick.", 0xFFFF8A3D, R.drawable.ic_game_bricks),
            game("snake", "Snake", "Eat, grow and keep clear.", 0xFF8BE04E, R.drawable.ic_game_snake),
            game("blocks", "Blocks", "Fit the falling pieces into full rows.", 0xFF7AA2FF, R.drawable.ic_game_blocks),
            game("reversi", "Reversi", "Outflank the phone's discs.", 0xFFB58CFF, R.drawable.ic_game_reversi),
            game("solitaire", "Solitaire", "Build each suit from ace to king.", 0xFFFF6B8A, R.drawable.ic_game_solitaire),
            game("mahjong", "Mahjong", "Clear matching free tiles.", 0xFF67D8C2, R.drawable.ic_game_mahjong),
            game("words", "Words", "Find the five-letter word.", 0xFFFFD166, R.drawable.ic_game_words),
            game(
                "cards",
                "Card Games",
                "Ten card games in one collection.",
                0xFFE85D75,
                R.drawable.ic_game_cards,
                SableGameKind.Collection,
            ),
            game(
                "snakesladders",
                "Snakes & Ladders",
                "Race to the top of the board.",
                0xFF4DD599,
                R.drawable.ic_game_snakes_ladders,
            ),
            game("ludo", "Ludo", "Bring every piece home.", 0xFF5B8DEF, R.drawable.ic_game_ludo),
            game("scaffold", "Scaffold", "Climb, explore and keep moving.", 0xFFFF9F43, R.drawable.ic_game_scaffold),
            game(
                "console",
                "Tilt Games",
                "Eight motion-controlled games.",
                0xFF4B8CFF,
                R.drawable.ic_game_tilt,
                SableGameKind.Console,
            ),
        )

    private val entriesById = catalog.associateBy(SableGameEntry::id)
    @Volatile private var configuration = SableGamesConfiguration()

    fun initialize(context: Context) {
        DesignText.t.bindTo(context.applicationContext)
        GamesText.t.bindTo(context.applicationContext)
    }

    fun initialize(context: Context, configuration: SableGamesConfiguration) {
        this.configuration = configuration
        initialize(context)
    }

    fun allowsExternalConsoleGames(): Boolean = configuration.allowExternalConsoleGames

    fun entry(gameId: String): SableGameEntry? = entriesById[gameId]

    /** Creates an in-process intent. Hosts must include games-console for the console route. */
    fun intent(context: Context, gameId: String): Intent {
        requireNotNull(entry(gameId)) { "Unknown Sable game: $gameId" }
        val intent =
            if (gameId == "console") {
                Intent().setClassName(context, "org.sableos.games.console.ConsoleActivity")
            } else {
                Intent(context, taskActivity(gameId)).putExtra(EXTRA_GAME_ID, gameId)
            }
        if (context !is Activity) intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return intent
    }

    internal fun taskActivity(gameId: String): Class<out SableGameActivity> =
        when (gameId) {
            "sudoku" -> SudokuActivity::class.java
            "minesweeper" -> MinesweeperActivity::class.java
            "2048" -> Game2048Activity::class.java
            "bricks" -> BricksActivity::class.java
            "snake" -> SnakeActivity::class.java
            "blocks" -> BlocksActivity::class.java
            "reversi" -> ReversiActivity::class.java
            "solitaire" -> SolitaireActivity::class.java
            "mahjong" -> MahjongActivity::class.java
            "words" -> WordsActivity::class.java
            "cards" -> CardGamesActivity::class.java
            "snakesladders" -> SnakesAndLaddersActivity::class.java
            "ludo" -> LudoActivity::class.java
            "scaffold" -> ScaffoldActivity::class.java
            else -> error("Unsupported Sable game task route: $gameId")
        }

    private fun game(
        id: String,
        title: String,
        description: String,
        accentArgb: Long,
        iconRes: Int,
        kind: SableGameKind = SableGameKind.Game,
    ) = SableGameEntry(id, title, description, kind, accentArgb, iconRes)
}
