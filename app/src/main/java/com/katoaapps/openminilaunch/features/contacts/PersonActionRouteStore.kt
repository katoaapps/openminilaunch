package com.katoaapps.openminilaunch.features.contacts

import android.content.Context
import com.katoaapps.openminilaunch.model.PersonActionRoutes
import com.katoaapps.openminilaunch.model.PersonAppRoute
import java.io.File

/**
 * Stores only encrypted, local action preferences for a person.
 *
 * Contact lookup keys and selected packages are sensitive relationship metadata. They are kept in
 * one authenticated ciphertext under no-backup storage; names, numbers, handles, photos, and
 * contact records are never copied into this store. Preferred phone routing uses only a one-way
 * fingerprint scoped to the person's opaque lookup identity.
 */
internal class PersonActionRouteStore private constructor(
    private val persistence: EncryptedPersonActionRoutePersistence,
) {
    private var cachedRoutes: Map<String, PersonActionRoutes>? = null

    constructor(context: Context) : this(createPersistence(context.applicationContext))

    @Synchronized
    fun routes(personKey: String): PersonActionRoutes = allRoutes()[personKey]
        ?: PersonActionRoutes()

    @Synchronized
    fun updateMessage(personKey: String, route: PersonAppRoute) {
        update(personKey) { it.copy(message = route.normalized()) }
    }

    @Synchronized
    fun updateCall(personKey: String, route: PersonAppRoute) {
        update(personKey) { it.copy(call = route.normalized()) }
    }

    @Synchronized
    fun updatePreferredPhone(personKey: String, fingerprint: String?) {
        update(personKey) {
            it.copy(preferredPhoneFingerprint = fingerprint?.takeIf(String::isNotBlank))
        }
    }

    private fun update(
        personKey: String,
        transform: (PersonActionRoutes) -> PersonActionRoutes,
    ) {
        if (personKey.isBlank()) return
        val current = allRoutes()
        val updatedRoutes = transform(current[personKey] ?: PersonActionRoutes())
        val updated = current.toMutableMap().apply {
            if (updatedRoutes.isInherited()) remove(personKey) else put(personKey, updatedRoutes)
        }
        if (updated == current) return

        // Keep the last successfully persisted snapshot authoritative. A storage or Keystore
        // failure must not make the UI claim a route was saved when it was not.
        if (persistence.save(updated)) cachedRoutes = updated
    }

    private fun allRoutes(): Map<String, PersonActionRoutes> = cachedRoutes
        ?: persistence.load().also { cachedRoutes = it }

    private companion object {
        const val LEGACY_PREFERENCES_NAME = "favorite_person_action_routes_v1"
        const val ENCRYPTED_FILE_NAME = "favorite_person_action_routes_v1.enc"
        const val KEY_ALIAS = "mink.favorite-person-action-routes.aes-gcm.v1"

        fun createPersistence(context: Context): EncryptedPersonActionRoutePersistence {
            // The first development pass used plaintext SharedPreferences. Never leave that file
            // behind after the secure store exists, even on devices that installed a test build.
            context.deleteSharedPreferences(LEGACY_PREFERENCES_NAME)
            return EncryptedPersonActionRoutePersistence(
                storage = AtomicPersonActionRouteStorage(
                    File(context.noBackupFilesDir, ENCRYPTED_FILE_NAME),
                ),
                cipher = AesGcmPersonActionRouteCipher(
                    AndroidKeystoreSecretKeyProvider(KEY_ALIAS),
                ),
            )
        }
    }
}

private fun PersonAppRoute.normalized(): PersonAppRoute = when (this) {
    is PersonAppRoute.Package -> if (packageName.isBlank()) PersonAppRoute.Inherit else this
    else -> this
}

private fun PersonActionRoutes.isInherited(): Boolean =
    message == PersonAppRoute.Inherit &&
        call == PersonAppRoute.Inherit &&
        preferredPhoneFingerprint == null
