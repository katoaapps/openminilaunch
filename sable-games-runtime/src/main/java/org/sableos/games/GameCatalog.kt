package org.sableos.games

import android.view.KeyEvent
import androidx.compose.ui.graphics.Color

/** The colours of one game's screens: the field behind the board, the top bar and the accent. */
internal data class GamePalette(
    val field: Color,
    val bar: Color,
    val accent: Color,
    val ink: Color = Color(0xFFFFFFFF),
    val muted: Color = Color(0xFFC3CBE0),
)

/** One page of How to play: a heading and the rules, a line each. */
internal data class HowToPage(
    val title: String,
    val rules: List<String>,
)

/** A choice on the title menu, such as Snake's speed; [values] are what it shows. */
internal data class GameOption(
    val name: String,
    val values: List<String>,
    val default: Int,
)

/**
 * What the shell needs to know about a game: its name and colours, its How to play pages, the keys
 * players can rebind ([controls], one set per game in the app) and an optional title-menu [option].
 */
internal data class GameSpec(
    val id: String,
    val title: String,
    val tagline: String,
    val palette: GamePalette,
    val howTo: List<HowToPage>,
    val controls: List<GameControls>,
    val option: GameOption? = null,
    /** Whether P and the Pause tab stop a game that runs on its own; otherwise they just open the menu. */
    val timed: Boolean = false,
)

private typealias Defaults = (DeviceProfile) -> List<Int>

private fun always(vararg codes: Int): Defaults = { codes.toList() }

/** [plain] on a phone with arrows or a trackpad, [letters] on one without (Titan 2, P26). */
private fun byProfile(
    plain: List<Int>,
    letters: List<Int>,
): Defaults = { if (it.letterMoves) letters else plain }

private const val K_I = KeyEvent.KEYCODE_I
private const val K_J = KeyEvent.KEYCODE_J
private const val K_K = KeyEvent.KEYCODE_K
private const val K_L = KeyEvent.KEYCODE_L
private const val K_W = KeyEvent.KEYCODE_W
private const val K_A = KeyEvent.KEYCODE_A
private const val K_S = KeyEvent.KEYCODE_S
private const val K_D = KeyEvent.KEYCODE_D
private const val K_P = KeyEvent.KEYCODE_P
private const val K_N = KeyEvent.KEYCODE_N
private const val K_SPACE = KeyEvent.KEYCODE_SPACE

private val UP = GameCommand.Move(0, -1)
private val DOWN = GameCommand.Move(0, 1)
private val LEFT = GameCommand.Move(-1, 0)
private val RIGHT = GameCommand.Move(1, 0)

/** I J K L on a phone without arrows; nothing extra where the arrows and trackpad move. */
private fun cursorMoves(): List<ControlAction> =
    listOf(
        ControlAction("up", "Up", UP, byProfile(emptyList(), listOf(K_I))),
        ControlAction("left", "Left", LEFT, byProfile(emptyList(), listOf(K_J))),
        ControlAction("down", "Down", DOWN, byProfile(emptyList(), listOf(K_K))),
        ControlAction("right", "Right", RIGHT, byProfile(emptyList(), listOf(K_L))),
    )

/** W A S D everywhere, and I J K L too on a phone without arrows. */
private fun arcadeMoves(
    up: String = "Up",
    down: String = "Down",
): List<ControlAction> =
    listOf(
        ControlAction("up", up, UP, byProfile(listOf(K_W), listOf(K_I, K_W))),
        ControlAction("left", "Left", LEFT, byProfile(listOf(K_A), listOf(K_J, K_A))),
        ControlAction("down", down, DOWN, byProfile(listOf(K_S), listOf(K_K, K_S))),
        ControlAction("right", "Right", RIGHT, byProfile(listOf(K_D), listOf(K_L, K_D))),
    )

private fun action(
    id: String,
    name: String,
    command: GameCommand,
    vararg codes: Int,
) = ControlAction(id, name, command, always(*codes))

private val pause = action("pause", "Pause", GameCommand.Pause, K_P)
private val newGame = action("new", "New game", GameCommand.NewGame, K_N)

private fun letter(
    id: String,
    name: String,
    c: Char,
) = action(id, name, GameCommand.Letter(c), KeyEvent.KEYCODE_A + (c - 'A'))

