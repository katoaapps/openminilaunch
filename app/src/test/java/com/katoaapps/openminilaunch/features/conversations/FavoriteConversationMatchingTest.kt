package com.katoaapps.openminilaunch.features.conversations

import com.katoaapps.openminilaunch.model.FavoriteAndroidContact
import com.katoaapps.openminilaunch.model.FavoriteContactId
import com.katoaapps.openminilaunch.model.FavoriteIntegratedContact
import com.katoaapps.openminilaunch.model.IntegratedContactEndpoint
import org.junit.Assert.assertEquals
import org.junit.Test

class FavoriteConversationMatchingTest {
    @Test
    fun favoriteOrderOverridesConversationRecency() {
        val conversations = listOf(
            conversation("mail.app:direct:newsletter", 400L),
            conversation("contact:low", 300L),
            conversation("contact:high", 100L),
        )

        val ordered = prioritizeFavoriteConversations(
            conversations = conversations,
            orderedFavorites = listOf(androidFavorite("high"), androidFavorite("low")),
        )

        assertEquals(
            listOf("contact:high", "contact:low", "mail.app:direct:newsletter"),
            ordered.map(NotificationConversation::id),
        )
    }

    @Test
    fun conversationsForOneFavoriteKeepTheirExistingRecencyOrder() {
        val conversations = listOf(
            conversation("mail.app:direct:newsletter", 400L),
            conversation("chat.app:direct:newer", 300L),
            conversation("chat.app:direct:older", 200L),
            conversation("mail.app:direct:older", 100L),
        )
        val favorite = FavoriteIntegratedContact(
            id = FavoriteContactId.Integrated("favorite-chat"),
            displayName = "Favorite",
            endpoints = listOf(
                shortcutEndpoint("newer"),
                shortcutEndpoint("older"),
            ),
        )

        val ordered = prioritizeFavoriteConversations(conversations, listOf(favorite))

        assertEquals(
            listOf(
                "chat.app:direct:newer",
                "chat.app:direct:older",
                "mail.app:direct:newsletter",
                "mail.app:direct:older",
            ),
            ordered.map(NotificationConversation::id),
        )
    }

    @Test
    fun noMatchingFavoritesPreservesConversationOrder() {
        val conversations = listOf(
            conversation("mail.app:direct:newer", 200L),
            conversation("mail.app:direct:older", 100L),
        )

        assertEquals(
            conversations,
            prioritizeFavoriteConversations(conversations, listOf(androidFavorite("missing"))),
        )
    }

    @Test
    fun matchingUsesTheHighestPriorityFavoriteAsTheRowOwner() {
        val conversation = conversation("chat.app:direct:shared", 100L)
        val highPriority = FavoriteIntegratedContact(
            id = FavoriteContactId.Integrated("high"),
            displayName = "High priority",
            endpoints = listOf(shortcutEndpoint("shared")),
        )
        val lowerPriority = FavoriteIntegratedContact(
            id = FavoriteContactId.Integrated("low"),
            displayName = "Lower priority",
            endpoints = listOf(shortcutEndpoint("shared")),
        )

        val matches = matchFavoriteConversations(
            conversations = listOf(conversation),
            orderedFavorites = listOf(highPriority, lowerPriority),
        )

        assertEquals(highPriority.id, matches.getValue(conversation.id).favoriteId)
        assertEquals(0, matches.getValue(conversation.id).favoriteRank)
    }

    private fun androidFavorite(lookupKey: String) = FavoriteAndroidContact(
        id = FavoriteContactId.Android(lookupKey),
        contactUri = "content://contacts/$lookupKey",
        displayName = lookupKey,
        photoUri = null,
        organization = null,
        phones = emptyList(),
    )

    private fun shortcutEndpoint(shortcutId: String) = IntegratedContactEndpoint.ConversationShortcut(
        packageName = "chat.app",
        shortcutId = shortcutId,
        userSerial = 0L,
        label = shortcutId,
    )

    private fun conversation(id: String, timestamp: Long): NotificationConversation {
        val notification = ConversationNotification(
            key = "key:$id",
            conversationId = id,
            conversationName = id,
            packageName = id.substringBefore(':'),
            appName = "App",
            postedAt = timestamp,
            isDismissible = true,
            messages = emptyList(),
            contentIntent = null,
            replyAction = null,
        )
        return NotificationConversation(
            id = id,
            name = id,
            notifications = listOf(notification),
            messages = emptyList(),
        )
    }
}
