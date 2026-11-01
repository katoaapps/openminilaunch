package com.katoaapps.openminilaunch.features.contacts

import android.content.Context
import android.database.ContentObserver
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.provider.ContactsContract
import android.telephony.PhoneNumberUtils
import com.katoaapps.openminilaunch.R
import com.katoaapps.openminilaunch.model.FavoriteContact
import com.katoaapps.openminilaunch.model.FavoritePhone
import java.io.Closeable
import java.text.Collator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/** Local, read-only access to contacts starred in Android Contacts. */
internal class FavoriteContactsRepository(context: Context) : Closeable {
    private val appContext = context.applicationContext
    private val resolver = appContext.contentResolver
    private val revisionState = MutableStateFlow(0L)
    val revision: StateFlow<Long> = revisionState.asStateFlow()

    private val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
        override fun onChange(selfChange: Boolean, uri: Uri?) {
            revisionState.update { it + 1L }
        }
    }

    init {
        resolver.registerContentObserver(ContactsContract.Contacts.CONTENT_URI, true, observer)
        resolver.registerContentObserver(ContactsContract.Data.CONTENT_URI, true, observer)
    }

    fun favorites(): List<FavoriteContact> {
        val records = queryFavoriteRecords()
        if (records.isEmpty()) return emptyList()
        val contactIds = records.map(FavoriteRecord::contactId).toSet()
        val phones = queryPhones(contactIds)
        val organizations = queryOrganizations(contactIds)
        val locale = appContext.resources.configuration.locales[0]
        val collator = Collator.getInstance(locale)
        return records.map { record ->
            FavoriteContact(
                lookupKey = record.lookupKey,
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
    }

    override fun close() {
        runCatching { resolver.unregisterContentObserver(observer) }
    }

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

    private fun queryPhones(contactIds: Set<Long>): Map<Long, List<FavoritePhone>> {
        if (contactIds.isEmpty()) return emptyMap()
        val collected = mutableMapOf<Long, MutableList<FavoritePhone>>()
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
                        FavoritePhone(
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
                compareByDescending<FavoritePhone>(FavoritePhone::isSuperPrimary)
                    .thenByDescending(FavoritePhone::isPrimary)
                    .thenBy(String.CASE_INSENSITIVE_ORDER, FavoritePhone::label)
                    .thenBy(FavoritePhone::normalizedNumber),
            ).distinctBy(FavoritePhone::normalizedNumber)
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

    private data class FavoriteRecord(
        val contactId: Long,
        val lookupKey: String,
        val displayName: String,
        val photoUri: String?,
    )

    private data class RankedOrganization(val company: String, val rank: Int)

    private companion object {
        const val MAX_IDS_PER_QUERY = 400
    }
}
