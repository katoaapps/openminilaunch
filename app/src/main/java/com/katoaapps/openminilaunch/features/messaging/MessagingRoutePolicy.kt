package com.katoaapps.openminilaunch.features.messaging

import com.katoaapps.openminilaunch.model.CommunicationRecipient
import com.katoaapps.openminilaunch.model.looksLikePhoneRecipient

/**
 * "Automatically" skips Mink's provider picker. Only System Messages can send in the
 * background; every other integrated provider still opens its own draft composer.
 */
internal fun messagingSendRoute(
    sendAutomatically: Boolean,
    preferredPackage: String?,
): MessagingSendRoute = when {
    !sendAutomatically -> MessagingSendRoute.PROVIDER_PICKER
    preferredPackage.isNullOrBlank() -> MessagingSendRoute.DIRECT_SMS
    else -> MessagingSendRoute.PREFERRED_DRAFT
}

/** The package whose icon can represent an automatically routed message draft. */
internal fun automaticMessagingPackage(
    sendAutomatically: Boolean,
    preferredPackage: String?,
    defaultMessagingPackage: String?,
): String? = when {
    !sendAutomatically -> null
    !preferredPackage.isNullOrBlank() -> preferredPackage
    else -> defaultMessagingPackage
}

/** A selected System Messages conversation preserves the user's direct-SMS preference. */
internal fun recentConversationSendRoute(
    selectedConversationPackage: String?,
    defaultSmsPackage: String?,
    sendAutomatically: Boolean,
    preferredPackage: String?,
    fallbackRoute: MessagingSendRoute,
): MessagingSendRoute = when {
    selectedConversationPackage == null -> fallbackRoute
    selectedConversationPackage == defaultSmsPackage &&
        sendAutomatically &&
        preferredPackage == null -> MessagingSendRoute.DIRECT_SMS
    else -> MessagingSendRoute.PREFERRED_DRAFT
}

/** Usernames always stay provider-owned; phone-shaped entries retain normal send behavior. */
internal fun resolvedMessageSendRoute(
    recipient: CommunicationRecipient,
    selectedConversationPackage: String?,
    defaultSmsPackage: String?,
    sendAutomatically: Boolean,
    preferredPackage: String?,
    fallbackRoute: MessagingSendRoute,
): MessagingSendRoute {
    if (recipient.userEntered && !recipient.address.looksLikePhoneRecipient()) {
        return MessagingSendRoute.PREFERRED_DRAFT
    }
    return recentConversationSendRoute(
        selectedConversationPackage = selectedConversationPackage,
        defaultSmsPackage = defaultSmsPackage,
        sendAutomatically = sendAutomatically,
        preferredPackage = preferredPackage,
        fallbackRoute = fallbackRoute,
    )
}

/** Maps settings saved by older releases into the current two-control messaging model. */
internal fun restoredAutomaticMessageSend(saved: Boolean?, legacyMode: String?): Boolean =
    saved ?: when (legacyMode) {
        "DIRECT_SMS", "PREFERRED_APP", "DEFAULT_MESSENGER", "MESSAGING_APP" -> true
        else -> false
    }

internal fun defaultMessageDraftResult(
    integratedPackage: String?,
    opened: Boolean,
): PreferredMessageDraftResult = when {
    !opened -> PreferredMessageDraftResult.FAILED
    integratedPackage.isNullOrBlank() -> PreferredMessageDraftResult.OPENED
    else -> PreferredMessageDraftResult.FALLBACK_OPENED
}