private val moveKeys = FixedKey("Move") { if (it.letterMoves) "I J K L (set above)" else "Trackpad, arrows" }
private val enterKey = FixedKey("Choose") { if (it.letterMoves) "Enter" else "Enter, trackpad click" }
private val menuKey = FixedKey("Menu") { "Back" }

/** Every game the apps ship, with its rules, keys and colours. */
internal object GameCatalog {
    val snake =
        GameSpec(
            id = "snake",
            title = "Snake",
            tagline = "Eat, grow and keep clear of the walls and your tail.",
            palette = GamePalette(Color(0xFF0D2418), Color(0xFF081A10), Color(0xFF8BE04E)),
            howTo =
                listOf(
                    HowToPage(
                        "Grow as long as you can",
                        listOf(
                            "Steer the snake around the 16 × 16 field. It never stops moving.",
                            "Eat the red apple to grow one segment and score a point.",
                            "Hitting a wall or your own tail ends the game.",
                            "Each apple makes the snake a little faster. Speed on the title menu sets where it starts.",
                        ),
                    ),
                ),
            controls = listOf(GameControls("snake", arcadeMoves() + pause + newGame, listOf(moveKeys, menuKey))),
            option = GameOption("Speed", listOf("Slow", "Normal", "Fast"), 1),
            timed = true,
        )

    val blocks =
        GameSpec(
            id = "blocks",
            title = "Blocks",
            tagline = "Fit the falling pieces into full rows.",
            palette = GamePalette(Color(0xFF0F1530), Color(0xFF0A0E22), Color(0xFF7AA2FF)),
            howTo =
                listOf(
                    HowToPage(
                        "Fill rows to clear them",
                        listOf(
                            "Pieces fall into a 16 × 16 well. Move and turn them as they fall.",
                            "A row with every cell filled clears and scores: 100, 300, 500 or 800 for 1 to 4 rows, times the level.",
                            "Every 10 rows is a new level, and pieces fall faster.",
                            "The game ends when a new piece has no room. The dashed ghost shows where a piece will land.",
                        ),
                    ),
                ),
            controls =
                listOf(
                    GameControls(
                        "blocks",
                        listOf(
                            ControlAction("left", "Left", LEFT, byProfile(listOf(K_A), listOf(K_J, K_A))),
                            ControlAction("right", "Right", RIGHT, byProfile(listOf(K_D), listOf(K_L, K_D))),
                            ControlAction("rotate", "Rotate", UP, byProfile(listOf(K_W), listOf(K_I, K_W))),
                            ControlAction("soft", "Soft drop", DOWN, byProfile(listOf(K_S), listOf(K_K, K_S))),
                            action("drop", "Drop", GameCommand.Primary, K_SPACE),
                            pause,
                            newGame,
                        ),
                        listOf(moveKeys, menuKey),
                    ),
                ),
            timed = true,
        )

    val bricks =
        GameSpec(
            id = "bricks",
            title = "Bricks",
            tagline = "Keep the ball in play and break every brick.",
            palette = GamePalette(Color(0xFF160F2E), Color(0xFF0E0920), Color(0xFFFF8A3D)),
            howTo =
                listOf(
                    HowToPage(
                        "Break the wall",
                        listOf(
                            "Move the paddle to bounce the ball into the wall of bricks. The further from the paddle's centre it lands, the wider it goes.",
                            "Each brick breaks on the first hit. Clear the wall for the next, faster level.",
                            "Miss the ball and you lose a life. You have three.",
                            "Launch the ball to start, and again after a miss. You can drag the paddle too.",
                        ),
                    ),
                ),
            controls =
                listOf(
                    GameControls(
                        "bricks",
                        listOf(
                            ControlAction("left", "Left", LEFT, byProfile(listOf(K_A), listOf(K_J, K_A))),
                            ControlAction("right", "Right", RIGHT, byProfile(listOf(K_D), listOf(K_L, K_D))),
                            action("launch", "Launch", GameCommand.Primary, K_SPACE),
                            pause,
                            newGame,
                        ),
                        listOf(
                            FixedKey("Move") { if (it.letterMoves) "J L (set above)" else "Trackpad, arrows" },
                            menuKey,
                        ),
                    ),
                ),
            timed = true,
        )

