package com.katoaapps.openminilaunch.features.profile

data class ProfileCard(
    val fullName: String = "",
    val organization: String = "",
    val jobTitle: String = "",
    val note: String = "",
    val selectedLinkIds: List<String> = emptyList(),
    val hasPortrait: Boolean = false,
)

data class ProfileLink(
    val id: String,
    val type: ProfileLinkType,
    val label: String,
    val value: String,
)

internal fun ProfileCard.resolveSelectedLinks(links: List<ProfileLink>): List<ProfileLink> =
    selectedLinkIds.mapNotNull { id -> links.firstOrNull { it.id == id } }

enum class ProfileLinkType {
    PHONE,
    EMAIL,
    WEBSITE,
    INSTAGRAM,
    LINKEDIN,
    X,
    FACEBOOK,
    YOUTUBE,
    TIKTOK,
    GITHUB,
    WHATSAPP,
    SIGNAL,
    VENMO,
    PAYPAL,
    CASH_APP,
    CUSTOM_URL,
}

sealed interface ProfileState {
    data object Empty : ProfileState
    data class Ready(val card: ProfileCard, val links: List<ProfileLink>) : ProfileState
    data class Invalid(val reason: String?) : ProfileState
    data class Unreadable(val reason: String?) : ProfileState
}
