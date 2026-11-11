package com.katoaapps.openminilaunch

import android.app.Notification
import com.katoaapps.openminilaunch.features.conversations.ConversationMessage
import com.katoaapps.openminilaunch.features.conversations.ConversationNotification
import com.katoaapps.openminilaunch.features.conversations.ConversationNotificationReconciler
import com.katoaapps.openminilaunch.features.conversations.ConversationNotificationStore
import com.katoaapps.openminilaunch.features.conversations.DemoConversationData
import com.katoaapps.openminilaunch.features.demo.DemoConversationState
import com.katoaapps.openminilaunch.features.demo.DemoScenario
import com.katoaapps.openminilaunch.features.conversations.NotificationConversation
import com.katoaapps.openminilaunch.features.conversations.conversationActivityAt
import com.katoaapps.openminilaunch.features.conversations.isConversationNotification
import com.katoaapps.openminilaunch.features.conversations.isDismissibleNotification
import com.katoaapps.openminilaunch.features.conversations.latestConversationActivityAt
import com.katoaapps.openminilaunch.features.conversations.resolvesConversationNotification
import com.katoaapps.openminilaunch.features.conversations.shouldShowConversationAttention
import com.katoaapps.openminilaunch.features.conversations.shouldExposeConversationAction
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ConversationNotificationStoreTest {
    @Test
    fun demoConversationFixturesFollowScenarioAndInboxState() {
        assertTrue(
            DemoConversationData.notifications(
                now = 1_000_000L,
                state = DemoConversationState.QUIET,
            ).isEmpty(),
        )
        val petMinkFavorite = DemoConversationData.notifications(
            now = 1_000_000L,
            scenario = DemoScenario.PET_MINK,
            state = DemoConversationState.ONE_FAVORITE,
        )
        assertEquals(listOf("May Wong"), petMinkFavorite.map { it.conversationName })
        val workday = DemoConversationData.notifications(
            now = 1_000_000L,
            scenario = DemoScenario.WORKDAY,
        )
        assertTrue(workday.isNotEmpty())
        assertTrue(workday.none { it.conversationName == "May Wong" })
    }

    @Test
    fun acceptsStructuredMessagesAndConversationCategories() {
        assertTrue(isConversationNotification(category = null, hasStructuredMessages = true))
        assertTrue(isConversationNotification(Notification.CATEGORY_MESSAGE, hasStructuredMessages = false))
        assertTrue(isConversationNotification(Notification.CATEGORY_EMAIL, hasStructuredMessages = false))
    }

    @Test
    fun rejectsUnrelatedNotificationCategories() {
        assertFalse(isConversationNotification(Notification.CATEGORY_PROMO, hasStructuredMessages = false))
        assertFalse(isConversationNotification(Notification.CATEGORY_REMINDER, hasStructuredMessages = false))
        assertFalse(isConversationNotification(category = null, hasStructuredMessages = false))
    }

    @Test
    fun dismissesOnlyClearableNotifications() {
        assertTrue(isDismissibleNotification(flags = 0))
        assertFalse(isDismissibleNotification(Notification.FLAG_ONGOING_EVENT))
        assertFalse(isDismissibleNotification(Notification.FLAG_NO_CLEAR))
        assertFalse(
            isDismissibleNotification(
                Notification.FLAG_ONGOING_EVENT or Notification.FLAG_NO_CLEAR,
            ),
        )
    }

    @Test
    fun exposesOnlyNamedActionsThatDoNotRequireInput() {
        assertTrue(
            shouldExposeConversationAction(
                title = "Mark as read",
                requiresRemoteInput = false,
            ),
        )
        assertFalse(
            shouldExposeConversationAction(
                title = "Reply",
                requiresRemoteInput = true,
            ),
        )
        assertTrue(
            shouldExposeConversationAction(
                title = "Reply",
                requiresRemoteInput = false,
            ),
        )
        assertFalse(
            shouldExposeConversationAction(
                title = "",
                requiresRemoteInput = false,
            ),
        )
    }

    @Test
    fun demoFeedKeepsMessagesSlackAndGmailThreadsEasyToRead() {
        val conversations = ConversationNotificationStore.groupConversations(
            sourceNotifications = DemoConversationData.notifications(now = 1_000_000L),
            replies = emptyList(),
        )

        assertEquals(6, conversations.size)
        val may = conversations.firstOrNull { it.name == "May Wong" }
        val maya = conversations.firstOrNull { it.name == "Maya Diaz" }
        val priya = conversations.firstOrNull { it.name == "Priya Shah" }
        val noah = conversations.firstOrNull { it.name == "Noah Williams" }
        assertNotNull(may)
        assertNotNull(maya)
        assertNotNull(priya)
        assertNotNull(noah)
        assertEquals(listOf(DemoConversationData.GOOGLE_MESSAGES_PACKAGE), may!!.sourcePackages)
        assertEquals(listOf(DemoConversationData.SLACK_PACKAGE), maya!!.sourcePackages)
        assertEquals(listOf(DemoConversationData.GMAIL_PACKAGE), priya!!.sourcePackages)
        assertEquals(listOf(DemoConversationData.GOOGLE_MESSAGES_PACKAGE), noah!!.sourcePackages)
        assertEquals("You can't have Minks as pets in LA", may.messages.single().text)
        assertEquals(1, maya.messages.size)
        assertTrue(maya.replyTarget?.isDemo == true)
    }

    @Test
    fun conversationAttentionTracksNewerActivityWithoutPersistingMessageData() {
        val conversations = ConversationNotificationStore.groupConversations(
            sourceNotifications = DemoConversationData.notifications(now = 1_000_000L),
            replies = emptyList(),
        )
        val latest = latestConversationActivityAt(conversations)

        assertNotNull(latest)
        assertTrue(shouldShowConversationAttention(latest, acknowledgedThrough = 0L))
        assertFalse(shouldShowConversationAttention(latest, acknowledgedThrough = latest!!))
        assertFalse(shouldShowConversationAttention(null, acknowledgedThrough = 0L))
    }

    @Test
    fun conversationAttentionUsesIncomingMessageTimeInsteadOfRepostTime() {
        val conversation = conversation(
            notification = notification(
                postedAt = 9_000L,
                messages = listOf(message(id = "incoming", timestamp = 200L)),
            ),
        )

        assertEquals(200L, conversationActivityAt(conversation))
        assertFalse(shouldShowConversationAttention(conversationActivityAt(conversation), 200L))
    }

    @Test
    fun outgoingRepliesDoNotCreateConversationAttention() {
        val conversation = conversation(
            notification = notification(
                postedAt = 9_000L,
                messages = listOf(
                    message(id = "incoming", timestamp = 200L),
                    message(id = "outgoing", timestamp = 8_000L, isOutgoing = true),
                ),
            ),
        )

        assertEquals(200L, conversationActivityAt(conversation))
    }

    @Test
    fun reconcilerRejectsOlderPayloadForReusedNotificationKey() {
        val reconciler = ConversationNotificationReconciler()
        val current = notification(
            postedAt = 500L,
            messages = listOf(message(id = "newer", timestamp = 400L)),
        )
        val staleRepost = notification(
            postedAt = 900L,
            messages = listOf(message(id = "older", timestamp = 200L)),
        )

        assertTrue(reconciler.shouldDisplay(current))
        assertFalse(reconciler.shouldDisplay(staleRepost))
    }

    @Test
    fun reconcilerKeepsSuppressedContentHiddenButAcceptsANewMessage() {
        val reconciler = ConversationNotificationReconciler()
        val dismissed = notification(
            postedAt = 500L,
            messages = listOf(message(id = "first", timestamp = 400L)),
        )
        val staleRepost = dismissed.copy(
            postedAt = 900L,
            messages = listOf(message(id = "older-history", timestamp = 200L)) + dismissed.messages,
        )
        val genuinelyNew = notification(
            postedAt = 1_000L,
            messages = dismissed.messages + message(id = "second", timestamp = 950L),
        )

        reconciler.suppress(dismissed)

        assertFalse(reconciler.shouldDisplay(staleRepost))
        assertTrue(reconciler.shouldDisplay(genuinelyNew))
    }

    @Test
    fun reconcilerAcceptsDifferentContentWhenProviderTimestampsMatch() {
        val reconciler = ConversationNotificationReconciler()
        val first = notification(
            postedAt = 500L,
            messages = listOf(message(id = "first", timestamp = 400L)),
        )
        val second = notification(
            postedAt = 600L,
            messages = listOf(message(id = "second", timestamp = 400L)),
        )

        reconciler.suppress(first)

        assertTrue(reconciler.shouldDisplay(second))
    }

    @Test
    fun onlyReadDeleteAndArchiveActionsResolveNotifications() {
        assertTrue(
            resolvesConversationNotification(Notification.Action.SEMANTIC_ACTION_MARK_AS_READ),
        )
        assertTrue(resolvesConversationNotification(Notification.Action.SEMANTIC_ACTION_DELETE))
        assertTrue(resolvesConversationNotification(Notification.Action.SEMANTIC_ACTION_ARCHIVE))
        assertFalse(resolvesConversationNotification(Notification.Action.SEMANTIC_ACTION_MUTE))
        assertFalse(resolvesConversationNotification(Notification.Action.SEMANTIC_ACTION_REPLY))
    }

    private fun conversation(notification: ConversationNotification) = NotificationConversation(
        id = notification.conversationId,
        name = notification.conversationName,
        notifications = listOf(notification),
        messages = notification.messages,
    )

    private fun notification(
        postedAt: Long,
        messages: List<ConversationMessage>,
    ) = ConversationNotification(
        key = "notification-key",
        conversationId = "conversation-id",
        conversationName = "Conversation",
        packageName = "message.app",
        appName = "Messages",
        postedAt = postedAt,
        isDismissible = true,
        messages = messages,
        contentIntent = null,
        replyAction = null,
    )

    private fun message(
        id: String,
        timestamp: Long,
        isOutgoing: Boolean = false,
    ) = ConversationMessage(
        id = id,
        conversationId = "conversation-id",
        notificationKey = "notification-key",
        packageName = "message.app",
        appName = "Messages",
        text = id,
        timestamp = timestamp,
        senderName = if (isOutgoing) null else "Sender",
        isOutgoing = isOutgoing,
    )
}
