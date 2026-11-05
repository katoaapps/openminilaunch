package com.katoaapps.openminilaunch.features.backup

import com.katoaapps.openminilaunch.features.contacts.isValid
import com.katoaapps.openminilaunch.model.FavoriteContactId
import com.katoaapps.openminilaunch.model.FavoriteIntegratedContact
import com.katoaapps.openminilaunch.model.IntegratedContactCallEndpoint
import com.katoaapps.openminilaunch.model.IntegratedContactEndpoint
import com.katoaapps.openminilaunch.model.ProviderAddressType
import org.json.JSONArray
import org.json.JSONObject

/**
 * Portable JSON representation for Mink-created Favorites.
 *
 * This deliberately excludes Android contacts and contact photos. Android contacts remain owned by
 * the system Contacts provider, while the exported fields here are shown to the user as readable
 * data before a backup is written.
 */
internal object FavoriteContactBackupCodec {
    private const val MAX_CONTACTS = 256
    private const val MAX_ENDPOINTS_PER_CONTACT = 8
    private const val MAX_ID_LENGTH = 128
    private const val MAX_NAME_LENGTH = 512
    private const val MAX_PACKAGE_LENGTH = 255
    private const val MAX_PROVIDER_ID_LENGTH = 256
    private const val MAX_ADDRESS_LENGTH = 1_024
    private const val MAX_TARGET_LENGTH = 2_048
    private const val MAX_PHONE_NUMBER_LENGTH = 256

    fun encodeContacts(contacts: List<FavoriteIntegratedContact>): JSONArray = JSONArray().apply {
        contacts.distinctBy { it.id.localId }
            .take(MAX_CONTACTS)
            .forEach { contact ->
                put(JSONObject().apply {
                    put("id", contact.id.localId)
                    put("displayName", contact.displayName)
                    put("messageEndpoints", JSONArray().apply {
                        contact.endpoints.take(MAX_ENDPOINTS_PER_CONTACT).forEach { endpoint ->
                            put(endpoint.toJson())
                        }
                    })
                    contact.callEndpoint?.let { callEndpoint ->
                        put("callEndpoint", callEndpoint.toJson())
                    }
                })
            }
    }

    fun decodeContacts(array: JSONArray?): List<FavoriteIntegratedContact> = buildList {
        val source = array ?: return@buildList
        val seenIds = mutableSetOf<String>()
        repeat(minOf(source.length(), MAX_CONTACTS)) { index ->
            val saved = source.optJSONObject(index) ?: return@repeat
            val id = saved.optString("id").safeText(MAX_ID_LENGTH)
            val displayName = saved.optString("displayName").safeText(MAX_NAME_LENGTH)
            if (id.isBlank() || displayName.isBlank() || !seenIds.add(id)) return@repeat

            val messageEndpoints = saved.optJSONArray("messageEndpoints").decodeMessageEndpoints()
            val callEndpoint = saved.optJSONObject("callEndpoint")?.decodeCallEndpoint()
            if (messageEndpoints.isEmpty() && callEndpoint == null) return@repeat

            add(
                FavoriteIntegratedContact(
                    id = FavoriteContactId.Integrated(id),
                    displayName = displayName,
                    endpoints = messageEndpoints,
                    callEndpoint = callEndpoint,
                ),
            )
        }
    }

    fun encodeOrder(order: List<String>): JSONArray = JSONArray().apply {
        order.asSequence()
            .map { it.safeText(MAX_ID_LENGTH) }
            .filter(String::isNotBlank)
            .distinct()
            .take(MAX_CONTACTS)
            .forEach(::put)
    }

    fun decodeOrder(array: JSONArray?): List<String> = buildList {
        val source = array ?: return@buildList
        repeat(minOf(source.length(), MAX_CONTACTS)) { index ->
            source.optString(index)
                .safeText(MAX_ID_LENGTH)
                .takeIf { it.isNotBlank() && it !in this }
                ?.let(::add)
        }
    }

    private fun IntegratedContactEndpoint.toJson(): JSONObject = JSONObject().apply {
        when (this@toJson) {
            is IntegratedContactEndpoint.ConversationShortcut -> {
                put("type", "shortcut")
                put("packageName", packageName)
                put("shortcutId", shortcutId)
                put("userSerial", userSerial)
                putNullable("label", label)
            }

            is IntegratedContactEndpoint.ProviderAddress -> {
                put("type", "providerAddress")
                put("providerId", providerId)
                put("packageName", packageName)
                put("addressType", addressType.name)
                put("address", address)
            }
        }
    }

    private fun IntegratedContactCallEndpoint.toJson(): JSONObject = JSONObject().apply {
        when (this@toJson) {
            is IntegratedContactCallEndpoint.PhoneNumber -> {
                put("type", "phoneNumber")
                put("number", number)
                putNullable("packageName", packageName)
            }
        }
    }

    private fun JSONArray?.decodeMessageEndpoints(): List<IntegratedContactEndpoint> = buildList {
        val source = this@decodeMessageEndpoints ?: return@buildList
        repeat(minOf(source.length(), MAX_ENDPOINTS_PER_CONTACT)) { index ->
            val saved = source.optJSONObject(index) ?: return@repeat
            val packageName = saved.optString("packageName").safePackage() ?: return@repeat
            val endpoint = when (saved.optString("type")) {
                "shortcut" -> IntegratedContactEndpoint.ConversationShortcut(
                    packageName = packageName,
                    shortcutId = saved.optString("shortcutId").safeText(MAX_TARGET_LENGTH),
                    userSerial = saved.optLong("userSerial", Long.MIN_VALUE),
                    label = saved.optNullableString("label")?.safeText(MAX_NAME_LENGTH),
                )

                "providerAddress" -> IntegratedContactEndpoint.ProviderAddress(
                    providerId = saved.optString("providerId").safeText(MAX_PROVIDER_ID_LENGTH),
                    packageName = packageName,
                    addressType = saved.enumValue("addressType", ProviderAddressType.PHONE),
                    address = saved.optString("address").safeText(MAX_ADDRESS_LENGTH),
                )

                else -> null
            }
            if (endpoint != null && endpoint.isValid() && endpoint !in this) add(endpoint)
        }
    }

    private fun JSONObject.decodeCallEndpoint(): IntegratedContactCallEndpoint? {
        if (optString("type") != "phoneNumber") return null
        val endpoint = IntegratedContactCallEndpoint.PhoneNumber(
            number = optString("number").safeText(MAX_PHONE_NUMBER_LENGTH),
            packageName = optNullableString("packageName")?.safePackage(),
        )
        return endpoint.takeIf(IntegratedContactCallEndpoint::isValid)
    }

    private inline fun <reified T : Enum<T>> JSONObject.enumValue(key: String, fallback: T): T =
        runCatching { enumValueOf<T>(optString(key)) }.getOrDefault(fallback)

    private fun String.safePackage(): String? =
        safeText(MAX_PACKAGE_LENGTH).takeIf(String::isNotBlank)

    private fun String.safeText(maxLength: Int): String = trim().take(maxLength)

    private fun JSONObject.putNullable(key: String, value: Any?) {
        put(key, value ?: JSONObject.NULL)
    }

    private fun JSONObject.optNullableString(key: String): String? =
        if (isNull(key)) null else optString(key).takeIf(String::isNotBlank)
}
