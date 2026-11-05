package com.katoaapps.openminilaunch.features.apps

import java.text.Normalizer
import java.util.Locale

private val acronymBoundary = Regex("(?<=\\p{Lu})(?=\\p{Lu}\\p{Ll})")
private val camelCaseBoundary = Regex("(?<=[\\p{Ll}\\p{Nd}])(?=\\p{Lu})")

internal data class NormalizedAppSearchText(
    val value: String,
    val tokens: List<String>,
) {
    val compact: String = tokens.joinToString(separator = "")
    val initials: String = tokens.mapNotNull(String::firstOrNull).joinToString(separator = "")
}

internal fun normalizeAppSearchText(value: String): NormalizedAppSearchText {
    val separatedWords = value
        .replace(acronymBoundary, " ")
        .replace(camelCaseBoundary, " ")
    val decomposed = Normalizer.normalize(
        separatedWords.lowercase(Locale.ROOT),
        Normalizer.Form.NFKD,
    )
    val normalized = buildString(decomposed.length) {
        decomposed.codePoints().forEach { codePoint ->
            when {
                isCombiningMark(codePoint) -> Unit
                Character.isLetterOrDigit(codePoint) -> appendCodePoint(
                    if (codePoint == DOTLESS_I) 'i'.code else codePoint,
                )
                else -> append(' ')
            }
        }
    }.trim().replace(Regex("\\s+"), " ")
    return NormalizedAppSearchText(
        value = normalized,
        tokens = normalized.split(' ').filter(String::isNotEmpty),
    )
}

private fun isCombiningMark(codePoint: Int): Boolean = when (Character.getType(codePoint)) {
    Character.NON_SPACING_MARK.toInt(),
    Character.COMBINING_SPACING_MARK.toInt(),
    Character.ENCLOSING_MARK.toInt(),
    -> true
    else -> false
}

private const val DOTLESS_I = 0x0131
