package com.katoaapps.openminilaunch.features.contacts

import com.katoaapps.openminilaunch.model.FavoriteAndroidContact
import com.katoaapps.openminilaunch.model.FavoriteContact
import com.katoaapps.openminilaunch.model.FavoriteIntegratedContact
import java.text.Collator
import java.util.Locale

/** Combines independently owned contact sources without guessing that two people are the same. */
internal object FavoriteContactCatalog {
    fun combine(
        androidContacts: List<FavoriteAndroidContact>,
        integratedContacts: List<FavoriteIntegratedContact>,
        locale: Locale,
    ): List<FavoriteContact> {
        val collator = Collator.getInstance(locale)
        return (androidContacts + integratedContacts).sortedWith { first, second ->
            collator.compare(first.displayName, second.displayName)
        }
    }

    fun applyOrder(
        contacts: List<FavoriteContact>,
        orderedStorageKeys: List<String>,
    ): List<FavoriteContact> {
        val rank = orderedStorageKeys.withIndex().associate { it.value to it.index }
        return contacts.withIndex()
            .sortedWith(
                compareBy<IndexedValue<FavoriteContact>>(
                    { rank[it.value.id.storageKey] ?: Int.MAX_VALUE },
                    IndexedValue<FavoriteContact>::index,
                ),
            )
            .map(IndexedValue<FavoriteContact>::value)
    }
}
