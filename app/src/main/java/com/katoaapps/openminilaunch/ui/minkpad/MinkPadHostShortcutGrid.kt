package com.katoaapps.openminilaunch.ui.minkpad

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.katoaapps.openminilaunch.features.minkpad.input.MinkPadGestureAction
import com.katoaapps.openminilaunch.features.minkpad.input.MinkPadHostPlatform
import com.katoaapps.openminilaunch.features.minkpad.input.shortcut

/** Reliable button equivalents for gestures that a phone's own system UI may reserve. */
@Composable
internal fun MinkPadHostShortcutGrid(
    platform: MinkPadHostPlatform,
    enabled: Boolean,
    onAction: (MinkPadGestureAction) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Host shortcuts", style = MaterialTheme.typography.titleSmall)
        if (platform == MinkPadHostPlatform.GENERIC) {
            Text(
                "Choose an operating system in controller settings to enable these controls.",
                style = MaterialTheme.typography.bodySmall,
            )
        }
        hostShortcutRows.forEach { actions ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                actions.forEach { action ->
                    FilledTonalButton(
                        onClick = { onAction(action) },
                        enabled = enabled && platform.shortcut(action) != null,
                        modifier = Modifier.weight(1f).heightIn(min = 52.dp),
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 12.dp),
                    ) {
                        Text(platform.shortLabel(action))
                    }
                }
            }
        }
    }
}

private val hostShortcutRows = listOf(
    listOf(MinkPadGestureAction.BACK, MinkPadGestureAction.OVERVIEW, MinkPadGestureAction.DISMISS_OVERVIEW),
    listOf(MinkPadGestureAction.PREVIOUS_APP, MinkPadGestureAction.NEXT_APP, MinkPadGestureAction.APP_LIST),
)

private fun MinkPadHostPlatform.shortLabel(action: MinkPadGestureAction): String = when (action) {
    MinkPadGestureAction.BACK -> "Back"
    MinkPadGestureAction.OVERVIEW -> when (this) {
        MinkPadHostPlatform.MAC -> "Mission Control"
        MinkPadHostPlatform.WINDOWS -> "Task View"
        else -> "Recent apps"
    }
    MinkPadGestureAction.DISMISS_OVERVIEW -> when (this) {
        MinkPadHostPlatform.MAC -> "App Exposé"
        MinkPadHostPlatform.WINDOWS -> "Close view"
        else -> "Dismiss view"
    }
    MinkPadGestureAction.PREVIOUS_APP -> if (this == MinkPadHostPlatform.MAC) "Previous Space" else "Previous app"
    MinkPadGestureAction.NEXT_APP -> if (this == MinkPadHostPlatform.MAC) "Next Space" else "Next app"
    MinkPadGestureAction.APP_LIST -> when (this) {
        MinkPadHostPlatform.WINDOWS -> "Start"
        else -> "App list"
    }
    MinkPadGestureAction.ZOOM_IN -> "Zoom in"
    MinkPadGestureAction.ZOOM_OUT -> "Zoom out"
}
