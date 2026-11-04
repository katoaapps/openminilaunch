package com.katoaapps.openminilaunch.model

enum class AppFolderIndicatorStyle {
    GRID,
    STACKED,
}

enum class AppFolderAnimationStyle {
    RIPPLE,
    GROW_AND_SPIN,
    APPEAR,
    RISE,
}

internal fun appFolderIndicatorStyle(storedStyle: String?): AppFolderIndicatorStyle =
    storedStyle.toEnumOrDefault(AppFolderIndicatorStyle.STACKED)

internal fun appFolderAnimationStyle(storedStyle: String?): AppFolderAnimationStyle =
    storedStyle.toEnumOrDefault(AppFolderAnimationStyle.RIPPLE)

private inline fun <reified T : Enum<T>> String?.toEnumOrDefault(default: T): T =
    this?.let { runCatching { enumValueOf<T>(it) }.getOrNull() } ?: default
