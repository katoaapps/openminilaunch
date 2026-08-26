package com.katoaapps.openminilaunch

import com.katoaapps.openminilaunch.features.conversations.*

import android.app.Notification
import org.junit.Assert.assertFalse
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class NotificationHubTest {
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
    fun demoFeedKeepsSlackAndGmailThreadsEasyToRead() {
        val conversations = NotificationHub.buildConversations(
            sourceNotifications = DemoConversationData.notifications(now = 1_000_000L),
            replies = emptyList(),
        )

        assertEquals(5, conversations.size)
        val maya = conversations.firstOrNull { it.name == "Maya Diaz" }
        val priya = conversations.firstOrNull { it.name == "Priya Shah" }
        val noah = conversations.firstOrNull { it.name == "Noah Williams" }
        assertNotNull(maya)
        assertNotNull(priya)
        assertNotNull(noah)
        assertEquals(listOf(DemoConversationData.SLACK_PACKAGE), maya!!.sourcePackages)
        assertEquals(listOf(DemoConversationData.GMAIL_PACKAGE), priya!!.sourcePackages)
        assertEquals(listOf(DemoConversationData.GOOGLE_MESSAGES_PACKAGE), noah!!.sourcePackages)
        assertEquals(1, maya.messages.size)
        assertTrue(maya.replyTarget?.isDemo == true)
    }
}
