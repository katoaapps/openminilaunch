package com.katoaapps.openminilaunch.ui.minkspace.core

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color

/** Runtime contract implemented by every built-in MinkSpace mini-app. */
internal interface MinkSpaceApp {
    val manifest: MinkSpaceMiniAppManifest

    @Composable
    fun Content(
        scope: MinkSpaceScope,
        modifier: Modifier = Modifier,
    )
}

/** Optional runtime contract for content hosted by MinkSpace's protected full-window shell. */
internal interface MinkSpaceTotalScreenApp : MinkSpaceApp {
    @Composable
    fun TotalScreenContent(
        scope: MinkSpaceTotalScreenScope,
        modifier: Modifier = Modifier,
    )
}

@Stable
internal class MinkSpaceTotalScreenScope internal constructor(
    val use24HourClock: Boolean,
)

/** Platform state and UI primitives supplied to one mini-app by MinkSpace. */
@Stable
internal class MinkSpaceScope internal constructor(
    val compact: Boolean,
    val contentColor: Color,
    val mutedContentColor: Color,
    val containerColor: Color,
    /** Remains true until the shared collapse animation has finished. */
    val expandedPresentation: Boolean,
    val expansion: MinkSpaceExpansionController,
) {
    @Composable
    fun Surface(
        modifier: Modifier = Modifier,
        contentColor: Color = this.contentColor,
        content: @Composable () -> Unit,
    ) {
        MinkSpaceMiniAppSurface(
            compact = compact,
            containerColor = containerColor,
            contentColor = contentColor,
            modifier = modifier,
            content = content,
        )
    }
}
