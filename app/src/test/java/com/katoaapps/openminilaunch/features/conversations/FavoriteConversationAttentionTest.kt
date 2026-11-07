package com.katoaapps.openminilaunch.features.conversations

import com.katoaapps.openminilaunch.model.FavoriteAndroidContact
import com.katoaapps.openminilaunch.model.FavoriteContactId
import com.katoaapps.openminilaunch.model.FavoriteIntegratedContact
import com.katoaapps.openminilaunch.model.IntegratedContactEndpoint
import com.katoaapps.openminilaunch.model.ProviderAddressType
import org.junit.Assert.assertEquals
import org.junit.Test

class FavoriteConversationAttentionTest {
    @Test
    fun includesOnlyFavoritesAndKeepsFavoriteOrder() {
        val androidFavorite = androidFavorite("amy")
        val integratedFavorite = integratedFavorite("gary", "chat.app", "gary-thread")
        val conversations = listOf(
            conversation("mail.app:direct:newsletter", 300L),
            conversation("contact:amy", 200L),
            conversation("chat.app:direct:gary-thread", 100L),
        )

        val pending = pendingFavoriteConversationAttention(
            conversations = conversations,
            orderedFavorites = listOf(integratedFavorite, androidFavorite),
            defaultColorArgb = DEFAULT_COLOR,
            acknowledgedThrough = { 0L },
            colorFor = { id -> if (id == androidFavorite.id) ANDROID_COLOR else null },
        )

        assertEquals(
            listOf(integratedFavorite.id, androidFavorite.id),
            pending.map(FavoriteConversationAttention::favoriteId),
        )
        assertEquals(listOf(DEFAULT_COLOR, ANDROID_COLOR), pending.map { it.colorArgb })
    }

    @Test
    fun acknowledgedFavoriteStopsRequestingAttention() {
        val favorite = androidFavorite("amy")
        val conversation = conversation("contact:amy", 200L)

        val pending = pendingFavoriteConversationAttention(
            conversations = listOf(conversation),
            orderedFavorites = listOf(favorite),
            defaultColorArgb = DEFAULT_COLOR,
            acknowledgedThrough = { 200L },
            colorFor = { null },
        )

        assertEquals(emptyList<FavoriteConversationAttention>(), pending)
    }

    @Test
    fun matchesProviderAddressUsingOpaqueIdentityHint() {
        val favorite = providerAddressFavorite(
            localId = "amy-signal",
            packageName = "chat.app",
            address = "+1 (555) 123-4567",
        )
        val conversation = conversation(
            id = "chat.app:direct:opaque-thread",
            timestamp = 200L,
            identityHints = setOf(
                requireNotNull(
                    providerAddressIdentityHint(
                        ProviderAddressType.PHONE,
                        "1 555 123 4567",
                    ),
                ),
            ),
        )

        val pending = pendingFavoriteConversationAttention(
            conversations = listOf(conversation),
            orderedFavorites = listOf(favorite),
            defaultColorArgb = DEFAULT_COLOR,
            acknowledgedThrough = { 0L },
            colorFor = { null },
        )

        assertEquals(listOf(favorite.id), pending.map(FavoriteConversationAttention::favoriteId))
    }

    @Test
    fun matchesConversationShortcutEvenWhenAndroidResolvedTheConversationId() {
        val favorite = integratedFavorite("amy-signal", "chat.app", "amy-thread")
        val conversation = conversation(
            id = "contact:amy",
            timestamp = 200L,
            packageName = "chat.app",
            identityHints = setOf(
                requireNotNull(conversationShortcutIdentityHint("chat.app", "amy-thread")),
            ),
        )

        val pending = pendingFavoriteConversationAttention(
            conversations = listOf(conversation),
            orderedFavorites = listOf(favorite),
            defaultColorArgb = DEFAULT_COLOR,
            acknowledgedThrough = { 0L },
            colorFor = { null },
        )

        assertEquals(listOf(favorite.id), pending.map(FavoriteConversationAttention::favoriteId))
    }

    @Test
    fun favoriteOrderClaimsConversationOnlyOnceWhenIdentitiesOverlap() {
        val integratedFavorite = providerAddressFavorite(
            localId = "amy-signal",
            packageName = "chat.app",
            address = "15551234567",
        )
        val androidFavorite = androidFavorite("amy")
        val conversation = conversation(
            id = "contact:amy",
            timestamp = 200L,
            packageName = "chat.app",
            identityHints = setOf(
                requireNotNull(
                    providerAddressIdentityHint(
                        ProviderAddressType.PHONE,
                        "+1 (555) 123-4567",
                    ),
                ),
            ),
        )

        val pending = pendingFavoriteConversationAttention(
            conversations = listOf(conversation),
            orderedFavorites = listOf(integratedFavorite, androidFavorite),
            defaultColorArgb = DEFAULT_COLOR,
            acknowledgedThrough = { 0L },
            colorFor = { null },
        )

        assertEquals(
            listOf(integratedFavorite.id),
            pending.map(FavoriteConversationAttention::favoriteId),
        )
    }

    private fun androidFavorite(lookupKey: String) = FavoriteAndroidContact(
        id = FavoriteContactId.Android(lookupKey),
        contactUri = "content://contacts/$lookupKey",
        displayName = lookupKey,
        photoUri = null,
        organization = null,
        phones = emptyList(),
    )

    private fun integratedFavorite(
        localId: String,
        packageName: String,
        shortcutId: String,
    ) = FavoriteIntegratedContact(
        id = FavoriteContactId.Integrated(localId),
        displayName = localId,
        endpoints = listOf(
            IntegratedContactEndpoint.ConversationShortcut(
                packageName = packageName,
                shortcutId = shortcutId,
                userSerial = 0L,
                label = localId,
            ),
        ),
    )

    private fun providerAddressFavorite(
        localId: String,
        packageName: String,
        address: String,
    ) = FavoriteIntegratedContact(
        id = FavoriteContactId.Integrated(localId),
        displayName = localId,
        endpoints = listOf(
            IntegratedContactEndpoint.ProviderAddress(
                providerId = "signal",
                packageName = packageName,
                addressType = ProviderAddressType.PHONE,
                address = address,
            ),
        ),
    )

    private fun conversation(
        id: String,
        timestamp: Long,
        packageName: String = id.substringBefore(':'),
        identityHints: Set<String> = emptySet(),
    ): NotificationConversation {
        val notification = ConversationNotification(
            key = "key:$id",
            conversationId = id,
            conversationName = id,
            packageName = packageName,
            appName = "App",
            postedAt = timestamp,
            isDismissible = true,
            messages = listOf(
                ConversationMessage(
                    id = "message:$id",
                    conversationId = id,
                    notificationKey = "key:$id",
                    packageName = id.substringBefore(':'),
                    appName = "App",
                    text = "hello",
                    timestamp = timestamp,
                    senderName = id,
                    isOutgoing = false,
                ),
            ),
            contentIntent = null,
            replyAction = null,
            identityHints = identityHints,
        )
        return NotificationConversation(
            id = id,
            name = id,
            notifications = listOf(notification),
            messages = notification.messages,
        )
    }

    private companion object {
        const val DEFAULT_COLOR = 0xFF602C00.toInt()
        const val ANDROID_COLOR = 0xFF005BBB.toInt()
    }
}
