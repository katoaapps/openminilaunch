package com.katoaapps.openminilaunch.model

/** A phone number or provider username selected for one outgoing call or message. */
internal data class CommunicationRecipient(
    val address: String,
    val displayName: String,
    val detail: String,
    val userEntered: Boolean = false,
)

internal fun ContactResult.toCommunicationRecipient(): CommunicationRecipient =
    CommunicationRecipient(
        address = phone,
        displayName = name,
        detail = phoneLabel,
    )

internal fun userEnteredRecipient(identifier: String): CommunicationRecipient {
    val address = identifier.trim()
    require(address.isNotEmpty()) { "A recipient address cannot be blank" }
    return CommunicationRecipient(
        address = address,
        displayName = address,
        detail = "BETA",
        userEntered = true,
    )
}

/** Keeps usernames out of direct carrier APIs while allowing common phone-number formats. */
internal fun String.looksLikePhoneRecipient(): Boolean =
    any(Char::isDigit) && all { character ->
        character.isDigit() || character in "+-(). /"
    }
