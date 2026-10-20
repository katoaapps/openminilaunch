package com.katoaapps.openminilaunch.ui.minkpad

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.katoaapps.openminilaunch.R
import com.katoaapps.openminilaunch.features.minkpad.input.MinkPadAction

/** Twelve consistently sized controls; sticky modifiers retain the existing report behavior. */
@Composable
internal fun MinkPadKeyboardGrid(
    enabled: Boolean, modifiers: Int, onModifiers: (Int) -> Unit, onInput: (MinkPadAction) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        MinkPadControlRow(listOf(MinkPadAction.ESCAPE, MinkPadAction.TAB, MinkPadAction.ENTER, MinkPadAction.BACKSPACE), enabled, onInput)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            modifierKeys.forEach { (label, bit) ->
                val active = modifiers and bit != 0
                FilledTonalButton(
                    onClick = { onModifiers(modifiers xor bit) }, enabled = enabled,
                    modifier = Modifier.weight(1f).heightIn(min = 52.dp).semantics { selected = active },
                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 14.dp),
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = if (active) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSecondaryContainer),
                ) { Text(stringResource(label)) }
            }
        }
        MinkPadControlRow(listOf(MinkPadAction.LEFT, MinkPadAction.UP, MinkPadAction.DOWN, MinkPadAction.RIGHT), enabled, onInput)
    }
}

private val modifierKeys = listOf(
    R.string.minkpad_control to 1,
    R.string.minkpad_shift to 2,
    R.string.minkpad_alt to 4,
    R.string.minkpad_meta to 8,
)
