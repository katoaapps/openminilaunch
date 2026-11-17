package com.katoaapps.minkboard.ui

/** Moves one visible item to the exact destination used by immediate drag interactions. */
internal fun <T> MutableList<T>.moveItem(fromIndex: Int, toIndex: Int): Boolean {
    if (fromIndex !in indices || toIndex !in indices || fromIndex == toIndex) return false
    add(toIndex, removeAt(fromIndex))
    return true
}
