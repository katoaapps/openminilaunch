package com.katoaapps.openminilaunch.features.conversations

import com.katoaapps.openminilaunch.model.FavoriteAndroidContact
import com.katoaapps.openminilaunch.model.FavoriteContact
import com.katoaapps.openminilaunch.model.FavoriteContactId
import com.katoaapps.openminilaunch.model.FavoriteIntegratedContact
import com.katoaapps.openminilaunch.model.IntegratedContactEndpoint
import com.katoaapps.openminilaunch.model.ProviderAddressType
import java.security.MessageDigest
import java.util.Locale

internal data class FavoriteConversationMatch(
    val favoriteId: FavoriteContactId,
    val favoriteRank: Int,
)

/**
 * Claims conversations for favorites in the user's chosen order.
 *
 * The same result drives ordering and visual treatment so overlapping provider identities cannot
 * make a row look like one favorite while sorting it as another.
 */
internal fun matchFavoriteConversations(
    conversations: List<NotificationConversation>,
    orderedFavorites: List<FavoriteContact>,
): Map<String, FavoriteConversationMatch> {
    if (conversations.isEmpty() || orderedFavorites.isEmpty()) return emptyMap()

    return buildMap {
        orderedFavorites.forEachIndexed { favoriteRank, favorite ->
            conversations.asSequence()
                .filterNot { containsKey(it.id) }
                .filter(favorite::matchesConversation)
                .forEach { conversation ->
                    put(
                        conversation.id,
                        FavoriteConversationMatch(
                            favoriteId = favorite.id,
                            favoriteRank = favoriteRank,
                        ),
                    )
                }
        }
    }
}

/**
 * Moves active favorite conversations ahead of other conversations.
 *
 * Favorite rank comes from the user's Favorites order. Multiple conversations belonging to the
 * same favorite, plus all non-favorite conversations, retain the recency order supplied by the
 * notification store. When identities overlap, the highest-priority favorite claims the thread.
 */
internal fun prioritizeFavoriteConversations(
    conversations: List<NotificationConversation>,
    orderedFavorites: List<FavoriteContact>,
): List<NotificationConversation> {
    if (conversations.size < 2 || orderedFavorites.isEmpty()) return conversations

    val matches = matchFavoriteConversations(conversations, orderedFavorites)
    if (matches.isEmpty()) return conversations

    return conversations.withIndex()
        .sortedWith(
            compareBy<IndexedValue<NotificationConversation>>(
                { matches[it.value.id]?.favoriteRank ?: Int.MAX_VALUE },
                IndexedValue<NotificationConversation>::index,
            ),
        )
        .map(IndexedValue<NotificationConversation>::value)
}

/** Matches only stable identities; display names are never treated as identity. */
internal fun FavoriteContact.matchesConversation(conversation: NotificationConversation): Boolean = when (this) {
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
