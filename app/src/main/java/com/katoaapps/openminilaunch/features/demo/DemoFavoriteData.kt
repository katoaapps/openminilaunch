package com.katoaapps.openminilaunch.features.demo

import com.katoaapps.openminilaunch.model.AndroidContactPhone
import com.katoaapps.openminilaunch.model.FavoriteAndroidContact
import com.katoaapps.openminilaunch.model.FavoriteContact
import com.katoaapps.openminilaunch.model.FavoriteContactId

/** Fictional Favorites used in demo mode so captures never expose the device owner's contacts. */
internal object DemoFavoriteData {
    private const val MAY_COLOR = 0xFF547EE8.toInt()
    private const val MAYA_COLOR = 0xFFE87654.toInt()
    private const val GARY_COLOR = 0xFF4EA36D.toInt()
    private const val PRIYA_COLOR = 0xFF9364D8.toInt()

    fun contacts(scenario: DemoScenario): List<FavoriteContact> = when (scenario) {
        DemoScenario.PRODUCT_TOUR -> listOf(
            contact("maya-diaz", "Maya Diaz", "Acme Studio", "+1 202-555-0101"),
            contact("may-wong", "May Wong", "Friends", "+1 202-555-0102"),
            contact("noah-williams", "Noah Williams", "Family", "+1 202-555-0103"),
        )
        DemoScenario.PET_MINK -> listOf(
            contact("may-wong", "May Wong", "Friends", "+1 202-555-0102"),
            contact("gary-chen", "Gary Chen", "Wildlife Center", "+1 202-555-0104"),
            contact("kara-ellis", "Kara Ellis", "Family", "+1 202-555-0105"),
        )
        DemoScenario.WORKDAY -> listOf(
            contact("maya-diaz", "Maya Diaz", "Acme Studio", "+1 202-555-0101"),
            contact("priya-shah", "Priya Shah", "Acme Studio", "+1 202-555-0106"),
            contact("avery-brooks", "Avery Brooks", "Design", "+1 202-555-0107"),
        )
    }

    fun attentionColor(id: FavoriteContactId): Int? = when (id) {
        is FavoriteContactId.Android -> when (id.lookupKey) {
            "demo-may-wong" -> MAY_COLOR
            "demo-maya-diaz" -> MAYA_COLOR
            "demo-gary-chen" -> GARY_COLOR
            "demo-priya-shah" -> PRIYA_COLOR
            else -> null
        }
        is FavoriteContactId.Integrated -> null
    }

    private fun contact(
        slug: String,
        name: String,
        organization: String,
        phone: String,
    ) = FavoriteAndroidContact(
        id = FavoriteContactId.Android("demo-$slug"),
        contactUri = "demo://contact/$slug",
        displayName = name,
        photoUri = null,
        organization = organization,
        phones = listOf(
            AndroidContactPhone(
                rawNumber = phone,
                normalizedNumber = phone.filter(Char::isDigit),
                label = "Mobile",
                isPrimary = true,
                isSuperPrimary = true,
            ),
        ),
    )
}
