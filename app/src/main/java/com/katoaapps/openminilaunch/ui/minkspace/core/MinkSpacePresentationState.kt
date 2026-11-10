package com.katoaapps.openminilaunch.ui.minkspace.core

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.katoaapps.openminilaunch.features.minkspace.MinkSpaceMiniApp

internal enum class MinkSpacePresentationMode {
    EXPANDED,
    TOTAL_SCREEN,
}

internal data class MinkSpacePresentation(
    val miniApp: MinkSpaceMiniApp,
    val mode: MinkSpacePresentationMode,
)

/**
 * Shared presentation state for mini-apps that temporarily need the Home pill or full window.
 *
 * The shell owns layout and gesture changes. Mini-apps only request or release their expansion.
 */
@Stable
internal class MinkSpacePresentationState(
    initiallyExpanded: MinkSpaceMiniApp? = null,
) {
    var presentation by mutableStateOf(
        initiallyExpanded
            ?.takeIf { MinkSpaceMiniAppCatalog.get(it).supportsExpansion }
            ?.let { MinkSpacePresentation(it, MinkSpacePresentationMode.EXPANDED) },
    )
        private set

    val expandedMiniApp: MinkSpaceMiniApp?
        get() = presentation?.takeIf { it.mode == MinkSpacePresentationMode.EXPANDED }?.miniApp
    val totalScreenMiniApp: MinkSpaceMiniApp?
        get() = presentation?.takeIf { it.mode == MinkSpacePresentationMode.TOTAL_SCREEN }?.miniApp
    val isExpanded: Boolean get() = expandedMiniApp != null
    val isTotalScreen: Boolean get() = totalScreenMiniApp != null

    internal fun expansionController(manifest: MinkSpaceMiniAppManifest): MinkSpaceExpansionController =
        MinkSpaceExpansionController(manifest, this)

    internal fun expand(manifest: MinkSpaceMiniAppManifest) {
        if (manifest.supportsExpansion) {
            presentation = MinkSpacePresentation(
                miniApp = manifest.miniApp,
                mode = MinkSpacePresentationMode.EXPANDED,
            )
        }
    }

    internal fun showTotalScreen(manifest: MinkSpaceMiniAppManifest) {
        if (manifest.supportsTotalScreen) {
            presentation = MinkSpacePresentation(
                miniApp = manifest.miniApp,
                mode = MinkSpacePresentationMode.TOTAL_SCREEN,
            )
        }
    }

    fun collapse(miniApp: MinkSpaceMiniApp? = null) {
        if (miniApp == null || presentation?.miniApp == miniApp) {
            presentation = null
        }
    }

    fun retainOnly(visibleMiniApp: MinkSpaceMiniApp?) {
        if (presentation?.mode != MinkSpacePresentationMode.EXPANDED) return
        if (expandedMiniApp != null && expandedMiniApp != visibleMiniApp) collapse()
    }
}

private val MinkSpacePresentationStateSaver = Saver<MinkSpacePresentationState, String>(
    // A total-screen experience is transient and must never reopen after recreation.
    save = { state -> state.expandedMiniApp?.stableId.orEmpty() },
    restore = { stableId ->
        MinkSpacePresentationState(MinkSpaceMiniApp.fromStableId(stableId))
    },
)

@Composable
internal fun rememberMinkSpacePresentationState(): MinkSpacePresentationState =
    rememberSaveable(saver = MinkSpacePresentationStateSaver) {
        MinkSpacePresentationState()
    }
