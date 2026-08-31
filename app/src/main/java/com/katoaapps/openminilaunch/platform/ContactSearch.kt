package com.katoaapps.openminilaunch.platform

import android.content.ContentUris
import android.content.Context
import android.provider.ContactsContract
import android.telephony.PhoneNumberUtils
import com.katoaapps.openminilaunch.R
import com.katoaapps.openminilaunch.features.demo.DemoSearchData
import com.katoaapps.openminilaunch.model.ContactResult

internal class ContactSearch(private val context: Context) {
    fun search(query: String, useDemoData: Boolean): List<ContactResult> {
        if (useDemoData) return DemoSearchData.searchContacts(query)
        if (query.isBlank()) return emptyList()

        val results = mutableListOf<ContactResult>()
        val seenNumbers = mutableSetOf<String>()
        val projection = arrayOf(
            ContactsContract.CommonDataKinds.Phone.CONTACT_ID,
            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME_PRIMARY,
            ContactsContract.CommonDataKinds.Phone.NUMBER,
            ContactsContract.CommonDataKinds.Phone.TYPE,
            ContactsContract.CommonDataKinds.Phone.LABEL,
        )
        val selection = "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME_PRIMARY} LIKE ?"
        context.contentResolver.query(
            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            projection,
            selection,
            arrayOf("$query%"),
            "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME_PRIMARY} ASC",
        )?.use { cursor ->
            val contactIdIndex = cursor.getColumnIndexOrThrow(projection[0])
            val nameIndex = cursor.getColumnIndexOrThrow(projection[1])
            val phoneIndex = cursor.getColumnIndexOrThrow(projection[2])
            val typeIndex = cursor.getColumnIndexOrThrow(projection[3])
            val labelIndex = cursor.getColumnIndexOrThrow(projection[4])
            while (cursor.moveToNext() && results.size < MAX_CONTACT_RESULTS) {
                val phone = cursor.getString(phoneIndex)
                val normalizedPhone = PhoneNumberUtils.normalizeNumber(phone).ifBlank { phone }
                val uniqueNumber = "${cursor.getLong(contactIdIndex)}:$normalizedPhone"
                if (seenNumbers.add(uniqueNumber)) {
                    val phoneLabel = ContactsContract.CommonDataKinds.Phone.getTypeLabel(
                        context.resources,
                        cursor.getInt(typeIndex),
                        cursor.getString(labelIndex),
                    ).toString().ifBlank { context.getString(R.string.phone) }
                    val contactUri = ContentUris.withAppendedId(
                        ContactsContract.Contacts.CONTENT_URI,
                        cursor.getLong(contactIdIndex),
                    ).toString()
                    results += ContactResult(contactUri, cursor.getString(nameIndex), phone, phoneLabel)
                }
            }
        }
        return results
    }

    private companion object {
        const val MAX_CONTACT_RESULTS = 8
    }
}
