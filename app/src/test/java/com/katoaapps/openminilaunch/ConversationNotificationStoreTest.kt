package com.katoaapps.openminilaunch

import android.app.Notification
import com.katoaapps.openminilaunch.features.conversations.ConversationNotificationStore
import com.katoaapps.openminilaunch.features.conversations.DemoConversationData
import com.katoaapps.openminilaunch.features.conversations.isConversationNotification
import com.katoaapps.openminilaunch.features.conversations.isDismissibleNotification
import com.katoaapps.openminilaunch.features.conversations.shouldExposeConversationAction
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ConversationNotificationStoreTest {
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
}
