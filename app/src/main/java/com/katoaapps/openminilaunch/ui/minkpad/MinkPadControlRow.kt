package com.katoaapps.openminilaunch.ui.minkpad

import com.katoaapps.minkpad.core.R as MinkPadR

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
import com.katoaapps.minkpad.core.input.MinkPadAction

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
        MinkPadAction.UP -> MinkPadR.string.minkpad_up
        MinkPadAction.DOWN -> MinkPadR.string.minkpad_down
        MinkPadAction.LEFT -> MinkPadR.string.minkpad_left
        MinkPadAction.RIGHT -> MinkPadR.string.minkpad_right
        MinkPadAction.ENTER -> MinkPadR.string.minkpad_enter
        MinkPadAction.ESCAPE -> MinkPadR.string.minkpad_escape
        MinkPadAction.TAB -> MinkPadR.string.minkpad_tab
        MinkPadAction.BACKSPACE -> MinkPadR.string.minkpad_backspace
        MinkPadAction.DELETE -> MinkPadR.string.minkpad_delete
    }
