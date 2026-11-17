package com.katoaapps.minkboard.core

import com.katoaapps.minkboard.model.BoardCard
import com.katoaapps.minkboard.model.BoardSnapshot
import java.time.LocalDate

enum class CardDueState {
    None,
    Future,
    Today,
    Overdue,
}

fun BoardCard.dueState(today: LocalDate, isCompleted: Boolean): CardDueState {
    val date = dueDate ?: return CardDueState.None
    if (isCompleted) return CardDueState.None
    return when {
        date.isBefore(today) -> CardDueState.Overdue
        date == today -> CardDueState.Today
        else -> CardDueState.Future
    }
}

/** Current-board calendar results. Done cards are omitted, matching Spencer's shipped calendar. */
fun BoardSnapshot.cardsDueOn(date: LocalDate): List<BoardCard> {
    val doneStatusIds = statuses.filter { it.isDoneStatus }.mapTo(mutableSetOf()) { it.id }
    return cards.asSequence()
        .filter { it.statusId !in doneStatusIds && it.dueDate == date }
        .sortedWith(compareBy<BoardCard> { card ->
            statuses.firstOrNull { it.id == card.statusId }?.position ?: Int.MAX_VALUE
        }.thenBy(BoardCard::position).thenBy { it.id.value })
        .toList()
}

fun BoardSnapshot.dueCardCount(date: LocalDate): Int = cardsDueOn(date).size
