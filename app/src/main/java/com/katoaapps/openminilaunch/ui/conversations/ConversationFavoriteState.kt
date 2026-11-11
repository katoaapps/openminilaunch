package com.katoaapps.openminilaunch.ui.conversations

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.katoaapps.openminilaunch.data.LauncherStore
import com.katoaapps.openminilaunch.features.contacts.FavoriteAndroidContactsRepository
import com.katoaapps.openminilaunch.features.contacts.FavoriteContactCatalog
import com.katoaapps.openminilaunch.features.demo.DemoFavoriteData
import com.katoaapps.openminilaunch.model.FavoriteContact
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Loads the mixed favorite catalog used to match notification conversations. */
@Composable
internal fun rememberOrderedConversationFavorites(
    store: LauncherStore,
    refreshKey: Any?,
): List<FavoriteContact> {
    if (store.demoModeEnabled) {
        return remember(store.demoScenario) { DemoFavoriteData.contacts(store.demoScenario) }
    }
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var resumeRevision by remember { mutableIntStateOf(0) }
    var favorites by remember { mutableStateOf<List<FavoriteContact>>(emptyList()) }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) resumeRevision++
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val locale = context.resources.configuration.locales[0] ?: Locale.getDefault()
    LaunchedEffect(refreshKey, resumeRevision, locale) {
        favorites = withContext(Dispatchers.IO) {
            val androidContacts = FavoriteAndroidContactsRepository(context).use { repository ->
                repository.favorites().also {
                    store.favoriteAndroidAttentionReconciler.reconcilePending(
                        repository::portableIdentityCandidates,
                    )
                }
            }
            val integratedContacts = store.favoriteIntegratedContactStore.contacts()
            store.favoriteContactOrderStore.applyTo(
                FavoriteContactCatalog.combine(androidContacts, integratedContacts, locale),
            )
        }
    }

    return favorites
}
