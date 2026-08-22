package com.katoaapps.openminilaunch

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

internal val ALL_APP_LETTERS: List<Char> = ('A'..'Z').toList()
internal const val ALL_APPS_CENTER_LETTER_INDEX = 12

internal data class LetterArcPosition(
    val x: Float,
    val y: Float,
)

internal fun appIndexForLetter(apps: List<LaunchableApp>, letter: Char): Int? =
    apps.indexOfFirst { app -> app.label.firstOrNull()?.uppercaseChar() == letter.uppercaseChar() }
        .takeIf { it >= 0 }

internal fun initialAllAppsIndex(apps: List<LaunchableApp>): Int =
    appIndexForLetter(apps, 'M') ?: 0

internal fun letterForApp(app: LaunchableApp?): Char? =
    app?.label?.firstOrNull()?.uppercaseChar()?.takeIf { it in 'A'..'Z' }

internal fun letterArcPosition(index: Int, width: Float, height: Float): LetterArcPosition {
    require(index in ALL_APP_LETTERS.indices)
    val angle = if (index <= ALL_APPS_CENTER_LETTER_INDEX) {
        PI - (PI / 2.0) * index / ALL_APPS_CENTER_LETTER_INDEX
    } else {
        (PI / 2.0) * (ALL_APP_LETTERS.lastIndex - index) /
            (ALL_APP_LETTERS.lastIndex - ALL_APPS_CENTER_LETTER_INDEX)
    }
    val centerX = width / 2f
    val baseY = height * .88f
    return LetterArcPosition(
        x = centerX + width * .46f * cos(angle).toFloat(),
        y = baseY - height * .70f * sin(angle).toFloat(),
    )
}

internal fun nearestLetterIndex(x: Float, y: Float, width: Float, height: Float): Int =
    ALL_APP_LETTERS.indices.minBy { index ->
        val position = letterArcPosition(index, width, height)
        val deltaX = position.x - x
        val deltaY = position.y - y
        deltaX * deltaX + deltaY * deltaY
    }
