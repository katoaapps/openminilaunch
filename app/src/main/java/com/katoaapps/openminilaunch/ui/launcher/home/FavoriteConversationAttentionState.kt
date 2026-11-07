package com.katoaapps.openminilaunch.ui.launcher.home

import androidx.compose.runtime.Composable
import com.katoaapps.openminilaunch.data.LauncherStore
import com.katoaapps.openminilaunch.features.conversations.FavoriteConversationAttention
import com.katoaapps.openminilaunch.features.conversations.NotificationConversation
import com.katoaapps.openminilaunch.features.conversations.pendingFavoriteConversationAttention
import com.katoaapps.openminilaunch.ui.conversations.rememberOrderedConversationFavorites

/** Loads favorite identities only when Home needs to resolve notification attention. */
@Composable
internal fun rememberPendingFavoriteAttention(
    store: LauncherStore,
    conversations: List<NotificationConversation>,
    defaultColorArgb: Int,
): List<FavoriteConversationAttention> {
    val favorites = rememberOrderedConversationFavorites(store, refreshKey = conversations)

    return pendingFavoriteConversationAttention(
        conversations = conversations,
        orderedFavorites = favorites,
        defaultColorArgb = defaultColorArgb,
        acknowledgedThrough = store::acknowledgedConversationActivityAt,
        colorFor = store.favoriteAttentionColorStore::color,
    )
}
