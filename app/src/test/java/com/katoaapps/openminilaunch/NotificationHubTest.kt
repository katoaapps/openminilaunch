package com.katoaapps.openminilaunch

import com.katoaapps.openminilaunch.features.conversations.*

import android.app.Notification
import org.junit.Assert.assertFalse
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
}
