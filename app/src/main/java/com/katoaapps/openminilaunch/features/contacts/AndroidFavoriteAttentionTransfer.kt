package com.katoaapps.openminilaunch.features.contacts

import java.io.ByteArrayOutputStream
import java.io.DataOutputStream
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

/** Opaque, export-scoped metadata used to reconcile Android favorite colors on another device. */
internal data class AndroidFavoriteAttentionTransfer(
    val scopeKey: String,
    val entries: List<AndroidFavoriteAttentionTransferEntry>,
)

internal data class AndroidFavoriteAttentionTransferEntry(
    val tokens: List<String>,
    val colorArgb: Int,
)

internal data class AndroidFavoriteAttentionResolution(
    val matches: List<AndroidFavoriteAttentionMatch>,
    val unresolvedEntries: List<AndroidFavoriteAttentionTransferEntry>,
)

internal data class AndroidFavoriteAttentionMatch(
    val lookupKey: String,
    val entry: AndroidFavoriteAttentionTransferEntry,
)

internal object AndroidFavoriteAttentionTransfers {
    fun create(
        colorsByLookupKey: Map<String, Int>,
        candidatesByLookupKey: Map<String, List<AndroidFavoriteIdentityCandidate>>,
        scopeKeyBytes: ByteArray = ByteArray(SCOPE_KEY_BYTES).also(SecureRandom()::nextBytes),
    ): AndroidFavoriteAttentionTransfer? {
        if (scopeKeyBytes.size != SCOPE_KEY_BYTES) return null
        val candidates = colorsByLookupKey.asSequence()
            .mapNotNull { (lookupKey, color) ->
                val tokens = candidatesByLookupKey[lookupKey]
                    .orEmpty()
                    .asSequence()
                    .filter(::isSafePortableContactCandidate)
                    .map { token(scopeKeyBytes, it) }
                    .distinct()
                    .sorted()
                    .take(MAX_TOKENS_PER_ENTRY)
                    .toList()
                tokens.takeIf { it.isNotEmpty() }?.let {
                    AndroidFavoriteAttentionTransferEntry(it, color or OPAQUE_ALPHA)
                }
            }
            .take(MAX_TRANSFER_ENTRIES)
            .toList()
        val ambiguousTokens = candidates
            .flatMap { it.tokens }
            .groupingBy { it }
            .eachCount()
            .filterValues { it > 1 }
            .keys
        val entries = candidates.mapNotNull { entry ->
            entry.copy(tokens = entry.tokens.filterNot(ambiguousTokens::contains))
                .takeIf { it.tokens.isNotEmpty() }
        }
        if (entries.isEmpty()) return null
        return AndroidFavoriteAttentionTransfer(
            scopeKey = URL_ENCODER.encodeToString(scopeKeyBytes),
            entries = entries,
        )
    }

    fun resolve(
        transfer: AndroidFavoriteAttentionTransfer,
        candidatesByLookupKey: Map<String, List<AndroidFavoriteIdentityCandidate>>,
    ): AndroidFavoriteAttentionResolution {
        val scopeKey = decodeScopeKey(transfer.scopeKey)
            ?: return AndroidFavoriteAttentionResolution(emptyList(), transfer.entries)
        val lookupKeysByToken = mutableMapOf<String, MutableSet<String>>()
        candidatesByLookupKey.forEach { (lookupKey, candidates) ->
            if (lookupKey.isBlank()) return@forEach
            candidates.asSequence()
                .filter(::isSafePortableContactCandidate)
                .map { token(scopeKey, it) }
                .forEach { matchToken ->
                    lookupKeysByToken.getOrPut(matchToken, ::mutableSetOf).add(lookupKey)
                }
        }

        val proposed = mutableListOf<Pair<Int, Pair<String, Int>>>()
        val unresolved = transfer.entries.indices.toMutableSet()
        transfer.entries.forEachIndexed { index, entry ->
            val matches = entry.tokens.asSequence()
                .flatMap { lookupKeysByToken[it].orEmpty().asSequence() }
                .toSet()
            if (matches.size == 1) proposed += index to (matches.single() to entry.colorArgb)
        }

        val duplicatedDestinations = proposed.groupBy { it.second.first }
            .filterValues { it.size > 1 }
            .keys
        val matches = buildList {
            proposed.forEach { (index, match) ->
                if (match.first !in duplicatedDestinations) {
                    add(
                        AndroidFavoriteAttentionMatch(
                            lookupKey = match.first,
                            entry = transfer.entries[index].copy(
                                colorArgb = match.second or OPAQUE_ALPHA,
                            ),
                        ),
                    )
                    unresolved.remove(index)
                }
            }
        }
        return AndroidFavoriteAttentionResolution(
            matches = matches,
            unresolvedEntries = unresolved.sorted().map(transfer.entries::get),
        )
    }

    internal fun valid(transfer: AndroidFavoriteAttentionTransfer): Boolean =
        decodeScopeKey(transfer.scopeKey) != null &&
            transfer.entries.size in 1..MAX_TRANSFER_ENTRIES &&
            transfer.entries.all { entry ->
                entry.tokens.size in 1..MAX_TOKENS_PER_ENTRY &&
                    entry.tokens.distinct().size == entry.tokens.size &&
                    entry.tokens.all(::isValidToken)
            }

    private fun token(
        scopeKey: ByteArray,
        candidate: AndroidFavoriteIdentityCandidate,
    ): String {
        val payload = ByteArrayOutputStream().use { bytes ->
            DataOutputStream(bytes).use { output ->
                output.writeTokenPart(TOKEN_DOMAIN)
                output.writeTokenPart(candidate.kind.name)
                output.writeTokenPart(candidate.accountType)
                output.writeTokenPart(candidate.dataSet.orEmpty())
                output.writeTokenPart(candidate.value)
            }
            bytes.toByteArray()
        }
        return try {
            val mac = Mac.getInstance(HMAC_ALGORITHM)
            mac.init(SecretKeySpec(scopeKey, HMAC_ALGORITHM))
            URL_ENCODER.encodeToString(mac.doFinal(payload))
        } finally {
            payload.fill(0)
        }
    }

    private fun decodeScopeKey(encoded: String): ByteArray? = runCatching {
        URL_DECODER.decode(encoded).takeIf { it.size == SCOPE_KEY_BYTES }
    }.getOrNull()

    private fun isValidToken(token: String): Boolean =
        token.length == SHA_256_TOKEN_LENGTH && token.all(URL_TOKEN_CHARACTERS::contains)

    private fun DataOutputStream.writeTokenPart(value: String) {
        val encoded = value.toByteArray(Charsets.UTF_8)
        try {
            writeInt(encoded.size)
            write(encoded)
        } finally {
            encoded.fill(0)
        }
    }

    private const val TOKEN_DOMAIN = "openmink:android-favorite-attention:v1"
    private const val HMAC_ALGORITHM = "HmacSHA256"
    private const val SCOPE_KEY_BYTES = 32
    private const val SHA_256_TOKEN_LENGTH = 43
    private const val OPAQUE_ALPHA = 0xFF000000.toInt()
    internal const val MAX_TRANSFER_ENTRIES = 512
    internal const val MAX_TOKENS_PER_ENTRY = 16
    private const val URL_TOKEN_CHARACTERS =
        "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-_"
    private val URL_ENCODER = Base64.getUrlEncoder().withoutPadding()
    private val URL_DECODER = Base64.getUrlDecoder()
}
