package com.katoaapps.openminilaunch.features.conversations

import com.katoaapps.openminilaunch.model.FavoriteAndroidContact
import com.katoaapps.openminilaunch.model.FavoriteContact
import com.katoaapps.openminilaunch.model.FavoriteContactId
import com.katoaapps.openminilaunch.model.FavoriteIntegratedContact
import com.katoaapps.openminilaunch.model.IntegratedContactEndpoint
import com.katoaapps.openminilaunch.model.ProviderAddressType
import java.security.MessageDigest
import java.util.Locale

internal data class FavoriteConversationAttention(
    val favoriteId: FavoriteContactId,
    val conversationId: String,
    val colorArgb: Int,
)

/**
 * Resolves only stable Android-contact, conversation-shortcut, and opaque provider-address identities.
 * Display names are intentionally never used as identity because they are neither unique nor stable.
 */
internal fun pendingFavoriteConversationAttention(
    conversations: List<NotificationConversation>,
    orderedFavorites: List<FavoriteContact>,
    defaultColorArgb: Int,
    acknowledgedThrough: (String) -> Long,
    colorFor: (FavoriteContactId) -> Int?,
): List<FavoriteConversationAttention> = buildList {
    val claimedConversationIds = mutableSetOf<String>()
    orderedFavorites.forEach { favorite ->
        val conversation = conversations
            .asSequence()
            .filterNot { it.id in claimedConversationIds }
            .filter(favorite::matchesConversation)
            .filter { conversation ->
                shouldShowConversationAttention(
                    latestActivityAt = conversationActivityAt(conversation),
                    acknowledgedThrough = acknowledgedThrough(conversation.id),
                )
            }
            .maxByOrNull(::conversationActivityAt)
            ?: return@forEach
        claimedConversationIds += conversation.id
        add(
            FavoriteConversationAttention(
                favoriteId = favorite.id,
                conversationId = conversation.id,
                colorArgb = colorFor(favorite.id) ?: defaultColorArgb,
            ),
        )
    }
}

private fun FavoriteContact.matchesConversation(conversation: NotificationConversation): Boolean = when (this) {
    is FavoriteAndroidContact -> conversation.id == "contact:${id.lookupKey}"
    is FavoriteIntegratedContact -> endpoints.any { endpoint ->
        when (endpoint) {
            is IntegratedContactEndpoint.ConversationShortcut ->
                conversation.id == "${endpoint.packageName}:direct:${endpoint.shortcutId}" ||
                    endpoint.packageName in conversation.sourcePackages &&
                    conversationShortcutIdentityHint(
                        endpoint.packageName,
                        endpoint.shortcutId,
                    ) in conversation.identityHints
            is IntegratedContactEndpoint.ProviderAddress ->
                endpoint.packageName in conversation.sourcePackages &&
                    providerAddressIdentityHint(endpoint.addressType, endpoint.address) in
                    conversation.identityHints
        }
    }
}

/** Produces an opaque, process-local comparison value without retaining provider addresses. */
internal fun providerAddressIdentityHint(
    type: ProviderAddressType,
    address: String,
): String? {
    val normalized = when (type) {
        ProviderAddressType.PHONE -> address.filter(Char::isDigit)
        ProviderAddressType.USERNAME -> address.trim().lowercase(Locale.ROOT)
    }.takeIf(String::isNotBlank) ?: return null
    return opaqueIdentityHint(type.name, normalized)
}

internal fun conversationShortcutIdentityHint(
    packageName: String,
    shortcutId: String?,
): String? {
    val normalizedPackage = packageName.trim().takeIf(String::isNotBlank) ?: return null
    val normalizedShortcut = shortcutId?.trim()?.takeIf(String::isNotBlank) ?: return null
    return opaqueIdentityHint("SHORTCUT", "$normalizedPackage:$normalizedShortcut")
}

private fun opaqueIdentityHint(kind: String, normalizedValue: String): String {
    val digestInput = "$kind:$normalizedValue".toByteArray(Charsets.UTF_8)
    return try {
        MessageDigest.getInstance("SHA-256")
            .digest(digestInput)
            .joinToString(separator = "") { byte -> "%02x".format(byte.toInt() and 0xff) }
    } finally {
        digestInput.fill(0)
    }
}
