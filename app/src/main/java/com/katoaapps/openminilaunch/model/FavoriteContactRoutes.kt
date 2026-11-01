package com.katoaapps.openminilaunch.model

import java.security.MessageDigest

/**
 * An optional app override for one person's action.
 *
 * Inherit keeps the launcher-wide setting authoritative. SystemDefault deliberately bypasses a
 * launcher-wide app choice, while Package selects one installed handler for this person only.
 */
internal sealed interface ContactAppRoute {
    data object Inherit : ContactAppRoute
    data object SystemDefault : ContactAppRoute
    data class Package(val packageName: String) : ContactAppRoute
}

/** Resolves one person's route without conflating inherit with an explicit system choice. */
internal fun ContactAppRoute.resolvePackage(inheritedPackage: String?): String? = when (this) {
    ContactAppRoute.Inherit -> inheritedPackage
    ContactAppRoute.SystemDefault -> null
    is ContactAppRoute.Package -> packageName
}

internal data class FavoriteContactActionRoutes(
    val message: ContactAppRoute = ContactAppRoute.Inherit,
    val call: ContactAppRoute = ContactAppRoute.Inherit,
    val preferredPhoneFingerprint: String? = null,
)

/** One-way correlation token; no phone number is copied into Mink's route store. */
internal fun favoritePhoneFingerprint(
    contactId: FavoriteContactId.Android,
    normalizedNumber: String,
): String {
    val identity = "${contactId.storageKey}\u0000$normalizedNumber".toByteArray(Charsets.UTF_8)
    return try {
        MessageDigest.getInstance("SHA-256").digest(identity)
            .joinToString(separator = "") { byte ->
                (byte.toInt() and 0xff).toString(radix = 16).padStart(length = 2, padChar = '0')
            }
    } finally {
        identity.fill(0)
    }
}
