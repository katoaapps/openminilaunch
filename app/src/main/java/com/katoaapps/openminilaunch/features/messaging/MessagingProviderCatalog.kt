package com.katoaapps.openminilaunch.features.messaging

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.katoaapps.openminilaunch.R

/** Describes how far Mink can carry a Magic Box message into a provider. */
internal enum class MessagingSupportTier {
    CONTACT_AND_DRAFT,
    RECIPIENT_IN_APP,
    CONDITIONAL,
}

/** Decides what pressing Send does before Android or a provider is invoked. */
internal enum class MessagingSendRoute {
    DIRECT_SMS,
    PREFERRED_DRAFT,
    PROVIDER_PICKER,
}

internal fun messagingSendRoute(
    sendAutomatically: Boolean,
    preferredPackage: String?,
): MessagingSendRoute = when {
    !sendAutomatically -> MessagingSendRoute.PROVIDER_PICKER
    preferredPackage.isNullOrBlank() -> MessagingSendRoute.DIRECT_SMS
    else -> MessagingSendRoute.PREFERRED_DRAFT
}

/** The Android or provider-owned contract used to prepare a message draft. */
internal enum class MessagingDraftKind {
    WHATSAPP,
    TELEGRAM,
    LINE,
    SMS_URI,
    GENERIC_SHARE,
}

/**
 * Static provider metadata used even when an app is not installed.
 *
 * Installed apps still use their PackageManager label and icon. bundledIconRes is only the
 * provider-owned fallback that a future picker may show for an unavailable integration.
 */
internal data class MessagingDraftProvider(
    val id: String,
    @param:StringRes val labelRes: Int,
    val packageName: String,
    val alternatePackageNames: List<String> = emptyList(),
    val kind: MessagingDraftKind,
    val supportTier: MessagingSupportTier,
    @param:DrawableRes val bundledIconRes: Int? = null,
    val storeUrl: String,
    val documentationUrl: String,
)

internal val MessagingDraftProvider.packageNames: List<String>
    get() = listOf(packageName) + alternatePackageNames

/** Resolved provider state used by both Settings and the one-time send chooser. */
internal data class MessagingProviderOption(
    val id: String,
    val label: String,
    val preferencePackageName: String?,
    val installedPackageName: String?,
    val supportTier: MessagingSupportTier,
    @param:DrawableRes val bundledIconRes: Int?,
    val installed: Boolean,
    val selectable: Boolean,
    val systemDefault: Boolean = false,
)

/**
 * Curated integrations are deliberately package-based. Activities are resolved at launch time
 * because providers can rename exported activities between releases.
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
            id = "signal",
            labelRes = R.string.provider_signal,
            packageName = "org.thoughtcrime.securesms",
            bundledIconRes = R.drawable.messaging_provider_signal,
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
        documentationUrl: String,
    ) = MessagingDraftProvider(
        id = id,
        labelRes = labelRes,
        packageName = packageName,
        kind = MessagingDraftKind.GENERIC_SHARE,
        supportTier = MessagingSupportTier.RECIPIENT_IN_APP,
        bundledIconRes = bundledIconRes,
        storeUrl = "https://play.google.com/store/apps/details?id=$packageName",
        documentationUrl = documentationUrl,
    )
}
