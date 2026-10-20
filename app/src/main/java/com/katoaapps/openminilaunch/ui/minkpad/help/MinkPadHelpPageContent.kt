package com.katoaapps.openminilaunch.ui.minkpad.help

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.katoaapps.openminilaunch.R

/** On wide screens, example and explanation sit side by side; large text favors one column. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun MinkPadHelpPageContent(page: MinkPadHelpPage, modifier: Modifier = Modifier) {
    var selectedExample by rememberSaveable(page.title) { mutableIntStateOf(0) }
    val example = page.examples.getOrNull(selectedExample)
    val fontScale = LocalDensity.current.fontScale
    BoxWithConstraints(modifier.fillMaxSize()) {
        val shortWindow = maxHeight < 320.dp
        val sideBySide = maxWidth >= (if (shortWindow) 500.dp else 640.dp) * fontScale && example != null
        val previewHeight = (maxHeight * .6f).coerceIn(96.dp, 224.dp)
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(page.title, style = MaterialTheme.typography.titleLarge)
            if (sideBySide) Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    MinkPadHelpExamples(page.examples, selectedExample, previewHeight) { selectedExample = it }
                }
                Text(page.description, Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
            } else {
                if (example != null) MinkPadHelpExamples(page.examples, selectedExample, previewHeight) { selectedExample = it }
                Text(page.description, style = MaterialTheme.typography.bodyLarge)
            }
            Text(stringResource(R.string.minkpad_help_swipe_hint),
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MinkPadHelpExamples(
    examples: List<MinkPadGestureExample>, selected: Int, maxPreviewHeight: Dp, onSelect: (Int) -> Unit,
) {
    val example = examples[selected]
    Text(pluralStringResource(R.plurals.minkpad_finger_count, example.fingers, example.fingers),
        style = MaterialTheme.typography.labelLarge)
    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        MinkPadGestureIllustration(example, Modifier.widthIn(max = maxPreviewHeight * 1.8f).fillMaxWidth())
    }
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        examples.forEachIndexed { index, item ->
            FilterChip(selected = selected == index, onClick = { onSelect(index) },
                label = { Text(item.label) })
        }
    }
    Text(example.caption, style = MaterialTheme.typography.bodyMedium)
}
