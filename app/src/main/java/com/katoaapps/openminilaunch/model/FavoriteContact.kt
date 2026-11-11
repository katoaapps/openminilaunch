package com.katoaapps.openminilaunch.model

/** A person shown in Favorites, regardless of whether Android Contacts owns the record. */
internal sealed interface FavoriteContact {
    val id: FavoriteContactId
    val displayName: String
}

/** Stable persistence identity. Display data must never be used as a key. */
internal sealed interface FavoriteContactId {
    val storageKey: String

    data class Android(val lookupKey: String) : FavoriteContactId {
        override val storageKey: String = "android-contact:$lookupKey"
    }

    data class Integrated(val localId: String) : FavoriteContactId {
        override val storageKey: String = "integrated-contact:$localId"
    }
}

/** A contact starred in Android Contacts. Nothing in this model is persisted by Mink. */
internal data class FavoriteAndroidContact(
    override val id: FavoriteContactId.Android,
    val contactUri: String,
    override val displayName: String,
    val photoUri: String?,
    val organization: String?,
    val phones: List<AndroidContactPhone>,
) : FavoriteContact

/** A local favorite connected to an app-owned conversation or address. */
internal data class FavoriteIntegratedContact(
    override val id: FavoriteContactId.Integrated,
    override val displayName: String,
    val endpoints: List<IntegratedContactEndpoint>,
    val callEndpoint: IntegratedContactCallEndpoint? = null,
    val nameSource: FavoriteIntegratedContactNameSource = FavoriteIntegratedContactNameSource.USER,
) : FavoriteContact

/** Whether the provider may refresh the favorite's visible name or the user owns the alias. */
internal enum class FavoriteIntegratedContactNameSource {
    PROVIDER,
    USER,
}

/** The app-owned destination behind an integrated favorite. */
internal sealed interface IntegratedContactEndpoint {
    val packageName: String

    /** A conversation published by Android's launcher-shortcut APIs. */
    data class ConversationShortcut(
        override val packageName: String,
        val shortcutId: String,
        val userSerial: Long,
        val label: String?,
    ) : IntegratedContactEndpoint

    /** A provider contract that accepts an explicit phone number or username. */
    data class ProviderAddress(
        val providerId: String,
        override val packageName: String,
        val addressType: ProviderAddressType,
        val address: String,
    ) : IntegratedContactEndpoint
}

/** A callable identity owned by Mink rather than Android Contacts. */
internal sealed interface IntegratedContactCallEndpoint {
    /**
     * A standard Android phone-number route. The optional package is an installed app that
     * explicitly advertises support for `tel:` intents; it is not a provider-native integration.
     */
    data class PhoneNumber(
        val number: String,
        val packageName: String?,
    ) : IntegratedContactCallEndpoint
}

/** Explicit address forms verified by a provider integration. */
internal enum class ProviderAddressType { PHONE, USERNAME }

internal data class AndroidContactPhone(
    val rawNumber: String,
    val normalizedNumber: String,
    val label: String,
    val isPrimary: Boolean,
    val isSuperPrimary: Boolean,
)

internal sealed interface AndroidPhoneSelection {
    data object None : AndroidPhoneSelection
    data class Resolved(val phone: AndroidContactPhone) : AndroidPhoneSelection
    data class Choose(val phones: List<AndroidContactPhone>) : AndroidPhoneSelection
}

/** Deduplicates equivalent provider rows while retaining their best-ranked representation. */
internal fun FavoriteAndroidContact.uniquePhones(): List<AndroidContactPhone> = phones
        .groupBy(AndroidContactPhone::normalizedNumber)
        .values
        .mapNotNull { matchingNumber ->
            matchingNumber.maxWithOrNull(
                compareBy<AndroidContactPhone>(AndroidContactPhone::isSuperPrimary)
                    .thenBy(AndroidContactPhone::isPrimary),
            )
        }

internal fun FavoriteAndroidContact.preferredPhone(
    contactId: FavoriteContactId.Android,
    fingerprint: String?,
): AndroidContactPhone? = fingerprint?.let { preferred ->
    uniquePhones().firstOrNull { phone ->
        favoritePhoneFingerprint(contactId, phone.normalizedNumber) == preferred
    }
}

/** Uses an explicit saved preference or requires a choice between distinct destinations. */
internal fun FavoriteAndroidContact.phoneSelection(
    preferredPhone: AndroidContactPhone? = null,
): AndroidPhoneSelection {
    val available = uniquePhones()
    preferredPhone?.let { selected ->
        available.firstOrNull { it.normalizedNumber == selected.normalizedNumber }?.let {
            return AndroidPhoneSelection.Resolved(it)
        }
    }
    return when (available.size) {
        0 -> AndroidPhoneSelection.None
        1 -> AndroidPhoneSelection.Resolved(available.single())
        else -> AndroidPhoneSelection.Choose(available)
    }
}
