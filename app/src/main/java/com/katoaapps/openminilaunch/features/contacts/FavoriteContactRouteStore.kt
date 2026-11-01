package com.katoaapps.openminilaunch.features.contacts

import android.content.Context
import com.katoaapps.openminilaunch.model.ContactAppRoute
import com.katoaapps.openminilaunch.model.FavoriteContactActionRoutes
import com.katoaapps.openminilaunch.model.FavoriteContactId
import java.io.File

/**
 * Stores only encrypted, local action preferences for a person.
 *
 * Contact lookup keys and selected packages are sensitive relationship metadata. They are kept in
 * one authenticated ciphertext under no-backup storage; names, numbers, handles, photos, and
 * contact records are never copied into this store. Preferred phone routing uses only a one-way
 * fingerprint scoped to the person's opaque lookup identity.
 */
internal class FavoriteContactRouteStore private constructor(
    private val persistence: EncryptedFavoriteContactRoutePersistence,
) {
    private var cachedRoutes: Map<String, FavoriteContactActionRoutes>? = null

    constructor(context: Context) : this(createPersistence(context.applicationContext))

    @Synchronized
    fun routes(contactId: FavoriteContactId): FavoriteContactActionRoutes =
        allRoutes()[contactId.storageKey] ?: FavoriteContactActionRoutes()

    @Synchronized
    fun updateMessage(contactId: FavoriteContactId, route: ContactAppRoute) {
        update(contactId) { it.copy(message = route.normalized()) }
    }

    @Synchronized
    fun updateCall(contactId: FavoriteContactId, route: ContactAppRoute) {
        update(contactId) { it.copy(call = route.normalized()) }
    }

    @Synchronized
    fun updatePreferredPhone(contactId: FavoriteContactId.Android, fingerprint: String?) {
        update(contactId) {
            it.copy(preferredPhoneFingerprint = fingerprint?.takeIf(String::isNotBlank))
        }
    }

    private fun update(
        contactId: FavoriteContactId,
        transform: (FavoriteContactActionRoutes) -> FavoriteContactActionRoutes,
    ) {
        val storageKey = contactId.storageKey
        if (storageKey.isBlank()) return
        val current = allRoutes()
        val updatedRoutes = transform(current[storageKey] ?: FavoriteContactActionRoutes())
        val updated = current.toMutableMap().apply {
            if (updatedRoutes.isInherited()) remove(storageKey) else put(storageKey, updatedRoutes)
        }
        if (updated == current) return

        // Keep the last successfully persisted snapshot authoritative. A storage or Keystore
        // failure must not make the UI claim a route was saved when it was not.
        if (persistence.save(updated)) cachedRoutes = updated
    }

    private fun allRoutes(): Map<String, FavoriteContactActionRoutes> = cachedRoutes
        ?: persistence.load().also { cachedRoutes = it }

    private companion object {
        // These persisted identifiers intentionally retain their pre-normalization names so an
        // existing install keeps its encrypted per-contact routes across the model rename.
        const val LEGACY_PREFERENCES_NAME = "favorite_person_action_routes_v1"
        const val ENCRYPTED_FILE_NAME = "favorite_person_action_routes_v1.enc"
        const val KEY_ALIAS = "mink.favorite-person-action-routes.aes-gcm.v1"

        fun createPersistence(context: Context): EncryptedFavoriteContactRoutePersistence {
            // The first development pass used plaintext SharedPreferences. Never leave that file
            // behind after the secure store exists, even on devices that installed a test build.
            context.deleteSharedPreferences(LEGACY_PREFERENCES_NAME)
            return EncryptedFavoriteContactRoutePersistence(
                storage = AtomicContactDataBlobStorage(
                    File(context.noBackupFilesDir, ENCRYPTED_FILE_NAME),
                ),
                cipher = AesGcmContactDataCipher(
                    keyProvider = AndroidKeystoreContactDataKeyProvider(KEY_ALIAS),
                    envelopeMagic = 0x4D_52_54_45,
                    authenticatedContext =
                        "openmink:favorite-person-action-routes:v1".toByteArray(Charsets.UTF_8),
                ),
            )
        }
    }
}

private fun ContactAppRoute.normalized(): ContactAppRoute = when (this) {
    is ContactAppRoute.Package -> if (packageName.isBlank()) ContactAppRoute.Inherit else this
    else -> this
}

private fun FavoriteContactActionRoutes.isInherited(): Boolean =
    message == ContactAppRoute.Inherit &&
        call == ContactAppRoute.Inherit &&
        preferredPhoneFingerprint == null
