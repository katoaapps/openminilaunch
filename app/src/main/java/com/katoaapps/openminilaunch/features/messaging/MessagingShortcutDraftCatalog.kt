package com.katoaapps.openminilaunch.features.messaging

import android.content.Intent

/**
 * Direct Share contracts verified against the provider APKs recorded in the integration spike.
 *
 * Android does not expose a conversation shortcut's underlying intent to launchers. Direct Share
 * instead routes an ACTION_SEND to the provider's declared share-target activity and identifies the
 * chosen conversation with EXTRA_SHORTCUT_ID. Each handoff is still resolved at runtime, so a
 * provider update that removes or renames an activity safely falls back to opening the shortcut.
 */
internal object MessagingShortcutDraftCatalog {
    private val routes = listOf(
        route(
            packageName = "com.beeper.android",
            targetActivity = "com.beeper.chat.booper.MainActivity",
            shortcutCategory = "com.beeper.android.SHORTCUT_SHARE",
        ),
        route(
            packageName = "im.molly.app",
            targetActivity = "org.thoughtcrime.securesms.sharing.v2.ShareActivity",
            shortcutCategory = SIGNAL_SHARE_CATEGORY,
        ),
        route(
            packageName = "org.thoughtcrime.securesms",
            targetActivity = "org.thoughtcrime.securesms.sharing.v2.ShareActivity",
            shortcutCategory = SIGNAL_SHARE_CATEGORY,
        ),
        route(
            packageName = "jp.naver.line.android",
            targetActivity = "com.linecorp.line.share.common.view.FullPickerLaunchActivity",
            shortcutCategory = "jp.naver.line.android.service.share.CHAT_SHARABLE_TYPE_CATEGORY",
        ),
        route(
            packageName = "com.kakao.talk",
            targetActivity = "com.kakao.talk.activity.RecentExcludeIntentFilterActivity",
            shortcutCategory = Intent.CATEGORY_DEFAULT,
        ),
        route(
            packageName = "com.viber.voip",
            targetActivity = "com.viber.voip.WelcomeShareActivity",
            shortcutCategory = "com.viber.voip.category.IMAGE_SHARE_TARGET",
        ),
        route(
            packageName = "com.Slack",
            targetActivity = "slack.app.ui.ShareReceiverActivity",
            shortcutCategory = "slack.services.shareshortcuts.SHARE_SHORTCUT_TARGET",
        ),
        route(
            packageName = "com.microsoft.teams",
            targetActivity = "com.microsoft.skype.teams.views.activities.SplashActivity",
            shortcutCategory = "com.microsoft.skype.teams.views.activities.SHARE_TARGET",
        ),
        route(
            packageName = "com.discord",
            targetActivity = "com.discord.share.ShareActivity",
            shortcutCategory = "com.discord.intent.category.DIRECT_SHARE_TARGET",
        ),
        route(
            packageName = "com.groupme.android",
            targetActivity = "com.groupme.android.sharing.SharingActivity",
            shortcutCategory = "com.groupme.android.sharing.category.TEXT_SHARE_TARGET",
        ),
        route(
            packageName = "com.zing.zalo",
            targetActivity = "com.zing.zalo.ui.TempShareViaActivity",
            shortcutCategory = "com.zing.zalo.sharingShortcuts.category.SHARE_TARGET",
        ),
        // Messenger strips its share-target category name from release resources. Its exported
        // handler and EXTRA_SHORTCUT_ID behavior were verified directly, so the component check is
        // the runtime guard for this route.
        MessagingShortcutDraftRoute(
            packageName = "com.facebook.orca",
            targetActivity = "com.facebook.messenger.intents.ShareIntentHandler",
            shortcutCategories = emptySet(),
        ),
    )

    fun routeFor(
        packageName: String,
        shortcutCategories: Set<String>,
    ): MessagingShortcutDraftRoute? = routes.firstOrNull { route ->
        route.packageName == packageName && (
            route.shortcutCategories.isEmpty() ||
                shortcutCategories.any(route.shortcutCategories::contains)
            )
    }

    fun routeForPackage(packageName: String): MessagingShortcutDraftRoute? =
        routes.firstOrNull { it.packageName == packageName }

    private fun route(
        packageName: String,
        targetActivity: String,
        shortcutCategory: String,
    ) = MessagingShortcutDraftRoute(
        packageName = packageName,
        targetActivity = targetActivity,
        shortcutCategories = setOf(shortcutCategory),
    )

    private const val SIGNAL_SHARE_CATEGORY =
        "org.thoughtcrime.securesms.sharing.CATEGORY_SHARE_TARGET"
}

internal data class MessagingShortcutDraftRoute(
    val packageName: String,
    val targetActivity: String,
    val shortcutCategories: Set<String>,
)
