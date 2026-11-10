package com.katoaapps.openminilaunch.features.contacts

import android.content.Context
import com.katoaapps.openminilaunch.model.FavoriteContactId

/** Coordinates export-scoped token creation and delayed, exact-only color reconciliation. */
internal class AndroidFavoriteAttentionReconciler(
    private val colorStore: FavoriteAttentionColorStore,
    private val pendingStore: AndroidFavoriteAttentionPendingStore,
    private val candidates: () -> Map<String, List<AndroidFavoriteIdentityCandidate>>,
) {
    constructor(context: Context, colorStore: FavoriteAttentionColorStore) : this(
        colorStore = colorStore,
        pendingStore = AndroidFavoriteAttentionPendingStore(context.applicationContext),
        candidates = {
            FavoriteAndroidContactsRepository(context.applicationContext).use {
                it.portableIdentityCandidates()
            }
        },
    )

    fun createTransfer(): AndroidFavoriteAttentionTransfer? {
        val colors = colorStore.androidColorsForReconciliation()
        if (colors.isEmpty()) return null
        return AndroidFavoriteAttentionTransfers.create(
            colorsByLookupKey = colors,
            candidatesByLookupKey = candidates(),
        )
    }

    /** Save first; contacts may not have finished syncing when a new device imports the backup. */
    @Synchronized
    fun restore(transfer: AndroidFavoriteAttentionTransfer?) {
        transfer?.takeIf(AndroidFavoriteAttentionTransfers::valid)?.let(pendingStore::add)
    }

    @Synchronized
    fun reconcilePending(
        currentCandidates: (() -> Map<String, List<AndroidFavoriteIdentityCandidate>>)? = null,
    ): Int {
        val pending = pendingStore.load()
        if (pending.isEmpty()) return 0
        val availableCandidates = (currentCandidates ?: candidates)()
        if (availableCandidates.isEmpty()) return 0
        var resolvedCount = 0
        val remaining = buildList {
            pending.forEach { transfer ->
                val result = AndroidFavoriteAttentionTransfers.resolve(transfer, availableCandidates)
                val failedWrites = buildList {
                    result.matches.forEach { match ->
                        if (
                            colorStore.update(
                                FavoriteContactId.Android(match.lookupKey),
                                match.entry.colorArgb,
                            )
                        ) {
                            resolvedCount++
                        } else {
                            add(match.entry)
                        }
                    }
                }
                val remainingEntries = (result.unresolvedEntries + failedWrites).distinct()
                if (remainingEntries.isNotEmpty()) {
                    add(transfer.copy(entries = remainingEntries))
                }
            }
        }
        pendingStore.replace(remaining)
        return resolvedCount
    }
}
