package com.katoaapps.openminilaunch.features.conversations

import com.katoaapps.openminilaunch.model.FavoriteContact
import com.katoaapps.openminilaunch.model.FavoriteContactId

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
