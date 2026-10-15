package com.katoaapps.openminilaunch.ui.minkspace.core

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.ui.graphics.vector.ImageVector
import com.katoaapps.openminilaunch.R
import com.katoaapps.openminilaunch.features.minkspace.MinkSpaceMiniApp

/**
 * UI metadata for every known MinkSpace mini-app.
 *
 * Add new mini-app presentation metadata here after assigning it a stable identity in
 * [MinkSpaceMiniApp]. Home content remains owned by that mini-app's UI package.
 */
internal data class MinkSpaceMiniAppSpec(
    val miniApp: MinkSpaceMiniApp,
    @param:StringRes val labelRes: Int,
    val icon: ImageVector,
    val manager: MinkSpaceManagerDestination? = null,
    @param:StringRes val managerLabelRes: Int? = null,
)

internal enum class MinkSpaceManagerDestination {
    TODO,
    CALCULATOR,
    MEDIA,
    MUSIC,
}

internal object MinkSpaceMiniAppCatalog {
    val all: List<MinkSpaceMiniAppSpec> = listOf(
        MinkSpaceMiniAppSpec(
            miniApp = MinkSpaceMiniApp.TODO,
            labelRes = R.string.todo_page_title,
            icon = Icons.Default.Checklist,
            manager = MinkSpaceManagerDestination.TODO,
            managerLabelRes = R.string.open_todos,
        ),
        MinkSpaceMiniAppSpec(
            miniApp = MinkSpaceMiniApp.CALCULATOR,
            labelRes = R.string.calculator,
            icon = Icons.Default.Calculate,
            manager = MinkSpaceManagerDestination.CALCULATOR,
            managerLabelRes = R.string.open_calculator_lab,
        ),
        MinkSpaceMiniAppSpec(
            miniApp = MinkSpaceMiniApp.MEDIA,
            labelRes = R.string.media,
            icon = Icons.Default.PhotoLibrary,
            manager = MinkSpaceManagerDestination.MEDIA,
            managerLabelRes = R.string.open_media_lab,
        ),
        MinkSpaceMiniAppSpec(
            miniApp = MinkSpaceMiniApp.RECORDER,
            labelRes = R.string.recorder,
            icon = Icons.Default.Mic,
        ),
        MinkSpaceMiniAppSpec(
            miniApp = MinkSpaceMiniApp.MUSIC,
            labelRes = R.string.music_player,
            icon = Icons.Default.MusicNote,
            manager = MinkSpaceManagerDestination.MUSIC,
            managerLabelRes = R.string.open_music_player_preview,
        ),
        MinkSpaceMiniAppSpec(
            miniApp = MinkSpaceMiniApp.WIDGET,
            labelRes = R.string.widgets,
            icon = Icons.Default.Widgets,
        ),
    )

    private val byMiniApp = all.associateBy(MinkSpaceMiniAppSpec::miniApp)

    init {
        check(all.size == MinkSpaceMiniApp.entries.size && byMiniApp.keys == MinkSpaceMiniApp.entries.toSet()) {
            "Every MinkSpaceMiniApp must have exactly one UI spec"
        }
    }

    fun get(miniApp: MinkSpaceMiniApp): MinkSpaceMiniAppSpec =
        checkNotNull(byMiniApp[miniApp]) { "Missing MinkSpace UI spec for ${miniApp.stableId}" }
}
