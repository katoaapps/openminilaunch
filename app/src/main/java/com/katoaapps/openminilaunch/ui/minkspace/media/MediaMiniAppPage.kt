package com.katoaapps.openminilaunch.ui.minkspace.media

import android.widget.Toast
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.katoaapps.openminilaunch.R
import com.katoaapps.openminilaunch.features.minkspace.media.MediaActions
import com.katoaapps.openminilaunch.features.minkspace.media.MediaRepository
import com.katoaapps.openminilaunch.ui.minkspace.core.MinkSpaceMiniAppSurface

/** Home-facing entry point. Feature state and common MinkSpace framing stay out of the host. */
@Composable
internal fun MediaMiniAppPage(
    compact: Boolean,
    contentColor: Color,
    mutedContentColor: Color,
    containerColor: Color,
    onManageMedia: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val repository = remember(context) { MediaRepository.get(context) }
    MinkSpaceMiniAppSurface(
        compact = compact,
        containerColor = containerColor,
        contentColor = contentColor,
        modifier = modifier,
    ) {
        MediaMiniApp(
            items = repository.items,
            onOpenItem = { item ->
                if (!MediaActions.open(context, item)) {
                    Toast.makeText(context, R.string.media_open_failed, Toast.LENGTH_LONG).show()
                }
            },
            onManageMedia = onManageMedia,
            modifier = Modifier.fillMaxSize(),
            contentColor = contentColor,
            mutedContentColor = mutedContentColor,
        )
    }
}
