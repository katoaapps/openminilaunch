package com.katoaapps.openminilaunch.ui.minkspace.profile

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ContactPage
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import com.katoaapps.openminilaunch.R
import com.katoaapps.openminilaunch.data.LauncherStore
import com.katoaapps.openminilaunch.features.minkspace.MinkSpaceMiniApp
import com.katoaapps.openminilaunch.features.profile.ProfileQrCode
import com.katoaapps.openminilaunch.features.profile.ProfileQrDensity
import com.katoaapps.openminilaunch.features.profile.ProfileQrGenerator
import com.katoaapps.openminilaunch.features.profile.ProfileState
import com.katoaapps.openminilaunch.features.profile.resolveSelectedLinks
import com.katoaapps.openminilaunch.ui.components.CenteredActionPrompt
import com.katoaapps.openminilaunch.ui.minkspace.core.MinkSpaceMiniAppSurface
import com.katoaapps.openminilaunch.ui.minkspace.core.MinkSpacePresentationState
import com.katoaapps.openminilaunch.ui.profile.ProfileQrDialog

/** Owns vCard profile state, MinkSpace expansion, and the full-screen QR handoff. */
@Composable
internal fun VCardMiniAppPage(
    store: LauncherStore,
    compact: Boolean,
    contentColor: Color,
    mutedContentColor: Color,
    containerColor: Color,
    presentationState: MinkSpacePresentationState,
    openVCardSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val expanded = presentationState.isExpanded(MinkSpaceMiniApp.VCARD)
    val state = store.profileRepository.state
    val ready = state as? ProfileState.Ready
    val selectedLinks = ready?.let { it.card.resolveSelectedLinks(it.links) }.orEmpty()
    val qrCode = remember(ready) {
        ready?.let { ProfileQrGenerator.describe(it.card, it.links) }
            ?.takeUnless { it.density == ProfileQrDensity.TOO_DENSE }
    }
    var qrDialog by remember { mutableStateOf<ProfileQrCode?>(null) }

    BackHandler(enabled = expanded) {
        presentationState.collapse(MinkSpaceMiniApp.VCARD)
    }

    MinkSpaceMiniAppSurface(
        compact = compact,
        containerColor = containerColor,
        contentColor = contentColor,
        modifier = modifier,
    ) {
        when {
            ready == null || ready.card.fullName.isBlank() -> VCardUnavailableContent(
                state = state,
                compact = compact,
                contentColor = contentColor,
                mutedContentColor = mutedContentColor,
                onOpenSettings = openVCardSettings,
            )
            else -> VCardMiniAppContent(
                store = store,
                card = ready.card,
                selectedLinks = selectedLinks,
                qrCode = qrCode,
                compact = compact,
                expanded = expanded,
                contentColor = contentColor,
                mutedContentColor = mutedContentColor,
                onExpandedChanged = { shouldExpand ->
                    presentationState.setExpanded(MinkSpaceMiniApp.VCARD, shouldExpand)
                },
                onOpenQr = { qrCode?.let { qrDialog = it } },
                onFixQr = openVCardSettings,
            )
        }
    }

    qrDialog?.let { code ->
        ProfileQrDialog(code, ready?.card?.fullName.orEmpty()) { qrDialog = null }
    }
}

@Composable
private fun VCardUnavailableContent(
    state: ProfileState,
    compact: Boolean,
    contentColor: Color,
    mutedContentColor: Color,
    onOpenSettings: () -> Unit,
) {
    val unavailable = state is ProfileState.Invalid || state is ProfileState.Unreadable
    CenteredActionPrompt(
        icon = Icons.Default.ContactPage,
        title = stringResource(
            if (unavailable) R.string.profile_could_not_be_opened else R.string.create_your_profile,
        ),
        description = stringResource(
            if (unavailable) R.string.profile_encryption_recovery else R.string.profile_empty_description,
        ),
        actionLabel = stringResource(if (unavailable) R.string.open_profile else R.string.create_profile),
        actionIcon = if (unavailable) null else Icons.Default.Add,
        onAction = onOpenSettings,
        contentColor = contentColor,
        mutedContentColor = mutedContentColor,
        compact = compact,
        modifier = Modifier.fillMaxSize(),
    )
}
