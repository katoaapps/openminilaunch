package com.katoaapps.openminilaunch.features.messaging

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.katoaapps.openminilaunch.R

/**
 * Curated integrations are deliberately package-based. Activities are resolved at launch time
 * because providers can rename exported activities between releases. Every primary and alternate
 * package listed here must also be declared in AndroidManifest.xml's queries block so Android can
 * report whether it is installed. New providers also need a bundled icon, attribution in NOTICE,
 * and an entry in docs/messaging-provider-assets.md.
 */
internal object MessagingProviderCatalog {
    const val SYSTEM_DEFAULT_PROVIDER_ID = "system_sms"

    val providers = listOf(
        MessagingDraftProvider(
            id = "whatsapp",
            labelRes = R.string.provider_whatsapp,
            packageName = "com.whatsapp",
            kind = MessagingDraftKind.WHATSAPP,
            supportTier = MessagingSupportTier.CONTACT_AND_DRAFT,
            bundledIconRes = R.drawable.messaging_provider_whatsapp,
            storeUrl = "https://play.google.com/store/apps/details?id=com.whatsapp",
            documentationUrl = "https://faq.whatsapp.com/5913398998672934/",
        ),
        MessagingDraftProvider(
            id = "whatsapp_business",
            labelRes = R.string.provider_whatsapp_business,
            packageName = "com.whatsapp.w4b",
            kind = MessagingDraftKind.WHATSAPP,
            supportTier = MessagingSupportTier.CONTACT_AND_DRAFT,
            bundledIconRes = R.drawable.messaging_provider_whatsapp_business,
            storeUrl = "https://play.google.com/store/apps/details?id=com.whatsapp.w4b",
            documentationUrl = "https://faq.whatsapp.com/5913398998672934/",
        ),
        MessagingDraftProvider(
            id = "telegram",
            labelRes = R.string.provider_telegram,
            packageName = "org.telegram.messenger",
            alternatePackageNames = listOf("org.telegram.messenger.web"),
            kind = MessagingDraftKind.TELEGRAM,
            supportTier = MessagingSupportTier.CONTACT_AND_DRAFT,
            bundledIconRes = R.drawable.messaging_provider_telegram,
            storeUrl = "https://play.google.com/store/apps/details?id=org.telegram.messenger",
            documentationUrl = "https://core.telegram.org/api/links#phone-number-links",
        ),
        MessagingDraftProvider(
            id = "line",
            labelRes = R.string.provider_line,
            packageName = "jp.naver.line.android",
            kind = MessagingDraftKind.LINE,
            supportTier = MessagingSupportTier.RECIPIENT_IN_APP,
            bundledIconRes = R.drawable.messaging_provider_line,
            storeUrl = "https://play.google.com/store/apps/details?id=jp.naver.line.android",
            documentationUrl = "https://developers.line.biz/en/docs/messaging-api/using-line-url-scheme/#sending-text-messages",
        ),
        genericShareProvider(
            id = "beeper",
            labelRes = R.string.provider_beeper,
            packageName = "com.beeper.android",
            bundledIconRes = R.drawable.messaging_provider_beeper,
            documentationUrl = "https://help.beeper.com/en_US/beeper-plus/merge-chats-getting-started-guide",
        ),
        MessagingDraftProvider(
            id = "molly",
            labelRes = R.string.provider_molly,
            packageName = "im.molly.app",
            kind = MessagingDraftKind.SIGNAL_CONTACT,
            supportTier = MessagingSupportTier.CONTACT_AND_DRAFT,
            bundledIconRes = R.drawable.messaging_provider_molly,
            storeUrl = "https://molly.im/",
            documentationUrl = "https://github.com/mollyim/mollyim-android/blob/main/app/src/main/java/org/thoughtcrime/securesms/SystemContactsEntrypointViewModel.kt",
        ),
        MessagingDraftProvider(
            id = "signal",
            labelRes = R.string.provider_signal,
            packageName = "org.thoughtcrime.securesms",
            kind = MessagingDraftKind.SIGNAL_CONTACT,
            supportTier = MessagingSupportTier.CONTACT_AND_DRAFT,
            bundledIconRes = R.drawable.messaging_provider_signal,
            storeUrl = "https://play.google.com/store/apps/details?id=org.thoughtcrime.securesms",
            documentationUrl = "https://github.com/signalapp/Signal-Android/blob/main/app/src/main/AndroidManifest.xml",
        ),
        genericShareProvider(
            id = "kakaotalk",
            labelRes = R.string.provider_kakaotalk,
            packageName = "com.kakao.talk",
            bundledIconRes = R.drawable.messaging_provider_kakaotalk,
            documentationUrl = "https://developers.kakao.com/docs/en/kakaotalk-share/android-link",
        ),
        genericShareProvider(
            id = "wechat",
            labelRes = R.string.provider_wechat,
            packageName = "com.tencent.mm",
            bundledIconRes = R.drawable.messaging_provider_wechat,
            documentationUrl = "https://javadoc.io/doc/com.tencent.mm.opensdk/wechat-sdk-android-without-mta/latest/com/tencent/mm/opensdk/modelmsg/SendMessageToWX.Req.html",
        ),
        genericShareProvider(
            id = "viber",
            labelRes = R.string.provider_viber,
            packageName = "com.viber.voip",
            bundledIconRes = R.drawable.messaging_provider_viber,
            documentationUrl = "https://developers.viber.com/docs/tools/deep-links/",
        ),
        genericShareProvider(
            id = "messenger",
            labelRes = R.string.provider_messenger,
            packageName = "com.facebook.orca",
            bundledIconRes = R.drawable.messaging_provider_messenger,
            documentationUrl = "https://developer.android.com/training/sharing/send",
        ),
        genericShareProvider(
            id = "slack",
            labelRes = R.string.provider_slack,
            packageName = "com.Slack",
            bundledIconRes = R.drawable.messaging_provider_slack,
            documentationUrl = "https://slack.com/release-notes/android",
        ),
        genericShareProvider(
            id = "teams",
            labelRes = R.string.provider_teams,
            packageName = "com.microsoft.teams",
            bundledIconRes = R.drawable.messaging_provider_teams,
            documentationUrl = "https://support.microsoft.com/teams",
        ),
        genericShareProvider(
            id = "discord",
            labelRes = R.string.provider_discord,
            packageName = "com.discord",
            bundledIconRes = R.drawable.messaging_provider_discord,
            documentationUrl = "https://support.discord.com/",
        ),
        genericShareProvider(
            id = "snapchat",
            labelRes = R.string.provider_snapchat,
            packageName = "com.snapchat.android",
            bundledIconRes = R.drawable.messaging_provider_snapchat,
            documentationUrl = "https://developers.snap.com/snap-kit/creative-kit/overview",
        ),
        genericShareProvider(
            id = "groupme",
            labelRes = R.string.provider_groupme,
            packageName = "com.groupme.android",
            bundledIconRes = R.drawable.messaging_provider_groupme,
            documentationUrl = "https://support.microsoft.com/groupme",
        ),
        genericShareProvider(
            id = "zalo",
            labelRes = R.string.provider_zalo,
            packageName = "com.zing.zalo",
            bundledIconRes = R.drawable.messaging_provider_zalo,
            documentationUrl = "https://developers.zalo.me/",
        ),
        MessagingDraftProvider(
            id = "textnow",
            labelRes = R.string.provider_textnow,
            packageName = "com.enflick.android.TextNow",
            kind = MessagingDraftKind.SMS_URI,
            supportTier = MessagingSupportTier.CONDITIONAL,
            bundledIconRes = R.drawable.messaging_provider_textnow,
            storeUrl = "https://play.google.com/store/apps/details?id=com.enflick.android.TextNow",
            documentationUrl = "https://help.textnow.com/",
        ),
        MessagingDraftProvider(
            id = "textfree",
            labelRes = R.string.provider_textfree,
            packageName = "com.pinger.textfree",
            kind = MessagingDraftKind.SMS_URI,
            supportTier = MessagingSupportTier.CONDITIONAL,
            bundledIconRes = R.drawable.messaging_provider_textfree,
            storeUrl = "https://play.google.com/store/apps/details?id=com.pinger.textfree",
            documentationUrl = "https://pinger.zendesk.com/",
        ),
    )

    fun providerForPackage(packageName: String?): MessagingDraftProvider? =
        providers.firstOrNull { packageName in it.packageNames }

    private fun genericShareProvider(
        id: String,
        @StringRes labelRes: Int,
        packageName: String,
        @DrawableRes bundledIconRes: Int,
        storeUrl: String = "https://play.google.com/store/apps/details?id=$packageName",
        documentationUrl: String,
    ) = MessagingDraftProvider(
        id = id,
        labelRes = labelRes,
        packageName = packageName,
        kind = MessagingDraftKind.GENERIC_SHARE,
        supportTier = MessagingSupportTier.RECIPIENT_IN_APP,
        bundledIconRes = bundledIconRes,
        storeUrl = storeUrl,
        documentationUrl = documentationUrl,
    )
}