    val game2048 =
        GameSpec(
            id = "2048",
            title = "2048",
            tagline = "Slide, merge and build the highest tile.",
            palette = GamePalette(Color(0xFF121633), Color(0xFF0C0F26), Color(0xFFFFC72C)),
            howTo =
                listOf(
                    HowToPage(
                        "Merge your way up",
                        listOf(
                            "Each move slides every tile as far as it goes, up, down, left or right.",
                            "Two equal tiles that meet merge into one of double the value, and it adds to your score.",
                            "A new tile appears after every move.",
                            "The game ends when no move is left. Swipes work too.",
                        ),
                    ),
                ),
            controls = listOf(GameControls("2048", arcadeMoves() + pause + newGame, listOf(moveKeys, menuKey))),
        )

    val sudoku =
        GameSpec(
            id = "sudoku",
            title = "Sudoku",
            tagline = "A focused number puzzle.",
            palette = GamePalette(Color(0xFF111A33), Color(0xFF0B1226), Color(0xFF7AD7FF)),
            howTo =
                listOf(
                    HowToPage(
                        "Fill the grid",
                        listOf(
                            "Put a digit from 1 to 9 in every empty cell.",
                            "Each row, each column and each 3 × 3 box holds every digit once.",
                            "The white digits are given and can't change. A digit that clashes shows in coral.",
                            "Erase or undo freely; the puzzle is done when every cell is right.",
                        ),
                    ),
                ),
            controls =
                listOf(
                    GameControls(
                        "sudoku",
                        cursorMoves() +
                            listOf(
                                action("erase", "Erase", GameCommand.Erase, KeyEvent.KEYCODE_DEL),
                                action("undo", "Undo", GameCommand.Undo, KeyEvent.KEYCODE_U),
                                pause,
                                newGame,
                            ),
                        listOf(
                            moveKeys,
                            FixedKey("Digits 1-9") {
                                if (it ==
                                    DeviceProfile.Titan2
                                ) {
                                    "1-9, or W E R S D F X C V"
                                } else {
                                    "1-9, or W E R S D F Z X C"
                                }
                            },
                            menuKey,
                        ),
                    ),
                ),
        )

    val minesweeper =
        GameSpec(
            id = "minesweeper",
            title = "Minesweeper",
            tagline = "Clear the field without touching a mine.",
            palette = GamePalette(Color(0xFF131A2E), Color(0xFF0C1122), Color(0xFFFF8A3D)),
            howTo =
                listOf(
                    HowToPage(
                        "Find the 10 mines",
                        listOf(
                            "The 9 × 9 field hides 10 mines. Reveal every other cell to win.",
                            "A number tells how many of the eight cells around it hide a mine.",
                            "Flag a cell you think holds a mine so you don't reveal it by mistake.",
                            "Reveal a mine and the game is over.",
                        ),
                    ),
                ),
            controls =
                listOf(
                    GameControls(
                        "minesweeper",
                        cursorMoves() +
                            listOf(
                                action("reveal", "Reveal", GameCommand.Primary, K_SPACE),
                                action("flag", "Flag", GameCommand.Flag, KeyEvent.KEYCODE_F, KeyEvent.KEYCODE_G),
                                pause,
                                newGame,
                            ),
                        listOf(moveKeys, FixedKey("Reveal") { "Enter" }, menuKey),
                    ),
                ),
        )

    val reversi =
        GameSpec(
            id = "reversi",
            title = "Reversi",
            tagline = "Outflank the phone's discs to turn them yours.",
            palette = GamePalette(Color(0xFF0E2420), Color(0xFF091A17), Color(0xFF2FD0B5)),
            howTo =
                listOf(
                    HowToPage(
                        "Turn the board your colour",
                        listOf(
                            "You play the light discs, the phone the dark ones, a move each.",
                            "Place a disc so that one or more of the phone's discs lie in a line between it and another of yours.",
                            "Every disc in those lines turns yours. Gold dots mark the squares you can play.",
                            "When neither side can move, the most discs wins.",
                        ),
                    ),
                ),
            controls =
                listOf(
                    GameControls(
                        "reversi",
                        cursorMoves() + listOf(action("place", "Place", GameCommand.Primary, K_SPACE), pause, newGame),
                        listOf(moveKeys, FixedKey("Place") { "Enter" }, menuKey),
                    ),
                ),
        )

