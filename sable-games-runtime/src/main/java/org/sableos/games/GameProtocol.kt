package org.sableos.games

internal fun requireOk(protocol: String): String {
    check(protocol.startsWith("OK:")) { protocol }
    return protocol.removePrefix("OK:")
}

internal fun humanize(protocol: String): String =
    protocol
        .removePrefix("ERR:")
        .replace('_', ' ')
