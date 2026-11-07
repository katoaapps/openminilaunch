package com.katoaapps.openminilaunch.ui.launcher.home

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.katoaapps.openminilaunch.data.LauncherStore
import com.katoaapps.openminilaunch.features.contacts.FavoriteAndroidContactsRepository
import com.katoaapps.openminilaunch.features.contacts.FavoriteContactCatalog
import com.katoaapps.openminilaunch.features.conversations.FavoriteConversationAttention
import com.katoaapps.openminilaunch.features.conversations.NotificationConversation
import com.katoaapps.openminilaunch.features.conversations.pendingFavoriteConversationAttention
import com.katoaapps.openminilaunch.model.FavoriteContact
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Loads favorite identities only when Home needs to resolve notification attention. */
@Composable
internal fun rememberPendingFavoriteAttention(
    store: LauncherStore,
    conversations: List<NotificationConversation>,
    defaultColorArgb: Int,
): List<FavoriteConversationAttention> {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var resumeRevision by remember { mutableIntStateOf(0) }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) resumeRevision++
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val locale = context.resources.configuration.locales[0] ?: Locale.getDefault()
    val favorites by produceState<List<FavoriteContact>>(
        initialValue = emptyList(),
        key1 = conversations,
        key2 = resumeRevision,
        key3 = locale,
    ) {
        value = withContext(Dispatchers.IO) {
            val androidContacts = FavoriteAndroidContactsRepository(context).use { it.favorites() }
            val integratedContacts = store.favoriteIntegratedContactStore.contacts()
            store.favoriteContactOrderStore.applyTo(
                FavoriteContactCatalog.combine(androidContacts, integratedContacts, locale),
            )
        }
    }

    return pendingFavoriteConversationAttention(
        conversations = conversations,
        orderedFavorites = favorites,
        defaultColorArgb = defaultColorArgb,
        acknowledgedThrough = store::acknowledgedConversationActivityAt,
        colorFor = store.favoriteAttentionColorStore::color,
    )
}
