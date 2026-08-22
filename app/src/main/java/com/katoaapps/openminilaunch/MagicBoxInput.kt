package com.katoaapps.openminilaunch

internal const val MAGIC_NOTE_PREFIX = '/'
internal val MAGIC_COMMAND_PREFIXES = setOf('@', '#', '-', MAGIC_NOTE_PREFIX, '+', '?')

internal data class MagicBoxInput(
    val prefix: Char?,
    val searchTerm: String,
    val plainQuery: String,
)

internal fun parseMagicBoxInput(text: String, lockedPrefix: Char? = null): MagicBoxInput {
    val prefix = lockedPrefix ?: text.firstOrNull()
    val searchTerm = if (lockedPrefix == null) {
        text.drop(1).substringBefore(' ').trim()
    } else {
        ""
    }
    val plainQuery = text.trim().takeIf { prefix !in MAGIC_COMMAND_PREFIXES }.orEmpty()
    return MagicBoxInput(prefix, searchTerm, plainQuery)
}
