package com.katoaapps.openminilaunch.features.contacts

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.database.ContentObserver
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.provider.ContactsContract
import android.telephony.PhoneNumberUtils
import androidx.core.content.ContextCompat
import com.katoaapps.openminilaunch.R
import com.katoaapps.openminilaunch.model.AndroidContactPhone
import com.katoaapps.openminilaunch.model.FavoriteAndroidContact
import com.katoaapps.openminilaunch.model.FavoriteContactId
import java.io.Closeable
import java.text.Collator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/** Local, read-only access to contacts starred in Android Contacts. */
internal class FavoriteAndroidContactsRepository(context: Context) : Closeable {
    private val appContext = context.applicationContext
    private val resolver = appContext.contentResolver
    private val revisionState = MutableStateFlow(0L)
    val revision: StateFlow<Long> = revisionState.asStateFlow()

    private val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
        override fun onChange(selfChange: Boolean, uri: Uri?) {
            revisionState.update { it + 1L }
        }
    }
    private var observerRegistered = false

    init {
        registerObserverIfPermitted()
    }

    fun favorites(): List<FavoriteAndroidContact> {
        if (!hasContactsPermission()) return emptyList()
        return try {
            val records = queryFavoriteRecords()
            if (records.isEmpty()) return emptyList()
            val contactIds = records.map(FavoriteRecord::contactId).toSet()
            val phones = queryPhones(contactIds)
            val organizations = queryOrganizations(contactIds)
            val locale = appContext.resources.configuration.locales[0]
            val collator = Collator.getInstance(locale)
            records.map { record ->
                FavoriteAndroidContact(
                    id = FavoriteContactId.Android(record.lookupKey),
                    contactUri = ContactsContract.Contacts.getLookupUri(
                        record.contactId,
                        record.lookupKey,
                    ).toString(),
                    displayName = record.displayName,
                    photoUri = record.photoUri,
                    organization = organizations[record.contactId],
                    phones = phones[record.contactId].orEmpty(),
                )
            }.sortedWith { first, second -> collator.compare(first.displayName, second.displayName) }
        } catch (_: SecurityException) {
            emptyList()
        }
    }

    /**
     * Returns provider-owned identities for starred contacts without reading account names.
     * Values stay in memory long enough to create or resolve backup-scoped match tokens.
     */
    fun portableIdentityCandidates(): Map<String, List<AndroidFavoriteIdentityCandidate>> {
        if (!hasContactsPermission()) return emptyMap()
        return try {
            val records = queryFavoriteIdentityRecords()
            if (records.isEmpty()) return emptyMap()
            val candidates = queryPortableIdentityCandidates(
                records.map(FavoriteIdentityRecord::contactId).toSet(),
            )
            records.mapNotNull { record ->
                candidates[record.contactId]
                    ?.takeIf { it.isNotEmpty() }
                    ?.let { record.lookupKey to it }
            }.toMap()
        } catch (_: RuntimeException) {
            emptyMap()
        }
    }

    override fun close() {
        if (observerRegistered) {
            runCatching { resolver.unregisterContentObserver(observer) }
            observerRegistered = false
        }
    }

    private fun registerObserverIfPermitted() {
        if (!hasContactsPermission()) return
        try {
            resolver.registerContentObserver(ContactsContract.Contacts.CONTENT_URI, true, observer)
            observerRegistered = true
            resolver.registerContentObserver(ContactsContract.Data.CONTENT_URI, true, observer)
        } catch (_: SecurityException) {
            close()
        }
    }

    private fun hasContactsPermission(): Boolean =
        ContextCompat.checkSelfPermission(appContext, Manifest.permission.READ_CONTACTS) ==
            PackageManager.PERMISSION_GRANTED

    private fun queryFavoriteRecords(): List<FavoriteRecord> {
        val projection = arrayOf(
            ContactsContract.Contacts._ID,
            ContactsContract.Contacts.LOOKUP_KEY,
            ContactsContract.Contacts.DISPLAY_NAME_PRIMARY,
            ContactsContract.Contacts.PHOTO_THUMBNAIL_URI,
        )
        return buildList {
            resolver.query(
                ContactsContract.Contacts.CONTENT_URI,
                projection,
                "${ContactsContract.Contacts.STARRED} = 1",
                null,
                null,
            )?.use { cursor ->
                val idIndex = cursor.getColumnIndexOrThrow(projection[0])
                val lookupIndex = cursor.getColumnIndexOrThrow(projection[1])
                val nameIndex = cursor.getColumnIndexOrThrow(projection[2])
                val photoIndex = cursor.getColumnIndexOrThrow(projection[3])
                while (cursor.moveToNext()) {
                    val lookupKey = cursor.getString(lookupIndex).orEmpty()
                    if (lookupKey.isBlank()) continue
                    add(
                        FavoriteRecord(
                            contactId = cursor.getLong(idIndex),
                            lookupKey = lookupKey,
                            displayName = cursor.getString(nameIndex)
                                ?.takeIf(String::isNotBlank)
                                ?: appContext.getString(R.string.unnamed_contact),
                            photoUri = cursor.getString(photoIndex)?.takeIf(String::isNotBlank),
                        ),
                    )
                }
            }
        }
    }

    /** Minimal identity-only query for backup reconciliation; no names, photos, or endpoints. */
    private fun queryFavoriteIdentityRecords(): List<FavoriteIdentityRecord> {
        val projection = arrayOf(
            ContactsContract.Contacts._ID,
            ContactsContract.Contacts.LOOKUP_KEY,
        )
        return buildList {
            resolver.query(
                ContactsContract.Contacts.CONTENT_URI,
                projection,
                "${ContactsContract.Contacts.STARRED} = 1",
                null,
                null,
            )?.use { cursor ->
                val idIndex = cursor.getColumnIndexOrThrow(projection[0])
                val lookupIndex = cursor.getColumnIndexOrThrow(projection[1])
                while (cursor.moveToNext()) {
                    val lookupKey = cursor.getString(lookupIndex).orEmpty()
                    if (lookupKey.isNotBlank()) {
                        add(
                            FavoriteIdentityRecord(
                                contactId = cursor.getLong(idIndex),
                                lookupKey = lookupKey,
                            ),
                        )
                    }
                }
            }
        }
    }

    private fun queryPhones(contactIds: Set<Long>): Map<Long, List<AndroidContactPhone>> {
        if (contactIds.isEmpty()) return emptyMap()
        val collected = mutableMapOf<Long, MutableList<AndroidContactPhone>>()
        contactIds.chunked(MAX_IDS_PER_QUERY).forEach { ids ->
            val placeholders = ids.joinToString(",") { "?" }
            val projection = arrayOf(
                ContactsContract.CommonDataKinds.Phone.CONTACT_ID,
                ContactsContract.CommonDataKinds.Phone.NUMBER,
                ContactsContract.CommonDataKinds.Phone.NORMALIZED_NUMBER,
                ContactsContract.CommonDataKinds.Phone.TYPE,
                ContactsContract.CommonDataKinds.Phone.LABEL,
                ContactsContract.CommonDataKinds.Phone.IS_PRIMARY,
                ContactsContract.CommonDataKinds.Phone.IS_SUPER_PRIMARY,
            )
            resolver.query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                projection,
                "${ContactsContract.CommonDataKinds.Phone.CONTACT_ID} IN ($placeholders)",
                ids.map(Long::toString).toTypedArray(),
                null,
            )?.use { cursor ->
                val idIndex = cursor.getColumnIndexOrThrow(projection[0])
                val numberIndex = cursor.getColumnIndexOrThrow(projection[1])
                val normalizedIndex = cursor.getColumnIndexOrThrow(projection[2])
                val typeIndex = cursor.getColumnIndexOrThrow(projection[3])
                val labelIndex = cursor.getColumnIndexOrThrow(projection[4])
                val primaryIndex = cursor.getColumnIndexOrThrow(projection[5])
                val superPrimaryIndex = cursor.getColumnIndexOrThrow(projection[6])
                while (cursor.moveToNext()) {
                    val rawNumber = cursor.getString(numberIndex)?.takeIf(String::isNotBlank)
                        ?: continue
                    val normalizedNumber = cursor.getString(normalizedIndex)
                        ?.takeIf(String::isNotBlank)
                        ?: PhoneNumberUtils.normalizeNumber(rawNumber).ifBlank { rawNumber }
                    val label = ContactsContract.CommonDataKinds.Phone.getTypeLabel(
                        appContext.resources,
                        cursor.getInt(typeIndex),
                        cursor.getString(labelIndex),
                    ).toString().ifBlank { appContext.getString(R.string.phone) }
                    collected.getOrPut(cursor.getLong(idIndex), ::mutableListOf).add(
                        AndroidContactPhone(
                            rawNumber = rawNumber,
                            normalizedNumber = normalizedNumber,
                            label = label,
                            isPrimary = cursor.getInt(primaryIndex) != 0,
                            isSuperPrimary = cursor.getInt(superPrimaryIndex) != 0,
                        ),
                    )
                }
            }
        }
        return collected.mapValues { (_, phones) ->
            phones.sortedWith(
                compareByDescending<AndroidContactPhone>(AndroidContactPhone::isSuperPrimary)
                    .thenByDescending(AndroidContactPhone::isPrimary)
                    .thenBy(String.CASE_INSENSITIVE_ORDER, AndroidContactPhone::label)
                    .thenBy(AndroidContactPhone::normalizedNumber),
            ).distinctBy(AndroidContactPhone::normalizedNumber)
        }
    }

    private fun queryOrganizations(contactIds: Set<Long>): Map<Long, String> {
        if (contactIds.isEmpty()) return emptyMap()
        val collected = mutableMapOf<Long, RankedOrganization>()
        contactIds.chunked(MAX_IDS_PER_QUERY).forEach { ids ->
            val placeholders = ids.joinToString(",") { "?" }
            val projection = arrayOf(
                ContactsContract.Data.CONTACT_ID,
                ContactsContract.CommonDataKinds.Organization.COMPANY,
                ContactsContract.Data.IS_PRIMARY,
                ContactsContract.Data.IS_SUPER_PRIMARY,
            )
            val selection = buildString {
                append("${ContactsContract.Data.CONTACT_ID} IN ($placeholders)")
                append(" AND ${ContactsContract.Data.MIMETYPE} = ?")
            }
            val arguments = ids.map(Long::toString) +
                ContactsContract.CommonDataKinds.Organization.CONTENT_ITEM_TYPE
            resolver.query(
                ContactsContract.Data.CONTENT_URI,
                projection,
                selection,
                arguments.toTypedArray(),
                null,
            )?.use { cursor ->
                val idIndex = cursor.getColumnIndexOrThrow(projection[0])
                val companyIndex = cursor.getColumnIndexOrThrow(projection[1])
                val primaryIndex = cursor.getColumnIndexOrThrow(projection[2])
                val superPrimaryIndex = cursor.getColumnIndexOrThrow(projection[3])
                while (cursor.moveToNext()) {
                    val company = cursor.getString(companyIndex)?.trim()
                        ?.takeIf(String::isNotBlank)
                        ?: continue
                    val rank = when {
                        cursor.getInt(superPrimaryIndex) != 0 -> 2
                        cursor.getInt(primaryIndex) != 0 -> 1
                        else -> 0
                    }
                    val contactId = cursor.getLong(idIndex)
                    val current = collected[contactId]
                    if (current == null || rank > current.rank) {
                        collected[contactId] = RankedOrganization(company, rank)
                    }
                }
            }
        }
        return collected.mapValues { (_, organization) -> organization.company }
    }

    private fun queryPortableIdentityCandidates(
        contactIds: Set<Long>,
    ): Map<Long, List<AndroidFavoriteIdentityCandidate>> {
        if (contactIds.isEmpty()) return emptyMap()
        val collected = mutableMapOf<Long, MutableList<AndroidFavoriteIdentityCandidate>>()
        contactIds.chunked(MAX_IDS_PER_QUERY).forEach { ids ->
            val placeholders = ids.joinToString(",") { "?" }
            val projection = arrayOf(
                ContactsContract.RawContacts.CONTACT_ID,
                ContactsContract.RawContacts.ACCOUNT_TYPE,
                ContactsContract.RawContacts.DATA_SET,
                ContactsContract.RawContacts.BACKUP_ID,
                ContactsContract.RawContacts.SOURCE_ID,
            )
            resolver.query(
                ContactsContract.RawContacts.CONTENT_URI,
                projection,
                "${ContactsContract.RawContacts.CONTACT_ID} IN ($placeholders) AND " +
                    "${ContactsContract.RawContacts.DELETED} = 0",
                ids.map(Long::toString).toTypedArray(),
                null,
            )?.use { cursor ->
                val contactIdIndex = cursor.getColumnIndexOrThrow(projection[0])
                val accountTypeIndex = cursor.getColumnIndexOrThrow(projection[1])
                val dataSetIndex = cursor.getColumnIndexOrThrow(projection[2])
                val backupIdIndex = cursor.getColumnIndexOrThrow(projection[3])
                val sourceIdIndex = cursor.getColumnIndexOrThrow(projection[4])
                while (cursor.moveToNext()) {
                    val accountType = cursor.getString(accountTypeIndex)
                        ?.trim()
                        ?.takeIf(String::isNotBlank)
                        ?.takeIf { it.length <= MAX_PORTABLE_METADATA_LENGTH }
                        ?: continue
                    val dataSet = cursor.getString(dataSetIndex)
                        ?.trim()
                        ?.takeIf(String::isNotBlank)
                        ?.takeIf { it.length <= MAX_PORTABLE_METADATA_LENGTH }
                    val contactId = cursor.getLong(contactIdIndex)
                    fun collect(kind: AndroidFavoriteIdentityKind, index: Int) {
                        val value = cursor.getString(index)
                            ?.trim()
                            ?.takeIf(::isSafePortableContactIdentifier)
                            ?: return
                        val candidate = AndroidFavoriteIdentityCandidate(
                            kind = kind,
                            accountType = accountType,
                            dataSet = dataSet,
                            value = value,
                        )
                        if (isSafePortableContactCandidate(candidate)) {
                            collected.getOrPut(contactId, ::mutableListOf).add(candidate)
                        }
                    }
                    collect(AndroidFavoriteIdentityKind.BACKUP_ID, backupIdIndex)
                    collect(AndroidFavoriteIdentityKind.SOURCE_ID, sourceIdIndex)
                }
            }
        }
        return collected.mapValues { (_, candidates) -> candidates.distinct() }
    }

    private data class FavoriteRecord(
        val contactId: Long,
        val lookupKey: String,
        val displayName: String,
        val photoUri: String?,
    )

    private data class FavoriteIdentityRecord(
        val contactId: Long,
        val lookupKey: String,
    )

    private data class RankedOrganization(val company: String, val rank: Int)

    private companion object {
        const val MAX_IDS_PER_QUERY = 400
        const val MAX_PORTABLE_METADATA_LENGTH = 512
    }
}
