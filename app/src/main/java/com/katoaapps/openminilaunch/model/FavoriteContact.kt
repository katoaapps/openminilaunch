package com.katoaapps.openminilaunch.model

/** A contact starred in Android Contacts. Nothing in this model is persisted by Mink. */
internal data class FavoriteContact(
    val lookupKey: String,
    val contactUri: String,
    val displayName: String,
    val photoUri: String?,
    val organization: String?,
    val phones: List<FavoritePhone>,
)

internal data class FavoritePhone(
    val rawNumber: String,
    val normalizedNumber: String,
    val label: String,
    val isPrimary: Boolean,
    val isSuperPrimary: Boolean,
)

internal sealed interface FavoritePhoneSelection {
    data object None : FavoritePhoneSelection
    data class Resolved(val phone: FavoritePhone) : FavoritePhoneSelection
    data class Choose(val phones: List<FavoritePhone>) : FavoritePhoneSelection
}

/** Deduplicates equivalent provider rows while retaining their best-ranked representation. */
internal fun FavoriteContact.uniquePhones(): List<FavoritePhone> = phones
        .groupBy(FavoritePhone::normalizedNumber)
        .values
        .mapNotNull { matchingNumber ->
            matchingNumber.maxWithOrNull(
                compareBy<FavoritePhone>(FavoritePhone::isSuperPrimary)
                    .thenBy(FavoritePhone::isPrimary),
            )
        }

internal fun FavoriteContact.preferredPhone(
    personKey: String,
    fingerprint: String?,
): FavoritePhone? = fingerprint?.let { preferred ->
    uniquePhones().firstOrNull { phone ->
        favoritePhoneFingerprint(personKey, phone.normalizedNumber) == preferred
    }
}

/** Uses an explicit saved preference or requires a choice between distinct destinations. */
internal fun FavoriteContact.phoneSelection(
    preferredPhone: FavoritePhone? = null,
): FavoritePhoneSelection {
    val available = uniquePhones()
    preferredPhone?.let { selected ->
        available.firstOrNull { it.normalizedNumber == selected.normalizedNumber }?.let {
            return FavoritePhoneSelection.Resolved(it)
        }
    }
    return when (available.size) {
        0 -> FavoritePhoneSelection.None
        1 -> FavoritePhoneSelection.Resolved(available.single())
        else -> FavoritePhoneSelection.Choose(available)
    }
}