    val solitaire =
        GameSpec(
            id = "solitaire",
            title = "Solitaire",
            tagline = "Build each suit from ace to king.",
            palette = GamePalette(Color(0xFF0F3326), Color(0xFF0A2419), Color(0xFFFFC72C)),
            howTo =
                listOf(
                    HowToPage(
                        "Send every card home",
                        listOf(
                            "Build the four foundations up by suit, from ace to king.",
                            "In the seven columns, put a card on one of the other colour that is one higher.",
                            "Only a king, or a run starting with one, goes in an empty column.",
                            "Turn cards from the stock when you are stuck. You win when all 52 are home.",
                        ),
                    ),
                ),
            controls =
                listOf(
                    GameControls(
                        "solitaire",
                        cursorMoves() +
                            listOf(
                                action("draw", "Draw", GameCommand.Draw, K_SPACE),
                                action("auto", "Send home", GameCommand.Auto, K_A),
                                action("putback", "Put back", GameCommand.Erase, KeyEvent.KEYCODE_DEL),
                                pause,
                                newGame,
                            ),
                        listOf(moveKeys, FixedKey("Pick up, put down") { "Enter" }, menuKey),
                    ),
                ),
        )

    val mahjong =
        GameSpec(
            id = "mahjong",
            title = "Mahjong",
            tagline = "Clear the pile two matching free tiles at a time.",
            palette = GamePalette(Color(0xFF2A1622), Color(0xFF1D0F18), Color(0xFFFF8A8A)),
            howTo =
                listOf(
                    HowToPage(
                        "Clear the pile",
                        listOf(
                            "Pick two tiles with the same face to take them off the pile.",
                            "A tile is free when nothing is on it and its left or right side is open.",
                            "Hint shows a pair that works.",
                            "Clear every tile to win. If no pair is left, deal again.",
                        ),
                    ),
                ),
            controls =
                listOf(
                    GameControls(
                        "mahjong",
                        cursorMoves() +
                            listOf(
                                action("pick", "Pick", GameCommand.Primary, K_SPACE),
                                action("hint", "Hint", GameCommand.Hint, KeyEvent.KEYCODE_H),
                                pause,
                                newGame,
                            ),
                        listOf(moveKeys, FixedKey("Pick") { "Enter" }, menuKey),
                    ),
                ),
        )

    val words =
        GameSpec(
            id = "words",
            title = "Words",
            tagline = "Find the five-letter word in six guesses.",
            palette = GamePalette(Color(0xFF141428), Color(0xFF0D0D1E), Color(0xFF2FD0B5)),
            howTo =
                listOf(
                    HowToPage(
                        "Guess the word",
                        listOf(
                            "Type a five-letter word and press Enter to guess.",
                            "Teal: the letter is in the right place. Orange: it's in the word, elsewhere. Slate: it isn't in the word.",
                            "You have six guesses.",
                            "The letter keys type, so Words keeps fixed keys.",
                        ),
                    ),
                ),
            controls =
                listOf(
                    GameControls(
                        "words",
                        emptyList(),
                        listOf(
                            FixedKey("Type") { "Letter keys" },
                            FixedKey("Guess") { "Enter" },
                            FixedKey("Delete") { "Del" },
                            menuKey,
                        ),
                    ),
                ),
        )

    private val roll = action("roll", "Roll", GameCommand.Select, K_SPACE)

    val snakesLadders =
        GameSpec(
            id = "snakesladders",
            title = "Snakes and Ladders",
            tagline = "Climb ladders, dodge snakes, land exactly on 100.",
            palette = GamePalette(Color(0xFF1A1430), Color(0xFF110D22), Color(0xFF8BE04E)),
            howTo = listOf(BoardRules.snakes),
            controls =
                listOf(
                    GameControls(
                        "snakesladders",
                        listOf(roll, newGame),
                        listOf(FixedKey("Roll") { "Enter" }, FixedKey("Players") { "2 3 4" }, menuKey),
                    ),
                ),
            option = GameOption("Players", listOf("2", "3", "4"), 1),
        )

    val ludo =
        GameSpec(
            id = "ludo",
            title = "Ludo",
            tagline = "Bring all four tokens home.",
            palette = GamePalette(Color(0xFF161A30), Color(0xFF0E1122), Color(0xFFFF5A6A)),
            howTo = listOf(BoardRules.ludo),
            controls = listOf(BoardRules.ludoControls),
        )

