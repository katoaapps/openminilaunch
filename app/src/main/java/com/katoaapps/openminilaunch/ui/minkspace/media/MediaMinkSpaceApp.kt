package com.katoaapps.openminilaunch.ui.minkspace.media

import android.widget.Toast
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.katoaapps.openminilaunch.R
import com.katoaapps.openminilaunch.features.minkspace.MinkSpaceMiniApp
import com.katoaapps.openminilaunch.features.minkspace.media.MediaActions
import com.katoaapps.openminilaunch.features.minkspace.media.MediaRepository
import com.katoaapps.openminilaunch.ui.minkspace.core.MinkSpaceApp
import com.katoaapps.openminilaunch.ui.minkspace.core.MinkSpaceMiniAppManifest
import com.katoaapps.openminilaunch.ui.minkspace.core.MinkSpaceMiniAppSettingsTarget
import com.katoaapps.openminilaunch.ui.minkspace.core.MinkSpaceScope

/** Media implementation registered with the MinkSpace platform. */
internal class MediaMinkSpaceApp(
    private val onManageMedia: () -> Unit,
) : MinkSpaceApp {
    override val manifest: MinkSpaceMiniAppManifest = Manifest

    @Composable
    override fun Content(scope: MinkSpaceScope, modifier: Modifier) {
        val context = LocalContext.current
        val repository = remember(context) { MediaRepository.get(context) }
        scope.Surface(modifier = modifier) {
            MediaMiniApp(
                items = repository.items,
                expanded = scope.expansion.expanded,
                expandedPresentation = scope.expandedPresentation,
                onExpand = scope.expansion::expand,
                onOpenItem = { item ->
                    if (!MediaActions.open(context, item)) {
                        Toast.makeText(context, R.string.media_open_failed, Toast.LENGTH_LONG).show()
                    }
                },
                onManageMedia = onManageMedia,
                modifier = Modifier.fillMaxSize(),
                contentColor = scope.contentColor,
                mutedContentColor = scope.mutedContentColor,
            )
        }
    }

    companion object {
        val Manifest = MinkSpaceMiniAppManifest(
            miniApp = MinkSpaceMiniApp.MEDIA,
            labelRes = R.string.media,
            icon = Icons.Default.PhotoLibrary,
            supportsExpansion = true,
            showExpansionButton = true,
            settingsTarget = MinkSpaceMiniAppSettingsTarget.MEDIA,
        )
    }
}
