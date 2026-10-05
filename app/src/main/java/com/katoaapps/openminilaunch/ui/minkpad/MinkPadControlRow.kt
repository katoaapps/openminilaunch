package com.katoaapps.openminilaunch.ui.minkpad

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.katoaapps.openminilaunch.R
import com.katoaapps.openminilaunch.features.minkpad.input.MinkPadAction

@Composable
internal fun MinkPadControlRow(actions: List<MinkPadAction>, enabled: Boolean, onInput: (MinkPadAction) -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        actions.forEach { action ->
            FilledTonalButton(
                onClick = { onInput(action) },
                enabled = enabled,
                modifier = Modifier.weight(1f).heightIn(min = 52.dp),
                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 14.dp),
            ) { Text(stringResource(action.labelResource)) }
        }
    }
}

internal val MinkPadAction.labelResource: Int
    get() = when (this) {
        MinkPadAction.UP -> R.string.minkpad_up
        MinkPadAction.DOWN -> R.string.minkpad_down
        MinkPadAction.LEFT -> R.string.minkpad_left
        MinkPadAction.RIGHT -> R.string.minkpad_right
        MinkPadAction.ENTER -> R.string.minkpad_enter
        MinkPadAction.ESCAPE -> R.string.minkpad_escape
        MinkPadAction.TAB -> R.string.minkpad_tab
        MinkPadAction.BACKSPACE -> R.string.minkpad_backspace
        MinkPadAction.DELETE -> R.string.minkpad_delete
    }