    val cards =
        GameSpec(
            id = "cards",
            title = "Card Games",
            tagline = "Practice chips only: they have no value.",
            palette = GamePalette(Color(0xFF2A0F1C), Color(0xFF1C0913), Color(0xFFFFC72C), muted = Color(0xFFE6C7D3)),
            howTo = CardRules.pages,
            controls = CardRules.controls,
        )

    val scaffold =
        GameSpec(
            id = "scaffold",
            title = "Scaffold",
            tagline = ScaffoldText.TAGLINE,
            palette = GamePalette(Color(0xFF121A38), Color(0xFF0B1026), Color(0xFFFFC72C)),
            howTo =
                listOf(
                    HowToPage(
                        "Climb the site. Rescue Juno.",
                        listOf(
                            "Goal: reach Juno at the top of each of the 4 levels.",
                            "Hazards: Clunk sends pipes and rivets rolling down. Jump them or wait on a ladder: you're safe while climbing.",
                            "Hearts: three per run. A hit costs one; the level restarts from the bottom.",
                            "Score: bolts are 100, jumping a hazard 100, and the bonus counts down while you climb.",
                        ),
                    ),
                ),
            controls = emptyList(),
        )

    /** The keys a [GameKind] uses when its screen doesn't name its own. */
    fun controlsFor(kind: GameKind): GameControls? =
        when (kind) {
            GameKind.Sudoku -> sudoku
            GameKind.Minesweeper -> minesweeper
            GameKind.Game2048 -> game2048
            GameKind.Bricks -> bricks
            GameKind.Snake -> snake
            GameKind.Blocks -> blocks
            GameKind.Reversi -> reversi
            GameKind.Solitaire -> solitaire
            GameKind.Mahjong -> mahjong
            GameKind.Words -> words
            GameKind.Indian -> null
        }?.controls?.firstOrNull()

    val all =
        listOf(
            snake,
            blocks,
            bricks,
            game2048,
            sudoku,
            minesweeper,
            reversi,
            solitaire,
            mahjong,
            words,
            snakesLadders,
            ludo,
            cards,
        )
}

/** Snakes and Ladders and Ludo, which also live in the SableOS Indian games app. */
internal object BoardRules {
    val snakes =
        HowToPage(
            "Snakes and Ladders",
            listOf(
                "Roll the die and move your token that many squares along the board, from 1 towards 100.",
                "Land at the foot of a ladder to climb it; land on a snake's head to slide down to its tail.",
                "A six gives you another roll. You must land exactly on 100 to win.",
                "Play with 2 to 4 players; you are the token with the star.",
            ),
        )

    val ludo =
        HowToPage(
            "Ludo",
            listOf(
                "You are red, with a star on your tokens. Roll a six to bring a token out of your yard.",
                "Move a token round the track by the number rolled, then up your home column to the centre.",
                "Land on another player's token to send it back to its yard.",
                "A six gives you another roll. Bring all four tokens home to win.",
            ),
        )

    val ludoControls =
        GameControls(
            "ludo",
            listOf(
                ControlAction("left", "Previous token", LEFT, byProfile(emptyList(), listOf(K_J))),
                ControlAction("right", "Next token", RIGHT, byProfile(emptyList(), listOf(K_L))),
                action("roll", "Roll and move", GameCommand.Select, K_SPACE),
                newGame,
            ),
            listOf(FixedKey("Pick token") { "1-4" }, FixedKey("Move it") { "Enter" }, menuKey),
        )
}

/** The card games in the Cards app, with their own keys, since a letter means something else in each. */
internal object CardRules {
    private val select = action("select", "Pick", GameCommand.Select, K_SPACE)
    private val cardMoves =
        listOf(
            ControlAction("left", "Left", LEFT, byProfile(emptyList(), listOf(K_J))),
            ControlAction("right", "Right", RIGHT, byProfile(emptyList(), listOf(K_L))),
        )
    private val play = FixedKey("Play, confirm") { "Enter" }

    val teenPatti =
        GameControls(
            "teenpatti",
            cardMoves +
                listOf(
                    letter("chaal", "Chaal", 'C'),
                    letter("raise", "Raise", 'R'),
                    letter("show", "Show", 'T'),
                    letter("pack", "Pack", 'P'),
                    ControlAction("look", "Look", GameCommand.Letter('L'), byProfile(listOf(K_L), listOf(K_S))),
                    action("next", "Next round", GameCommand.NewGame, K_N),
                ),
            listOf(play, menuKey),
        )

