package com.katoaapps.openminilaunch.ui.minkspace.core

import androidx.compose.runtime.Stable
import com.katoaapps.openminilaunch.features.minkspace.MinkSpaceMiniApp

/** Platform handle that lets one mini-app request MinkSpace presentation changes. */
@Stable
internal class MinkSpaceExpansionController internal constructor(
    val manifest: MinkSpaceMiniAppManifest,
    private val presentationState: MinkSpacePresentationState,
) {
    val miniApp: MinkSpaceMiniApp get() = manifest.miniApp
    val enabled: Boolean get() = manifest.supportsExpansion
    val expanded: Boolean get() = enabled && presentationState.expandedMiniApp == miniApp

    fun expand() {
        if (enabled) presentationState.expand(manifest)
    }

    fun collapse() = presentationState.collapse(miniApp)
}
