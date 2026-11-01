package com.katoaapps.openminilaunch.model

import java.security.MessageDigest

/**
 * An optional app override for one person's action.
 *
 * Inherit keeps the launcher-wide setting authoritative. SystemDefault deliberately bypasses a
 * launcher-wide app choice, while Package selects one installed handler for this person only.
 */
internal sealed interface PersonAppRoute {
    data object Inherit : PersonAppRoute
    data object SystemDefault : PersonAppRoute
    data class Package(val packageName: String) : PersonAppRoute
}

internal data class PersonActionRoutes(
    val message: PersonAppRoute = PersonAppRoute.Inherit,
    val call: PersonAppRoute = PersonAppRoute.Inherit,
    val preferredPhoneFingerprint: String? = null,
)

/** Android contacts and future Mink-only people share this non-display persistence identity. */
internal fun androidContactPersonKey(lookupKey: String): String = "android-contact:$lookupKey"

/** One-way correlation token; no phone number is copied into Mink's route store. */
internal fun favoritePhoneFingerprint(personKey: String, normalizedNumber: String): String {
    val identity = "$personKey\u0000$normalizedNumber".toByteArray(Charsets.UTF_8)
    return try {
        MessageDigest.getInstance("SHA-256").digest(identity)
            .joinToString(separator = "") { byte ->
                (byte.toInt() and 0xff).toString(radix = 16).padStart(length = 2, padChar = '0')
            }
    } finally {
        identity.fill(0)
    }
}
