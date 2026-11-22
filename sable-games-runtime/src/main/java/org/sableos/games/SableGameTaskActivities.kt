package org.sableos.games

/**
 * Stable components that give every top-level game its own Android task identity.
 *
 * A route extra alone is not sufficient because some OEM task managers collapse documents hosted
 * by the same activity. These thin hosts still share the complete Sable runtime and game UI.
 */
class SudokuActivity : SableGameActivity() {
    override val gameRoute = "sudoku"
}

class MinesweeperActivity : SableGameActivity() {
    override val gameRoute = "minesweeper"
}

class Game2048Activity : SableGameActivity() {
    override val gameRoute = "2048"
}

class BricksActivity : SableGameActivity() {
    override val gameRoute = "bricks"
}

class SnakeActivity : SableGameActivity() {
    override val gameRoute = "snake"
}

class BlocksActivity : SableGameActivity() {
    override val gameRoute = "blocks"
}

class ReversiActivity : SableGameActivity() {
    override val gameRoute = "reversi"
}

class SolitaireActivity : SableGameActivity() {
    override val gameRoute = "solitaire"
}

class MahjongActivity : SableGameActivity() {
    override val gameRoute = "mahjong"
}

class WordsActivity : SableGameActivity() {
    override val gameRoute = "words"
}

class CardGamesActivity : SableGameActivity() {
    override val gameRoute = "cards"
}

class SnakesAndLaddersActivity : SableGameActivity() {
    override val gameRoute = "snakesladders"
}

class LudoActivity : SableGameActivity() {
    override val gameRoute = "ludo"
}

class ScaffoldActivity : SableGameActivity() {
    override val gameRoute = "scaffold"
}
