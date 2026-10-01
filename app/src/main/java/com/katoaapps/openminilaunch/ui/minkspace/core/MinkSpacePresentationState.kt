package com.katoaapps.openminilaunch.ui.minkspace.core

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.katoaapps.openminilaunch.features.minkspace.MinkSpaceMiniApp

/**
 * Shared presentation state for mini-apps that temporarily need the complete Home pill.
 *
 * The shell owns layout and gesture changes. Mini-apps only request or release their expansion.
 */
@Stable
internal class MinkSpacePresentationState(
    initiallyExpanded: MinkSpaceMiniApp? = null,
) {
    var expandedMiniApp by mutableStateOf(initiallyExpanded)
        private set

    val isExpanded: Boolean get() = expandedMiniApp != null

    fun isExpanded(miniApp: MinkSpaceMiniApp): Boolean = expandedMiniApp == miniApp

    fun setExpanded(miniApp: MinkSpaceMiniApp, expanded: Boolean) {
        if (expanded) {
            expandedMiniApp = miniApp
        } else {
            collapse(miniApp)
        }
    }

    fun collapse(miniApp: MinkSpaceMiniApp? = null) {
        if (miniApp == null || expandedMiniApp == miniApp) {
            expandedMiniApp = null
        }
    }

    fun retainOnly(visibleMiniApp: MinkSpaceMiniApp?) {
        if (expandedMiniApp != null && expandedMiniApp != visibleMiniApp) collapse()
    }
}

private val MinkSpacePresentationStateSaver = Saver<MinkSpacePresentationState, String>(
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
