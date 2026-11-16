package com.katoaapps.openminilaunch.model

/** Selects the structural Home experience without changing the user's launcher content. */
enum class HomeLayoutStyle {
    MINK,
    CLASSIC,
}

internal fun parseHomeLayoutStyle(storedStyle: String?): HomeLayoutStyle =
    storedStyle
        ?.let { runCatching { enumValueOf<HomeLayoutStyle>(it) }.getOrNull() }
        ?: HomeLayoutStyle.MINK
