package com.katoaapps.openminilaunch.features.contacts

import com.katoaapps.openminilaunch.model.IntegratedContactEndpoint
import com.katoaapps.openminilaunch.model.IntegratedContactCallEndpoint
import com.katoaapps.openminilaunch.model.LauncherShortcutTarget
import com.katoaapps.openminilaunch.model.ProviderAddressType
import com.katoaapps.openminilaunch.model.looksLikePhoneRecipient
import java.util.Locale

internal val IntegratedContactEndpoint.identity: String
    get() = when (this) {
        is IntegratedContactEndpoint.ConversationShortcut ->
            "$userSerial\u0000$packageName\u0000$shortcutId"
        is IntegratedContactEndpoint.ProviderAddress ->
            "address\u0000$providerId\u0000${addressType.name}\u0000" +
                address.normalizedProviderAddress(addressType)
    }

internal fun IntegratedContactEndpoint.isValid(): Boolean = when (this) {
    is IntegratedContactEndpoint.ConversationShortcut ->
        userSerial >= 0 && packageName.isNotBlank() && shortcutId.isNotBlank()
    is IntegratedContactEndpoint.ProviderAddress ->
        providerId.isNotBlank() && packageName.isNotBlank() &&
            address.cleanProviderAddress(addressType) == address
}

internal val IntegratedContactCallEndpoint.identity: String
    get() = when (this) {
        is IntegratedContactCallEndpoint.PhoneNumber ->
            "phone\u0000${packageName.orEmpty()}\u0000${number.normalizedCallNumber()}"
    }

internal fun IntegratedContactCallEndpoint.isValid(): Boolean = when (this) {
    is IntegratedContactCallEndpoint.PhoneNumber ->
        number.cleanCallNumber() == number &&
            (packageName == null || packageName.isNotBlank())
}

internal fun String.cleanCallNumber(): String? {
    val clean = trim()
    if (clean.length > FAVORITE_PROVIDER_ADDRESS_MAX_CHARS) return null
    return clean.takeIf(String::looksLikePhoneRecipient)
}

internal fun String.cleanProviderAddress(type: ProviderAddressType): String? {
    val clean = trim()
    if (clean.isBlank() || clean.length > FAVORITE_PROVIDER_ADDRESS_MAX_CHARS) return null
    return when (type) {
        ProviderAddressType.PHONE -> clean.takeIf(String::looksLikePhoneRecipient)
        ProviderAddressType.USERNAME -> clean.removePrefix("@").trim().takeIf { username ->
            username.isNotBlank() && username.none(Char::isWhitespace)
        }
    }
}

private fun String.normalizedProviderAddress(type: ProviderAddressType): String = when (type) {
    ProviderAddressType.PHONE -> {
        val clean = trim()
        (if (clean.startsWith("+")) "+" else "") + clean.filter(Char::isDigit)
    }
    ProviderAddressType.USERNAME -> trim().removePrefix("@").lowercase(Locale.ROOT)
}

private fun String.normalizedCallNumber(): String {
    val clean = trim()
    return (if (clean.startsWith("+")) "+" else "") + clean.filter(Char::isDigit)
}

internal fun LauncherShortcutTarget.toIntegratedEndpoint(): IntegratedContactEndpoint =
    IntegratedContactEndpoint.ConversationShortcut(
        packageName = packageName,
        shortcutId = shortcutId,
        userSerial = userSerial,
        label = label.takeIf(String::isNotBlank),
    )
