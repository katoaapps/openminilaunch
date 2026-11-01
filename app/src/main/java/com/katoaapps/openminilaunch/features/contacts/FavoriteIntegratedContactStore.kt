package com.katoaapps.openminilaunch.features.contacts

import android.content.Context
import com.katoaapps.openminilaunch.model.FavoriteContactId
import com.katoaapps.openminilaunch.model.FavoriteIntegratedContact
import com.katoaapps.openminilaunch.model.IntegratedContactEndpoint
import com.katoaapps.openminilaunch.model.LauncherShortcutTarget
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.File
import java.io.IOException
import java.util.UUID

// Kotlin's character count can understate UTF-8 size. This remains below the 512-byte codec limit
// so even a name made entirely of four-byte characters can be saved.
internal const val FAVORITE_INTEGRATED_CONTACT_NAME_MAX_CHARS = 120

/**
 * Encrypted, local-only ownership for favorites imported from conversation shortcuts.
 *
 * Unlike Android favorites, these records belong to Mink. They are deliberately kept out of
 * portable backups because shortcut ids and Android user serials are installation-specific.
 */
internal class FavoriteIntegratedContactStore private constructor(
    private val persistence: EncryptedFavoriteIntegratedContactPersistence,
) {
    private var cachedContacts: List<FavoriteIntegratedContact>? = null

    constructor(context: Context) : this(createPersistence(context.applicationContext))

    @Synchronized
    fun contacts(): List<FavoriteIntegratedContact> = allContacts()

    @Synchronized
    fun add(shortcut: LauncherShortcutTarget, displayName: String = shortcut.label): Boolean {
        val endpoint = shortcut.toIntegratedEndpoint()
        val current = allContacts()
        if (current.any { contact -> contact.endpoints.any { it.identity == endpoint.identity } }) {
            return false
        }
        val cleanName = displayName.trim().take(FAVORITE_INTEGRATED_CONTACT_NAME_MAX_CHARS)
            .ifBlank {
                shortcut.label.trim().take(FAVORITE_INTEGRATED_CONTACT_NAME_MAX_CHARS)
            }
            .ifBlank { return false }
        val updated = current + FavoriteIntegratedContact(
            id = FavoriteContactId.Integrated(UUID.randomUUID().toString()),
            displayName = cleanName,
            endpoints = listOf(endpoint),
        )
        if (!persistence.save(updated)) return false
        cachedContacts = updated
        return true
    }

    @Synchronized
    fun remove(id: FavoriteContactId.Integrated): Boolean {
        val current = allContacts()
        val updated = current.filterNot { it.id == id }
        if (updated.size == current.size) return false
        if (!persistence.save(updated)) return false
        cachedContacts = updated
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

internal class EncryptedFavoriteIntegratedContactPersistence(
    private val storage: ContactDataBlobStorage,
    private val cipher: ContactDataCipher,
) {
    fun load(): List<FavoriteIntegratedContact> {
        val encrypted = try {
            storage.read()
        } catch (_: Exception) {
            return emptyList()
        } ?: return emptyList()

        var plaintext: ByteArray? = null
        return try {
            plaintext = cipher.decrypt(encrypted)
            FavoriteIntegratedContactCodec.decode(checkNotNull(plaintext))
        } catch (_: Exception) {
            // Never log names, provider identities, ciphertext, or cryptographic failures here.
            runCatching(cipher::resetKey)
            runCatching(storage::clear)
            emptyList()
        } finally {
            encrypted.fill(0)
            plaintext?.fill(0)
        }
    }

    fun save(contacts: List<FavoriteIntegratedContact>): Boolean {
        var plaintext: ByteArray? = null
        var encrypted: ByteArray? = null
        return try {
            plaintext = FavoriteIntegratedContactCodec.encode(contacts)
            encrypted = encryptWithFreshKeyRecovery(checkNotNull(plaintext))
            storage.write(checkNotNull(encrypted))
            true
        } catch (_: Exception) {
            false
        } finally {
            plaintext?.fill(0)
            encrypted?.fill(0)
        }
    }

    private fun encryptWithFreshKeyRecovery(plaintext: ByteArray): ByteArray = try {
        cipher.encrypt(plaintext)
    } catch (_: Exception) {
        cipher.resetKey()
        cipher.encrypt(plaintext)
    }
}

internal object FavoriteIntegratedContactCodec {
    fun encode(contacts: List<FavoriteIntegratedContact>): ByteArray {
        require(contacts.size <= MAX_CONTACTS) { "Too many integrated contacts" }
        require(contacts.map { it.id.localId }.distinct().size == contacts.size) {
            "Duplicate integrated contact identity"
        }
        val endpointIdentities = contacts.flatMap(FavoriteIntegratedContact::endpoints)
            .map(IntegratedContactEndpoint::identity)
        require(endpointIdentities.distinct().size == endpointIdentities.size) {
            "Duplicate integrated endpoint identity"
        }
        return ByteArrayOutputStream().use { bytes ->
            DataOutputStream(bytes).use { output ->
                output.writeInt(PAYLOAD_MAGIC)
                output.writeInt(PAYLOAD_VERSION)
                output.writeInt(contacts.size)
                contacts.sortedBy { it.id.localId }.forEach { contact ->
                    require(contact.endpoints.isNotEmpty()) { "Integrated contact needs an endpoint" }
                    require(contact.endpoints.size <= MAX_ENDPOINTS_PER_CONTACT) {
                        "Too many integrated contact endpoints"
                    }
                    output.writeBoundedString(contact.id.localId, MAX_LOCAL_ID_BYTES)
                    output.writeBoundedString(contact.displayName, MAX_DISPLAY_NAME_BYTES)
                    output.writeInt(contact.endpoints.size)
                    contact.endpoints.forEach { endpoint ->
                        output.writeBoundedString(endpoint.packageName, MAX_PACKAGE_BYTES)
                        output.writeBoundedString(endpoint.shortcutId, MAX_SHORTCUT_ID_BYTES)
                        output.writeLong(endpoint.userSerial)
                        output.writeNullableBoundedString(endpoint.label, MAX_LABEL_BYTES)
                    }
                }
            }
            bytes.toByteArray()
        }
    }

    fun decode(bytes: ByteArray): List<FavoriteIntegratedContact> =
        DataInputStream(ByteArrayInputStream(bytes)).use { input ->
            if (input.readInt() != PAYLOAD_MAGIC) throw IOException("Invalid integrated contact payload")
            if (input.readInt() != PAYLOAD_VERSION) throw IOException("Unsupported integrated contact payload")
            val count = input.readInt()
            if (count !in 0..MAX_CONTACTS) throw IOException("Invalid integrated contact count")
            val seenIds = mutableSetOf<String>()
            val seenEndpoints = mutableSetOf<String>()
            buildList(count) {
                repeat(count) {
                    val localId = input.readBoundedString(MAX_LOCAL_ID_BYTES)
                    val displayName = input.readBoundedString(MAX_DISPLAY_NAME_BYTES)
                    if (localId.isBlank() || displayName.isBlank() || !seenIds.add(localId)) {
                        throw IOException("Invalid integrated contact identity")
                    }
                    val endpointCount = input.readInt()
                    if (endpointCount !in 1..MAX_ENDPOINTS_PER_CONTACT) {
                        throw IOException("Invalid integrated endpoint count")
                    }
                    val endpoints = buildList(endpointCount) {
                        repeat(endpointCount) {
                            val endpoint = IntegratedContactEndpoint(
                                packageName = input.readBoundedString(MAX_PACKAGE_BYTES),
                                shortcutId = input.readBoundedString(MAX_SHORTCUT_ID_BYTES),
                                userSerial = input.readLong(),
                                label = input.readNullableBoundedString(MAX_LABEL_BYTES),
                            )
                            if (endpoint.packageName.isBlank() || endpoint.shortcutId.isBlank() ||
                                !seenEndpoints.add(endpoint.identity)
                            ) {
                                throw IOException("Invalid integrated endpoint identity")
                            }
                            add(endpoint)
                        }
                    }
                    add(
                        FavoriteIntegratedContact(
                            id = FavoriteContactId.Integrated(localId),
                            displayName = displayName,
                            endpoints = endpoints,
                        ),
                    )
                }
                if (input.read() != -1) throw IOException("Trailing integrated contact data")
            }
        }

    private fun DataOutputStream.writeBoundedString(value: String, maxBytes: Int) {
        val encoded = value.toByteArray(Charsets.UTF_8)
        try {
            require(encoded.isNotEmpty() && encoded.size <= maxBytes) { "String is out of bounds" }
            writeInt(encoded.size)
            write(encoded)
        } finally {
            encoded.fill(0)
        }
    }

    private fun DataOutputStream.writeNullableBoundedString(value: String?, maxBytes: Int) {
        writeBoolean(value != null)
        if (value != null) writeBoundedString(value, maxBytes)
    }

    private fun DataInputStream.readBoundedString(maxBytes: Int): String {
        val size = readInt()
        if (size !in 1..maxBytes) throw IOException("Invalid string size")
        val encoded = ByteArray(size)
        return try {
            readFully(encoded)
            encoded.toString(Charsets.UTF_8)
        } finally {
            encoded.fill(0)
        }
    }

    private fun DataInputStream.readNullableBoundedString(maxBytes: Int): String? =
        if (readBoolean()) readBoundedString(maxBytes) else null

    private const val PAYLOAD_MAGIC = 0x4D_49_43_50 // MICP
    private const val PAYLOAD_VERSION = 1
    private const val MAX_CONTACTS = 256
    private const val MAX_ENDPOINTS_PER_CONTACT = 8
    private const val MAX_LOCAL_ID_BYTES = 128
    private const val MAX_DISPLAY_NAME_BYTES = 512
    private const val MAX_PACKAGE_BYTES = 512
    private const val MAX_SHORTCUT_ID_BYTES = 2048
    private const val MAX_LABEL_BYTES = 512
}

internal val IntegratedContactEndpoint.identity: String
    get() = "$userSerial\u0000$packageName\u0000$shortcutId"

internal fun LauncherShortcutTarget.toIntegratedEndpoint(): IntegratedContactEndpoint =
    IntegratedContactEndpoint(
        packageName = packageName,
        shortcutId = shortcutId,
        userSerial = userSerial,
        label = label.takeIf(String::isNotBlank),
    )
