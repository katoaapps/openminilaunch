package com.katoaapps.openminilaunch.features.conversations

import com.katoaapps.openminilaunch.features.demo.DemoConversationState
import com.katoaapps.openminilaunch.features.demo.DemoScenario

/** Fictional conversation data shown only while the hidden demo mode is enabled. */
internal object DemoConversationData {
    const val SLACK_PACKAGE = "com.Slack"
    const val GMAIL_PACKAGE = "com.google.android.gm"
    const val GOOGLE_MESSAGES_PACKAGE = "com.google.android.apps.messaging"

    fun notifications(
        now: Long = System.currentTimeMillis(),
        scenario: DemoScenario = DemoScenario.PRODUCT_TOUR,
        state: DemoConversationState = DemoConversationState.ACTIVE_INBOX,
    ): List<ConversationNotification> {
        val mayaConversation = "contact:demo-maya-diaz"
        val averyConversation = "contact:demo-avery-brooks"
        val karaConversation = "contact:demo-kara-ellis"
        val priyaConversation = "contact:demo-priya-shah"
        val noahConversation = "contact:demo-noah-williams"
        val mayConversation = "contact:demo-may-wong"

        val allNotifications = listOf(
            notification(
                key = "demo-messages-may",
                conversationId = mayConversation,
                conversationName = "May Wong",
                packageName = GOOGLE_MESSAGES_PACKAGE,
                appName = "Messages",
                postedAt = now - minutes(2),
                messages = listOf(
                    message(
                        id = "demo-messages-may-1",
                        conversationId = mayConversation,
                        notificationKey = "demo-messages-may",
                        packageName = GOOGLE_MESSAGES_PACKAGE,
                        appName = "Messages",
                        text = "You can't have Minks as pets in LA",
                        timestamp = now - minutes(2),
                        senderName = "May Wong",
                    ),
                ),
            ),
            notification(
                key = "demo-slack-maya-diaz",
                conversationId = mayaConversation,
                conversationName = "Maya Diaz",
                packageName = SLACK_PACKAGE,
                appName = "Slack",
                postedAt = now - minutes(4),
                messages = listOf(
                    message(
                        id = "demo-slack-maya-diaz-1",
                        conversationId = mayaConversation,
                        notificationKey = "demo-slack-maya-diaz",
                        packageName = SLACK_PACKAGE,
                        appName = "Slack",
                        text = "I pushed the revised forecast. Can you check the hosting line before 2?",
                        timestamp = now - minutes(4),
                        senderName = "Maya Diaz",
                    ),
                ),
            ),
            notification(
                key = "demo-slack-avery",
                conversationId = averyConversation,
                conversationName = "Avery Brooks",
                packageName = SLACK_PACKAGE,
                appName = "Slack",
                postedAt = now - minutes(14),
                messages = listOf(
                    message(
                        id = "demo-slack-avery-1",
                        conversationId = averyConversation,
                        notificationKey = "demo-slack-avery",
                        packageName = SLACK_PACKAGE,
                        appName = "Slack",
                        text = "Coffee next Friday still work for you?",
                        timestamp = now - minutes(14),
                        senderName = "Avery Brooks",
                    ),
                ),
            ),
            notification(
                key = "demo-messages-noah",
                conversationId = noahConversation,
                conversationName = "Noah Williams",
                packageName = GOOGLE_MESSAGES_PACKAGE,
                appName = "Messages",
                postedAt = now - minutes(22),
                messages = listOf(
                    message(
                        id = "demo-messages-noah-1",
                        conversationId = noahConversation,
                        notificationKey = "demo-messages-noah",
                        packageName = GOOGLE_MESSAGES_PACKAGE,
                        appName = "Messages",
                        text = "Still good for dinner at 7?",
                        timestamp = now - minutes(22),
                        senderName = "Noah Williams",
                    ),
                ),
            ),
            notification(
                key = "demo-gmail-kara",
                conversationId = karaConversation,
                conversationName = "Kara Ellis",
                packageName = GMAIL_PACKAGE,
                appName = "Gmail",
                postedAt = now - minutes(31),
                messages = listOf(
                    message(
                        id = "demo-gmail-kara-1",
                        conversationId = karaConversation,
                        notificationKey = "demo-gmail-kara",
                        packageName = GMAIL_PACKAGE,
                        appName = "Gmail",
                        text = "Your train confirmation is in the family folder.",
                        timestamp = now - minutes(31),
                        senderName = "Kara Ellis",
                    ),
                ),
            ),
            notification(
                key = "demo-gmail-priya",
                conversationId = priyaConversation,
                conversationName = "Priya Shah",
                packageName = GMAIL_PACKAGE,
                appName = "Gmail",
                postedAt = now - minutes(46),
                messages = listOf(
                    message(
                        id = "demo-gmail-priya-1",
                        conversationId = priyaConversation,
                        notificationKey = "demo-gmail-priya",
                        packageName = GMAIL_PACKAGE,
                        appName = "Gmail",
                        text = "The launch checklist is updated. I left two questions in the shared sheet.",
                        timestamp = now - minutes(46),
                        senderName = "Priya Shah",
                    ),
                ),
            ),
        )
        val scenarioNotifications = when (scenario) {
            DemoScenario.PRODUCT_TOUR -> allNotifications
            DemoScenario.PET_MINK -> allNotifications.filter { notification ->
                notification.conversationName in setOf("May Wong", "Noah Williams", "Kara Ellis")
            }
            DemoScenario.WORKDAY -> allNotifications.filter { notification ->
                notification.conversationName in setOf(
                    "Maya Diaz",
                    "Avery Brooks",
                    "Kara Ellis",
                    "Priya Shah",
                )
            }
        }
        return when (state) {
            DemoConversationState.QUIET -> emptyList()
            DemoConversationState.ONE_FAVORITE -> listOfNotNull(
                scenarioNotifications.firstOrNull { notification ->
                    notification.conversationName == when (scenario) {
                        DemoScenario.PET_MINK -> "May Wong"
                        DemoScenario.PRODUCT_TOUR,
                        DemoScenario.WORKDAY,
                        -> "Maya Diaz"
                    }
                },
            )
            DemoConversationState.ACTIVE_INBOX -> scenarioNotifications
        }
    }

    fun isDemoPackage(packageName: String): Boolean =
        packageName == SLACK_PACKAGE ||
            packageName == GMAIL_PACKAGE ||
            packageName == GOOGLE_MESSAGES_PACKAGE

    private fun notification(
        key: String,
        conversationId: String,
        conversationName: String,
        packageName: String,
        appName: String,
        postedAt: Long,
        messages: List<ConversationMessage>,
    ) = ConversationNotification(
        key = key,
        conversationId = conversationId,
        conversationName = conversationName,
        packageName = packageName,
        appName = appName,
        postedAt = postedAt,
        isDismissible = false,
        messages = messages,
        contentIntent = null,
        replyAction = null,
        isDemo = true,
    )

    private fun message(
        id: String,
        conversationId: String,
        notificationKey: String,
        packageName: String,
        appName: String,
        text: String,
        timestamp: Long,
        senderName: String,
    ) = ConversationMessage(
        id = id,
        conversationId = conversationId,
        notificationKey = notificationKey,
        packageName = packageName,
        appName = appName,
        text = text,
        timestamp = timestamp,
        senderName = senderName,
        isOutgoing = false,
    )

    private fun minutes(value: Int): Long = value * 60_000L
}
