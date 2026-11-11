package com.katoaapps.openminilaunch.ui.conversations

import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.Dp
import com.katoaapps.openminilaunch.model.FavoriteIntegratedContact
import com.katoaapps.openminilaunch.platform.DeviceActions
import com.katoaapps.openminilaunch.ui.components.DrawableIcon

/** Uses the provider's live conversation artwork without copying it into Mink-owned storage. */
@Composable
internal fun FavoriteIntegratedContactAvatar(
    contact: FavoriteIntegratedContact,
    availability: FavoriteIntegratedContactAvailability?,
    actions: DeviceActions,
    size: Dp,
) {
    val shortcut = (availability as? FavoriteIntegratedContactAvailability.Available)?.shortcut
    if (shortcut != null) {
        DrawableIcon(
            drawable = actions.launcherTargetIcon(shortcut)
                ?: actions.appIcon(shortcut.packageName),
            iconKey = "favorite-contact:${shortcut.selectionKey}",
            size = size,
        )
    } else {
        FavoriteContactAvatar(contact.displayName, photoUri = null, size = size)
    }
}
