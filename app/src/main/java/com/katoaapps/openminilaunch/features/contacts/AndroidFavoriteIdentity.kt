package com.katoaapps.openminilaunch.features.contacts

internal enum class AndroidFavoriteIdentityKind { BACKUP_ID, SOURCE_ID }

/** Provider metadata used only in memory to derive a backup-scoped opaque token. */
internal data class AndroidFavoriteIdentityCandidate(
    val kind: AndroidFavoriteIdentityKind,
    val accountType: String,
    val dataSet: String?,
    val value: String,
)

internal fun isSafePortableContactCandidate(
    candidate: AndroidFavoriteIdentityCandidate,
): Boolean =
    candidate.accountType == candidate.accountType.trim() &&
        candidate.dataSet?.let { it == it.trim() } != false &&
        candidate.value == candidate.value.trim() &&
        isSafePortableContactMetadata(candidate.accountType) &&
        candidate.dataSet?.let(::isSafePortableContactMetadata) != false &&
        isSafePortableContactIdentifier(candidate.value)

/**
 * Refuses identifiers that resemble direct contact information. A provider identifier is still
 * pseudonymous metadata, so accepted values are tokenized before they enter a readable backup.
 */
internal fun isSafePortableContactIdentifier(value: String): Boolean {
    val candidate = value.trim()
    if (candidate.length !in MIN_IDENTIFIER_LENGTH..MAX_IDENTIFIER_LENGTH) return false
    if (candidate.any(Char::isISOControl)) return false
    if ('@' in candidate || "://" in candidate) return false
    if (candidate.matches(PHONE_LIKE_IDENTIFIER)) return false
    return true
}

private fun isSafePortableContactMetadata(value: String): Boolean =
    value.length in 1..MAX_METADATA_LENGTH &&
        value.none(Char::isISOControl) &&
        '@' !in value &&
        "://" !in value &&
        !value.matches(PHONE_LIKE_IDENTIFIER)

private const val MIN_IDENTIFIER_LENGTH = 8
private const val MAX_IDENTIFIER_LENGTH = 512
private const val MAX_METADATA_LENGTH = 512
private val PHONE_LIKE_IDENTIFIER = Regex("^\\+?[0-9 ()\\-.]+$")