    val rummy =
        GameControls(
            "rummy",
            cardMoves +
                listOf(
                    select,
                    letter("stock", "Draw from stock", 'Z'),
                    letter("open", "Draw open card", 'X'),
                    letter("declare", "Declare", 'M'),
                    letter("group", "Group", 'G'),
                    letter("sort", "Sort", 'O'),
                    newGame,
                ),
            listOf(FixedKey("Throw a card") { "Enter" }, menuKey),
        )

    val trick =
        GameControls(
            "trick",
            cardMoves +
                listOf(
                    ControlAction("up", "Bid up", UP, byProfile(emptyList(), listOf(K_I))),
                    ControlAction("down", "Bid down", DOWN, byProfile(emptyList(), listOf(K_K))),
                    newGame,
                ),
            listOf(play, FixedKey("Bid") { if (it.letterMoves) "I K" else "Up, Down" }, menuKey),
        )

    val bluff =
        GameControls(
            "bluff",
            cardMoves +
                listOf(
                    select,
                    letter("call", "Call bluff", 'B'),
                    action("clear", "Clear picks", GameCommand.Erase, KeyEvent.KEYCODE_DEL),
                    newGame,
                ),
            listOf(FixedKey("Put cards down") { "Enter" }, menuKey),
        )

    val donkey =
        GameControls(
            "donkey",
            cardMoves + listOf(action("spoon", "Grab a spoon", GameCommand.Select, K_SPACE), newGame),
            listOf(FixedKey("Pass a card") { "Enter" }, menuKey),
        )

    val gulam = GameControls("gulam", cardMoves + newGame, listOf(FixedKey("Take a card") { "Enter" }, menuKey))

    val controls = listOf(teenPatti, rummy, trick, bluff, donkey, gulam)

    val pages =
        listOf(
            HowToPage(
                "Teen Patti",
                listOf(
                    "You and three phone players get three cards each and put the boot in the pot. Practice chips have no value.",
                    "Play blind, or look at your cards; a blind player pays half the stake.",
                    "Chaal matches the stake, Raise doubles it and Pack folds.",
                    "With two players left, either may ask for a show: the better hand wins the pot.",
                ),
            ),
            HowToPage(
                "Indian Rummy and Filmi Rummy",
                listOf(
                    "You hold 13 cards against two phone players. Draw from the stock or the open pile, then throw one card.",
                    "A random rank is the wild joker for the round, along with two printed jokers.",
                    "Win with two or more sequences, one of them pure (no joker), and every other card in sets or sequences.",
                    "Group arranges your hand into melds; declare when it is complete. Filmi Rummy plays the same with film-set cards.",
                ),
            ),
            HowToPage(
                "Callbreak, Judgement, 2-3-5 and Mendicot",
                listOf(
                    "Trick games: everyone plays a card, the highest of the suit led wins the trick, and a trump beats it.",
                    "Follow the suit led if you can.",
                    "Callbreak: bid the tricks you'll win; spades are trump. Judgement: bid exactly, and the trump changes every round.",
                    "2-3-5: three players with targets of 5, 3 and 2 tricks. Mendicot: partners, a hidden trump, collect the tens.",
                ),
            ),
            HowToPage(
                "Bluff",
                listOf(
                    "The whole deck is dealt. In turn, put one to four cards face down and say they are the rank whose turn it is.",
                    "Ranks go aces, twos and so on up to kings, then round again. You may lie.",
                    "The next player can call bluff: a liar picks up the whole pile; if the claim was true, the caller does.",
                    "The first player out of cards wins.",
                ),
            ),
            HowToPage(
                "Donkey",
                listOf(
                    "Four players, sixteen cards. Everyone passes a card to the left at the same time, trying to hold four of a kind.",
                    "The first with four grabs a spoon, and everyone else must grab too. There is one spoon fewer than players.",
                    "Left without a spoon, or grab too early, and you get a letter of D-O-N-K-E-Y. Spell it and you are the donkey.",
                ),
            ),
            HowToPage(
                "Gulam Choor",
                listOf(
                    "One Jack is taken out, so its partner has no pair.",
                    "Pairs are thrown away at the start. Then in turn each player takes a card unseen from the next player's hand and throws any new pair.",
                    "Whoever is left holding the odd Jack is the Gulam Choor.",
                ),
            ),
        )
}
