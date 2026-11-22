package org.sableos.games

import kotlin.random.Random

/** Shared pieces of the Sable Indian Games: decks, hand order, and the names the phone's players use. */
internal object IndianCards {
    /** A fresh 52-card deck, shuffled. [decks] decks make a bigger shoe (rummy uses two). */
    fun deck(
        random: Random,
        decks: Int = 1,
        minRank: Int = 1,
    ): List<PlayCard> =
        List(decks) {
            (0 until SUITS).flatMap { suit -> (minRank..KING).map { PlayCard(it, suit) } }
        }.flatten().shuffled(random)

    /** Card value for trick-taking: the ace is high. */
    fun high(card: PlayCard): Int = if (card.rank == 1) 14 else card.rank

    /** Hand order: by suit (spades, hearts, clubs, diamonds alternate colours), then high to low. */
    fun sorted(hand: List<PlayCard>): List<PlayCard> =
        hand.sortedWith(compareBy<PlayCard> { SUIT_ORDER.indexOf(it.suit) }.thenByDescending { high(it) })

    /** Face value in rummy points: ace and court cards ten, others their number. */
    fun points(card: PlayCard): Int = if (card.rank == 1 || card.rank >= 10) 10 else card.rank

    fun rankName(rank: Int): String = RANK_NAMES[rank - 1]

    fun suitName(suit: Int): String = SUIT_NAMES[suit]

    fun suitSymbol(suit: Int): String = SUIT_SYMBOLS[suit]

    const val SUITS = 4
    const val KING = 13
    const val SPADES = 0
    const val HEARTS = 1
    const val DIAMONDS = 2
    const val CLUBS = 3
    private val SUIT_ORDER = listOf(SPADES, HEARTS, CLUBS, DIAMONDS)
    private val RANK_NAMES =
        listOf("A", "2", "3", "4", "5", "6", "7", "8", "9", "10", "J", "Q", "K")
    private val SUIT_NAMES = listOf("Spades", "Hearts", "Diamonds", "Clubs")
    private val SUIT_SYMBOLS = listOf("♠", "♥", "♦", "♣")
}

/** Names for the phone's players. Drawn from all over the world, mixed up each game. */
internal object PlayerNames {
    private val POOL =
        listOf(
            "Meera",
            "Arjun",
            "Dev",
            "Priya",
            "Rohan",
            "Anaya",
            "Kabir",
            "Isha",
            "Maria",
            "Carlos",
            "Sofia",
            "Mateo",
            "Lucia",
            "Diego",
            "Liam",
            "Emma",
            "Noah",
            "Olivia",
            "Jack",
            "Chloe",
            "Chen",
            "Mei",
            "Haru",
            "Yuki",
            "Jin",
            "Min-ji",
            "Amara",
            "Kofi",
            "Zola",
            "Tunde",
            "Nia",
            "Sipho",
            "Omar",
            "Layla",
            "Yusuf",
            "Fatima",
            "Karim",
            "Noor",
            "Ivan",
            "Katya",
            "Lars",
            "Freya",
            "Marco",
            "Giulia",
            "Elena",
            "Pavel",
            "Anika",
            "Mia",
            "Leo",
            "Zara",
        )

    /** [count] different names, none of them in [exclude]. */
    fun pick(
        random: Random,
        count: Int,
        exclude: Set<String> = emptySet(),
    ): List<String> = POOL.filter { it !in exclude }.shuffled(random).take(count)

    fun size(): Int = POOL.size
}
