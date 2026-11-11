package com.katoaapps.openminilaunch.features.contacts

import android.content.Context
import com.katoaapps.openminilaunch.model.FavoriteContactId
import com.katoaapps.openminilaunch.model.FavoriteIntegratedContact
import com.katoaapps.openminilaunch.model.FavoriteIntegratedContactNameSource
import com.katoaapps.openminilaunch.model.IntegratedContactCallEndpoint
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
 * Encrypted local ownership for favorites connected to provider-owned conversations or calls.
 * Portable exports carry a deliberately readable copy after an explicit privacy warning; the
 * normal on-device store remains encrypted and excluded from Android Auto Backup.
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
        val providerName = shortcut.label.cleanFavoriteName()
        val cleanName = displayName.cleanFavoriteName()
            ?: providerName
            ?: return FavoriteIntegratedContactAddResult.FAILED
        return add(
            endpoint = endpoint,
            displayName = cleanName,
            nameSource = if (cleanName == providerName) {
                FavoriteIntegratedContactNameSource.PROVIDER
            } else {
                FavoriteIntegratedContactNameSource.USER
            },
        )
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
        return add(endpoint, cleanName, FavoriteIntegratedContactNameSource.USER)
    }

    /** Adds a call-only favorite through Android's standard phone-number contract. */
    @Synchronized
    fun addCall(
        packageName: String?,
        phoneNumber: String,
        displayName: String,
    ): FavoriteIntegratedContactAddResult {
        val endpoint = callEndpoint(packageName, phoneNumber)
            ?: return FavoriteIntegratedContactAddResult.FAILED
        val cleanName = displayName.cleanFavoriteName()
            ?: return FavoriteIntegratedContactAddResult.FAILED
        val current = allContacts()
        if (current.hasCallEndpoint(endpoint, excluding = null)) {
            return FavoriteIntegratedContactAddResult.DUPLICATE
        }
        val updated = current + FavoriteIntegratedContact(
            id = FavoriteContactId.Integrated(UUID.randomUUID().toString()),
            displayName = cleanName,
            endpoints = emptyList(),
            callEndpoint = endpoint,
            nameSource = FavoriteIntegratedContactNameSource.USER,
        )
        return if (save(updated)) {
            FavoriteIntegratedContactAddResult.ADDED
        } else {
            FavoriteIntegratedContactAddResult.FAILED
        }
    }

    @Synchronized
    fun remove(id: FavoriteContactId.Integrated): Boolean {
        val current = allContacts()
        val updated = current.filterNot { it.id == id }
        if (updated.size == current.size) return false
        return save(updated)
    }

    /** Saves an explicit user alias without changing the provider identity or routes. */
    @Synchronized
    fun rename(id: FavoriteContactId.Integrated, displayName: String): Boolean {
        val cleanName = displayName.cleanFavoriteName() ?: return false
        val current = allContacts()
        val contactIndex = current.indexOfFirst { it.id == id }
        if (contactIndex == -1) return false
        val existing = current[contactIndex]
        if (
            existing.displayName == cleanName &&
            existing.nameSource == FavoriteIntegratedContactNameSource.USER
        ) {
            return true
        }
        val updated = current.toMutableList().apply {
            this[contactIndex] = existing.copy(
                displayName = cleanName,
                nameSource = FavoriteIntegratedContactNameSource.USER,
            )
        }
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

    /** Adds or changes this favorite's standard phone-number calling route. */
    @Synchronized
    fun updateCall(
        id: FavoriteContactId.Integrated,
        packageName: String?,
        phoneNumber: String,
    ): Boolean {
        val replacement = callEndpoint(packageName, phoneNumber) ?: return false
        val current = allContacts()
        val contactIndex = current.indexOfFirst { it.id == id }
        if (contactIndex == -1 || current.hasCallEndpoint(replacement, excluding = id)) return false
        val updated = current.toMutableList().apply {
            this[contactIndex] = this[contactIndex].copy(callEndpoint = replacement)
        }
        return save(updated)
    }

    /**
     * Imports Mink-owned people without deleting contacts already created on this installation.
     * Existing local ids and endpoint identities win so importing an older backup is non-destructive.
     */
    @Synchronized
    fun mergeFromBackup(imported: List<FavoriteIntegratedContact>): Int {
        val merged = allContacts().toMutableList()
        var added = 0
        imported.forEach { contact ->
            if (contact.displayName.cleanFavoriteName() != contact.displayName ||
                (contact.endpoints.isEmpty() && contact.callEndpoint == null) ||
                contact.endpoints.any { !it.isValid() } ||
                contact.callEndpoint?.isValid() == false ||
                merged.any { it.id == contact.id } ||
                contact.endpoints.any { endpoint -> merged.hasEndpoint(endpoint, excluding = null) } ||
                contact.callEndpoint?.let { merged.hasCallEndpoint(it, excluding = null) } == true
            ) {
                return@forEach
            }
            merged += contact
            added++
        }
        return if (added == 0 || save(merged)) added else 0
    }

    private fun add(
        endpoint: IntegratedContactEndpoint,
        displayName: String,
        nameSource: FavoriteIntegratedContactNameSource,
    ): FavoriteIntegratedContactAddResult {
        val current = allContacts()
        if (current.hasEndpoint(endpoint, excluding = null)) {
            return FavoriteIntegratedContactAddResult.DUPLICATE
        }
        val updated = current + FavoriteIntegratedContact(
            id = FavoriteContactId.Integrated(UUID.randomUUID().toString()),
            displayName = displayName,
            endpoints = listOf(endpoint),
            nameSource = nameSource,
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

        val providerManagedReplacementName = if (
            existing.nameSource == FavoriteIntegratedContactNameSource.PROVIDER
        ) {
            (replacement as? IntegratedContactEndpoint.ConversationShortcut)
                ?.label
                ?.cleanFavoriteName()
        } else {
            null
        }
        val updated = current.toMutableList().apply {
            this[contactIndex] = existing.copy(
                displayName = providerManagedReplacementName ?: existing.displayName,
                endpoints = listOf(replacement),
                nameSource = if (
                    existing.nameSource == FavoriteIntegratedContactNameSource.PROVIDER &&
                    replacement !is IntegratedContactEndpoint.ConversationShortcut
                ) {
                    FavoriteIntegratedContactNameSource.USER
                } else {
                    existing.nameSource
                },
            )
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

    private fun callEndpoint(
        packageName: String?,
        phoneNumber: String,
    ): IntegratedContactCallEndpoint.PhoneNumber? {
        val cleanPackage = packageName?.trim()?.takeIf(String::isNotBlank)
        val cleanNumber = phoneNumber.cleanCallNumber() ?: return null
        return IntegratedContactCallEndpoint.PhoneNumber(
            number = cleanNumber,
            packageName = cleanPackage,
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

private fun List<FavoriteIntegratedContact>.hasCallEndpoint(
    endpoint: IntegratedContactCallEndpoint,
    excluding: FavoriteContactId.Integrated?,
): Boolean = any { contact ->
    contact.id != excluding && contact.callEndpoint?.identity == endpoint.identity
}
