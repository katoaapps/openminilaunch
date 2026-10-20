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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.katoaapps.openminilaunch.R
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
        Text(stringResource(R.string.minkpad_host_shortcuts), style = MaterialTheme.typography.titleSmall)
        if (platform == MinkPadHostPlatform.GENERIC) {
            Text(
                stringResource(R.string.minkpad_choose_os_for_shortcuts),
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
                        Text(stringResource(platform.shortcutLabelResource(action)))
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
