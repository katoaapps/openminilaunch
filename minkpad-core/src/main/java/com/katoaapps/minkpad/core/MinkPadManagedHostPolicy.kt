package com.katoaapps.minkpad.core

/** Pure ordering policy for app-managed hosts layered over Android-owned bonds. */
data class ManagedHostCandidate<T>(
    val address: String,
    val name: String,
    val lastUsed: Long,
    val value: T,
)

fun <T> orderManagedHosts(candidates: Collection<ManagedHostCandidate<T>>): List<T> =
    candidates
        .asSequence()
        .filter { it.lastUsed > 0L }
        .sortedWith(
            compareByDescending<ManagedHostCandidate<T>> { it.lastUsed }
                .thenBy(String.CASE_INSENSITIVE_ORDER) { it.name }
                .thenBy { it.address },
        )
        .map { it.value }
        .toList()

enum class MinkPadKeyLossPlan {
    IGNORE,
    SUPPRESS_RECONNECT,
    DISCONNECT_ACTIVE_ATTEMPT,
}

/** Pure Android 16 key-loss policy, separate from Bluetooth framework callbacks. */
fun minkPadKeyLossPlan(
    lastUsed: Long,
    lostAddress: String,
    activeAddress: String?,
    pendingAddress: String?,
): MinkPadKeyLossPlan = when {
    lastUsed <= 0L -> MinkPadKeyLossPlan.IGNORE
    lostAddress == activeAddress || lostAddress == pendingAddress ->
        MinkPadKeyLossPlan.DISCONNECT_ACTIVE_ATTEMPT
    else -> MinkPadKeyLossPlan.SUPPRESS_RECONNECT
}
