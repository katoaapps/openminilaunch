package com.katoaapps.openminilaunch

import com.katoaapps.openminilaunch.features.messaging.uniqueContactForConversation
import com.katoaapps.openminilaunch.features.messaging.RecentConversationSelection
import com.katoaapps.openminilaunch.features.messaging.resolveRecentConversationSelection
import com.katoaapps.openminilaunch.model.ContactResult
import com.katoaapps.openminilaunch.model.LauncherShortcutTarget
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class RecentConversationRecipientMatcherTest {
    @Test fun exactUniqueNameCanBecomeAMessageDraftRecipient() {
        val sam = contact("Sam Rivera", "+1 202-555-0101")

        assertEquals(sam, uniqueContactForConversation("sam rivera", listOf(sam)))
    }

    @Test fun whitespaceAndUnicodePresentationDoNotPreventAnExactMatch() {
        val sam = contact("Sam   Rivera", "+1 202-555-0101")

        assertEquals(sam, uniqueContactForConversation("  Sam Rivera  ", listOf(sam)))
    }

    @Test fun multiplePhoneRowsAreLeftAmbiguous() {
        val mobile = contact("Sam Rivera", "+1 202-555-0101", "Mobile")
        val work = contact("Sam Rivera", "+1 202-555-0102", "Work")

        assertNull(uniqueContactForConversation("Sam Rivera", listOf(mobile, work)))
    }

    @Test fun partialNamesAndGroupChatsAreNotGuessed() {
        val sam = contact("Sam Rivera", "+1 202-555-0101")

        assertNull(uniqueContactForConversation("Sam", listOf(sam)))
        assertNull(uniqueContactForConversation("Sam and Alex", listOf(sam)))
    }

    @Test fun recipientCapablePersonalConversationSelectsDraftMode() {
        val sam = contact("Sam Rivera", "+1 202-555-0101")
        val selection = resolveRecentConversationSelection(
            shortcut = shortcut("Sam Rivera", "im.molly.app"),
            canSearchContacts = true,
            canDraftToShortcut = { false },
            canAddressDraft = { true },
            searchContacts = { listOf(sam) },
        )

        assertEquals(
            RecentConversationSelection.DraftRecipient(sam, "im.molly.app"),
            selection,
        )
    }

    @Test fun verifiedDirectShareConversationSelectsShortcutDraftMode() {
        val conversation = shortcut("Design Team", "com.Slack")

        assertEquals(
            RecentConversationSelection.ShortcutDraft(conversation),
            resolveRecentConversationSelection(
                shortcut = conversation,
                canSearchContacts = false,
                canDraftToShortcut = { true },
                canAddressDraft = { error("A shortcut draft does not need a phone route") },
                searchContacts = { error("A shortcut draft does not need contact access") },
            ),
        )
    }

    @Test fun bodyOnlyAndWorkConversationsRemainDirectOpenActions() {
        val bodyOnly = shortcut("Sam Rivera", "org.thoughtcrime.securesms")
        val work = shortcut("Sam Rivera", "im.molly.app", isWorkProfile = true)

        assertEquals(
            RecentConversationSelection.DirectOpen(bodyOnly),
            resolveRecentConversationSelection(
                shortcut = bodyOnly,
                canSearchContacts = true,
                canDraftToShortcut = { false },
                canAddressDraft = { false },
                searchContacts = { error("Body-only apps must not query contacts") },
            ),
        )
        assertEquals(
            RecentConversationSelection.DirectOpen(work),
            resolveRecentConversationSelection(
                shortcut = work,
                canSearchContacts = true,
                canDraftToShortcut = { true },
                canAddressDraft = { true },
                searchContacts = { error("Work shortcuts must not query personal contacts") },
            ),
        )
    }

    private fun contact(
        name: String,
        phone: String,
        phoneLabel: String = "Mobile",
    ) = ContactResult(
        contactUri = "content://contacts/${phone.filter(Char::isDigit)}",
        name = name,
        phone = phone,
        phoneLabel = phoneLabel,
    )

    private fun shortcut(
        label: String,
        packageName: String,
        isWorkProfile: Boolean = false,
    ) = LauncherShortcutTarget(
        label = label,
        packageName = packageName,
        shortcutId = "conversation",
        userSerial = if (isWorkProfile) 10 else 0,
        isWorkProfile = isWorkProfile,
    )
}
