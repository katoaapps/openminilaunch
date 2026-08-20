package com.katoaapps.openminilaunch

import java.net.URI

private val bareDomainPattern = Regex(
    pattern = "^(?:[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,62}[a-zA-Z0-9])?\\.)+[a-zA-Z]{2,63}(?::\\d{1,5})?(?:[/?#].*)?$",
)

internal fun normalizedWebUrl(query: String): String? {
    val candidate = query.trim()
    if (candidate.isEmpty() || candidate.any(Char::isWhitespace)) return null

    val normalized = when {
        candidate.startsWith("https://", ignoreCase = true) ||
            candidate.startsWith("http://", ignoreCase = true) -> candidate
        candidate.startsWith("www.", ignoreCase = true) -> "https://$candidate"
        bareDomainPattern.matches(candidate) -> "https://$candidate"
        else -> return null
    }

    return runCatching {
        val uri = URI(normalized)
        normalized.takeIf {
            uri.scheme.equals("http", ignoreCase = true) || uri.scheme.equals("https", ignoreCase = true)
        }?.takeIf { !uri.host.isNullOrBlank() }
    }.getOrNull()
}
