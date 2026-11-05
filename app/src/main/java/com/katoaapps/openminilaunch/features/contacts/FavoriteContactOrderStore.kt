package com.katoaapps.openminilaunch.features.contacts

import android.content.Context
import com.katoaapps.openminilaunch.model.FavoriteContact
import com.katoaapps.openminilaunch.model.FavoriteContactId
import java.io.File

/** Persists the user's mixed Android and Mink favorite order without copying contact details. */
internal class FavoriteContactOrderStore(
    private val persistence: EncryptedFavoriteContactOrderPersistence,
) {
    private var cachedOrder: List<String>? = null

    constructor(context: Context) : this(createPersistence(context.applicationContext))

    @Synchronized
    fun applyTo(contacts: List<FavoriteContact>): List<FavoriteContact> =
        FavoriteContactCatalog.applyOrder(contacts, order())

    /** Reorders currently visible contacts while retaining hidden or temporarily unavailable IDs. */
    @Synchronized
    fun updateVisibleOrder(visibleKeys: List<String>): Boolean {
        val clean = visibleKeys.filter(String::isNotBlank).distinct()
        if (clean.size != visibleKeys.size) return false
        val current = order()
        val visible = clean.toSet()
        val reordered = clean.iterator()
        val updated = buildList {
            current.forEach { key ->
                add(if (key in visible && reordered.hasNext()) reordered.next() else key)
            }
            while (reordered.hasNext()) add(reordered.next())
        }.distinct()
        if (updated == current) return true
        return persistence.save(updated).also { saved ->
            if (saved) cachedOrder = updated
        }
    }

    @Synchronized
    fun portableIntegratedOrder(availableLocalIds: Set<String>): List<String> = order()
        .mapNotNull { key ->
            key.removePrefix(INTEGRATED_KEY_PREFIX)
                .takeIf { key.startsWith(INTEGRATED_KEY_PREFIX) && it in availableLocalIds }
        }

    @Synchronized
    fun restoreIntegratedOrder(localIds: List<String>): Boolean = updateVisibleOrder(
        localIds.filter(String::isNotBlank)
            .distinct()
            .map { FavoriteContactId.Integrated(it).storageKey },
    )

    @Synchronized
    internal fun order(): List<String> = cachedOrder
        ?: persistence.load().also { cachedOrder = it }

    private companion object {
        const val ENCRYPTED_FILE_NAME = "favorite_contact_order_v1.enc"
        const val KEY_ALIAS = "mink.favorite-contact-order.aes-gcm.v1"
        const val INTEGRATED_KEY_PREFIX = "integrated-contact:"

        fun createPersistence(context: Context): EncryptedFavoriteContactOrderPersistence =
            EncryptedFavoriteContactOrderPersistence(
                storage = AtomicContactDataBlobStorage(
                    File(context.noBackupFilesDir, ENCRYPTED_FILE_NAME),
                ),
                cipher = AesGcmContactDataCipher(
                    keyProvider = AndroidKeystoreContactDataKeyProvider(KEY_ALIAS),
                    envelopeMagic = 0x4D_46_43_45,
                    authenticatedContext =
                        "openmink:favorite-contact-order:v1".toByteArray(Charsets.UTF_8),
                ),
            )
    }
}
