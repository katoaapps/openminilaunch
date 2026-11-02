package com.katoaapps.openminilaunch.features.contacts

import android.content.Context
import com.katoaapps.openminilaunch.model.FavoriteContactId
import com.katoaapps.openminilaunch.model.FavoriteIntegratedContact
import com.katoaapps.openminilaunch.model.IntegratedContactEndpoint
import com.katoaapps.openminilaunch.model.LauncherShortcutTarget
import com.katoaapps.openminilaunch.model.ProviderAddressType
import java.io.File
import java.util.UUID

// Kotlin's character count can understate UTF-8 size. This remains below the 512-byte codec limit
// so even a name made entirely of four-byte characters can be saved.
internal const val FAVORITE_INTEGRATED_CONTACT_NAME_MAX_CHARS = 120
internal const val FAVORITE_PROVIDER_ADDRESS_MAX_CHARS = 256

internal enum class FavoriteIntegratedContactAddResult {
    ADDED,
    DUPLICATE,
    FAILED,
}

/**
 * Encrypted, local-only ownership for favorites connected to provider-owned conversations.
 *
 * Unlike Android favorites, these records belong to Mink. They are deliberately kept out of
 * portable backups because shortcut ids, provider addresses, and Android user serials are either
 * sensitive or installation-specific.
 */
internal class FavoriteIntegratedContactStore(
    private val persistence: EncryptedFavoriteIntegratedContactPersistence,
) {
    private var cachedContacts: List<FavoriteIntegratedContact>? = null

    constructor(context: Context) : this(createPersistence(context.applicationContext))

    @Synchronized
    fun contacts(): List<FavoriteIntegratedContact> = allContacts()

    @Synchronized
    fun add(
        shortcut: LauncherShortcutTarget,
        displayName: String = shortcut.label,
    ): FavoriteIntegratedContactAddResult {
        val endpoint = shortcut.toIntegratedEndpoint()
        val cleanName = displayName.cleanFavoriteName()
            ?: shortcut.label.cleanFavoriteName()
            ?: return FavoriteIntegratedContactAddResult.FAILED
        return add(endpoint, cleanName)
    }

    /** Adds a favorite through a provider contract that accepts an explicit recipient address. */
    @Synchronized
    fun addProviderAddress(
        providerId: String,
        packageName: String,
        addressType: ProviderAddressType,
        address: String,
        displayName: String,
    ): FavoriteIntegratedContactAddResult {
        val endpoint = providerAddressEndpoint(
            providerId = providerId,
            packageName = packageName,
            addressType = addressType,
            address = address,
        ) ?: return FavoriteIntegratedContactAddResult.FAILED
        val cleanName = displayName.cleanFavoriteName()
            ?: return FavoriteIntegratedContactAddResult.FAILED
        return add(endpoint, cleanName)
    }

    @Synchronized
    fun remove(id: FavoriteContactId.Integrated): Boolean {
        val current = allContacts()
        val updated = current.filterNot { it.id == id }
        if (updated.size == current.size) return false
        return save(updated)
    }

    /** Points an existing favorite at another conversation without changing its local identity. */
    @Synchronized
    fun replaceEndpoint(
        id: FavoriteContactId.Integrated,
        shortcut: LauncherShortcutTarget,
    ): Boolean = replaceEndpoint(id, shortcut.toIntegratedEndpoint())

    /** Reroutes an existing favorite through another verified provider-address contract. */
    @Synchronized
    fun replaceProviderAddress(
        id: FavoriteContactId.Integrated,
        providerId: String,
        packageName: String,
        addressType: ProviderAddressType,
        address: String,
    ): Boolean {
        val replacement = providerAddressEndpoint(
            providerId = providerId,
            packageName = packageName,
            addressType = addressType,
            address = address,
        ) ?: return false
        return replaceEndpoint(id, replacement)
    }

    private fun add(
        endpoint: IntegratedContactEndpoint,
        displayName: String,
    ): FavoriteIntegratedContactAddResult {
        val current = allContacts()
        if (current.hasEndpoint(endpoint, excluding = null)) {
            return FavoriteIntegratedContactAddResult.DUPLICATE
        }
        val updated = current + FavoriteIntegratedContact(
            id = FavoriteContactId.Integrated(UUID.randomUUID().toString()),
            displayName = displayName,
            endpoints = listOf(endpoint),
        )
        return if (save(updated)) {
            FavoriteIntegratedContactAddResult.ADDED
        } else {
            FavoriteIntegratedContactAddResult.FAILED
        }
    }

    private fun replaceEndpoint(
        id: FavoriteContactId.Integrated,
        replacement: IntegratedContactEndpoint,
    ): Boolean {
        val current = allContacts()
        val contactIndex = current.indexOfFirst { it.id == id }
        if (contactIndex == -1 || current.hasEndpoint(replacement, excluding = id)) return false

        val existing = current[contactIndex]
        if (existing.endpoints.singleOrNull()?.identity == replacement.identity) return true

        val updated = current.toMutableList().apply {
            this[contactIndex] = existing.copy(endpoints = listOf(replacement))
        }
        return save(updated)
    }

    private fun providerAddressEndpoint(
        providerId: String,
        packageName: String,
        addressType: ProviderAddressType,
        address: String,
    ): IntegratedContactEndpoint.ProviderAddress? {
        val cleanProviderId = providerId.trim().takeIf(String::isNotBlank) ?: return null
        val cleanPackageName = packageName.trim().takeIf(String::isNotBlank) ?: return null
        val cleanAddress = address.cleanProviderAddress(addressType) ?: return null
        return IntegratedContactEndpoint.ProviderAddress(
            providerId = cleanProviderId,
            packageName = cleanPackageName,
            addressType = addressType,
            address = cleanAddress,
        )
    }

    private fun save(contacts: List<FavoriteIntegratedContact>): Boolean {
        if (!persistence.save(contacts)) return false
        cachedContacts = contacts
        return true
    }

    private fun allContacts(): List<FavoriteIntegratedContact> = cachedContacts
        ?: persistence.load().also { cachedContacts = it }

    private companion object {
        const val ENCRYPTED_FILE_NAME = "favorite_integrated_contacts_v1.enc"
        const val KEY_ALIAS = "mink.favorite-integrated-contacts.aes-gcm.v1"

        fun createPersistence(context: Context): EncryptedFavoriteIntegratedContactPersistence =
            EncryptedFavoriteIntegratedContactPersistence(
                storage = AtomicContactDataBlobStorage(
                    File(context.noBackupFilesDir, ENCRYPTED_FILE_NAME),
                ),
                cipher = AesGcmContactDataCipher(
                    keyProvider = AndroidKeystoreContactDataKeyProvider(KEY_ALIAS),
                    envelopeMagic = 0x4D_49_43_45, // MICE
                    authenticatedContext =
                        "openmink:favorite-integrated-contacts:v1".toByteArray(Charsets.UTF_8),
                ),
            )
    }
}

private fun String.cleanFavoriteName(): String? =
    trim().take(FAVORITE_INTEGRATED_CONTACT_NAME_MAX_CHARS).takeIf(String::isNotBlank)

private fun List<FavoriteIntegratedContact>.hasEndpoint(
    endpoint: IntegratedContactEndpoint,
    excluding: FavoriteContactId.Integrated?,
): Boolean = any { contact ->
    contact.id != excluding && contact.endpoints.any { it.identity == endpoint.identity }
}
