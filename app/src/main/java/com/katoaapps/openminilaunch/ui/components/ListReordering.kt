package com.katoaapps.openminilaunch.ui.components

/** Moves one item in place and reports whether the requested move was valid. */
internal fun <T> MutableList<T>.moveItem(fromIndex: Int, toIndex: Int): Boolean {
    if (fromIndex !in indices || toIndex !in indices || fromIndex == toIndex) return false
    add(toIndex, removeAt(fromIndex))
    return true
}
